package com.aci.smart_onboarding.util.walletron.subsections;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrontPassField {

  @JsonProperty("isChecked")
  private boolean checked;

  private String labelNumber;
  private String fieldName;
  private String dataLabels;
  private String dataPoints;
  private String formatting;
  private String comments;
}
