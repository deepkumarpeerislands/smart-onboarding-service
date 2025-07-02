package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.UATTestCase;
import com.aci.smart_onboarding.repository.UATTestCaseRepository;
import com.aci.smart_onboarding.service.IUATTestCaseService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Implementation of UAT test case service operations. */
@Service
@RequiredArgsConstructor
public class UATTestCaseService implements IUATTestCaseService {

  private final UATTestCaseRepository repository;
  private final DtoModelMapper mapper;
  private final AIService aiService;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  private static final String VECTOR_INDEX = "uat_test_case_vector_index";
  private static final String VECTOR_PATH = "vectors";
  private static final int DEFAULT_NUM_CANDIDATES = 150;

  @Override
  public Mono<UATTestCaseRequestResponseDTO> createTestCase(
      UATTestCaseRequestResponseDTO requestDTO) {
    UATTestCase testCase = mapper.mapToUATTestCase(requestDTO);
    String contextForEmbedding =
        String.format("%s %s", testCase.getScenario(), testCase.getPosition());

    return aiService
        .getEmbeddings(contextForEmbedding)
        .flatMap(
            vectors -> {
              testCase.setVectors(vectors);
              return repository.save(testCase);
            })
        .map(mapper::mapToUATTestCaseRequestResponseDTO);
  }

  @Override
  public Mono<UATTestCaseDTO> updateTestCase(String id, UATTestCaseDTO testCaseDTO) {
    return repository
        .findById(id)
        .switchIfEmpty(
            Mono.defer(
                () -> Mono.error(new NotFoundException(BrdConstants.UAT_TEST_CASE_NOT_FOUND + id))))
        .flatMap(
            existing -> {
              UATTestCase updated = mapper.mapToUATTestCase(testCaseDTO);
              updated.setId(id);
              return repository.save(updated);
            })
        .map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Mono<Void> deleteTestCase(String id) {
    return repository
        .findById(id)
        .switchIfEmpty(
            Mono.defer(
                () -> Mono.error(new NotFoundException(BrdConstants.UAT_TEST_CASE_NOT_FOUND + id))))
        .flatMap(repository::delete);
  }

  @Override
  public Mono<UATTestCaseDTO> getTestCase(String id) {
    return repository
        .findById(id)
        .switchIfEmpty(
            Mono.defer(
                () -> Mono.error(new NotFoundException(BrdConstants.UAT_TEST_CASE_NOT_FOUND + id))))
        .map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> getAllTestCases() {
    return repository.findAll().map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> getTestCasesByBrdId(String brdId) {
    return repository.findByBrdId(brdId).map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> getTestCasesByStatus(TestStatus status) {
    return repository.findByStatus(status).map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> getTestCasesByBrdIdAndUatType(String brdId, PortalTypes uatType) {
    return repository.findByBrdIdAndUatType(brdId, uatType).map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> getTestCasesByFeatureName(String featureName) {
    return repository.findByFeatureName(featureName).map(mapper::mapToUATTestCaseDTO);
  }

  @Override
  public Flux<UATTestCaseDTO> performVectorSearch(
      List<Double> vector, int limit, PortalTypes uatType) {
    Map<String, Object> vectorSearchQuery = new HashMap<>();
    vectorSearchQuery.put("index", VECTOR_INDEX);
    vectorSearchQuery.put("path", VECTOR_PATH);
    vectorSearchQuery.put("queryVector", vector);
    vectorSearchQuery.put("numCandidates", DEFAULT_NUM_CANDIDATES);
    vectorSearchQuery.put("limit", limit);
    vectorSearchQuery.put("similarity", "euclidean");

    Map<String, Object> filter = new HashMap<>();
    filter.put("uatType", uatType.toString());
    vectorSearchQuery.put("filter", new Document(filter));

    Document vectorSearchStage = new Document("$vectorSearch", new Document(vectorSearchQuery));
    AggregationOperation aggregationOperation = context -> vectorSearchStage;

    TypedAggregation<UATTestCase> aggregation =
        Aggregation.newAggregation(UATTestCase.class, aggregationOperation)
            .withOptions(AggregationOptions.builder().allowDiskUse(true).build());

    return reactiveMongoTemplate
        .aggregate(aggregation, UATTestCase.class, UATTestCase.class)
        .map(mapper::mapToUATTestCaseDTO);
  }

  /**
   * Retrieves test cases by BRD ID and feature names.
   *
   * @param brdId The BRD identifier
   * @param featureNames List of feature names to filter by
   * @return Flux of UATTestCaseDTO containing matching test cases
   */
  @Override
  public Flux<UATTestCaseDTO> getTestCasesByBrdIdAndFeatureNames(
      String brdId, List<String> featureNames) {
    return repository
        .findByBrdIdAndFeatureNameIn(brdId, featureNames)
        .map(mapper::mapToUATTestCaseDTO);
  }
}
