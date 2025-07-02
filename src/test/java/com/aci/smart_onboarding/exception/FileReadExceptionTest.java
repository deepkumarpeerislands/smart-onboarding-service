package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileReadExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void constructor_WithMessage_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to read file";

    // Act
    FileReadException exception = new FileReadException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void constructor_WithMessageAndCause_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to read file";
    Throwable cause = new RuntimeException("IO Error");

    // Act
    FileReadException exception = new FileReadException(errorMessage, cause);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals("IO Error", exception.getCause().getMessage());
  }
}
