package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldCommentThreadsResp {
  private String fieldPath;
  private List<MainCommentWithReplies> comments;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MainCommentWithReplies {
    private BrdCommentResp mainComment;
    private List<BrdCommentResp> replies;
  }
}
