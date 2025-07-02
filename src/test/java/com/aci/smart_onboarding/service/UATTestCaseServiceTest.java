package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.UATTestCase;
import com.aci.smart_onboarding.repository.UATTestCaseRepository;
import com.aci.smart_onboarding.service.implementation.AIService;
import com.aci.smart_onboarding.service.implementation.UATTestCaseService;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UATTestCaseServiceTest {

  @Mock private UATTestCaseRepository repository;

  @Mock private DtoModelMapper mapper;

  @Mock private AIService aiService;

  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;

  @Captor private ArgumentCaptor<TypedAggregation<UATTestCase>> aggregationCaptor;

  @InjectMocks private UATTestCaseService service;

  private UATTestCase testCase;
  private UATTestCaseDTO testCaseDTO;

  @BeforeEach
  void setUp() {
    testCase = createTestCase();
    testCaseDTO = createTestCaseDTO();
  }

  @Test
  void getTestCasesByFeatureName_validFeature_success() {
    String featureName = "Login Feature";
    UATTestCase localTestCase = createTestCase();
    UATTestCaseDTO localTestCaseDTO = createTestCaseDTO();

    when(repository.findByFeatureName(featureName)).thenReturn(Flux.just(localTestCase));
    when(mapper.mapToUATTestCaseDTO(localTestCase)).thenReturn(localTestCaseDTO);

    StepVerifier.create(service.getTestCasesByFeatureName(featureName))
        .expectNext(localTestCaseDTO)
        .verifyComplete();
  }

  @Test
  void getTestCasesByFeatureName_nonExistentFeature_empty() {
    String featureName = "NonExistent Feature";

    when(repository.findByFeatureName(featureName)).thenReturn(Flux.empty());

    StepVerifier.create(service.getTestCasesByFeatureName(featureName)).verifyComplete();
  }

  @Test
  void createTestCase_validRequest_success() {
    // Given
    UATTestCaseRequestResponseDTO localRequestDTO = createRequestDTO();
    UATTestCase localTestCase = createTestCase();
    localTestCase.setVectors(List.of(0.1, 0.2, 0.3)); // Add vectors for AI service

    // When
    when(mapper.mapToUATTestCase(any(UATTestCaseRequestResponseDTO.class)))
        .thenReturn(localTestCase);
    when(aiService.getEmbeddings(anyString())).thenReturn(Mono.just(List.of(0.1, 0.2, 0.3)));
    when(repository.save(any(UATTestCase.class))).thenReturn(Mono.just(localTestCase));
    when(mapper.mapToUATTestCaseRequestResponseDTO(any(UATTestCase.class)))
        .thenReturn(localRequestDTO);

    // Then
    StepVerifier.create(service.createTestCase(localRequestDTO))
        .expectNextMatches(
            response -> {
              assertEquals(localRequestDTO.getBrdId(), response.getBrdId());
              assertEquals(localRequestDTO.getTestName(), response.getTestName());
              assertEquals(localRequestDTO.getScenario(), response.getScenario());
              assertEquals(localRequestDTO.getPosition(), response.getPosition());
              assertEquals(localRequestDTO.getUatType(), response.getUatType());
              assertEquals(localRequestDTO.getTestType(), response.getTestType());
              assertEquals(localRequestDTO.getStatus(), response.getStatus());
              return true;
            })
        .verifyComplete();

    verify(repository).save(any(UATTestCase.class));
    verify(aiService).getEmbeddings(anyString());
  }

  @Test
  void updateTestCase_validId_success() {
    when(repository.findById(anyString())).thenReturn(Mono.just(testCase));
    when(mapper.mapToUATTestCase(any(UATTestCaseDTO.class))).thenReturn(testCase);
    when(repository.save(any())).thenReturn(Mono.just(testCase));
    when(mapper.mapToUATTestCaseDTO(any())).thenReturn(testCaseDTO);

    StepVerifier.create(service.updateTestCase("test-id", testCaseDTO))
        .expectNext(testCaseDTO)
        .verifyComplete();

    verify(repository).save(testCase);
  }

  @Test
  void updateTestCase_nonExistentId_notFound() {
    when(repository.findById(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(service.updateTestCase("non-existent-id", testCaseDTO))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error
                        .getMessage()
                        .equals(BrdConstants.UAT_TEST_CASE_NOT_FOUND + "non-existent-id"))
        .verify();
  }

  @Test
  void performVectorSearch_validInput_success() {
    // Given
    List<Double> queryVector = List.of(0.1, 0.2, 0.3);
    int limit = 5;
    UATTestCase localTestCase = createTestCase();
    UATTestCaseDTO localTestCaseDTO = createTestCaseDTO();

    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(UATTestCase.class), eq(UATTestCase.class)))
        .thenReturn(Flux.just(localTestCase));
    when(mapper.mapToUATTestCaseDTO(localTestCase)).thenReturn(localTestCaseDTO);

    // When
    StepVerifier.create(service.performVectorSearch(queryVector, limit, PortalTypes.AGENT))
        .expectNextCount(1)
        .verifyComplete();

    // Then
    verify(reactiveMongoTemplate)
        .aggregate(aggregationCaptor.capture(), eq(UATTestCase.class), eq(UATTestCase.class));

    TypedAggregation<UATTestCase> aggregation = aggregationCaptor.getValue();
    assertTrue(aggregation.toString().contains("uat_test_case_vector_index"));
    assertTrue(aggregation.toString().contains("vectors"));
    assertTrue(aggregation.toString().contains("euclidean"));
  }

  @Test
  void performVectorSearch_noResults_empty_case1() {
    // Given
    List<Double> queryVector = List.of(0.1, 0.2, 0.3);
    int limit = 5;

    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(UATTestCase.class), eq(UATTestCase.class)))
        .thenReturn(Flux.empty());

    // When/Then
    StepVerifier.create(service.performVectorSearch(queryVector, limit, PortalTypes.AGENT))
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdIdAndFeatureNames_validInput_success() {
    // Given
    String brdId = "BRD001";
    List<String> featureNames = List.of("Feature1", "Feature2");
    UATTestCase localTestCase1 = createTestUATTestCase("1", brdId, "Feature1");
    UATTestCase localTestCase2 = createTestUATTestCase("2", brdId, "Feature2");
    UATTestCaseDTO dto1 = UATTestCaseDTO.builder().brdId(brdId).featureName("Feature1").build();
    UATTestCaseDTO dto2 = UATTestCaseDTO.builder().brdId(brdId).featureName("Feature2").build();
    List<UATTestCase> testCases = List.of(localTestCase1, localTestCase2);

    // When
    when(repository.findByBrdIdAndFeatureNameIn(brdId, featureNames))
        .thenReturn(Flux.fromIterable(testCases));
    when(mapper.mapToUATTestCaseDTO(localTestCase1)).thenReturn(dto1);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    // Then
    StepVerifier.create(service.getTestCasesByBrdIdAndFeatureNames(brdId, featureNames))
        .expectNext(dto1)
        .expectNext(dto2)
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdIdAndFeatureNames_noResults_empty() {
    // Given
    String brdId = "BRD001";
    List<String> featureNames = List.of("Feature1", "Feature2");

    // When
    when(repository.findByBrdIdAndFeatureNameIn(brdId, featureNames)).thenReturn(Flux.empty());

    // Then
    StepVerifier.create(service.getTestCasesByBrdIdAndFeatureNames(brdId, featureNames))
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdIdAndFeatureNames_repositoryError_database_case1() {
    // Given
    String brdId = "BRD001";
    List<String> featureNames = List.of("Feature1", "Feature2");

    // When
    when(repository.findByBrdIdAndFeatureNameIn(brdId, featureNames))
        .thenReturn(Flux.error(new RuntimeException("Database Error")));

    // Then
    StepVerifier.create(service.getTestCasesByBrdIdAndFeatureNames(brdId, featureNames))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void performVectorSearch_multipleResults_success() {
    // Given
    List<Double> vector = List.of(0.1, 0.2, 0.3);
    int limit = 5;
    UATTestCase localTestCase1 = createTestUATTestCase("1", "BRD001", "Feature1");
    UATTestCase localTestCase2 = createTestUATTestCase("2", "BRD001", "Feature2");
    UATTestCaseDTO dto1 = UATTestCaseDTO.builder().brdId("BRD001").featureName("Feature1").build();
    UATTestCaseDTO dto2 = UATTestCaseDTO.builder().brdId("BRD001").featureName("Feature2").build();
    List<UATTestCase> testCases = List.of(localTestCase1, localTestCase2);

    // When
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(UATTestCase.class), eq(UATTestCase.class)))
        .thenReturn(Flux.fromIterable(testCases));
    when(mapper.mapToUATTestCaseDTO(localTestCase1)).thenReturn(dto1);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    // Then
    StepVerifier.create(service.performVectorSearch(vector, limit, PortalTypes.CONSUMER))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void performVectorSearch_noResults_empty_case2() {
    // Given
    List<Double> vector = List.of(0.1, 0.2, 0.3);
    int limit = 5;

    // When
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(UATTestCase.class), eq(UATTestCase.class)))
        .thenReturn(Flux.empty());

    // Then
    StepVerifier.create(service.performVectorSearch(vector, limit, PortalTypes.CONSUMER))
        .verifyComplete();
  }

  @Test
  void performVectorSearch_repositoryError_vectorSearch() {
    // Given
    List<Double> vector = List.of(0.1, 0.2, 0.3);
    int limit = 5;

    // When
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(UATTestCase.class), eq(UATTestCase.class)))
        .thenReturn(Flux.error(new RuntimeException("Vector Search Error")));

    // Then
    StepVerifier.create(service.performVectorSearch(vector, limit, PortalTypes.CONSUMER))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createTestCase_nullVectors_success() {
    // Given
    UATTestCaseRequestResponseDTO requestDTO = createRequestDTO();
    UATTestCase localTestCase = createTestCase();
    localTestCase.setVectors(null);

    // When
    when(mapper.mapToUATTestCase(requestDTO)).thenReturn(localTestCase);
    when(aiService.getEmbeddings(anyString())).thenReturn(Mono.just(List.of(0.1, 0.2, 0.3)));
    when(repository.save(any(UATTestCase.class))).thenReturn(Mono.just(localTestCase));
    when(mapper.mapToUATTestCaseRequestResponseDTO(localTestCase)).thenReturn(requestDTO);

    // Then
    StepVerifier.create(service.createTestCase(requestDTO)).expectNext(requestDTO).verifyComplete();
  }

  @Test
  void createTestCase_embeddingError_error() {
    // Given
    UATTestCaseRequestResponseDTO requestDTO = createRequestDTO();
    UATTestCase localTestCase = createTestCase();

    // When
    when(mapper.mapToUATTestCase(requestDTO)).thenReturn(localTestCase);
    when(aiService.getEmbeddings(anyString()))
        .thenReturn(Mono.error(new RuntimeException("AI Service Error")));

    // Then
    StepVerifier.create(service.createTestCase(requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createTestCase_saveError_error() {
    // Given
    UATTestCaseRequestResponseDTO requestDTO = createRequestDTO();
    UATTestCase localTestCase = createTestCase();

    // When
    when(mapper.mapToUATTestCase(requestDTO)).thenReturn(localTestCase);
    when(aiService.getEmbeddings(anyString())).thenReturn(Mono.just(List.of(0.1, 0.2, 0.3)));
    when(repository.save(any(UATTestCase.class)))
        .thenReturn(Mono.error(new RuntimeException("Save Error")));

    // Then
    StepVerifier.create(service.createTestCase(requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void deleteTestCase_validId_success() {
    when(repository.findById(anyString())).thenReturn(Mono.just(testCase));
    when(repository.delete(any())).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteTestCase("test-id")).verifyComplete();

    verify(repository).delete(testCase);
  }

  @Test
  void deleteTestCase_nonExistentId_notFound() {
    when(repository.findById(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteTestCase("non-existent-id"))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error
                        .getMessage()
                        .equals(BrdConstants.UAT_TEST_CASE_NOT_FOUND + "non-existent-id"))
        .verify();
  }

  @Test
  void getTestCase_validId_success() {
    when(repository.findById(anyString())).thenReturn(Mono.just(testCase));
    when(mapper.mapToUATTestCaseDTO(any())).thenReturn(testCaseDTO);

    StepVerifier.create(service.getTestCase("test-id")).expectNext(testCaseDTO).verifyComplete();
  }

  @Test
  void getTestCase_nonExistentId_notFound() {
    when(repository.findById(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(service.getTestCase("non-existent-id"))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error
                        .getMessage()
                        .equals(BrdConstants.UAT_TEST_CASE_NOT_FOUND + "non-existent-id"))
        .verify();
  }

  @Test
  void getAllTestCases_multipleResults_success() {
    UATTestCase localTestCase2 = createTestCase();
    localTestCase2.setId("test-id-2");
    UATTestCaseDTO dto2 = createTestCaseDTO();
    dto2.setId("test-id-2");

    when(repository.findAll()).thenReturn(Flux.just(testCase, localTestCase2));
    when(mapper.mapToUATTestCaseDTO(testCase)).thenReturn(testCaseDTO);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    StepVerifier.create(service.getAllTestCases())
        .expectNext(testCaseDTO)
        .expectNext(dto2)
        .verifyComplete();
  }

  @Test
  void getAllTestCases_noResults_empty() {
    when(repository.findAll()).thenReturn(Flux.empty());

    StepVerifier.create(service.getAllTestCases()).verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_validId_success() {
    UATTestCase localTestCase2 = createTestCase();
    localTestCase2.setId("test-id-2");
    UATTestCaseDTO dto2 = createTestCaseDTO();
    dto2.setId("test-id-2");

    when(repository.findByBrdId(anyString())).thenReturn(Flux.just(testCase, localTestCase2));
    when(mapper.mapToUATTestCaseDTO(testCase)).thenReturn(testCaseDTO);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    StepVerifier.create(service.getTestCasesByBrdId("BRD-123"))
        .expectNext(testCaseDTO)
        .expectNext(dto2)
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_noResults_empty() {
    when(repository.findByBrdId(anyString())).thenReturn(Flux.empty());

    StepVerifier.create(service.getTestCasesByBrdId("BRD-123")).verifyComplete();
  }

  @Test
  void getTestCasesByStatus_validStatus_success() {
    UATTestCase localTestCase2 = createTestCase();
    localTestCase2.setId("test-id-2");
    UATTestCaseDTO dto2 = createTestCaseDTO();
    dto2.setId("test-id-2");

    when(repository.findByStatus(any())).thenReturn(Flux.just(testCase, localTestCase2));
    when(mapper.mapToUATTestCaseDTO(testCase)).thenReturn(testCaseDTO);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    StepVerifier.create(service.getTestCasesByStatus(TestStatus.PASSED))
        .expectNext(testCaseDTO)
        .expectNext(dto2)
        .verifyComplete();
  }

  @Test
  void getTestCasesByStatus_noResults_empty() {
    when(repository.findByStatus(any())).thenReturn(Flux.empty());

    StepVerifier.create(service.getTestCasesByStatus(TestStatus.PASSED)).verifyComplete();
  }

  @Test
  void getTestCasesByBrdIdAndUatType_validInput_success() {
    UATTestCase localTestCase2 = createTestCase();
    localTestCase2.setId("test-id-2");
    UATTestCaseDTO dto2 = createTestCaseDTO();
    dto2.setId("test-id-2");

    when(repository.findByBrdIdAndUatType(anyString(), any()))
        .thenReturn(Flux.just(testCase, localTestCase2));
    when(mapper.mapToUATTestCaseDTO(testCase)).thenReturn(testCaseDTO);
    when(mapper.mapToUATTestCaseDTO(localTestCase2)).thenReturn(dto2);

    StepVerifier.create(service.getTestCasesByBrdIdAndUatType("BRD-123", PortalTypes.AGENT))
        .expectNext(testCaseDTO)
        .expectNext(dto2)
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdIdAndUatType_noResults_empty() {
    when(repository.findByBrdIdAndUatType(anyString(), any())).thenReturn(Flux.empty());

    StepVerifier.create(service.getTestCasesByBrdIdAndUatType("BRD-123", PortalTypes.AGENT))
        .verifyComplete();
  }

  @Test
  void getTestCasesByFeatureName_repositoryError_database_case1() {
    // Given
    String featureName = "Login Feature";

    // When
    when(repository.findByFeatureName(featureName))
        .thenReturn(Flux.error(new RuntimeException("Database Error")));

    // Then
    StepVerifier.create(service.getTestCasesByFeatureName(featureName))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getTestCasesByBrdIdAndFeatureNames_repositoryError_database_case2() {
    // Given
    String brdId = "BRD001";
    List<String> featureNames = List.of("Feature1", "Feature2");
    String errorMessage = "Connection timeout while accessing database";

    // When
    when(repository.findByBrdIdAndFeatureNameIn(brdId, featureNames))
        .thenReturn(Flux.error(new RuntimeException(errorMessage)));

    // Then
    StepVerifier.create(service.getTestCasesByBrdIdAndFeatureNames(brdId, featureNames))
        .expectErrorMatches(
            error -> error instanceof RuntimeException && error.getMessage().equals(errorMessage))
        .verify();
  }

  private UATTestCase createTestCase() {
    UATTestCase uatTestCase = new UATTestCase();
    uatTestCase.setId("test-id");
    uatTestCase.setBrdId("BRD-123");
    uatTestCase.setTestName("Login Test");
    uatTestCase.setScenario("Verify login functionality");
    uatTestCase.setPosition("top-right");
    uatTestCase.setStatus(TestStatus.PASSED);
    uatTestCase.setUatType(PortalTypes.AGENT);
    uatTestCase.setTestType(TestType.NORMAL);
    uatTestCase.setComments("Test passed successfully");
    uatTestCase.setFeatureName("Login Feature");
    uatTestCase.setFields(new HashMap<>());
    return uatTestCase;
  }

  private UATTestCaseDTO createTestCaseDTO() {
    return UATTestCaseDTO.builder()
        .id("test-id")
        .brdId("BRD-123")
        .testName("Login Test")
        .scenario("Verify login functionality")
        .position("top-right")
        .answer("Login successful")
        .uatType(PortalTypes.AGENT)
        .testType(TestType.NORMAL)
        .status(TestStatus.PASSED)
        .comments("Test passed successfully")
        .featureName("Login Feature")
        .fields(new HashMap<>())
        .build();
  }

  private UATTestCaseRequestResponseDTO createRequestDTO() {
    return UATTestCaseRequestResponseDTO.builder()
        .brdId("BRD-123")
        .testName("Login Test")
        .scenario("Verify login functionality")
        .position("top-right")
        .answer("Login successful")
        .uatType(PortalTypes.AGENT)
        .testType(TestType.NORMAL)
        .status(TestStatus.PASSED)
        .comments("Test passed successfully")
        .fields(new HashMap<>())
        .build();
  }

  private UATTestCase createTestUATTestCase(String id, String brdId, String featureName) {
    UATTestCase uatTestCase = new UATTestCase();
    uatTestCase.setId(id);
    uatTestCase.setBrdId(brdId);
    uatTestCase.setFeatureName(featureName);
    uatTestCase.setScenario("Test Scenario");
    uatTestCase.setPosition("Test Position");
    uatTestCase.setStatus(TestStatus.PASSED);
    uatTestCase.setUatType(PortalTypes.AGENT);
    return uatTestCase;
  }
}
