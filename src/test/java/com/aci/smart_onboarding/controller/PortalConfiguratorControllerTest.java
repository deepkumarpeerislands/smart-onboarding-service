package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.PortalConfigConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.PortalConfigurationRequest;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.PortalConfiguration;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IPortalConfigurationService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PortalConfiguratorControllerTest {

  @Mock private IPortalConfigurationService portalConfigurationService;

  @Mock private BRDSecurityService brdSecurityService;

  @Mock private BRDRepository brdRepository;

  @InjectMocks private PortalConfigurationController portalConfiguratorController;

  private PortalConfiguration testConfiguration;
  private PortalConfigurationRequest testRequest;
  private static final String PASSWORD_MASK = "********";

  @BeforeEach
  void setUp() {
    testConfiguration = new PortalConfiguration();
    testConfiguration.setId("test-id");
    testConfiguration.setBrdId("BRD001");
    testConfiguration.setUrl("https://test.com");
    testConfiguration.setType(PortalTypes.AGENT);
    testConfiguration.setUsername("testUser");
    testConfiguration.setPassword(PASSWORD_MASK);

    testRequest = new PortalConfigurationRequest();
    testRequest.setBrdId("BRD001");
    testRequest.setUrl("https://test.com");
    testRequest.setType(PortalTypes.AGENT);
    testRequest.setUsername("testUser");
    testRequest.setPassword("testPass");
  }

  @Test
  void createPortalConfiguration_validRequest_success_case1() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.just(testConfiguration));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIG_CREATED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(testConfiguration, api.getData().get().get(0));
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_accessDenied_returnsForbidden() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(false));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("Access denied: Only the creator PM can access this endpoint.", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertTrue(api.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_serviceError_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.error(new RuntimeException("Database error")));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(testRequest)))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createPortalConfiguration_emptyRequest_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    
    PortalConfigurationRequest emptyRequest = new PortalConfigurationRequest();

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(emptyRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("BRD ID is required", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertEquals("BRD ID cannot be null", api.getErrors().get().get("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_emptyFields_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));

    PortalConfigurationRequest emptyRequest = new PortalConfigurationRequest();
    emptyRequest.setBrdId("BRD001"); // BRD ID is required
    emptyRequest.setUrl("");
    emptyRequest.setType(PortalTypes.AGENT);
    emptyRequest.setUsername("");
    emptyRequest.setPassword("");

    PortalConfiguration emptyConfig = new PortalConfiguration();
    emptyConfig.setId("test-id");
    emptyConfig.setBrdId("BRD001");
    emptyConfig.setUrl("");
    emptyConfig.setType(PortalTypes.AGENT);
    emptyConfig.setUsername("");
    emptyConfig.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.just(emptyConfig));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(emptyRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertTrue(api.getData().isPresent());
              assertEquals("BRD001", api.getData().get().get(0).getBrdId());
              assertEquals("", api.getData().get().get(0).getUrl());
              assertEquals(PASSWORD_MASK, api.getData().get().get(0).getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_validId_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .thenReturn(Flux.just(testConfiguration));

    StepVerifier.create(portalConfiguratorController.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIGS_RETRIEVED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(1, api.getData().get().size());
              assertEquals(testConfiguration, api.getData().get().get(0));
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_noConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .thenReturn(Flux.empty());

    StepVerifier.create(portalConfiguratorController.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIGS_RETRIEVED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertTrue(api.getData().get().isEmpty());
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_validType_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just("pm@gmail.com"));
    when(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(testConfiguration));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIGS_RETRIEVED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(1, api.getData().get().size());
              assertEquals(testConfiguration, api.getData().get().get(0));
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_noConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just("pm@gmail.com"));
    when(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .thenReturn(Flux.empty());

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIGS_RETRIEVED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertTrue(api.getData().get().isEmpty());
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_validIdAndType_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(portalConfigurationService.getPortalConfigurationByIdAndType("test-id", PortalTypes.AGENT))
        .thenReturn(Mono.just(testConfiguration));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationByIdAndType(
                "test-id", PortalTypes.AGENT))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<PortalConfiguration> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIGS_RETRIEVED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(testConfiguration, api.getData().get());
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_notFound_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.getPortalConfigurationByIdAndType("test-id", PortalTypes.AGENT))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationByIdAndType(
                "test-id", PortalTypes.AGENT))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<PortalConfiguration> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("Portal configuration not found", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertTrue(api.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_serviceError_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.getPortalConfigurationByIdAndType("test-id", PortalTypes.AGENT))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationByIdAndType(
                "test-id", PortalTypes.AGENT))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationsByBrdId_multipleConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("test-id-1");
    config1.setBrdId("BRD001");
    config1.setType(PortalTypes.AGENT);
    config1.setPassword(PASSWORD_MASK);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("test-id-2");
    config2.setBrdId("BRD001");
    config2.setType(PortalTypes.CONSUMER);
    config2.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .thenReturn(Flux.just(config1, config2));

    StepVerifier.create(portalConfiguratorController.getPortalConfigurationsByBrdId("BRD001"))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertTrue(api.getData().isPresent());
              assertEquals(2, api.getData().get().size());
              assertEquals(PortalTypes.AGENT, api.getData().get().get(0).getType());
              assertEquals(PortalTypes.CONSUMER, api.getData().get().get(1).getType());
              assertEquals(PASSWORD_MASK, api.getData().get().get(0).getPassword());
              assertEquals(PASSWORD_MASK, api.getData().get().get(1).getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_multipleConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just("pm@gmail.com"));
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("test-id-1");
    config1.setBrdId("BRD001");
    config1.setType(PortalTypes.AGENT);
    config1.setPassword(PASSWORD_MASK);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("test-id-2");
    config2.setBrdId("BRD002");
    config2.setType(PortalTypes.AGENT);
    config2.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(config1, config2));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(brdRepository.findByBrdId("BRD002"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD002").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertTrue(api.getData().isPresent());
              assertEquals(2, api.getData().get().size());
              assertTrue(
                  api.getData().get().stream().allMatch(c -> c.getType() == PortalTypes.AGENT));
              assertTrue(
                  api.getData().get().stream()
                      .allMatch(c -> PASSWORD_MASK.equals(c.getPassword())));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationByIdAndType_variousTypes_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    for (PortalTypes type : PortalTypes.values()) {
      PortalConfiguration config = new PortalConfiguration();
      config.setId("test-id");
      config.setBrdId("BRD001");
      config.setType(type);
      config.setPassword(PASSWORD_MASK);

      when(portalConfigurationService.getPortalConfigurationByIdAndType("test-id", type))
          .thenReturn(Mono.just(config));

      StepVerifier.create(
              portalConfiguratorController.getPortalConfigurationByIdAndType("test-id", type))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                Api<PortalConfiguration> api = response.getBody();
                assertNotNull(api);
                assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
                assertTrue(api.getData().isPresent());
                assertEquals(type, api.getData().get().getType());
                assertEquals(PASSWORD_MASK, api.getData().get().getPassword());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void getPortalConfigurationByIdAndType_variousIds_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    List<String> ids = List.of("test-id-1", "test-id-2", "test-id-3");

    for (String id : ids) {
      PortalConfiguration config = new PortalConfiguration();
      config.setId(id);
      config.setBrdId("BRD001");
      config.setType(PortalTypes.AGENT);
      config.setPassword(PASSWORD_MASK);

      when(portalConfigurationService.getPortalConfigurationByIdAndType(id, PortalTypes.AGENT))
          .thenReturn(Mono.just(config));

      StepVerifier.create(
              portalConfiguratorController.getPortalConfigurationByIdAndType(id, PortalTypes.AGENT))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                Api<PortalConfiguration> api = response.getBody();
                assertNotNull(api);
                assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
                assertTrue(api.getData().isPresent());
                assertEquals(id, api.getData().get().getId());
                assertEquals(PASSWORD_MASK, api.getData().get().getPassword());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void createPortalConfiguration_invalidData_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));

    PortalConfigurationRequest invalidRequest = new PortalConfigurationRequest();
    invalidRequest.setBrdId("BRD001");
    invalidRequest.setUrl("invalid-url");
    invalidRequest.setType(PortalTypes.AGENT);
    invalidRequest.setUsername("testUser");
    invalidRequest.setPassword("testPass");

    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid URL format")));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(invalidRequest)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createPortalConfiguration_validRequest_success_case2() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));

    PortalConfigurationRequest createRequest = new PortalConfigurationRequest();
    createRequest.setBrdId("BRD001");
    createRequest.setUrl("https://test.com");
    createRequest.setType(PortalTypes.AGENT);
    createRequest.setUsername("testUser");
    createRequest.setPassword("testPass");

    PortalConfiguration expectedConfig = new PortalConfiguration();
    expectedConfig.setBrdId(createRequest.getBrdId());
    expectedConfig.setUrl(createRequest.getUrl());
    expectedConfig.setType(createRequest.getType());
    expectedConfig.setUsername(createRequest.getUsername());
    expectedConfig.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.just(expectedConfig));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(createRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertTrue(api.getData().isPresent());
              assertEquals(expectedConfig, api.getData().get().get(0));
              assertEquals(PASSWORD_MASK, api.getData().get().get(0).getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_accessDenied_returnsForbidden() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(false));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("Access denied: Only the creator PM can access this endpoint.", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertTrue(api.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(List.of(testConfiguration)));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertTrue(api.getData().isPresent());
              assertEquals(testConfiguration, api.getData().get().get(0));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_withPmRole_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(List.of(testConfiguration)));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Portal configurations updated successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(1, api.getData().get().size());
              assertEquals(testConfiguration, api.getData().get().get(0));
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_emptyList_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(Collections.emptyList()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "BRD001", Collections.emptyList()))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Portal configurations updated successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertTrue(api.getData().get().isEmpty());
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_serviceError_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_multipleConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("test-id-1");
    config1.setBrdId("BRD001");
    config1.setType(PortalTypes.AGENT);
    config1.setPassword(PASSWORD_MASK);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("test-id-2");
    config2.setBrdId("BRD001");
    config2.setType(PortalTypes.CONSUMER);
    config2.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(List.of(config1, config2)));

    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Portal configurations updated successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(2, api.getData().get().size());
              assertEquals(PortalTypes.AGENT, api.getData().get().get(0).getType());
              assertEquals(PortalTypes.CONSUMER, api.getData().get().get(1).getType());
              assertEquals(PASSWORD_MASK, api.getData().get().get(0).getPassword());
              assertEquals(PASSWORD_MASK, api.getData().get().get(1).getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_multipleConfigs_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfigurationRequest request1 = new PortalConfigurationRequest();
    request1.setBrdId("BRD001");
    request1.setUrl("https://test1.com");
    request1.setType(PortalTypes.AGENT);
    request1.setUsername("user1");
    request1.setPassword("pass1");

    PortalConfigurationRequest request2 = new PortalConfigurationRequest();
    request2.setBrdId("BRD001");
    request2.setUrl("https://test2.com");
    request2.setType(PortalTypes.CONSUMER);
    request2.setUsername("user2");
    request2.setPassword("pass2");

    PortalConfiguration config1 = new PortalConfiguration();
    config1.setId("test-id-1");
    config1.setBrdId("BRD001");
    config1.setUrl("https://test1.com");
    config1.setType(PortalTypes.AGENT);
    config1.setUsername("user1");
    config1.setPassword(PASSWORD_MASK);

    PortalConfiguration config2 = new PortalConfiguration();
    config2.setId("test-id-2");
    config2.setBrdId("BRD001");
    config2.setUrl("https://test2.com");
    config2.setType(PortalTypes.CONSUMER);
    config2.setUsername("user2");
    config2.setPassword(PASSWORD_MASK);

    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.just(config1))
        .thenReturn(Mono.just(config2));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(request1, request2)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals(PortalConfigConstants.CONFIG_CREATED, api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals(2, api.getData().get().size());
              assertEquals(PortalTypes.AGENT, api.getData().get().get(0).getType());
              assertEquals(PortalTypes.CONSUMER, api.getData().get().get(1).getType());
              assertEquals(PASSWORD_MASK, api.getData().get().get(0).getPassword());
              assertEquals(PASSWORD_MASK, api.getData().get().get(1).getPassword());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByBrdId_serviceError_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("BRD001"))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(portalConfigurationService.getPortalConfigurationsByBrdId("BRD001"))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    StepVerifier.create(portalConfiguratorController.getPortalConfigurationsByBrdId("BRD001"))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationsByType_serviceError_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just("pm@gmail.com"));
    when(portalConfigurationService.getPortalConfigurationsByType(PortalTypes.AGENT))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationsByType(PortalTypes.AGENT))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_invalidData_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfigurationRequest invalidRequest = new PortalConfigurationRequest();
    invalidRequest.setBrdId("BRD001");
    invalidRequest.setUrl("invalid-url");
    invalidRequest.setType(PortalTypes.AGENT);
    invalidRequest.setUsername("testUser");
    invalidRequest.setPassword("testPass");

    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid URL format")));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "BRD001", List.of(invalidRequest)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationByIdAndType_invalidId_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.getPortalConfigurationByIdAndType(
            anyString(), any(PortalTypes.class)))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid ID format")));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationByIdAndType(
                "invalid-id", PortalTypes.AGENT))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void createPortalConfiguration_emptyList_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(Collections.emptyList()))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("BRD ID is required", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertTrue(api.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_invalidPortalType_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfigurationRequest invalidRequest = new PortalConfigurationRequest();
    invalidRequest.setBrdId("BRD001");
    invalidRequest.setUrl("https://test.com");
    invalidRequest.setType(null); // Invalid portal type
    invalidRequest.setUsername("testUser");
    invalidRequest.setPassword("testPass");

    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(invalidRequest)))
        .expectError()
        .verify();
  }

  @Test
  void createPortalConfiguration_blankFields_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfigurationRequest blankRequest = new PortalConfigurationRequest();
    blankRequest.setBrdId(""); // Blank BRD ID
    blankRequest.setUrl("https://test.com");
    blankRequest.setType(PortalTypes.AGENT);
    blankRequest.setUsername("testUser");
    blankRequest.setPassword("testPass");

    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.createPortalConfiguration(List.of(blankRequest)))
        .expectError()
        .verify();
  }

  @Test
  void getPortalConfigurationsByBrdId_invalidFormat_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId("invalid-brd-id"))
        .thenReturn(Mono.just(BRD.builder().brdId("invalid-brd-id").creator("pm@gmail.com").build()));
    when(portalConfigurationService.getPortalConfigurationsByBrdId("invalid-brd-id"))
        .thenReturn(Flux.error(new IllegalArgumentException("Invalid BRD ID format")));

    StepVerifier.create(
            portalConfiguratorController.getPortalConfigurationsByBrdId("invalid-brd-id"))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void getPortalConfigurationsByType_invalidType_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just("pm@gmail.com"));
    when(portalConfigurationService.getPortalConfigurationsByType(null))
        .thenReturn(Flux.error(new IllegalArgumentException("Invalid portal type")));

    StepVerifier.create(portalConfiguratorController.getPortalConfigurationsByType(null))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_invalidBrdId_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.updatePortalConfigurations(eq("invalid-brd-id"), anyList()))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid BRD ID format")));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "invalid-brd-id", List.of(testRequest)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_blankFields_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    PortalConfigurationRequest blankRequest = new PortalConfigurationRequest();
    blankRequest.setBrdId("BRD001");
    blankRequest.setUrl(""); // Blank URL
    blankRequest.setType(PortalTypes.AGENT);
    blankRequest.setUsername("testUser");
    blankRequest.setPassword("testPass");

    when(portalConfigurationService.updatePortalConfigurations(eq("BRD001"), anyList()))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid request data")));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "BRD001", List.of(blankRequest)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void updatePortalConfigurations_emptyConfigList_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(Collections.emptyList()));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "BRD001", Collections.emptyList()))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Portal configurations updated successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertTrue(api.getData().get().isEmpty());
              assertTrue(api.getErrors().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updatePortalConfigurations_withMultipleRoles_includesPm_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(portalConfigurationService.updatePortalConfigurations(anyString(), any()))
        .thenReturn(Mono.just(List.of(testConfiguration)));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
        .verifyComplete();
  }

  @Test
  void createPortalConfiguration_largeList_success() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    List<PortalConfigurationRequest> requests = Collections.nCopies(100, testRequest);
    when(portalConfigurationService.createPortalConfiguration(any()))
        .thenReturn(Mono.just(testConfiguration));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));
    StepVerifier.create(portalConfiguratorController.createPortalConfiguration(requests))
        .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED)
        .verifyComplete();
  }

  @Test
  void getPortalConfigurationsByType_invalidEnum_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PortalTypes.valueOf("INVALID_TYPE");
        });
  }

  @Test
  void updatePortalConfigurations_withPmRole_invalidBrdId_error() {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations(
                "INVALID_BRD", List.of(testRequest)))
        .expectError(RuntimeException.class)
        .verify();
  }

  private static List<List<org.springframework.security.core.GrantedAuthority>>
      forbiddenRoleSets() {
    return List.of(
        List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_USER)),
        List.of(),
        List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_USER),
            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_OTHER)));
  }

  @ParameterizedTest
  @MethodSource("forbiddenRoleSets")
  void updatePortalConfigurations_forbiddenRoles(
      List<org.springframework.security.core.GrantedAuthority> roles) {
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdSecurityService.canModifyBrd(anyString())).thenReturn(Mono.just(false));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD001").creator("pm@gmail.com").build()));

    StepVerifier.create(
            portalConfiguratorController.updatePortalConfigurations("BRD001", List.of(testRequest)))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<PortalConfiguration>> api = response.getBody();
              assertNotNull(api);
              assertEquals(PortalConfigConstants.ERROR, api.getStatus());
              assertEquals("Access denied: Only the creator PM can access this endpoint.", api.getMessage());
              assertTrue(api.getData().isEmpty());
              assertTrue(api.getErrors().isPresent());
              assertTrue(api.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }
}
