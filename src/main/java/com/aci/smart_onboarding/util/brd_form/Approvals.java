package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Approvals {
  private String clientSignature;
  private String clientSignatureDate;
  private String aciSignature;
  private String aciSignatureDate;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
