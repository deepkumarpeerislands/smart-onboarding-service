package com.aci.smart_onboarding.exception;

/**
 * Exception thrown when JSON file validation fails. This exception is used specifically for JSON
 * file upload validation errors.
 */
public class JsonFileValidationException extends RuntimeException {

  public JsonFileValidationException(String message) {
    super(message);
  }

  public JsonFileValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
