package com.aci.smart_onboarding.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CustomBrdValidatorTest {

  @Mock private Validator validator;

  private CustomBrdValidator customBrdValidator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    customBrdValidator = new CustomBrdValidator(validator, objectMapper);
  }

  @Test
  void validatePartialUpdateField_WithValidData_ShouldReturnFields() {
    // Given
    Map<String, Object> fields = new HashMap<>();
    fields.put("name", "Test Name");
    fields.put("value", "Test Value");

    when(validator.validate(any(TestDto.class))).thenReturn(new HashSet<>());

    // When
    Mono<Map<String, Object>> result =
        customBrdValidator.validatePartialUpdateField(fields, TestDto.class);

    // Then
    StepVerifier.create(result).expectNext(fields).verifyComplete();
  }

  @Test
  void validatePartialUpdateField_WithInvalidData_ShouldReturnError() {
    // Given
    Map<String, Object> fields = new HashMap<>();
    fields.put("name", "");

    Set<ConstraintViolation<TestDto>> violations = new HashSet<>();
    ConstraintViolation<TestDto> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(new TestPropertyPath("name"));
    when(violation.getMessage()).thenReturn("must not be blank");
    violations.add(violation);

    when(validator.validate(any(TestDto.class))).thenReturn(violations);

    // When
    Mono<Map<String, Object>> result =
        customBrdValidator.validatePartialUpdateField(fields, TestDto.class);

    // Then
    StepVerifier.create(result).expectError(BadRequestException.class).verify();
  }

  @Test
  void validatePartialUpdateField_WithInvalidJson_ShouldReturnError() {
    // Given
    Map<String, Object> fields = new HashMap<>();
    fields.put("invalidField", new Object()); // This will cause JSON serialization to fail

    // When
    Mono<Map<String, Object>> result =
        customBrdValidator.validatePartialUpdateField(fields, TestDto.class);

    // Then
    StepVerifier.create(result).expectError(BadRequestException.class).verify();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "clientInformation",
        "aciInformation",
        "paymentChannels",
        "fundingMethods",
        "achPaymentProcessing",
        "miniAccountMaster",
        "accountIdentifierInformation",
        "paymentRules",
        "notifications",
        "remittance",
        "agentPortal",
        "recurringPayments",
        "ivr",
        "generalImplementations",
        "approvals",
        "revisionHistory"
      })
  void isValidField_WithValidFields_ShouldReturnTrue(String fieldName) {
    assertTrue(customBrdValidator.isValidField(fieldName));
  }

  @Test
  void isValidField_WithInvalidField_ShouldReturnFalse() {
    assertFalse(customBrdValidator.isValidField("invalidField"));
  }

  // Helper classes for testing
  private static class TestDto {
    @NotBlank private String name;
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  private static class TestPropertyPath implements jakarta.validation.Path {
    private final String propertyName;

    TestPropertyPath(String propertyName) {
      this.propertyName = propertyName;
    }

    @Override
    public String toString() {
      return propertyName;
    }

    @Override
    public java.util.Iterator<Node> iterator() {
      return null;
    }
  }
}
