package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for average comment resolution time. Provides data for all three time periods in a
 * single response: - Month: single figure (current month) - Quarter: trend line across months in
 * current quarter - Year: trend line across all 12 months of the past year
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResolutionTimeResponse {
  // Month period - single value in days (can include decimals)
  private MonthPeriod monthPeriod;

  // Quarter period - trend data across months in the quarter
  private PeriodTrend quarterPeriod;

  // Year period - trend data across all 12 months of the past year
  private PeriodTrend yearPeriod;

  /** Single value for month period */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthPeriod {
    // Month name (e.g., "January 2023")
    private String month;

    // Average resolution time in days for this month
    private Double averageResolutionDays;

    // Count of resolved comments in this month
    private Integer resolvedCommentsCount;
  }

  /** Trend data for quarter and year periods */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PeriodTrend {
    // List of monthly data points
    private List<MonthlyDataPoint> monthlyData;
  }

  /** Monthly data point for trend lines */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyDataPoint {
    // Month name (e.g., "January", "February")
    private String month;

    // Average resolution time in days for this month
    private Double averageResolutionDays;

    // Count of resolved comments in this month (useful for context)
    private Integer resolvedCommentsCount;
  }
}
