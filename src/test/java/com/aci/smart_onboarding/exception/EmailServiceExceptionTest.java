package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailServiceExceptionTest {

  @Test
  void constructor_ShouldSetMessage_WhenMessageProvided() {
    // Given
    String expectedMessage = "Test error message";

    // When
    EmailServiceException exception = new EmailServiceException(expectedMessage);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldSetMessageAndCause_WhenBothProvided() {
    // Given
    String expectedMessage = "Test error message";
    Throwable expectedCause = new RuntimeException("Root cause");

    // When
    EmailServiceException exception = new EmailServiceException(expectedMessage, expectedCause);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(expectedCause, exception.getCause());
  }

  @Test
  void constructor_ShouldHandleNullMessage() {
    // When
    EmailServiceException exception = new EmailServiceException(null);

    // Then
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldHandleNullMessageWithCause() {
    // Given
    Throwable expectedCause = new RuntimeException("Root cause");

    // When
    EmailServiceException exception = new EmailServiceException(null, expectedCause);

    // Then
    assertNull(exception.getMessage());
    assertEquals(expectedCause, exception.getCause());
  }

  @Test
  void constructor_ShouldHandleNullCause() {
    // Given
    String expectedMessage = "Test error message";

    // When
    EmailServiceException exception = new EmailServiceException(expectedMessage, null);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }
}
