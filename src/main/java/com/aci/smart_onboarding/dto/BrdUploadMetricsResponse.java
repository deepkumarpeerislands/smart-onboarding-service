package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for BRD upload metrics response */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing SSD and Contract upload metrics for BRDs")
public class BrdUploadMetricsResponse {

  @Schema(description = "Filter type applied (OPEN or ALL)", example = "ALL")
  private String filterType;

  @Schema(description = "Scope applied (ME or TEAM)", example = "TEAM")
  private String scope;

  @Schema(description = "SSD upload metrics data")
  private UploadMetrics ssdUploads;

  @Schema(description = "Contract upload metrics data")
  private UploadMetrics contractUploads;

  @Schema(description = "Weekly metrics for the past 52 weeks (only populated when filter is ALL)")
  private WeeklyMetrics weeklyMetrics;

  /** Inner class for upload metrics by BRD type */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Upload metrics for BRDs by type")
  public static class UploadMetrics {
    @Schema(description = "NEW BRD metrics")
    private TypeMetrics newBrds;

    @Schema(description = "UPDATE BRD metrics")
    private TypeMetrics updateBrds;
  }

  /** Inner class for metrics for a specific BRD type */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Metrics for a specific BRD type")
  public static class TypeMetrics {
    @Schema(description = "Total count of BRDs of this type", example = "100")
    private int totalCount;

    @Schema(description = "Count of BRDs with file uploaded", example = "70")
    private int uploadedCount;

    @Schema(description = "Percentage of BRDs with file uploaded", example = "70")
    private int uploadedPercentage;

    @Schema(description = "Count of BRDs without file uploaded", example = "30")
    private int notUploadedCount;

    @Schema(description = "Percentage of BRDs without file uploaded", example = "30")
    private int notUploadedPercentage;
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

    @Schema(description = "Weekly counts for NEW BRDs")
    private WeeklyTypeMetrics newBrds;

    @Schema(description = "Weekly counts for UPDATE BRDs")
    private WeeklyTypeMetrics updateBrds;
  }

  /** Inner class for weekly metrics by type */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Weekly metrics for a specific BRD type")
  public static class WeeklyTypeMetrics {
    @Schema(description = "Total count of BRDs created each week", example = "[5, 8, 10, 7, 12]")
    private List<Integer> totalCounts;

    @Schema(description = "Count of BRDs with SSD uploaded each week", example = "[3, 6, 7, 5, 9]")
    private List<Integer> ssdUploadedCounts;

    @Schema(
        description = "Count of BRDs with Contract uploaded each week",
        example = "[2, 5, 6, 4, 8]")
    private List<Integer> contractUploadedCounts;
  }
}
