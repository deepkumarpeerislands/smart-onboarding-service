package com.aci.smart_onboarding.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for average time taken for BRDs to move between statuses. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BrdStatusTransitionTimeResponse {

  /** Selected period for the metrics (month, quarter, year). */
  private String period;

  /** Username of the logged-in manager. */
  private String loggedinManager;

  /**
   * For Month period: A single PeriodData object with the monthly averages For Quarter period:
   * Three PeriodData objects, one for each month For Year period: Four PeriodData objects, one for
   * each quarter
   */
  private List<PeriodData> periodData;

  @Builder.Default private List<TrendPoint> trendData = new ArrayList<>();

  /** Represents transition time data for a specific time segment (month or quarter) */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PeriodData {
    /** Label for this period segment (e.g., "Jan 2023", "Q1 2023", etc.) */
    private String label;

    /**
     * Map of transition names to average time in days (rounded to 1 decimal). Example: "Draft ➔ In
     * Progress" -> 2.5 (days)
     */
    private Map<String, Double> averages;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TrendPoint {
    private String label;
    private Double blendedAverage;
  }
}
