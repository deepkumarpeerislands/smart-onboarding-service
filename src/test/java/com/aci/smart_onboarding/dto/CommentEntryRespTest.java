package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CommentEntryRespTest {

  @Test
  void testBuilderAndGetters() {
    // Setup test data
    String id = "COMMENT-123";
    String content = "Test comment content";
    String createdBy = "user1";
    String userType = "REVIEWER";
    String parentCommentId = "PARENT-456";
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    LocalDateTime updatedAt = LocalDateTime.now();

    // Create using builder pattern
    CommentEntryResp comment =
        CommentEntryResp.builder()
            .id(id)
            .content(content)
            .createdBy(createdBy)
            .userType(userType)
            .parentCommentId(parentCommentId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

    // Assert all fields were set correctly
    assertEquals(id, comment.getId());
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
    assertEquals(createdAt, comment.getCreatedAt());
    assertEquals(updatedAt, comment.getUpdatedAt());
  }

  @Test
  void testNoArgsConstructorAndSetters() {
    // Setup test data
    String id = "COMMENT-789";
    String content = "Another test comment";
    String createdBy = "user2";
    String userType = "PM";
    String parentCommentId = "PARENT-101";
    LocalDateTime createdAt = LocalDateTime.now().minusHours(5);
    LocalDateTime updatedAt = LocalDateTime.now().minusHours(2);

    // Create using no-args constructor
    CommentEntryResp comment = new CommentEntryResp();

    // Set properties
    comment.setId(id);
    comment.setContent(content);
    comment.setCreatedBy(createdBy);
    comment.setUserType(userType);
    comment.setParentCommentId(parentCommentId);
    comment.setCreatedAt(createdAt);
    comment.setUpdatedAt(updatedAt);

    // Assert all fields were set correctly
    assertEquals(id, comment.getId());
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
    assertEquals(createdAt, comment.getCreatedAt());
    assertEquals(updatedAt, comment.getUpdatedAt());
  }

  @Test
  void testAllArgsConstructor() {
    // Setup test data
    String id = "COMMENT-202";
    String content = "All args test";
    String createdBy = "user3";
    String userType = "ADMIN";
    String parentCommentId = "PARENT-303";
    Boolean isRead = false;
    LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
    LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

    // Create using all-args constructor
    CommentEntryResp comment =
        new CommentEntryResp(
            id, content, createdBy, userType, parentCommentId, isRead, createdAt, updatedAt);

    // Assert all fields were set correctly
    assertEquals(id, comment.getId());
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
    assertEquals(isRead, comment.getIsRead());
    assertEquals(createdAt, comment.getCreatedAt());
    assertEquals(updatedAt, comment.getUpdatedAt());
  }

  @Test
  void testEqualsAndHashCode() {
    // Setup test data
    LocalDateTime timestamp = LocalDateTime.now();

    // Create two identical comments
    CommentEntryResp comment1 =
        CommentEntryResp.builder()
            .id("COMMENT-111")
            .content("Same content")
            .createdBy("user1")
            .userType("REVIEWER")
            .parentCommentId("PARENT-111")
            .createdAt(timestamp)
            .updatedAt(timestamp)
            .build();

    CommentEntryResp comment2 =
        CommentEntryResp.builder()
            .id("COMMENT-111")
            .content("Same content")
            .createdBy("user1")
            .userType("REVIEWER")
            .parentCommentId("PARENT-111")
            .createdAt(timestamp)
            .updatedAt(timestamp)
            .build();

    // Create a different comment
    CommentEntryResp comment3 =
        CommentEntryResp.builder()
            .id("COMMENT-222")
            .content("Different content")
            .createdBy("user2")
            .userType("PM")
            .parentCommentId("PARENT-222")
            .createdAt(timestamp.plusHours(1))
            .updatedAt(timestamp.plusHours(1))
            .build();

    // Test equals and hashCode
    assertEquals(comment1, comment2);
    assertEquals(comment1.hashCode(), comment2.hashCode());
    assertNotEquals(comment1, comment3);
    assertNotEquals(comment1.hashCode(), comment3.hashCode());
  }

  @Test
  void testToString() {
    // Create a comment
    CommentEntryResp comment =
        CommentEntryResp.builder().id("COMMENT-333").content("ToString test").build();

    // Test toString contains important information
    String toString = comment.toString();
    assertTrue(toString.contains("COMMENT-333"));
    assertTrue(toString.contains("ToString test"));
  }
}
