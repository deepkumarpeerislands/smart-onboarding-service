package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for bulk site creation operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating multiple sites at once")
public class BulkSiteRequest {

  /** The BRD ID to create sites for */
  @NotBlank(message = "BRD ID cannot be blank")
  @Schema(description = "BRD ID to create sites for", example = "BRD0003")
  private String brdId;

  /** The number of sites to create */
  @Positive(message = "Number of sites must be greater than 0")
  @Max(value = 100, message = "Maximum number of sites is 100")
  @Schema(description = "Number of sites to create (max 100)", example = "5")
  private int numberOfSites;
}
