package com.aci.smart_onboarding.enums;

public enum QrCodeType {
  ONE_TIME("One-time"),
  CONTINUING("Continuing");

  private final String value;

  QrCodeType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
