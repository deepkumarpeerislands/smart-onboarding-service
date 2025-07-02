package com.aci.smart_onboarding.security.service;

import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.exception.AccountBlockedException;
import com.aci.smart_onboarding.security.config.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LoginAttemptServiceTest {
  private SecurityConstants securityConstants;
  private LoginAttemptService loginAttemptService;

  @BeforeEach
  void setUp() {
    securityConstants = mock(SecurityConstants.class);
    when(securityConstants.getMaxAttempts()).thenReturn(3);
    when(securityConstants.getBlockDurationSeconds()).thenReturn(60L);
    loginAttemptService = new LoginAttemptService(securityConstants);
  }

  @Test
  void checkBlockedStatus_shouldReturnTrueWhenNotBlocked() {
    StepVerifier.create(loginAttemptService.checkBlockedStatus("user1"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void checkBlockedStatus_shouldThrowWhenBlocked() {
    // Simulate user blocked
    loginAttemptService.loginFailed("user2").block();
    loginAttemptService.loginFailed("user2").block();
    loginAttemptService.loginFailed("user2").onErrorResume(e -> Mono.empty()).block();
    StepVerifier.create(loginAttemptService.checkBlockedStatus("user2"))
        .expectError(AccountBlockedException.class)
        .verify();
  }

  @Test
  void checkBlockedStatus_shouldResetAfterBlockExpires() {
    // Set a very short block duration (100ms)
    when(securityConstants.getBlockDurationSeconds()).thenReturn(0L);
    LoginAttemptService shortBlockService = new LoginAttemptService(securityConstants);

    // Simulate failed attempts
    shortBlockService.loginFailed("user3").block();
    shortBlockService.loginFailed("user3").block();
    shortBlockService.loginFailed("user3").onErrorResume(e -> Mono.empty()).block();

    // Since block duration is 0, it should be immediately unblocked
    StepVerifier.create(shortBlockService.checkBlockedStatus("user3"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void loginSucceeded_shouldClearAttemptsAndBlock() {
    loginAttemptService.loginFailed("user4").block();
    loginAttemptService.loginFailed("user4").block();
    loginAttemptService.loginSucceeded("user4");
    StepVerifier.create(loginAttemptService.checkBlockedStatus("user4"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void loginFailed_shouldIncrementAttemptsAndBlock() {
    StepVerifier.create(loginAttemptService.loginFailed("user5"))
        .expectNext(false)
        .verifyComplete();
    StepVerifier.create(loginAttemptService.loginFailed("user5"))
        .expectNext(false)
        .verifyComplete();
    StepVerifier.create(loginAttemptService.loginFailed("user5"))
        .expectError(AccountBlockedException.class)
        .verify();
  }
}
