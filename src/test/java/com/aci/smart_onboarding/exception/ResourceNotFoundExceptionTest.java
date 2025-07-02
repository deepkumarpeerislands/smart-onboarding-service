package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResourceNotFoundExceptionTest {

  @Test
  void constructor_WithMessage_ShouldCreateExceptionWithMessage() {
    // Given
    String message = "Resource not found";

    // When
    ResourceNotFoundException exception = new ResourceNotFoundException(message);

    // Then
    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_WithMessageAndCause_ShouldCreateExceptionWithMessageAndCause() {
    // Given
    String message = "Resource not found";
    Throwable cause = new RuntimeException("Original error");

    // When
    ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

    // Then
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
