package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.brd_form.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BRD Request Details")
@Builder
public class BRDRequest {

  @NotBlank(message = "Status is required")
  @Pattern(
      regexp =
          "^(Draft|In Progress|Biller Review|Internal Review|Reviewed|Ready for Sign-Off|Signed Off|Submit|Edit Complete)$",
      message = "Invalid status value")
  private String status;

  @NotBlank(message = "Project ID is required")
  private String projectId;

  @NotBlank(message = "BRD ID is required")
  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "BRD ID must contain only alphanumeric characters")
  private String brdId;

  @NotBlank(message = "brdName is required")
  @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "BRD name must contain only letters and spaces")
  private String brdName;

  @NotBlank(message = "BRD description is required")
  private String description;

  @NotBlank(message = "Customer ID is required")
  @Pattern(
      regexp = "^[0-9a-zA-Z]{18}$",
      message =
          "Customer ID must be exactly 18 characters long and contain only alphanumeric characters")
  private String customerId;

  private String creator;
  private String type;
  private String notes;

  @Schema(description = "Industry vertical for the BRD")
  private String industryVertical;

  @Schema(description = "Template file name for the BRD")
  private String templateFileName;

  @Valid private ClientInformation clientInformation;
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
  private String originalSSDFileName;
  private String originalACHFileName;
  private String originalContractFileName;
  private String originalOtherFileName;
  private boolean wallentronIncluded;
  private boolean achEncrypted;

  @Schema(description = "AI prefill rate (percentage of fields prefilled by AI)", example = "75.5")
  @Min(value = 0, message = "AI prefill rate cannot be less than 0")
  @Max(value = 100, message = "AI prefill rate cannot be more than 100")
  private Double aiPrefillRate;

  @Schema(description = "Flag indicating if SSD is available", example = "true")
  private boolean ssdAvailable;

  @Schema(description = "Flag indicating if contract is available", example = "true")
  private boolean contractAvailable;

  private boolean testRigorFlag;
  private LocalDateTime testRigorTimeStamp;

  @Schema(description = "Flag indicating if UAT settings are enabled", example = "false")
  private boolean uatSettingsEnabled;
}
