package com.aci.smart_onboarding.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrdCommentResp {
  private String id;
  private String brdId;
  private String fieldPath;
  private Object fieldPathShadowValue;
  private String status;
  private String sectionName;
  private String content;
  private String createdBy;
  private String parentCommentId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
