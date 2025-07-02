package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BRDRequestTest {

  private Validator validator;
  private BRDRequest validRequest;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();

    validRequest = new BRDRequest();
    validRequest.setStatus("Draft");
    validRequest.setProjectId("PRJ001");
    validRequest.setBrdId("BRD123");
    validRequest.setBrdName("Test BRD");
    validRequest.setDescription("Test Description");
    validRequest.setCustomerId("0010g00001imw8xAAA");
  }

  @Test
  void whenAllFieldsValid_ShouldHaveNoViolations() {
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Draft", "In Progress", "Biller Review",
        "Internal Review", "Signed Off", "Submit"
      })
  void status_WhenValid_ShouldHaveNoViolations(String status) {
    validRequest.setStatus(status);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Invalid", "DRAFT", "IN_PROGRESS", "draft", "in progress", "random status"})
  @NullAndEmptySource
  void status_WhenInvalid_ShouldHaveViolations(String status) {
    validRequest.setStatus(status);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage().contains("Invalid status value")
                        || v.getMessage().contains("Status is required")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"BRD123", "brd123", "BRD123ABC", "123456"})
  void brdId_WhenValid_ShouldHaveNoViolations(String brdId) {
    validRequest.setBrdId(brdId);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"BRD-123", "BRD_123", "BRD 123", "!@#$%"})
  @NullAndEmptySource
  void brdId_WhenInvalid_ShouldHaveViolations(String brdId) {
    validRequest.setBrdId(brdId);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage().contains("BRD ID must contain only alphanumeric characters")
                        || v.getMessage().contains("BRD ID is required")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Test BRD", "Simple BRD", "Complex Project BRD"})
  void brdName_WhenValid_ShouldHaveNoViolations(String brdName) {
    validRequest.setBrdName(brdName);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Test123", "Test-BRD", "Test_BRD", "Test@BRD"})
  @NullAndEmptySource
  void brdName_WhenInvalid_ShouldHaveViolations(String brdName) {
    validRequest.setBrdName(brdName);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage().contains("BRD name must contain only letters and spaces")
                        || v.getMessage().contains("brdName is required")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0010g00001imw8xAAA", "0010g00001imw8xBBB"})
  void customerId_WhenValid_ShouldHaveNoViolations(String customerId) {
    validRequest.setCustomerId(customerId);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "123", "1234567890123456789"})
  void customerId_WhenInvalid_ShouldHaveViolations(String customerId) {
    validRequest.setCustomerId(customerId);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertFalse(violations.isEmpty());
  }

  @Test
  void description_WhenBlank_ShouldHaveViolation() {
    validRequest.setDescription("");
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().equals("BRD description is required")));
  }

  @Test
  void optionalFields_WhenNull_ShouldHaveNoViolations() {
    validRequest.setCreator(null);
    validRequest.setType(null);
    validRequest.setNotes(null);
    validRequest.setClientInformation(null);
    validRequest.setAciInformation(null);
    validRequest.setPaymentChannels(null);
    validRequest.setFundingMethods(null);
    validRequest.setAchPaymentProcessing(null);
    validRequest.setMiniAccountMaster(null);
    validRequest.setAccountIdentifierInformation(null);
    validRequest.setPaymentRules(null);
    validRequest.setNotifications(null);
    validRequest.setRemittance(null);
    validRequest.setAgentPortal(null);
    validRequest.setRecurringPayments(null);
    validRequest.setIvr(null);
    validRequest.setGeneralImplementations(null);
    validRequest.setApprovals(null);
    validRequest.setRevisionHistory(null);

    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void clientInformation_WhenInvalid_ShouldHaveViolations() {
    ClientInformation clientInfo = new ClientInformation();
    validRequest.setClientInformation(clientInfo);
    Set<ConstraintViolation<BRDRequest>> violations = validator.validate(validRequest);
    assertNotNull(violations);
  }

  @Test
  void equalsAndHashCode_WithSameData_ShouldBeEqual() {
    BRDRequest request1 = new BRDRequest();
    BRDRequest request2 = new BRDRequest();

    request1.setStatus("Draft");
    request1.setBrdId("BRD123");
    request2.setStatus("Draft");
    request2.setBrdId("BRD123");

    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());
  }

  @Test
  void toString_ShouldContainMainFields() {
    String toString = validRequest.toString();
    assertTrue(toString.contains("status=Draft"));
    assertTrue(toString.contains("brdId=BRD123"));
    assertTrue(toString.contains("brdName=Test BRD"));
    assertTrue(toString.contains("description=Test Description"));
    assertTrue(toString.contains("customerId=0010g00001imw8xAAA"));
  }
}
