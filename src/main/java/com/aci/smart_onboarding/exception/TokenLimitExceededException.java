package com.aci.smart_onboarding.exception;

public class TokenLimitExceededException extends RuntimeException {
  public TokenLimitExceededException(String message) {
    super(message);
  }

  public TokenLimitExceededException(String message, Throwable cause) {
    super(message, cause);
  }
}
