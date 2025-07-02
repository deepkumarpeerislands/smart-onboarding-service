package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.UATConfiguratorRequestDTO;
import com.aci.smart_onboarding.dto.UATConfiguratorResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.UATConfigurator;
import com.aci.smart_onboarding.repository.UATConfiguratorRepository;
import com.aci.smart_onboarding.service.IUATConfiguratorService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Implementation of UAT Configurator service operations. */
@Service
@RequiredArgsConstructor
public class UATConfiguratorService implements IUATConfiguratorService {

  private final UATConfiguratorRepository repository;

  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName);
  }

  @Override
  public Mono<UATConfiguratorResponseDTO> createConfiguration(
      UATConfiguratorRequestDTO requestDTO) {
    return getUsernameFromContext()
        .flatMap(
            username -> {
              UATConfigurator configurator = convertToEntity(requestDTO);
              configurator.setCreatedBy(username);
              configurator.setCreatedAt(LocalDateTime.now());
              return repository.save(configurator).map(this::convertToResponseDTO);
            });
  }

  @Override
  public Mono<UATConfiguratorResponseDTO> updateConfiguration(
      String uatId, UATConfiguratorRequestDTO requestDTO) {
    return getUsernameFromContext()
        .flatMap(
            username ->
                repository
                    .findById(uatId)
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId)))
                    .flatMap(
                        existing -> {
                          UATConfigurator updated = convertToEntity(requestDTO);
                          updated.setUatId(uatId);
                          updated.setCreatedBy(existing.getCreatedBy());
                          updated.setCreatedAt(existing.getCreatedAt());
                          updated.setUpdatedBy(username);
                          updated.setUpdatedAt(LocalDateTime.now());
                          return repository.save(updated);
                        })
                    .map(this::convertToResponseDTO));
  }

  @Override
  public Mono<Void> deleteConfiguration(String uatId) {
    return repository
        .findById(uatId)
        .switchIfEmpty(Mono.error(new NotFoundException(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId)))
        .flatMap(repository::delete);
  }

  @Override
  public Mono<UATConfiguratorResponseDTO> getConfiguration(String uatId) {
    return repository
        .findById(uatId)
        .switchIfEmpty(Mono.error(new NotFoundException(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId)))
        .map(this::convertToResponseDTO);
  }

  @Override
  public Flux<UATConfiguratorResponseDTO> getAllConfigurations() {
    return repository.findAll().map(this::convertToResponseDTO);
  }

  @Override
  public Flux<UATConfiguratorResponseDTO> getConfigurationsByType(PortalTypes type) {
    return repository.findByType(type).map(this::convertToResponseDTO);
  }

  @Override
  public Flux<UATConfiguratorResponseDTO> getConfigurationsByNames(
      List<String> configurationNames) {
    return repository.findByConfigurationNameIn(configurationNames).map(this::convertToResponseDTO);
  }

  private UATConfigurator convertToEntity(UATConfiguratorRequestDTO dto) {
    return UATConfigurator.builder()
        .type(dto.getType())
        .configurationName(dto.getConfigurationName())
        .fields(dto.getFields())
        .position(dto.getPosition())
        .scenario(dto.getScenario())
        .build();
  }

  private UATConfiguratorResponseDTO convertToResponseDTO(UATConfigurator entity) {
    return UATConfiguratorResponseDTO.builder()
        .uatId(entity.getUatId())
        .type(entity.getType())
        .configurationName(entity.getConfigurationName())
        .fields(entity.getFields())
        .position(entity.getPosition())
        .scenario(entity.getScenario())
        .createdBy(entity.getCreatedBy())
        .createdAt(entity.getCreatedAt())
        .updatedBy(entity.getUpdatedBy())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
