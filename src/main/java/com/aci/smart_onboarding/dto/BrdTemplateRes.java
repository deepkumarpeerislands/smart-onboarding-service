package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.InstantToFormattedStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BRD template config Response Details")
public class BrdTemplateRes {
  private String id;
  private String templateName;
  private String templateTypes;

  @JsonSerialize(using = InstantToFormattedStringSerializer.class)
  private Instant createdAt;

  @JsonSerialize(using = InstantToFormattedStringSerializer.class)
  private Instant updatedAt;

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
  private int percentage;
}
