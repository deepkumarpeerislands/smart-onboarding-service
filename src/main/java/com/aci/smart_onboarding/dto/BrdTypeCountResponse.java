package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for BRD type count response with counts for multiple time periods */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing counts of BRDs by type for the past 52 weeks")
public class BrdTypeCountResponse {

  @Schema(description = "Scope applied (ME or TEAM)", example = "TEAM")
  private String scope;

  @Schema(description = "Weekly metrics for the past 52 weeks")
  private WeeklyMetrics weeklyMetrics;

  /** Inner class for counts in a specific time period */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "BRD counts for a specific time period")
  public static class PeriodCounts {
    @Schema(description = "Period label", example = "April 2023")
    private String periodLabel;

    @Schema(description = "Count of NEW BRDs", example = "15")
    private int newCount;

    @Schema(description = "Count of UPDATE BRDs", example = "20")
    private int updateCount;

    @Schema(description = "Count of TRIAGE BRDs", example = "7")
    private int triageCount;

    @Schema(description = "Total count of BRDs in this period", example = "42")
    private int totalCount;
  }

  /** Inner class for weekly metrics */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Weekly metrics for the past 52 weeks")
  public static class WeeklyMetrics {
    @Schema(
        description =
            "List of week identifiers (e.g., 'Week 1', 'Week 2', etc.) where Week 1 is the most recent week (last week of previous month)",
        example = "[\"Week 1\", \"Week 2\", \"Week 3\"]")
    private List<String> weeks;

    @Schema(description = "Weekly counts for BRDs by type")
    private WeeklyTypeCounts counts;
  }

  /** Inner class for weekly BRD counts by type */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Weekly counts of BRDs by type")
  public static class WeeklyTypeCounts {
    @Schema(description = "Weekly counts of NEW BRDs", example = "[5, 8, 10, 7, 12]")
    private List<Integer> newCounts;

    @Schema(description = "Weekly counts of UPDATE BRDs", example = "[3, 6, 7, 5, 9]")
    private List<Integer> updateCounts;

    @Schema(description = "Weekly counts of TRIAGE BRDs", example = "[2, 5, 6, 4, 8]")
    private List<Integer> triageCounts;

    @Schema(description = "Weekly total counts of all BRDs", example = "[10, 19, 23, 16, 29]")
    private List<Integer> totalCounts;
  }
}
