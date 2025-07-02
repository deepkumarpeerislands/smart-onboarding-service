package com.aci.smart_onboarding.enums;

/** Enum representing different types of UAT test cases. */
public enum TestType {
  REUSABLE, // Test cases that can be reused across different test suites
  PRE_SUITE, // Test cases that need to be executed before the main test suite
  NORMAL // Regular test cases
}
