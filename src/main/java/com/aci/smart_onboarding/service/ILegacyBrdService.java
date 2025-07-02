package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.BrdRules;
import com.aci.smart_onboarding.dto.GuidanceData;
import com.aci.smart_onboarding.dto.LegacyBrdRequest;
import com.aci.smart_onboarding.dto.LegacyPrefillRequest;
import java.util.List;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

/** Interface for legacy BRD operations */
public interface ILegacyBrdService {
  /**
   * Retrieves standard data from a file
   *
   * @param filePath The path to the file containing standard data
   * @return A Mono containing a list of GuidanceData
   */
  Mono<List<GuidanceData>> getStandardData(String filePath);

  /**
   * Retrieves user rules from a file
   *
   * @param filePath The path to the file containing user rules
   * @return A Mono containing a list of BrdRules
   */
  Mono<List<BrdRules>> getUserRules(String filePath);

  /**
   * Retrieves rules combined with their corresponding data
   *
   * @param request The LegacyBrdRequest containing the BRD rules file URL
   * @return A Mono containing a Resource with the combined rules and data
   */
  Mono<Resource> getRulesWithData(LegacyBrdRequest request);

  /**
   * Prefills a legacy BRD by fetching BRD data and converting it to PrefillSections
   *
   * @param request The LegacyPrefillRequest containing brdId and documentName
   * @return A Mono containing a Boolean indicating success or failure
   */
  Mono<Boolean> prefillLegacyBRD(LegacyPrefillRequest request);
}
