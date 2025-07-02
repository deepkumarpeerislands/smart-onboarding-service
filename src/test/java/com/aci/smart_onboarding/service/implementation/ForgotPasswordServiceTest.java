package com.aci.smart_onboarding.service.implementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.model.PasswordResetToken;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.PasswordResetTokenRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.validator.LoginRequestValidator;
import com.aci.smart_onboarding.service.IEmailService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;


@ExtendWith(MockitoExtension.class)
@DisplayName("Forgot Password Service Tests")
class ForgotPasswordServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordResetTokenRepository tokenRepository;

  @Mock private IEmailService emailService;

  @InjectMocks private ForgotPasswordService forgotPasswordService;

  @Captor private ArgumentCaptor<PasswordResetToken> tokenCaptor;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_USER_ID = "user123";
  private static final String FRONTEND_URL = "http://localhost:3000";
  private static final long TOKEN_EXPIRY_MINUTES = 30;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", FRONTEND_URL);
    ReflectionTestUtils.setField(forgotPasswordService, "tokenExpiryMinutes", TOKEN_EXPIRY_MINUTES);
  }

  @Nested
  @DisplayName("Request Password Reset Tests")
  class RequestPasswordResetTests {

    @Test
    @DisplayName("Should process reset request for existing user")
    void requestPasswordReset_WithExistingUser_ShouldSendEmail() {
      // Given
      User user = new User();
      user.setId(TEST_USER_ID);
      user.setEmail(TEST_EMAIL);

      when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.just(user));
      when(tokenRepository.save(any(PasswordResetToken.class)))
          .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(emailService.sendPasswordResetEmail(anyString(), anyString())).thenReturn(Mono.empty());

      // When
      Mono<Void> result = forgotPasswordService.requestPasswordReset(TEST_EMAIL);

      // Then
      StepVerifier.create(result).verifyComplete();

      verify(tokenRepository).save(tokenCaptor.capture());
      PasswordResetToken savedToken = tokenCaptor.getValue();

      assertNotNull(savedToken.getToken());
      assertEquals(TEST_USER_ID, savedToken.getUserId());
      assertFalse(savedToken.isUsed());
      assertNotNull(savedToken.getExpiryDate());
      assertTrue(
          savedToken
              .getExpiryDate()
              .isAfter(Instant.now().plus(Duration.ofMinutes(TOKEN_EXPIRY_MINUTES - 1))));

      verify(emailService)
          .sendPasswordResetEmail(
              eq(TEST_EMAIL), contains(FRONTEND_URL + "/forgot-password?token="));
    }

    @Test
    @DisplayName("Should handle non-existent user silently")
    void requestPasswordReset_WithNonExistentUser_ShouldCompleteEmpty() {
      // Given
      when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.empty());

      // When
      Mono<Void> result = forgotPasswordService.requestPasswordReset(TEST_EMAIL);

      // Then
      StepVerifier.create(result).verifyComplete();

      verify(tokenRepository, never()).save(any());
      verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle email service errors gracefully")
    void requestPasswordReset_WithEmailError_ShouldCompleteEmpty() {
      // Given
      User user = new User();
      user.setId(TEST_USER_ID);
      user.setEmail(TEST_EMAIL);

      when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.just(user));
      when(tokenRepository.save(any(PasswordResetToken.class)))
          .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(emailService.sendPasswordResetEmail(anyString(), anyString()))
          .thenReturn(Mono.error(new RuntimeException("Email service error")));

      // When
      Mono<Void> result = forgotPasswordService.requestPasswordReset(TEST_EMAIL);

      // Then
      StepVerifier.create(result).verifyComplete();

      verify(tokenRepository).save(any());
      verify(emailService).sendPasswordResetEmail(anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("Token Cleanup Tests")
  class TokenCleanupTests {

    @Test
    @DisplayName("Should clean up expired tokens")
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens() {
      // Given
      when(tokenRepository.deleteByExpiryDateLessThan(any())).thenReturn(Mono.empty());

      // When
      forgotPasswordService.cleanupExpiredTokens();

      // Then
      verify(tokenRepository).deleteByExpiryDateLessThan(any(Instant.class));
    }

    @Test
    @DisplayName("Should handle cleanup errors gracefully")
    void cleanupExpiredTokens_WithError_ShouldHandleGracefully() {
      // Given
      when(tokenRepository.deleteByExpiryDateLessThan(any()))
          .thenReturn(Mono.error(new RuntimeException("Cleanup error")));

      // When & Then
      assertDoesNotThrow(() -> forgotPasswordService.cleanupExpiredTokens());
      verify(tokenRepository).deleteByExpiryDateLessThan(any(Instant.class));
    }
  }

  @Nested
  @DisplayName("Reset Password Tests")
  class ResetPasswordTests {

    @Mock
    LoginRequestValidator loginRequestValidator;
    @Mock
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void injectMocks() {
      ReflectionTestUtils.setField(forgotPasswordService, "loginRequestValidator", loginRequestValidator);
      ReflectionTestUtils.setField(forgotPasswordService, "passwordEncoder", passwordEncoder);
    }

    @Test
    @DisplayName("Should fail if new password and confirm password do not match")
    void resetPassword_PasswordsDoNotMatch_ShouldReturnFailure() {
      var req = new com.aci.smart_onboarding.dto.ResetPasswordRequest("Password1!", "Password2!");
      StepVerifier.create(forgotPasswordService.resetPassword("token123", req))
          .assertNext(api -> {
            assertEquals("failure", api.getStatus());
            assertEquals("New password and confirm password do not match", api.getMessage());
            assertTrue(api.getErrors().isPresent());
            assertEquals("New password and confirm password do not match", api.getErrors().get().get("error"));
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should fail if token is invalid or expired")
    void resetPassword_InvalidOrExpiredToken_ShouldReturnFailure() {
      var req = new com.aci.smart_onboarding.dto.ResetPasswordRequest("Password1!", "Password1!");
      when(tokenRepository.findByTokenAndUsedFalseAndExpiryDateGreaterThan(anyString(), any())).thenReturn(Mono.empty());
      StepVerifier.create(forgotPasswordService.resetPassword("badtoken", req))
          .assertNext(api -> {
            assertEquals("failure", api.getStatus());
            assertEquals("Invalid or expired token", api.getMessage());
            assertTrue(api.getErrors().isPresent());
            assertEquals("Invalid or expired token", api.getErrors().get().get("error"));
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should fail if user not found for valid token")
    void resetPassword_UserNotFound_ShouldReturnFailure() {
      var req = new com.aci.smart_onboarding.dto.ResetPasswordRequest("Password1!", "Password1!");
      var token = com.aci.smart_onboarding.model.PasswordResetToken.builder().userId("userX").token("tokenX").expiryDate(java.time.Instant.now().plusSeconds(1000)).used(false).build();
      when(tokenRepository.findByTokenAndUsedFalseAndExpiryDateGreaterThan(eq("tokenX"), any())).thenReturn(Mono.just(token));
      when(userRepository.findById("userX")).thenReturn(Mono.empty());
      StepVerifier.create(forgotPasswordService.resetPassword("tokenX", req))
          .assertNext(api -> {
            assertEquals("failure", api.getStatus());
            assertEquals("User not found", api.getMessage());
            assertTrue(api.getErrors().isPresent());
            assertEquals("User not found", api.getErrors().get().get("error"));
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should fail if password validation fails")
    void resetPassword_PasswordValidationFails_ShouldReturnFailure() {
      var req = new com.aci.smart_onboarding.dto.ResetPasswordRequest("bad", "bad");
      var token = com.aci.smart_onboarding.model.PasswordResetToken.builder().userId("userY").token("tokenY").expiryDate(java.time.Instant.now().plusSeconds(1000)).used(false).build();
      var user = com.aci.smart_onboarding.model.User.builder().id("userY").build();
      when(tokenRepository.findByTokenAndUsedFalseAndExpiryDateGreaterThan(eq("tokenY"), any())).thenReturn(Mono.just(token));
      when(userRepository.findById("userY")).thenReturn(Mono.just(user));
      doThrow(new org.springframework.security.authentication.BadCredentialsException("Password too short")).when(loginRequestValidator).validatePassword("bad");
      StepVerifier.create(forgotPasswordService.resetPassword("tokenY", req))
          .assertNext(api -> {
            assertEquals("failure", api.getStatus());
            assertEquals("Password too short", api.getMessage());
            assertTrue(api.getErrors().isPresent());
            assertEquals("Password too short", api.getErrors().get().get("error"));
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should reset password successfully and mark token as used")
    void resetPassword_Success_ShouldUpdatePasswordAndToken() {
      var req = new com.aci.smart_onboarding.dto.ResetPasswordRequest("Password1!", "Password1!");
      var token = com.aci.smart_onboarding.model.PasswordResetToken.builder().userId("userZ").token("tokenZ").expiryDate(java.time.Instant.now().plusSeconds(1000)).used(false).build();
      var user = com.aci.smart_onboarding.model.User.builder().id("userZ").activeRole("USER").roles(null).build();
      when(tokenRepository.findByTokenAndUsedFalseAndExpiryDateGreaterThan(eq("tokenZ"), any())).thenReturn(Mono.just(token));
      when(userRepository.findById("userZ")).thenReturn(Mono.just(user));
      doNothing().when(loginRequestValidator).validatePassword("Password1!");
      when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
      when(userRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(tokenRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      StepVerifier.create(forgotPasswordService.resetPassword("tokenZ", req))
          .assertNext(api -> {
            assertEquals("SUCCESS", api.getStatus());
            assertEquals("Password reset successful. Please login with your new password.", api.getMessage());
            assertTrue(api.getErrors().isEmpty());
            assertTrue(api.getData().isEmpty());
          })
          .verifyComplete();
      // Ensure user roles are set if null and activeRole is present
      verify(userRepository).save(argThat(u -> u.getRoles() != null && u.getRoles().contains("USER")));
      // Ensure token is marked as used
      verify(tokenRepository).save(argThat(t -> t.isUsed()));
    }
  }
}
