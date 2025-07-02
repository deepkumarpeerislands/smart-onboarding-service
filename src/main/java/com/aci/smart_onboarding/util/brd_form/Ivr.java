package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ivr {
  private String ivrInScope;
  private List<FundingPrompt> fundingPrompts;
  private List<AchSubType> achSubType;
  private String promptForInput;
  private String transferNumber;
  private List<ViewValueAndSelected> supportedLanguages;
  private String interDigitTimeout;
  private String ivrTimeout;
  private String securityKeys;
  private String securityKey1;
  private String securityKey2;
  private String billSummary;
  private String presentAmountDue;
  private String presentDueDate;
  private String presentMinimumAmountDue;
  private String presentBillDate;
  private List<EntryValidationPrompt> entryValidationPrompts;
  private String defaultPaymentAmount;

  @JsonProperty("otherMAMFieldMethod")
  private String otherMAMFieldMethod;

  private String defaultPaymentDate;
  private String paymentInputPromptBehavior;
  private String fundingInputPromptBehavior;
  private String playTermsAndConditions;
  private String ivrNumber1;
  private String dnis1;
  private String ivrNumber2;
  private String dnis2;
  private String ivrNumber3;
  private String dnis3;
  private String ivrNumber4;
  private String dnis4;
  private String ivrNumber5;
  private String dnis5;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
