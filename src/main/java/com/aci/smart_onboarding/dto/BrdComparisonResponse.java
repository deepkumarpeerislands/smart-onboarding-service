package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.brd_form.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrdComparisonResponse {
  private String brdId;
  private String siteId;
  private String siteName;
  private Map<String, Object> differences;
  private ClientInformation clientInformationDiff;
  private AciInformation aciInformationDiff;
  private PaymentChannels paymentChannelsDiff;
  private FundingMethods fundingMethodsDiff;
  private AchPaymentProcessing achPaymentProcessingDiff;
  private MiniAccountMaster miniAccountMasterDiff;
  private AccountIdentifierInformation accountIdentifierInformationDiff;
  private PaymentRules paymentRulesDiff;
  private Notifications notificationsDiff;
  private Remittance remittanceDiff;
  private AgentPortalConfig agentPortalDiff;
  private RecurringPayments recurringPaymentsDiff;
  private Ivr ivrDiff;
  private GeneralImplementations generalImplementationsDiff;
  private Approvals approvalsDiff;
  private RevisionHistory revisionHistoryDiff;
}
