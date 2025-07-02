package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.util.brd_form.*;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.validation.annotation.Validated;

@Document(collection = "brd")
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BRD {

  @Id private String brdFormId;
  private String status;
  private String projectId;
  private String brdId;
  private String brdName;
  private String description;
  private String customerId;
  private String creator;
  private String type;
  private String notes;
  private String baId;
  private String baName;
  private String comments;
  private String industryVertical;

  private String walletronId;

  private String templateFileName;

  @CreatedDate private LocalDateTime createdAt;

  @LastModifiedDate private LocalDateTime updatedAt;
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
  private String originalSSDFileName;
  private String originalACHFileName;
  private String originalContractFileName;
  private String originalOtherFileName;
  private LocalDateTime achUploadedOn;
  private LocalDateTime walletronUploadedOn;
  private Double aiPrefillRate;
  private boolean ssdAvailable;
  private boolean contractAvailable;
  private boolean testRigorFlag;
  private LocalDateTime testRigorTimeStamp;

  private boolean uatSettingsEnabled;
  private boolean userPMRemoved;
  private boolean userBARemoved;

  @SuppressWarnings("unused")
  @AssertTrue(message = "Walletron ID is required when Walletron is included")
  private boolean isWalletronIdValid() {
    return !wallentronIncluded || (walletronId != null && !walletronId.trim().isEmpty());
  }
}
