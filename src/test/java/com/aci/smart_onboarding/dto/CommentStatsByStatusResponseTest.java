package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentStatsByStatusResponseTest {

  @Test
  @DisplayName("Builder pattern should correctly set all properties")
  void testBuilderPattern() {
    // Arrange
    int totalCount = 10;
    int brdCount = 3;
    List<BrdFieldCommentGroupResp> brdComments = new ArrayList<>();
    Map<String, Integer> siteCounts = new HashMap<>();
    Map<String, List<BrdFieldCommentGroupResp>> siteComments = new HashMap<>();

    // Add some test data
    brdComments.add(new BrdFieldCommentGroupResp());
    siteCounts.put("site1", 4);
    siteCounts.put("site2", 3);
    siteComments.put("site1", Collections.singletonList(new BrdFieldCommentGroupResp()));

    // Act
    CommentStatsByStatusResponse response =
        CommentStatsByStatusResponse.builder()
            .totalCount(totalCount)
            .brdCount(brdCount)
            .brdComments(brdComments)
            .siteCounts(siteCounts)
            .siteComments(siteComments)
            .build();

    // Assert
    assertEquals(totalCount, response.getTotalCount());
    assertEquals(brdCount, response.getBrdCount());
    assertSame(brdComments, response.getBrdComments());
    assertSame(siteCounts, response.getSiteCounts());
    assertSame(siteComments, response.getSiteComments());
  }

  @Test
  @DisplayName("All-args constructor should correctly set all properties")
  void testAllArgsConstructor() {
    // Arrange
    int totalCount = 10;
    int brdCount = 3;
    List<BrdFieldCommentGroupResp> brdComments = new ArrayList<>();
    Map<String, Integer> siteCounts = new HashMap<>();
    Map<String, List<BrdFieldCommentGroupResp>> siteComments = new HashMap<>();

    // Act
    CommentStatsByStatusResponse response =
        new CommentStatsByStatusResponse(
            totalCount, brdCount, brdComments, siteCounts, siteComments);

    // Assert
    assertEquals(totalCount, response.getTotalCount());
    assertEquals(brdCount, response.getBrdCount());
    assertSame(brdComments, response.getBrdComments());
    assertSame(siteCounts, response.getSiteCounts());
    assertSame(siteComments, response.getSiteComments());
  }

  @Test
  @DisplayName("No-args constructor should initialize with default values")
  void testNoArgsConstructor() {
    // Act
    CommentStatsByStatusResponse response = new CommentStatsByStatusResponse();

    // Assert
    assertEquals(0, response.getTotalCount());
    assertEquals(0, response.getBrdCount());
    assertNull(response.getBrdComments());
    assertNull(response.getSiteCounts());
    assertNull(response.getSiteComments());
  }

  @Test
  @DisplayName("Setters should correctly update properties")
  void testSetters() {
    // Arrange
    CommentStatsByStatusResponse response = new CommentStatsByStatusResponse();
    int totalCount = 5;
    int brdCount = 2;
    List<BrdFieldCommentGroupResp> brdComments = new ArrayList<>();
    Map<String, Integer> siteCounts = new HashMap<>();
    Map<String, List<BrdFieldCommentGroupResp>> siteComments = new HashMap<>();

    // Act
    response.setTotalCount(totalCount);
    response.setBrdCount(brdCount);
    response.setBrdComments(brdComments);
    response.setSiteCounts(siteCounts);
    response.setSiteComments(siteComments);

    // Assert
    assertEquals(totalCount, response.getTotalCount());
    assertEquals(brdCount, response.getBrdCount());
    assertSame(brdComments, response.getBrdComments());
    assertSame(siteCounts, response.getSiteCounts());
    assertSame(siteComments, response.getSiteComments());
  }

  @Test
  @DisplayName("equals() and hashCode() should work correctly")
  void testEqualsAndHashCode() {
    // Arrange
    CommentStatsByStatusResponse response1 =
        CommentStatsByStatusResponse.builder()
            .totalCount(10)
            .brdCount(3)
            .brdComments(new ArrayList<>())
            .siteCounts(new HashMap<>())
            .siteComments(new HashMap<>())
            .build();

    CommentStatsByStatusResponse response2 =
        CommentStatsByStatusResponse.builder()
            .totalCount(10)
            .brdCount(3)
            .brdComments(new ArrayList<>())
            .siteCounts(new HashMap<>())
            .siteComments(new HashMap<>())
            .build();

    CommentStatsByStatusResponse response3 =
        CommentStatsByStatusResponse.builder()
            .totalCount(5) // Different value
            .brdCount(3)
            .brdComments(new ArrayList<>())
            .siteCounts(new HashMap<>())
            .siteComments(new HashMap<>())
            .build();

    // Assert
    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());

    assertNotEquals(response1, response3);
    assertNotEquals(response1.hashCode(), response3.hashCode());
  }

  @Test
  @DisplayName("toString() should contain all field names and values")
  void testToString() {
    // Arrange
    CommentStatsByStatusResponse response =
        CommentStatsByStatusResponse.builder().totalCount(10).brdCount(3).build();

    String toString = response.toString();

    // Assert
    assertTrue(toString.contains("totalCount=10"));
    assertTrue(toString.contains("brdCount=3"));
    assertTrue(toString.contains("CommentStatsByStatusResponse"));
  }

  @Test
  @DisplayName("Test data consistency with collections")
  void testCollectionConsistency() {
    // Arrange
    List<BrdFieldCommentGroupResp> brdComments = new ArrayList<>();
    brdComments.add(new BrdFieldCommentGroupResp());
    brdComments.add(new BrdFieldCommentGroupResp());

    Map<String, Integer> siteCounts = new HashMap<>();
    siteCounts.put("site1", 3);
    siteCounts.put("site2", 4);

    Map<String, List<BrdFieldCommentGroupResp>> siteComments = new HashMap<>();
    siteComments.put("site1", Collections.singletonList(new BrdFieldCommentGroupResp()));

    // Act
    CommentStatsByStatusResponse response =
        CommentStatsByStatusResponse.builder()
            .totalCount(9) // 2 BRD + 3 site1 + 4 site2
            .brdCount(2)
            .brdComments(brdComments)
            .siteCounts(siteCounts)
            .siteComments(siteComments)
            .build();

    // Assert
    assertEquals(2, response.getBrdComments().size());
    assertEquals(2, response.getSiteCounts().size());
    assertEquals(1, response.getSiteComments().size());
    assertEquals(9, response.getTotalCount());
    assertEquals(2, response.getBrdCount());
    assertEquals(3, response.getSiteCounts().get("site1").intValue());
  }
}
