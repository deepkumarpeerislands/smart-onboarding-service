package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BrdStatusUpdateRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should create request with valid status")
  void createRequest_WithValidStatus_ShouldBeValid() {
    // Arrange & Act
    BrdStatusUpdateRequest request = BrdStatusUpdateRequest.builder().status("Draft").build();

    // Assert
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
    assertEquals("Draft", request.getStatus());
  }

  @ParameterizedTest
  @DisplayName("Should validate all allowed status values")
  @ValueSource(
      strings = {
        "Draft",
        "In Progress",
        "Edit Complete",
        "Internal Review",
        "Reviewed",
        "Ready for Sign-Off",
        "Signed Off",
        "Submit"
      })
  void validStatus_ShouldPassValidation(String validStatus) {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();
    request.setStatus(validStatus);

    // Act
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertTrue(violations.isEmpty(), "Status '" + validStatus + "' should be valid");
  }

  @Test
  @DisplayName("Should fail validation when status is null")
  void nullStatus_ShouldFailValidation() {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();
    request.setStatus(null);

    // Act
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    assertTrue(
        violations.iterator().next().getMessage().contains("blank")
            || violations.iterator().next().getMessage().contains("Blank"),
        "Error message should mention 'blank'");
  }

  @Test
  @DisplayName("Should fail validation when status is empty")
  void emptyStatus_ShouldFailValidation() {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();
    request.setStatus("");

    // Act
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertFalse(violations.isEmpty());
    assertTrue(violations.size() >= 1);
    boolean hasBlankError = false;
    boolean hasPatternError = false;

    for (ConstraintViolation<BrdStatusUpdateRequest> violation : violations) {
      String message = violation.getMessage();
      if (message.contains("blank") || message.contains("Blank")) {
        hasBlankError = true;
      }
      if (message.contains("valid types")) {
        hasPatternError = true;
      }
    }

    assertTrue(
        hasBlankError || hasPatternError,
        "Should have either blank error or pattern validation error");
  }

  @Test
  @DisplayName("Should fail validation when status is blank")
  void blankStatus_ShouldFailValidation() {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();
    request.setStatus("   ");

    // Act
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertFalse(violations.isEmpty());
    assertTrue(violations.size() >= 1);
    boolean hasBlankError = false;
    boolean hasPatternError = false;

    for (ConstraintViolation<BrdStatusUpdateRequest> violation : violations) {
      String message = violation.getMessage();
      if (message.contains("blank") || message.contains("Blank")) {
        hasBlankError = true;
      }
      if (message.contains("valid types")) {
        hasPatternError = true;
      }
    }

    assertTrue(
        hasBlankError || hasPatternError,
        "Should have either blank error or pattern validation error");
  }

  @Test
  @DisplayName("Should fail validation when status is invalid")
  void invalidStatus_ShouldFailValidation() {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();
    request.setStatus("Invalid Status");

    // Act
    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    assertTrue(violations.iterator().next().getMessage().contains("valid types"));
  }

  @Test
  @DisplayName("Builder should create valid object")
  void builder_ShouldCreateValidObject() {
    // Arrange & Act
    BrdStatusUpdateRequest request = BrdStatusUpdateRequest.builder().status("In Progress").build();

    // Assert
    assertEquals("In Progress", request.getStatus());

    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("NoArgsConstructor should create empty object")
  void noArgsConstructor_ShouldCreateEmptyObject() {
    // Arrange & Act
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();

    // Assert
    assertNull(request.getStatus());
  }

  @Test
  @DisplayName("AllArgsConstructor should create valid object")
  void allArgsConstructor_ShouldCreateValidObject() {
    // Arrange & Act
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest("Reviewed", "Status reviewed");

    // Assert
    assertEquals("Reviewed", request.getStatus());

    Set<ConstraintViolation<BrdStatusUpdateRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("Setter should set status correctly")
  void setter_ShouldSetStatusCorrectly() {
    // Arrange
    BrdStatusUpdateRequest request = new BrdStatusUpdateRequest();

    // Act
    request.setStatus("Submit");

    // Assert
    assertEquals("Submit", request.getStatus());
  }

  @Test
  @DisplayName("toString method should include status value")
  void toString_ShouldIncludeStatusValue() {
    // Arrange
    BrdStatusUpdateRequest request =
        new BrdStatusUpdateRequest("Ready for Sign-Off", "status ready for sign off");

    // Act
    String toStringResult = request.toString();

    // Assert
    assertTrue(toStringResult.contains("status=Ready for Sign-Off"));
  }

  @Test
  @DisplayName("equals and hashCode should work correctly")
  void equalsAndHashCode_ShouldWorkCorrectly() {
    // Arrange
    BrdStatusUpdateRequest request1 =
        new BrdStatusUpdateRequest("Signed Off", "status ready for sign off");
    BrdStatusUpdateRequest request2 =
        new BrdStatusUpdateRequest("Signed Off", "status ready for sign off");
    BrdStatusUpdateRequest request3 = new BrdStatusUpdateRequest("Draft", "status in draft");

    // Assert
    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());

    assertNotEquals(request1, request3);
    assertNotEquals(request1.hashCode(), request3.hashCode());
  }
}
