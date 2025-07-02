package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchPaymentProcessing {
  private String creditToBillerThreshold;
  private String debitToBillerThreshold;
  private String achcompanyName;
  private String achcompanyDiscretionaryData;
  private String achcompanyID;
  private String achcompanyDescription;
  private String webACHDescription;
  private String csrACHDescription;
  private String ivrACHDescription;
  private String batchACHDescription;

  @JsonProperty("fundingNSFValidation")
  private String fundingNSFValidation;

  private String timeFrame;
  private String blockAllPayments;
  private String achReturnResubmission;

  @JsonProperty("resubmissionTimes")
  private String resubmissionTimes;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
