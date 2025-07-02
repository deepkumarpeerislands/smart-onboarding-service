package com.aci.smart_onboarding.constants;

public class FileConstants {
  public static final String FILE_NOT_FOUND = "File not found: ";
  public static final String FILE_EMPTY = "EMPTY";
  public static final String FILE_CHAR_SEQUENCE = "%02X";
  public static final long MAX_IMAGE_FILE_SIZE = 1024L * 1024; // 1 MB in bytes
  public static final int SAS_TOKEN_EXPIRY_YEARS = 2; // Configurable expiry time in years

  private FileConstants() {}
}
