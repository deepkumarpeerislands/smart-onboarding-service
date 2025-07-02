package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentGroupStatsResponse {
  private int totalCommentGroups;
  private int resolvedCommentGroups;
  private PendingCommentStats pendingCommentStats;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PendingCommentStats {
    private int totalPendingGroups;
    private int groupsWithPmComment;
    private int groupsWithoutPmComment;
  }
}
