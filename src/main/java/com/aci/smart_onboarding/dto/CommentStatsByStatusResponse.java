package com.aci.smart_onboarding.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for comment statistics by status for a BRD. Contains information about both BRD and
 * Site related comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatsByStatusResponse {

  /** Total count of comments across all sources (BRD + Sites) */
  private int totalCount;

  /** Count of comments specific to the BRD source type */
  private int brdCount;

  /** List of all comment groups for BRD source type */
  private List<BrdFieldCommentGroupResp> brdComments;

  /** Map of site IDs to their comment counts */
  private Map<String, Integer> siteCounts;

  /** Map of site IDs to their lists of comment groups */
  private Map<String, List<BrdFieldCommentGroupResp>> siteComments;
}
