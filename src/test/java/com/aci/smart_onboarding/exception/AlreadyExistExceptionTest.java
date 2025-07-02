package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AlreadyExistExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    // Given
    String errorMessage = "Resource already exists";

    // When
    AlreadyExistException exception = new AlreadyExistException(errorMessage);

    // Then
    assertEquals(errorMessage, exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception);
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    // When
    AlreadyExistException exception = new AlreadyExistException("test");

    // Then
    assertTrue(
        exception instanceof RuntimeException,
        "AlreadyExistException should be a RuntimeException");
  }
}
