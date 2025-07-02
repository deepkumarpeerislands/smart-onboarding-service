package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.SiteConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.InternalServerException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.SiteRepository;
import com.aci.smart_onboarding.service.ISiteService;
import com.aci.smart_onboarding.util.BrdComparisonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteService implements ISiteService {

  private final BRDRepository brdRepository;
  private final SiteRepository divisionRepository;
  private final TransactionalOperator transactionalOperator;
  private final ReactiveMongoTemplate mongoTemplate;
  private final DtoModelMapper dtoModelMapper;

  private static final String SELECTED_FIELD = "selected";
  private static final String VALUE_FIELD = "value";

  private String generateSiteId(String brdId, int sequence) {
    return String.format("SITE_%s_%03d", brdId, sequence);
  }

  @Override
  public Mono<ResponseEntity<Api<SiteResponse>>> createDivision(SiteRequest request) {
    return transactionalOperator
        .transactional( // Wrap the whole transaction at the start
            Mono.justOrEmpty(request)
                .filter(Objects::nonNull)
                .switchIfEmpty(
                    Mono.error(new BadRequestException("request", SiteConstants.REQUEST_NULL)))
                .flatMap(
                    req -> {
                      Set<String> identifierCodes = new HashSet<>();
                      for (SiteRequest.SiteDetails detail : req.getSiteList()) {
                        if (!identifierCodes.add(detail.getIdentifierCode())) {
                          return Mono.error(
                              new BadRequestException(
                                  String.format(
                                      SiteConstants.DUPLICATE_IDENTIFIER_CODE,
                                      detail.getIdentifierCode())));
                        }
                      }
                      return Mono.just(req);
                    })
                .flatMap(
                    req ->
                        brdRepository
                            .findByBrdId(req.getBrdId())
                            .switchIfEmpty(
                                Mono.error(
                                    new NotFoundException(
                                        String.format(
                                            SiteConstants.BRD_NOT_FOUND, req.getBrdId()))))
                            .flatMap(
                                brd -> {
                                  brd.setWallentronIncluded(request.isWallentronIncluded());
                                  brd.setAchEncrypted(request.isAchEncrypted());
                                  return brdRepository.save(brd);
                                })
                            .flatMap(
                                savedBrd ->
                                    divisionRepository
                                        .findByBrdId(req.getBrdId())
                                        .collectList()
                                        .flatMap(
                                            existingSites -> {
                                              int startSequence = existingSites.size() + 1;
                                              List<Site> newSites = new ArrayList<>();
                                              for (int i = 0;
                                                  i < request.getSiteList().size();
                                                  i++) {
                                                SiteRequest.SiteDetails detail =
                                                    request.getSiteList().get(i);
                                                Site site = new Site();
                                                site.setBrdId(req.getBrdId());
                                                site.setSiteId(
                                                    generateSiteId(
                                                        req.getBrdId(), startSequence + i));
                                                site.setSiteName(detail.getSiteName());
                                                site.setIdentifierCode(detail.getIdentifierCode());
                                                site.setDescription(detail.getDescription());
                                                newSites.add(site);
                                              }
                                              return divisionRepository
                                                  .saveAll(newSites)
                                                  .collectList()
                                                  .map(
                                                      savedSites ->
                                                          Tuples.of(savedBrd, savedSites));
                                            }))))
        .retryWhen(
            reactor.util.retry.Retry.backoff(3, Duration.ofMillis(100))
                .filter(
                    throwable ->
                        throwable instanceof MongoException mongoException
                            && mongoException.hasErrorLabel("TransientTransactionError")))
        .map(tuple -> createDivisionResponse(tuple.getT1(), tuple.getT2()))
        .map(
            response ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Api<>(
                            SiteConstants.SUCCESSFUL,
                            "Sites created and BRD updated successfully",
                            Optional.of(response),
                            Optional.empty())))
        .onErrorResume(this::handleError);
  }

  @Override
  public Mono<ResponseEntity<Api<SiteResponse>>> getDivisionsByBrdId(String brdId) {
    return Mono.zip(
            brdRepository
                .findByBrdId(brdId)
                .switchIfEmpty(Mono.error(new NotFoundException("BRD not found with ID: " + brdId)))
                .cast(BRD.class),
            divisionRepository.findByBrdId(brdId).collectList())
        .flatMap(
            tuple -> {
              BRD brd = tuple.getT1();
              List<Site> sites = tuple.getT2();

              // Calculate BRD score
              return calculateBrdScore(brdId)
                  .flatMap(
                      brdScore -> {
                        // Build the basic SiteResponse without siteList first
                        SiteResponse.SiteResponseBuilder responseBuilder =
                            SiteResponse.builder()
                                .brdFormId(brd.getBrdFormId())
                                .brdId(brd.getBrdId())
                                .brdName(brd.getBrdName())
                                .description(brd.getDescription())
                                .customerId(brd.getCustomerId())
                                .wallentronIncluded(brd.isWallentronIncluded())
                                .achEncrypted(brd.isAchEncrypted())
                                .ssdAvailable(brd.isSsdAvailable())
                                .contractAvailable(brd.isContractAvailable())
                                .originalSSDFileName(brd.getOriginalSSDFileName())
                                .originalContractFileName(brd.getOriginalContractFileName())
                                .originalACHFileName(brd.getOriginalACHFileName())
                                .originalOtherFileName(brd.getOriginalOtherFileName())
                                .createdAt(brd.getCreatedAt())
                                .updatedAt(brd.getUpdatedAt())
                                .score(brdScore);

                        // If sites list is empty, return response with empty siteList
                        if (sites.isEmpty()) {
                          return Mono.just(responseBuilder.siteList(List.of()).build());
                        }

                        // Otherwise process sites and add to response
                        List<Mono<SiteResponse.DivisionDetails>> siteScoreMonos =
                            sites.stream()
                                .map(
                                    site -> {
                                      // Calculate individual site score
                                      double siteScore = calculateIndividualSiteScore(site);
                                      return Mono.just(
                                          SiteResponse.DivisionDetails.builder()
                                              .id(site.getId())
                                              .siteId(site.getSiteId())
                                              .siteName(site.getSiteName())
                                              .identifierCode(site.getIdentifierCode())
                                              .description(site.getDescription())
                                              .brdForm(site.getBrdForm())
                                              .score(siteScore)
                                              .build());
                                    })
                                .toList();

                        // Combine all site scores
                        return Mono.zip(
                            siteScoreMonos,
                            siteDetails -> {
                              List<SiteResponse.DivisionDetails> details =
                                  Arrays.asList(siteDetails).stream()
                                      .map(obj -> (SiteResponse.DivisionDetails) obj)
                                      .toList();

                              // Add siteList to the response builder and build
                              return responseBuilder.siteList(details).build();
                            });
                      });
            })
        .map(
            response ->
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            SiteConstants.SUCCESSFUL,
                            "Divisions retrieved successfully",
                            Optional.of(response),
                            Optional.empty())))
        .onErrorResume(this::handleError);
  }

  private double calculateIndividualSiteScore(Site site) {
    if (site.getBrdForm() == null) {
      return 0.0;
    }

    try {
      // Convert BRD form to Map to dynamically access sections
      ObjectMapper mapper = new ObjectMapper();
      Map<?, ?> brdFormMap = mapper.convertValue(site.getBrdForm(), Map.class);

      // Initialize counters
      long totalFields = 0;
      long filledFields = 0;

      // Process each section in BRD form
      for (Map.Entry<?, ?> entry : brdFormMap.entrySet()) {
        String sectionName = entry.getKey().toString();

        // Skip metadata fields
        if (shouldSkipField(sectionName)) {
          continue;
        }

        Object sectionValue = entry.getValue();
        if (sectionValue != null) {
          ScoreResult result = processSectionValue(sectionName, sectionValue, mapper);
          totalFields += result.totalFields;
          filledFields += result.filledFields;
        }
      }

      log.debug(
          "Site {} score calculation - Total fields: {}, Filled fields: {}",
          site.getSiteId(),
          totalFields,
          filledFields);

      return totalFields > 0 ? ((double) filledFields / totalFields) * 100 : 0.0;
    } catch (Exception e) {
      log.error("Error calculating score for site {}: {}", site.getSiteId(), e.getMessage());
      return 0.0;
    }
  }

  private ScoreResult processSectionValue(
      String sectionName, Object sectionValue, ObjectMapper mapper) {
    try {
      // Convert section to Map for consistent processing
      Map<?, ?> sectionMap = mapper.convertValue(sectionValue, Map.class);

      // Count fields in the section using the same logic as BRD score
      ScoreResult result = countFields(sectionMap);

      log.debug(
          "Section {} - Total: {}, Filled: {}",
          sectionName,
          result.totalFields,
          result.filledFields);

      return result;
    } catch (Exception e) {
      log.error("Error processing section {}: {}", sectionName, e.getMessage());
      return new ScoreResult(); // Return empty result in case of error
    }
  }

  private static class ScoreResult {
    long totalFields = 0;
    long filledFields = 0;
  }

  private SiteResponse createDivisionResponse(BRD brd, List<Site> sites) {
    List<SiteResponse.DivisionDetails> siteDetails =
        sites.stream()
            .map(
                site ->
                    SiteResponse.DivisionDetails.builder()
                        .id(site.getId())
                        .siteId(site.getSiteId())
                        .siteName(site.getSiteName())
                        .identifierCode(site.getIdentifierCode())
                        .description(site.getDescription())
                        .brdForm(site.getBrdForm())
                        .score(0.0) // Default score, will be updated in the controller
                        .build())
            .toList();

    return SiteResponse.builder()
        .brdFormId(brd.getBrdFormId())
        .brdId(brd.getBrdId())
        .brdName(brd.getBrdName())
        .description(brd.getDescription())
        .customerId(brd.getCustomerId())
        .wallentronIncluded(brd.isWallentronIncluded())
        .achEncrypted(brd.isAchEncrypted())
        .ssdAvailable(brd.isSsdAvailable())
        .contractAvailable(brd.isContractAvailable())
        .originalSSDFileName(brd.getOriginalSSDFileName())
        .originalContractFileName(brd.getOriginalContractFileName())
        .originalACHFileName(brd.getOriginalACHFileName())
        .originalOtherFileName(brd.getOriginalOtherFileName())
        .siteList(siteDetails)
        .createdAt(brd.getCreatedAt())
        .updatedAt(brd.getUpdatedAt())
        .score(0.0) // Default score, will be updated in the controller
        .build();
  }

  @Override
  public Mono<ResponseEntity<Api<BrdComparisonResponse>>> compareBrdAndSiteBrdForm(
      String siteId, String sectionName) {
    return validateSiteId(siteId)
        .flatMap(this::findSiteAndBrd)
        .flatMap(tuple -> performComparison(tuple.getT1(), tuple.getT2(), sectionName))
        .onErrorResume(this::handleError);
  }

  private Mono<String> validateSiteId(String siteId) {
    return Mono.justOrEmpty(siteId)
        .filter(id -> id != null && !id.isEmpty())
        .switchIfEmpty(
            Mono.error(new BadRequestException(SiteConstants.SITE_ID, SiteConstants.SITE_ID_NULL)));
  }

  private Mono<Tuple2<Site, BRD>> findSiteAndBrd(String siteId) {
    return divisionRepository
        .findById(siteId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(String.format(SiteConstants.SITE_NOT_FOUND, siteId))))
        .flatMap(
            site ->
                brdRepository
                    .findByBrdId(site.getBrdId())
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(
                                String.format(SiteConstants.BRD_NOT_FOUND, site.getBrdId()))))
                    .map(brd -> Tuples.of(site, brd)));
  }

  private Mono<ResponseEntity<Api<BrdComparisonResponse>>> performComparison(
      Site site, BRD brd, String sectionName) {
    validateBrdFormData(brd, site);

    try {
      BrdComparisonResponse comparison =
          BrdComparisonUtil.compareBrdAndSiteBrdForm(brd, site, sectionName);
      return Mono.just(
          ResponseEntity.ok()
              .body(
                  new Api<>(
                      SiteConstants.SUCCESSFUL,
                      "BRD and Site BRD form comparison completed successfully",
                      Optional.of(comparison),
                      Optional.empty())));
    } catch (Exception e) {
      return Mono.error(createComparisonError(e, site, brd));
    }
  }

  private void validateBrdFormData(BRD brd, Site site) {
    if (brd.getBrdFormId() == null) {
      throw new IllegalArgumentException("BRD form ID is null for BRD: " + brd.getBrdId());
    }
    if (site.getBrdForm() == null) {
      throw new IllegalArgumentException("Site BRD form is null for site: " + site.getSiteId());
    }
  }

  public InternalServerException createComparisonError(Exception e, Site site, BRD brd) {
    Map<String, String> errorDetails = new HashMap<>();
    errorDetails.put("errorType", "COMPARISON_ERROR");
    errorDetails.put(
        "originalError", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    errorDetails.put(SiteConstants.TIMESTAMP, LocalDateTime.now().toString());
    errorDetails.put(SiteConstants.SITE_ID, site.getSiteId());
    errorDetails.put(SiteConstants.BRD_ID, site.getBrdId());
    errorDetails.put("brdFormId", brd.getBrdFormId());
    errorDetails.put("siteBrdForm", site.getBrdForm() != null ? "present" : "null");

    return new InternalServerException(SiteConstants.BRD_COMPARISON_FAILED, errorDetails);
  }

  private <T> Mono<ResponseEntity<Api<T>>> handleError(Throwable ex) {
    String status = "FAILURE";
    Map<String, String> errorDetails = new HashMap<>();

    var errorInfo =
        switch (ex) {
          case NotFoundException notFoundEx ->
              new ErrorInfo(HttpStatus.NOT_FOUND, notFoundEx.getMessage(), errorDetails);
          case AlreadyExistException alreadyExistEx ->
              new ErrorInfo(HttpStatus.CONFLICT, alreadyExistEx.getMessage(), errorDetails);
          case BadRequestException badRequestEx ->
              new ErrorInfo(HttpStatus.BAD_REQUEST, badRequestEx.getMessage(), errorDetails);
          case InternalServerException internalServerEx -> {
            errorDetails.putAll(internalServerEx.getErrorDetails());
            yield new ErrorInfo(
                HttpStatus.INTERNAL_SERVER_ERROR, internalServerEx.getMessage(), errorDetails);
          }
          default -> new ErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), errorDetails);
        };

    errorDetails.computeIfAbsent(SiteConstants.TIMESTAMP, k -> LocalDateTime.now().toString());
    errorDetails.computeIfAbsent(SiteConstants.ERROR_MESSAGE, k -> errorInfo.message());

    Api<T> apiResponse =
        new Api<>(status, errorInfo.message(), Optional.empty(), Optional.of(errorDetails));
    return Mono.just(ResponseEntity.status(errorInfo.httpStatus()).body(apiResponse));
  }

  private record ErrorInfo(
      HttpStatus httpStatus, String message, Map<String, String> errorDetails) {}

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException) {
      return ex;
    } else if (ex instanceof DuplicateKeyException) {
      return new AlreadyExistException(SiteConstants.BRD_ALREADY_EXISTS);
    } else if (ex instanceof Exception && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  @Override
  public Mono<ResponseEntity<Api<SingleSiteResponse>>> updateSite(
      String siteId, SiteRequest.SiteDetails siteDetails) {

    return Mono.justOrEmpty(siteId)
        .filter(id -> id != null && !id.isEmpty())
        .switchIfEmpty(
            Mono.error(new BadRequestException(SiteConstants.SITE_ID, SiteConstants.SITE_ID_NULL)))
        .flatMap(
            id -> {
              if (siteDetails == null) {
                return Mono.error(
                    new BadRequestException("siteDetails", SiteConstants.SITE_DETAILS_NULL));
              }

              return divisionRepository
                  .findById(id)
                  .switchIfEmpty(
                      Mono.error(
                          new NotFoundException(String.format(SiteConstants.SITE_NOT_FOUND, id))))
                  .flatMap(
                      existingSite -> {
                        // Selectively update the fields if they are not null in the request
                        if (siteDetails.getSiteName() != null
                            && !siteDetails.getSiteName().isEmpty()) {
                          existingSite.setSiteName(siteDetails.getSiteName());
                        }

                        // Update identifier code if provided, without uniqueness check
                        if (siteDetails.getIdentifierCode() != null
                            && !siteDetails.getIdentifierCode().isEmpty()) {
                          existingSite.setIdentifierCode(siteDetails.getIdentifierCode());
                        }

                        return updateSiteAndRespond(existingSite, siteDetails);
                      });
            })
        .onErrorMap(this::handleErrors);
  }

  private Mono<ResponseEntity<Api<SingleSiteResponse>>> updateSiteAndRespond(
      Site existingSite, SiteRequest.SiteDetails siteDetails) {

    if (siteDetails.getDescription() != null) {
      existingSite.setDescription(siteDetails.getDescription());
    }

    if (siteDetails.getBrdForm() != null) {
      if (existingSite.getBrdForm() == null) {
        existingSite.setBrdForm(siteDetails.getBrdForm());
      } else {
        updateBrdFormFields(existingSite.getBrdForm(), siteDetails.getBrdForm());
      }
    }

    existingSite.setUpdatedAt(LocalDateTime.now());

    return divisionRepository
        .save(existingSite)
        .map(
            updatedSite -> {
              SingleSiteResponse response = mapSiteToResponse(updatedSite);
              return ResponseEntity.ok()
                  .body(
                      new Api<>(
                          "SUCCESS",
                          "Site updated successfully",
                          Optional.of(response),
                          Optional.empty()));
            });
  }

  private void updateBrdFormFields(BrdForm existingForm, BrdForm updateForm) {
    BeanUtils.copyProperties(updateForm, existingForm, getNullPropertyNames(updateForm));
  }

  private String[] getNullPropertyNames(Object source) {
    final BeanWrapper src = new BeanWrapperImpl(source);
    java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

    Set<String> emptyNames = new HashSet<>();
    for (java.beans.PropertyDescriptor pd : pds) {
      Object srcValue = src.getPropertyValue(pd.getName());
      if (srcValue == null) emptyNames.add(pd.getName());
    }

    String[] result = new String[emptyNames.size()];
    return emptyNames.toArray(result);
  }

  private SingleSiteResponse mapSiteToResponse(Site site) {
    return SingleSiteResponse.builder()
        .id(site.getId())
        .brdId(site.getBrdId())
        .siteId(site.getSiteId())
        .siteName(site.getSiteName())
        .identifierCode(site.getIdentifierCode())
        .description(site.getDescription())
        .brdForm(site.getBrdForm())
        .createdAt(site.getCreatedAt())
        .updatedAt(site.getUpdatedAt())
        .build();
  }

  @Override
  public Mono<ResponseEntity<Api<SiteResponse>>> updateBrdFormFieldsForAllSites(
      String brdId, Map<String, Object> brdFormFields) {

    return Mono.justOrEmpty(brdId)
        .filter(id -> id != null && !id.isEmpty())
        .switchIfEmpty(
            Mono.error(new BadRequestException(SiteConstants.BRD_ID, SiteConstants.NOT_NULL)))
        .flatMap(
            id -> {
              if (brdFormFields == null || brdFormFields.isEmpty()) {
                return Mono.error(new BadRequestException("brdFormFields", SiteConstants.NOT_NULL));
              }

              return brdRepository
                  .findByBrdId(id)
                  .switchIfEmpty(Mono.error(new NotFoundException("BRD not found with ID: " + id)))
                  .flatMap(
                      brd -> {
                        Update update = new Update().set("updatedAt", LocalDateTime.now());

                        brdFormFields.forEach(
                            (fieldPath, value) -> update.set("brdForm." + fieldPath, value));

                        Query query = Query.query(Criteria.where(SiteConstants.BRD_ID).is(id));

                        return mongoTemplate
                            .updateMulti(query, update, Site.class)
                            .flatMap(
                                updateResult -> {
                                  if (updateResult.getMatchedCount() == 0) {
                                    return Mono.error(
                                        new NotFoundException("No sites found for BRD ID: " + id));
                                  }

                                  return Mono.zip(
                                          Mono.just(brd),
                                          divisionRepository.findByBrdId(id).collectList())
                                      .map(
                                          tuple -> {
                                            BRD updatedBrd = tuple.getT1();
                                            List<Site> updatedSites = tuple.getT2();

                                            SiteResponse response =
                                                createDivisionResponse(updatedBrd, updatedSites);

                                            return ResponseEntity.ok()
                                                .body(
                                                    new Api<>(
                                                        "SUCCESS",
                                                        String.format(
                                                            "BRD form fields updated successfully for %d sites",
                                                            updatedSites.size()),
                                                        Optional.of(response),
                                                        Optional.empty()));
                                          });
                                });
                      });
            })
        .onErrorResume(this::handleError);
  }

  @Override
  public Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> updateMultipleSites(
      List<SiteUpdateRequest> siteUpdates) {
    return validateAndProcessUpdates(siteUpdates)
        .flatMap(this::processUpdatesInParallel)
        .map(this::createSuccessResponse)
        .onErrorResume(this::handleError);
  }

  private Mono<List<SiteUpdateRequest>> validateAndProcessUpdates(
      List<SiteUpdateRequest> siteUpdates) {
    return Mono.justOrEmpty(siteUpdates)
        .filter(updates -> !updates.isEmpty())
        .switchIfEmpty(Mono.error(new BadRequestException("siteUpdates", "cannot be empty")));
  }

  private Mono<List<SingleSiteResponse>> processUpdatesInParallel(List<SiteUpdateRequest> updates) {
    List<Mono<SingleSiteResponse>> updateMonos =
        updates.stream().map(this::processSingleUpdate).toList();

    return Mono.zip(updateMonos, responses -> responses)
        .map(responses -> Arrays.stream(responses).map(SingleSiteResponse.class::cast).toList());
  }

  private Mono<SingleSiteResponse> processSingleUpdate(SiteUpdateRequest update) {
    return divisionRepository
        .findById(update.getSiteId())
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    String.format(SiteConstants.SITE_NOT_FOUND, update.getSiteId()))))
        .flatMap(existingSite -> updateSiteFields(existingSite, update.getSiteDetails()))
        .map(this::mapSiteToResponse);
  }

  private Mono<Site> updateSiteFields(Site existingSite, SiteRequest.SiteDetails siteDetails) {
    if (siteDetails.getSiteName() != null) {
      existingSite.setSiteName(siteDetails.getSiteName());
    }
    if (siteDetails.getIdentifierCode() != null) {
      existingSite.setIdentifierCode(siteDetails.getIdentifierCode());
    }
    if (siteDetails.getDescription() != null) {
      existingSite.setDescription(siteDetails.getDescription());
    }
    if (siteDetails.getBrdForm() != null) {
      updateBrdForm(existingSite, siteDetails.getBrdForm());
    }
    existingSite.setUpdatedAt(LocalDateTime.now());
    return divisionRepository.save(existingSite);
  }

  private void updateBrdForm(Site existingSite, BrdForm newBrdForm) {
    if (existingSite.getBrdForm() == null) {
      existingSite.setBrdForm(newBrdForm);
    } else {
      updateBrdFormFields(existingSite.getBrdForm(), newBrdForm);
    }
  }

  private ResponseEntity<Api<List<SingleSiteResponse>>> createSuccessResponse(
      List<SingleSiteResponse> responses) {
    return ResponseEntity.ok()
        .body(
            new Api<>(
                SiteConstants.SUCCESSFUL,
                "Sites updated successfully",
                Optional.of(responses),
                Optional.empty()));
  }

  @Override
  public Mono<ResponseEntity<Api<Void>>> deleteMultipleSites(List<String> siteIds) {
    return Mono.justOrEmpty(siteIds)
        .filter(ids -> !ids.isEmpty())
        .switchIfEmpty(Mono.error(new BadRequestException("Site IDs list cannot be empty")))
        .flatMap(
            ids ->
                Flux.fromIterable(ids)
                    .flatMap(
                        siteId ->
                            divisionRepository
                                .findById(siteId)
                                .switchIfEmpty(
                                    Mono.error(
                                        new NotFoundException("Site not found with ID: " + siteId)))
                                .flatMap(divisionRepository::delete))
                    .collectList()
                    .thenReturn(
                        ResponseEntity.ok()
                            .body(
                                new Api<Void>(
                                    SiteConstants.SUCCESSFUL,
                                    "Sites deleted successfully",
                                    Optional.empty(),
                                    Optional.empty()))))
        .onErrorResume(
            ex -> {
              String status = "FAILURE";
              Map<String, String> errorDetails = new HashMap<>();

              var errorInfo =
                  switch (ex) {
                    case NotFoundException notFoundEx ->
                        new ErrorInfo(HttpStatus.NOT_FOUND, notFoundEx.getMessage(), errorDetails);
                    case AlreadyExistException alreadyExistEx ->
                        new ErrorInfo(
                            HttpStatus.CONFLICT, alreadyExistEx.getMessage(), errorDetails);
                    case BadRequestException badRequestEx ->
                        new ErrorInfo(
                            HttpStatus.BAD_REQUEST, badRequestEx.getMessage(), errorDetails);
                    case InternalServerException internalServerEx -> {
                      errorDetails.putAll(internalServerEx.getErrorDetails());
                      yield new ErrorInfo(
                          HttpStatus.INTERNAL_SERVER_ERROR,
                          internalServerEx.getMessage(),
                          errorDetails);
                    }
                    default ->
                        new ErrorInfo(
                            HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), errorDetails);
                  };

              errorDetails.computeIfAbsent(
                  SiteConstants.TIMESTAMP, k -> LocalDateTime.now().toString());
              errorDetails.computeIfAbsent(SiteConstants.ERROR_MESSAGE, k -> errorInfo.message());

              Api<Void> apiResponse =
                  new Api<>(
                      status, errorInfo.message(), Optional.empty(), Optional.of(errorDetails));
              return Mono.just(ResponseEntity.status(errorInfo.httpStatus()).body(apiResponse));
            });
  }

  @Override
  public Mono<String> getSiteBrdId(String siteId) {
    return divisionRepository
        .findById(siteId)
        .switchIfEmpty(Mono.error(new NotFoundException("Site not found with id: " + siteId)))
        .map(
            site -> {
              if (site.getBrdId() == null || site.getBrdId().trim().isEmpty()) {
                throw new NotFoundException("No BRD ID associated with site: " + siteId);
              }
              return site.getBrdId();
            });
  }

  @Override
  public Mono<Double> calculateBrdScore(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .doOnNext(brd -> log.debug("Found BRD: {}", brd != null ? brd.getBrdId() : "null"))
        .map(
            brd -> {
              if (brd == null) {
                return 0.0;
              }

              long totalFields = 0;
              long filledFields = 0;

              try {
                // Process each section directly
                Map<String, Object> sections = new HashMap<>();
                sections.put("clientInformation", brd.getClientInformation());
                sections.put("aciInformation", brd.getAciInformation());
                sections.put("paymentChannels", brd.getPaymentChannels());
                sections.put("fundingMethods", brd.getFundingMethods());
                sections.put("achPaymentProcessing", brd.getAchPaymentProcessing());
                sections.put("miniAccountMaster", brd.getMiniAccountMaster());
                sections.put("accountIdentifierInformation", brd.getAccountIdentifierInformation());
                sections.put("paymentRules", brd.getPaymentRules());
                sections.put("notifications", brd.getNotifications());
                sections.put("remittance", brd.getRemittance());
                sections.put("agentPortal", brd.getAgentPortal());
                sections.put("recurringPayments", brd.getRecurringPayments());
                sections.put("ivr", brd.getIvr());

                // Process each section
                for (Map.Entry<String, Object> entry : sections.entrySet()) {
                  String sectionName = entry.getKey();
                  Object sectionData = entry.getValue();

                  if (sectionData != null) {
                    ScoreResult result = processSection(sectionName, sectionData);
                    totalFields += result.totalFields;
                    filledFields += result.filledFields;
                  }
                }

                return totalFields > 0 ? ((double) filledFields / totalFields) * 100 : 0.0;
              } catch (Exception e) {
                log.error("Error calculating score for BRD {}: {}", brdId, e.getMessage());
                return 0.0;
              }
            });
  }

  private ScoreResult processSection(String sectionName, Object sectionData) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<?, ?> sectionMap = mapper.convertValue(sectionData, Map.class);
      return countFields(sectionMap);
    } catch (Exception e) {
      log.error("Error processing section {}: {}", sectionName, e.getMessage());
      return new ScoreResult(); // Return empty result in case of error
    }
  }

  private ScoreResult countFields(Object obj) {
    ScoreResult result = new ScoreResult();

    if (obj == null) {
      return result;
    }

    if (obj instanceof Map) {
      return countFieldsInMap((Map<?, ?>) obj);
    } else if (obj instanceof List) {
      return countFieldsInList((List<?>) obj);
    } else {
      return countSimpleField(obj);
    }
  }

  private ScoreResult countFieldsInMap(Map<?, ?> map) {
    ScoreResult result = new ScoreResult();

    if (map.isEmpty()) {
      return result;
    }

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = entry.getKey().toString();
      if (shouldSkipField(key)) {
        continue;
      }

      Object value = entry.getValue();
      if (value instanceof Map) {
        processNestedMap((Map<?, ?>) value, result);
      } else if (value instanceof List) {
        processListValue((List<?>) value, result);
      } else {
        processSimpleValue(value, result);
      }
    }

    return result;
  }

  private void processNestedMap(Map<?, ?> nestedMap, ScoreResult result) {
    if (isCheckboxGroup(nestedMap)) {
      processCheckboxGroup(nestedMap, result);
    } else {
      ScoreResult nestedResult = countFields(nestedMap);
      result.totalFields += nestedResult.totalFields;
      result.filledFields += nestedResult.filledFields;
    }
  }

  private void processCheckboxGroup(Map<?, ?> checkboxGroup, ScoreResult result) {
    result.totalFields++;
    result.filledFields +=
        checkboxGroup.values().stream()
                .filter(Map.class::isInstance)
                .map(v -> (Map<?, ?>) v)
                .anyMatch(m -> Boolean.TRUE.equals(m.get(SELECTED_FIELD)))
            ? 1
            : 0;
  }

  private void processListValue(List<?> list, ScoreResult result) {
    if (list.isEmpty()) {
      return;
    }

    if (list.get(0) instanceof Map) {
      processMapList(list, result);
    } else {
      processSimpleList(list, result);
    }
  }

  private void processMapList(List<?> list, ScoreResult result) {
    Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
    if (isCheckboxGroup(firstItem)) {
      processCheckboxGroupList(list, result);
    } else {
      processRegularList(list, result);
    }
  }

  private void processCheckboxGroupList(List<?> list, ScoreResult result) {
    result.totalFields++;
    result.filledFields +=
        list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item)
                .anyMatch(item -> Boolean.TRUE.equals(item.get(SELECTED_FIELD)))
            ? 1
            : 0;
  }

  private void processRegularList(List<?> list, ScoreResult result) {
    for (int i = 0; i < list.size(); i++) {
      Object item = list.get(i);
      ScoreResult nestedResult = countFields(item);
      result.totalFields += nestedResult.totalFields;
      result.filledFields += nestedResult.filledFields;
    }
  }

  private void processSimpleList(List<?> list, ScoreResult result) {
    result.totalFields++;
    if (isFieldValid(list)) {
      result.filledFields++;
    }
  }

  private void processSimpleValue(Object value, ScoreResult result) {
    result.totalFields++;
    if (isFieldValid(value)) {
      result.filledFields++;
    }
  }

  private ScoreResult countFieldsInList(List<?> list) {
    ScoreResult result = new ScoreResult();
    if (!list.isEmpty()) {
      result.totalFields = 1;
      result.filledFields = isFieldValid(list) ? 1 : 0;
    }
    return result;
  }

  private ScoreResult countSimpleField(Object obj) {
    ScoreResult result = new ScoreResult();
    result.totalFields = 1;
    result.filledFields = isFieldValid(obj) ? 1 : 0;
    return result;
  }

  private boolean isCheckboxGroup(Map<?, ?> map) {
    // Check if this is a group of checkboxes (all nested objects must have only boolean values)
    return map.values().stream()
        .filter(Map.class::isInstance)
        .map(value -> (Map<?, ?>) value)
        .allMatch(
            nestedMap ->
                nestedMap.entrySet().stream()
                    .allMatch(
                        entry -> {
                          String key = entry.getKey().toString();
                          Object value = entry.getValue();
                          return (key.equals(SELECTED_FIELD) && value instanceof Boolean)
                              || (key.equals(VALUE_FIELD) && value instanceof Boolean);
                        }));
  }

  @Override
  public Mono<Double> calculateSiteScore(String brdId) {
    if (brdId == null) {
      return Mono.just(0.0);
    }

    return divisionRepository
        .findByBrdId(brdId)
        .collectList()
        .doOnNext(sites -> log.debug("Found {} sites for BRD ID: {}", sites.size(), brdId))
        .map(
            sites -> {
              if (sites == null || sites.isEmpty()) {
                log.debug("No sites found for BRD ID: {}", brdId);
                return 0.0;
              }

              // Calculate score for each site individually
              double totalScore = 0.0;
              int validSites = 0;

              for (Site site : sites) {
                if (site.getBrdForm() != null) {
                  ObjectMapper mapper = new ObjectMapper();
                  Map<String, Object> brdFormMap =
                      mapper.convertValue(site.getBrdForm(), Map.class);

                  ScoreResult result = countFields(brdFormMap);

                  if (result.totalFields > 0) {
                    double siteScore = ((double) result.filledFields / result.totalFields) * 100;
                    totalScore += siteScore;
                    validSites++;
                  }
                }
              }

              // Return the average score of all sites
              return validSites > 0 ? totalScore / validSites : 0.0;
            })
        .defaultIfEmpty(0.0)
        .onErrorResume(
            e -> {
              log.error("Error calculating site score for brdId: {}", brdId, e);
              return Mono.just(0.0);
            });
  }

  private boolean shouldSkipField(String fieldName) {
    return "_id".equals(fieldName)
        || "createdAt".equals(fieldName)
        || "updatedAt".equals(fieldName)
        || "class".equals(fieldName)
        || "$oid".equals(fieldName)
        || "$date".equals(fieldName)
        || fieldName.equals(SELECTED_FIELD)
        || fieldName.equals(VALUE_FIELD);
  }

  private boolean isFieldValid(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof String strValue) {
      return isValidString(strValue);
    }

    if (value instanceof Map) {
      return isValidMap((Map<?, ?>) value);
    }

    if (value instanceof List) {
      return isValidList((List<?>) value);
    }

    if (value instanceof Boolean) {
      return Boolean.TRUE.equals(value);
    }

    return true;
  }

  private boolean isValidString(String strValue) {
    return !strValue.isEmpty() && !strValue.equals("null");
  }

  private boolean isValidMap(Map<?, ?> map) {
    if (map.containsKey(SELECTED_FIELD)) {
      return Boolean.TRUE.equals(map.get(SELECTED_FIELD));
    }
    if (map.containsKey(VALUE_FIELD)) {
      return isValidValueField(map.get(VALUE_FIELD));
    }
    return !map.isEmpty();
  }

  private boolean isValidValueField(Object val) {
    if (val instanceof Boolean) {
      return Boolean.TRUE.equals(val);
    }
    return val != null && !val.toString().isEmpty() && !val.toString().equals("null");
  }

  private boolean isValidList(List<?> list) {
    if (list.isEmpty()) {
      return false;
    }
    if (list.get(0) instanceof Map) {
      return hasSelectedFieldInList(list);
    }
    return true;
  }

  private boolean hasSelectedFieldInList(List<?> list) {
    Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
    if (firstItem.containsKey(SELECTED_FIELD)) {
      return list.stream()
          .filter(Map.class::isInstance)
          .map(item -> (Map<?, ?>) item)
          .anyMatch(item -> Boolean.TRUE.equals(item.get(SELECTED_FIELD)));
    }
    return true;
  }

  @Override
  public Mono<ResponseEntity<Api<BulkSiteResponse>>> bulkCreateSites(
      String brdId, int numberOfSites) {
    if (numberOfSites > SiteConstants.MAX_BULK_SITES) {
      Map<String, String> errors = new HashMap<>();
      errors.put(SiteConstants.ERROR_MESSAGE, SiteConstants.MAX_BULK_SITES_EXCEEDED);
      errors.put(SiteConstants.TIMESTAMP, LocalDateTime.now().toString());

      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<>(
                      SiteConstants.FAILURE,
                      SiteConstants.MAX_BULK_SITES_EXCEEDED,
                      Optional.empty(),
                      Optional.of(errors))));
    }

    Mono<ResponseEntity<Api<BulkSiteResponse>>> monoOperation =
        Mono.justOrEmpty(brdId)
            .filter(id -> id != null && !id.isEmpty())
            .switchIfEmpty(Mono.error(new BadRequestException("brdId", "cannot be null or empty")))
            .flatMap(
                id ->
                    brdRepository
                        .findByBrdId(id)
                        .switchIfEmpty(
                            Mono.error(new NotFoundException(SiteConstants.BRD_NOT_FOUND + id))))
            .flatMap(
                brd ->
                    divisionRepository
                        .findByBrdId(brdId)
                        .collectList()
                        .flatMap(
                            existingSites -> {
                              int startSequence = existingSites.size() + 1;
                              List<Site> newSites =
                                  createBulkSites(brd, numberOfSites, startSequence);
                              return divisionRepository.saveAll(newSites).collectList();
                            })
                        .map(
                            savedSites -> {
                              List<BulkSiteResponse.SiteBasicDetails> siteDetails =
                                  savedSites.stream()
                                      .map(
                                          site ->
                                              BulkSiteResponse.SiteBasicDetails.builder()
                                                  .siteId(site.getSiteId())
                                                  .siteName(site.getSiteName())
                                                  .identifierCode(site.getIdentifierCode())
                                                  .brdForm(site.getBrdForm())
                                                  .build())
                                      .toList();

                              BulkSiteResponse response =
                                  BulkSiteResponse.builder()
                                      .siteCount(savedSites.size())
                                      .brdId(brdId)
                                      .sites(siteDetails)
                                      .build();

                              return ResponseEntity.status(HttpStatus.CREATED)
                                  .body(
                                      new Api<>(
                                          SiteConstants.SUCCESSFUL,
                                          String.format(
                                              SiteConstants.BULK_SITES_CREATED, savedSites.size()),
                                          Optional.of(response),
                                          Optional.empty()));
                            }));

    return Optional.ofNullable(transactionalOperator.transactional(monoOperation))
        .orElse(monoOperation)
        .onErrorResume(this::handleBulkCreateError);
  }

  /**
   * Creates a list of Site objects for bulk creation
   *
   * @param brd Source BRD to clone from
   * @param count Number of sites to create
   * @param startSequence Starting sequence number
   * @return List of Site objects ready to be saved
   */
  private List<Site> createBulkSites(BRD brd, int count, int startSequence) {
    List<Site> sites = new ArrayList<>(count);

    for (int i = 0; i < count; i++) {
      int siteNumber = startSequence + i;
      Site site =
          Site.builder()
              .brdId(brd.getBrdId())
              .siteId(generateSiteId(brd.getBrdId(), siteNumber))
              .siteName(String.format(SiteConstants.BULK_CLONE_NAME_FORMAT, siteNumber))
              .identifierCode(String.format(SiteConstants.BULK_CLONE_IDENTIFIER_FORMAT, siteNumber))
              .description("Bulk created site")
              .brdForm(createBrdFormFromBrd(brd))
              .build();

      sites.add(site);
    }

    return sites;
  }

  /**
   * Creates a BrdForm object from a BRD entity
   *
   * @param brd Source BRD to copy form data from
   * @return BrdForm object with copied data
   */
  private BrdForm createBrdFormFromBrd(BRD brd) {
    return new BrdForm(
        brd.getClientInformation(),
        brd.getAciInformation(),
        brd.getPaymentChannels(),
        brd.getFundingMethods(),
        brd.getAchPaymentProcessing(),
        brd.getMiniAccountMaster(),
        brd.getAccountIdentifierInformation(),
        brd.getPaymentRules(),
        brd.getNotifications(),
        brd.getRemittance(),
        brd.getAgentPortal(),
        brd.getRecurringPayments(),
        brd.getIvr(),
        brd.getGeneralImplementations(),
        brd.getApprovals(),
        brd.getRevisionHistory());
  }

  /** Handles errors during bulk site creation */
  private Mono<ResponseEntity<Api<BulkSiteResponse>>> handleBulkCreateError(Throwable ex) {
    log.error("Error during bulk site creation: {}", ex.getMessage());

    if (ex instanceof NotFoundException) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(
                  new Api<>(
                      SiteConstants.FAILURE,
                      ex.getMessage(),
                      Optional.empty(),
                      Optional.of(createErrorMap(ex)))));
    } else if (ex instanceof BadRequestException) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      SiteConstants.FAILURE,
                      ex.getMessage(),
                      Optional.empty(),
                      Optional.of(createErrorMap(ex)))));
    } else {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(
                  new Api<>(
                      SiteConstants.FAILURE,
                      ex.getMessage(),
                      Optional.empty(),
                      Optional.of(createErrorMap(ex)))));
    }
  }

  private Map<String, String> createErrorMap(Throwable ex) {
    Map<String, String> errors = new HashMap<>();
    errors.put(SiteConstants.ERROR_MESSAGE, ex.getMessage());
    errors.put(SiteConstants.TIMESTAMP, LocalDateTime.now().toString());
    return errors;
  }

  @Override
  public Mono<ResponseEntity<Api<Void>>> deleteSite(String siteId) {
    return Mono.justOrEmpty(siteId)
        .filter(id -> !id.isEmpty())
        .switchIfEmpty(Mono.error(new BadRequestException("Site ID cannot be empty")))
        .flatMap(
            id ->
                divisionRepository
                    .findById(id)
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(String.format(SiteConstants.SITE_NOT_FOUND, id))))
                    .flatMap(divisionRepository::delete)
                    .then(
                        Mono.just(
                            ResponseEntity.ok()
                                .body(
                                    new Api<Void>(
                                        SiteConstants.SUCCESSFUL,
                                        "Site deleted successfully",
                                        Optional.empty(),
                                        Optional.empty()))))
                    .onErrorResume(
                        error -> {
                          HttpStatus httpStatus =
                              error instanceof BadRequestException
                                  ? HttpStatus.BAD_REQUEST
                                  : HttpStatus.NOT_FOUND;
                          Api<Void> apiResponse =
                              new Api<>(
                                  SiteConstants.FAILURE,
                                  error.getMessage(),
                                  Optional.empty(),
                                  Optional.empty());
                          return Mono.just(ResponseEntity.status(httpStatus).body(apiResponse));
                        }));
  }

  /**
   * Extracts the sequence number from a site ID
   *
   * @param siteId Format: SITE_BRD0001_002
   * @return The sequence number (e.g., 2)
   */
  private int extractSequenceNumber(String siteId) {
    try {
      String[] parts = siteId.split("_");
      if (parts.length >= 3) {
        return Integer.parseInt(parts[parts.length - 1]);
      }
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
      // If the format is invalid, return 0
      return 0;
    }
    return 0;
  }

  /**
   * Finds the highest sequence number among existing sites for a given BRD
   *
   * @param brdId The BRD ID to search for
   * @return A Mono containing the highest sequence number
   */
  private Mono<Integer> findHighestSequenceNumber(String brdId) {
    return divisionRepository
        .findByBrdId(brdId)
        .map(site -> extractSequenceNumber(site.getSiteId()))
        .reduce(0, Integer::max)
        .defaultIfEmpty(0);
  }

  @Override
  public Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> cloneSite(String siteId) {
    return divisionRepository
        .findById(siteId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(String.format(SiteConstants.SITE_NOT_FOUND, siteId))))
        .flatMap(
            site -> {
              // Create a clone of the site
              Site clonedSite = new Site();
              BeanUtils.copyProperties(site, clonedSite);
              clonedSite.setId(null); // MongoDB will generate a new ID

              // Find highest sequence number and generate new site ID
              return findHighestSequenceNumber(site.getBrdId())
                  .map(
                      highestSequence -> {
                        // For invalid formats, start with sequence 1
                        if (highestSequence == 0) {
                          clonedSite.setSiteId(generateSiteId(site.getBrdId(), 1));
                        } else {
                          clonedSite.setSiteId(
                              generateSiteId(site.getBrdId(), highestSequence + 1));
                        }

                        // Append "(Copy)" to the site name
                        String originalName = site.getSiteName();
                        clonedSite.setSiteName(originalName + " (Copy)");

                        return clonedSite;
                      })
                  .flatMap(
                      siteToSave ->
                          divisionRepository
                              .save(siteToSave)
                              .onErrorResume(
                                  e -> Mono.error(new RuntimeException("Database error"))))
                  .map(
                      saved -> {
                        SiteResponse.DivisionDetails response =
                            SiteResponse.DivisionDetails.builder()
                                .id(saved.getId())
                                .siteId(saved.getSiteId())
                                .siteName(saved.getSiteName())
                                .identifierCode(saved.getIdentifierCode())
                                .description(saved.getDescription())
                                .brdForm(saved.getBrdForm())
                                .score(0.0) // Default score for new site
                                .build();

                        return ResponseEntity.ok(
                            new Api<>(
                                SiteConstants.SUCCESSFUL,
                                "Site cloned successfully",
                                Optional.of(response),
                                Optional.empty()));
                      });
            });
  }

  @Override
  public Mono<ResponseEntity<Api<SiteDifferencesResponse>>> getSiteDifferences(String brdId) {
    return validateBrdId(brdId)
        .flatMap(this::findBrdAndSites)
        .map(this::processDifferences)
        .switchIfEmpty(createBrdNotFoundResponse(brdId))
        .onErrorResume(this::handleError);
  }

  private Mono<String> validateBrdId(String brdId) {
    if (brdId == null || brdId.isEmpty()) {
      return Mono.error(new BadRequestException(SiteConstants.BRD_ID_EMPTY));
    }
    return Mono.just(brdId);
  }

  private Mono<Tuple2<BRD, List<Site>>> findBrdAndSites(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .flatMap(
            brd ->
                divisionRepository
                    .findByBrdId(brdId)
                    .collectList()
                    .map(sites -> Tuples.of(brd, sites)));
  }

  private ResponseEntity<Api<SiteDifferencesResponse>> processDifferences(
      Tuple2<BRD, List<Site>> tuple) {
    BRD brd = tuple.getT1();
    List<Site> sites = tuple.getT2();

    SiteDifferencesResponse response = new SiteDifferencesResponse();
    response.setBrdId(brd.getBrdId());

    List<SiteDifferencesResponse.SiteDifference> siteDifferences =
        findDifferencesForAllSites(brd, sites);

    response.setSites(siteDifferences);

    return createSuccessResponse(response, siteDifferences.size());
  }

  private List<SiteDifferencesResponse.SiteDifference> findDifferencesForAllSites(
      BRD brd, List<Site> sites) {
    return sites.stream()
        .map(site -> processSingleSite(brd, site))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Optional<SiteDifferencesResponse.SiteDifference> processSingleSite(BRD brd, Site site) {
    try {
      List<SiteDifferencesResponse.FieldDifference> differences = findFieldDifferences(brd, site);
      if (!differences.isEmpty()) {
        return Optional.of(createSiteDifference(site, differences));
      }
    } catch (Exception e) {
      log.error("Error comparing BRD and site {}: {}", site.getSiteId(), e.getMessage());
    }
    return Optional.empty();
  }

  private SiteDifferencesResponse.SiteDifference createSiteDifference(
      Site site, List<SiteDifferencesResponse.FieldDifference> differences) {

    SiteDifferencesResponse.SiteDifference siteDiff = new SiteDifferencesResponse.SiteDifference();
    siteDiff.setSiteId(site.getSiteId());
    siteDiff.setSiteName(site.getSiteName());
    siteDiff.setDifferences(differences);
    return siteDiff;
  }

  private ResponseEntity<Api<SiteDifferencesResponse>> createSuccessResponse(
      SiteDifferencesResponse response, int differenceCount) {

    String message =
        differenceCount == 0
            ? "No differences found between BRD and sites"
            : String.format("Found differences in %d sites", differenceCount);

    return ResponseEntity.ok()
        .body(
            new Api<>(SiteConstants.SUCCESSFUL, message, Optional.of(response), Optional.empty()));
  }

  private Mono<ResponseEntity<Api<SiteDifferencesResponse>>> createBrdNotFoundResponse(
      String brdId) {
    Map<String, String> errors = new HashMap<>();
    errors.put("errorMessage", String.format(SiteConstants.BRD_NOT_FOUND, brdId));
    errors.put("timestamp", LocalDateTime.now().toString());

    return Mono.just(
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                new Api<>(
                    SiteConstants.FAILURE,
                    String.format(SiteConstants.BRD_NOT_FOUND, brdId),
                    Optional.empty(),
                    Optional.of(errors))));
  }

  private List<SiteDifferencesResponse.FieldDifference> findFieldDifferences(BRD brd, Site site) {
    List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();
    BrdForm siteBrdForm = site.getBrdForm();

    if (siteBrdForm == null) {
      log.warn(String.format(SiteConstants.SITE_BRD_FORM_NULL, site.getSiteId()));
      return differences;
    }

    try {
      // Create a BrdForm from BRD to ensure we're comparing the same structure
      BrdForm brdForm = createBrdFormFromBrd(brd);

      // Convert both BrdForms to maps for comparison
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> brdFormMap = mapper.convertValue(brdForm, Map.class);
      Map<String, Object> siteBrdFormMap = mapper.convertValue(siteBrdForm, Map.class);

      logComparisonDetails(brd, site, brdFormMap, siteBrdFormMap);

      // Get all unique section names
      Set<String> allSections = new HashSet<>();
      allSections.addAll(brdFormMap.keySet());
      allSections.addAll(siteBrdFormMap.keySet());

      // Compare each section
      allSections.stream()
          .filter(sectionName -> shouldCompareSection(sectionName, brdFormMap, siteBrdFormMap))
          .forEach(
              sectionName -> {
                Object brdSection = brdFormMap.get(sectionName);
                Object siteSection = siteBrdFormMap.get(sectionName);

                log.debug(
                    "Comparing section: {} (BRD value present: {}, Site value present: {})",
                    sectionName,
                    brdSection != null,
                    siteSection != null);

                if (isNullValueDifference(brdSection, siteSection)) {
                  log.info(
                      "Found null section difference in {}: BRD: {}, Site: {}",
                      sectionName,
                      brdSection != null,
                      siteSection != null);
                  differences.add(createFieldDifference(sectionName, brdSection, siteSection));
                  return;
                }

                compareSection(sectionName, brdSection, siteSection, differences);
              });

      log.debug("Found {} differences for site {}", differences.size(), site.getSiteId());
      return differences;
    } catch (Exception e) {
      log.error("Error comparing BRD and Site BRD form: {}", e.getMessage(), e);
      return differences;
    }
  }

  private void logComparisonDetails(
      BRD brd, Site site, Map<String, Object> brdFormMap, Map<String, Object> siteBrdFormMap) {
    log.debug("Comparing BRD {} with Site {}", brd.getBrdId(), site.getSiteId());
    log.debug("BRD form sections: {}", brdFormMap.keySet());
    log.debug("Site BRD form sections: {}", siteBrdFormMap.keySet());
  }

  private boolean shouldCompareSection(
      String sectionName, Map<String, Object> brdFormMap, Map<String, Object> siteBrdFormMap) {
    // Skip revisionHistory section
    if ("revisionHistory".equals(sectionName)) {
      return false;
    }

    Object brdSection = brdFormMap.get(sectionName);
    Object siteSection = siteBrdFormMap.get(sectionName);
    return !(brdSection == null && siteSection == null);
  }

  private boolean isNullValueDifference(Object brdSection, Object siteSection) {
    return brdSection == null || siteSection == null;
  }

  private SiteDifferencesResponse.FieldDifference createFieldDifference(
      String fieldName, Object orgBrdValue, Object siteBrdValue) {
    return SiteDifferencesResponse.FieldDifference.builder()
        .fieldName(fieldName)
        .orgBrdValue(orgBrdValue)
        .siteBrdValue(siteBrdValue)
        .build();
  }

  private void compareSection(
      String sectionName,
      Object brdSection,
      Object siteSection,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    try {
      ObjectMapper mapper = new ObjectMapper();

      // Log the actual values being compared
      log.info("Comparing section {}", sectionName);
      log.info("BRD section value: {}", brdSection);
      log.info("Site section value: {}", siteSection);

      Map<String, Object> brdMap = mapper.convertValue(brdSection, Map.class);
      Map<String, Object> siteMap = mapper.convertValue(siteSection, Map.class);

      compareMapFields(sectionName, brdMap, siteMap, differences);
    } catch (IllegalArgumentException e) {
      handleDirectValueComparison(sectionName, brdSection, siteSection, differences);
    } catch (Exception e) {
      log.error("Error comparing section {}: {}", sectionName, e.getMessage());
      differences.add(createFieldDifference(sectionName, brdSection, siteSection));
    }
  }

  private void handleDirectValueComparison(
      String sectionName,
      Object brdSection,
      Object siteSection,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    if (!Objects.equals(brdSection, siteSection)) {
      log.info(
          "Found direct value difference in section {}: BRD value: {}, Site value: {}",
          sectionName,
          brdSection,
          siteSection);
      differences.add(createFieldDifference(sectionName, brdSection, siteSection));
    }
  }

  private void compareMapFields(
      String parentPath,
      Map<String, Object> brdMap,
      Map<String, Object> siteMap,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    Set<String> allFields = new HashSet<>();
    allFields.addAll(brdMap.keySet());
    allFields.addAll(siteMap.keySet());

    // Log all fields being compared
    log.info("Comparing fields in {}: {}", parentPath, allFields);

    allFields.stream()
        .filter(field -> !shouldSkipField(field))
        .forEach(field -> processField(parentPath, field, brdMap, siteMap, differences));
  }

  private void processField(
      String parentPath,
      String field,
      Map<String, Object> brdMap,
      Map<String, Object> siteMap,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    Object brdValue = brdMap.get(field);
    Object siteValue = siteMap.get(field);
    String fieldPath = parentPath + "." + field;

    // Log the values being compared
    log.info("Comparing field {}: BRD value: {}, Site value: {}", fieldPath, brdValue, siteValue);

    if (shouldProcessField(brdValue, siteValue)) {
      processFieldValues(fieldPath, brdValue, siteValue, differences);
    }
  }

  private boolean shouldProcessField(Object brdValue, Object siteValue) {
    return !(brdValue == null && siteValue == null);
  }

  private void processFieldValues(
      String fieldPath,
      Object brdValue,
      Object siteValue,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    if (isNullValueDifference(brdValue, siteValue)) {
      log.info(
          "Found null value difference in field {}: BRD value: {}, Site value: {}",
          fieldPath,
          brdValue,
          siteValue);
      differences.add(createFieldDifference(fieldPath, brdValue, siteValue));
      return;
    }

    if (isViewValueAndSelectedList(brdValue) && isViewValueAndSelectedList(siteValue)) {
      compareViewValueAndSelectedLists(fieldPath, brdValue, siteValue, differences);
    } else if (brdValue instanceof Map && siteValue instanceof Map) {
      compareMapFields(
          fieldPath, (Map<String, Object>) brdValue, (Map<String, Object>) siteValue, differences);
    } else if (brdValue instanceof List && siteValue instanceof List) {
      compareListFields(fieldPath, (List<?>) brdValue, (List<?>) siteValue, differences);
    } else if (!Objects.equals(brdValue, siteValue)) {
      log.info(
          "Found value difference in field {}: BRD value: {}, Site value: {}",
          fieldPath,
          brdValue,
          siteValue);
      differences.add(createFieldDifference(fieldPath, brdValue, siteValue));
    }
  }

  private boolean isViewValueAndSelectedList(Object value) {
    if (!(value instanceof List<?>)) {
      return false;
    }
    List<?> list = (List<?>) value;
    if (list.isEmpty()) {
      return false;
    }
    Object firstItem = list.get(0);
    return firstItem instanceof Map
        && ((Map<?, ?>) firstItem).containsKey(SiteConstants.VIEW_VALUE)
        && ((Map<?, ?>) firstItem).containsKey(SiteConstants.SELECTED);
  }

  private void compareViewValueAndSelectedLists(
      String fieldPath,
      Object brdValue,
      Object siteValue,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    List<String> brdSelectedValues = extractSelectedViewValues((List<?>) brdValue);
    List<String> siteSelectedValues = extractSelectedViewValues((List<?>) siteValue);

    if (!Objects.equals(brdSelectedValues, siteSelectedValues)) {
      log.info(
          "Found difference in selected values for field {}: BRD values: {}, Site values: {}",
          fieldPath,
          brdSelectedValues,
          siteSelectedValues);

      Map<String, List<String>> brdResponse = new HashMap<>();
      brdResponse.put("viewValues", brdSelectedValues);

      Map<String, List<String>> siteResponse = new HashMap<>();
      siteResponse.put("viewValues", siteSelectedValues);

      differences.add(createFieldDifference(fieldPath, brdResponse, siteResponse));
    }
  }

  private List<String> extractSelectedViewValues(List<?> items) {
    return items.stream()
        .filter(Map.class::isInstance)
        .map(item -> (Map<?, ?>) item)
        .filter(map -> Boolean.TRUE.equals(map.get(SiteConstants.SELECTED)))
        .map(map -> Objects.toString(map.get(SiteConstants.VIEW_VALUE), ""))
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private void compareListFields(
      String fieldPath,
      List<?> brdList,
      List<?> siteList,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    // Log the lists being compared
    log.info("Comparing lists in {}", fieldPath);
    log.info("BRD list: {}", brdList);
    log.info("Site list: {}", siteList);

    if (brdList.size() != siteList.size()) {
      handleListSizeDifference(fieldPath, brdList, siteList, differences);
      return;
    }

    compareRegularList(fieldPath, brdList, siteList, differences);
  }

  public void handleListSizeDifference(
      String fieldPath,
      List<?> brdList,
      List<?> siteList,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    log.info(
        "Found list size difference in {}: BRD size: {}, Site size: {}",
        fieldPath,
        brdList.size(),
        siteList.size());
    differences.add(createFieldDifference(fieldPath, brdList, siteList));
  }

  private void compareRegularList(
      String fieldPath,
      List<?> brdList,
      List<?> siteList,
      List<SiteDifferencesResponse.FieldDifference> differences) {

    for (int i = 0; i < brdList.size(); i++) {
      Object brdItem = brdList.get(i);
      Object siteItem = siteList.get(i);

      // Log the items being compared
      log.info("Comparing list item {} in {}: BRD: {}, Site: {}", i, fieldPath, brdItem, siteItem);

      if (shouldProcessField(brdItem, siteItem)) {
        processFieldValues(fieldPath + "[" + i + "]", brdItem, siteItem, differences);
      }
    }
  }
}
