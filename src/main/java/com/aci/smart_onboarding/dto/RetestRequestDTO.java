package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class RetestRequestDTO {
  @NotBlank(message = "BRD ID is required")
  private String brdId;

  @NotEmpty(message = "At least one feature name is required")
  private List<String> featureNames;

  @NotNull(message = "Portal type is required")
  private PortalTypes type;
}
