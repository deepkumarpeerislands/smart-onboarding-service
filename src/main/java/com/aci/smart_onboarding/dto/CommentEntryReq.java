package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
public class CommentEntryReq {
  @NotBlank(message = "Content cannot be blank")
  private String content;

  @NotBlank(message = "Creator cannot be blank")
  private String createdBy;

  @NotBlank(message = "User type cannot be blank")
  private String userType;

  // Optional, only needed for replies
  private String parentCommentId;

  private Boolean isRead;
}
