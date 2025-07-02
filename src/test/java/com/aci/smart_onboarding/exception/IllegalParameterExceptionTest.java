package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IllegalParameterExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Invalid parameter";
    IllegalParameterException exception = new IllegalParameterException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldCreateExceptionWithMessageAndCause() {
    String errorMessage = "Invalid parameter";
    Throwable cause = new RuntimeException("Root cause");
    IllegalParameterException exception = new IllegalParameterException(errorMessage, cause);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    IllegalParameterException exception = new IllegalParameterException("test");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void constructor_ShouldHandleNullMessageAndCause() {
    IllegalParameterException exception = new IllegalParameterException(null, null);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }
}
