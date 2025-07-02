package com.aci.smart_onboarding.security.controller;

import com.aci.smart_onboarding.constants.ApiPaths;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ForgotPasswordRequest;
import com.aci.smart_onboarding.dto.LoginRequest;
import com.aci.smart_onboarding.dto.ResetPasswordRequest;
import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.repository.PasswordResetTokenRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.*;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.security.validator.LoginRequestValidator;
import com.aci.smart_onboarding.service.IForgotPasswordService;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IAuthService;
import com.aci.smart_onboarding.service.RedisSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.concurrent.TimeoutException;
import org.springframework.scheduling.annotation.Scheduled;

@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
@RequiredArgsConstructor
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final JwtService jwtService;
  private final CustomAuthenticationManager customAuthenticationManager;
  private final LoginAuditService loginAuditService;
  private final LoginRequestValidator loginRequestValidator;
  private final LoginAttemptService loginAttemptService;
  private final Optional<AzureADAuthenticationManager> azureADAuthenticationManager;
  private final IForgotPasswordService forgotPasswordService;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final RedisSessionService redisSessionService;
  private final IAuditLogService auditLogService;
  private final IAuthService authService;

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  // Cache for user details to reduce database calls
  private final Map<String, com.aci.smart_onboarding.model.User> userCache = new ConcurrentHashMap<>();
  private static final int USER_CACHE_SIZE = 1000;
  private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";

  /**
   * Extracts authentication details from Authentication object
   */
  private static AuthenticationDetails extractAuthenticationDetails(Authentication auth) {
    List<String> authRoles =
        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    String activeRole = auth.getAuthorities().iterator().next().getAuthority();
    String dbRole = activeRole.replace(SecurityConstants.ROLE_PREFIX, "");
    
    return new AuthenticationDetails(authRoles, activeRole, dbRole);
  }

  /**
   * Creates UserInfo object from user and authentication details
   */
  private static UserInfo createUserInfo(
      com.aci.smart_onboarding.model.User user, 
      String username, 
      String activeRole, 
      List<String> roles, 
      String token) {
    return UserInfo.builder()
        .username(username)
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .activeRole(activeRole)
        .roles(roles)
        .token(token)
        .email(username)
        .build();
  }

  /**
   * Creates session asynchronously without blocking the response
   */
  private void createSessionAsync(String username, String jti, String activeRole, List<String> roles) {
    redisSessionService
        .createSession(username, jti, activeRole, roles)
        .subscribe(
            sessionCreated -> {
              if (Boolean.FALSE.equals(sessionCreated)) {
                log.error("Failed to create session for user: {}", username);
              }
            },
            error -> log.error("Error creating session for user {}: {}", username, error.getMessage())
        );
  }

  /**
   * Cached user lookup to reduce database calls
   */
  private Mono<com.aci.smart_onboarding.model.User> getCachedUser(String username, String dbRole) {
    String cacheKey = username + ":" + dbRole;
    com.aci.smart_onboarding.model.User cachedUser = userCache.get(cacheKey);
    
    if (cachedUser != null) {
      log.debug("User found in cache: {}", username);
      return Mono.just(cachedUser);
    }

    return userRepository
        .findByEmailAndRole(username, dbRole)
        .doOnNext(user -> {
          // Cache the user if cache is not full
          if (userCache.size() < USER_CACHE_SIZE) {
            userCache.put(cacheKey, user);
            log.debug("User cached: {}", username);
          }
        })
        .switchIfEmpty(Mono.error(new BadCredentialsException("User details not found")));
  }

  /**
   * Optimized user lookup with circuit breaker pattern
   */
  private Mono<com.aci.smart_onboarding.model.User> getUserWithFallback(String username, String dbRole) {
    return getCachedUser(username, dbRole)
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(TimeoutException.class, e -> {
          log.warn("User lookup timeout for {}, falling back to direct DB call", username);
          return userRepository
              .findByEmailAndRole(username, dbRole)
              .switchIfEmpty(Mono.error(new BadCredentialsException("User details not found")));
        })
        .onErrorResume(Exception.class, e -> {
          log.error("Error in user lookup for {}: {}", username, e.getMessage());
          return Mono.error(new BadCredentialsException("User details not found"));
        });
  }

  /**
   * Record for holding authentication details
   */
  private static class AuthenticationDetails {
    private final List<String> roles;
    private final String activeRole;
    private final String dbRole;

    public AuthenticationDetails(List<String> roles, String activeRole, String dbRole) {
      this.roles = roles;
      this.activeRole = activeRole;
      this.dbRole = dbRole;
    }

    public List<String> getRoles() { return roles; }
    public String getActiveRole() { return activeRole; }
    public String getDbRole() { return dbRole; }
  }

  @PostMapping(ApiPaths.AUTH_LOGIN)
  @Operation(
      summary = "User Login",
      description = "Authenticate user and return JWT token with user information",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                """
                            {
                                "status": "success",
                                "message": "Login successful",
                                "data": {
                                    "username": "pm@gmail.com",
                                    "firstName": "PM",
                                    "lastName": "User",
                                    "activeRole": "ROLE_PM",
                                    "roles": ["ROLE_PM", "ROLE_MANAGER"],
                                    "token": "jwt.token.here",
                                    "email": "pm@gmail.com"
                                },
                                "errors": null
                            }
                            """))),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                """
                            {
                                "status": "failure",
                                "message": "Invalid credentials",
                                "data": null,
                                "errors": null
                            }
                            """))),
        @ApiResponse(
            responseCode = "423",
            description = "Account blocked",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                """
                            {
                                "status": "failure",
                                "message": "Too many failed attempts. Account blocked for 15 minutes.",
                                "data": null,
                                "errors": null
                            }
                            """)))
      })
  public Mono<ResponseEntity<Api<UserInfo>>> login(
      @RequestBody LoginRequest loginRequest, ServerWebExchange exchange) {
    String username = loginRequest.getUsername();
    String password = loginRequest.getPassword();
    String clientIp =
        Optional.ofNullable(exchange.getRequest().getRemoteAddress())
            .map(address -> address.getAddress().getHostAddress())
            .orElse("unknown");

    // Early validation with minimal overhead
    if (username == null || password == null || 
        username.trim().isEmpty() || password.trim().isEmpty()) {
      String errorMessage = "Username and password are required";
      loginAuditService.logLoginAttempt(username, clientIp, false, errorMessage);
      return Mono.just(
          ResponseEntity.badRequest()
              .<Api<UserInfo>>body(
                  new Api<>(
                      ErrorValidationMessage.FAILURE,
                      errorMessage,
                      Optional.empty(),
                      Optional.empty())));
    }

    log.info("Login request received - Username: {}, IP: {}", username, clientIp);
    exchange.getAttributes().put("username", username);
    exchange.getAttributes().put("clientIp", clientIp);

    // Check if we're in prod profile and need Azure AD
    if ("prod".equals(activeProfile)) {
      if (azureADAuthenticationManager.isEmpty()) {
        String errorMessage = "Azure AD authentication manager not configured in production";
        loginAuditService.logLoginAttempt(username, clientIp, false, errorMessage);
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .<Api<UserInfo>>body(
                    new Api<>(
                        ErrorValidationMessage.FAILURE,
                        errorMessage,
                        Optional.empty(),
                        Optional.empty())));
      }
      return azureADAuthenticationManager
          .get()
          .authenticate(new UsernamePasswordAuthenticationToken(username, password))
          .switchIfEmpty(
              Mono.error(new BadCredentialsException(SecurityConstants.INVALID_CREDENTIALS)))
          .flatMap(auth -> handleSuccessfulAuthentication(auth, username, clientIp))
          .onErrorResume(error -> handleFailedAuthentication(exchange, error));
    }

    // Optimized regular authentication flow with reduced overhead
    return customAuthenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(username, password))
        .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
        .flatMap(auth -> processSuccessfulAuthentication(auth, username, clientIp))
        .doOnSuccess(
            response -> {
              // Non-blocking audit logging
              loginAttemptService.loginSucceeded(username);
              loginAuditService.logLoginAttempt(
                  username, clientIp, true, SecurityConstants.AUTHENTICATION_SUCCESS);
            })
        .onErrorResume(error -> handleFailedAuthentication(exchange, error));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> processSuccessfulAuthentication(
      Authentication auth, String username, String clientIp) {
    // Extract roles and active role once
    AuthenticationDetails details = extractAuthenticationDetails(auth);

    log.debug(
        "Authentication successful - User: {}, Active Role: {}, All Roles: {}",
        username,
        details.getActiveRole(),
        details.getRoles());

    // Generate JTI early for parallel processing
    String jti = redisSessionService.generateJti();

    // Parallel database lookup and JWT token generation
    Mono<com.aci.smart_onboarding.model.User> userMono = getUserWithFallback(username, details.getDbRole());

    Mono<String> tokenMono = Mono.fromCallable(() -> 
        jwtService.generateToken(username, details.getRoles(), details.getActiveRole(), jti));

    // Combine user lookup and token generation in parallel
    return Mono.zip(userMono, tokenMono)
        .flatMap(tuple -> {
          com.aci.smart_onboarding.model.User user = tuple.getT1();
          String token = tuple.getT2();

          // Create session asynchronously (don't block response)
          createSessionAsync(username, jti, details.getActiveRole(), details.getRoles());

          // Build response immediately
          UserInfo userInfo = createUserInfo(user, username, details.getActiveRole(), details.getRoles(), token);

          return Mono.just(
              ResponseEntity.ok()
                  .<Api<UserInfo>>body(
                      new Api<>(
                          UserConstants.SUCCESS,
                          "Login successful",
                          Optional.of(userInfo),
                          Optional.empty())));
        });
  }

  private Mono<ResponseEntity<Api<UserInfo>>> handleSuccessfulAuthentication(
      Authentication auth, String username, String clientIp) {
    // Non-blocking audit logging
    loginAttemptService.loginSucceeded(username);
    loginAuditService.logLoginAttempt(
        username, clientIp, true, SecurityConstants.AUTHENTICATION_SUCCESS);

    // Get all roles from authentication
    AuthenticationDetails details = extractAuthenticationDetails(auth);

    log.debug(
        "Authentication successful - User: {}, Active Role: {}, All Roles: {}",
        username,
        details.getActiveRole(),
        details.getRoles());

    // Generate JTI early for parallel processing
    String jti = redisSessionService.generateJti();

    // Parallel database lookup and JWT token generation
    Mono<com.aci.smart_onboarding.model.User> userMono = getUserWithFallback(username, details.getDbRole());

    Mono<String> tokenMono = Mono.fromCallable(() -> 
        jwtService.generateToken(username, details.getRoles(), details.getActiveRole(), jti));

    // Combine user lookup and token generation in parallel
    return Mono.zip(userMono, tokenMono)
        .flatMap(tuple -> {
          com.aci.smart_onboarding.model.User user = tuple.getT1();
          String token = tuple.getT2();

          // Create session asynchronously (don't block response)
          createSessionAsync(username, jti, details.getActiveRole(), details.getRoles());

          // Build response immediately
          UserInfo userInfo = createUserInfo(user, username, details.getActiveRole(), details.getRoles(), token);

          return Mono.just(
              ResponseEntity.ok()
                  .<Api<UserInfo>>body(
                      new Api<>(
                          UserConstants.SUCCESS,
                          SecurityConstants.AUTHENTICATION_SUCCESS,
                          Optional.of(userInfo),
                          Optional.empty())));
        })
        .switchIfEmpty(
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Api<UserInfo>>body(
                        new Api<>(
                            ErrorValidationMessage.FAILURE,
                            String.format(
                                "User not found with email: %s and role: %s", username, details.getDbRole()),
                            Optional.empty(),
                            Optional.empty()))));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> handleFailedAuthentication(
      ServerWebExchange exchange, Throwable error) {
    String username = (String) exchange.getAttributes().get("username");
    String clientIp = (String) exchange.getAttributes().get("clientIp");

    if (error instanceof BadCredentialsException) {
      return loginAttemptService
          .loginFailed(username)
          .defaultIfEmpty(false)
          .map(
              blocked -> {
                if (Boolean.TRUE.equals(blocked)) {
                  String blockMessage = "Too many failed attempts. Account blocked for 15 minutes.";
                  loginAuditService.logLoginAttempt(username, clientIp, false, blockMessage);
                  loginAuditService.logSuspiciousActivity(username, clientIp, blockMessage);
                  return ResponseEntity.status(HttpStatus.LOCKED)
                      .<Api<UserInfo>>body(
                          new Api<>(
                              ErrorValidationMessage.FAILURE,
                              blockMessage,
                              Optional.empty(),
                              Optional.empty()));
                } else {
                  String errorMessage =
                      StringUtils.hasText(error.getMessage())
                          ? error.getMessage()
                          : "Invalid credentials";
                  loginAuditService.logLoginAttempt(username, clientIp, false, errorMessage);
                  return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .<Api<UserInfo>>body(
                          new Api<>(
                              ErrorValidationMessage.FAILURE,
                              errorMessage,
                              Optional.empty(),
                              Optional.empty()));
                }
              });
    } else if (error instanceof IllegalStateException) {
      loginAuditService.logLoginAttempt(username, clientIp, false, error.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .<Api<UserInfo>>body(
                  new Api<>(
                      ErrorValidationMessage.FAILURE,
                      error.getMessage(),
                      Optional.empty(),
                      Optional.empty())));
    }

    log.error("Error during login process for user {}: {}", username, error.getMessage());
    loginAuditService.logLoginAttempt(username, clientIp, false, error.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .<Api<UserInfo>>body(
                new Api<>(
                    ErrorValidationMessage.FAILURE,
                    "An unexpected error occurred: " + error.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get current user information",
      description = "Returns information about the currently authenticated user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<UserInfo>>> getCurrentUser() {
    log.debug("Entering method: AuthController.getCurrentUser");

    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap(this::buildUserInfoFromAuthentication)
        .switchIfEmpty(handleUnauthenticated())
        .onErrorResume(this::handleError)
        .doFinally(signal -> log.debug("Exiting method: AuthController.getCurrentUser"));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> buildUserInfoFromAuthentication(Authentication auth) {
    String username = auth.getName();
    AuthenticationDetails details = extractAuthenticationDetails(auth);

    log.debug(
        "Looking up user - Email: {}, Active Role: {}, DB Role: {}, All Roles: {}",
        username,
        details.getActiveRole(),
        details.getDbRole(),
        details.getRoles());

    // Only use DB user for firstName/lastName, but always return roles from JWT (auth)
    return getUserWithFallback(username, details.getActiveRole())
        .map(user -> {
          UserInfo userInfo = UserInfo.builder()
              .username(username)
              .firstName(user.getFirstName())
              .lastName(user.getLastName())
              .activeRole(details.getActiveRole())
              .roles(details.getRoles()) // Always use roles from JWT/authentication
              .token(null) // Do not generate a new token
              .email(username)
              .build();
          return ResponseEntity.ok()
              .body(new Api<>(UserConstants.SUCCESS, UserConstants.USERS_RETRIEVED, Optional.of(userInfo), Optional.empty()));
        })
        .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new Api<>(ErrorValidationMessage.FAILURE, String.format("User not found with email: %s and role: %s", username, details.getActiveRole()), Optional.empty(), Optional.empty()))));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> handleUnauthenticated() {
    return Mono.just(
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                new Api<>(
                    ErrorValidationMessage.FAILURE,
                    "User not authenticated",
                    Optional.empty(),
                    Optional.empty())));
  }

  private Mono<ResponseEntity<Api<UserInfo>>> handleError(Throwable e) {
    log.error("Error getting current user: {}", e.getMessage(), e);
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    ErrorValidationMessage.FAILURE,
                    "Error retrieving user information: " + e.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  @GetMapping(ApiPaths.AUTH_USER_INFO)
  @Operation(summary = "Get User Info", description = "Get user information with JWT token")
  @ApiResponse(
      responseCode = "200",
      description = "User info retrieved successfully",
      content = @Content)
  public Mono<ResponseEntity<Api<UserInfo>>> getUserInfo(Authentication auth) {
    String username = auth.getName();
    AuthenticationDetails details = extractAuthenticationDetails(auth);

    // Generate JTI early for parallel processing
    String jti = redisSessionService.generateJti();

    // Parallel database lookup and JWT token generation
    Mono<com.aci.smart_onboarding.model.User> userMono = getUserWithFallback(username, details.getDbRole());

    Mono<String> tokenMono = Mono.fromCallable(() -> 
        jwtService.generateToken(username, details.getRoles(), details.getActiveRole(), jti));

    // Combine user lookup and token generation in parallel
    return Mono.zip(userMono, tokenMono)
        .flatMap(tuple -> {
          com.aci.smart_onboarding.model.User user = tuple.getT1();
          String token = tuple.getT2();

          // Create session asynchronously (don't block response)
          createSessionAsync(username, jti, details.getActiveRole(), details.getRoles());

          // Build response immediately
          UserInfo userInfo = createUserInfo(user, username, details.getActiveRole(), details.getRoles(), token);

          return Mono.just(
              ResponseEntity.ok()
                  .<Api<UserInfo>>body(
                      new Api<>(
                          UserConstants.SUCCESS,
                          "User information retrieved successfully",
                          Optional.of(userInfo),
                          Optional.empty())));
        })
        .switchIfEmpty(
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Api<UserInfo>>body(
                        new Api<>(
                            ErrorValidationMessage.FAILURE,
                            String.format(
                                "User not found with email: %s and role: %s", username, details.getActiveRole()),
                            Optional.empty(),
                            Optional.empty()))));
  }

  @PostMapping("/request-password-reset")
  @Operation(
      summary = "Request password reset",
      description =
          "Initiates the password reset process by sending a reset link to the provided email address")
  @ApiResponse(
      responseCode = "200",
      description =
          "Password reset request processed. If the email exists, a reset link will be sent.",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  public Mono<ResponseEntity<Api<Void>>> requestPasswordReset(
      @Valid @RequestBody ForgotPasswordRequest request) {
    return forgotPasswordService
        .requestPasswordReset(request.getEmail())
        .onErrorResume(
            e -> {
              log.error("Error processing password reset request: {}", e.getMessage());
              return Mono.empty();
            })
        .then(
            Mono.just(
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            SUCCESS_STATUS,
                            "If an account with that email exists, a password reset link has been sent.",
                            Optional.empty(),
                            Optional.empty()))));
  }

  @PostMapping("/reset-password")
  @Operation(
      summary = "Reset password using token",
      description = "Resets user's password using a valid reset token")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset successful",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or validation error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired token",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<Void>>> resetPassword(
      @RequestParam String token, @Valid @RequestBody ResetPasswordRequest request) {
    return forgotPasswordService
        .resetPassword(token, request)
        .map(this::createResponseEntity)
        .onErrorResume(this::handleResetPasswordError);
  }

  private ResponseEntity<Api<Void>> createResponseEntity(Api<Void> apiResponse) {
    if (SUCCESS_STATUS.equals(apiResponse.getStatus())) {
      return ResponseEntity.ok(apiResponse);
    }
    
    if (INVALID_TOKEN_MESSAGE.equals(apiResponse.getMessage())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
    }
    
    return ResponseEntity.badRequest().body(apiResponse);
  }

  private Mono<ResponseEntity<Api<Void>>> handleResetPasswordError(Throwable e) {
    log.error("Error in reset password endpoint: {}", e.getMessage());
    
    Api<Void> errorResponse = buildErrorResponse(e);
    
    if (isTokenExpiredError(e)) {
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }
    
    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  private Api<Void> buildErrorResponse(Throwable e) {
    return new Api<>(
        "FAILURE",
        e.getMessage(),
        Optional.empty(),
        Optional.of(Map.of("error", e.getMessage())));
  }

  private boolean isTokenExpiredError(Throwable e) {
    return e instanceof BadCredentialsException 
        && e.getMessage() != null 
        && e.getMessage().contains(INVALID_TOKEN_MESSAGE);
  }

  @PostMapping("/logout")
  @Operation(
      summary = "User Logout",
      description = "Invalidate JWT token and log the logout event",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                """
                            {
                                "status": "SUCCESS",
                                "message": "Logout successful",
                                "data": null,
                                "errors": null
                            }
                            """))),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<Void>>> logout(Authentication auth) {
    return authService.logout(auth);
  }

  /**
   * Clear expired cache entries periodically
   */
  @Scheduled(fixedRate = 300000) // Every 5 minutes
  public void clearExpiredCacheEntries() {
    if (userCache.size() > USER_CACHE_SIZE * 0.8) {
      log.debug("Clearing user cache, current size: {}", userCache.size());
      userCache.clear();
    }
  }
}
