package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IAuthService;
import com.aci.smart_onboarding.service.RedisSessionService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {
    private final RedisSessionService redisSessionService;
    private final UserRepository userRepository;
    private final IAuditLogService auditLogService;

    @Override
    public Mono<ResponseEntity<Api<Void>>> logout(Authentication auth) {
        if (auth == null) {
            log.warn("Logout attempted with null authentication");
            return Mono.just(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                        new Api<>(
                            ErrorValidationMessage.FAILURE,
                            "User not authenticated",
                            Optional.empty(),
                            Optional.empty())));
        }

        String username = auth.getName();
        String activeRole = null;
        String jti = null;

        try {
            // Safely get the first authority
            if (auth.getAuthorities() != null && auth.getAuthorities().iterator().hasNext()) {
                activeRole = auth.getAuthorities().iterator().next().getAuthority();
            } else {
                log.warn("No authorities found for user: {}", username);
                activeRole = "UNKNOWN";
            }

            // Extract JTI from JWT authentication token
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                jti = jwtAuth.getJti();
                log.debug("Extracted JTI from JWT token: {}", jti);
            } else {
                log.warn("Authentication is not JwtAuthenticationToken for user: {}", username);
            }

            log.info("Logout request received - Username: {}, Active Role: {}, JTI: {}", username, activeRole, jti);

            // Invalidate session if JTI is available
            Mono<Boolean> sessionInvalidation = jti != null
                ? redisSessionService.invalidateSession(username, jti)
                    .doOnSuccess(result -> log.debug("Session invalidation result for user {}: {}", username, result))
                    .doOnError(error -> log.error("Session invalidation failed for user {}: {}", username, error.getMessage()))
                : Mono.just(true);

            // Create audit log entry
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .entityType("User")
                .entityId(username)
                .action("LOGOUT")
                .userId(username)
                .userName(username)
                .userRole(activeRole)
                .eventTimestamp(java.time.LocalDateTime.now())
                .comment("User logged out successfully")
                .build();

            return sessionInvalidation
                .flatMap(invalidated -> {
                    log.debug("Session invalidation completed for user {}: {}", username, invalidated);

                    return auditLogService.logCreation(auditRequest)
                        .doOnSuccess(result -> log.debug("Audit log created successfully for user: {}", username))
                        .doOnError(error -> log.error("Failed to create audit log for logout: {}", error.getMessage()))
                        .onErrorResume(error -> {
                            log.error("Failed to create audit log for logout: {}", error.getMessage());
                            return Mono.empty();
                        })
                        .then(Mono.just(ResponseEntity.ok()
                            .body(new Api<Void>(
                                UserConstants.SUCCESS,
                                SecurityConstants.LOGOUT_SUCCESS,
                                Optional.empty(),
                                Optional.empty()))));
                })
                .onErrorResume(error -> {
                    log.error("Error during logout process for user {}: {}", username, error.getMessage(), error);
                    return Mono.just(ResponseEntity.ok()
                        .body(new Api<Void>(
                            UserConstants.SUCCESS,
                                SecurityConstants.LOGOUT_SUCCESS,
                            Optional.empty(),
                            Optional.empty())));
                });
        } catch (Exception e) {
            log.error("Unexpected error in logout method for user {}: {}", username, e.getMessage(), e);
            return Mono.just(ResponseEntity.ok()
                .body(new Api<Void>(
                    UserConstants.SUCCESS,
                        SecurityConstants.LOGOUT_SUCCESS,
                    Optional.empty(),
                    Optional.empty())));
        }
    }
} 