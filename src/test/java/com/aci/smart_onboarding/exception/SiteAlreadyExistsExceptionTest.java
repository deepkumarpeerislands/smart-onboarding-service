package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SiteAlreadyExistsExceptionTest {

  @Test
  void constructor_ShouldCreateExceptionWithMessage() {
    String errorMessage = "Site already exists";
    SiteAlreadyExistsException exception = new SiteAlreadyExistsException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  void exception_ShouldBeRuntimeException() {
    SiteAlreadyExistsException exception = new SiteAlreadyExistsException("test");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void constructor_ShouldHandleNullMessage() {
    SiteAlreadyExistsException exception = new SiteAlreadyExistsException(null);
    assertNull(exception.getMessage());
  }
}
