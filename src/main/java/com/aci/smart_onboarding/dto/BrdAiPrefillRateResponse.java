package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for AI pre-fill rate metrics over time. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrdAiPrefillRateResponse {

  /** Selected period for the metrics (month, quarter, year). */
  private String period;

  /** Username of the logged-in manager. */
  private String loggedinManager;

  /**
   * Data points for each time segment: - Month period: One TimeSegment with the monthly average -
   * Quarter period: Three TimeSegments, one for each month - Year period: Twelve TimeSegments, one
   * for each month
   */
  private List<TimeSegment> timeSegments;

  /** Trend data points for visualizing the overall trend across the selected period. */
  @Builder.Default private List<TrendPoint> trendData = new ArrayList<>();

  /** Represents AI pre-fill rate data for a specific time segment. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TimeSegment {
    /** Label for this time segment (e.g., "Jan 2023") */
    private String label;

    /** Average AI pre-fill rate for this time segment (rounded to 2 decimal places) */
    private Double averagePrefillRate;

    /** Number of BRDs in this time segment */
    private Integer brdCount;

    /** Placeholder for future types of BRDs (new, update, triage) */
    private BrdTypeRates brdTypeRates;
  }

  /** Represents trend data point for visualization. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TrendPoint {
    private String label;
    private Double prefillRate;
  }

  /** Placeholder for future breakdown by BRD types. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class BrdTypeRates {
    private Double newBrdRate;
    private Double updateBrdRate;
    private Double triageBrdRate;
  }
}
