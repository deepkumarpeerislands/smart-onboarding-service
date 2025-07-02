package com.aci.smart_onboarding.exception;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String field, String message) {
    super(String.format("Invalid %s: %s", field, message));
  }
}
