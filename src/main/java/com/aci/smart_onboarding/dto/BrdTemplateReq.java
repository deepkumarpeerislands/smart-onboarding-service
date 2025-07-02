package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BRD template Request Details")
public class BrdTemplateReq {
  @NotBlank(message = "Template name is required")
  private String templateName;

  @NotEmpty(message = "templateTypes can not null or blank")
  private String templateTypes;

  private String summary;
  private boolean clientInformation;
  private boolean aciInformation;
  private boolean paymentChannels;
  private boolean fundingMethods;
  private boolean achPaymentProcessing;
  private boolean miniAccountMaster;
  private boolean accountIdentifierInformation;
  private boolean paymentRules;
  private boolean notifications;
  private boolean remittance;
  private boolean agentPortal;
  private boolean recurringPayments;
  private boolean ivr;
  private boolean generalImplementations;
  private boolean approvals;
  private boolean revisionHistory;
}
