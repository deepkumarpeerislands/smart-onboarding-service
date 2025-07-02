package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveRoleRequest {
  @NotBlank(message = "Role cannot be blank")
  private String role;
}
