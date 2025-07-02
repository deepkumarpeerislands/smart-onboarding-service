package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.brd_form.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrdForm {
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
}
