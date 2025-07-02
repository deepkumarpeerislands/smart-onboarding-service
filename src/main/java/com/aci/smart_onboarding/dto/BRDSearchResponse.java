package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BRDSearchResponse {
  private String brdFormId;
  private String brdId;
  private String walletronId;
  private String customerId;
  private String brdName;
  private String creator;
  private String type;
  private String status;
  private String notes;
  private String templateFileName;

  @Schema(description = "Timestamp for test rigor")
  private LocalDateTime testRigorTimeStamp;

  @Schema(description = "Flag indicating if test rigor is enabled")
  private boolean testRigorFlag;

  @Schema(description = "Flag indicating if UAT settings are enabled", example = "false")
  private boolean uatSettingsEnabled;
}
