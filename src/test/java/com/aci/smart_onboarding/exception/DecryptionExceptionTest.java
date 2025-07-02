package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DecryptionExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Decryption failed";
    DecryptionException exception = new DecryptionException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldCreateExceptionWithMessageAndCause() {
    String errorMessage = "Decryption failed";
    Throwable cause = new RuntimeException("Root cause");
    DecryptionException exception = new DecryptionException(errorMessage, cause);
    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    DecryptionException exception = new DecryptionException("test");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void constructor_ShouldHandleNullMessageAndCause() {
    DecryptionException exception = new DecryptionException(null, null);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }
}
