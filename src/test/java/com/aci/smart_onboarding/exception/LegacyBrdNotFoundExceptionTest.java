package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegacyBrdNotFoundExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void constructor_WithMessage_ShouldCreateException() {
    // Arrange
    String errorMessage = "Legacy BRD not found for ID: BRD001";

    // Act
    LegacyBrdNotFoundException exception = new LegacyBrdNotFoundException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Should extend RuntimeException")
  void class_ShouldExtendRuntimeException() {
    // Arrange & Act
    LegacyBrdNotFoundException exception = new LegacyBrdNotFoundException("Test message");

    // Assert
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  @DisplayName("Should preserve empty message")
  void constructor_WithEmptyMessage_ShouldPreserveMessage() {
    // Arrange
    String errorMessage = "";

    // Act
    LegacyBrdNotFoundException exception = new LegacyBrdNotFoundException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }
}
