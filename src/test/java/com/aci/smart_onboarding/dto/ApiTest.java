package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void constructor_WithAllParameters_ShouldCreateValidInstance() {
    // Given
    String status = "SUCCESS";
    String message = "Operation completed successfully";
    Optional<String> data = Optional.of("Test Data");
    Map<String, String> errorMap = new HashMap<>();
    errorMap.put("field", "error message");
    Optional<Map<String, String>> errors = Optional.of(errorMap);

    // When
    Api<String> api = new Api<>(status, message, data, errors);

    // Then
    assertEquals(status, api.getStatus());
    assertEquals(message, api.getMessage());
    assertEquals(data, api.getData());
    assertEquals(errors, api.getErrors());
  }

  @Test
  void noArgsConstructor_ShouldCreateInstanceWithDefaultValues() {
    // When
    Api<Object> api = new Api<>();

    // Then
    assertNull(api.getStatus());
    assertNull(api.getMessage());
    assertEquals(Optional.empty(), api.getData());
    assertEquals(Optional.empty(), api.getErrors());
  }

  @Test
  void settersAndGetters_ShouldWorkCorrectly() {
    // Given
    Api<String> api = new Api<>();
    String status = "FAILURE";
    String message = "Operation failed";
    Optional<String> data = Optional.of("Test Data");
    Map<String, String> errorMap = new HashMap<>();
    errorMap.put("field", "error message");
    Optional<Map<String, String>> errors = Optional.of(errorMap);

    // When
    api.setStatus(status);
    api.setMessage(message);
    api.setData(data);
    api.setErrors(errors);

    // Then
    assertEquals(status, api.getStatus());
    assertEquals(message, api.getMessage());
    assertEquals(data, api.getData());
    assertEquals(errors, api.getErrors());
  }

  @Test
  void validation_WhenStatusIsBlank_ShouldHaveViolation() {
    // Given
    Api<String> api = new Api<>("", "message", Optional.empty(), Optional.empty());

    // When
    Set<ConstraintViolation<Api<String>>> violations = validator.validate(api);

    // Then
    assertFalse(violations.isEmpty());
    ConstraintViolation<Api<String>> violation = violations.iterator().next();
    assertEquals("Status cannot be blank", violation.getMessage());
  }

  @Test
  void validation_WhenMessageIsBlank_ShouldHaveViolation() {
    // Given
    Api<String> api = new Api<>("SUCCESS", "", Optional.empty(), Optional.empty());

    // When
    Set<ConstraintViolation<Api<String>>> violations = validator.validate(api);

    // Then
    assertFalse(violations.isEmpty());
    ConstraintViolation<Api<String>> violation = violations.iterator().next();
    assertEquals("Message cannot be blank", violation.getMessage());
  }

  @Test
  void validation_WithValidData_ShouldHaveNoViolations() {
    // Given
    Api<String> api = new Api<>("SUCCESS", "Valid message", Optional.empty(), Optional.empty());

    // When
    Set<ConstraintViolation<Api<String>>> violations = validator.validate(api);

    // Then
    assertTrue(violations.isEmpty());
  }

  @Test
  void equals_WithSameData_ShouldBeEqual() {
    // Given
    Api<String> api1 = new Api<>("SUCCESS", "message", Optional.empty(), Optional.empty());
    Api<String> api2 = new Api<>("SUCCESS", "message", Optional.empty(), Optional.empty());

    // Then
    assertEquals(api1, api2);
    assertEquals(api1.hashCode(), api2.hashCode());
  }

  @Test
  void toString_ShouldContainAllFields() {
    // Given
    Api<String> api = new Api<>("SUCCESS", "message", Optional.empty(), Optional.empty());

    // When
    String toString = api.toString();

    // Then
    assertTrue(toString.contains("status=SUCCESS"));
    assertTrue(toString.contains("message=message"));
    assertTrue(toString.contains("data=Optional.empty"));
    assertTrue(toString.contains("errors=Optional.empty"));
  }
}
