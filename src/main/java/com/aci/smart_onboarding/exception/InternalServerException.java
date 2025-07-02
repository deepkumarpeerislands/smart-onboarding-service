package com.aci.smart_onboarding.exception;

import java.util.Map;

public class InternalServerException extends RuntimeException {
  private final Map<String, String> errorDetails;

  public InternalServerException(String message) {
    super(message);
    this.errorDetails = null;
  }

  public InternalServerException(String message, Map<String, String> errorDetails) {
    super(message);
    this.errorDetails = errorDetails;
  }

  public Map<String, String> getErrorDetails() {
    return errorDetails;
  }
}
