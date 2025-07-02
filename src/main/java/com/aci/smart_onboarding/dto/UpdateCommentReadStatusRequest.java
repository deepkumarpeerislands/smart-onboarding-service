package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for updating the read status of a specific comment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentReadStatusRequest {

  @NotBlank(message = "Comment ID cannot be blank")
  private String commentId;

  @NotNull(message = "Read status cannot be null")
  private Boolean isRead;
}
