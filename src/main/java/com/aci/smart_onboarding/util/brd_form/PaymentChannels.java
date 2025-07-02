package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChannels {
  @JsonProperty("paymentChannelsPortal")
  private List<ViewValueAndSelected> paymentChannelsPortal;

  @JsonProperty("paymentChannelsFeatures")
  private List<PaymentChannelFeature> paymentChannelsFeatures;

  @JsonProperty("acceptBankAccounts")
  private String acceptBankAccounts;

  @JsonProperty("acceptBankAccountsChannels")
  private List<PaymentChannelFeature> acceptBankAccountsChannels;

  @JsonProperty("acceptCreditDebitCards")
  private String acceptCreditDebitCards;

  @JsonProperty("acceptCreditDebitCardsChannels")
  private List<PaymentChannelFeature> acceptCreditDebitCardsChannels;

  @JsonProperty("acceptAlternativePaymentMethods")
  private String acceptAlternativePaymentMethods;

  @JsonProperty("acceptAlternativePaymentMethodsChannels")
  private List<PaymentChannelFeature> acceptAlternativePaymentMethodsChannels;

  @JsonProperty("cvvValidation")
  private String cvvValidation;

  @JsonProperty("cvvValidationApiSdk")
  private String cvvValidationApiSdk;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
