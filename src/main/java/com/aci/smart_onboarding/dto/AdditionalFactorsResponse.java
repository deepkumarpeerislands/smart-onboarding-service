package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for additional factors (Walletron and ACH) statistics. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalFactorsResponse {

  private String scope;
  private String brdScope;
  private String period;
  private String loggedinPm;
  private FactorStats walletron;
  private FactorStats achForm;
  private double averageSites;

  /** Statistics for a specific factor (Yes/No counts and percentages). */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FactorStats {
    private int yesCount;
    private int noCount;
    private double yesPercentage;
    private double noPercentage;
  }
}
