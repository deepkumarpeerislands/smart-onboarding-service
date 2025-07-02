package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BRD Response List Details")
public class BRDListResponse {
  private String brdId;
  private String brdFormId;
  private String customerId;
  private String brdName;
  private String originalACHFileName;
  private String creator;
  private String type;
  private String notes;
  private String status;

  private String walletronId;

  private String templateFileName;

  @Schema(description = "Timestamp for test rigor")
  private LocalDateTime testRigorTimeStamp;

  @Schema(description = "Flag indicating if test rigor is enabled")
  private boolean testRigorFlag;

  @Schema(description = "Flag indicating if UAT settings are enabled", example = "false")
  private boolean uatSettingsEnabled;
}
