package com.aci.smart_onboarding.exception;

/** Exception thrown when a JSON template is not found. */
public class TemplateNotFoundException extends RuntimeException {

  public TemplateNotFoundException(String message) {
    super(message);
  }

  public TemplateNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
