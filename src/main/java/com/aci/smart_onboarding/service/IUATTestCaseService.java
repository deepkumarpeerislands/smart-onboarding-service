package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Service interface for UAT test case operations. */
public interface IUATTestCaseService {

  /**
   * Create a new UAT test case.
   *
   * @param requestDTO the test case to create
   * @return the created test case
   */
  Mono<UATTestCaseRequestResponseDTO> createTestCase(UATTestCaseRequestResponseDTO requestDTO);

  /**
   * Update an existing UAT test case.
   *
   * @param id the ID of the test case to update
   * @param testCaseDTO the updated test case
   * @return the updated test case
   */
  Mono<UATTestCaseDTO> updateTestCase(String id, UATTestCaseDTO testCaseDTO);

  /**
   * Delete a UAT test case.
   *
   * @param id the ID of the test case to delete
   * @return empty mono on successful deletion
   */
  Mono<Void> deleteTestCase(String id);

  /**
   * Get a UAT test case by ID.
   *
   * @param id the ID of the test case
   * @return the test case if found
   */
  Mono<UATTestCaseDTO> getTestCase(String id);

  /**
   * Get all UAT test cases.
   *
   * @return flux of all test cases
   */
  Flux<UATTestCaseDTO> getAllTestCases();

  /**
   * Get all test cases by BRD ID.
   *
   * @param brdId the BRD ID
   * @return flux of matching test cases
   */
  Flux<UATTestCaseDTO> getTestCasesByBrdId(String brdId);

  /**
   * Get all test cases by status.
   *
   * @param status the test status
   * @return flux of matching test cases
   */
  Flux<UATTestCaseDTO> getTestCasesByStatus(TestStatus status);

  /**
   * Get all test cases by BRD ID and UAT type.
   *
   * @param brdId the BRD ID
   * @param uatType the portal type
   * @return flux of matching test cases
   */
  Flux<UATTestCaseDTO> getTestCasesByBrdIdAndUatType(String brdId, PortalTypes uatType);

  /**
   * Get all test cases by feature name.
   *
   * @param featureName the feature name
   * @return flux of matching test cases
   */
  Flux<UATTestCaseDTO> getTestCasesByFeatureName(String featureName);

  /**
   * Perform semantic search on test cases using vector similarity with portal type filter.
   *
   * @param vector The vector to search with
   * @param limit Maximum number of results to return
   * @param uatType Portal type to filter by
   * @return Flux of matching test cases ordered by vector similarity
   */
  Flux<UATTestCaseDTO> performVectorSearch(List<Double> vector, int limit, PortalTypes uatType);

  /** Retrieves test cases by BRD ID and feature names. */
  Flux<UATTestCaseDTO> getTestCasesByBrdIdAndFeatureNames(String brdId, List<String> featureNames);
}
