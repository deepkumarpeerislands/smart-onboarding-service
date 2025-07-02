package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for assigning a BA to a BRD. Contains the BA details and the desired status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for assigning a BA to a BRD")
public class AssignBARequest {

  /** Email of the BA to be assigned */
  @NotNull(message = "BA email cannot be null")
  @NotBlank(message = "BA email cannot be empty")
  @Schema(description = "Email of the BA to be assigned", example = "ba@example.com")
  private String baEmail;

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
