package com.aci.smart_onboarding.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when there is an error in biller assignment operations. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BillerAssignmentException extends RuntimeException {

  public BillerAssignmentException(String message) {
    super(message);
  }

  public BillerAssignmentException(String message, Throwable cause) {
    super(message, cause);
  }
}
