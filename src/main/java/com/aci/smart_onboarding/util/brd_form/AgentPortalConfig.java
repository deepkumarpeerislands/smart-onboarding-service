package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPortalConfig {
  private String proxyForMultipleAddresses;
  private String passwordExpiration;
  private String lockoutThreshold;
  private String selectLockoutDuration;
  private String lockoutDuration;
  private String ssoAgentPortal;

  @JsonProperty("redirectURL")
  private List<EnvValue> redirectURL;

  private String metadataFile;
  private String ssoClientCertificate;
  private String idpOrSp;
  private String samlEndPoint;
  private String mfa;

  @JsonProperty("customBillerLogo")
  private String customBillerLogo;

  private String logoFileName;
  private String logoFileType;

  @JsonProperty("fromAddress")
  @Email
  private String fromAddress;

  @JsonProperty("replyToAddress")
  @Email
  private String replyToAddress;

  private String reporting;
  private String paymentSearch;
  private String agentPaymentEntry;
  private String modifyPayment;
  private String cancelPayment;
  private String seriesOfPayments;
  private String usePreviousFunding;
  private String blockPayment;
  private String refundPayment;
  private List<ViewValueAndSelected> refundType;
  private List<ViewValueAndSelected> refundReasons;
  private String feeWaivers;

  @JsonProperty("feeWaiveReasons")
  private List<ViewValueAndSelected> feeWaiveReasons;

  private List<BillSummary> billSummaryItems;
  private List<BillSummary> billSummaryDetail;

  private List<AgentPortal> agentPortal;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
