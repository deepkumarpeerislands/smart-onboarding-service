package com.aci.smart_onboarding.exception;

/** Exception thrown when Legacy BRD data is not found or not available */
public class LegacyBrdNotFoundException extends RuntimeException {
  public LegacyBrdNotFoundException(String message) {
    super(message);
  }
}
