package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EncryptionExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Encryption failed";
    EncryptionException exception = new EncryptionException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldCreateExceptionWithMessageAndCause() {
    String errorMessage = "Encryption failed";
    Throwable cause = new RuntimeException("Root cause");
    EncryptionException exception = new EncryptionException(errorMessage, cause);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    EncryptionException exception = new EncryptionException("test");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void constructor_ShouldHandleNullMessageAndCause() {
    EncryptionException exception = new EncryptionException(null, null);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }
}
