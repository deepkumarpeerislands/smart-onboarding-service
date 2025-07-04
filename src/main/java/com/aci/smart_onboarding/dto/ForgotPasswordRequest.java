package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String email;
}
