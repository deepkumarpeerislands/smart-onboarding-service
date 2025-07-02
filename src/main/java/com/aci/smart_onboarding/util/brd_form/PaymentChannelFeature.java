package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChannelFeature {
  @JsonProperty("viewValue")
  private String viewValue;

  @JsonProperty("selected")
  private boolean selected;

  @JsonProperty("indeterminate")
  private boolean indeterminate;

  @JsonProperty("children")
  private List<ViewValueAndSelected> children;
}
