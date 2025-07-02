package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundingMethods {

  private String itmLeveraged;

  @JsonProperty("processingFeeHandling")
  private String processingFeeHandling;

  private List<BillerAbsorbs> billerAbsorbs;

  @JsonProperty("consumerPays")
  private List<ViewValueAndSelected> consumerPays;

  private List<CommonFundingDetails> agentPortal;
  private List<CommonFundingDetails> ivr;
  private List<CommonFundingDetails> apiSdk;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
