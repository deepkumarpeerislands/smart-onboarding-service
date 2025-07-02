package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

  @NotBlank(message = "New password is required")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  @Pattern(
      regexp = ".*\\d.*[@#$%^&+=!_].*|.*[@#$%^&+=!_].*\\d.*",
      message = "Password must contain at least one number and one special character (@#$%^&+=!_)")
  private String newPassword;

  @NotBlank(message = "Confirm password is required")
  private String confirmPassword;
}
