package com.aci.smart_onboarding.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TestStatusTest {

  @Test
  @DisplayName("Should have exactly two enum values")
  void enum_ShouldHaveTwoValues() {
    assertEquals(2, TestStatus.values().length);
  }

  @ParameterizedTest
  @EnumSource(TestStatus.class)
  @DisplayName("Should have non-null name for each enum value")
  void enum_ShouldHaveNonNullName(TestStatus status) {
    assertNotNull(status.name());
  }

  @Test
  @DisplayName("Should have PASSED and FAILED values")
  void enum_ShouldHaveCorrectValues() {
    assertTrue(containsEnumValue(TestStatus.PASSED));
    assertTrue(containsEnumValue(TestStatus.FAILED));
  }

  @Test
  @DisplayName("Should have correct ordinal values")
  void enum_ShouldHaveCorrectOrdinals() {
    assertEquals(0, TestStatus.PASSED.ordinal());
    assertEquals(1, TestStatus.FAILED.ordinal());
  }

  @Test
  @DisplayName("Should convert string to enum value")
  void valueOf_ShouldConvertStringToEnum() {
    assertEquals(TestStatus.PASSED, TestStatus.valueOf("PASSED"));
    assertEquals(TestStatus.FAILED, TestStatus.valueOf("FAILED"));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException for invalid enum value")
  void valueOf_WithInvalidValue_ShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> TestStatus.valueOf("INVALID"));
  }

  private boolean containsEnumValue(TestStatus status) {
    for (TestStatus value : TestStatus.values()) {
      if (value == status) {
        return true;
      }
    }
    return false;
  }
}
