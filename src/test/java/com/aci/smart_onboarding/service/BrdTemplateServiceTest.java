package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.BrdFormResponse;
import com.aci.smart_onboarding.dto.BrdTemplateReq;
import com.aci.smart_onboarding.dto.BrdTemplateRes;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdTemplateConfig;
import com.aci.smart_onboarding.repository.BRDTemplateRepository;
import com.aci.smart_onboarding.service.implementation.BrdTemplateService;
import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import com.aci.smart_onboarding.util.brd_form.PaymentChannels;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BrdTemplateServiceTest {

  @Mock private BRDTemplateRepository brdTemplateRepository;

  @Mock private DtoModelMapper dtoModelMapper;

  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;

  @InjectMocks private BrdTemplateService brdTemplateService;

  private BrdTemplateReq templateRequest;
  private BrdTemplateConfig templateConfig;
  private BrdTemplateRes templateResponse;
  private BRD brd;

  @BeforeEach
  void setUp() {
    templateRequest = new BrdTemplateReq();
    templateRequest.setTemplateName("Test Template");
    templateRequest.setTemplateTypes("TEST_TYPE");
    templateRequest.setSummary("Test Summary");
    templateRequest.setClientInformation(true);
    templateRequest.setAciInformation(true);

    templateConfig = new BrdTemplateConfig();
    templateConfig.setId("123");
    templateConfig.setTemplateName("Test Template");
    templateConfig.setTemplateTypes("TEST_TYPE");
    templateConfig.setSummary("Test Summary");
    templateConfig.setClientInformation(true);
    templateConfig.setAciInformation(true);

    templateResponse = new BrdTemplateRes();
    templateResponse.setId("123");
    templateResponse.setTemplateTypes("TEST_TYPE");
    templateResponse.setSummary("Test Summary");
    templateResponse.setClientInformation(true);
    templateResponse.setAciInformation(true);
    templateResponse.setPaymentChannels(true);
    templateResponse.setFundingMethods(true);
    templateResponse.setNotifications(true);
    templateResponse.setAgentPortal(true);

    brd = new BRD();
    brd.setBrdFormId("BRD-001");
    brd.setStatus("DRAFT");
    brd.setProjectId("PRJ-001");
    brd.setBrdId("BRD-ID-001");
    brd.setBrdName("Test BRD");
    brd.setDescription("Test Description");
    brd.setCustomerId("ORG-001");
    brd.setCreator("test.user");
    brd.setType("STANDARD");
    brd.setCreatedAt(LocalDateTime.now());
    brd.setUpdatedAt(LocalDateTime.now());

    ClientInformation clientInfo = new ClientInformation();
    clientInfo.setCompanyName("Test Company");
    brd.setClientInformation(clientInfo);

    PaymentChannels paymentChannels = new PaymentChannels();
    brd.setPaymentChannels(paymentChannels);
  }

  @Test
  void createTemplate_Success_201() {
    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(brdTemplateRepository.save(any(BrdTemplateConfig.class)))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.createTemplate(templateRequest))
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("Template created successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("123", response.getBody().getData().get().getId());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createTemplate_DuplicateKey_409() {
    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(brdTemplateRepository.save(any(BrdTemplateConfig.class)))
        .thenReturn(Mono.error(new DuplicateKeyException("Duplicate key")));

    StepVerifier.create(brdTemplateService.createTemplate(templateRequest))
        .expectErrorMatches(
            throwable -> {
              assertNotNull(throwable);
              assertTrue(throwable instanceof AlreadyExistException);
              assertEquals(
                  "Template config already exists with given type", throwable.getMessage());
              return true;
            })
        .verify();
  }

  @Test
  void updateTemplate_Success_200() {
    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(), any(FindAndModifyOptions.class), any()))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.updateTemplate("123", templateRequest))
        .expectNextMatches(
            response -> {
              assert response.getStatusCode() == HttpStatus.OK;
              assert response.getBody().getMessage().equals("Template updated successfully");
              assert response.getBody().getData().isPresent();
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateTemplate_NotFound_404() {
    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(), any(FindAndModifyOptions.class), any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(brdTemplateService.updateTemplate("123", templateRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().contains("Template not found with id: 123"))
        .verify();
  }

  @Test
  void getAllTemplates_Success_200() {
    when(brdTemplateRepository.findAll()).thenReturn(Flux.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.getAllTemplates())
        .expectNextMatches(
            response -> {
              assert response.getStatusCode() == HttpStatus.OK;
              assert response.getBody().getMessage().equals("Found 1 templates");
              assert response.getBody().getData().isPresent();
              assert response.getBody().getData().get().size() == 1;
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getAllTemplates_NoTemplatesFound() {
    when(brdTemplateRepository.findAll()).thenReturn(Flux.empty());

    StepVerifier.create(brdTemplateService.getAllTemplates())
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("No templates found"))
        .verify();
  }

  @Test
  void getTemplateByType_Success_200() {
    when(brdTemplateRepository.findByTemplateTypes("TEST_TYPE"))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.getTemplateByType("TEST_TYPE"))
        .expectNextMatches(
            response -> {
              assert response.getStatusCode() == HttpStatus.OK;
              assert response.getBody().getMessage().equals("Template found successfully");
              assert response.getBody().getData().isPresent();
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTemplateByType_NotFound_404() {
    when(brdTemplateRepository.findByTemplateTypes("TEST_TYPE")).thenReturn(Mono.empty());

    StepVerifier.create(brdTemplateService.getTemplateByType("TEST_TYPE"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().contains("Template not found with type"))
        .verify();
  }

  @Test
  void getTemplateByType_EmptyType_400() {
    StepVerifier.create(brdTemplateService.getTemplateByType(""))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("cannot be null or empty"))
        .verify();
  }

  @Test
  void getTemplateByType_NullType_400() {
    StepVerifier.create(brdTemplateService.getTemplateByType(null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("cannot be null or empty"))
        .verify();
  }

  @Test
  void handleErrors_GenericException_500() {
    Exception genericException = new Exception("Some unexpected error");
    Throwable result = brdTemplateService.handleErrors(genericException);

    assertAll(
        () -> assertTrue(result instanceof Exception, "Result should be an Exception"),
        () ->
            assertEquals(
                "Something went wrong: Some unexpected error",
                result.getMessage(),
                "Error message should match"),
        () -> assertNotNull(result.getMessage(), "Error message should not be null"));
  }

  @Test
  void handleErrors_DuplicateKeyException_409() {
    DuplicateKeyException duplicateKeyException = new DuplicateKeyException("Duplicate key");
    Throwable result = brdTemplateService.handleErrors(duplicateKeyException);

    assertAll(
        () ->
            assertTrue(
                result instanceof AlreadyExistException,
                "Result should be an AlreadyExistException"),
        () ->
            assertEquals(
                "Template config already exists with given type",
                result.getMessage(),
                "Error message should match"),
        () -> assertNotNull(result, "Result should not be null"));
  }

  @Test
  void handleErrors_BadRequestException_400() {
    BadRequestException badRequestException = new BadRequestException("Invalid input");
    Throwable result = brdTemplateService.handleErrors(badRequestException);

    assertAll(
        () ->
            assertTrue(
                result instanceof BadRequestException, "Result should be a BadRequestException"),
        () -> assertEquals("Invalid input", result.getMessage(), "Error message should match"),
        () -> assertNotNull(result.getMessage(), "Error message should not be null"),
        () ->
            assertSame(
                badRequestException,
                result,
                "Should return the same BadRequestException instance"));
  }

  @Test
  void getBrdFormByIdAndTemplateType_Success() {
    when(brdTemplateRepository.findByTemplateTypes("TEST_TYPE"))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(this.templateResponse);
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class))).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-ID-001", "TEST_TYPE"))
        .expectNextMatches(
            response -> {
              assert response.getStatusCode() == HttpStatus.OK;
              assert response.getBody().getMessage().equals("BRD form generated successfully");
              assert response.getBody().getData().isPresent();
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdFormByIdAndTemplateType_EmptyTemplateType() {
    StepVerifier.create(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-ID-001", ""))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("cannot be null or empty"))
        .verify();
  }

  @Test
  void getBrdFormByIdAndTemplateType_TemplateNotFound() {
    when(brdTemplateRepository.findByTemplateTypes("INVALID_TYPE")).thenReturn(Mono.empty());

    StepVerifier.create(
            brdTemplateService.getBrdFormByIdAndTemplateType("BRD-ID-001", "INVALID_TYPE"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().contains("Template not found with type"))
        .verify();
  }

  @Test
  void getBrdForm_Success() {
    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdForm("BRD-ID-001", templateResponse))
        .expectNextMatches(
            response -> {
              assert response.getBrdFormId().equals(brd.getBrdFormId());
              assert response.getStatus().equals(brd.getStatus());
              assert response.getProjectId().equals(brd.getProjectId());
              assert response.getSections() != null;
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdForm_BrdNotFound() {
    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.empty());

    StepVerifier.create(brdTemplateService.getBrdForm("INVALID_BRD", templateResponse))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().contains("BRD not found with id"))
        .verify();
  }

  @Test
  void getBrdForm_WithExistingSections() {
    ClientInformation existingClientInfo = new ClientInformation();
    existingClientInfo.setCompanyName("Existing Company");
    brd.setClientInformation(existingClientInfo);

    PaymentChannels existingChannels = new PaymentChannels();
    brd.setPaymentChannels(existingChannels);

    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdForm("BRD-ID-001", templateResponse))
        .expectNextMatches(
            response -> {
              Map<String, Object> sections = response.getSections();
              assert sections.containsKey("clientInformation");
              assert sections.containsKey("paymentChannels");
              ClientInformation clientInfo = (ClientInformation) sections.get("clientInformation");
              assert clientInfo.getCompanyName().equals("Existing Company");
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdForm_WithDefaultSections() {
    brd.setClientInformation(null);
    brd.setPaymentChannels(null);

    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdForm("BRD-ID-001", templateResponse))
        .expectNextMatches(
            response -> {
              Map<String, Object> sections = response.getSections();
              assert sections.containsKey("clientInformation");
              assert sections.containsKey("paymentChannels");
              ClientInformation clientInfo = (ClientInformation) sections.get("clientInformation");
              assert clientInfo != null;
              PaymentChannels channels = (PaymentChannels) sections.get("paymentChannels");
              assert channels != null;
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdForm_WithDisabledSections() {
    templateResponse.setClientInformation(false);
    templateResponse.setPaymentChannels(false);

    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdForm("BRD-ID-001", templateResponse))
        .expectNextMatches(
            response -> {
              Map<String, Object> sections = response.getSections();
              assert !sections.containsKey("clientInformation");
              assert !sections.containsKey("paymentChannels");
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdForm_WithAllSections() {
    templateResponse.setClientInformation(true);
    templateResponse.setAciInformation(true);
    templateResponse.setPaymentChannels(true);
    templateResponse.setFundingMethods(true);
    templateResponse.setAchPaymentProcessing(true);
    templateResponse.setMiniAccountMaster(true);
    templateResponse.setAccountIdentifierInformation(true);
    templateResponse.setPaymentRules(true);
    templateResponse.setNotifications(true);
    templateResponse.setRemittance(true);
    templateResponse.setAgentPortal(true);
    templateResponse.setRecurringPayments(true);
    templateResponse.setIvr(true);
    templateResponse.setGeneralImplementations(true);
    templateResponse.setApprovals(true);
    templateResponse.setRevisionHistory(true);

    when(reactiveMongoTemplate.findOne(any(Query.class), any())).thenReturn(Mono.just(brd));

    StepVerifier.create(brdTemplateService.getBrdForm("BRD-ID-001", templateResponse))
        .expectNextMatches(
            response -> {
              Map<String, Object> sections = response.getSections();
              assert sections.containsKey("clientInformation");
              assert sections.containsKey("aciInformation");
              assert sections.containsKey("paymentChannels");
              assert sections.containsKey("fundingMethods");
              assert sections.containsKey("achPaymentProcessing");
              assert sections.containsKey("miniAccountMaster");
              assert sections.containsKey("accountIdentifierInformation");
              assert sections.containsKey("paymentRules");
              assert sections.containsKey("notifications");
              assert sections.containsKey("remittance");
              assert sections.containsKey("agentPortal");
              assert sections.containsKey("recurringPayments");
              assert sections.containsKey("ivr");
              assert sections.containsKey("generalImplementations");
              assert sections.containsKey("approvals");
              assert sections.containsKey("revisionHistory");
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBrdFormByIdAndTemplateType_NullBrdResponse() {

    when(brdTemplateRepository.findByTemplateTypes("TEST_TYPE"))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(this.templateResponse);
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(new BRD()));

    StepVerifier.create(brdTemplateService.getBrdFormByIdAndTemplateType("BRD-ID-001", "TEST_TYPE"))
        .expectNextMatches(
            response -> {
              assert response.getStatusCode() == HttpStatus.OK;
              assert response.getBody().getData().isPresent();
              BrdFormResponse formResponse = response.getBody().getData().get();
              assert formResponse.getSections() != null;
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createTemplate_ShouldSetTimestamps() {
    Instant now = Instant.now();
    templateConfig.setCreatedAt(now);
    templateConfig.setUpdatedAt(now);
    templateResponse.setCreatedAt(now);
    templateResponse.setUpdatedAt(now);

    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(brdTemplateRepository.save(any(BrdTemplateConfig.class)))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.createTemplate(templateRequest))
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertTrue(response.getBody().getData().isPresent());
              BrdTemplateRes template = response.getBody().getData().get();
              assertNotNull(template.getCreatedAt());
              assertNotNull(template.getUpdatedAt());
              assertEquals(now, template.getCreatedAt());
              assertEquals(now, template.getUpdatedAt());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateTemplate_ShouldUpdateTimestamp() {
    Instant now = Instant.now();
    Instant before = now.minusSeconds(3600); // 1 hour ago

    templateConfig.setCreatedAt(before);
    templateConfig.setUpdatedAt(now);
    templateResponse.setCreatedAt(before);
    templateResponse.setUpdatedAt(now);

    when(dtoModelMapper.mapToBrdTemplateConfig(any(BrdTemplateReq.class)))
        .thenReturn(templateConfig);
    when(reactiveMongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(BrdTemplateConfig.class)))
        .thenReturn(Mono.just(templateConfig));
    when(dtoModelMapper.mapToBrdTemplateConfigResponse(any(BrdTemplateConfig.class)))
        .thenReturn(templateResponse);

    StepVerifier.create(brdTemplateService.updateTemplate("123", templateRequest))
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertTrue(response.getBody().getData().isPresent());
              BrdTemplateRes template = response.getBody().getData().get();
              assertNotNull(template.getUpdatedAt());
              assertEquals(before, template.getCreatedAt());
              assertEquals(now, template.getUpdatedAt());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createTemplate_WithInvalidTemplateName_ShouldFail() {
    templateRequest.setTemplateName("");

    StepVerifier.create(brdTemplateService.createTemplate(templateRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("Template name cannot be empty"))
        .verify();
  }

  @Test
  void createTemplate_WithNullTemplateName_ShouldFail() {
    templateRequest.setTemplateName(null);

    StepVerifier.create(brdTemplateService.createTemplate(templateRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("Template name cannot be null"))
        .verify();
  }
}
