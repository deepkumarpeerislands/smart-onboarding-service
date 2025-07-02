package com.aci.smart_onboarding.constants;

public class ErrorValidationMessage {

  public static final String VALIDATION_ERROR_MESSAGE = "Validation error occurred";
  public static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred: ";
  public static final String UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN =
      "An unexpected error occurred. Please try again later.";
  public static final String ERROR_KEY = "error";
  public static final String FAILURE = "failure";
  public static final String BAD_REQUEST = "Bad Request";
  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String FORBIDDEN = "Forbidden";
  public static final String USER_NOT_FOUND_WITH_EMAIL = "User not found with email: ";
  public static final String BRD_ID_REQUIRED = "BRD ID is required";

  private ErrorValidationMessage() {}
}
