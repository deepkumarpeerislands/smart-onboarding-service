package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BAAssignmentExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Assignment failed";
    BAAssignmentException exception = new BAAssignmentException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldCreateExceptionWithMessageAndCause() {
    String errorMessage = "Assignment failed";
    Throwable cause = new RuntimeException("Root cause");
    BAAssignmentException exception = new BAAssignmentException(errorMessage, cause);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    BAAssignmentException exception = new BAAssignmentException("test");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void constructor_ShouldHandleNullMessageAndCause() {
    BAAssignmentException exception = new BAAssignmentException(null, null);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }
}
