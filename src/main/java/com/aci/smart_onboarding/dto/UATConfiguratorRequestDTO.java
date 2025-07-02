package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for UAT Configurator operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UAT Configurator Request")
public class UATConfiguratorRequestDTO {

  @NotNull(message = "Portal type cannot be null")
  @Schema(description = "Type of portal (AGENT/CONSUMER)", example = "AGENT")
  private PortalTypes type;

  @NotBlank(message = "Configuration name cannot be blank")
  @Schema(description = "Name of the configuration", example = "Login Page Configuration")
  private String configurationName;

  @NotNull(message = "Fields cannot be null")
  @Schema(description = "List of field names", example = "[\"loginButton\", \"submitButton\"]")
  private List<String> fields;

  @NotBlank(message = "Position cannot be blank")
  @Schema(description = "Position of the field", example = "top-right")
  private String position;

  @NotBlank(message = "Scenario cannot be blank")
  @Schema(description = "Test scenario description", example = "Verify login button functionality")
  private String scenario;
}
