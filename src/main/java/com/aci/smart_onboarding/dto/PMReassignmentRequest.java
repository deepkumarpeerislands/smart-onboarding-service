package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PMReassignmentRequest {

  @NotBlank(message = "BRD ID cannot be empty")
  private String brdId;

  @NotBlank(message = "New PM username cannot be empty")
  @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
  private String newPmUsername;
}
