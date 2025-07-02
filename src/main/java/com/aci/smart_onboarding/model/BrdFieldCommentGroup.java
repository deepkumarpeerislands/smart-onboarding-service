package com.aci.smart_onboarding.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "brd_field_comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrdFieldCommentGroup {
  @Id private String id;

  // BRD Form ID - required for both BRD and SITE source types
  private String brdFormId;

  // Site ID - only required for SITE source type
  private String siteId;

  // Source type to distinguish between BRD and Site (BRD or SITE)
  private String sourceType;

  // Field path - common for all comments in the group
  private String fieldPath;

  // Shadow value of the field - common for all comments in the group
  private Object fieldPathShadowValue;

  // Status of the field comments - common for all comments in the group
  private String status;

  // Section name - common for all comments in the group
  private String sectionName;

  // Username of the person who created the comment group
  private String createdBy;

  // List of comments for this field
  @Builder.Default private List<CommentEntry> comments = new ArrayList<>();

  // Timestamps
  @CreatedDate private LocalDateTime createdAt;

  @LastModifiedDate private LocalDateTime updatedAt;

  /** Individual comment entry without the redundant field information */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CommentEntry {
    private String id;
    private String content;
    private String createdBy;
    private String userType;
    private String parentCommentId; // Reference to another comment's ID in the same list
    @Builder.Default private Boolean isRead = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}
