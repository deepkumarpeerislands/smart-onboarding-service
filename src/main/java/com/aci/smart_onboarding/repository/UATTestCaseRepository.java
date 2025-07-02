package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.model.UATTestCase;
import java.util.List;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Repository for UAT test case operations. */
@Repository
public interface UATTestCaseRepository extends ReactiveMongoRepository<UATTestCase, String> {

  /**
   * Find all test cases by BRD ID.
   *
   * @param brdId the BRD ID
   * @return flux of matching test cases
   */
  Flux<UATTestCase> findByBrdId(String brdId);

  /**
   * Find all test cases by status.
   *
   * @param status the test status
   * @return flux of matching test cases
   */
  Flux<UATTestCase> findByStatus(TestStatus status);

  /**
   * Find all test cases by BRD ID and UAT type.
   *
   * @param brdId the BRD ID
   * @param uatType the portal type
   * @return flux of matching test cases
   */
  Flux<UATTestCase> findByBrdIdAndUatType(String brdId, PortalTypes uatType);

  /**
   * Find all test cases by feature name.
   *
   * @param featureName the feature name
   * @return flux of matching test cases
   */
  Flux<UATTestCase> findByFeatureName(String featureName);

  /**
   * Find all test cases by BRD ID and feature names.
   *
   * @param brdId the BRD ID
   * @param featureNames list of feature names to filter by
   * @return flux of matching test cases
   */
  Flux<UATTestCase> findByBrdIdAndFeatureNameIn(String brdId, List<String> featureNames);
}
