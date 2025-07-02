package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BadRequestExceptionTest {

  @Test
  void constructor_WithMessage_ShouldCreateExceptionWithMessage() {
    // Given
    String errorMessage = "Invalid request parameter";

    // When
    BadRequestException exception = new BadRequestException(errorMessage);

    // Then
    assertEquals(errorMessage, exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception);
  }

  @ParameterizedTest
  @CsvSource({
    "email, Invalid format, Invalid email: Invalid format",
    "age, Must be positive, Invalid age: Must be positive",
    "username, Cannot be empty, Invalid username: Cannot be empty"
  })
  void constructor_WithFieldAndMessage_ShouldCreateFormattedMessage(
      String field, String message, String expectedMessage) {
    // When
    BadRequestException exception = new BadRequestException(field, message);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception);
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    // When
    BadRequestException exception = new BadRequestException("test");

    // Then
    assertTrue(
        exception instanceof RuntimeException, "BadRequestException should be a RuntimeException");
  }
}
