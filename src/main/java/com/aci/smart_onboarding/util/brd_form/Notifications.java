package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notifications {

  @JsonProperty("sendNotifications")
  private String sendNotifications;

  List<ConsumerNotifications> consumerNotifications;
  private String fromAddress;
  private String shortCode;
  private List<ViewValueAndSelected> shortCodeType;
  private String primaryBrandingColor;
  private String secondaryBrandingColor;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
