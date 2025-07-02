package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for biller assignment operation. Contains the status of the assignment and biller
 * details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing biller assignment details")
public class AssignBillerResponse {

  /** ID of the BRD that was assigned */
  @NotNull(message = "BRD ID cannot be null")
  @Schema(description = "ID of the BRD that was assigned", example = "BRD123")
  private String brdId;

  /** Status of the BRD after biller assignment */
  @NotNull(message = "Status cannot be null")
  @Schema(description = "Status of the BRD after biller assignment", example = "In Progress")
  private String status;

  /** Email of the assigned biller */
  @NotNull(message = "Biller email cannot be null")
  @Schema(description = "Email of the assigned biller", example = "biller@example.com")
  private String billerEmail;

  /** Description of the assignment */
  @Schema(description = "Description of the assignment", example = "Assigned for initial review")
  private String description;
}
