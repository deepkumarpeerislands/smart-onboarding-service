package com.aci.smart_onboarding.util.brd_form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerNotifications {
  private String name;
  private String purpose;
  private String purposeInput;
  private Object optOut;
  private Object email;
  private Object sms;
}
