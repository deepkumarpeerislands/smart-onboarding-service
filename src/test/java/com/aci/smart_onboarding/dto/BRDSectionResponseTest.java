package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BRDSectionResponseTest {

  @Test
  void noArgsConstructor_ShouldCreateEmptyInstance() {
    BRDSectionResponse<String> response = new BRDSectionResponse<>();

    assertNull(response.getBrdFormId());
    assertNull(response.getBrdId());
    assertNull(response.getSectionName());
    assertNull(response.getSectionData());
  }

  @Test
  void allArgsConstructor_WithStringData_ShouldCreateValidInstance() {
    String brdFormId = "form123";
    String brdId = "BRD123";
    String sectionName = "TestSection";
    String sectionData = "Test Data";

    BRDSectionResponse<String> response =
        new BRDSectionResponse<>(brdFormId, brdId, sectionName, sectionData);

    assertEquals(brdFormId, response.getBrdFormId());
    assertEquals(brdId, response.getBrdId());
    assertEquals(sectionName, response.getSectionName());
    assertEquals(sectionData, response.getSectionData());
  }

  @Test
  void allArgsConstructor_WithMapData_ShouldCreateValidInstance() {
    String brdFormId = "form123";
    String brdId = "BRD123";
    String sectionName = "TestSection";
    Map<String, Object> sectionData = new HashMap<>();
    sectionData.put("key1", "value1");
    sectionData.put("key2", 123);

    BRDSectionResponse<Map<String, Object>> response =
        new BRDSectionResponse<>(brdFormId, brdId, sectionName, sectionData);

    assertEquals(brdFormId, response.getBrdFormId());
    assertEquals(brdId, response.getBrdId());
    assertEquals(sectionName, response.getSectionName());
    assertEquals(sectionData, response.getSectionData());
    assertEquals("value1", response.getSectionData().get("key1"));
    assertEquals(123, response.getSectionData().get("key2"));
  }

  @Test
  void settersAndGetters_ShouldWorkCorrectly() {
    BRDSectionResponse<String> response = new BRDSectionResponse<>();

    response.setBrdFormId("form123");
    response.setBrdId("BRD123");
    response.setSectionName("TestSection");
    response.setSectionData("Test Data");

    assertEquals("form123", response.getBrdFormId());
    assertEquals("BRD123", response.getBrdId());
    assertEquals("TestSection", response.getSectionName());
    assertEquals("Test Data", response.getSectionData());
  }

  @Test
  void equals_WithSameData_ShouldBeEqual() {
    BRDSectionResponse<String> response1 =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data");
    BRDSectionResponse<String> response2 =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data");

    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void equals_WithDifferentData_ShouldNotBeEqual() {
    BRDSectionResponse<String> response1 =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data 1");
    BRDSectionResponse<String> response2 =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data 2");

    assertNotEquals(response1, response2);
    assertNotEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void equals_WithDifferentGenericTypes_ShouldHandleCorrectly() {
    BRDSectionResponse<String> stringResponse =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data");
    BRDSectionResponse<Integer> intResponse =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", 123);

    assertNotEquals(stringResponse, intResponse);
  }

  @Test
  void toString_ShouldContainAllFields() {
    BRDSectionResponse<String> response =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", "Test Data");

    String toString = response.toString();

    assertTrue(toString.contains("brdFormId=form123"));
    assertTrue(toString.contains("brdId=BRD123"));
    assertTrue(toString.contains("sectionName=TestSection"));
    assertTrue(toString.contains("sectionData=Test Data"));
  }

  @Test
  void withCustomObject_ShouldHandleCorrectly() {
    TestData testData = new TestData("Test Value");
    BRDSectionResponse<TestData> response =
        new BRDSectionResponse<>("form123", "BRD123", "TestSection", testData);

    assertEquals("Test Value", response.getSectionData().getValue());
  }

  // Helper class for testing custom objects
  private static class TestData {
    private final String value;

    TestData(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
