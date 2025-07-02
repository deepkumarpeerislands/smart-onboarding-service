package com.aci.smart_onboarding.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.UATConfiguratorRequestDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.UATConfigurator;
import com.aci.smart_onboarding.repository.UATConfiguratorRepository;
import com.aci.smart_onboarding.service.implementation.UATConfiguratorService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UATConfiguratorServiceTest {

  @Mock private UATConfiguratorRepository repository;

  @InjectMocks private UATConfiguratorService service;

  @Test
  void createConfiguration_validRequest_success() {
    UATConfiguratorRequestDTO request = createRequestDTO();
    UATConfigurator entity =
        UATConfigurator.builder()
            .uatId("UAT-123")
            .type(PortalTypes.AGENT)
            .configurationName("Login Config")
            .fields(List.of("loginButton"))
            .position("top-right")
            .scenario("Test login")
            .createdBy("test-user")
            .createdAt(LocalDateTime.now())
            .build();

    when(repository.save(any(UATConfigurator.class))).thenReturn(Mono.just(entity));
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("test-user");
    SecurityContext context = new SecurityContextImpl(authentication);
    StepVerifier.create(
            service
                .createConfiguration(request)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectNextMatches(
            dto ->
                dto.getUatId().equals("UAT-123")
                    && dto.getConfigurationName().equals("Login Config")
                    && dto.getType() == PortalTypes.AGENT)
        .verifyComplete();
  }

  @Test
  void createConfiguration_nullFields_success() {
    UATConfiguratorRequestDTO request =
        UATConfiguratorRequestDTO.builder()
            .type(PortalTypes.AGENT)
            .configurationName("Login Config")
            .build();

    UATConfigurator entity =
        UATConfigurator.builder()
            .uatId("UAT-123")
            .type(PortalTypes.AGENT)
            .configurationName("Login Config")
            .createdBy("test-user")
            .createdAt(LocalDateTime.now())
            .build();

    when(repository.save(any(UATConfigurator.class))).thenReturn(Mono.just(entity));
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("test-user");
    SecurityContext context = new SecurityContextImpl(authentication);
    StepVerifier.create(
            service
                .createConfiguration(request)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectNextMatches(
            dto ->
                dto.getUatId().equals("UAT-123")
                    && dto.getConfigurationName().equals("Login Config")
                    && dto.getType() == PortalTypes.AGENT)
        .verifyComplete();
  }

  @Test
  void updateConfiguration_validRequest_success() {
    String uatId = "UAT-123";
    UATConfiguratorRequestDTO updateDTO =
        UATConfiguratorRequestDTO.builder()
            .type(PortalTypes.AGENT)
            .configurationName("Updated Config")
            .fields(List.of("field1"))
            .position("2")
            .scenario("Updated Scenario")
            .build();

    UATConfigurator existingEntity =
        UATConfigurator.builder()
            .uatId(uatId)
            .type(PortalTypes.AGENT)
            .configurationName("Login Config")
            .fields(List.of("loginButton"))
            .position("top-right")
            .scenario("Test login")
            .createdBy("test-user")
            .createdAt(LocalDateTime.now())
            .build();

    UATConfigurator updatedEntity =
        UATConfigurator.builder()
            .uatId(uatId)
            .type(PortalTypes.AGENT)
            .configurationName("Updated Config")
            .fields(List.of("field1"))
            .position("2")
            .scenario("Updated Scenario")
            .createdBy("test-user")
            .createdAt(LocalDateTime.now())
            .updatedBy("test-user")
            .updatedAt(LocalDateTime.now())
            .build();

    when(repository.findById(uatId)).thenReturn(Mono.just(existingEntity));
    when(repository.save(any(UATConfigurator.class))).thenReturn(Mono.just(updatedEntity));
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("test-user");
    SecurityContext context = new SecurityContextImpl(authentication);
    StepVerifier.create(
            service
                .updateConfiguration(uatId, updateDTO)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectNextMatches(
            dto ->
                dto.getUatId().equals(uatId)
                    && dto.getConfigurationName().equals("Updated Config")
                    && dto.getType() == PortalTypes.AGENT)
        .verifyComplete();
  }

  @Test
  void updateConfiguration_notFound_error() {
    String uatId = "UAT-123";
    UATConfiguratorRequestDTO updateDTO =
        UATConfiguratorRequestDTO.builder()
            .type(PortalTypes.AGENT)
            .configurationName("Updated Config")
            .fields(List.of("field1"))
            .position("2")
            .scenario("Updated Scenario")
            .build();

    when(repository.findById(uatId)).thenReturn(Mono.empty());
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("test-user");
    SecurityContext context = new SecurityContextImpl(authentication);
    StepVerifier.create(
            service
                .updateConfiguration(uatId, updateDTO)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error.getMessage().equals(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId))
        .verify();
  }

  @Test
  void deleteConfiguration_validId_success() {
    String uatId = "UAT-123";
    UATConfigurator entity = createEntity();

    when(repository.findById(uatId)).thenReturn(Mono.just(entity));
    when(repository.delete(entity)).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteConfiguration(uatId)).verifyComplete();

    verify(repository).delete(entity);
  }

  @Test
  void deleteConfiguration_notFound_error() {
    String uatId = "UAT-123";

    when(repository.findById(uatId)).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteConfiguration(uatId))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error.getMessage().equals(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId))
        .verify();
  }

  @Test
  void getConfiguration_validId_success() {
    String uatId = "UAT-123";
    UATConfigurator entity = createEntity();

    when(repository.findById(uatId)).thenReturn(Mono.just(entity));

    StepVerifier.create(service.getConfiguration(uatId))
        .expectNextMatches(
            dto ->
                dto.getUatId().equals(uatId)
                    && dto.getConfigurationName().equals("Login Config")
                    && dto.getType() == PortalTypes.AGENT)
        .verifyComplete();
  }

  @Test
  void getConfiguration_notFound_error() {
    String uatId = "UAT-123";

    when(repository.findById(uatId)).thenReturn(Mono.empty());

    StepVerifier.create(service.getConfiguration(uatId))
        .expectErrorMatches(
            error ->
                error instanceof NotFoundException
                    && error.getMessage().equals(BrdConstants.UAT_CONFIG_NOT_FOUND + uatId))
        .verify();
  }

  @Test
  void getAllConfigurations_configsExist_success() {
    UATConfigurator entity1 = createEntity();
    UATConfigurator entity2 = createEntity();
    entity2.setUatId("UAT-124");

    when(repository.findAll()).thenReturn(Flux.just(entity1, entity2));

    StepVerifier.create(service.getAllConfigurations()).expectNextCount(2).verifyComplete();
  }

  @Test
  void getAllConfigurations_noConfigs_success() {
    when(repository.findAll()).thenReturn(Flux.empty());

    StepVerifier.create(service.getAllConfigurations()).verifyComplete();
  }

  @Test
  void getConfigurationsByType_validType_success() {
    UATConfigurator entity = createEntity();

    when(repository.findByType(PortalTypes.AGENT)).thenReturn(Flux.just(entity));

    StepVerifier.create(service.getConfigurationsByType(PortalTypes.AGENT))
        .expectNextMatches(
            dto ->
                dto.getType() == PortalTypes.AGENT
                    && dto.getConfigurationName().equals("Login Config"))
        .verifyComplete();
  }

  @Test
  void getConfigurationsByType_noConfigs_success() {
    when(repository.findByType(PortalTypes.AGENT)).thenReturn(Flux.empty());

    StepVerifier.create(service.getConfigurationsByType(PortalTypes.AGENT)).verifyComplete();
  }

  @Test
  void getConfigurationsByNames_validNames_success() {
    UATConfigurator entity = createEntity();
    List<String> configNames = List.of("Login Config");

    when(repository.findByConfigurationNameIn(configNames)).thenReturn(Flux.just(entity));

    StepVerifier.create(service.getConfigurationsByNames(configNames))
        .expectNextMatches(
            dto ->
                dto.getUatId().equals("UAT-123")
                    && dto.getConfigurationName().equals("Login Config")
                    && dto.getType() == PortalTypes.AGENT
                    && dto.getFields().equals(List.of("loginButton"))
                    && dto.getPosition().equals("top-right")
                    && dto.getScenario().equals("Test login"))
        .verifyComplete();
  }

  @Test
  void getConfigurationsByNames_multipleNames_success() {
    UATConfigurator entity1 = createEntity();
    UATConfigurator entity2 = createEntity();
    entity2.setUatId("UAT-124");
    entity2.setConfigurationName("Logout Config");

    List<String> configNames = List.of("Login Config", "Logout Config");

    when(repository.findByConfigurationNameIn(configNames)).thenReturn(Flux.just(entity1, entity2));

    StepVerifier.create(service.getConfigurationsByNames(configNames))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void getConfigurationsByNames_noNames_success() {
    List<String> configNames = List.of("NonExistentConfig");

    when(repository.findByConfigurationNameIn(configNames)).thenReturn(Flux.empty());

    StepVerifier.create(service.getConfigurationsByNames(configNames)).verifyComplete();
  }

  @Test
  void getConfigurationsByNames_nullList_success() {
    when(repository.findByConfigurationNameIn(null)).thenReturn(Flux.empty());

    StepVerifier.create(service.getConfigurationsByNames(null)).verifyComplete();
  }

  private UATConfiguratorRequestDTO createRequestDTO() {
    return UATConfiguratorRequestDTO.builder()
        .type(PortalTypes.AGENT)
        .configurationName("Login Config")
        .fields(List.of("loginButton"))
        .position("top-right")
        .scenario("Test login")
        .build();
  }

  private UATConfigurator createEntity() {
    return UATConfigurator.builder()
        .uatId("UAT-123")
        .type(PortalTypes.AGENT)
        .configurationName("Login Config")
        .fields(List.of("loginButton"))
        .position("top-right")
        .scenario("Test login")
        .build();
  }
}
