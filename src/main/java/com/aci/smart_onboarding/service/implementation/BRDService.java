package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdTemplateConfig;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.ISiteService;
import com.aci.smart_onboarding.util.CustomBrdValidator;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BRDService implements IBRDService {

  private static final Logger log = LoggerFactory.getLogger(BRDService.class);
  private final BRDRepository brdRepository;
  private final DtoModelMapper dtoModelMapper;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final CustomBrdValidator customBrdValidator;
  private final AuditLogService auditLogService;
  private final ISiteService siteService;
  private final BRDSecurityService securityService;

  @Override
  public Mono<ResponseEntity<Api<BRDResponse>>> createBrdForm(BRDRequest brdRequest) {
    return Mono.justOrEmpty(brdRequest)
        .filter(Objects::nonNull)
        .switchIfEmpty(Mono.error(new BadRequestException("brdRequest", "cannot be null or empty")))
        .map(dtoModelMapper::mapToBrd)
        .flatMap(brd -> brdRepository.save(brd).onErrorResume(Mono::error))
        .map(dtoModelMapper::mapToBrdResponse)
        .flatMap(
            response -> {
              ResponseEntity<Api<BRDResponse>> responseEntity =
                  ResponseEntity.status(HttpStatus.CREATED)
                      .body(
                          new Api<>(
                              BrdConstants.SUCCESSFUL,
                              "BRD created",
                              Optional.of(response),
                              Optional.empty()));

              return ReactiveSecurityContextHolder.getContext()
                  .map(SecurityContext::getAuthentication)
                  .map(Authentication::getName)
                  .defaultIfEmpty(BrdConstants.SYSTEM)
                  .flatMap(
                      username ->
                          // Get role from securityService
                          securityService
                              .getCurrentUserRole()
                              .defaultIfEmpty(SecurityConstants.UNKNOWN_ROLE)
                              .flatMap(
                                  role -> {
                                    // Create the audit log with the authenticated username and role
                                    AuditLogRequest auditRequest =
                                        AuditLogRequest.builder()
                                            .entityType("BRD")
                                            .entityId(response.getBrdFormId())
                                            .action(BrdConstants.ACTION_CREATE)
                                            .userId(username)
                                            .userName(username)
                                            .userRole(role) // Add user role to audit log
                                            .eventTimestamp(LocalDateTime.now())
                                            .newValues(dtoModelMapper.mapBrdResponseToMAP(response))
                                            .build();

                                    log.debug(
                                        "Creating audit log for BRD creation with user: {}, role: {}",
                                        username,
                                        role);

                                    // Process audit log asynchronously
                                    return auditLogService
                                        .logCreation(auditRequest)
                                        .then(Mono.just(responseEntity));
                                  }))
                  .defaultIfEmpty(
                      // Fallback if security context is not available
                      ResponseEntity.status(HttpStatus.CREATED)
                          .body(
                              new Api<>(
                                  BrdConstants.SUCCESSFUL,
                                  "BRD created (audit logging skipped - no security context)",
                                  Optional.of(response),
                                  Optional.empty())));
            })
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<BRDResponse>>> getBrdById(String brdFormId) {
    return Mono.justOrEmpty(brdFormId)
        .flatMap(
            id ->
                brdRepository
                    .findById(id)
                    .switchIfEmpty(
                        Mono.error(new NotFoundException(BrdConstants.BRD_NOT_FOUND + id))))
        .map(dtoModelMapper::mapToBrdResponse)
        .map(
            brdResponse ->
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "BRD Found",
                            Optional.of(brdResponse),
                            Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  private void flattenNestedMap(Map<?, ?> map, String parentKey, Map<String, Object> result) {
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = entry.getKey().toString();
      Object value = entry.getValue();

      String newKey = parentKey + "." + key;

      if (value instanceof Map<?, ?>) {
        // Recursive call for nested maps
        flattenNestedMap((Map<?, ?>) value, newKey, result);
      } else {
        // Add leaf node
        result.put(newKey, value);
      }
    }
  }

  @Override
  public Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> getBrdSectionById(
      String brdFormId, String sectionName) {
    if (!customBrdValidator.isValidField(sectionName)) {
      return Mono.error(new BadRequestException("Invalid section name: " + sectionName));
    }
    return Mono.fromCallable(
            () -> {
              try {
                return new ObjectId(brdFormId);
              } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                    "Invalid brdFormId format. Must be a valid 24-character hexadecimal ObjectId.");
              }
            })
        .flatMap(
            objectId -> {
              Query query = Query.query(Criteria.where("_id").is(objectId));
              query.fields().include(BrdConstants.BRD_ID).include(sectionName);

              return reactiveMongoTemplate
                  .findOne(query, Map.class, "brd")
                  .switchIfEmpty(
                      Mono.error(
                          new NotFoundException("BRD not found with brdFormId: " + brdFormId)));
            })
        .map(
            result -> {
              if (!result.containsKey(sectionName)) {
                throw new NotFoundException(
                    "Section " + sectionName + " not found for given brdFormId");
              }
              return ResponseEntity.ok(
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      "BRD section found successfully",
                      Optional.of(
                          new BRDSectionResponse<>(
                              brdFormId,
                              (String) result.get(BrdConstants.BRD_ID),
                              sectionName,
                              result.get(sectionName))),
                      Optional.empty()));
            })
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<BRDCountDataResponse>>> getBrdList(int page, int size) {
    if (page < 0 || size <= 0) {
      return Mono.error(
          new BadRequestException(
              "Page number must be non-negative and size must be greater than 0."));
    }

    Sort sortedByUpdatedAtAndCreatedAt =
        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"));

    return brdRepository
        .count()
        .flatMap(
            totalCount ->
                brdRepository
                    .findAllBy(PageRequest.of(page, size, sortedByUpdatedAtAndCreatedAt))
                    .collectList()
                    .flatMap(
                        brdSections -> {
                          if (brdSections.isEmpty()) {
                            return Mono.error(
                                new NotFoundException(
                                    "No BRD sections found for the given pagination parameters."));
                          }

                          List<BRDListResponse> brdResponseList =
                              brdSections.stream()
                                  .map(dtoModelMapper::mapToBrdListResponse)
                                  .toList();

                          BRDCountDataResponse responseData =
                              new BRDCountDataResponse(totalCount.intValue(), brdResponseList);

                          Api<BRDCountDataResponse> apiResponse =
                              new Api<>(
                                  BrdConstants.SUCCESSFUL,
                                  "BRD list retrieved successfully",
                                  Optional.of(responseData),
                                  Optional.empty());

                          return Mono.just(ResponseEntity.ok(apiResponse));
                        }))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<Page<BRDSearchResponse>>>> searchBRDs(
      String searchText, int page, int size, String sortBy, String sortDirection) {

    return Mono.defer(
            () -> {
              try {
                // Create sort and page request
                Sort sort = Sort.by(Sort.Direction.fromString(sortDirection.toUpperCase()), sortBy);
                PageRequest pageRequest = PageRequest.of(page, size, sort);

                // Build query with projections
                Query query = new Query();
                query
                    .fields()
                    .include("_id")
                    .include(BrdConstants.BRD_ID)
                    .include("customerId")
                    .include("brdName")
                    .include(BrdConstants.CREATOR)
                    .include("type")
                    .include(BrdConstants.STATUS_FIELD)
                    .include("notes")
                    .include("templateFileName");

                // Add search criteria if provided
                if (searchText != null && !searchText.trim().isEmpty()) {
                  List<Criteria> criteriaList = new ArrayList<>();

                  Arrays.stream(searchText.split(","))
                      .map(String::trim)
                      .filter(term -> !term.isEmpty())
                      .forEach(
                          term -> {
                            String sanitizedTerm = term.replaceAll("[^a-zA-Z0-9\\s,-]", "");
                            criteriaList.add(
                                new Criteria()
                                    .orOperator(
                                        // Partial matches for ID and Name
                                        Criteria.where(BrdConstants.BRD_ID)
                                            .regex(".*" + sanitizedTerm + ".*", "i"),
                                        Criteria.where("brdName")
                                            .regex(".*" + sanitizedTerm + ".*", "i"),
                                        // Exact matches for creator, type, and status
                                        Criteria.where("creator").is(sanitizedTerm),
                                        Criteria.where("type").is(sanitizedTerm),
                                        Criteria.where(BrdConstants.STATUS_FIELD)
                                            .is(sanitizedTerm)));
                          });

                  if (!criteriaList.isEmpty()) {
                    query.addCriteria(
                        new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
                  }
                }

                // Create separate queries for count and search
                Query countQuery = Query.of(query);
                Query searchQuery = Query.of(query).with(pageRequest);

                // Execute count and search in parallel
                return Mono.zip(
                        // Search query with pagination
                        reactiveMongoTemplate
                            .find(searchQuery, Document.class, "brd")
                            .map(dtoModelMapper::mapToSearchResponse)
                            .collectList(),

                        // Count query
                        reactiveMongoTemplate.count(countQuery, "brd"))
                    .map(
                        tuple -> {
                          List<BRDSearchResponse> results = tuple.getT1();
                          long totalElements = tuple.getT2();

                          // Calculate pagination metadata
                          int totalPages = (int) Math.ceil((double) totalElements / size);
                          boolean isFirst = page == 0;
                          boolean isLast = page >= totalPages - 1;
                          int numberOfElements = results.size();

                          // Create page response
                          Page<BRDSearchResponse> pageResponse =
                              new CustomPageImpl<>(
                                  results,
                                  pageRequest,
                                  totalElements,
                                  isFirst,
                                  isLast,
                                  numberOfElements,
                                  totalPages);

                          return ResponseEntity.ok(
                              new Api<>(
                                  BrdConstants.SUCCESSFUL,
                                  String.format("Found %d results", totalElements),
                                  Optional.of(pageResponse),
                                  Optional.empty()));
                        });

              } catch (Exception e) {
                return Mono.error(handleErrors(e));
              }
            })
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException
        || ex instanceof AccessDeniedException) {
      return ex;
    } else if (ex instanceof DuplicateKeyException) {
      return new AlreadyExistException("Brd already exist for given brdId");
    } else if (ex instanceof Exception && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  @Override
  @Transactional(
      propagation = Propagation.REQUIRED,
      timeout = 30,
      rollbackFor = {Exception.class},
      noRollbackFor = {NotFoundException.class})
  public Mono<ResponseEntity<Api<BRDResponse>>> updateBrdPartiallyWithOrderedOperations(
      String brdFormId, Map<String, Object> fields) {
    return validateAndPrepareUpdate(brdFormId, fields)
        .flatMap(this::performUpdate)
        .switchIfEmpty(handleAccessDenied(brdFormId, "UPDATE", fields))
        .onErrorMap(this::handleErrors);
  }

  private Mono<UpdateContext> validateAndPrepareUpdate(
      String brdFormId, Map<String, Object> fields) {
    return customBrdValidator
        .validatePartialUpdateField(fields, BRDRequest.class)
        .flatMap(
            validFields -> {
              Update update = new Update();
              Map<String, Object> changedFields = new HashMap<>();
              validFields.forEach(
                  (key, value) -> {
                    if (value != null) {
                      update.set(key, value);
                      changedFields.put(key, value);
                    }
                  });

              Query query = Query.query(Criteria.where(BrdConstants.BRD_FORM_ID).is(brdFormId));
              query
                  .fields()
                  .include(BrdConstants.BRD_FORM_ID)
                  .include(BrdConstants.BRD_ID)
                  .include(BrdConstants.CREATOR);
              changedFields.keySet().forEach(key -> query.fields().include(key));

              return findExistingBrd(query)
                  .flatMap(
                      existingBrd ->
                          createUpdateContext(
                              brdFormId, validFields, update, changedFields, existingBrd));
            });
  }

  private Mono<BRD> findExistingBrd(Query query) {
    return reactiveMongoTemplate
        .findOne(query, BRD.class)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    "BRD not found with id: "
                        + query.getQueryObject().get(BrdConstants.BRD_FORM_ID))));
  }

  private Mono<UpdateContext> createUpdateContext(
      String brdFormId,
      Map<String, Object> validFields,
      Update update,
      Map<String, Object> changedFields,
      BRD existingBrd) {
    return securityService
        .canModifyBrd(existingBrd.getCreator())
        .flatMap(
            canModify -> {
              if (Boolean.FALSE.equals(canModify)) {
                return Mono.empty();
              }

              Map<String, Object> oldValues = extractOldValues(existingBrd, changedFields);

              return getCurrentUserInfo()
                  .map(
                      userInfo ->
                          new UpdateContext(
                              brdFormId,
                              validFields,
                              update,
                              changedFields,
                              existingBrd,
                              oldValues,
                              userInfo.getUsername(),
                              userInfo.getRole()));
            });
  }

  private Map<String, Object> extractOldValues(BRD existingBrd, Map<String, Object> changedFields) {
    return changedFields.keySet().stream()
        .filter(key -> dtoModelMapper.getFieldValue(existingBrd, key) != null)
        .collect(
            HashMap::new,
            (map, key) -> map.put(key, dtoModelMapper.getFieldValue(existingBrd, key)),
            HashMap::putAll);
  }

  private Mono<UserInfo> getCurrentUserInfo() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName)
        .defaultIfEmpty(BrdConstants.SYSTEM)
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .defaultIfEmpty(SecurityConstants.UNKNOWN_ROLE)
                    .map(role -> new UserInfo(username, role)));
  }

  private Mono<ResponseEntity<Api<BRDResponse>>> performUpdate(UpdateContext context) {
    AuditLogRequest auditRequest = createAuditLogRequest(context);

    Mono<BRD> brdUpdateMono = updateBrd(context);
    Mono<Object> siteUpdateMono = updateSites(context);

    return Mono.zip(brdUpdateMono, siteUpdateMono)
        .flatMap(
            tuple -> {
              BRD updatedBrd = tuple.getT1();
              auditRequest.setComment("Update successful (SUCCESS)");

              return auditLogService
                  .logCreation(auditRequest)
                  .onErrorResume(
                      error -> {
                        log.error("Error processing audit log: {}", error.getMessage());
                        return Mono.empty();
                      })
                  .then(createSuccessResponse(updatedBrd));
            });
  }

  private Mono<BRD> updateBrd(UpdateContext context) {
    return reactiveMongoTemplate.findAndModify(
        Query.query(Criteria.where(BrdConstants.BRD_FORM_ID).is(context.getBrdFormId())),
        context.getUpdate(),
        FindAndModifyOptions.options().returnNew(true),
        BRD.class);
  }

  private Mono<Object> updateSites(UpdateContext context) {
    return extractBrdFormFields(context.getValidFields())
        .flatMap(
            brdFormFields -> {
              if (brdFormFields.isEmpty()) {
                return Mono.just(new Object());
              }
              Mono<ResponseEntity<Api<SiteResponse>>> siteUpdateResult =
                  siteService.updateBrdFormFieldsForAllSites(context.getBrdFormId(), brdFormFields);
              return siteUpdateResult != null
                  ? siteUpdateResult.map(Object.class::cast)
                  : Mono.just(new Object());
            })
        .onErrorResume(e -> Mono.just(new Object()));
  }

  private AuditLogRequest createAuditLogRequest(UpdateContext context) {
    return AuditLogRequest.builder()
        .entityType("BRD")
        .entityId(context.getBrdFormId())
        .action("UPDATE")
        .userId(context.getUsername())
        .userName(context.getUsername())
        .userRole(context.getRole())
        .eventTimestamp(LocalDateTime.now())
        .oldValues(context.getOldValues())
        .newValues(context.getChangedFields())
        .build();
  }

  private Mono<ResponseEntity<Api<BRDResponse>>> createSuccessResponse(BRD updatedBrd) {
    return Mono.just(
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "BRD updated successfully",
                Optional.of(dtoModelMapper.mapToBrdResponse(updatedBrd)),
                Optional.empty())));
  }

  // Helper class to hold update context
  @lombok.Value
  private static class UpdateContext {
    String brdFormId;
    Map<String, Object> validFields;
    Update update;
    Map<String, Object> changedFields;
    BRD existingBrd;
    Map<String, Object> oldValues;
    String username;
    String role;
  }

  // Helper class to hold user information
  @lombok.Value
  private static class UserInfo {
    String username;
    String role;
  }

  public Mono<ResponseEntity<Api<BRDResponse>>> updateBrdStatus(
      String brdFormId, String status, String comment) {
    Query query = Query.query(Criteria.where(BrdConstants.BRD_FORM_ID).is(brdFormId));
    Update update = new Update().set(BrdConstants.STATUS_FIELD, status);

    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName)
        .defaultIfEmpty(BrdConstants.SYSTEM)
        .flatMap(
            username ->
                // Get role from securityService
                securityService
                    .getCurrentUserRole()
                    .defaultIfEmpty("UNKNOWN_ROLE")
                    .flatMap(
                        role -> {
                          log.debug("Updating BRD status with user: {}, role: {}", username, role);

                          return reactiveMongoTemplate
                              .findAndModify(
                                  query,
                                  update,
                                  FindAndModifyOptions.options().returnNew(true),
                                  BRD.class)
                              .switchIfEmpty(
                                  Mono.error(
                                      new NotFoundException("BRD not found with id: " + brdFormId)))
                              .map(
                                  updatedBrd -> {
                                    BRDResponse response =
                                        dtoModelMapper.mapToBrdResponse(updatedBrd);

                                    // Create maps for old and new values
                                    Map<String, Object> oldValues =
                                        Collections.singletonMap(BrdConstants.STATUS_FIELD, "");
                                    Map<String, Object> newValues =
                                        Collections.singletonMap(BrdConstants.STATUS_FIELD, status);

                                    // Create audit log request with the authenticated username and
                                    // role
                                    AuditLogRequest auditRequest =
                                        AuditLogRequest.builder()
                                            .entityId(brdFormId)
                                            .entityType("BRD")
                                            .action(BrdConstants.ACTION_STATUS_UPDATE)
                                            .userId(username)
                                            .userName(username)
                                            .comment(comment)
                                            .userRole(role) // Include the user role
                                            .eventTimestamp(LocalDateTime.now())
                                            .oldValues(oldValues)
                                            .newValues(newValues)
                                            .build();

                                    // Process audit log asynchronously
                                    return processAuditLogAsynchronously(auditRequest)
                                        .then(
                                            Mono.just(
                                                ResponseEntity.ok(
                                                    new Api<>(
                                                        BrdConstants.SUCCESSFUL,
                                                        "BRD status updated successfully",
                                                        Optional.of(response),
                                                        Optional.empty()))));
                                  });
                        }))
        .defaultIfEmpty(
            Mono.just(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                        new Api<>(
                            BrdConstants.FAILURE,
                            "Authentication required to update BRD status",
                            Optional.empty(),
                            Optional.empty()))))
        .flatMap(mono -> mono)
        .onErrorResume(Mono::error);
  }

  @Override
  public Mono<List<String>> getIndustryVerticals() {
    // Query to get all template types from brd_template_config collection
    return reactiveMongoTemplate
        .findDistinct("templateTypes", BrdTemplateConfig.class, String.class)
        .collectList()
        .map(
            verticals -> {
              // Create a new list to avoid UnsupportedOperationException
              List<String> allVerticals = new ArrayList<>(verticals);
              // Add "Other" if not already present
              if (!allVerticals.contains("Other")) {
                allVerticals.add("Other");
              }
              return allVerticals;
            });
  }

  @Override
  public Mono<List<BRDResponse>> getBrdsByPmUsername(String username) {
    log.debug("Fetching BRDs for PM username: {}", username);

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Access denied. Only Manager role can access this endpoint."));
              }

              Query query = new Query(Criteria.where(BrdConstants.CREATOR).is(username));
              return reactiveMongoTemplate
                  .find(query, BRD.class)
                  .map(dtoModelMapper::mapToBrdResponse)
                  .collectList()
                  .flatMap(
                      brds -> {
                        if (brds.isEmpty()) {
                          return Mono.error(
                              new NotFoundException("No BRDs found for PM: " + username));
                        }
                        return Mono.just(brds);
                      })
                  .onErrorMap(
                      error -> {
                        log.error(
                            "Error retrieving BRDs for PM {}: {}", username, error.getMessage());
                        if (error instanceof AccessDeniedException
                            || error instanceof NotFoundException) {
                          return error;
                        }
                        return new RuntimeException(
                            "An error occurred while retrieving BRDs", error);
                      });
            });
  }

  private Mono<Map<String, Object>> extractBrdFormFields(Map<String, Object> brdFields) {
    if (brdFields == null || brdFields.isEmpty()) {
      return Mono.just(Collections.emptyMap());
    }

    Map<String, Object> brdFormFields = new HashMap<>();
    brdFields.forEach((key, value) -> processField(key, value, brdFormFields));
    return Mono.just(brdFormFields);
  }

  private void processField(String key, Object value, Map<String, Object> brdFormFields) {
    if (key.startsWith("brdForm.")) {
      // Remove the "brdForm." prefix and add the field
      String actualKey = key.substring("brdForm.".length());
      brdFormFields.put(actualKey, value);
    }
  }

  private Mono<Void> processAuditLogAsynchronously(AuditLogRequest auditRequest) {
    return auditLogService
        .logCreation(auditRequest)
        .onErrorResume(
            error -> {
              log.error("Error processing audit log: {}", error.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private Mono<ResponseEntity<Api<BRDResponse>>> handleAccessDenied(
      String brdFormId, String action, Map<String, Object> fields) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName)
        .defaultIfEmpty(BrdConstants.SYSTEM)
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .defaultIfEmpty(SecurityConstants.UNKNOWN_ROLE)
                    .flatMap(
                        role -> {
                          AuditLogRequest auditRequest =
                              AuditLogRequest.builder()
                                  .entityType("BRD")
                                  .entityId(brdFormId)
                                  .action(action)
                                  .userId(username)
                                  .userName(username)
                                  .userRole(role)
                                  .eventTimestamp(LocalDateTime.now())
                                  .comment("Access denied - Insufficient permissions (DENIED)")
                                  .oldValues(Collections.emptyMap())
                                  .newValues(fields)
                                  .build();

                          return processAuditLogAsynchronously(auditRequest)
                              .then(
                                  Mono.just(
                                      ResponseEntity.status(HttpStatus.FORBIDDEN)
                                          .body(
                                              new Api<>(
                                                  BrdConstants.FAILURE,
                                                  "This BRD is currently in In Progress status and not yet open for collaboration.",
                                                  Optional.empty(),
                                                  Optional.empty()))));
                        }));
  }
}
