package com.aci.smart_onboarding.util.brd_form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonFundingDetails {
  private String tier;
  private String startEndStart;
  private String startEndEnd;
  private String achFlat;
  private String achPercent;
  private String creditFlat;
  private String creditPercent;
  private String sigDebitFlat;
  private String sigDebitPercent;
  private String atmFlat;
  private String atmPercent;
}
