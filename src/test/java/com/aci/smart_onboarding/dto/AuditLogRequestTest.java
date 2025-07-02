package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuditLogRequestTest {

  private Validator validator;
  private LocalDateTime testTimestamp;
  private Map<String, Object> testValues;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
    testTimestamp = LocalDateTime.now();
    testValues = new HashMap<>();
    testValues.put("testKey", "testValue");
  }

  @Test
  void builder_WithValidData_ShouldCreateValidInstance() {
    // When
    AuditLogRequest request =
        AuditLogRequest.builder()
            .entityType("BRD")
            .entityId("123")
            .action("CREATE")
            .userId("user123")
            .userName("John Doe")
            .eventTimestamp(testTimestamp)
            .oldValues(testValues)
            .newValues(testValues)
            .build();

    // Then
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
    assertEquals("BRD", request.getEntityType());
    assertEquals("123", request.getEntityId());
    assertEquals("CREATE", request.getAction());
    assertEquals("user123", request.getUserId());
    assertEquals("John Doe", request.getUserName());
    assertEquals(testTimestamp, request.getEventTimestamp());
    assertEquals(testValues, request.getOldValues());
    assertEquals(testValues, request.getNewValues());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void validation_WhenEntityTypeIsInvalid_ShouldHaveViolation(String entityType) {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setEntityType(entityType);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().equals("Entity type cannot be blank")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void validation_WhenEntityIdIsInvalid_ShouldHaveViolation(String entityId) {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setEntityId(entityId);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().equals("Entity ID cannot be blank")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void validation_WhenActionIsInvalid_ShouldHaveViolation(String action) {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setAction(action);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Action cannot be blank")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void validation_WhenUserIdIsInvalid_ShouldHaveViolation(String userId) {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setUserId(userId);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID cannot be blank")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void validation_WhenUserNameIsInvalid_ShouldHaveViolation(String userName) {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setUserName(userName);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().equals("User name cannot be blank")));
  }

  @Test
  void validation_WhenEventTimestampIsNull_ShouldHaveViolation() {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setEventTimestamp(null);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().equals("Event timestamp cannot be null")));
  }

  @Test
  void optionalFields_ShouldAllowNullValues() {
    // Given
    AuditLogRequest request = createValidRequest();
    request.setOldValues(null);
    request.setNewValues(null);

    // When
    Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty());
    assertNull(request.getOldValues());
    assertNull(request.getNewValues());
  }

  @Test
  void equalsAndHashCode_WithSameData_ShouldBeEqual() {
    // Given
    AuditLogRequest request1 = createValidRequest();
    AuditLogRequest request2 = createValidRequest();

    // Then
    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());
  }

  @Test
  void toString_ShouldContainAllFields() {
    // Given
    AuditLogRequest request = createValidRequest();

    // When
    String toString = request.toString();

    // Then
    assertTrue(toString.contains("entityType=BRD"));
    assertTrue(toString.contains("entityId=123"));
    assertTrue(toString.contains("action=CREATE"));
    assertTrue(toString.contains("userId=user123"));
    assertTrue(toString.contains("userName=John Doe"));
  }

  private AuditLogRequest createValidRequest() {
    return AuditLogRequest.builder()
        .entityType("BRD")
        .entityId("123")
        .action("CREATE")
        .userId("user123")
        .userName("John Doe")
        .eventTimestamp(testTimestamp)
        .oldValues(testValues)
        .newValues(testValues)
        .build();
  }
}
