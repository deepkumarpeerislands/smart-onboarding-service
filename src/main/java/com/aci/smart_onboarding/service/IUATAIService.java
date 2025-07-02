package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import java.util.List;
import reactor.core.publisher.Flux;

public interface IUATAIService {
  /**
   * Generates UAT test cases for the given BRD ID and configuration names.
   *
   * @param brdId The BRD identifier
   * @param configurationNames List of configuration names
   * @param uatType The type of UAT (AGENT/CONSUMER)
   * @return Flux of generated test cases
   */
  Flux<UATTestCaseRequestResponseDTO> generateUATTestCases(
      String brdId, List<String> configurationNames, PortalTypes uatType);

  /**
   * Retests features for the given BRD ID and configuration names.
   *
   * @param brdId The BRD identifier
   * @param configurationNames List of configuration names
   * @param uatType The type of UAT (AGENT/CONSUMER)
   * @return Flux of test cases
   */
  Flux<UATTestCaseRequestResponseDTO> retestFeatures(
      String brdId, List<String> configurationNames, PortalTypes uatType);
}
