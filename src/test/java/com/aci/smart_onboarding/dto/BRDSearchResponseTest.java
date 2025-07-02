package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BRDSearchResponseTest {

  @Test
  void builder_ShouldCreateValidInstance() {
    BRDSearchResponse response =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .brdName("Test BRD")
            .creator("John Doe")
            .type("Type1")
            .status("Draft")
            .notes("Test notes")
            .build();

    assertEquals("form123", response.getBrdFormId());
    assertEquals("BRD123", response.getBrdId());
    assertEquals("Test BRD", response.getBrdName());
    assertEquals("John Doe", response.getCreator());
    assertEquals("Type1", response.getType());
    assertEquals("Draft", response.getStatus());
    assertEquals("Test notes", response.getNotes());
  }

  @Test
  void noArgsConstructor_ShouldCreateEmptyInstance() {
    BRDSearchResponse response = new BRDSearchResponse();

    assertNull(response.getBrdFormId());
    assertNull(response.getBrdId());
    assertNull(response.getBrdName());
    assertNull(response.getCreator());
    assertNull(response.getType());
    assertNull(response.getStatus());
    assertNull(response.getNotes());
  }

  @Test
  void builder_ShouldCreatePopulatedInstance() {
    LocalDateTime now = LocalDateTime.now();
    BRDSearchResponse response =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .customerId("0010g00001imw8xAAA")
            .brdName("Test BRD")
            .creator("John Doe")
            .type("Type1")
            .status("Draft")
            .notes("Test notes")
            .testRigorTimeStamp(now)
            .testRigorFlag(true)
            .build();

    assertEquals("form123", response.getBrdFormId());
    assertEquals("BRD123", response.getBrdId());
    assertEquals("0010g00001imw8xAAA", response.getCustomerId());
    assertEquals("Test BRD", response.getBrdName());
    assertEquals("John Doe", response.getCreator());
    assertEquals("Type1", response.getType());
    assertEquals("Draft", response.getStatus());
    assertEquals("Test notes", response.getNotes());
    assertEquals(now, response.getTestRigorTimeStamp());
    assertEquals(true, response.isTestRigorFlag());
  }

  @Test
  void allArgsConstructor_ShouldCreatePopulatedInstanceWithUatSettings() {
    LocalDateTime now = LocalDateTime.now();
    BRDSearchResponse response =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .customerId("0010g00001imw8xAAA")
            .brdName("Test BRD")
            .creator("John Doe")
            .type("Type1")
            .status("Draft")
            .notes("Test notes")
            .templateFileName("template.xlsx")
            .testRigorTimeStamp(now)
            .testRigorFlag(true)
            .uatSettingsEnabled(true)
            .build();

    assertEquals("form123", response.getBrdFormId());
    assertEquals("BRD123", response.getBrdId());
    assertEquals("0010g00001imw8xAAA", response.getCustomerId());
    assertEquals("Test BRD", response.getBrdName());
    assertEquals("John Doe", response.getCreator());
    assertEquals("Type1", response.getType());
    assertEquals("Draft", response.getStatus());
    assertEquals("Test notes", response.getNotes());
    assertEquals("template.xlsx", response.getTemplateFileName());
    assertEquals(now, response.getTestRigorTimeStamp());
    assertTrue(response.isTestRigorFlag());
    assertTrue(response.isUatSettingsEnabled());
  }

  @Test
  void settersAndGetters_ShouldWorkCorrectly() {
    LocalDateTime now = LocalDateTime.now();
    BRDSearchResponse response = new BRDSearchResponse();

    response.setBrdFormId("form123");
    response.setBrdId("BRD123");
    response.setBrdName("Test BRD");
    response.setCreator("John Doe");
    response.setType("Type1");
    response.setStatus("Draft");
    response.setNotes("Test notes");
    response.setTestRigorTimeStamp(now);
    response.setTestRigorFlag(true);

    assertEquals("form123", response.getBrdFormId());
    assertEquals("BRD123", response.getBrdId());
    assertEquals("Test BRD", response.getBrdName());
    assertEquals("John Doe", response.getCreator());
    assertEquals("Type1", response.getType());
    assertEquals("Draft", response.getStatus());
    assertEquals("Test notes", response.getNotes());
    assertEquals(now, response.getTestRigorTimeStamp());
    assertEquals(true, response.isTestRigorFlag());
  }

  @Test
  void equals_WithSameData_ShouldBeEqual() {
    LocalDateTime now = LocalDateTime.now();
    BRDSearchResponse response1 =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .testRigorTimeStamp(now)
            .testRigorFlag(true)
            .build();

    BRDSearchResponse response2 =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .testRigorTimeStamp(now)
            .testRigorFlag(true)
            .build();

    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void equals_WithDifferentData_ShouldNotBeEqual() {
    LocalDateTime now = LocalDateTime.now();
    BRDSearchResponse response1 =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .testRigorTimeStamp(now)
            .testRigorFlag(true)
            .build();

    BRDSearchResponse response2 =
        BRDSearchResponse.builder()
            .brdFormId("form456")
            .brdId("BRD456")
            .testRigorTimeStamp(now.plusDays(1))
            .testRigorFlag(false)
            .build();

    assertNotEquals(response1, response2);
    assertNotEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void toString_ShouldContainAllFields() {
    BRDSearchResponse response =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId("BRD123")
            .brdName("Test BRD")
            .creator("John Doe")
            .type("Type1")
            .status("Draft")
            .notes("Test notes")
            .build();

    String toString = response.toString();

    assertTrue(toString.contains("brdFormId=form123"));
    assertTrue(toString.contains("brdId=BRD123"));
    assertTrue(toString.contains("brdName=Test BRD"));
    assertTrue(toString.contains("creator=John Doe"));
    assertTrue(toString.contains("type=Type1"));
    assertTrue(toString.contains("status=Draft"));
    assertTrue(toString.contains("notes=Test notes"));
  }

  @Test
  void builder_ShouldAllowNullValues() {
    BRDSearchResponse response =
        BRDSearchResponse.builder()
            .brdFormId("form123")
            .brdId(null)
            .brdName(null)
            .creator(null)
            .type(null)
            .status(null)
            .notes(null)
            .build();

    assertEquals("form123", response.getBrdFormId());
    assertNull(response.getBrdId());
    assertNull(response.getBrdName());
    assertNull(response.getCreator());
    assertNull(response.getType());
    assertNull(response.getStatus());
    assertNull(response.getNotes());
  }
}
