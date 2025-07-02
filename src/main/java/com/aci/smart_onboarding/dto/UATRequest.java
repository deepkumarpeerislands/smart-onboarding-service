package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UAT Request Details")
public class UATRequest {

  @Schema(description = "Type of UAT request", example = "CONSUMER")
  @NotNull(message = "UAT type cannot be null")
  private UATType type;

  @Schema(description = "BRD ID", example = "BRD-1234")
  @NotBlank(message = "BRD ID cannot be blank")
  private String brdId;

  public enum UATType {
    CONSUMER,
    AGENT
  }
}
