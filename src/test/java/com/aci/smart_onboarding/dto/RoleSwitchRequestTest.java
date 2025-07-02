package com.aci.smart_onboarding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoleSwitchRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PM", "BA", "MANAGER", "BILLER"})
  void validate_WhenValidRole_ShouldPassValidation(String role) {
    // Given
    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole(role);

    // When
    var violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  void validate_WhenBlankRole_ShouldFailValidation() {
    // Given
    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole("");

    // When
    var violations = validator.validate(request);

    // Then
    assertThat(violations).hasSize(2);
    assertThat(violations)
        .extracting("message")
        .containsExactlyInAnyOrder(
            "Role cannot be blank", "Role must be either PM, BA, MANAGER, or BILLER");
  }

  @Test
  void validate_WhenNullRole_ShouldFailValidation() {
    // Given
    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole(null);

    // When
    var violations = validator.validate(request);

    // Then
    assertThat(violations).hasSize(1);
    assertThat(violations).extracting("message").containsExactly("Role cannot be blank");
  }

  @Test
  void validate_WhenInvalidRole_ShouldFailValidation() {
    // Given
    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole("INVALID_ROLE");

    // When
    var violations = validator.validate(request);

    // Then
    assertThat(violations).hasSize(1);
    assertThat(violations)
        .extracting("message")
        .containsExactly("Role must be either PM, BA, MANAGER, or BILLER");
  }
}
