package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing Business Analyst information including email and display name. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Business Analyst information including email and display name")
public class BAInfo {

  /** BA's email address */
  @NotNull(message = "Email address cannot be null")
  @Schema(description = "BA's email address", example = "ba@example.com")
  private String email;

  /** BA's display name */
  @NotNull(message = "Display name cannot be null")
  @Schema(description = "BA's display name", example = "John Doe - BRD123")
  private String displayName;
}
