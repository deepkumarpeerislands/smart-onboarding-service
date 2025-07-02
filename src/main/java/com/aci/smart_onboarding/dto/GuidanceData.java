package com.aci.smart_onboarding.dto;

import lombok.Data;

@Data
public class GuidanceData {
  private String ruleName;
  private String mappingKey;
  private String similarity;
  private String explanation;
  private String questiondId;
}
