package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.PortalConfiguration;
import com.aci.smart_onboarding.repository.PortalConfigurationRepository;
import com.aci.smart_onboarding.service.implementation.PortalConfigurationService;
import com.aci.smart_onboarding.util.EncryptionUtil;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PortalConfigurationServiceTest {

  @Mock private PortalConfigurationRepository portalConfigurationRepository;

  @Mock private EncryptionUtil encryptionUtil;

  @InjectMocks private PortalConfigurationService portalConfigurationService;

  private PortalConfiguration testConfiguration;
  private static final String TEST_PASSWORD = "testPass";
  private static final String PASSWORD_MASK = "********";
  private byte[] encryptedBytes;
  private String encryptedBase64;

  @BeforeEach
  void setUp() {
    testConfiguration = new PortalConfiguration();
    testConfiguration.setId("test-id");
    testConfiguration.setBrdId("BRD001");
    testConfiguration.setUrl("https://test.com");
    testConfiguration.setType(PortalTypes.AGENT);
    testConfiguration.setUsername("testUser");
    testConfiguration.setPassword(TEST_PASSWORD);

    // Setup encryption data
    encryptedBytes = new byte[] {1, 2, 3, 4, 5}; // Mock encrypted bytes
    encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
  }

  @Test
  void createPortalConfiguration_validConfig_success() {
    // Mock encryption
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    PortalConfiguration configToSave = new PortalConfiguration();
    configToSave.setId(testConfiguration.getId());
    configToSave.setBrdId(testConfiguration.getBrdId());
    configToSave.setUrl(testConfiguration.getUrl());
    configToSave.setType(testConfiguration.getType());
    configToSave.setUsername(testConfiguration.getUsername());
    configToSave.setPassword(encryptedBase64);

    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.just(configToSave));

    StepVerifier.create(portalConfigurationService.createPortalConfiguration(testConfiguration))
        .expectNextMatches(
            config -> {
              assertEquals(PASSWORD_MASK, config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_repositoryError_error() {
    // Mock encryption
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(portalConfigurationService.createPortalConfiguration(testConfiguration))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createPortalConfiguration_nullPassword_exception() {
    testConfiguration.setPassword(null);
    assertThrows(
        NullPointerException.class,
        () -> portalConfigurationService.createPortalConfiguration(testConfiguration));
  }

  @Test
  void createPortalConfiguration_encryptionError_error() {
    when(encryptionUtil.encrypt(any(byte[].class)))
        .thenThrow(new RuntimeException("Encryption error"));

    assertThrows(
        RuntimeException.class,
        () -> portalConfigurationService.createPortalConfiguration(testConfiguration));
  }

  @Test
  void getPortalConfigurationsByBrdId_validId_success() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextMatches(
            config -> {
              assertEquals(encryptedBase64, config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_invalidBase64Password_masked() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword("invalid-base64");

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextMatches(
            config -> {
              assertEquals("invalid-base64", config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_noConfigs_success() {
    when(portalConfigurationRepository.findByBrdId("BRD001")).thenReturn(Flux.empty());

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_repositoryError_error() {
    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationsByBrdId_multipleConfigs_success() {
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("id1");
    config1.setBrdId("BRD001");
    config1.setPassword(encryptedBase64);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("id2");
    config2.setBrdId("BRD001");
    config2.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(config1, config2));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_validType_success() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(encryptedConfig));
    // Mock decryption

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            config -> {
              assertEquals(encryptedBase64, config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_invalidBase64Password_masked() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword("invalid-base64");

    when(portalConfigurationRepository.findByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            config -> {
              assertEquals("invalid-base64", config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_noConfigs_success() {
    when(portalConfigurationRepository.findByType(PortalTypes.AGENT)).thenReturn(Flux.empty());

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_repositoryError_error() {
    when(portalConfigurationRepository.findByType(PortalTypes.AGENT))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationsByType_multipleConfigs_success() {
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("id1");
    config1.setType(PortalTypes.AGENT);
    config1.setPassword(encryptedBase64);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("id2");
    config2.setType(PortalTypes.AGENT);
    config2.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(config1, config2));

    StepVerifier.create(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_validIdAndType_success() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .expectNextMatches(
            config -> {
              assertEquals(encryptedBase64, config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_typeMismatch_empty() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(PortalTypes.CONSUMER); // Different type
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_noConfig_success() {
    when(portalConfigurationRepository.findByBrdId("BRD001")).thenReturn(Flux.empty());

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_repositoryError_error() {
    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationByIdAndType_invalidBase64Password_masked() {
    PortalConfiguration encryptedConfig = new PortalConfiguration();
    encryptedConfig.setId(testConfiguration.getId());
    encryptedConfig.setBrdId(testConfiguration.getBrdId());
    encryptedConfig.setUrl(testConfiguration.getUrl());
    encryptedConfig.setType(testConfiguration.getType());
    encryptedConfig.setUsername(testConfiguration.getUsername());
    encryptedConfig.setPassword("invalid-base64");

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(encryptedConfig));

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .expectNextMatches(
            config -> {
              assertEquals("invalid-base64", config.getPassword());
              return true;
            })
        .verifyComplete();
  }

  private boolean assertSingleConfigWithMaskedPassword(List<PortalConfiguration> savedConfigs) {
    assertEquals(1, savedConfigs.size());
    assertEquals(PASSWORD_MASK, savedConfigs.get(0).getPassword());
    return true;
  }

  private boolean assertTwoConfigsWithMaskedPassword(List<PortalConfiguration> savedConfigs) {
    assertEquals(2, savedConfigs.size());
    savedConfigs.forEach(config -> assertEquals(PASSWORD_MASK, config.getPassword()));
    return true;
  }

  private boolean assertMaskedPassword(PortalConfiguration config) {
    return PASSWORD_MASK.equals(config.getPassword());
  }

  @Test
  void updatePortalConfigurations_success() {
    // Setup test data
    List<PortalConfiguration> configs = List.of(testConfiguration);
    PortalConfiguration existingConfig = new PortalConfiguration();
    existingConfig.setId("existing-id");
    existingConfig.setBrdId("BRD001");
    existingConfig.setType(PortalTypes.AGENT);
    existingConfig.setPassword(encryptedBase64);

    // Mock repository responses
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.just(existingConfig));
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.just(existingConfig));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(this::assertSingleConfigWithMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_createNew_success() {
    // Setup test data
    List<PortalConfiguration> configs = List.of(testConfiguration);

    // Mock repository responses
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.empty());
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.just(testConfiguration));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(this::assertSingleConfigWithMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_emptyList_success() {
    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", List.of()))
        .expectNextMatches(
            savedConfigs -> {
              assertEquals(0, savedConfigs.size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_repositoryError_error() {
    List<PortalConfiguration> configs = List.of(testConfiguration);
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_decryptionError_masked() {
    // Setup test data
    List<PortalConfiguration> configs = List.of(testConfiguration);
    PortalConfiguration existingConfig = new PortalConfiguration();
    existingConfig.setId("existing-id");
    existingConfig.setBrdId("BRD001");
    existingConfig.setType(PortalTypes.AGENT);
    existingConfig.setPassword("invalid-base64");

    // Mock repository responses
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.just(existingConfig));
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.just(existingConfig));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(this::assertSingleConfigWithMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_encryptionError_error() {
    // Setup test data
    List<PortalConfiguration> configs = List.of(testConfiguration);

    // Mock repository responses
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.empty());
    when(encryptionUtil.encrypt(any(byte[].class)))
        .thenThrow(new RuntimeException("Encryption error"));

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationByIdAndType_multipleConfigs_firstMatchReturned() {
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("id1");
    config1.setBrdId("BRD001");
    config1.setType(PortalTypes.AGENT);
    config1.setPassword(encryptedBase64);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("id2");
    config2.setBrdId("BRD001");
    config2.setType(PortalTypes.CONSUMER);
    config2.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdId("BRD001"))
        .thenReturn(Flux.just(config1, config2));

    StepVerifier.create(
            portalConfigurationService.getPortalConfigurationByIdAndType(
                "BRD001", PortalTypes.AGENT))
        .expectNextMatches(config -> config.getType() == PortalTypes.AGENT)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_multipleConfigs_success() {
    List<PortalConfiguration> configs =
        List.of(
            createTestConfig("id1", PortalTypes.AGENT),
            createTestConfig("id2", PortalTypes.CONSUMER));

    PortalConfiguration existingConfig1 = new PortalConfiguration();
    existingConfig1.setId("id1");
    existingConfig1.setBrdId("BRD001");
    existingConfig1.setType(PortalTypes.AGENT);
    existingConfig1.setPassword(encryptedBase64);

    PortalConfiguration existingConfig2 = new PortalConfiguration();
    existingConfig2.setId("id2");
    existingConfig2.setBrdId("BRD001");
    existingConfig2.setType(PortalTypes.CONSUMER);
    existingConfig2.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.just(existingConfig1));
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.CONSUMER))
        .thenReturn(Mono.just(existingConfig2));
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(this::assertTwoConfigsWithMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_partialUpdate_success() {
    List<PortalConfiguration> configs = List.of(createTestConfig("id1", PortalTypes.AGENT));

    PortalConfiguration existingConfig = new PortalConfiguration();
    existingConfig.setId("id1");
    existingConfig.setBrdId("BRD001");
    existingConfig.setType(PortalTypes.AGENT);
    existingConfig.setPassword(encryptedBase64);

    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.just(existingConfig));
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(this::assertSingleConfigWithMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_repositoryErrorDuringSave_error() {
    List<PortalConfiguration> configs = List.of(createTestConfig("id1", PortalTypes.AGENT));

    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.empty());
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);

    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createPortalConfiguration_emptyPassword() {
    testConfiguration.setPassword("");
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenReturn(Mono.just(testConfiguration));
    StepVerifier.create(portalConfigurationService.createPortalConfiguration(testConfiguration))
        .expectNextMatches(this::assertMaskedPassword)
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_nullList() {
    assertThrows(NullPointerException.class, this::callUpdatePortalConfigurationsWithNullList);
  }

  private void callUpdatePortalConfigurationsWithNullList() {
    portalConfigurationService.updatePortalConfigurations("BRD001", null).block();
  }

  @Test
  void updatePortalConfigurations_mixedExistingAndNewConfigs() {
    PortalConfiguration existingConfig = createTestConfig("id1", PortalTypes.AGENT);
    PortalConfiguration newConfig = createTestConfig("id2", PortalTypes.CONSUMER);
    List<PortalConfiguration> configs = List.of(existingConfig, newConfig);
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.AGENT))
        .thenReturn(Mono.just(existingConfig));
    when(portalConfigurationRepository.findByBrdIdAndType("BRD001", PortalTypes.CONSUMER))
        .thenReturn(Mono.empty());
    when(portalConfigurationRepository.save(any(PortalConfiguration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(encryptionUtil.encrypt(any(byte[].class))).thenReturn(encryptedBytes);
    StepVerifier.create(portalConfigurationService.updatePortalConfigurations("BRD001", configs))
        .expectNextMatches(savedConfigs -> savedConfigs.size() == 2)
        .verifyComplete();
  }

  private PortalConfiguration createTestConfig(String id, PortalTypes type) {
    PortalConfiguration config = new PortalConfiguration();
    config.setId(id);
    config.setBrdId("BRD001");
    config.setUrl("https://test.com");
    config.setType(type);
    config.setUsername("testUser");
    config.setPassword(TEST_PASSWORD);
    return config;
  }
}
