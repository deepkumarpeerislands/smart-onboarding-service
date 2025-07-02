package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UATTestCaseDTOTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void validate_allFieldsValid_noViolations() {
    UATTestCaseDTO dto =
        UATTestCaseDTO.builder()
            .brdId("BRD-123")
            .testName("Login Test")
            .scenario("Verify login functionality")
            .position("top-right")
            .answer("Login successful")
            .uatType(PortalTypes.AGENT)
            .testType(TestType.NORMAL)
            .status(TestStatus.PASSED)
            .comments("Test passed successfully")
            .featureName("Login Feature")
            .fields(new HashMap<>())
            .build();

    assertTrue(validator.validate(dto).isEmpty());
  }

  @Test
  void validate_blankBrdId_violation() {
    UATTestCaseDTO dto =
        UATTestCaseDTO.builder()
            .brdId("")
            .testName("Login Test")
            .scenario("Verify login functionality")
            .position("top-right")
            .answer("Login successful")
            .uatType(PortalTypes.AGENT)
            .testType(TestType.NORMAL)
            .build();

    assertEquals("BRD ID cannot be blank", validator.validate(dto).iterator().next().getMessage());
  }

  @Test
  void validate_blankTestName_violation() {
    UATTestCaseDTO dto =
        UATTestCaseDTO.builder()
            .brdId("BRD-123")
            .testName("")
            .scenario("Verify login functionality")
            .position("top-right")
            .answer("Login successful")
            .uatType(PortalTypes.AGENT)
            .testType(TestType.NORMAL)
            .build();

    assertEquals(
        "Test name cannot be blank", validator.validate(dto).iterator().next().getMessage());
  }

  @Test
  void validate_blankScenario_violation() {
    UATTestCaseDTO dto =
        UATTestCaseDTO.builder()
            .brdId("BRD-123")
            .testName("Login Test")
            .scenario("")
            .position("top-right")
            .answer("Login successful")
            .uatType(PortalTypes.AGENT)
            .testType(TestType.NORMAL)
            .build();

    assertEquals(
        "Scenario cannot be blank", validator.validate(dto).iterator().next().getMessage());
  }

  @Test
  void validate_nullUatType_violation() {
    UATTestCaseDTO dto =
        UATTestCaseDTO.builder()
            .brdId("BRD-123")
            .testName("Login Test")
            .scenario("Verify login functionality")
            .position("top-right")
            .answer("Login successful")
            .testType(TestType.NORMAL)
            .build();

    assertEquals("UAT type cannot be null", validator.validate(dto).iterator().next().getMessage());
  }
}
