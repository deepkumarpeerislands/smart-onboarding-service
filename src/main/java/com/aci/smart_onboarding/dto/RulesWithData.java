package com.aci.smart_onboarding.dto;

import lombok.Data;

@Data
public class RulesWithData {
  private String brdId;
  private String brdName;
  private String ruleId;
  private String ruleName;
  private String value;
  private String order;
  private String mappingKey;
  private String explanation;
  private String similarity;
}
