package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringPayments {
  private List<ViewValueAndSelected> paymentDateOptions;
  private List<ViewValueAndSelected> paymentAmountOptions;
  private List<ViewValueAndSelected> durationOptions;
  private String recurringPaymentThreshold;
  private String recurringPaymentThresholdValue;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
