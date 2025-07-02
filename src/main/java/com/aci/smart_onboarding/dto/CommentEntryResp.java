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
public class CommentEntryResp {
  private String id;
  private String content;
  private String createdBy;
  private String userType;
  private String parentCommentId;
  private Boolean isRead;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
