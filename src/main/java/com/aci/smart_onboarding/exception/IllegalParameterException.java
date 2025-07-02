package com.aci.smart_onboarding.exception;

/** Exception thrown when an illegal or invalid parameter is provided to an API endpoint. */
public class IllegalParameterException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public IllegalParameterException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public IllegalParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
