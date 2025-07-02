package com.aci.smart_onboarding.exception;

/** Exception thrown when encryption or decryption operations fail */
public class DecryptionException extends RuntimeException {

  public DecryptionException(String message) {
    super(message);
  }

  public DecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
