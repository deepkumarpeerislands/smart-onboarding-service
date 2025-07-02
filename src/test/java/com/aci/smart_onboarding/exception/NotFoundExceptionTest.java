package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotFoundExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    // Given
    String errorMessage = "Resource not found";

    // When
    NotFoundException exception = new NotFoundException(errorMessage);

    // Then
    assertEquals(errorMessage, exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "User not found with id: 123",
        "Document not found",
        "Resource not found with key: ABC"
      })
  void constructor_ShouldHandleVariousMessages(String message) {
    // When
    NotFoundException exception = new NotFoundException(message);

    // Then
    assertEquals(message, exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception);
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    // When
    NotFoundException exception = new NotFoundException("test");

    // Then
    assertTrue(
        exception instanceof RuntimeException, "NotFoundException should be a RuntimeException");
  }
}
