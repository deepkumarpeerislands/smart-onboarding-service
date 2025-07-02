package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalServerExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Internal error";
    InternalServerException exception = new InternalServerException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getErrorDetails());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldCreateExceptionWithMessageAndErrorDetails() {
    String errorMessage = "Internal error";
    Map<String, String> details = new HashMap<>();
    details.put("key", "value");
    InternalServerException exception = new InternalServerException(errorMessage, details);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(details, exception.getErrorDetails());
  }

  @Test
  void getErrorDetails_ShouldReturnNullIfNotSet() {
    InternalServerException exception = new InternalServerException("error");
    assertNull(exception.getErrorDetails());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    InternalServerException exception = new InternalServerException("test");
    assertTrue(exception instanceof RuntimeException);
  }
}
