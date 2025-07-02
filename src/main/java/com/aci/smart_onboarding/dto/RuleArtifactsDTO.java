package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO to hold a rule name and its associated artifact texts */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleArtifactsDTO {
  private String ruleName;
  private List<String> artifactTexts;
}
