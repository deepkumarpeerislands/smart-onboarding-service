package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.serializer.CustomDateSerializer;
import com.aci.smart_onboarding.util.brd_form.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@Schema(description = "BRD Response Details")
public class BRDResponse {
  private String brdFormId;
  private String status;
  private String projectId;
  private String brdId;
  private String brdName;
  private String description;
  private String customerId;
  private String creator;
  private String type;
  private String notes;

  @Schema(description = "Industry vertical for the BRD")
  private String industryVertical;

  private String walletronId;

  @Schema(description = "Template file name for the BRD")
  private String templateFileName;

  private ClientInformation clientInformation;
  private AciInformation aciInformation;
  private PaymentChannels paymentChannels;
  private FundingMethods fundingMethods;
  private AchPaymentProcessing achPaymentProcessing;
  private MiniAccountMaster miniAccountMaster;
  private AccountIdentifierInformation accountIdentifierInformation;
  private PaymentRules paymentRules;
  private Notifications notifications;
  private Remittance remittance;
  private AgentPortalConfig agentPortal;
  private RecurringPayments recurringPayments;
  private Ivr ivr;
  private GeneralImplementations generalImplementations;
  private Approvals approvals;
  private RevisionHistory revisionHistory;
  private boolean wallentronIncluded;
  private boolean achEncrypted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String originalSSDFileName;
  private String originalACHFileName;
  private String originalContractFileName;
  private String originalOtherFileName;

  @Schema(description = "Flag indicating if test rigor is enabled")
  private boolean testRigorFlag;

  @Schema(description = "Timestamp when ACH was uploaded")
  @JsonSerialize(using = CustomDateSerializer.class)
  private LocalDateTime achUploadedOn;

  @Schema(description = "Timestamp when the Walletron file was uploaded", example = "Jan 10, 2025")
  @JsonSerialize(using = CustomDateSerializer.class)
  private LocalDateTime walletronUploadedOn;

  @Schema(description = "AI prefill rate (percentage of fields prefilled by AI)", example = "75.5")
  private Double aiPrefillRate;

  @Schema(description = "Flag indicating if SSD is available", example = "true")
  private boolean ssdAvailable;

  @Schema(description = "Flag indicating if contract is available", example = "true")
  private boolean contractAvailable;

  private LocalDateTime testRigorTimeStamp;

  @Schema(description = "Flag indicating if UAT settings are enabled", example = "false")
  private boolean uatSettingsEnabled;
}
