package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.enums.PortalTypes;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortalConfigurationRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void constructor_noArgs_defaultValues() {
    PortalConfigurationRequest request = new PortalConfigurationRequest();
    assertNotNull(request);
    assertNull(request.getBrdId());
    assertNull(request.getUrl());
    assertNull(request.getType());
    assertNull(request.getUsername());
    assertNull(request.getPassword());
  }

  @Test
  void constructor_allArgs_allFieldsSet() {
    PortalConfigurationRequest request =
        new PortalConfigurationRequest(
            "BRD001", "https://test.com", PortalTypes.AGENT, "testUser", "testPass");

    assertEquals("BRD001", request.getBrdId());
    assertEquals("https://test.com", request.getUrl());
    assertEquals(PortalTypes.AGENT, request.getType());
    assertEquals("testUser", request.getUsername());
    assertEquals("testPass", request.getPassword());
  }

  @Test
  void settersAndGetters_validData_fieldsSetCorrectly() {
    PortalConfigurationRequest request = new PortalConfigurationRequest();

    request.setBrdId("BRD001");
    request.setUrl("https://test.com");
    request.setType(PortalTypes.CONSUMER);
    request.setUsername("testUser");
    request.setPassword("testPass");

    assertEquals("BRD001", request.getBrdId());
    assertEquals("https://test.com", request.getUrl());
    assertEquals(PortalTypes.CONSUMER, request.getType());
    assertEquals("testUser", request.getUsername());
    assertEquals("testPass", request.getPassword());
  }

  @Test
  void validate_validData_noViolations() {
    PortalConfigurationRequest request =
        new PortalConfigurationRequest(
            "BRD001", "https://test.com", PortalTypes.AGENT, "testUser", "testPass");

    var violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void validate_invalidData_violations() {
    PortalConfigurationRequest request =
        new PortalConfigurationRequest(
            "", // blank brdId
            "", // blank url
            null, // null type
            "", // blank username
            "" // blank password
            );

    var violations = validator.validate(request);
    assertEquals(5, violations.size());
  }

  @Test
  void equalsAndHashCode_sameAndDifferentObjects_expectedResults() {
    PortalConfigurationRequest request1 =
        new PortalConfigurationRequest(
            "BRD001", "https://test.com", PortalTypes.AGENT, "testUser", "testPass");

    PortalConfigurationRequest request2 =
        new PortalConfigurationRequest(
            "BRD001", "https://test.com", PortalTypes.AGENT, "testUser", "testPass");

    PortalConfigurationRequest request3 =
        new PortalConfigurationRequest(
            "BRD002", "https://test2.com", PortalTypes.CONSUMER, "testUser2", "testPass2");

    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());
    assertNotEquals(request1, request3);
    assertNotEquals(request1.hashCode(), request3.hashCode());
  }

  @Test
  void toString_validData_containsFieldValues() {
    PortalConfigurationRequest request =
        new PortalConfigurationRequest(
            "BRD001", "https://test.com", PortalTypes.AGENT, "testUser", "testPass");

    String toString = request.toString();
    assertTrue(toString.contains("BRD001"));
    assertTrue(toString.contains("https://test.com"));
    assertTrue(toString.contains("AGENT"));
    assertTrue(toString.contains("testUser"));
    assertTrue(toString.contains("testPass"));
  }
}
