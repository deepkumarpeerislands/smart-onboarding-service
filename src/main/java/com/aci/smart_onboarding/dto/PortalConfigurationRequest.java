package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Portal Configuration Request DTO")
public class PortalConfigurationRequest {

  @NotBlank(message = "BRD ID cannot be blank")
  @Schema(description = "BRD ID", example = "BRD001")
  private String brdId;

  @NotBlank(message = "URL cannot be blank")
  @Schema(description = "Portal URL", example = "https://agent-portal.example.com")
  private String url;

  @NotNull(message = "Portal type cannot be null")
  @Schema(description = "Portal type (AGENT/CONSUMER)", example = "AGENT")
  private PortalTypes type;

  @NotBlank(message = "Username cannot be blank")
  @Schema(description = "Portal username", example = "agent_user")
  private String username;

  @NotBlank(message = "Password cannot be blank")
  @Schema(description = "Portal password", example = "agent_password")
  private String password;
}
