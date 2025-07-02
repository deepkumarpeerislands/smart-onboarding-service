package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.UATConfiguratorRequestDTO;
import com.aci.smart_onboarding.dto.UATConfiguratorResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Service interface for UAT Configurator operations. */
public interface IUATConfiguratorService {

  /**
   * Create a new UAT configuration.
   *
   * @param requestDTO the configuration to create
   * @return the created configuration
   */
  Mono<UATConfiguratorResponseDTO> createConfiguration(UATConfiguratorRequestDTO requestDTO);

  /**
   * Update an existing UAT configuration.
   *
   * @param uatId the ID of the configuration to update
   * @param requestDTO the updated configuration
   * @return the updated configuration
   */
  Mono<UATConfiguratorResponseDTO> updateConfiguration(
      String uatId, UATConfiguratorRequestDTO requestDTO);

  /**
   * Delete a UAT configuration.
   *
   * @param uatId the ID of the configuration to delete
   * @return empty mono on successful deletion
   */
  Mono<Void> deleteConfiguration(String uatId);

  /**
   * Get a UAT configuration by ID.
   *
   * @param uatId the ID of the configuration
   * @return the configuration if found
   */
  Mono<UATConfiguratorResponseDTO> getConfiguration(String uatId);

  /**
   * Get all UAT configurations.
   *
   * @return flux of all configurations
   */
  Flux<UATConfiguratorResponseDTO> getAllConfigurations();

  /**
   * Get all UAT configurations by portal type.
   *
   * @param type the portal type
   * @return flux of matching configurations
   */
  Flux<UATConfiguratorResponseDTO> getConfigurationsByType(PortalTypes type);

  /**
   * Get UAT configurations by configuration names.
   *
   * @param configurationNames list of configuration names to fetch
   * @return flux of matching configurations
   */
  Flux<UATConfiguratorResponseDTO> getConfigurationsByNames(List<String> configurationNames);
}
