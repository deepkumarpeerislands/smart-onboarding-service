package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for BA assignment status check. Contains whether a BA is assigned to a BRD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing BA assignment status")
public class BAAssignmentStatusResponse {

  /** Whether a BA is assigned to the BRD */
  @NotNull(message = "Assignment status cannot be null")
  @Schema(description = "Whether a BA is assigned to the BRD", example = "true")
  private Boolean isAssigned;
} 