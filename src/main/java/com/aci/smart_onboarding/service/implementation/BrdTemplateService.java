package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
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
import com.aci.smart_onboarding.service.IBrdTemplateService;
import com.aci.smart_onboarding.util.BrdFormInitializer;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service implementation for managing BRD (Business Requirements Document) templates. Handles CRUD
 * operations for templates and BRD form generation.
 *
 * @author Smart Onboarding Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class BrdTemplateService implements IBrdTemplateService {

  private static final String TEMPLATE_NOT_FOUND = "Template not found with id: ";
  private static final String BRD_NOT_FOUND = "BRD not found with id: ";
  private static final String TEMPLATE_TYPE_ERROR = "Template type cannot be null or empty";
  private static final String ACCESS_DENIED_MESSAGE =
      "Access denied. Only Manager role can access templates";
  private static final String FAILURE_STATUS = "failure";
  private static final String TEMPLATE_NAME = "success";

  private final BRDTemplateRepository brdTemplateRepository;
  private final DtoModelMapper dtoModelMapper;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  /**
   * Creates a new BRD template.
   *
   * @param brdTemplateReq The template request containing configuration
   * @return A Mono containing the created template response
   */
  @Override
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> createTemplate(BrdTemplateReq brdTemplateReq) {
    return Mono.just(brdTemplateReq)
        .flatMap(this::validateTemplateRequest)
        .map(dtoModelMapper::mapToBrdTemplateConfig)
        .flatMap(brdTemplateRepository::save)
        .map(dtoModelMapper::mapToBrdTemplateConfigResponse)
        .map(
            response ->
                createSuccessResponse(
                    response, HttpStatus.CREATED, "Template created successfully"))
        .onErrorResume(this::handleAccessDenied)
        .onErrorMap(this::handleErrors);
  }

  /**
   * Validates the template request.
   *
   * @param req The template request to validate
   * @return A Mono containing the validated request
   */
  private Mono<BrdTemplateReq> validateTemplateRequest(BrdTemplateReq req) {
    if (req.getTemplateName() == null) {
      return Mono.error(new BadRequestException(TEMPLATE_NAME, "Template name cannot be null"));
    }
    if (req.getTemplateName().trim().isEmpty()) {
      return Mono.error(new BadRequestException(TEMPLATE_NAME, "Template name cannot be empty"));
    }
    return Mono.just(req);
  }

  /**
   * Creates a success response with the given data and status.
   *
   * @param data The response data
   * @param status The HTTP status
   * @param message The success message
   * @return The response entity
   */
  private <T> ResponseEntity<Api<T>> createSuccessResponse(
      T data, HttpStatus status, String message) {
    Api<T> api = new Api<>(BrdConstants.SUCCESSFUL, message, Optional.of(data), Optional.empty());
    return ResponseEntity.status(status).body(api);
  }

  /**
   * Handles access denied errors.
   *
   * @param e The exception
   * @return A Mono containing the error response
   */
  private <T> Mono<ResponseEntity<Api<T>>> handleAccessDenied(Throwable e) {
    if (e instanceof AccessDeniedException) {
      Api<T> api =
          new Api<>(
              FAILURE_STATUS,
              ACCESS_DENIED_MESSAGE,
              Optional.empty(),
              Optional.of(Collections.singletonMap("error", ACCESS_DENIED_MESSAGE)));
      return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(api));
    }
    return Mono.error(e);
  }

  @Override
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> updateTemplate(
      String id, BrdTemplateReq brdTemplateReq) {
    BrdTemplateConfig brdTemplateConfig = dtoModelMapper.mapToBrdTemplateConfig(brdTemplateReq);

    Query query = new Query(Criteria.where("_id").is(id));
    Update update =
        new Update()
            .set("templateName", brdTemplateConfig.getTemplateName())
            .set("templateTypes", brdTemplateConfig.getTemplateTypes())
            .set("summary", brdTemplateConfig.getSummary())
            .set("clientInformation", brdTemplateConfig.isClientInformation())
            .set("aciInformation", brdTemplateConfig.isAciInformation())
            .set("paymentChannels", brdTemplateConfig.isPaymentChannels())
            .set("fundingMethods", brdTemplateConfig.isFundingMethods())
            .set("achPaymentProcessing", brdTemplateConfig.isAchPaymentProcessing())
            .set("miniAccountMaster", brdTemplateConfig.isMiniAccountMaster())
            .set("accountIdentifierInformation", brdTemplateConfig.isAccountIdentifierInformation())
            .set("paymentRules", brdTemplateConfig.isPaymentRules())
            .set("notifications", brdTemplateConfig.isNotifications())
            .set("remittance", brdTemplateConfig.isRemittance())
            .set("agentPortal", brdTemplateConfig.isAgentPortal())
            .set("recurringPayments", brdTemplateConfig.isRecurringPayments())
            .set("ivr", brdTemplateConfig.isIvr())
            .set("generalImplementations", brdTemplateConfig.isGeneralImplementations())
            .set("approvals", brdTemplateConfig.isApprovals())
            .set("revisionHistory", brdTemplateConfig.isRevisionHistory())
            .set("updatedAt", Instant.now());

    FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(false);

    return reactiveMongoTemplate
        .findAndModify(query, update, options, BrdTemplateConfig.class)
        .switchIfEmpty(Mono.error(new NotFoundException(TEMPLATE_NOT_FOUND + id)))
        .map(dtoModelMapper::mapToBrdTemplateConfigResponse)
        .map(
            response ->
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "Template updated successfully",
                            Optional.of(response),
                            Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<List<BrdTemplateRes>>>> getAllTemplates() {
    return brdTemplateRepository
        .findAll()
        .map(dtoModelMapper::mapToBrdTemplateConfigResponse)
        .map(
            template -> {
              template.setPercentage(50); // Set default percentage for all templates
              return template;
            })
        .collectList()
        .map(
            templates -> {
              if (templates.isEmpty()) {
                throw new NotFoundException("No templates found");
              }
              return ResponseEntity.ok()
                  .body(
                      new Api<>(
                          BrdConstants.SUCCESSFUL,
                          String.format("Found %d templates", templates.size()),
                          Optional.of(templates),
                          Optional.empty()));
            })
        .onErrorResume(this::handleAccessDenied)
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> getTemplateByType(String brdTemplateType) {
    return Mono.justOrEmpty(brdTemplateType)
        .filter(type -> !type.trim().isEmpty())
        .switchIfEmpty(Mono.error(new BadRequestException("brdTemplateType", TEMPLATE_TYPE_ERROR)))
        .flatMap(
            type ->
                brdTemplateRepository
                    .findByTemplateTypes(type)
                    .switchIfEmpty(
                        Mono.error(new NotFoundException("Template not found with type: " + type))))
        .map(dtoModelMapper::mapToBrdTemplateConfigResponse)
        .map(
            response ->
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "Template found successfully",
                            Optional.of(response),
                            Optional.empty())))
        .onErrorResume(this::handleAccessDenied)
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException) {
      return ex;
    } else if (ex instanceof DuplicateKeyException) {
      return new AlreadyExistException("Template config already exists with given type");
    } else if (ex instanceof Exception && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  @Override
  public Mono<BrdFormResponse> getBrdForm(String brdId, BrdTemplateRes templateConfig) {
    final boolean clientInfoEnabled = templateConfig.isClientInformation();
    final boolean aciInfoEnabled = templateConfig.isAciInformation();
    final boolean paymentChannelsEnabled = templateConfig.isPaymentChannels();
    final boolean fundingMethodsEnabled = templateConfig.isFundingMethods();
    final boolean achPaymentProcessingEnabled = templateConfig.isAchPaymentProcessing();
    final boolean miniAccountMasterEnabled = templateConfig.isMiniAccountMaster();
    final boolean accountIdentifierInfoEnabled = templateConfig.isAccountIdentifierInformation();
    final boolean paymentRulesEnabled = templateConfig.isPaymentRules();
    final boolean notificationsEnabled = templateConfig.isNotifications();
    final boolean remittanceEnabled = templateConfig.isRemittance();
    final boolean agentPortalEnabled = templateConfig.isAgentPortal();
    final boolean recurringPaymentsEnabled = templateConfig.isRecurringPayments();
    final boolean ivrEnabled = templateConfig.isIvr();
    final boolean generalImplementationsEnabled = templateConfig.isGeneralImplementations();
    final boolean approvalsEnabled = templateConfig.isApprovals();
    final boolean revisionHistoryEnabled = templateConfig.isRevisionHistory();

    return reactiveMongoTemplate
        .findOne(Query.query(Criteria.where("brdFormId").is(brdId)), BRD.class)
        .switchIfEmpty(Mono.error(new NotFoundException(BRD_NOT_FOUND + brdId)))
        .map(
            brd -> {
              if (brd == null) {
                throw new NotFoundException(BRD_NOT_FOUND + brdId);
              }
              BrdFormResponse.BrdFormResponseBuilder builder =
                  BrdFormResponse.builder()
                      .brdFormId(brd.getBrdFormId())
                      .status(brd.getStatus())
                      .projectId(brd.getProjectId())
                      .brdId(brd.getBrdId())
                      .brdName(brd.getBrdName())
                      .description(brd.getDescription())
                      .organizationId(brd.getCustomerId())
                      .creator(brd.getCreator())
                      .type(brd.getType())
                      .notes(brd.getNotes())
                      .createdAt(brd.getCreatedAt())
                      .updatedAt(brd.getUpdatedAt())
                      .templateType(templateConfig.getTemplateTypes())
                      .summary(templateConfig.getSummary());

              Map<String, Object> sections = new HashMap<>();
              addSectionIfEnabled(
                  sections,
                  "clientInformation",
                  clientInfoEnabled,
                  brd::getClientInformation,
                  BrdFormInitializer::createClientInformation);

              addSectionIfEnabled(
                  sections,
                  "aciInformation",
                  aciInfoEnabled,
                  brd::getAciInformation,
                  BrdFormInitializer::createAciInformation);

              addSectionIfEnabled(
                  sections,
                  "paymentChannels",
                  paymentChannelsEnabled,
                  brd::getPaymentChannels,
                  BrdFormInitializer::createPaymentChannels);

              addSectionIfEnabled(
                  sections,
                  "fundingMethods",
                  fundingMethodsEnabled,
                  brd::getFundingMethods,
                  BrdFormInitializer::createFundingMethods);

              addSectionIfEnabled(
                  sections,
                  "achPaymentProcessing",
                  achPaymentProcessingEnabled,
                  brd::getAchPaymentProcessing,
                  BrdFormInitializer::createAchPaymentProcessing);

              addSectionIfEnabled(
                  sections,
                  "miniAccountMaster",
                  miniAccountMasterEnabled,
                  brd::getMiniAccountMaster,
                  BrdFormInitializer::createMiniAccountMaster);

              addSectionIfEnabled(
                  sections,
                  "accountIdentifierInformation",
                  accountIdentifierInfoEnabled,
                  brd::getAccountIdentifierInformation,
                  BrdFormInitializer::createAccountIdentifierInformation);

              addSectionIfEnabled(
                  sections,
                  "paymentRules",
                  paymentRulesEnabled,
                  brd::getPaymentRules,
                  BrdFormInitializer::createPaymentRules);

              addSectionIfEnabled(
                  sections,
                  "notifications",
                  notificationsEnabled,
                  brd::getNotifications,
                  BrdFormInitializer::createNotifications);

              addSectionIfEnabled(
                  sections,
                  "remittance",
                  remittanceEnabled,
                  brd::getRemittance,
                  BrdFormInitializer::createRemittance);

              addSectionIfEnabled(
                  sections,
                  "agentPortal",
                  agentPortalEnabled,
                  brd::getAgentPortal,
                  BrdFormInitializer::createAgentPortalConfig);

              addSectionIfEnabled(
                  sections,
                  "recurringPayments",
                  recurringPaymentsEnabled,
                  brd::getRecurringPayments,
                  BrdFormInitializer::createRecurringPayments);

              addSectionIfEnabled(
                  sections, "ivr", ivrEnabled, brd::getIvr, BrdFormInitializer::createIvr);

              addSectionIfEnabled(
                  sections,
                  "generalImplementations",
                  generalImplementationsEnabled,
                  brd::getGeneralImplementations,
                  BrdFormInitializer::createGeneralImplementations);

              addSectionIfEnabled(
                  sections,
                  "approvals",
                  approvalsEnabled,
                  brd::getApprovals,
                  BrdFormInitializer::createApprovals);

              addSectionIfEnabled(
                  sections,
                  "revisionHistory",
                  revisionHistoryEnabled,
                  brd::getRevisionHistory,
                  BrdFormInitializer::createRevisionHistory);

              builder.sections(sections);
              return builder.build();
            });
  }

  private <T> void addSectionIfEnabled(
      Map<String, Object> sections,
      String sectionName,
      boolean isEnabled,
      Supplier<T> existingGetter,
      Supplier<T> defaultCreator) {
    if (isEnabled) {
      T existingValue = existingGetter.get();
      sections.put(sectionName, existingValue != null ? existingValue : defaultCreator.get());
    }
  }

  @Override
  public Mono<ResponseEntity<Api<BrdFormResponse>>> getBrdFormByIdAndTemplateType(
      String brdId, String templateType) {
    return Mono.justOrEmpty(templateType)
        .filter(type -> !type.trim().isEmpty())
        .switchIfEmpty(Mono.error(new BadRequestException("templateType", TEMPLATE_TYPE_ERROR)))
        .flatMap(this::getTemplateByType)
        .flatMap(
            templateResponse ->
                Mono.justOrEmpty(templateResponse.getBody())
                    .flatMap(apiBody -> Mono.justOrEmpty(apiBody.getData()))
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(
                                "Template configuration not found for type: " + templateType)))
                    .flatMap(
                        templateConfig ->
                            getBrdForm(brdId, templateConfig)
                                .map(
                                    brdFormResponse ->
                                        ResponseEntity.ok()
                                            .body(
                                                new Api<>(
                                                    BrdConstants.SUCCESSFUL,
                                                    "BRD form generated successfully",
                                                    Optional.of(brdFormResponse),
                                                    Optional.empty())))))
        .onErrorMap(this::handleErrors);
  }
}
