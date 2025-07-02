package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegacyBrdResponse {
  private List<GuidanceData> standardData;
  private List<BrdRules> userRules;
  private List<RulesWithData> combinedRules;
}
