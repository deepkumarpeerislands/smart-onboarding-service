package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UATAIRequestDTO {

  @NotBlank(message = "BRD ID is required")
  private String brdId;

  @NotEmpty(message = "At least one configuration name is required")
  private List<String> configurationNames;

  @NotNull(message = "UAT type is required")
  private PortalTypes uatType;
}
