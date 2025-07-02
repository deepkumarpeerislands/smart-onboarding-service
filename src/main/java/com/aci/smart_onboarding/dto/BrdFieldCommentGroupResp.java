package com.aci.smart_onboarding.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrdFieldCommentGroupResp {
  private String id;
  private String brdFormId;
  private String siteId;
  private String sourceType;
  private String fieldPath;
  private Object fieldPathShadowValue;
  private String status;
  private String sectionName;
  private String createdBy;

  @Builder.Default private List<CommentEntryResp> comments = new ArrayList<>();

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
