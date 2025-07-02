package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.RoleSwitchRequest;
import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.RedisSessionService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final RedisSessionService redisSessionService;
  private static final String ROLE_PREFIX = "ROLE_";

  @PostMapping("/switch")
  @Operation(summary = "Switch user role")
  public Mono<ResponseEntity<Api<UserInfo>>> switchRole(
      @RequestBody RoleSwitchRequest request, Authentication auth) {
    String userId = auth.getName();
    String requestedRole = request.getRole();

    return validateAndPrepareRoleSwitch(userId, requestedRole, auth)
        .flatMap(this::performRoleSwitch);
  }

  private Mono<RoleSwitchContext> validateAndPrepareRoleSwitch(
      String userId, String requestedRole, Authentication auth) {
    String currentJti = null;
    List<String> availableRoles;

    // Get the current JTI and roles if available
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      currentJti = jwtAuth.getJti();
      availableRoles = jwtAuth.getRoles();
      log.debug("Current JTI found: {}, Available roles: {}", currentJti, availableRoles);
    } else {
      availableRoles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    // Ensure the requested role has the ROLE_ prefix
    String prefixedRequestedRole = ensureRolePrefix(requestedRole);

    log.debug(
        "Role switch request - User: {}, Available Roles: {}, Requested Role: {}, Prefixed Role: {}",
        userId,
        availableRoles,
        requestedRole,
        prefixedRequestedRole);

    // Verify the requested role is available
    if (!isRoleAvailable(availableRoles, requestedRole, prefixedRequestedRole)) {
      log.warn(
          "User {} does not have access to role: {} or {}",
          userId,
          requestedRole,
          prefixedRequestedRole);
      return Mono.error(
          new IllegalArgumentException("User does not have access to the requested role"));
    }

    return Mono.just(
        new RoleSwitchContext(
            userId,
            requestedRole,
            prefixedRequestedRole,
            currentJti,
            availableRoles,
            redisSessionService.generateJti()));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> performRoleSwitch(RoleSwitchContext context) {
    // First invalidate the old session if JTI is available
    return invalidateOldSessionIfNeeded(context)
        .flatMap(invalidated -> updateUserAndCreateSession(context));
  }

  private Mono<Boolean> invalidateOldSessionIfNeeded(RoleSwitchContext context) {
    if (context.getCurrentJti() != null) {
      return redisSessionService
          .invalidateSession(context.getUserId(), context.getCurrentJti())
          .doOnNext(
              invalidated -> {
                if (Boolean.FALSE.equals(invalidated)) {
                  log.warn(
                      "Failed to invalidate old session for user {} with JTI {}",
                      context.getUserId(),
                      context.getCurrentJti());
                }
              });
    }
    return Mono.just(true);
  }

  private Mono<ResponseEntity<Api<UserInfo>>> updateUserAndCreateSession(
      RoleSwitchContext context) {
    return userRepository
        .findByEmail(context.getUserId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
        .flatMap(
            user -> {
              // Update only the activeRole without the ROLE_ prefix
              String dbRole = context.getRequestedRole().replace(ROLE_PREFIX, "");
              user.setActiveRole(dbRole);
              return userRepository.save(user);
            })
        .flatMap(user -> createNewSession(user, context));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> createNewSession(
      User user, RoleSwitchContext context) {
    // Get all roles from the user object and ensure ROLE_ prefix
    List<String> allRoles = ensureRolePrefixForAll(user.getRoles());

    // Create new session with the updated role
    return redisSessionService
        .createSession(
            context.getUserId(), context.getNewJti(), context.getPrefixedRequestedRole(), allRoles)
        .flatMap(sessionCreated -> handleSessionCreation(sessionCreated, user, context, allRoles));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> handleSessionCreation(
      Boolean sessionCreated, User user, RoleSwitchContext context, List<String> allRoles) {
    if (Boolean.FALSE.equals(sessionCreated)) {
      log.error(
          "Failed to create session for user {} with new role {}",
          context.getUserId(),
          context.getPrefixedRequestedRole());
      return createErrorResponse("Failed to create session");
    }

    // Generate new token with updated active role but preserve all roles
    String token =
        jwtService.generateToken(
            context.getUserId(), allRoles, context.getPrefixedRequestedRole(), context.getNewJti());

    log.debug(
        "Role switch successful - User: {}, New Role: {}, All Roles: {}, New JTI: {}",
        context.getUserId(),
        context.getPrefixedRequestedRole(),
        allRoles,
        context.getNewJti());

    return createSuccessResponse(user, context.getPrefixedRequestedRole(), allRoles, token);
  }

  private String ensureRolePrefix(String role) {
    return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
  }

  private List<String> ensureRolePrefixForAll(List<String> roles) {
    return roles.stream()
        .map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role)
        .toList();
  }

  private boolean isRoleAvailable(
      List<String> availableRoles, String requestedRole, String prefixedRequestedRole) {
    return availableRoles.contains(prefixedRequestedRole)
        || availableRoles.contains(requestedRole)
        || availableRoles.contains(requestedRole.replace(ROLE_PREFIX, ""));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> createErrorResponse(String message) {
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .<Api<UserInfo>>body(
                new Api<>(
                    ErrorValidationMessage.FAILURE, message, Optional.empty(), Optional.empty())));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> createSuccessResponse(
      User user, String activeRole, List<String> roles, String token) {
    return Mono.just(
        ResponseEntity.ok()
            .<Api<UserInfo>>body(
                new Api<>(
                    "success",
                    "Role switched successfully",
                    Optional.of(
                        UserInfo.builder()
                            .username(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .activeRole(activeRole)
                            .roles(roles)
                            .token(token)
                            .email(user.getEmail())
                            .build()),
                    Optional.empty())));
  }

  // Context class to hold role switch operation data
  @Data
  @AllArgsConstructor
  private static class RoleSwitchContext {
    private final String userId;
    private final String requestedRole;
    private final String prefixedRequestedRole;
    private final String currentJti;
    private final List<String> availableRoles;
    private final String newJti;
  }

  private boolean isTransientException(Throwable throwable) {
    if (throwable == null) {
      return false;
    }

    return throwable instanceof java.net.SocketTimeoutException
        || throwable instanceof java.net.ConnectException
        || throwable instanceof java.io.IOException
        || throwable instanceof java.util.concurrent.TimeoutException
        || (throwable.getCause() != null && isTransientException(throwable.getCause()));
  }
}
