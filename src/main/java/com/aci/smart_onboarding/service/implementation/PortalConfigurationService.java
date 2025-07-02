package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.PortalConfiguration;
import com.aci.smart_onboarding.repository.PortalConfigurationRepository;
import com.aci.smart_onboarding.service.IPortalConfigurationService;
import com.aci.smart_onboarding.util.EncryptionUtil;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PortalConfigurationService implements IPortalConfigurationService {

  private final PortalConfigurationRepository portalConfigurationRepository;
  private final EncryptionUtil encryptionUtil;
  private static final String PASSWORD_MASK = "********";

  @Override
  public Mono<PortalConfiguration> createPortalConfiguration(PortalConfiguration configuration) {
    // Encrypt password before saving
    String originalPassword = configuration.getPassword();
    byte[] passwordBytes = originalPassword.getBytes(StandardCharsets.UTF_8);
    byte[] encryptedBytes = encryptionUtil.encrypt(passwordBytes);
    String encryptedPassword = Base64.getEncoder().encodeToString(encryptedBytes);

    configuration.setPassword(encryptedPassword);

    return portalConfigurationRepository
        .save(configuration)
        .map(
            savedConfig -> {
              // Mask password in response
              savedConfig.setPassword(PASSWORD_MASK);
              return savedConfig;
            });
  }

  @Override
  public Flux<PortalConfiguration> getPortalConfigurationsByBrdId(String brdId) {
    return portalConfigurationRepository.findByBrdId(brdId);
  }

  @Override
  public Flux<PortalConfiguration> getPortalConfigurationsByType(PortalTypes type) {
    return portalConfigurationRepository.findByType(type);
  }

  @Override
  public Mono<PortalConfiguration> getPortalConfigurationByIdAndType(String id, PortalTypes type) {
    return portalConfigurationRepository
        .findByBrdId(id)
        .filter(config -> config.getType() == type)
        .next();
  }

  @Override
  public Mono<List<PortalConfiguration>> updatePortalConfigurations(
      String brdId, List<PortalConfiguration> configurations) {
    return Flux.fromIterable(configurations)
        .flatMap(
            config -> {
              // Set the brdId for each configuration
              config.setBrdId(brdId);

              // Find existing configuration by brdId and type
              return portalConfigurationRepository
                  .findByBrdIdAndType(brdId, config.getType())
                  .flatMap(
                      existingConfig -> {
                        // Update existing configuration
                        existingConfig.setUrl(config.getUrl());
                        existingConfig.setUsername(config.getUsername());

                        // Encrypt password before saving
                        String originalPassword = config.getPassword();
                        byte[] passwordBytes = originalPassword.getBytes(StandardCharsets.UTF_8);
                        byte[] encryptedBytes = encryptionUtil.encrypt(passwordBytes);
                        String encryptedPassword =
                            Base64.getEncoder().encodeToString(encryptedBytes);
                        existingConfig.setPassword(encryptedPassword);

                        return portalConfigurationRepository
                            .save(existingConfig)
                            .map(
                                savedConfig -> {
                                  // Mask password in response
                                  savedConfig.setPassword(PASSWORD_MASK);
                                  return savedConfig;
                                });
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            // If no existing configuration found, create new one
                            String originalPassword = config.getPassword();
                            byte[] passwordBytes =
                                originalPassword.getBytes(StandardCharsets.UTF_8);
                            byte[] encryptedBytes = encryptionUtil.encrypt(passwordBytes);
                            String encryptedPassword =
                                Base64.getEncoder().encodeToString(encryptedBytes);
                            config.setPassword(encryptedPassword);

                            return portalConfigurationRepository
                                .save(config)
                                .map(
                                    savedConfig -> {
                                      // Mask password in response
                                      savedConfig.setPassword(PASSWORD_MASK);
                                      return savedConfig;
                                    });
                          }));
            })
        .collectList();
  }
}
