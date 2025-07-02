package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ResetPasswordRequest;
import com.aci.smart_onboarding.model.PasswordResetToken;
import com.aci.smart_onboarding.repository.PasswordResetTokenRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.validator.LoginRequestValidator;
import com.aci.smart_onboarding.service.IEmailService;
import com.aci.smart_onboarding.service.IForgotPasswordService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService implements IForgotPasswordService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final IEmailService emailService;
  private final PasswordEncoder passwordEncoder;
  private final LoginRequestValidator loginRequestValidator;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @Value("${app.password-reset.token-expiry-minutes:30}")
  private long tokenExpiryMinutes;

  @Override
  public Mono<Void> requestPasswordReset(String email) {
    log.debug("Processing password reset request for email: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            user -> {
              String token = UUID.randomUUID().toString();
              PasswordResetToken resetToken =
                  PasswordResetToken.builder()
                      .userId(user.getId())
                      .token(token)
                      .expiryDate(Instant.now().plus(Duration.ofMinutes(tokenExpiryMinutes)))
                      .used(false)
                      .build();

              return tokenRepository
                  .save(resetToken)
                  .flatMap(
                      savedToken -> {
                        String resetLink =
                            String.format("%s/forgot-password?token=%s", frontendUrl, token);
                        log.debug(resetLink);
                        return emailService.sendPasswordResetEmail(email, resetLink);
                      });
            })
        .then()
        .onErrorResume(
            e -> {
              log.error("Error processing password reset request: {}", e.getMessage());
              return Mono.empty(); // Always return success to prevent user enumeration
            });
  }

  @Override
  public Mono<Api<Void>> resetPassword(String token, ResetPasswordRequest request) {
    log.debug("Processing password reset for token: {}", token);

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      Api<Void> response =
          new Api<>(
              ErrorValidationMessage.FAILURE,
              "New password and confirm password do not match",
              Optional.empty(),
              Optional.of(
                  Map.of(
                      ErrorValidationMessage.ERROR_KEY,
                      "New password and confirm password do not match")));
      return Mono.just(response);
    }

    return tokenRepository
        .findByTokenAndUsedFalseAndExpiryDateGreaterThan(token, Instant.now())
        .flatMap(
            resetToken ->
                userRepository
                    .findById(resetToken.getUserId())
                    .flatMap(
                        user -> {
                          try {
                            // Validate password using existing validation logic
                            loginRequestValidator.validatePassword(request.getNewPassword());
                          } catch (BadCredentialsException e) {
                            Api<Void> response =
                                new Api<>(
                                    ErrorValidationMessage.FAILURE,
                                    e.getMessage(),
                                    Optional.empty(),
                                    Optional.of(
                                        Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())));
                            return Mono.just(response);
                          }

                          // Update password while preserving roles
                          String encodedPassword = passwordEncoder.encode(request.getNewPassword());
                          user.setPassword(encodedPassword.toCharArray());
                          user.setPasswordChangeRequired(false);
                          user.setUpdatedAt(LocalDateTime.now());

                          // Preserve existing roles
                          if (user.getRoles() == null && user.getActiveRole() != null) {
                            user.setRoles(List.of(user.getActiveRole()));
                          }

                          // Mark token as used
                          resetToken.setUsed(true);

                          // Save both user and token
                          return Mono.zip(
                                  userRepository.save(user), tokenRepository.save(resetToken))
                              .thenReturn(
                                  new Api<Void>(
                                      "SUCCESS",
                                      "Password reset successful. Please login with your new password.",
                                      Optional.empty(),
                                      Optional.empty()));
                        })
                    .switchIfEmpty(
                        Mono.just(
                            new Api<Void>(
                                ErrorValidationMessage.FAILURE,
                                "User not found",
                                Optional.empty(),
                                Optional.of(
                                    Map.of(ErrorValidationMessage.ERROR_KEY, "User not found"))))))
        .switchIfEmpty(
            Mono.just(
                new Api<Void>(
                    ErrorValidationMessage.FAILURE,
                    "Invalid or expired token",
                    Optional.empty(),
                    Optional.of(
                        Map.of(ErrorValidationMessage.ERROR_KEY, "Invalid or expired token")))));
  }

  /**
   * Scheduled task to clean up expired tokens. Runs every hour to remove tokens that have expired.
   */
  @Scheduled(fixedRate = 3600000) // Run every hour
  public void cleanupExpiredTokens() {
    log.debug("Running scheduled cleanup of expired password reset tokens");
    tokenRepository
        .deleteByExpiryDateLessThan(Instant.now())
        .subscribe(
            null,
            error -> log.error("Error cleaning up expired tokens: {}", error.getMessage()),
            () -> log.debug("Completed cleanup of expired password reset tokens"));
  }
}
