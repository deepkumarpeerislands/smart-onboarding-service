package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdFormResponse;
import com.aci.smart_onboarding.dto.BrdTemplateReq;
import com.aci.smart_onboarding.dto.BrdTemplateRes;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.implementation.BrdTemplateService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BrdTemplateConfigurationControllerTest {

  @Mock private BrdTemplateService brdTemplateService;

  @Mock private BRDSecurityService securityService;

  @InjectMocks private BrdTemplateConfigurationController controller;

  private BrdTemplateReq templateRequest;
  private BrdTemplateRes templateResponse;
  private BrdFormResponse brdFormResponse;

  @BeforeEach
  void setUp() {
    templateRequest = new BrdTemplateReq();
    templateRequest.setTemplateName("Test Template");
    templateRequest.setTemplateTypes("TEST_TYPE");
    templateRequest.setSummary("Test Summary");
    templateRequest.setClientInformation(true);
    templateRequest.setAciInformation(true);
    templateRequest.setPaymentChannels(true);

    templateResponse = new BrdTemplateRes();
    templateResponse.setId("123");
    templateResponse.setTemplateName("Test Template");
    templateResponse.setTemplateTypes("TEST_TYPE");
    templateResponse.setSummary("Test Summary");
    templateResponse.setClientInformation(true);
    templateResponse.setAciInformation(true);
    templateResponse.setPaymentChannels(true);

    brdFormResponse =
        BrdFormResponse.builder()
            .brdFormId("BRD-001")
            .status("DRAFT")
            .projectId("PRJ-001")
            .brdId("BRD-ID-001")
            .brdName("Test BRD")
            .description("Test Description")
            .organizationId("ORG-001")
            .creator("test.user")
            .type("STANDARD")
            .templateType("TEST_TYPE")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .sections(new HashMap<>())
            .build();
  }

  @Test
  void saveBrdForm_WithValidValues_ShouldReturnsSuccess() {
    ResponseEntity<Api<BrdTemplateRes>> successResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Template created successfully",
                    Optional.of(templateResponse),
                    Optional.empty()));

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.createTemplate(any(BrdTemplateReq.class)))
        .thenReturn(Mono.just(successResponse));

    StepVerifier.create(controller.saveBrdForm(Mono.just(templateRequest)))
        .expectNext(successResponse)
        .verifyComplete();
  }

  @Test
  void saveBrdForm_AlreadyExists() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.createTemplate(any(BrdTemplateReq.class)))
        .thenReturn(Mono.error(new AlreadyExistException("Template already exists")));

    StepVerifier.create(controller.saveBrdForm(Mono.just(templateRequest)))
        .expectError(AlreadyExistException.class)
        .verify();
  }

  @Test
  void updateTemplate_Success() {
    ResponseEntity<Api<BrdTemplateRes>> successResponse =
        ResponseEntity.ok()
            .body(
                new Api<>(
                    "SUCCESS",
                    "Template updated successfully",
                    Optional.of(templateResponse),
                    Optional.empty()));

    when(brdTemplateService.updateTemplate(any(String.class), any(BrdTemplateReq.class)))
        .thenReturn(Mono.just(successResponse));

    StepVerifier.create(controller.updateTemplate("123", Mono.just(templateRequest)))
        .expectNext(successResponse)
        .verifyComplete();
  }

  @Test
  void updateTemplate_NotFound() {
    when(brdTemplateService.updateTemplate(any(String.class), any(BrdTemplateReq.class)))
        .thenReturn(Mono.error(new NotFoundException("Template not found")));

    StepVerifier.create(controller.updateTemplate("123", Mono.just(templateRequest)))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getAllTemplates_Success() {
    List<BrdTemplateRes> templates = Arrays.asList(templateResponse);
    ResponseEntity<Api<List<BrdTemplateRes>>> successResponse =
        ResponseEntity.ok()
            .body(
                new Api<>(
                    "SUCCESS",
                    "Templates retrieved successfully",
                    Optional.of(templates),
                    Optional.empty()));

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getAllTemplates()).thenReturn(Mono.just(successResponse));

    StepVerifier.create(controller.getAllTemplates()).expectNext(successResponse).verifyComplete();
  }

  @Test
  void getAllTemplates_NoTemplatesFound() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getAllTemplates())
        .thenReturn(Mono.error(new NotFoundException("No templates found")));

    StepVerifier.create(controller.getAllTemplates()).expectError(NotFoundException.class).verify();
  }

  @Test
  void getTemplateByType_Success() {
    ResponseEntity<Api<BrdTemplateRes>> successResponse =
        ResponseEntity.ok()
            .body(
                new Api<>(
                    "SUCCESS",
                    "Template found successfully",
                    Optional.of(templateResponse),
                    Optional.empty()));

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getTemplateByType("TEST_TYPE")).thenReturn(Mono.just(successResponse));

    StepVerifier.create(controller.getTemplateByType("TEST_TYPE"))
        .expectNext(successResponse)
        .verifyComplete();
  }

  @Test
  void getTemplateByType_NotFound() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getTemplateByType("INVALID_TYPE"))
        .thenReturn(Mono.error(new NotFoundException("Template not found")));

    StepVerifier.create(controller.getTemplateByType("INVALID_TYPE"))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getTemplateByType_BadRequest() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getTemplateByType(""))
        .thenReturn(Mono.error(new BadRequestException("templateType", "cannot be empty")));

    StepVerifier.create(controller.getTemplateByType(""))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void getBrdForm_Success() {
    ResponseEntity<Api<BrdFormResponse>> successResponse =
        ResponseEntity.ok()
            .body(
                new Api<>(
                    "SUCCESS",
                    "BRD form generated successfully",
                    Optional.of(brdFormResponse),
                    Optional.empty()));

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-001", "TEST_TYPE"))
        .thenReturn(Mono.just(successResponse));

    StepVerifier.create(controller.getBrdForm("BRD-001", "TEST_TYPE"))
        .expectNext(successResponse)
        .verifyComplete();
  }

  @Test
  void getBrdForm_NotFound() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getBrdFormByIdAndTemplateType("INVALID_BRD", "TEST_TYPE"))
        .thenReturn(Mono.error(new NotFoundException("BRD not found")));

    StepVerifier.create(controller.getBrdForm("INVALID_BRD", "TEST_TYPE"))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getBrdForm_BadRequest() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-001", ""))
        .thenReturn(Mono.error(new BadRequestException("templateType", "cannot be empty")));

    StepVerifier.create(controller.getBrdForm("BRD-001", ""))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void getBrdForm_TemplateNotFound() {
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-001", "INVALID_TYPE"))
        .thenReturn(Mono.error(new NotFoundException("Template not found")));

    StepVerifier.create(controller.getBrdForm("BRD-001", "INVALID_TYPE"))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void saveBrdForm_WithNonManagerRole_ShouldFail() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    StepVerifier.create(controller.saveBrdForm(Mono.just(templateRequest)))
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("failure", response.getBody().getStatus());
              assertEquals(
                  "Access denied. Only Manager role can access templates",
                  response.getBody().getMessage());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void saveBrdForm_WithNoRole_ShouldFail() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.empty());

    StepVerifier.create(controller.saveBrdForm(Mono.just(templateRequest)))
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("failure", response.getBody().getStatus());
              assertEquals(
                  "Access denied. Only Manager role can access templates",
                  response.getBody().getMessage());
              return true;
            })
        .verifyComplete();
  }
}
