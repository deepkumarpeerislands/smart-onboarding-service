package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class BillerAssignmentExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void constructor_WithMessage_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to assign biller: Invalid biller ID";

    // Act
    BillerAssignmentException exception = new BillerAssignmentException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void constructor_WithMessageAndCause_ShouldCreateException() {
    // Arrange
    String errorMessage = "Failed to assign biller: Database error";
    Throwable cause = new RuntimeException("Connection timeout");

    // Act
    BillerAssignmentException exception = new BillerAssignmentException(errorMessage, cause);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals("Connection timeout", exception.getCause().getMessage());
  }

  @Test
  @DisplayName("Should extend RuntimeException")
  void class_ShouldExtendRuntimeException() {
    // Arrange & Act
    BillerAssignmentException exception = new BillerAssignmentException("Test message");

    // Assert
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  @DisplayName("Should have BAD_REQUEST response status")
  void class_ShouldHaveCorrectResponseStatus() {
    // Arrange
    ResponseStatus annotation = BillerAssignmentException.class.getAnnotation(ResponseStatus.class);

    // Assert
    assertNotNull(annotation, "ResponseStatus annotation should be present");
    assertEquals(
        HttpStatus.BAD_REQUEST, annotation.value(), "Response status should be BAD_REQUEST");
  }

  @Test
  @DisplayName("Should preserve empty message")
  void constructor_WithEmptyMessage_ShouldPreserveMessage() {
    // Arrange
    String errorMessage = "";

    // Act
    BillerAssignmentException exception = new BillerAssignmentException(errorMessage);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Should handle null cause")
  void constructor_WithNullCause_ShouldHandleNull() {
    // Arrange
    String errorMessage = "Test message";

    // Act
    BillerAssignmentException exception = new BillerAssignmentException(errorMessage, null);

    // Assert
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }
}
