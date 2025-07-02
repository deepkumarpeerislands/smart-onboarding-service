package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlobStorageExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void constructor_WithMessage_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to store blob";

    // Act
    BlobStorageException exception = new BlobStorageException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void constructor_WithMessageAndCause_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to store blob";
    Throwable cause = new RuntimeException("Storage Error");

    // Act
    BlobStorageException exception = new BlobStorageException(errorMessage, cause);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals("Storage Error", exception.getCause().getMessage());
  }
}
