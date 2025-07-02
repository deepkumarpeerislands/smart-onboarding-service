package com.aci.smart_onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.BrdTemplateConfig;
import com.aci.smart_onboarding.model.UATTestCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@ExtendWith(MockitoExtension.class)
class DtoModelMapperTest {

  @Mock private ModelMapper modelMapper;

  @Mock private ObjectMapper objectMapper;

  private DtoModelMapper dtoModelMapper;

  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    dtoModelMapper = new DtoModelMapper(modelMapper, objectMapper);
    now = LocalDateTime.now();
  }

  @Test
  @DisplayName("Should map BRD to BRDResponse successfully")
  void mapToBrdResponse_WithValidBRD_ShouldMapSuccessfully() {
    // Arrange
    BRD brd =
        BRD.builder()
            .brdFormId("brdForm123")
            .status("DRAFT")
            .projectId("PRJ001")
            .brdId("BRD0003")
            .brdName("Test BRD")
            .description("Test Description")
            .customerId("ORG001")
            .creator("TestUser")
            .type("Standard")
            .notes("Test Notes")
            .createdAt(now)
            .updatedAt(now)
            .wallentronIncluded(true)
            .achEncrypted(false)
            .build();

    // Mock the model mapper to return a properly mapped response
    BRDResponse expectedResponse = new BRDResponse();
    expectedResponse.setBrdId("BRD0003");
    expectedResponse.setBrdName("Test BRD");
    expectedResponse.setStatus("DRAFT");
    expectedResponse.setWallentronIncluded(true);
    expectedResponse.setAchEncrypted(false);
    when(modelMapper.map(any(BRD.class), eq(BRDResponse.class))).thenReturn(expectedResponse);

    // Act
    BRDResponse response = dtoModelMapper.mapToBrdResponse(brd);

    // Assert
    assertNotNull(response);
    assertEquals(brd.getBrdId(), response.getBrdId());
    assertEquals(brd.getBrdName(), response.getBrdName());
    assertEquals(brd.getStatus(), response.getStatus());
    assertEquals(brd.isWallentronIncluded(), response.isWallentronIncluded());
    assertEquals(brd.isAchEncrypted(), response.isAchEncrypted());
  }

  @Test
  void mapToBrdResponse_ThrowsBadRequestException() {
    // Given
    BRD brd = null; // This will cause ModelMapper to throw an exception
    when(modelMapper.map(isNull(), eq(BRDResponse.class)))
        .thenThrow(new IllegalArgumentException("Null BRD"));

    // When & Then
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToBrdResponse(brd));
    assertTrue(exception.getMessage().contains("Error mapping BRD"));
  }

  @Test
  @DisplayName("Should map BRDRequest to BRD successfully")
  void mapToBrd_WithValidRequest_ShouldMapSuccessfully() {
    // Arrange
    BRDRequest request =
        BRDRequest.builder()
            .brdId("BRD0003")
            .status("DRAFT")
            .projectId("PRJ001")
            .brdName("Test BRD")
            .description("Test Description")
            .build();

    // Mock the model mapper to return a properly mapped BRD
    BRD expectedBrd =
        BRD.builder()
            .brdId("BRD0003")
            .status("DRAFT")
            .projectId("PRJ001")
            .brdName("Test BRD")
            .description("Test Description")
            .build();
    when(modelMapper.map(any(BRDRequest.class), eq(BRD.class))).thenReturn(expectedBrd);

    // Act
    BRD brd = dtoModelMapper.mapToBrd(request);

    // Assert
    assertNotNull(brd);
    assertEquals(request.getBrdId(), brd.getBrdId());
    assertEquals(request.getBrdName(), brd.getBrdName());
    assertEquals(request.getStatus(), brd.getStatus());
  }

  @Test
  void mapToAuditLog_Success() {
    // Given
    AuditLogRequest request = new AuditLogRequest();
    request.setAction("TEST_ACTION");

    // Mock the model mapper response
    AuditLog expectedLog = new AuditLog();
    expectedLog.setAction("TEST_ACTION");
    when(modelMapper.map(any(AuditLogRequest.class), eq(AuditLog.class))).thenReturn(expectedLog);

    // When
    AuditLog actualLog = dtoModelMapper.mapToAuditLog(request);

    // Then
    assertNotNull(actualLog);
    assertNull(actualLog.getAuditId());
    assertEquals(request.getAction(), actualLog.getAction());
  }

  @Test
  void mapToAuditLogResponse_Success() {
    // Given
    AuditLog auditLog = new AuditLog();
    auditLog.setAction("TEST_ACTION");

    // Mock the model mapper response
    AuditLogResponse expectedResponse = new AuditLogResponse();
    expectedResponse.setAction("TEST_ACTION");
    when(modelMapper.map(any(AuditLog.class), eq(AuditLogResponse.class)))
        .thenReturn(expectedResponse);

    // When
    AuditLogResponse actualResponse = dtoModelMapper.mapToAuditLogResponse(auditLog);

    // Then
    assertNotNull(actualResponse);
    assertEquals(auditLog.getAction(), actualResponse.getAction());
  }

  @Test
  void mapBrdResponseToMAP_Success() {
    // Given
    BRDResponse response = new BRDResponse();
    Map<String, Object> expectedMap = new HashMap<>();
    when(objectMapper.convertValue(eq(response), any(TypeReference.class))).thenReturn(expectedMap);

    // When
    Map<String, Object> actualMap = dtoModelMapper.mapBrdResponseToMAP(response);

    // Then
    assertNotNull(actualMap);
    assertEquals(expectedMap, actualMap);
    verify(objectMapper).convertValue(eq(response), any(TypeReference.class));
  }

  @Test
  void mapBrdResponseToMAP_ShouldIncludeStatus() {
    // Given
    BRDResponse response = new BRDResponse();
    response.setStatus("DRAFT");
    response.setBrdId("BRD123");

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("status", "DRAFT");
    expectedMap.put("brdId", "BRD123");

    when(objectMapper.convertValue(any(), any(TypeReference.class))).thenReturn(expectedMap);

    // When
    Map<String, Object> actualMap = dtoModelMapper.mapBrdResponseToMAP(response);

    // Then
    assertNotNull(actualMap);
    assertEquals("DRAFT", actualMap.get("status"));
    assertEquals("BRD123", actualMap.get("brdId"));
  }

  @Test
  void mapBrdResponseToMAP_WithNullStatus_ShouldNotIncludeStatus() {
    // Given
    BRDResponse response = new BRDResponse();
    response.setBrdId("BRD123");

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("brdId", "BRD123");

    when(objectMapper.convertValue(any(), any(TypeReference.class))).thenReturn(expectedMap);

    // When
    Map<String, Object> actualMap = dtoModelMapper.mapBrdResponseToMAP(response);

    // Then
    assertNotNull(actualMap);
    assertFalse(actualMap.containsKey("status"));
    assertEquals("BRD123", actualMap.get("brdId"));
  }

  @Test
  void mapBrdResponseToMAP_ThrowsBadRequestException() {
    // Given
    BRDResponse response = new BRDResponse();
    when(objectMapper.convertValue(any(), any(TypeReference.class)))
        .thenThrow(new IllegalArgumentException("Conversion error"));

    // When & Then
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapBrdResponseToMAP(response));
    assertTrue(exception.getMessage().contains("Error mapping BRDResponse to map"));
  }

  @Test
  @DisplayName("Should map Document to BRDSearchResponse successfully")
  void mapToSearchResponse_WithValidDocument_ShouldMapSuccessfully() {
    // Arrange
    ObjectId objectId = new ObjectId();
    Document doc =
        new Document()
            .append("_id", objectId)
            .append("brdId", "BRD0003")
            .append("customerId", "CUST001")
            .append("brdName", "Test BRD")
            .append("creator", "TestUser")
            .append("type", "Standard")
            .append("status", "DRAFT")
            .append("notes", "Test Notes");

    // Act
    BRDSearchResponse response = dtoModelMapper.mapToSearchResponse(doc);

    // Assert
    assertNotNull(response);
    assertEquals(objectId.toString(), response.getBrdFormId());
    assertEquals("BRD0003", response.getBrdId());
    assertEquals("CUST001", response.getCustomerId());
    assertEquals("Test BRD", response.getBrdName());
    assertEquals("TestUser", response.getCreator());
    assertEquals("Standard", response.getType());
    assertEquals("DRAFT", response.getStatus());
    assertEquals("Test Notes", response.getNotes());
  }

  @Test
  void mapToSearchResponse_ThrowsBadRequestException() {
    // Given
    Document doc = new Document(); // Invalid document without required fields

    // When & Then
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToSearchResponse(doc));
    assertTrue(exception.getMessage().contains("Error mapping Document to BRDSearchResponse"));
  }

  @Test
  @DisplayName("Should map SiteResponse to Map successfully")
  void mapDivisionResponseToMAP_WithValidResponse_ShouldMapSuccessfully() {
    // Arrange
    SiteResponse.DivisionDetails divisionDetails =
        SiteResponse.DivisionDetails.builder()
            .siteId("SITE001")
            .siteName("Test Site")
            .identifierCode("TST")
            .description("Test Description")
            .build();

    SiteResponse response =
        SiteResponse.builder()
            .brdId("BRD0003")
            .wallentronIncluded(true)
            .achEncrypted(false)
            .siteList(List.of(divisionDetails))
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Act
    Map<String, Object> result = dtoModelMapper.mapDivisionResponseToMAP(response);

    // Assert
    assertNotNull(result);
    assertEquals("BRD0003", result.get("brdId"));
    assertTrue((Boolean) result.get("wallentronIncluded"));
    assertFalse((Boolean) result.get("achEncrypted"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> divisionList = (List<Map<String, Object>>) result.get("divisionList");
    assertNotNull(divisionList);
    assertEquals(1, divisionList.size());

    Map<String, Object> mappedDivision = divisionList.get(0);
    assertEquals("SITE001", mappedDivision.get("divisionId"));
    assertEquals("Test Site", mappedDivision.get("divisionName"));
    assertEquals("TST", mappedDivision.get("identifierCode"));
    assertEquals("Test Description", mappedDivision.get("description"));
  }

  @Test
  @DisplayName("Should get nested field value successfully")
  void getFieldValue_WithNestedField_ShouldReturnValue() {
    // Arrange
    TestObject nested = new TestObject("nestedValue");
    TestObject parent = new TestObject("parentValue");
    parent.setNested(nested);

    // Act
    Object result = dtoModelMapper.getFieldValue(parent, "nested.value");

    // Assert
    assertEquals("nestedValue", result);
  }

  @Test
  @DisplayName("Should return null for non-existent field")
  void getFieldValue_WithNonExistentField_ShouldReturnNull() {
    // Arrange
    TestObject testObject = new TestObject("test");

    // Act
    Object result = dtoModelMapper.getFieldValue(testObject, "nonexistent");

    // Assert
    assertNull(result);
  }

  // Helper class for testing nested field access
  private static class TestObject {
    private String value;
    private TestObject nested;

    public TestObject(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public TestObject getNested() {
      return nested;
    }

    public void setNested(TestObject nested) {
      this.nested = nested;
    }
  }

  @ParameterizedTest
  @MethodSource("provideFieldValueTestCases")
  void getFieldValue_WithVariousInputs_ReturnsExpectedValue(
      Object input, String fieldName, Object expectedValue, String testDescription) {
    // When
    Object result = dtoModelMapper.getFieldValue(input, fieldName);

    // Then
    assertEquals(expectedValue, result, testDescription);
  }

  private static Stream<Arguments> provideFieldValueTestCases() {
    return Stream.of(
        Arguments.of(null, "fieldName", null, "Should return null when input object is null"),
        Arguments.of(new BRDResponse(), null, null, "Should return null when field name is null"),
        Arguments.of(
            new BRDResponse(),
            "nonExistentField",
            null,
            "Should return null when field name is invalid"),
        Arguments.of(
            new BRDResponse(),
            "fieldName",
            null,
            "Should return null when exception occurs during field retrieval"),
        Arguments.of(
            null,
            "fieldName",
            null,
            "Should return null when input object is null for simple field value"),
        Arguments.of(
            new BRDResponse(),
            null,
            null,
            "Should return null when field name is null for simple field value"),
        Arguments.of(
            new BRDResponse(),
            "nonExistentField",
            null,
            "Should return null when field name is invalid for simple field value"),
        Arguments.of(
            new BRDResponse(),
            "fieldName",
            null,
            "Should return null when exception occurs during simple field retrieval"));
  }

  @Test
  @DisplayName("Should map BrdFieldCommentGroup to BrdFieldCommentGroupResp successfully")
  void mapToGroupResponse_WithValidGroup_ShouldMapSuccessfully() {
    // Arrange
    BrdFieldCommentGroup.CommentEntry comment =
        BrdFieldCommentGroup.CommentEntry.builder()
            .id("comment1")
            .content("Test comment")
            .createdBy("user1")
            .userType("user")
            .parentCommentId("parent1")
            .isRead(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

    BrdFieldCommentGroup group =
        BrdFieldCommentGroup.builder()
            .id("group1")
            .brdFormId("brdForm1")
            .siteId("site1")
            .sourceType("sourceType1")
            .fieldPath("field.path")
            .fieldPathShadowValue("shadowValue")
            .status("OPEN")
            .sectionName("Test Section")
            .createdBy("user1")
            .comments(List.of(comment))
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Act
    BrdFieldCommentGroupResp response = dtoModelMapper.mapToGroupResponse(group);

    // Assert
    assertNotNull(response);
    assertEquals(group.getId(), response.getId());
    assertEquals(group.getBrdFormId(), response.getBrdFormId());
    assertEquals(group.getSiteId(), response.getSiteId());
    assertEquals(group.getSourceType(), response.getSourceType());
    assertEquals(group.getFieldPath(), response.getFieldPath());
    assertEquals(group.getFieldPathShadowValue(), response.getFieldPathShadowValue());
    assertEquals(group.getStatus(), response.getStatus());
    assertEquals(group.getSectionName(), response.getSectionName());
    assertEquals(group.getCreatedBy(), response.getCreatedBy());
    assertEquals(group.getCreatedAt(), response.getCreatedAt());
    assertEquals(group.getUpdatedAt(), response.getUpdatedAt());
    assertEquals(1, response.getComments().size());
  }

  @Test
  @DisplayName("Should map CommentEntry to CommentEntryResp successfully")
  void mapToCommentResponse_WithValidComment_ShouldMapSuccessfully() {
    // Arrange
    BrdFieldCommentGroup.CommentEntry comment =
        BrdFieldCommentGroup.CommentEntry.builder()
            .id("comment1")
            .content("Test comment")
            .createdBy("user1")
            .userType("user")
            .parentCommentId("parent1")
            .isRead(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Act
    CommentEntryResp response = dtoModelMapper.mapToCommentResponse(comment);

    // Assert
    assertNotNull(response);
    assertEquals(comment.getId(), response.getId());
    assertEquals(comment.getContent(), response.getContent());
    assertEquals(comment.getCreatedBy(), response.getCreatedBy());
    assertEquals(comment.getUserType(), response.getUserType());
    assertEquals(comment.getParentCommentId(), response.getParentCommentId());
    assertEquals(comment.getIsRead(), response.getIsRead());
    assertEquals(comment.getCreatedAt(), response.getCreatedAt());
    assertEquals(comment.getUpdatedAt(), response.getUpdatedAt());
  }

  @Test
  @DisplayName("Should map BRDResponse to PrefillSections successfully")
  void mapResponseToPrefillSections_WithValidResponse_ShouldMapSuccessfully() {
    // Arrange
    BRDResponse response = new BRDResponse();
    response.setBrdId("BRD123");
    response.setBrdName("Test BRD");
    response.setStatus("DRAFT");
    response.setDescription("Test Description");

    // Mock ModelMapper behavior
    PrefillSections expectedSections = new PrefillSections();
    // Set properties appropriately based on actual PrefillSections class structure

    when(modelMapper.map(any(BRDResponse.class), eq(PrefillSections.class)))
        .thenReturn(expectedSections);

    // Act
    PrefillSections result = dtoModelMapper.mapResponseToPrefillSections(response);

    // Assert
    assertNotNull(result);
    assertEquals(expectedSections, result);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping to PrefillSections fails")
  void mapResponseToPrefillSections_WithMappingError_ShouldThrowException() {
    // Arrange
    BRDResponse response = new BRDResponse();
    when(modelMapper.map(any(BRDResponse.class), eq(PrefillSections.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> dtoModelMapper.mapResponseToPrefillSections(response));
    assertTrue(exception.getMessage().contains("Error mapping BRDResponse to PrefillSections"));
  }

  @Test
  @DisplayName("Should map BRD to BRDListResponse successfully")
  void mapToBrdListResponse_WithValidBRD_ShouldMapSuccessfully() {
    // Arrange
    BRD brd =
        BRD.builder()
            .brdId("BRD123")
            .brdName("Test BRD")
            .status("DRAFT")
            .creator("TestUser")
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Mock ModelMapper behavior
    BRDListResponse expectedResponse = new BRDListResponse();
    expectedResponse.setBrdId("BRD123");
    expectedResponse.setBrdName("Test BRD");
    expectedResponse.setStatus("DRAFT");

    when(modelMapper.map(any(BRD.class), eq(BRDListResponse.class))).thenReturn(expectedResponse);

    // Act
    BRDListResponse result = dtoModelMapper.mapToBrdListResponse(brd);

    // Assert
    assertNotNull(result);
    assertEquals(expectedResponse, result);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping to BRDListResponse fails")
  void mapToBrdListResponse_WithMappingError_ShouldThrowException() {
    // Arrange
    BRD brd = BRD.builder().brdId("BRD123").build();
    when(modelMapper.map(any(BRD.class), eq(BRDListResponse.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToBrdListResponse(brd));
    assertTrue(exception.getMessage().contains("Error mapping BRD"));
  }

  @Test
  @DisplayName("Should map BrdTemplateReq to BrdTemplateConfig successfully")
  void mapToBrdTemplateConfig_WithValidRequest_ShouldMapSuccessfully() {
    // Arrange
    BrdTemplateReq templateReq = new BrdTemplateReq();
    // Set properties according to actual BrdTemplateReq class structure

    // Mock ModelMapper behavior
    BrdTemplateConfig expectedConfig = new BrdTemplateConfig();
    // Set properties according to actual BrdTemplateConfig class structure

    when(modelMapper.map(any(BrdTemplateReq.class), eq(BrdTemplateConfig.class)))
        .thenReturn(expectedConfig);

    // Act
    BrdTemplateConfig result = dtoModelMapper.mapToBrdTemplateConfig(templateReq);

    // Assert
    assertNotNull(result);
    assertEquals(expectedConfig, result);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping to BrdTemplateConfig fails")
  void mapToBrdTemplateConfig_WithMappingError_ShouldThrowException() {
    // Arrange
    BrdTemplateReq templateReq = new BrdTemplateReq();
    when(modelMapper.map(any(BrdTemplateReq.class), eq(BrdTemplateConfig.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> dtoModelMapper.mapToBrdTemplateConfig(templateReq));
    assertTrue(exception.getMessage().contains("Error mapping BrdTemplateReq"));
  }

  @Test
  @DisplayName("Should map BrdTemplateConfig to BrdTemplateRes successfully")
  void mapToBrdTemplateConfigResponse_WithValidConfig_ShouldMapSuccessfully() {
    // Arrange
    BrdTemplateConfig templateConfig = new BrdTemplateConfig();
    // Set properties according to actual BrdTemplateConfig class structure

    // Mock ModelMapper behavior
    BrdTemplateRes expectedResponse = new BrdTemplateRes();
    // Set properties according to actual BrdTemplateRes class structure

    when(modelMapper.map(any(BrdTemplateConfig.class), eq(BrdTemplateRes.class)))
        .thenReturn(expectedResponse);

    // Act
    BrdTemplateRes result = dtoModelMapper.mapToBrdTemplateConfigResponse(templateConfig);

    // Assert
    assertNotNull(result);
    assertEquals(expectedResponse, result);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping to BrdTemplateRes fails")
  void mapToBrdTemplateConfigResponse_WithMappingError_ShouldThrowException() {
    // Arrange
    BrdTemplateConfig templateConfig = new BrdTemplateConfig();
    when(modelMapper.map(any(BrdTemplateConfig.class), eq(BrdTemplateRes.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> dtoModelMapper.mapToBrdTemplateConfigResponse(templateConfig));
    assertTrue(exception.getMessage().contains("Error mapping BrdTemplateConfig"));
  }

  @Test
  @DisplayName("Should map UATTestCase to UATTestCaseDTO successfully")
  void mapToUATTestCaseDTO_WithValidTestCase_ShouldMapSuccessfully() {
    // Arrange
    UATTestCase testCase = new UATTestCase();
    testCase.setId("TEST001");
    testCase.setBrdId("BRD-123");
    testCase.setTestName("Test Case 1");
    testCase.setScenario("Test Description");
    testCase.setPosition("top-right");
    testCase.setAnswer("Expected Result");
    testCase.setUatType(PortalTypes.AGENT);
    testCase.setTestType(TestType.NORMAL);
    testCase.setStatus(TestStatus.PASSED);
    testCase.setComments("Some comment");
    testCase.setFeatureName("Login Feature");
    testCase.setFields(new HashMap<>());

    // Mock ModelMapper behavior
    UATTestCaseDTO expectedDTO = new UATTestCaseDTO();
    expectedDTO.setId("TEST001");
    expectedDTO.setBrdId("BRD-123");
    expectedDTO.setTestName("Test Case 1");
    expectedDTO.setScenario("Test Description");
    expectedDTO.setPosition("top-right");
    expectedDTO.setAnswer("Expected Result");
    expectedDTO.setUatType(PortalTypes.AGENT);
    expectedDTO.setTestType(TestType.NORMAL);
    expectedDTO.setStatus(TestStatus.PASSED);
    expectedDTO.setComments("Some comment");
    expectedDTO.setFeatureName("Login Feature");
    expectedDTO.setFields(new HashMap<>());

    when(modelMapper.map(any(UATTestCase.class), eq(UATTestCaseDTO.class))).thenReturn(expectedDTO);

    // Act
    UATTestCaseDTO result = dtoModelMapper.mapToUATTestCaseDTO(testCase);

    // Assert
    assertNotNull(result);
    assertEquals(expectedDTO, result);
    verify(modelMapper).map(testCase, UATTestCaseDTO.class);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping UATTestCase to DTO fails")
  void mapToUATTestCaseDTO_WithMappingError_ShouldThrowException() {
    // Arrange
    UATTestCase testCase = new UATTestCase();
    when(modelMapper.map(any(UATTestCase.class), eq(UATTestCaseDTO.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToUATTestCaseDTO(testCase));
    assertTrue(exception.getMessage().contains("Error mapping UATTestCase to DTO"));
  }

  @Test
  @DisplayName("Should map UATTestCaseDTO to UATTestCase successfully")
  void mapToUATTestCase_WithValidDTO_ShouldMapSuccessfully() {
    // Arrange
    UATTestCaseDTO dto = new UATTestCaseDTO();
    dto.setId("TEST001");
    dto.setBrdId("BRD-123");
    dto.setTestName("Test Case 1");
    dto.setScenario("Test Description");
    dto.setPosition("top-right");
    dto.setAnswer("Expected Result");
    dto.setUatType(PortalTypes.AGENT);
    dto.setTestType(TestType.NORMAL);
    dto.setStatus(TestStatus.PASSED);
    dto.setComments("Some comment");
    dto.setFeatureName("Login Feature");
    dto.setFields(new HashMap<>());

    // Mock ModelMapper behavior
    UATTestCase expectedTestCase = new UATTestCase();
    expectedTestCase.setId("TEST001");
    expectedTestCase.setBrdId("BRD-123");
    expectedTestCase.setTestName("Test Case 1");
    expectedTestCase.setScenario("Test Description");
    expectedTestCase.setPosition("top-right");
    expectedTestCase.setAnswer("Expected Result");
    expectedTestCase.setUatType(PortalTypes.AGENT);
    expectedTestCase.setTestType(TestType.NORMAL);
    expectedTestCase.setStatus(TestStatus.PASSED);
    expectedTestCase.setComments("Some comment");
    expectedTestCase.setFeatureName("Login Feature");
    expectedTestCase.setFields(new HashMap<>());

    when(modelMapper.map(any(UATTestCaseDTO.class), eq(UATTestCase.class)))
        .thenReturn(expectedTestCase);

    // Act
    UATTestCase result = dtoModelMapper.mapToUATTestCase(dto);

    // Assert
    assertNotNull(result);
    assertEquals(expectedTestCase, result);
    verify(modelMapper).map(dto, UATTestCase.class);
  }

  @Test
  @DisplayName("Should throw BadRequestException when mapping UATTestCaseDTO to entity fails")
  void mapToUATTestCase_WithMappingError_ShouldThrowException() {
    // Arrange
    UATTestCaseDTO dto = new UATTestCaseDTO();
    when(modelMapper.map(any(UATTestCaseDTO.class), eq(UATTestCase.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToUATTestCase(dto));
    assertTrue(exception.getMessage().contains("Error mapping UATTestCaseDTO to entity"));
  }

  @Test
  @DisplayName("Should map UATTestCaseRequestResponseDTO to UATTestCase successfully")
  void mapToUATTestCase_WithValidRequestResponseDTO_ShouldMapSuccessfully() {
    // Arrange
    UATTestCaseRequestResponseDTO dto = new UATTestCaseRequestResponseDTO();
    dto.setBrdId("BRD-123");
    dto.setTestName("Test Case 1");
    dto.setScenario("Test Description");
    dto.setPosition("top-right");
    dto.setAnswer("Expected Result");
    dto.setUatType(PortalTypes.CONSUMER);
    dto.setTestType(TestType.NORMAL);
    dto.setStatus(TestStatus.PASSED);
    dto.setComments("Some comment");
    dto.setFeatureName("Login Feature");
    dto.setFields(new HashMap<>());

    // Mock ModelMapper behavior
    UATTestCase expectedTestCase = new UATTestCase();
    expectedTestCase.setId(null);
    expectedTestCase.setBrdId("BRD-123");
    expectedTestCase.setTestName("Test Case 1");
    expectedTestCase.setScenario("Test Description");
    expectedTestCase.setPosition("top-right");
    expectedTestCase.setAnswer("Expected Result");
    expectedTestCase.setUatType(PortalTypes.CONSUMER);
    expectedTestCase.setTestType(TestType.NORMAL);
    expectedTestCase.setStatus(TestStatus.PASSED);
    expectedTestCase.setComments("Some comment");
    expectedTestCase.setFeatureName("Login Feature");
    expectedTestCase.setFields(new HashMap<>());

    when(modelMapper.map(any(UATTestCaseRequestResponseDTO.class), eq(UATTestCase.class)))
        .thenReturn(expectedTestCase);

    // Act
    UATTestCase result = dtoModelMapper.mapToUATTestCase(dto);

    // Assert
    assertNotNull(result);
    assertEquals(expectedTestCase, result);
    verify(modelMapper).map(dto, UATTestCase.class);
  }

  @Test
  @DisplayName(
      "Should throw BadRequestException when mapping UATTestCaseRequestResponseDTO to entity fails")
  void mapToUATTestCase_WithRequestResponseDTOMappingError_ShouldThrowException() {
    // Arrange
    UATTestCaseRequestResponseDTO dto = new UATTestCaseRequestResponseDTO();
    when(modelMapper.map(any(UATTestCaseRequestResponseDTO.class), eq(UATTestCase.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> dtoModelMapper.mapToUATTestCase(dto));
    assertTrue(
        exception.getMessage().contains("Error mapping UATTestCaseRequestResponseDTO to entity"));
  }

  @Test
  @DisplayName("Should map UATTestCase to UATTestCaseRequestResponseDTO successfully")
  void mapToUATTestCaseRequestResponseDTO_WithValidTestCase_ShouldMapSuccessfully() {
    // Arrange
    UATTestCase testCase = new UATTestCase();
    testCase.setId("TEST001");
    testCase.setBrdId("BRD-123");
    testCase.setTestName("Test Case 1");
    testCase.setScenario("Test Description");
    testCase.setPosition("top-right");
    testCase.setAnswer("Expected Result");
    testCase.setUatType(PortalTypes.AGENT);
    testCase.setTestType(TestType.NORMAL);
    testCase.setStatus(TestStatus.PASSED);
    testCase.setComments("Some comment");
    testCase.setFeatureName("Login Feature");
    testCase.setFields(new HashMap<>());

    // Mock ModelMapper behavior
    UATTestCaseRequestResponseDTO expectedDTO = new UATTestCaseRequestResponseDTO();
    expectedDTO.setBrdId("BRD-123");
    expectedDTO.setTestName("Test Case 1");
    expectedDTO.setScenario("Test Description");
    expectedDTO.setPosition("top-right");
    expectedDTO.setAnswer("Expected Result");
    expectedDTO.setUatType(PortalTypes.AGENT);
    expectedDTO.setTestType(TestType.NORMAL);
    expectedDTO.setStatus(TestStatus.PASSED);
    expectedDTO.setComments("Some comment");
    expectedDTO.setFeatureName("Login Feature");
    expectedDTO.setFields(new HashMap<>());

    when(modelMapper.map(any(UATTestCase.class), eq(UATTestCaseRequestResponseDTO.class)))
        .thenReturn(expectedDTO);

    // Act
    UATTestCaseRequestResponseDTO result =
        dtoModelMapper.mapToUATTestCaseRequestResponseDTO(testCase);

    // Assert
    assertNotNull(result);
    assertEquals(expectedDTO, result);
    verify(modelMapper).map(testCase, UATTestCaseRequestResponseDTO.class);
  }

  @Test
  @DisplayName(
      "Should throw BadRequestException when mapping UATTestCase to RequestResponseDTO fails")
  void mapToUATTestCaseRequestResponseDTO_WithMappingError_ShouldThrowException() {
    // Arrange
    UATTestCase testCase = new UATTestCase();
    when(modelMapper.map(any(UATTestCase.class), eq(UATTestCaseRequestResponseDTO.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> dtoModelMapper.mapToUATTestCaseRequestResponseDTO(testCase));
    assertTrue(exception.getMessage().contains("Error mapping UATTestCase to RequestResponseDTO"));
  }

  @Test
  @DisplayName(
      "Should map UATTestCaseDTO to UATTestCaseRequestResponseDTO with portal type successfully")
  void mapToUATTestCaseRequestResponseDTOWithPortalType_WithValidTestCase_ShouldMapSuccessfully() {
    // Arrange
    UATTestCaseDTO testCase = new UATTestCaseDTO();
    testCase.setId("TEST001");
    testCase.setBrdId("BRD-123");
    testCase.setTestName("Test Case 1");
    testCase.setScenario("Test Description");
    testCase.setPosition("top-right");
    testCase.setAnswer("Expected Result");
    testCase.setUatType(PortalTypes.AGENT);
    testCase.setTestType(TestType.NORMAL);
    testCase.setStatus(TestStatus.PASSED);
    testCase.setComments("Some comment");
    testCase.setFeatureName("Login Feature");
    testCase.setFields(new HashMap<>());

    // Mock ModelMapper behavior
    UATTestCaseRequestResponseDTO expectedDTO = new UATTestCaseRequestResponseDTO();
    expectedDTO.setBrdId("BRD-123");
    expectedDTO.setTestName("Test Case 1");
    expectedDTO.setScenario("Test Description");
    expectedDTO.setPosition("top-right");
    expectedDTO.setAnswer("Expected Result");
    expectedDTO.setUatType(PortalTypes.CONSUMER);
    expectedDTO.setTestType(TestType.NORMAL);
    expectedDTO.setStatus(TestStatus.PASSED);
    expectedDTO.setComments("Some comment");
    expectedDTO.setFeatureName("Login Feature");
    expectedDTO.setFields(new HashMap<>());

    when(modelMapper.map(any(UATTestCaseDTO.class), eq(UATTestCaseRequestResponseDTO.class)))
        .thenReturn(expectedDTO);

    // Act
    UATTestCaseRequestResponseDTO result =
        dtoModelMapper.mapToUATTestCaseRequestResponseDTOWithPortalType(
            testCase, PortalTypes.CONSUMER);

    // Assert
    assertNotNull(result);
    assertEquals(expectedDTO, result);
    assertEquals(PortalTypes.CONSUMER, result.getUatType());
    verify(modelMapper).map(testCase, UATTestCaseRequestResponseDTO.class);
  }

  @Test
  @DisplayName(
      "Should throw BadRequestException when mapping UATTestCaseDTO to RequestResponseDTO with portal type fails")
  void mapToUATTestCaseRequestResponseDTOWithPortalType_WithMappingError_ShouldThrowException() {
    // Arrange
    UATTestCaseDTO testCase = new UATTestCaseDTO();
    when(modelMapper.map(any(UATTestCaseDTO.class), eq(UATTestCaseRequestResponseDTO.class)))
        .thenThrow(new IllegalArgumentException("Mapping error"));

    // Act & Assert
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                dtoModelMapper.mapToUATTestCaseRequestResponseDTOWithPortalType(
                    testCase, PortalTypes.CONSUMER));
    assertTrue(
        exception.getMessage().contains("Error mapping UATTestCaseDTO to RequestResponseDTO"));
  }
}
