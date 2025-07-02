package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO containing biller email information retrieved from Azure AD. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing biller emails and associated information")
public class BillerEmailsResponse {

  /** List of biller information objects */
  @Valid
  @NotEmpty(message = "Biller emails list cannot be empty")
  @Schema(description = "List of biller email information")
  private List<BillerInfo> billerEmails;
}
