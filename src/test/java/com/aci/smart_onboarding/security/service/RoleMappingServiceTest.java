package com.aci.smart_onboarding.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aci.smart_onboarding.security.config.AzureADConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RoleMappingServiceTest {

  private RoleMappingService roleMappingService;

  @BeforeEach
  void setUp() {
    AzureADConfig azureADConfig = new AzureADConfig();
    azureADConfig.setRolePrefix("AZURE_");
    roleMappingService = new RoleMappingService(azureADConfig);
  }

  @ParameterizedTest
  @MethodSource("provideRoleMappingScenarios")
  void mapAzureRoleToApplicationRole_WithVariousRoles_ShouldMapToCorrectRoles(
      String azureRole, String expectedRole) {
    // When
    String applicationRole = roleMappingService.mapAzureRoleToApplicationRole(azureRole);

    // Then
    assertThat(applicationRole).isEqualTo(expectedRole);
  }

  private static Stream<Arguments> provideRoleMappingScenarios() {
    return Stream.of(
        // Standard role mappings
        Arguments.of("PM", "ROLE_PM"),
        Arguments.of("BA", "ROLE_BA"),
        Arguments.of("BILLER", "ROLE_BILLER"),

        // Default role for unknown roles
        Arguments.of("UNKNOWN_ROLE", "ROLE_USER"),

        // Role prefix handling
        Arguments.of("AZURE_PM", "ROLE_PM"),

        // Empty role handling
        Arguments.of("", "ROLE_USER"),

        // Case sensitivity
        Arguments.of("pm", "ROLE_USER"),

        // Whitespace handling
        Arguments.of("   PM   ", "ROLE_USER"));
  }

  @Test
  void mapAzureRoleToApplicationRole_WithCustomMapping_ShouldReturnMappedRole() {
    // Given
    String azureRole = "CUSTOM_ROLE";
    String expectedRole = "ROLE_CUSTOM";
    roleMappingService.addRoleMapping("CUSTOM_ROLE", expectedRole);

    // When
    String applicationRole = roleMappingService.mapAzureRoleToApplicationRole(azureRole);

    // Then
    assertThat(applicationRole).isEqualTo(expectedRole);
  }

  @Test
  void mapAzureRoleToApplicationRole_WithPrefixedCustomRole_ShouldReturnMappedRole() {
    // Given
    String azureRole = "AZURE_CUSTOM_ROLE";
    String expectedRole = "ROLE_CUSTOM";
    roleMappingService.addRoleMapping("CUSTOM_ROLE", expectedRole);

    // When
    String applicationRole = roleMappingService.mapAzureRoleToApplicationRole(azureRole);

    // Then
    assertThat(applicationRole).isEqualTo(expectedRole);
  }

  @Test
  void addRoleMapping_WithMultipleMappings_ShouldMapAllRolesCorrectly() {
    // Given
    roleMappingService.addRoleMapping("ROLE1", "APP_ROLE1");
    roleMappingService.addRoleMapping("ROLE2", "APP_ROLE2");

    // When
    String applicationRole1 = roleMappingService.mapAzureRoleToApplicationRole("ROLE1");
    String applicationRole2 = roleMappingService.mapAzureRoleToApplicationRole("ROLE2");

    // Then
    assertThat(applicationRole1).isEqualTo("APP_ROLE1");
    assertThat(applicationRole2).isEqualTo("APP_ROLE2");
  }
}
