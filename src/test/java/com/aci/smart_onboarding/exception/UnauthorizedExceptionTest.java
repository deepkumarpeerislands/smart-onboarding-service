package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UnauthorizedExceptionTest {

  @Test
  void constructor_ShouldSetMessage_WhenMessageProvided() {
    // Given
    String expectedMessage = "User is not authorized to access this resource";

    // When
    UnauthorizedException exception = new UnauthorizedException(expectedMessage);

    // Then
    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldHandleNullMessage() {
    // When
    UnauthorizedException exception = new UnauthorizedException(null);

    // Then
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldHandleEmptyMessage() {
    // Given
    String emptyMessage = "";

    // When
    UnauthorizedException exception = new UnauthorizedException(emptyMessage);

    // Then
    assertEquals(emptyMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructor_ShouldHandleBlankMessage() {
    // Given
    String blankMessage = "   ";

    // When
    UnauthorizedException exception = new UnauthorizedException(blankMessage);

    // Then
    assertEquals(blankMessage, exception.getMessage());
    assertNull(exception.getCause());
  }
}
