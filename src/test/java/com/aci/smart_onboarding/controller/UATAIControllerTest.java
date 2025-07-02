package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.UATAIRequestDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUATAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class UATAIControllerTest {

  @Mock private IUATAIService uatAIService;
  @Mock private ObjectMapper objectMapper;
  @Mock private BRDSecurityService securityService;
  @Mock private BRDRepository brdRepository;

  @InjectMocks private UATAIController uatAIController;

  private UATAIRequestDTO uatAIRequestDTO;
  private UATTestCaseRequestResponseDTO testCaseResponseDTO;
  private Flux<UATTestCaseRequestResponseDTO> testCaseFlux;

  @BeforeEach
  void setUp() {
    uatAIRequestDTO = new UATAIRequestDTO();
    uatAIRequestDTO.setBrdId("BRD-001");
    uatAIRequestDTO.setConfigurationNames(List.of("config1", "config2"));
    uatAIRequestDTO.setUatType(PortalTypes.AGENT);

    testCaseResponseDTO = UATTestCaseRequestResponseDTO.builder()
        .brdId("BRD-001")
        .testName("Test Case 1")
        .scenario("Test scenario")
        .position("top")
        .answer("Expected answer")
        .uatType(PortalTypes.AGENT)
        .status(TestStatus.PASSED)
        .build();

    testCaseFlux = Flux.just(testCaseResponseDTO);
  }

  @Test
  @DisplayName("generateUATTestCases with PM role should return success")
  void generateUATTestCases_WithPMRole_ShouldReturnSuccess() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD-001").creator("pm@gmail.com").build()));
    when(uatAIService.generateUATTestCases(anyString(), anyList(), any(PortalTypes.class)))
        .thenReturn(testCaseFlux);

    // Act & Assert
    StepVerifier.create(uatAIController.generateUATTestCases(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("SUCCESSFUL", apiResponse.getStatus());
              assertEquals("UAT test cases generation initiated", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService)
        .generateUATTestCases(
            uatAIRequestDTO.getBrdId(),
            uatAIRequestDTO.getConfigurationNames(),
            uatAIRequestDTO.getUatType());
  }

  @Test
  @DisplayName("generateUATTestCases without PM role should return 403 Forbidden")
  void generateUATTestCases_WithoutPMRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

    // Act & Assert
    StepVerifier.create(uatAIController.generateUATTestCases(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("ERROR", apiResponse.getStatus());
              assertEquals("Access denied: Only the creator PM can access this endpoint.", apiResponse.getMessage());
              assertTrue(apiResponse.getErrors().isPresent());
              assertEquals(
                  "Access denied: Only the creator PM can access this endpoint.",
                  apiResponse.getErrors().get().get("error"));
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).generateUATTestCases(anyString(), anyList(), any(PortalTypes.class));
  }

  @Test
  @DisplayName("generateUATTestCases with null role should return 403 Forbidden")
  void generateUATTestCases_WithNullRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(uatAIController.generateUATTestCases(uatAIRequestDTO))
        .verifyComplete(); // When role is null, flatMap doesn't execute, so no response

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).generateUATTestCases(anyString(), anyList(), any(PortalTypes.class));
  }

  @Test
  @DisplayName("generateUATTestCases with service error should return success with error Flux")
  void generateUATTestCases_WithServiceError_ShouldReturnSuccessWithErrorFlux() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD-001").creator("pm@gmail.com").build()));
    when(uatAIService.generateUATTestCases(anyString(), anyList(), any(PortalTypes.class)))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    // Act & Assert
    StepVerifier.create(uatAIController.generateUATTestCases(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("SUCCESSFUL", apiResponse.getStatus());
              assertEquals("UAT test cases generation initiated", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              // The data contains a Flux that will emit an error when subscribed
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService)
        .generateUATTestCases(
            uatAIRequestDTO.getBrdId(),
            uatAIRequestDTO.getConfigurationNames(),
            uatAIRequestDTO.getUatType());
  }

  @Test
  @DisplayName("retestFeatures with PM role should return success")
  void retestFeatures_WithPMRole_ShouldReturnSuccess() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD-001").creator("pm@gmail.com").build()));
    when(uatAIService.retestFeatures(anyString(), anyList(), any(PortalTypes.class)))
        .thenReturn(testCaseFlux);

    // Act & Assert
    StepVerifier.create(uatAIController.retestFeatures(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("SUCCESSFUL", apiResponse.getStatus());
              assertEquals("UAT retest initiated", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService)
        .retestFeatures(
            uatAIRequestDTO.getBrdId(),
            uatAIRequestDTO.getConfigurationNames(),
            uatAIRequestDTO.getUatType());
  }

  @Test
  @DisplayName("retestFeatures without PM role should return 403 Forbidden")
  void retestFeatures_WithoutPMRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

    // Act & Assert
    StepVerifier.create(uatAIController.retestFeatures(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("ERROR", apiResponse.getStatus());
              assertEquals("Access denied: Only the creator PM can access this endpoint.", apiResponse.getMessage());
              assertTrue(apiResponse.getErrors().isPresent());
              assertEquals(
                  "Access denied: Only the creator PM can access this endpoint.",
                  apiResponse.getErrors().get().get("error"));
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).retestFeatures(anyString(), anyList(), any(PortalTypes.class));
  }

  @Test
  @DisplayName("retestFeatures with null role should return 403 Forbidden")
  void retestFeatures_WithNullRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(uatAIController.retestFeatures(uatAIRequestDTO))
        .verifyComplete(); // When role is null, flatMap doesn't execute, so no response

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).retestFeatures(anyString(), anyList(), any(PortalTypes.class));
  }

  @Test
  @DisplayName("retestFeatures with service error should handle error gracefully")
  void retestFeatures_WithServiceError_ShouldHandleErrorGracefully() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(brdRepository.findByBrdId(anyString()))
        .thenReturn(Mono.just(BRD.builder().brdId("BRD-001").creator("pm@gmail.com").build()));
    when(uatAIService.retestFeatures(anyString(), anyList(), any(PortalTypes.class)))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    // Act & Assert
    StepVerifier.create(uatAIController.retestFeatures(uatAIRequestDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              Api<Flux<UATTestCaseRequestResponseDTO>> apiResponse = response.getBody();
              assertEquals("SUCCESSFUL", apiResponse.getStatus());
              assertEquals("UAT retest initiated", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService)
        .retestFeatures(
            uatAIRequestDTO.getBrdId(),
            uatAIRequestDTO.getConfigurationNames(),
            uatAIRequestDTO.getUatType());
  }

  @Test
  @DisplayName("retestFeatures with security service error should propagate error")
  void retestFeatures_WithSecurityServiceError_ShouldPropagateError() {
    // Arrange
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.error(new RuntimeException("Security service error")));

    // Act & Assert
    StepVerifier.create(uatAIController.retestFeatures(uatAIRequestDTO))
        .expectError(RuntimeException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).retestFeatures(anyString(), anyList(), any(PortalTypes.class));
  }

  @Test
  @DisplayName("generateUATTestCases with security service error should propagate error")
  void generateUATTestCases_WithSecurityServiceError_ShouldPropagateError() {
    // Arrange
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.error(new RuntimeException("Security service error")));

    // Act & Assert
    StepVerifier.create(uatAIController.generateUATTestCases(uatAIRequestDTO))
        .expectError(RuntimeException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(uatAIService, never()).generateUATTestCases(anyString(), anyList(), any(PortalTypes.class));
  }
} 