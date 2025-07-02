package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrdSnapshotMetricsResponse {
  private String scope;
  private SnapshotMetrics snapshotMetrics;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SnapshotMetrics {
    private int totalBrds;
    private int openBrds;
    private int walletronEnabledBrds;
  }
}
