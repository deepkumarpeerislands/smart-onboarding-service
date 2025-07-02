package com.aci.smart_onboarding.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for comments grouped by source type for a BRD. Contains information about both BRD
 * and Site related comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentsBySourceResponse {

  /** Total count of comments across all sources (BRD + Sites) */
  private int totalCount;

  /** Source type for this response (BRD or SITE) */
  private String sourceType;

  /** BRD Form ID */
  private String brdFormId;

  /** Site ID (only applicable if sourceType is SITE) */
  private String siteId;

  /** List of all comment groups for this source */
  private List<BrdFieldCommentGroupResp> comments;

  /** Map of section names to field comment counts for easy navigation */
  private Map<String, Integer> sectionCounts;

  /** Map of section names to their field paths with comments */
  private Map<String, List<String>> sectionFieldPaths;
}
