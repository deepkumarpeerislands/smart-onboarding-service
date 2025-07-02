package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.LegacyBRD;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.LegacyBRDRepository;
import com.aci.smart_onboarding.repository.SiteRepository;
import com.aci.smart_onboarding.service.implementation.AssistantService;
import com.aci.smart_onboarding.service.implementation.BlobStorageService;
import com.aci.smart_onboarding.service.implementation.LegacyBrdService;
import com.aci.smart_onboarding.util.FileReader;
import com.aci.smart_onboarding.util.FileReader.FileType;
import com.aci.smart_onboarding.util.brd_form.GeneralImplementations;
import com.aci.smart_onboarding.util.brd_form.ImplementationNote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LegacyBrdServiceTest {

  @Mock private FileReader fileReader;

  @Mock private BlobStorageService blobStorageService;

  @Mock private LegacyBRDRepository legacyBRDRepository;

  @Mock private IBRDService brdService;

  @Mock private DtoModelMapper dtoModelMapper;

  @Mock private AssistantService assistantService;

  @Mock private SiteRepository siteRepository;

  @Mock private ObjectWriter objectWriter;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private LegacyBrdService legacyBrdService;

  private static final String TEST_BRD_ID = "test-brd-123";
  private static final String TEST_DOCUMENT_NAME = "test-document.pdf";
  private static final String TEST_RULES_FILE = "test-rules.json";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(legacyBrdService, "franklinRulesFileName", TEST_RULES_FILE);
    lenient().when(blobStorageService.fetchFile(any())).thenReturn(Mono.just("test".getBytes()));
  }

  @Test
  void getStandardData_validFileUrl_returnsGuidanceDataList() {
    // Given
    String standardDataFileUrl = "standardDataFileUrl";
    byte[] standardDataBytes = "standardData".getBytes();
    List<GuidanceData> guidanceDataList = List.of(new GuidanceData());

    when(blobStorageService.fetchFileFromUrl(standardDataFileUrl))
        .thenReturn(Mono.just(standardDataBytes));
    when(fileReader.parseByteArrayContent(
            standardDataBytes, FileType.STANDARD_DATA, GuidanceData.class))
        .thenReturn(guidanceDataList);

    // When & Then
    StepVerifier.create(legacyBrdService.getStandardData(standardDataFileUrl))
        .expectNext(guidanceDataList)
        .verifyComplete();
  }

  @Test
  void getUserRules_validFileUrl_returnsBrdRulesList() {
    // Given
    String userRulesFileUrl = "userRulesFileUrl";
    byte[] userRulesBytes = "userRules".getBytes();
    BrdRules brdRule = new BrdRules();
    brdRule.setRuleName("Test Rule");
    brdRule.setOrder("1");
    List<BrdRules> brdRulesList = List.of(brdRule);

    when(blobStorageService.fetchFileFromUrl(userRulesFileUrl))
        .thenReturn(Mono.just(userRulesBytes));
    when(fileReader.parseByteArrayContent(userRulesBytes, FileType.USER_RULES, BrdRules.class))
        .thenReturn(brdRulesList);

    // When & Then
    StepVerifier.create(legacyBrdService.getUserRules(userRulesFileUrl))
        .expectNext(brdRulesList)
        .verifyComplete();
  }

  @Test
  void getRulesWithData_validBrdRequest_returnsProcessedRules() throws JsonProcessingException {
    // Given
    LegacyBrdRequest request = new LegacyBrdRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setBrdRulesFileUrl("test-url");

    // Create valid guidance data
    GuidanceData guidanceData = new GuidanceData();
    guidanceData.setRuleName("Test Rule");
    List<GuidanceData> guidanceDataList = List.of(guidanceData);

    // Create valid BRD rules
    BrdRules mainBrdRule = new BrdRules();
    mainBrdRule.setBrdId(TEST_BRD_ID);
    mainBrdRule.setBrdName("Test BRD");
    mainBrdRule.setRuleId("1013");
    mainBrdRule.setValue("test-value");
    mainBrdRule.setOrder("1");
    List<BrdRules> brdRulesList = List.of(mainBrdRule);

    // Create expected combined rules
    RulesWithData rulesWithData = new RulesWithData();
    rulesWithData.setBrdId(TEST_BRD_ID);
    rulesWithData.setBrdName("Test BRD");
    rulesWithData.setRuleId("1013");
    rulesWithData.setValue("test-value");
    rulesWithData.setOrder("1");
    List<RulesWithData> combinedRules = List.of(rulesWithData);

    byte[] guidanceDataBytes = "test".getBytes();
    byte[] brdRulesBytes = "test".getBytes();
    byte[] jsonData =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(combinedRules);

    when(blobStorageService.fetchFile(anyString())).thenReturn(Mono.just(guidanceDataBytes));
    when(blobStorageService.fetchFileFromUrl(anyString())).thenReturn(Mono.just(brdRulesBytes));

    // Use specific matchers for parseByteArrayContent
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(GuidanceData.class)))
        .thenReturn(guidanceDataList);
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(BrdRules.class)))
        .thenReturn(brdRulesList);

    LegacyBRD legacyBRD = new LegacyBRD();
    legacyBRD.setBrdId(TEST_BRD_ID);
    when(legacyBRDRepository.save(any(LegacyBRD.class))).thenReturn(Mono.just(legacyBRD));
    when(assistantService.findSemanticMatches(anyList())).thenReturn(Mono.just(guidanceDataList));
    when(blobStorageService.updateFile(anyString(), anyString())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(legacyBrdService.getRulesWithData(request))
        .expectNextMatches(
            resource ->
                resource instanceof ByteArrayResource
                    && Arrays.equals(((ByteArrayResource) resource).getByteArray(), jsonData))
        .verifyComplete();

    verify(blobStorageService).fetchFile(TEST_RULES_FILE);
    verify(blobStorageService).fetchFileFromUrl("test-url");
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(GuidanceData.class));
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(BrdRules.class));
    verify(legacyBRDRepository).save(any(LegacyBRD.class));
  }

  @Test
  void getRulesWithData_newRulesRequest_returnsProcessedRulesWithSemanticMatches() {
    // Given
    LegacyBrdRequest request = new LegacyBrdRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setBrdRulesFileUrl("test-url");

    // Create test data with a new rule that needs semantic matching
    GuidanceData existingGuidance = new GuidanceData();
    existingGuidance.setRuleName("Existing Rule");

    GuidanceData newGuidance = new GuidanceData();
    newGuidance.setRuleName("New Rule");
    newGuidance.setMappingKey("NEW_MAPPING");

    List<GuidanceData> existingGuidanceList = Arrays.asList(existingGuidance);
    List<GuidanceData> newGuidanceList = Arrays.asList(newGuidance);

    BrdRules newRule = new BrdRules();
    newRule.setBrdId(TEST_BRD_ID);
    newRule.setBrdName("Test BRD");
    newRule.setRuleId("1013");
    newRule.setValue("test-value");
    newRule.setOrder("1");
    List<BrdRules> brdRulesList = Arrays.asList(newRule);

    // Mock blob storage and file reader
    when(blobStorageService.fetchFile(TEST_RULES_FILE)).thenReturn(Mono.just("data".getBytes()));
    when(blobStorageService.fetchFileFromUrl(anyString()))
        .thenReturn(Mono.just("rules".getBytes()));
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(GuidanceData.class)))
        .thenReturn(existingGuidanceList);
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(BrdRules.class)))
        .thenReturn(brdRulesList);

    // Mock repository and semantic matching
    when(legacyBRDRepository.save(any(LegacyBRD.class))).thenReturn(Mono.just(new LegacyBRD()));
    when(assistantService.findSemanticMatches(anyList())).thenReturn(Mono.just(newGuidanceList));
    when(blobStorageService.updateFile(anyString(), anyString())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(legacyBrdService.getRulesWithData(request))
        .expectNextMatches(ByteArrayResource.class::isInstance)
        .verifyComplete();

    verify(assistantService).findSemanticMatches(anyList());
    verify(blobStorageService).updateFile(anyString(), anyString());
  }

  @Test
  void prefillLegacyBRD_validBrdWithSites_returnsTrue() {
    // Given
    LegacyPrefillRequest request = new LegacyPrefillRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setDocumentName(TEST_DOCUMENT_NAME);

    LegacyBRD legacyBrd = new LegacyBRD();
    legacyBrd.setBrdId(TEST_BRD_ID);
    LegacyBRDInfo mainInfo = new LegacyBRDInfo();
    mainInfo.setId(TEST_BRD_ID);
    mainInfo.setName("Test BRD");
    legacyBrd.setMain(mainInfo);
    List<LegacyBRDInfo> sites = new ArrayList<>();
    LegacyBRDInfo siteInfo = new LegacyBRDInfo();
    siteInfo.setId("site-123");
    siteInfo.setName("Test Site");
    sites.add(siteInfo);
    legacyBrd.setSites(sites);

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdName("Test BRD");

    PrefillSections prefillSections = new PrefillSections();
    ObjectNode processedJson = objectMapper.createObjectNode();
    Api<BRDResponse> apiResponse = new Api<>();
    apiResponse.setData(Optional.of(brdResponse));

    when(legacyBRDRepository.findByBrdId(anyString())).thenReturn(Mono.just(legacyBrd));
    when(brdService.getBrdById(anyString())).thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(dtoModelMapper.mapResponseToPrefillSections(any())).thenReturn(prefillSections);
    when(assistantService.prefillBRDProcessJson(any(), anyList(), any(), anyString()))
        .thenReturn(Mono.just(processedJson));
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), any()))
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

    // Add mock for siteRepository
    Site siteEntity = new Site();
    siteEntity.setId("test-site-id");
    siteEntity.setBrdId(TEST_BRD_ID);
    siteEntity.setSiteId("site-123");
    siteEntity.setSiteName("Test Site");
    siteEntity.setCreatedAt(LocalDateTime.now());
    siteEntity.setUpdatedAt(LocalDateTime.now());
    when(siteRepository.findByBrdIdAndSiteId(anyString(), anyString()))
        .thenReturn(Mono.just(siteEntity));
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // When & Then
    StepVerifier.create(legacyBrdService.prefillLegacyBRD(request))
        .expectNext(true)
        .verifyComplete();

    // Verify interactions with atLeastOnce() since the method may call these multiple times
    verify(legacyBRDRepository, atLeastOnce()).findByBrdId(TEST_BRD_ID);
    verify(brdService, atLeastOnce()).getBrdById(TEST_BRD_ID);
    verify(dtoModelMapper, atLeastOnce()).mapResponseToPrefillSections(any());
    verify(assistantService, atLeastOnce())
        .prefillBRDProcessJson(any(), anyList(), any(), anyString());
    verify(brdService, atLeastOnce()).updateBrdPartiallyWithOrderedOperations(anyString(), any());
  }

  @Test
  void prefillLegacyBRD_noExistingLegacyBrd_returnsTrue() {
    // Given
    LegacyPrefillRequest request = new LegacyPrefillRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setDocumentName(TEST_DOCUMENT_NAME);

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdName("Test BRD");

    PrefillSections prefillSections = new PrefillSections();
    ObjectNode processedJson = objectMapper.createObjectNode();
    Api<BRDResponse> apiResponse = new Api<>();
    apiResponse.setData(Optional.of(brdResponse));

    when(legacyBRDRepository.findByBrdId(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdById(anyString())).thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(dtoModelMapper.mapResponseToPrefillSections(any(BRDResponse.class)))
        .thenReturn(prefillSections);
    when(assistantService.prefillBRDProcessJson(any(JsonNode.class), anyList()))
        .thenReturn(Mono.just(processedJson));
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), any(Map.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

    // When & Then
    StepVerifier.create(legacyBrdService.prefillLegacyBRD(request))
        .expectNext(true)
        .verifyComplete();

    verify(assistantService, never())
        .prefillBRDProcessJson(any(JsonNode.class), anyList(), any(ContextName.class), anyString());
    verify(assistantService).prefillBRDProcessJson(any(JsonNode.class), anyList());
  }

  @Test
  void prefillLegacyBRD_validBrdWithMultipleSites_returnsTrue() {
    // Given
    LegacyPrefillRequest request = new LegacyPrefillRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setDocumentName(TEST_DOCUMENT_NAME);

    LegacyBRD legacyBrd = new LegacyBRD();
    legacyBrd.setBrdId(TEST_BRD_ID);
    LegacyBRDInfo mainInfo = new LegacyBRDInfo();
    mainInfo.setId(TEST_BRD_ID);
    mainInfo.setName("Test BRD");
    legacyBrd.setMain(mainInfo);
    List<LegacyBRDInfo> sites = new ArrayList<>();
    LegacyBRDInfo siteInfo = new LegacyBRDInfo();
    siteInfo.setId("site-123");
    siteInfo.setName("Test Site");
    sites.add(siteInfo);
    legacyBrd.setSites(sites);

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdName("Test BRD");

    PrefillSections prefillSections = new PrefillSections();
    ObjectNode processedJson = objectMapper.createObjectNode();
    Api<BRDResponse> apiResponse = new Api<>();
    apiResponse.setData(Optional.of(brdResponse));

    Site siteEntity = new Site();
    siteEntity.setId("test-site-id");
    siteEntity.setBrdId(TEST_BRD_ID);
    siteEntity.setSiteId("site-123");
    siteEntity.setSiteName("Test Site");
    siteEntity.setCreatedAt(LocalDateTime.now());
    siteEntity.setUpdatedAt(LocalDateTime.now());

    when(legacyBRDRepository.findByBrdId(anyString())).thenReturn(Mono.just(legacyBrd));
    when(brdService.getBrdById(anyString())).thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(dtoModelMapper.mapResponseToPrefillSections(any(BRDResponse.class)))
        .thenReturn(prefillSections);
    when(assistantService.prefillBRDProcessJson(
            any(JsonNode.class), anyList(), any(ContextName.class), anyString()))
        .thenReturn(Mono.just(processedJson));
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), any(Map.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(siteRepository.findByBrdIdAndSiteId(anyString(), anyString()))
        .thenReturn(Mono.just(siteEntity));
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // When & Then
    StepVerifier.create(legacyBrdService.prefillLegacyBRD(request))
        .expectNext(true)
        .verifyComplete();

    // Verify interactions with atLeastOnce() since the method may call these multiple times
    verify(siteRepository, atLeastOnce()).findByBrdIdAndSiteId(anyString(), anyString());
    verify(siteRepository, atLeastOnce()).save(any(Site.class));
  }

  @Test
  void getStandardData_emptyFilePath_throwsIllegalArgumentException() {
    when(blobStorageService.fetchFileFromUrl(""))
        .thenReturn(Mono.error(new IllegalArgumentException("File path cannot be empty")));

    StepVerifier.create(legacyBrdService.getStandardData(""))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void getStandardData_nullFilePath_throwsIllegalArgumentException() {
    when(blobStorageService.fetchFileFromUrl(null))
        .thenReturn(Mono.error(new IllegalArgumentException("File path cannot be null")));

    StepVerifier.create(legacyBrdService.getStandardData(null))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void getRulesWithData_emptyBrdRulesList_throwsRuntimeException() {
    // Given
    LegacyBrdRequest request = new LegacyBrdRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setBrdRulesFileUrl("test-url");

    when(blobStorageService.fetchFile(any())).thenReturn(Mono.just("test".getBytes()));
    when(blobStorageService.fetchFileFromUrl(any())).thenReturn(Mono.just("test".getBytes()));
    when(fileReader.parseByteArrayContent(any(), any(), any())).thenReturn(Collections.emptyList());

    // When & Then
    StepVerifier.create(legacyBrdService.getRulesWithData(request))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void prefillLegacyBRD_invalidBrdId_returnsFalse() {
    // Given
    LegacyPrefillRequest request = new LegacyPrefillRequest();
    request.setBrdId("invalid-brd-id");
    request.setDocumentName("test-document");

    when(legacyBRDRepository.findByBrdId("invalid-brd-id")).thenReturn(Mono.empty());
    when(brdService.getBrdById("invalid-brd-id"))
        .thenReturn(Mono.error(new RuntimeException("BRD not found")));

    // When & Then
    StepVerifier.create(legacyBrdService.prefillLegacyBRD(request))
        .expectNext(false)
        .verifyComplete();

    verify(legacyBRDRepository).findByBrdId("invalid-brd-id");
    verify(brdService).getBrdById("invalid-brd-id");
  }

  @Test
  void prefillLegacyBRD_jsonProcessingError_returnsFalse() {
    // Given
    LegacyPrefillRequest request = new LegacyPrefillRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setDocumentName(TEST_DOCUMENT_NAME);

    BRDResponse brdResponse = new BRDResponse();
    Api<BRDResponse> apiResponse = new Api<>();
    apiResponse.setData(Optional.of(brdResponse));

    when(brdService.getBrdById(anyString())).thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(legacyBRDRepository.findByBrdId(anyString())).thenReturn(Mono.empty());
    when(dtoModelMapper.mapResponseToPrefillSections(any()))
        .thenThrow(new RuntimeException("Error processing JSON"));

    // When & Then
    StepVerifier.create(legacyBrdService.prefillLegacyBRD(request))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void getRulesWithData_emptyGuidanceData_returnsProcessedRules() {
    // Given
    LegacyBrdRequest request = new LegacyBrdRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setBrdRulesFileUrl("test-url");

    BrdRules mainBrdRule = new BrdRules();
    mainBrdRule.setBrdId(TEST_BRD_ID);
    mainBrdRule.setBrdName("Test BRD");
    mainBrdRule.setRuleId("1013");
    mainBrdRule.setValue("test-value");
    mainBrdRule.setOrder("1");
    List<BrdRules> brdRulesList = Collections.singletonList(mainBrdRule);

    when(blobStorageService.fetchFile(TEST_RULES_FILE)).thenReturn(Mono.just("test".getBytes()));
    when(blobStorageService.fetchFileFromUrl("test-url")).thenReturn(Mono.just("test".getBytes()));
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(GuidanceData.class)))
        .thenReturn(Collections.emptyList());
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(BrdRules.class)))
        .thenReturn(brdRulesList);
    when(legacyBRDRepository.save(any(LegacyBRD.class))).thenReturn(Mono.just(new LegacyBRD()));
    when(assistantService.findSemanticMatches(anyList()))
        .thenReturn(Mono.just(Collections.emptyList()));

    // When & Then
    StepVerifier.create(legacyBrdService.getRulesWithData(request))
        .expectNextMatches(
            resource -> {
              if (!(resource instanceof ByteArrayResource)) {
                return false;
              }
              try {
                byte[] content = ((ByteArrayResource) resource).getByteArray();
                String jsonStr = new String(content);
                return jsonStr.contains(TEST_BRD_ID) && jsonStr.contains("1013");
              } catch (Exception e) {
                return false;
              }
            })
        .verifyComplete();

    verify(blobStorageService).fetchFile(TEST_RULES_FILE);
    verify(blobStorageService).fetchFileFromUrl("test-url");
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(GuidanceData.class));
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(BrdRules.class));
    verify(legacyBRDRepository).save(any(LegacyBRD.class));
    verify(assistantService).findSemanticMatches(anyList());
  }

  @Test
  void getRulesWithData_validSiteProcessing_returnsProcessedRules() {
    // Given
    LegacyBrdRequest request = new LegacyBrdRequest();
    request.setBrdId(TEST_BRD_ID);
    request.setBrdRulesFileUrl("test-url");

    // Create main BRD rule (first in the list)
    BrdRules mainBrdRule = new BrdRules();
    mainBrdRule.setBrdId(TEST_BRD_ID);
    mainBrdRule.setBrdName("Test BRD");
    mainBrdRule.setRuleId("1013");
    mainBrdRule.setValue("test-value");
    mainBrdRule.setOrder("1");

    // Create site BRD rule
    BrdRules siteBrdRule = new BrdRules();
    siteBrdRule.setBrdId("site-123");
    siteBrdRule.setBrdName("Test Site");
    siteBrdRule.setRuleId("1013");
    siteBrdRule.setValue("different-value");

    // Create another non-site rule with "1013"
    BrdRules nonSiteBrdRule = new BrdRules();
    nonSiteBrdRule.setBrdId("non-site-123");
    nonSiteBrdRule.setBrdName("Non Site");
    nonSiteBrdRule.setRuleId("1013");
    nonSiteBrdRule.setValue("non-site-123");

    List<BrdRules> brdRulesList = Arrays.asList(mainBrdRule, siteBrdRule, nonSiteBrdRule);
    GuidanceData guidanceData = new GuidanceData();
    guidanceData.setRuleName("Test Rule");
    List<GuidanceData> guidanceDataList = Collections.singletonList(guidanceData);

    byte[] testBytes = "test".getBytes();
    when(blobStorageService.fetchFile(anyString())).thenReturn(Mono.just(testBytes));
    when(blobStorageService.fetchFileFromUrl(anyString())).thenReturn(Mono.just(testBytes));

    // Use specific matchers for parseByteArrayContent
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(GuidanceData.class)))
        .thenReturn(guidanceDataList);
    when(fileReader.parseByteArrayContent(
            any(byte[].class), any(FileType.class), eq(BrdRules.class)))
        .thenReturn(brdRulesList);

    when(legacyBRDRepository.save(any(LegacyBRD.class)))
        .thenAnswer(
            invocation -> {
              LegacyBRD savedBrd = invocation.getArgument(0);

              assert TEST_BRD_ID.equals(savedBrd.getBrdId()) : "Wrong BRD ID";
              assert savedBrd.getMain() != null : "Main BRD info is null";
              assert TEST_BRD_ID.equals(savedBrd.getMain().getId()) : "Wrong main BRD ID";
              assert "Test BRD".equals(savedBrd.getMain().getName()) : "Wrong main BRD name";
              assert savedBrd.getSites() != null : "Sites list is null";
              assert savedBrd.getSites().size() == 1 : "Wrong number of sites";
              LegacyBRDInfo site = savedBrd.getSites().get(0);
              assert "site-123".equals(site.getId()) : "Wrong site ID";
              assert "Test Site".equals(site.getName()) : "Wrong site name";

              return Mono.just(savedBrd);
            });

    when(assistantService.findSemanticMatches(anyList())).thenReturn(Mono.just(guidanceDataList));
    when(blobStorageService.updateFile(anyString(), anyString())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(legacyBrdService.getRulesWithData(request))
        .expectNextMatches(ByteArrayResource.class::isInstance)
        .verifyComplete();

    verify(blobStorageService).fetchFile(TEST_RULES_FILE);
    verify(blobStorageService).fetchFileFromUrl("test-url");
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(GuidanceData.class));
    verify(fileReader)
        .parseByteArrayContent(any(byte[].class), any(FileType.class), eq(BrdRules.class));
    verify(legacyBRDRepository).save(any(LegacyBRD.class));
  }

  @Test
  void processWithLegacyBrd_validBrdAndSites_returnsTrue() {
    // Given
    JsonNode prefillJson = objectMapper.createObjectNode();

    LegacyBRD legacyBrd = new LegacyBRD();
    legacyBrd.setBrdId(TEST_BRD_ID);
    LegacyBRDInfo mainInfo = new LegacyBRDInfo();
    mainInfo.setId(TEST_BRD_ID);
    mainInfo.setName("Test BRD");
    legacyBrd.setMain(mainInfo);
    List<LegacyBRDInfo> sites = new ArrayList<>();
    LegacyBRDInfo siteInfo = new LegacyBRDInfo();
    siteInfo.setId("site-123");
    siteInfo.setName("Test Site");
    sites.add(siteInfo);
    legacyBrd.setSites(sites);

    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);
    ObjectNode processedJson = objectMapper.createObjectNode();
    Api<BRDResponse> apiResponse = new Api<>();
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdName("Test BRD");
    apiResponse.setData(Optional.of(brdResponse));

    // Mock the assistant service call
    when(assistantService.prefillBRDProcessJson(
            any(JsonNode.class), anyList(), any(ContextName.class), anyString()))
        .thenReturn(Mono.just(processedJson));

    // Mock the BRD service update call
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), any()))
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

    // Mock site repository calls for site processing
    Site siteEntity = new Site();
    siteEntity.setId("test-site-id");
    siteEntity.setBrdId(TEST_BRD_ID);
    siteEntity.setSiteId("site-123");
    siteEntity.setSiteName("Test Site");
    siteEntity.setCreatedAt(LocalDateTime.now());
    siteEntity.setUpdatedAt(LocalDateTime.now());

    when(siteRepository.findByBrdIdAndSiteId(anyString(), anyString()))
        .thenReturn(Mono.just(siteEntity));
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // When & Then
    StepVerifier.create(
            Mono.from(
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "processWithLegacyBrd",
                    prefillJson,
                    TEST_BRD_ID,
                    legacyBrd,
                    documentNames)))
        .expectNext(true)
        .verifyComplete();

    // Verify interactions - using atLeastOnce() since the method may call these multiple times
    verify(assistantService, atLeastOnce())
        .prefillBRDProcessJson(any(JsonNode.class), anyList(), any(ContextName.class), anyString());
    verify(brdService, atLeastOnce()).updateBrdPartiallyWithOrderedOperations(anyString(), any());
    verify(siteRepository, atLeastOnce()).findByBrdIdAndSiteId(anyString(), anyString());
    verify(siteRepository, atLeastOnce()).save(any(Site.class));
  }

  @Test
  void processWithoutLegacyBrd_validBrdId_returnsTrue() {
    // Given
    JsonNode prefillJson = objectMapper.createObjectNode();
    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);
    ObjectNode processedJson = objectMapper.createObjectNode();
    Api<BRDResponse> apiResponse = new Api<>();
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdName("Test BRD");
    apiResponse.setData(Optional.of(brdResponse));

    when(assistantService.prefillBRDProcessJson(any(), anyList()))
        .thenReturn(Mono.just(processedJson));
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), any()))
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

    // When & Then
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "processWithoutLegacyBrd",
                    prefillJson,
                    TEST_BRD_ID,
                    documentNames))
        .expectNext(true)
        .verifyComplete();

    verify(assistantService).prefillBRDProcessJson(any(), anyList());
  }

  @Test
  void processSingleSite_validSiteAndDocuments_returnsTrue() {
    // Given
    Site siteEntity = new Site();
    siteEntity.setId("test-site-id");
    siteEntity.setBrdId(TEST_BRD_ID);
    siteEntity.setSiteId("site-123");
    siteEntity.setSiteName("Test Site");
    siteEntity.setCreatedAt(LocalDateTime.now());
    siteEntity.setUpdatedAt(LocalDateTime.now());

    JsonNode baseJsonNode = objectMapper.createObjectNode();
    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);
    ObjectNode processedJson = objectMapper.createObjectNode();

    when(assistantService.prefillBRDProcessJson(any(), anyList(), any(), anyString()))
        .thenReturn(Mono.just(processedJson));
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // When & Then
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService, "processSingleSite", siteEntity, baseJsonNode, documentNames))
        .expectNext(true)
        .verifyComplete();

    verify(siteRepository).save(any(Site.class));
  }

  @Test
  void updateSiteWithProcessedJson_validSiteAndJson_returnsTrue() {
    // Given
    ObjectNode processedJson = objectMapper.createObjectNode();
    Site siteEntity = new Site();
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // When & Then
    Object result =
        ReflectionTestUtils.invokeMethod(
            legacyBrdService, "updateSiteWithProcessedJson", siteEntity, processedJson);

    assertNotNull(result);
    assertTrue(result instanceof Mono<?>);

    @SuppressWarnings("unchecked")
    Mono<Boolean> resultMono = (Mono<Boolean>) result;

    StepVerifier.create(resultMono).expectNext(true).verifyComplete();

    // Verify interactions
    verify(siteRepository).save(any(Site.class));
  }

  @Test
  void createAndProcessSites_validSitesAndBrdForm_returnsTrue() {
    // Given
    List<LegacyBRDInfo> sites = new ArrayList<>();
    LegacyBRDInfo siteInfo = new LegacyBRDInfo();
    siteInfo.setId("site-123");
    siteInfo.setName("Test Site");
    sites.add(siteInfo);

    BrdForm brdForm = new BrdForm();
    GeneralImplementations genImpl = new GeneralImplementations();
    ImplementationNote note = new ImplementationNote();
    note.setDate("2024-03-20");
    note.setArea("General");
    note.setNote("Test Implementation");
    genImpl.setImplementationNotes(Collections.singletonList(note));
    brdForm.setGeneralImplementations(genImpl);

    ObjectNode cleanedJson = objectMapper.createObjectNode();
    ObjectNode generalImpl = objectMapper.createObjectNode();
    ArrayNode implNotes = objectMapper.createArrayNode();
    ObjectNode implNote = objectMapper.createObjectNode();
    implNote.put("date", "2024-03-20");
    implNote.put("area", "General");
    implNote.put("note", "Test Implementation");
    implNotes.add(implNote);
    generalImpl.set("implementationNotes", implNotes);
    cleanedJson.set("generalImplementations", generalImpl);

    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);
    Site siteEntity = new Site();
    siteEntity.setId("test-site-id");
    siteEntity.setBrdId(TEST_BRD_ID);
    siteEntity.setSiteId("site-123");
    siteEntity.setSiteName("Test Site");
    siteEntity.setCreatedAt(LocalDateTime.now());
    siteEntity.setUpdatedAt(LocalDateTime.now());
    siteEntity.setBrdForm(brdForm);

    // Mock initial site lookup to return empty (site doesn't exist)
    when(siteRepository.findByBrdIdAndSiteId(anyString(), anyString())).thenReturn(Mono.empty());

    // Mock site creation and update
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(siteEntity));

    // Mock the assistant service for site processing
    when(assistantService.prefillBRDProcessJson(
            any(JsonNode.class), anyList(), any(ContextName.class), anyString()))
        .thenReturn(Mono.just(cleanedJson));

    // When & Then
    StepVerifier.create(
            Mono.from(
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "createAndProcessSites",
                    TEST_BRD_ID,
                    sites,
                    brdForm,
                    cleanedJson,
                    documentNames)))
        .expectNext(true)
        .verifyComplete();

    // Verify interactions - using atLeastOnce() since the method may call these multiple times
    verify(siteRepository, atLeastOnce()).findByBrdIdAndSiteId(anyString(), anyString());
    verify(siteRepository, atLeastOnce()).save(any(Site.class));
    verify(assistantService, atLeastOnce())
        .prefillBRDProcessJson(any(JsonNode.class), anyList(), any(ContextName.class), anyString());
  }

  @Test
  void processSavedSites_emptySitesList_returnsTrue() {
    // Given
    List<Site> savedSites = Collections.emptyList();
    JsonNode cleanedJson = objectMapper.createObjectNode();
    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);

    // When & Then
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService, "processSavedSites", savedSites, cleanedJson, documentNames))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void handleConversionError_nullLegacyBrd_throwsNotFoundException() {
    // Given
    Exception testException = new RuntimeException("Test error");

    // When & Then
    try {
      StepVerifier.create(
              (Mono<Boolean>)
                  ReflectionTestUtils.invokeMethod(
                      legacyBrdService, "handleConversionError", testException, TEST_BRD_ID, null))
          .expectError(LegacyBrdService.LegacyBrdNotFoundException.class)
          .verify();
    } catch (Exception e) {
      // The exception is expected to be thrown directly, not as part of the Mono
      assert e instanceof LegacyBrdService.LegacyBrdNotFoundException;
      assert e.getMessage().equals("Legacy BRD data is not available for BRD ID: " + TEST_BRD_ID);
    }
  }

  @Test
  void processNewRules_emptyNewRulesList_returnsExistingGuidance() {
    // Given
    List<BrdRules> emptyNewRules = Collections.emptyList();
    List<GuidanceData> existingGuidanceData =
        Arrays.asList(
            new GuidanceData() {
              {
                setRuleName("Rule1");
                setMappingKey("Key1");
                setSimilarity("0.95");
                setExplanation("Test explanation");
                setQuestiondId("Q123");
              }
            },
            new GuidanceData() {
              {
                setRuleName("Rule2");
                setMappingKey("Key2");
                setSimilarity("0.95");
                setExplanation("Test explanation");
                setQuestiondId("Q123");
              }
            });

    // When & Then
    StepVerifier.create(
            (Mono<List<GuidanceData>>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService, "processNewRules", emptyNewRules, existingGuidanceData))
        .expectNext(existingGuidanceData)
        .verifyComplete();

    // Verify that assistantService was not called
    verify(assistantService, never()).findSemanticMatches(anyList());
  }

  @Test
  void shouldCreateCombinedRule_invalidRuleCases_returnsFalse() {
    // Given
    BrdRules invalidRule = new BrdRules();
    invalidRule.setRuleId("2000");
    invalidRule.setBrdId("test-brd");
    invalidRule.setBrdName("Test BRD");
    invalidRule.setRuleName("Test Rule");

    // When & Then
    Boolean result =
        (Boolean)
            ReflectionTestUtils.invokeMethod(
                legacyBrdService, "shouldCreateCombinedRule", invalidRule, null);
    assertFalse(result, "Should return false for invalid rule without mapping");
  }

  @Test
  void createCombinedRulesResource_jsonProcessingError_throwsRuntimeException()
      throws JsonProcessingException {
    // Given
    BrdRules brdRule = new BrdRules();
    brdRule.setBrdId(TEST_BRD_ID);
    brdRule.setBrdName("Test BRD");
    brdRule.setRuleId("1013");
    brdRule.setValue("test-value");
    brdRule.setOrder("1");
    List<BrdRules> brdRulesList = Collections.singletonList(brdRule);

    GuidanceData guidanceData = new GuidanceData();
    guidanceData.setRuleName("TestRule");
    guidanceData.setMappingKey("TestKey");
    guidanceData.setSimilarity("0.95");
    guidanceData.setExplanation("Test explanation");
    guidanceData.setQuestiondId("Q123");
    List<GuidanceData> guidanceDataList = Collections.singletonList(guidanceData);

    // Mock ObjectMapper to throw JsonProcessingException
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    ObjectWriter mockWriter = mock(ObjectWriter.class);
    when(mockMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
    when(mockWriter.writeValueAsBytes(any()))
        .thenThrow(new JsonProcessingException("Test JSON processing error") {});

    ReflectionTestUtils.setField(legacyBrdService, "objectMapper", mockMapper);

    // When & Then
    StepVerifier.create(
            (Mono<?>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "createCombinedRulesResource",
                    brdRulesList,
                    guidanceDataList))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Error creating JSON")
                    && throwable.getCause().getMessage().equals("Test JSON processing error"))
        .verify();

    // Reset the original ObjectMapper
    ReflectionTestUtils.setField(legacyBrdService, "objectMapper", objectMapper);
  }

  @Test
  void shouldCreateCombinedRule_validRuleCases_returnsTrue() {
    // Given
    BrdRules rule1013 = new BrdRules();
    rule1013.setRuleId("1013");

    BrdRules ruleWithNullId = new BrdRules();
    ruleWithNullId.setBrdId("test-brd");
    ruleWithNullId.setBrdName("Test BRD");
    ruleWithNullId.setRuleId(null);

    BrdRules ruleWithValidMapping = new BrdRules();
    ruleWithValidMapping.setRuleId("2000");

    GuidanceData validMapping = new GuidanceData();
    validMapping.setRuleName("Test Rule");
    validMapping.setMappingKey("TEST_KEY");
    validMapping.setSimilarity("0.95");
    validMapping.setExplanation("Test explanation");
    validMapping.setQuestiondId("Q123");

    // When & Then
    // Case 1: Rule with ID "1013"
    Boolean result1 =
        (Boolean)
            ReflectionTestUtils.invokeMethod(
                legacyBrdService, "shouldCreateCombinedRule", rule1013, null);
    assertTrue(result1, "Should return true for rule with ID 1013");

    // Case 2: Rule with null RuleId but valid BrdId and BrdName
    Boolean result2 =
        (Boolean)
            ReflectionTestUtils.invokeMethod(
                legacyBrdService, "shouldCreateCombinedRule", ruleWithNullId, null);
    assertTrue(result2, "Should return true for rule with null RuleId but valid BrdId and BrdName");

    // Case 3: Rule with valid mapping
    Boolean result3 =
        (Boolean)
            ReflectionTestUtils.invokeMethod(
                legacyBrdService, "shouldCreateCombinedRule", ruleWithValidMapping, validMapping);
    assertTrue(result3, "Should return true for rule with valid mapping");
  }

  @Test
  void processBrdResponse_nullOrEmptyResponse_returnsFalse() {
    // Given
    ResponseEntity<Api<BRDResponse>> nullResponse = ResponseEntity.ok(null);
    Api<BRDResponse> emptyApi = new Api<>();
    emptyApi.setData(Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);

    // When & Then
    // Test with null response
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "processBrdResponse",
                    nullResponse,
                    TEST_BRD_ID,
                    null,
                    TEST_DOCUMENT_NAME))
        .expectNext(false)
        .verifyComplete();

    // Test with empty response
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "processBrdResponse",
                    emptyResponse,
                    TEST_BRD_ID,
                    null,
                    TEST_DOCUMENT_NAME))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void convertJsonToMap_conversionException_returnsEmptyMap() {
    // Given
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    when(mockMapper.convertValue(any(JsonNode.class), eq(Map.class)))
        .thenThrow(new IllegalArgumentException("Test conversion error"));

    ReflectionTestUtils.setField(legacyBrdService, "objectMapper", mockMapper);

    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("testField", "testValue");

    // When
    Map<String, Object> result =
        (Map<String, Object>)
            ReflectionTestUtils.invokeMethod(legacyBrdService, "convertJsonToMap", jsonNode);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());

    // Reset the original ObjectMapper
    ReflectionTestUtils.setField(legacyBrdService, "objectMapper", objectMapper);
  }

  @Test
  void handleBrdUpdateResponse_nullOrEmptyResponse_returnsTrue() {
    // Given
    JsonNode cleanedJson = objectMapper.createObjectNode();
    List<String> documentNames = Collections.singletonList(TEST_DOCUMENT_NAME);

    // Test case 1: Null response body
    ResponseEntity<Api<BRDResponse>> nullResponse = ResponseEntity.ok(null);

    // Test case 2: Empty data in response
    Api<BRDResponse> emptyApi = new Api<>();
    emptyApi.setData(Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);

    // When & Then
    // Test with null response body
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "handleBrdUpdateResponse",
                    nullResponse,
                    cleanedJson,
                    null,
                    documentNames))
        .expectNext(true)
        .verifyComplete();

    // Test with empty data
    StepVerifier.create(
            (Mono<Boolean>)
                ReflectionTestUtils.invokeMethod(
                    legacyBrdService,
                    "handleBrdUpdateResponse",
                    emptyResponse,
                    cleanedJson,
                    null,
                    documentNames))
        .expectNext(true)
        .verifyComplete();
  }
}
