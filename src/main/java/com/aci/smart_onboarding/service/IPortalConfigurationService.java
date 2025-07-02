package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.PortalConfiguration;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IPortalConfigurationService {
  Mono<PortalConfiguration> createPortalConfiguration(PortalConfiguration configuration);

  Flux<PortalConfiguration> getPortalConfigurationsByBrdId(String brdId);

  Flux<PortalConfiguration> getPortalConfigurationsByType(PortalTypes type);

  Mono<PortalConfiguration> getPortalConfigurationByIdAndType(String id, PortalTypes type);

  /**
   * Updates portal configurations for a BRD
   *
   * @param brdId The BRD ID
   * @param configurations List of portal configurations to update
   * @return Mono containing the list of updated configurations
   */
  Mono<List<PortalConfiguration>> updatePortalConfigurations(
      String brdId, java.util.List<PortalConfiguration> configurations);
}
