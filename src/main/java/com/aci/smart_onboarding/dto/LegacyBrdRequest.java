package com.aci.smart_onboarding.dto;

import lombok.Data;

@Data
public class LegacyBrdRequest {
  private String brdId;
  private String brdRulesFileUrl;
}
