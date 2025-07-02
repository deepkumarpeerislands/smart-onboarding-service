package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommentEntryReqTest {

  @Test
  void testBuilderAndGetters() {
    // Setup test data
    String content = "Test comment content";
    String createdBy = "user1";
    String userType = "REVIEWER";
    String parentCommentId = "PARENT-456";

    // Create using builder pattern
    CommentEntryReq comment =
        CommentEntryReq.builder()
            .content(content)
            .createdBy(createdBy)
            .userType(userType)
            .parentCommentId(parentCommentId)
            .build();

    // Assert all fields were set correctly
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
  }

  @Test
  void testNoArgsConstructorAndSetters() {
    // Setup test data
    String content = "Another test comment";
    String createdBy = "user2";
    String userType = "PM";
    String parentCommentId = "PARENT-101";

    // Create using no-args constructor
    CommentEntryReq comment = new CommentEntryReq();

    // Set properties
    comment.setContent(content);
    comment.setCreatedBy(createdBy);
    comment.setUserType(userType);
    comment.setParentCommentId(parentCommentId);

    // Assert all fields were set correctly
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
  }

  @Test
  void testAllArgsConstructor() {
    // Setup test data
    String content = "All args test";
    String createdBy = "user3";
    String userType = "ADMIN";
    String parentCommentId = "PARENT-303";
    Boolean isRead = false;

    // Create using all-args constructor
    CommentEntryReq comment =
        new CommentEntryReq(content, createdBy, userType, parentCommentId, isRead);

    // Assert all fields were set correctly
    assertEquals(content, comment.getContent());
    assertEquals(createdBy, comment.getCreatedBy());
    assertEquals(userType, comment.getUserType());
    assertEquals(parentCommentId, comment.getParentCommentId());
    assertEquals(isRead, comment.getIsRead());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create two identical comments
    CommentEntryReq comment1 =
        CommentEntryReq.builder()
            .content("Same content")
            .createdBy("user1")
            .userType("REVIEWER")
            .parentCommentId("PARENT-111")
            .build();

    CommentEntryReq comment2 =
        CommentEntryReq.builder()
            .content("Same content")
            .createdBy("user1")
            .userType("REVIEWER")
            .parentCommentId("PARENT-111")
            .build();

    // Create a different comment
    CommentEntryReq comment3 =
        CommentEntryReq.builder()
            .content("Different content")
            .createdBy("user2")
            .userType("PM")
            .parentCommentId("PARENT-222")
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
    CommentEntryReq comment =
        CommentEntryReq.builder().content("ToString test").createdBy("user4").build();

    // Test toString contains important information
    String toString = comment.toString();
    assertTrue(toString.contains("ToString test"));
    assertTrue(toString.contains("user4"));
  }

  @Test
  void testBuilderWithNullParentCommentId() {
    // Create comment with null parentCommentId
    CommentEntryReq comment =
        CommentEntryReq.builder()
            .content("Content with null parent")
            .createdBy("user5")
            .userType("REVIEWER")
            .parentCommentId(null)
            .build();

    // Assert parentCommentId is null
    assertNull(comment.getParentCommentId());

    // Assert other fields still set correctly
    assertEquals("Content with null parent", comment.getContent());
    assertEquals("user5", comment.getCreatedBy());
    assertEquals("REVIEWER", comment.getUserType());
  }
}
