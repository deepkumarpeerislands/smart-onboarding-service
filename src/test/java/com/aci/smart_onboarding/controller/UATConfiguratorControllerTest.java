package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.UATConfiguratorRequestDTO;
import com.aci.smart_onboarding.dto.UATConfiguratorResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.service.IUATConfiguratorService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UATConfiguratorControllerTest {

  @Mock private IUATConfiguratorService service;

  @InjectMocks private UATConfiguratorController controller;

  @Test
  void createConfiguration_validRequest_success() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals("Test Config", body.getData().get().getConfigurationName());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateConfiguration_validRequest_success() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();

    when(service.updateConfiguration(uatId, requestDTO)).thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(uatId, body.getData().get().getUatId());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void deleteConfiguration_validId_success() {
    String uatId = "test-id";
    when(service.deleteConfiguration(uatId)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteConfiguration(uatId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getConfiguration_validId_success() {
    String uatId = "test-id";
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();

    when(service.getConfiguration(uatId)).thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.getConfiguration(uatId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(uatId, body.getData().get().getUatId());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getAllConfigurations_configsExist_success() {
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();

    when(service.getAllConfigurations()).thenReturn(Flux.just(responseDTO));

    StepVerifier.create(controller.getAllConfigurations())
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(1, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getConfigurationsByType_validType_success() {
    PortalTypes type = PortalTypes.AGENT;
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();

    when(service.getConfigurationsByType(type)).thenReturn(Flux.just(responseDTO));

    StepVerifier.create(controller.getConfigurationsByType(type))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(1, body.getData().get().size());
              assertEquals(type, body.getData().get().get(0).getType());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createConfiguration_serviceError_error_case1() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updateConfiguration_serviceError_error_case1() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();

    when(service.updateConfiguration(uatId, requestDTO))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void deleteConfiguration_serviceError_error_case1() {
    String uatId = "test-id";

    when(service.deleteConfiguration(uatId))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    StepVerifier.create(controller.deleteConfiguration(uatId))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getConfiguration_notFound_complete() {
    String uatId = "test-id";
    when(service.getConfiguration(uatId)).thenReturn(Mono.empty());
    StepVerifier.create(controller.getConfiguration(uatId)).expectComplete().verify();
  }

  @Test
  void getAllConfigurations_noConfigs_success() {
    when(service.getAllConfigurations()).thenReturn(Flux.empty());

    StepVerifier.create(controller.getAllConfigurations())
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertTrue(body.getData().get().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getConfigurationsByType_noConfigs_success() {
    PortalTypes type = PortalTypes.AGENT;
    when(service.getConfigurationsByType(type)).thenReturn(Flux.empty());

    StepVerifier.create(controller.getConfigurationsByType(type))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertTrue(body.getData().get().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createConfiguration_serviceError_error_case2() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updateConfiguration_serviceError_error_case2() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();

    when(service.updateConfiguration(uatId, requestDTO))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void deleteConfiguration_serviceError_error_case2() {
    String uatId = "test-id";

    when(service.deleteConfiguration(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.deleteConfiguration(uatId))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getConfiguration_serviceError_error() {
    String uatId = "test-id";

    when(service.getConfiguration(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getConfiguration(uatId))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getAllConfigurations_serviceError_error() {
    when(service.getAllConfigurations())
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getAllConfigurations())
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getConfigurationsByType_serviceError_error() {
    PortalTypes type = PortalTypes.AGENT;
    when(service.getConfigurationsByType(type))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getConfigurationsByType(type))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void createConfiguration_emptyFields_success() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setFields(List.of());
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setFields(List.of());

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertTrue(body.getData().get().getFields().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateConfiguration_emptyFields_success() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setFields(List.of());
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setFields(List.of());

    when(service.updateConfiguration(uatId, requestDTO)).thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertTrue(body.getData().get().getFields().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getConfigurationsByType_allTypes_success() {
    UATConfiguratorResponseDTO config1 = createResponseDTO();
    config1.setType(PortalTypes.AGENT);
    UATConfiguratorResponseDTO config2 = createResponseDTO();
    config2.setType(PortalTypes.CONSUMER);
    config2.setUatId("UAT-124");

    when(service.getConfigurationsByType(any())).thenReturn(Flux.just(config1, config2));

    StepVerifier.create(controller.getConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(2, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createConfiguration_maxLengthFields_success() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setConfigurationName("a".repeat(255));
    requestDTO.setPosition("b".repeat(255));
    requestDTO.setScenario("c".repeat(255));
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setConfigurationName("a".repeat(255));
    responseDTO.setPosition("b".repeat(255));
    responseDTO.setScenario("c".repeat(255));

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(255, body.getData().get().getConfigurationName().length());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateConfiguration_maxLengthFields_success() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setConfigurationName("a".repeat(255));
    requestDTO.setPosition("b".repeat(255));
    requestDTO.setScenario("c".repeat(255));
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setConfigurationName("a".repeat(255));
    responseDTO.setPosition("b".repeat(255));
    responseDTO.setScenario("c".repeat(255));

    when(service.updateConfiguration(uatId, requestDTO)).thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(255, body.getData().get().getConfigurationName().length());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createConfiguration_allFields_success() {
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setFields(List.of("field1", "field2", "field3"));
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setFields(List.of("field1", "field2", "field3"));

    when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
        .thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.createConfiguration(requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(3, body.getData().get().getFields().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateConfiguration_allFields_success() {
    String uatId = "test-id";
    UATConfiguratorRequestDTO requestDTO = createRequestDTO();
    requestDTO.setFields(List.of("field1", "field2", "field3"));
    UATConfiguratorResponseDTO responseDTO = createResponseDTO();
    responseDTO.setFields(List.of("field1", "field2", "field3"));

    when(service.updateConfiguration(uatId, requestDTO)).thenReturn(Mono.just(responseDTO));

    StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(3, body.getData().get().getFields().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getAllConfigurations_multipleConfigs_success() {
    UATConfiguratorResponseDTO config1 = createResponseDTO();
    config1.setUatId("UAT-123");
    UATConfiguratorResponseDTO config2 = createResponseDTO();
    config2.setUatId("UAT-124");
    UATConfiguratorResponseDTO config3 = createResponseDTO();
    config3.setUatId("UAT-125");

    when(service.getAllConfigurations()).thenReturn(Flux.just(config1, config2, config3));

    StepVerifier.create(controller.getAllConfigurations())
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(3, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getConfigurationsByType_multipleTypes_success() {
    UATConfiguratorResponseDTO config1 = createResponseDTO();
    config1.setType(PortalTypes.AGENT);
    config1.setUatId("UAT-123");
    UATConfiguratorResponseDTO config2 = createResponseDTO();
    config2.setType(PortalTypes.AGENT);
    config2.setUatId("UAT-124");
    UATConfiguratorResponseDTO config3 = createResponseDTO();
    config3.setType(PortalTypes.CONSUMER);
    config3.setUatId("UAT-125");

    when(service.getConfigurationsByType(PortalTypes.AGENT))
        .thenReturn(Flux.just(config1, config2));

    StepVerifier.create(controller.getConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(2, body.getData().get().size());
              assertTrue(
                  body.getData().get().stream().allMatch(c -> c.getType() == PortalTypes.AGENT));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createConfiguration_variousPositions_success() {
    List<String> positions =
        List.of("top-left", "top-right", "bottom-left", "bottom-right", "center");

    for (String position : positions) {
      UATConfiguratorRequestDTO requestDTO = createRequestDTO();
      requestDTO.setPosition(position);
      UATConfiguratorResponseDTO responseDTO = createResponseDTO();
      responseDTO.setPosition(position);

      when(service.createConfiguration(any(UATConfiguratorRequestDTO.class)))
          .thenReturn(Mono.just(responseDTO));

      StepVerifier.create(controller.createConfiguration(requestDTO))
          .expectNextMatches(
              responseEntity -> {
                assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
                var body = responseEntity.getBody();
                assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
                assertTrue(body.getData().isPresent());
                assertEquals(position, body.getData().get().getPosition());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void updateConfiguration_variousPositions_success() {
    List<String> positions =
        List.of("top-left", "top-right", "bottom-left", "bottom-right", "center");
    String uatId = "test-id";

    for (String position : positions) {
      UATConfiguratorRequestDTO requestDTO = createRequestDTO();
      requestDTO.setPosition(position);
      UATConfiguratorResponseDTO responseDTO = createResponseDTO();
      responseDTO.setPosition(position);

      when(service.updateConfiguration(uatId, requestDTO)).thenReturn(Mono.just(responseDTO));

      StepVerifier.create(controller.updateConfiguration(uatId, requestDTO))
          .expectNextMatches(
              responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                var body = responseEntity.getBody();
                assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
                assertTrue(body.getData().isPresent());
                assertEquals(position, body.getData().get().getPosition());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void getConfiguration_variousIds_success() {
    List<String> uatIds = List.of("UAT-123", "UAT-124", "UAT-125");

    for (String uatId : uatIds) {
      UATConfiguratorResponseDTO config = createResponseDTO();
      config.setUatId(uatId);

      when(service.getConfiguration(uatId)).thenReturn(Mono.just(config));

      StepVerifier.create(controller.getConfiguration(uatId))
          .expectNextMatches(
              responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                var body = responseEntity.getBody();
                assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
                assertTrue(body.getData().isPresent());
                assertEquals(uatId, body.getData().get().getUatId());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void deleteConfiguration_variousIds_success() {
    List<String> uatIds = List.of("UAT-123", "UAT-124", "UAT-125");

    for (String uatId : uatIds) {
      when(service.deleteConfiguration(uatId)).thenReturn(Mono.empty());

      StepVerifier.create(controller.deleteConfiguration(uatId))
          .expectNextMatches(
              responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                var body = responseEntity.getBody();
                assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
                assertTrue(body.getData().isEmpty());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  void getConfigurationsByType_allPortalTypes_success() {
    for (PortalTypes type : PortalTypes.values()) {
      UATConfiguratorResponseDTO config = createResponseDTO();
      config.setType(type);

      when(service.getConfigurationsByType(type)).thenReturn(Flux.just(config));

      StepVerifier.create(controller.getConfigurationsByType(type))
          .expectNextMatches(
              responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                var body = responseEntity.getBody();
                assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
                assertTrue(body.getData().isPresent());
                assertEquals(1, body.getData().get().size());
                assertEquals(type, body.getData().get().get(0).getType());
                return true;
              })
          .verifyComplete();
    }
  }

  private UATConfiguratorRequestDTO createRequestDTO() {
    return UATConfiguratorRequestDTO.builder()
        .type(PortalTypes.AGENT)
        .configurationName("Test Config")
        .fields(List.of("field1"))
        .position("1")
        .scenario("Test Scenario")
        .build();
  }

  private UATConfiguratorResponseDTO createResponseDTO() {
    return UATConfiguratorResponseDTO.builder()
        .uatId("test-id")
        .type(PortalTypes.AGENT)
        .configurationName("Test Config")
        .fields(List.of("field1"))
        .position("1")
        .scenario("Test Scenario")
        .createdBy("test-user")
        .createdAt(LocalDateTime.now())
        .updatedBy("test-user")
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
