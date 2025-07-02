package com.aci.smart_onboarding.exception;

public class SiteAlreadyExistsException extends RuntimeException {
  public SiteAlreadyExistsException(String message) {
    super(message);
  }
}
