package com.aci.smart_onboarding.exception;

public class BAAssignmentException extends RuntimeException {
  public BAAssignmentException(String message) {
    super(message);
  }

  public BAAssignmentException(String message, Throwable cause) {
    super(message, cause);
  }
}
