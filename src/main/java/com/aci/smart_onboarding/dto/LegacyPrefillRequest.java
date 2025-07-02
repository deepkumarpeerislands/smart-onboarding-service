package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for legacy BRD prefill functionality. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyPrefillRequest {

  @NotBlank(message = "BRD ID is required")
  private String brdId;

  @NotBlank(message = "Document name is required")
  private String documentName;
}
