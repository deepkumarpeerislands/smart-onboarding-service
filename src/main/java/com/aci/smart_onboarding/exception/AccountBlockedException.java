package com.aci.smart_onboarding.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountBlockedException extends RuntimeException {
  public AccountBlockedException(String message) {
    super(message);
  }
}
