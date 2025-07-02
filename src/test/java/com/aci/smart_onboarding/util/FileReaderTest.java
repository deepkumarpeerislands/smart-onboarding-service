package com.aci.smart_onboarding.util;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.dto.BrdRules;
import com.aci.smart_onboarding.dto.GuidanceData;
import com.aci.smart_onboarding.util.FileReader.FileType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileReaderTest {

  private FileReader fileReader;

  @BeforeEach
  void setUp() {
    fileReader = new FileReader();
  }

  @Test
  void parseByteArrayContent_StandardData_Success() {
    // Given
    String content = "Rule1|Key1|0.9|Description1\n" + "Rule2|Key2|0.8|Description2";
    byte[] fileContent = content.getBytes(StandardCharsets.UTF_8);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(2, result.size());

    GuidanceData firstRule = result.get(0);
    assertEquals("Rule1", firstRule.getRuleName());
    assertEquals("Key1", firstRule.getMappingKey());
    assertEquals("0.9", firstRule.getSimilarity());
    assertEquals("Description1", firstRule.getExplanation());

    GuidanceData secondRule = result.get(1);
    assertEquals("Rule2", secondRule.getRuleName());
    assertEquals("Key2", secondRule.getMappingKey());
    assertEquals("0.8", secondRule.getSimilarity());
    assertEquals("Description2", secondRule.getExplanation());
  }

  @Test
  void parseByteArrayContent_UserRules_Success() {
    // Given
    String content =
        "BRD1|BRDName1|1001|RuleName1|Value1|1\n" + "BRD2|BRDName2|1002|RuleName2|Value2|2";
    byte[] fileContent = content.getBytes(StandardCharsets.UTF_8);

    // When
    List<BrdRules> result =
        fileReader.parseByteArrayContent(fileContent, FileType.USER_RULES, BrdRules.class);

    // Then
    assertEquals(2, result.size());

    BrdRules firstRule = result.get(0);
    assertEquals("BRD1", firstRule.getBrdId());
    assertEquals("BRDName1", firstRule.getBrdName());
    assertEquals("1001", firstRule.getRuleId());
    assertEquals("RuleName1", firstRule.getRuleName());
    assertEquals("Value1", firstRule.getValue());
    assertEquals("1", firstRule.getOrder());

    BrdRules secondRule = result.get(1);
    assertEquals("BRD2", secondRule.getBrdId());
    assertEquals("BRDName2", secondRule.getBrdName());
    assertEquals("1002", secondRule.getRuleId());
    assertEquals("RuleName2", secondRule.getRuleName());
    assertEquals("Value2", secondRule.getValue());
    assertEquals("2", secondRule.getOrder());
  }

  @Test
  void parseByteArrayContent_WithUTF8BOM_Success() {
    // Given
    byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    String content = "Rule1|Key1|0.9|Description1";
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    byte[] fileContent = new byte[bom.length + contentBytes.length];
    System.arraycopy(bom, 0, fileContent, 0, bom.length);
    System.arraycopy(contentBytes, 0, fileContent, bom.length, contentBytes.length);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(1, result.size());
    GuidanceData rule = result.get(0);
    assertEquals("Rule1", rule.getRuleName());
    assertEquals("Key1", rule.getMappingKey());
    assertEquals("0.9", rule.getSimilarity());
    assertEquals("Description1", rule.getExplanation());
  }

  @Test
  void parseByteArrayContent_WithUTF16LEBOM_Success() {
    // Given
    byte[] bom = new byte[] {(byte) 0xFF, (byte) 0xFE};
    String content = "Rule1|Key1|0.9|Description1";
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_16LE);
    byte[] fileContent = new byte[bom.length + contentBytes.length];
    System.arraycopy(bom, 0, fileContent, 0, bom.length);
    System.arraycopy(contentBytes, 0, fileContent, bom.length, contentBytes.length);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(1, result.size());
    GuidanceData rule = result.get(0);
    assertEquals("Rule1", rule.getRuleName());
    assertEquals("Key1", rule.getMappingKey());
    assertEquals("0.9", rule.getSimilarity());
    assertEquals("Description1", rule.getExplanation());
  }

  @Test
  void parseByteArrayContent_EmptyContent_ReturnsEmptyList() {
    // Given
    byte[] fileContent = new byte[0];

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void parseByteArrayContent_WithBlankLines_IgnoresBlankLines() {
    // Given
    String content = "\nRule1|Key1|0.9|Description1\n\n\nRule2|Key2|0.8|Description2\n\n";
    byte[] fileContent = content.getBytes(StandardCharsets.UTF_8);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(2, result.size());
  }

  @Test
  void parseByteArrayContent_PartialFields_HandlesGracefully() {
    // Given
    String content = "Rule1|Key1\nRule2|Key2|0.8";
    byte[] fileContent = content.getBytes(StandardCharsets.UTF_8);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(2, result.size());

    GuidanceData firstRule = result.get(0);
    assertEquals("Rule1", firstRule.getRuleName());
    assertEquals("Key1", firstRule.getMappingKey());
    assertNull(firstRule.getSimilarity());
    assertNull(firstRule.getExplanation());

    GuidanceData secondRule = result.get(1);
    assertEquals("Rule2", secondRule.getRuleName());
    assertEquals("Key2", secondRule.getMappingKey());
    assertEquals("0.8", secondRule.getSimilarity());
    assertNull(secondRule.getExplanation());
  }

  @Test
  void parseByteArrayContent_InvalidSimilarityFormat_HandlesGracefully() {
    // Given
    String content = "Rule1|Key1|invalid|Description1";
    byte[] fileContent = content.getBytes(StandardCharsets.UTF_8);

    // When
    List<GuidanceData> result =
        fileReader.parseByteArrayContent(fileContent, FileType.STANDARD_DATA, GuidanceData.class);

    // Then
    assertEquals(1, result.size());
    GuidanceData rule = result.get(0);
    assertEquals("Rule1", rule.getRuleName());
    assertEquals("Key1", rule.getMappingKey());
    assertEquals("invalid", rule.getSimilarity());
    assertEquals("Description1", rule.getExplanation());
  }

  @Test
  void parseByteArrayContent_UnsupportedFileType_ThrowsException() {
    // Given
    byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);
    FileType invalidType = null;

    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> fileReader.parseByteArrayContent(fileContent, invalidType, GuidanceData.class));
  }

  @Test
  void parseByteArrayContent_NullContent_ThrowsException() {
    // When & Then
    assertThrows(
        RuntimeException.class,
        () -> fileReader.parseByteArrayContent(null, FileType.STANDARD_DATA, GuidanceData.class));
  }
}
