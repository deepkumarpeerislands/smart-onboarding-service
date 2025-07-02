package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO containing Business Analyst email information retrieved from Azure AD. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing BA emails and associated information")
public class BAEmailsResponse {

  /** List of BA information objects */
  @Valid
  @NotEmpty(message = "BA emails list cannot be empty")
  @Schema(description = "List of BA email information")
  private List<BAInfo> baEmails;
}
