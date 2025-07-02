package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for searching UAT test cases by BRD and UAT type. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UAT Test Case Search by BRD and Type Request")
public class UATTestCaseBrdTypeSearchRequest {

  @NotBlank(message = "BRD ID cannot be blank")
  @Schema(description = "BRD ID", example = "BRD-1234")
  private String brdId;

  @NotNull(message = "UAT type cannot be null")
  @Schema(description = "Type of portal (AGENT/CONSUMER)", example = "AGENT")
  private PortalTypes uatType;
}
