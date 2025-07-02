package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenLimitExceededExceptionTest {

  @Test
  void constructor_WithMessage_CreatesExceptionWithMessage() {
    // Given
    String expectedMessage = "Token limit exceeded";

    // When
    TokenLimitExceededException exception = new TokenLimitExceededException(expectedMessage);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_WithMessageAndCause_CreatesExceptionWithMessageAndCause() {
    // Given
    String expectedMessage = "Token limit exceeded";
    Throwable cause = new RuntimeException("Underlying cause");

    // When
    TokenLimitExceededException exception = new TokenLimitExceededException(expectedMessage, cause);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
