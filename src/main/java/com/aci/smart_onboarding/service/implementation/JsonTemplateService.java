package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.JsonTemplateConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.JsonTemplateResponse;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.exception.TemplateNotFoundException;
import com.aci.smart_onboarding.model.JsonTemplate;
import com.aci.smart_onboarding.repository.JsonTemplateRepository;
import com.aci.smart_onboarding.service.IJsonTemplateService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service implementation for JSON template operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonTemplateService implements IJsonTemplateService {

  private final JsonTemplateRepository jsonTemplateRepository;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  @Override
  public Mono<ResponseEntity<Api<JsonTemplateResponse>>> createTemplate(
      String templateName, String fileName, String originalFileName, String uploadedBy) {
    log.info("Creating template: templateName={}, uploadedBy={}", templateName, uploadedBy);

    // Validate template data synchronously
    String validationResult = validateTemplateData(templateName, fileName, originalFileName);
    if (validationResult != null) {
      log.warn("Template validation failed: {}", validationResult);
      return Mono.error(new BadRequestException(validationResult));
    }

    // Build template record with optimized field assignment
    LocalDateTime now = LocalDateTime.now(); // Single timestamp for consistency
    JsonTemplate template =
        JsonTemplate.builder()
            .templateName(templateName.trim())
            .fileName(fileName.trim())
            .originalFileName(originalFileName.trim())
            .uploadedBy(uploadedBy != null ? uploadedBy.trim() : "system")
            .status(JsonTemplateConstants.STATUS_INACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Save to database and build response with optimized chain
    return jsonTemplateRepository
        .save(template)
        .map(
            savedTemplate -> {
              // Direct DTO creation for better performance
              JsonTemplateResponse responseDto =
                  JsonTemplateResponse.builder()
                      .id(savedTemplate.getId())
                      .templateName(savedTemplate.getTemplateName())
                      .fileName(savedTemplate.getFileName())
                      .originalFileName(savedTemplate.getOriginalFileName())
                      .uploadedBy(savedTemplate.getUploadedBy())
                      .status(savedTemplate.getStatus())
                      .createdAt(savedTemplate.getCreatedAt())
                      .updatedAt(savedTemplate.getUpdatedAt())
                      .build();

              Api<JsonTemplateResponse> response =
                  new Api<>(
                      HttpStatus.CREATED.toString(),
                      "Template created successfully with all details",
                      Optional.of(responseDto),
                      Optional.empty());

              ResponseEntity<Api<JsonTemplateResponse>> result =
                  ResponseEntity.status(HttpStatus.CREATED).body(response);
              log.info("Template created successfully: templateName={}", templateName);
              return result;
            })
        .doOnError(
            error ->
                log.error(
                    "Template creation failed: templateName={}, error: {}",
                    templateName,
                    error.getMessage()))
        .onErrorMap(this::handleError);
  }

  @Override
  public Mono<ResponseEntity<Api<List<JsonTemplateResponse>>>> getAllTemplates() {
    log.debug("Retrieving all templates");

    return jsonTemplateRepository
        .findAll()
        .map(this::mapToResponseDto) // Convert each entity to DTO
        .collectList() // Collect all DTOs into a list
        .map(this::buildSuccessResponseWithList) // Build response with list
        .doOnSuccess(
            response ->
                log.debug(
                    "All templates retrieved successfully. Count: {}",
                    response.getBody().getData().map(List::size).orElse(0)))
        .onErrorMap(this::handleError);
  }

  @Override
  public Mono<ResponseEntity<Api<Boolean>>> updateTemplateStatusWithSingleActive(
      String templateName, String status) {
    log.info(
        "Updating template status with single active constraint: templateName={}, newStatus={}",
        templateName,
        status);

    // For Active status, ensure only one template is active at a time
    return ensureSingleActiveTemplate(templateName)
        .flatMap(
            result -> {
              Api<Boolean> response =
                  new Api<>(
                      HttpStatus.OK.toString(),
                      "Template status updated successfully. Only one template is now active.",
                      Optional.of(true),
                      Optional.empty());

              log.info(
                  "Template status updated with single active constraint: templateName={}, status={}",
                  templateName,
                  status);
              return Mono.just(ResponseEntity.ok(response));
            })
        .onErrorMap(this::handleError);
  }

  /**
   * Ensures only one template is active at a time using reactive bulk operations. This method
   * performs atomic operations to maintain data consistency.
   */
  private Mono<Boolean> ensureSingleActiveTemplate(String templateName) {
    LocalDateTime now = LocalDateTime.now();

    // Step 1: Verify the target template exists
    Query targetQuery =
        new Query(Criteria.where(JsonTemplateConstants.FIELD_TEMPLATE_NAME).is(templateName));

    return reactiveMongoTemplate
        .exists(targetQuery, JsonTemplate.class)
        .flatMap(
            exists -> {
              if (Boolean.FALSE.equals(exists)) {
                return Mono.error(new NotFoundException("Template not found: " + templateName));
              }

              // Step 2: Deactivate all currently active templates (except the target)
              Query deactivateQuery =
                  new Query(
                      Criteria.where(JsonTemplateConstants.FIELD_STATUS)
                          .is(JsonTemplateConstants.STATUS_ACTIVE)
                          .and(JsonTemplateConstants.FIELD_TEMPLATE_NAME)
                          .ne(templateName));

              Update deactivateUpdate =
                  new Update()
                      .set(
                          JsonTemplateConstants.FIELD_STATUS, JsonTemplateConstants.STATUS_INACTIVE)
                      .set("updatedAt", now);

              return reactiveMongoTemplate
                  .updateMulti(deactivateQuery, deactivateUpdate, JsonTemplate.class)
                  .doOnNext(
                      updateResult ->
                          log.info(
                              "Deactivated {} templates to ensure single active constraint",
                              updateResult.getModifiedCount()))
                  .then(
                      // Step 3: Activate the target template
                      reactiveMongoTemplate.findAndModify(
                          targetQuery,
                          new Update()
                              .set(
                                  JsonTemplateConstants.FIELD_STATUS,
                                  JsonTemplateConstants.STATUS_ACTIVE)
                              .set("updatedAt", now),
                          JsonTemplate.class))
                  .map(
                      updatedTemplate -> {
                        if (updatedTemplate == null) {
                          throw new NotFoundException(
                              "Template not found during activation: " + templateName);
                        }
                        log.info(
                            "Successfully activated template: {} while maintaining single active constraint",
                            templateName);
                        return true;
                      });
            });
  }

  /** Validates template creation data */
  private String validateTemplateData(
      String templateName, String fileName, String originalFileName) {
    if (templateName == null || templateName.trim().isEmpty()) {
      return "Template name cannot be null or empty";
    }
    if (fileName == null || fileName.trim().isEmpty()) {
      return "File name cannot be null or empty";
    }
    if (originalFileName == null || originalFileName.trim().isEmpty()) {
      return "Original file name cannot be null or empty";
    }
    return null;
  }

  /** Maps JsonTemplate entity to JsonTemplateResponse DTO */
  private JsonTemplateResponse mapToResponseDto(JsonTemplate template) {
    return JsonTemplateResponse.builder()
        .id(template.getId())
        .templateName(template.getTemplateName())
        .fileName(template.getFileName())
        .originalFileName(template.getOriginalFileName())
        .uploadedBy(template.getUploadedBy())
        .status(template.getStatus())
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .build();
  }

  /** Builds success response for all templates retrieval */
  private ResponseEntity<Api<List<JsonTemplateResponse>>> buildSuccessResponseWithList(
      List<JsonTemplateResponse> templateList) {
    String message =
        templateList.isEmpty()
            ? "No templates found"
            : String.format("Found %d templates", templateList.size());

    Api<List<JsonTemplateResponse>> response =
        new Api<>(HttpStatus.OK.toString(), message, Optional.of(templateList), Optional.empty());

    return ResponseEntity.ok(response);
  }

  /**
   * Handles errors for JSON template operations by throwing appropriate exceptions for
   * GlobalExceptionHandler
   */
  public Throwable handleError(Throwable throwable) {
    if (throwable instanceof TemplateNotFoundException) {
      return throwable;
    } else if (throwable instanceof IllegalArgumentException) {
      return throwable;
    } else if (throwable instanceof BadRequestException) {
      return throwable;
    } else if (throwable instanceof AlreadyExistException) {
      return throwable;
    } else if (throwable instanceof DuplicateKeyException
        || throwable instanceof org.springframework.dao.DuplicateKeyException
        || (throwable.getMessage() != null
            && throwable.getMessage().contains("duplicate key error")
            && throwable.getMessage().contains(JsonTemplateConstants.FIELD_TEMPLATE_NAME))) {
      return new AlreadyExistException("Template with this name already exists");
    } else {
      log.error("Something went wrong", throwable);
      return new Exception("Something went wrong: " + throwable.getMessage());
    }
  }
}
