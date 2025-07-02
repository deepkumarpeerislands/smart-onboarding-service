package com.aci.smart_onboarding.util.brd_form;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChannelWithIndeterminate {
  private String viewValue;
  private boolean selected;
  private boolean indeterminate;
  private List<PaymentChannel> children;
}
