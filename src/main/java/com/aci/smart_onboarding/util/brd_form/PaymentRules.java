package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRules {
  @JsonProperty("globalLimits")
  private String globalLimits;

  private String minimumPaymentAmount;
  private String maximumPaymentAmount;

  @JsonProperty("futureDatedPayments")
  private String futureDatedPayments;

  @JsonProperty("futureDatedPaymentsDays")
  private String futureDatedPaymentsDays;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
