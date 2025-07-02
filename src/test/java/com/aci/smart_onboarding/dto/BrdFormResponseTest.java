package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BrdFormResponse Tests")
class BrdFormResponseTest {

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create object with all fields set")
    void builderShouldCreateObjectWithAllFieldsSet() {
      String brdFormId = "BRD-001";
      String status = "DRAFT";
      String projectId = "PRJ-001";
      String brdId = "BRD-ID-001";
      String brdName = "Test BRD";
      String description = "Test Description";
      String organizationId = "ORG-001";
      String creator = "test.user";
      String type = "STANDARD";
      String notes = "Test Notes";
      LocalDateTime createdAt = LocalDateTime.now();
      LocalDateTime updatedAt = LocalDateTime.now();
      String templateType = "DEFAULT";
      String summary = "Test Summary";
      Map<String, Object> sections = new HashMap<>();
      sections.put("testSection", "testValue");

      BrdFormResponse response =
          BrdFormResponse.builder()
              .brdFormId(brdFormId)
              .status(status)
              .projectId(projectId)
              .brdId(brdId)
              .brdName(brdName)
              .description(description)
              .organizationId(organizationId)
              .creator(creator)
              .type(type)
              .notes(notes)
              .createdAt(createdAt)
              .updatedAt(updatedAt)
              .templateType(templateType)
              .summary(summary)
              .sections(sections)
              .build();

      assertEquals(brdFormId, response.getBrdFormId());
      assertEquals(status, response.getStatus());
      assertEquals(projectId, response.getProjectId());
      assertEquals(brdId, response.getBrdId());
      assertEquals(brdName, response.getBrdName());
      assertEquals(description, response.getDescription());
      assertEquals(organizationId, response.getOrganizationId());
      assertEquals(creator, response.getCreator());
      assertEquals(type, response.getType());
      assertEquals(notes, response.getNotes());
      assertEquals(createdAt, response.getCreatedAt());
      assertEquals(updatedAt, response.getUpdatedAt());
      assertEquals(templateType, response.getTemplateType());
      assertEquals(summary, response.getSummary());
      assertEquals(sections, response.getSections());
    }
  }

  @Nested
  @DisplayName("Setter Tests")
  class SetterTests {

    @Test
    @DisplayName("Setters should update fields correctly")
    void settersShouldUpdateFieldsCorrectly() {
      BrdFormResponse response = new BrdFormResponse();
      String brdFormId = "BRD-002";
      String status = "IN_PROGRESS";
      Map<String, Object> sections = new HashMap<>();
      sections.put("newSection", "newValue");

      response.setBrdFormId(brdFormId);
      response.setStatus(status);
      response.setSections(sections);

      assertEquals(brdFormId, response.getBrdFormId());
      assertEquals(status, response.getStatus());
      assertEquals(sections, response.getSections());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("Equal objects should have same hashcode")
    void equalObjectsShouldHaveSameHashcode() {
      LocalDateTime now = LocalDateTime.now();
      BrdFormResponse response1 =
          BrdFormResponse.builder().brdFormId("BRD-001").status("DRAFT").createdAt(now).build();

      BrdFormResponse response2 =
          BrdFormResponse.builder().brdFormId("BRD-001").status("DRAFT").createdAt(now).build();

      assertEquals(response1, response2);
      assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Different objects should not be equal")
    void differentObjectsShouldNotBeEqual() {
      BrdFormResponse response1 =
          BrdFormResponse.builder().brdFormId("BRD-001").status("DRAFT").build();

      BrdFormResponse response2 =
          BrdFormResponse.builder().brdFormId("BRD-002").status("COMPLETED").build();

      assertNotEquals(response1, response2);
      assertNotEquals(response1.hashCode(), response2.hashCode());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("ToString should include all fields")
    void toStringShouldIncludeAllFields() {
      String brdFormId = "BRD-001";
      String status = "DRAFT";

      BrdFormResponse response =
          BrdFormResponse.builder().brdFormId(brdFormId).status(status).build();

      String toString = response.toString();

      assertTrue(toString.contains(brdFormId));
      assertTrue(toString.contains(status));
    }
  }
}
