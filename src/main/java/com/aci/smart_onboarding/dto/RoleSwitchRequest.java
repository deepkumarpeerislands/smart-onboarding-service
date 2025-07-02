package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSwitchRequest {
  @NotBlank(message = "Role cannot be blank")
  @Pattern(
      regexp = "^(PM|BA|MANAGER|BILLER)$",
      message = "Role must be either PM, BA, MANAGER, or BILLER")
  private String role; // The role to switch to (with ROLE_ prefix)
}
