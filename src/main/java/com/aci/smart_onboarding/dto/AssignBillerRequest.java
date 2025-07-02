package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a biller to a BRD. Contains the biller details and the desired status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for assigning a biller to a BRD")
public class AssignBillerRequest {

  /** Email of the biller to be assigned */
  @NotNull(message = "Biller email cannot be null")
  @NotBlank(message = "Biller email cannot be empty")
  @Schema(description = "Email of the biller to be assigned", example = "biller@example.com")
  private String billerEmail;

  /** Status to set for the BRD after assignment */
  @NotNull(message = "Status cannot be null")
  @NotBlank(message = "Status cannot be empty")
  @Schema(description = "Status to set for the BRD after assignment", example = "In Progress")
  private String status;

  /** Optional description of the assignment */
  @Schema(
      description = "Optional description of the assignment",
      example = "Assigned for initial review")
  private String description;
}
