package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing biller information including email and display name. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Biller information including email and display name")
public class BillerInfo {

  /** Biller's email address */
  @NotNull(message = "Email address cannot be null")
  @Schema(description = "Biller's email address", example = "biller@example.com")
  private String email;

  /** Biller's display name */
  @NotNull(message = "Display name cannot be null")
  @Schema(description = "Biller's display name", example = "John Doe - BRD123")
  private String displayName;
}
