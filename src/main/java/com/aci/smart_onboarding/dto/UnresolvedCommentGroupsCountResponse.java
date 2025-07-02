package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for unresolved comment groups count response */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description = "Response containing counts of unresolved comment groups for non-submitted BRDs")
public class UnresolvedCommentGroupsCountResponse {

  /** Total count of unresolved comment groups across all BRDs */
  @Schema(description = "Total count of unresolved comment groups across all BRDs", example = "42")
  private int totalCount;

  /** Map of BRD form IDs to their unresolved comment groups counts */
  @Schema(description = "Map of BRD form IDs to their unresolved comment groups details and counts")
  private Map<String, BrdCommentGroupCount> brdCounts;

  /** Inner class for BRD and its unresolved comment groups count */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Information about a BRD and its unresolved comment groups count")
  public static class BrdCommentGroupCount {
    @Schema(description = "BRD identifier", example = "BRD-123")
    private String brdId;

    @Schema(description = "Name of the BRD", example = "New Feature Implementation")
    private String brdName;

    @Schema(description = "Current status of the BRD", example = "Draft")
    private String status;

    @Schema(description = "Count of unresolved comment groups for this BRD", example = "5")
    private int count;
  }
}
