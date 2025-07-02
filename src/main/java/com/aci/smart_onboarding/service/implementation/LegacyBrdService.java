package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.BrdForm;
import com.aci.smart_onboarding.dto.BrdRules;
import com.aci.smart_onboarding.dto.GuidanceData;
import com.aci.smart_onboarding.dto.LegacyBRDInfo;
import com.aci.smart_onboarding.dto.LegacyBrdRequest;
import com.aci.smart_onboarding.dto.LegacyPrefillRequest;
import com.aci.smart_onboarding.dto.PrefillSections;
import com.aci.smart_onboarding.dto.RulesWithData;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.LegacyBRD;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.LegacyBRDRepository;
import com.aci.smart_onboarding.repository.SiteRepository;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.aci.smart_onboarding.service.ILegacyBrdService;
import com.aci.smart_onboarding.util.FileReader;
import com.aci.smart_onboarding.util.FileReader.FileType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class LegacyBrdService implements ILegacyBrdService {
  /**
   * Template raor BRD context with a placeholder for BRD ID. The {brdId} placeholder can be
   * replaced with the actual BRD ID when used.
   */
  private static final String BRD_CONTEXT_TEMPLATE =
      "You will be processing a JSON document that contains BRD information. "
          + "Focus ONLY on objects with brdId: {brdId}. from context"
          + "Ignore any other objects with different brdId's. from context"
          + "If you don't find any objects with the given brdId: {brdId}, then return the question exactly as it was given, maintaining the same format.";

  private final FileReader fileReader;
  private final IBlobStorageService blobStorageService;
  private final LegacyBRDRepository legacyBRDRepository;
  private final IBRDService brdService;
  private final DtoModelMapper dtoModelMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AssistantService assistantService;
  private final SiteRepository siteRepository;

  @Value("${blob.files.rules-file}")
  private String franklinRulesFileName;

  public LegacyBrdService(
      FileReader fileReader,
      BlobStorageService blobStorageService,
      LegacyBRDRepository legacyBRDRepository,
      IBRDService brdService,
      DtoModelMapper dtoModelMapper,
      AssistantService assistantService,
      SiteRepository siteRepository) {
    this.fileReader = fileReader;
    this.blobStorageService = blobStorageService;
    this.legacyBRDRepository = legacyBRDRepository;
    this.brdService = brdService;
    this.dtoModelMapper = dtoModelMapper;
    this.assistantService = assistantService;
    this.siteRepository = siteRepository;
  }

  @Override
  public Mono<List<GuidanceData>> getStandardData(String filePath) {
    return blobStorageService
        .fetchFileFromUrl(filePath)
        .map(
            content ->
                fileReader.parseByteArrayContent(
                    content, FileType.STANDARD_DATA, GuidanceData.class));
  }

  @Override
  public Mono<List<BrdRules>> getUserRules(String filePath) {
    return blobStorageService
        .fetchFileFromUrl(filePath)
        .map(
            content ->
                fileReader.parseByteArrayContent(content, FileType.USER_RULES, BrdRules.class));
  }

  private Mono<Boolean> processRootEntityAndSites(List<BrdRules> brdRulesList, String brdId) {
    if (brdRulesList == null || brdRulesList.isEmpty()) {
      return Mono.just(false);
    }

    // Get the first BRD as main entity
    BrdRules mainBrd = brdRulesList.getFirst();
    String mainBrdId = mainBrd.getBrdId();

    // Create LegacyBRD object for main entity
    LegacyBRD legacyBRD = new LegacyBRD();
    legacyBRD.setBrdId(brdId);

    // Set main BRD info
    LegacyBRDInfo mainInfo = new LegacyBRDInfo();
    mainInfo.setId(mainBrdId);
    mainInfo.setName(mainBrd.getBrdName());
    legacyBRD.setMain(mainInfo);

    // Find and set sites
    List<LegacyBRDInfo> sites = new ArrayList<>();
    for (int i = 1; i < brdRulesList.size(); i++) {
      BrdRules rule = brdRulesList.get(i);
      if ("1013".equals(rule.getRuleId()) && !rule.getBrdId().equals(rule.getValue())) {
        // This is a site
        LegacyBRDInfo siteInfo = new LegacyBRDInfo();
        siteInfo.setId(rule.getBrdId());
        siteInfo.setName(rule.getBrdName());
        sites.add(siteInfo);
      }
    }
    legacyBRD.setSites(sites);

    // Save to MongoDB reactively
    return legacyBRDRepository
        .save(legacyBRD)
        .thenReturn(true)
        .onErrorResume(e -> Mono.just(false));
  }

  private Mono<byte[]> fetchGuidanceData() {
    return blobStorageService
        .fetchFile(franklinRulesFileName)
        .doOnSuccess(
            bytes ->
                log.info("Successfully fetched guidance data file, size: {} bytes", bytes.length))
        .doOnError(e -> log.error("Error fetching guidance data file: {}", e.getMessage(), e));
  }

  private Mono<byte[]> fetchBrdRules(String brdRulesFileUrl) {
    return blobStorageService
        .fetchFileFromUrl(brdRulesFileUrl)
        .doOnSuccess(
            bytes -> log.info("Successfully fetched BRD rules file, size: {} bytes", bytes.length))
        .doOnError(e -> log.error("Error fetching BRD rules file: {}", e.getMessage(), e));
  }

  private List<GuidanceData> parseGuidanceData(byte[] guidanceDataBytes) {
    log.info("Parsing guidance data...");
    List<GuidanceData> guidanceDataList =
        fileReader.parseByteArrayContent(
            guidanceDataBytes, FileType.STANDARD_DATA, GuidanceData.class);
    log.info("Parsed {} guidance data entries", guidanceDataList.size());
    return guidanceDataList;
  }

  private List<BrdRules> parseBrdRules(byte[] brdRulesBytes) {
    log.info("Parsing BRD rules...");
    List<BrdRules> brdRulesList =
        fileReader.parseByteArrayContent(brdRulesBytes, FileType.USER_RULES, BrdRules.class);
    log.info("Parsed {} BRD rules", brdRulesList.size());
    return brdRulesList;
  }

  private Map<String, List<GuidanceData>> createGuidanceDataMap(
      List<GuidanceData> guidanceDataList) {
    log.info("Creating guidance data map...");
    return guidanceDataList.stream().collect(Collectors.groupingBy(GuidanceData::getRuleName));
  }

  private List<BrdRules> findNewRules(
      List<BrdRules> brdRulesList, Map<String, List<GuidanceData>> ruleNameToGuidanceMap) {
    log.info("Finding new rules...");
    List<BrdRules> newRules =
        brdRulesList.stream()
            .filter(rule -> !ruleNameToGuidanceMap.containsKey(rule.getRuleName()))
            .toList();
    log.info("Found {} new rules to process with AI", newRules.size());
    return newRules;
  }

  private Mono<List<GuidanceData>> processNewRules(
      List<BrdRules> newRules, List<GuidanceData> guidanceDataList) {
    if (newRules.isEmpty()) {
      log.info("No new rules to process, using existing guidance data");
      return Mono.just(guidanceDataList);
    }

    return assistantService
        .findSemanticMatches(newRules)
        .doOnSubscribe(s -> log.info("Starting semantic matching process"))
        .doOnSuccess(r -> log.info("Semantic matching completed successfully"))
        .doOnError(e -> log.error("Error in semantic matching: {}", e.getMessage(), e))
        .flatMap(newGuidanceData -> updateGuidanceWithNewData(newGuidanceData, guidanceDataList));
  }

  private Mono<List<GuidanceData>> updateGuidanceWithNewData(
      List<GuidanceData> newGuidanceData, List<GuidanceData> existingGuidanceData) {
    if (newGuidanceData == null || newGuidanceData.isEmpty()) {
      return Mono.just(existingGuidanceData);
    }

    List<GuidanceData> combinedGuidanceData = new ArrayList<>(existingGuidanceData);
    Set<String> existingRuleNames =
        existingGuidanceData.stream().map(GuidanceData::getRuleName).collect(Collectors.toSet());

    for (GuidanceData newData : newGuidanceData) {
      if (newData.getRuleName() != null && !existingRuleNames.contains(newData.getRuleName())) {
        combinedGuidanceData.add(newData);
      }
    }

    return updateGuidanceDocument(combinedGuidanceData)
        .thenReturn(combinedGuidanceData)
        .onErrorResume(
            e -> {
              log.error("Error updating guidance document: {}", e.getMessage(), e);
              return Mono.just(existingGuidanceData);
            });
  }

  private Mono<Resource> createCombinedRulesResource(
      List<BrdRules> brdRulesList, List<GuidanceData> guidanceDataList) {
    try {
      List<RulesWithData> combinedRules = createCombinedRules(brdRulesList, guidanceDataList);
      byte[] jsonData =
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(combinedRules);
      return Mono.just(
          new ByteArrayResource(jsonData) {
            @Override
            public String getFilename() {
              return "combined_rules.json";
            }
          });
    } catch (JsonProcessingException e) {
      log.error("Error creating JSON: {}", e.getMessage(), e);
      return Mono.error(new RuntimeException("Error creating JSON", e));
    }
  }

  private List<RulesWithData> createCombinedRules(
      List<BrdRules> brdRulesList, List<GuidanceData> guidanceDataList) {
    Map<String, List<GuidanceData>> ruleNameToGuidanceMap = createGuidanceDataMap(guidanceDataList);
    List<RulesWithData> combinedRules = new ArrayList<>();
    Set<String> noMappingRules = new HashSet<>();
    AtomicInteger totalRules = new AtomicInteger(0);
    AtomicInteger mappedRules = new AtomicInteger(0);

    processBrdRulesForCombining(
        brdRulesList,
        ruleNameToGuidanceMap,
        combinedRules,
        noMappingRules,
        totalRules,
        mappedRules);
    logRuleProcessingResults(totalRules.get(), mappedRules.get(), noMappingRules, combinedRules);

    return combinedRules;
  }

  private void processBrdRulesForCombining(
      List<BrdRules> brdRulesList,
      Map<String, List<GuidanceData>> ruleNameToGuidanceMap,
      List<RulesWithData> combinedRules,
      Set<String> noMappingRules,
      AtomicInteger totalRules,
      AtomicInteger mappedRules) {
    for (BrdRules brdRule : brdRulesList) {
      totalRules.incrementAndGet();
      RulesWithData combinedRule =
          createCombinedRule(brdRule, ruleNameToGuidanceMap, noMappingRules, mappedRules);
      if (combinedRule != null) {
        combinedRules.add(combinedRule);
      }
    }
  }

  private RulesWithData createCombinedRule(
      BrdRules brdRule,
      Map<String, List<GuidanceData>> ruleNameToGuidanceMap,
      Set<String> noMappingRules,
      AtomicInteger mappedRules) {
    List<GuidanceData> matchingGuidanceList = ruleNameToGuidanceMap.get(brdRule.getRuleName());
    GuidanceData validMapping = findValidMapping(matchingGuidanceList);

    if (shouldCreateCombinedRule(brdRule, validMapping)) {
      RulesWithData combinedRule = new RulesWithData();
      populateBasicRuleInfo(combinedRule, brdRule);

      if (validMapping != null) {
        mappedRules.incrementAndGet();
        populateGuidanceInfo(combinedRule, validMapping);
      } else {
        noMappingRules.add(brdRule.getRuleName() + " (ID: " + brdRule.getRuleId() + ")");
      }

      return combinedRule;
    }
    return null;
  }

  private GuidanceData findValidMapping(List<GuidanceData> matchingGuidanceList) {
    if (matchingGuidanceList == null || matchingGuidanceList.isEmpty()) {
      return null;
    }
    return matchingGuidanceList.stream()
        .filter(data -> data.getMappingKey() != null && !data.getMappingKey().equals("No Mapping"))
        .findFirst()
        .orElse(null);
  }

  private boolean shouldCreateCombinedRule(BrdRules brdRule, GuidanceData validMapping) {
    return "1013".equals(brdRule.getRuleId())
        || (brdRule.getBrdId() != null
            && brdRule.getBrdName() != null
            && (brdRule.getRuleId() == null || brdRule.getRuleName() == null))
        || validMapping != null;
  }

  private void populateBasicRuleInfo(RulesWithData combinedRule, BrdRules brdRule) {
    combinedRule.setBrdId(brdRule.getBrdId());
    combinedRule.setBrdName(brdRule.getBrdName());
    combinedRule.setRuleId(brdRule.getRuleId());
    combinedRule.setRuleName(brdRule.getRuleName());
    combinedRule.setValue(brdRule.getValue());
    combinedRule.setOrder(brdRule.getOrder());
  }

  private void populateGuidanceInfo(RulesWithData combinedRule, GuidanceData validMapping) {
    combinedRule.setMappingKey(validMapping.getMappingKey());
    combinedRule.setExplanation(validMapping.getExplanation());
    combinedRule.setSimilarity(validMapping.getSimilarity());
  }

  private void logRuleProcessingResults(
      int totalRules,
      int mappedRules,
      Set<String> noMappingRules,
      List<RulesWithData> combinedRules) {
    log.info(
        "Processing complete - Total Rules: {}, Mapped: {}, No Mapping: {}",
        totalRules,
        mappedRules,
        noMappingRules.size());
    if (!noMappingRules.isEmpty()) {
      log.info("Rules with no mapping: {}", String.join(", ", noMappingRules));
    }
    log.info("Processed {} rules into {} combined rules", totalRules, combinedRules.size());

    if (!combinedRules.isEmpty()) {
      long rulesWithMapping =
          combinedRules.stream()
              .filter(r -> r.getMappingKey() != null && !r.getMappingKey().isEmpty())
              .count();
      log.info(
          "Rules with mapping: {}/{} ({}%)",
          rulesWithMapping, combinedRules.size(), (rulesWithMapping * 100) / combinedRules.size());
    }
  }

  @Override
  public Mono<Resource> getRulesWithData(LegacyBrdRequest request) {
    log.info("Starting getRulesWithData for BRD ID: {}", request.getBrdId());
    log.info("BRD Rules File URL: {}", request.getBrdRulesFileUrl());

    return Mono.zip(fetchGuidanceData(), fetchBrdRules(request.getBrdRulesFileUrl()))
        .flatMap(
            tuple -> {
              List<GuidanceData> guidanceDataList = parseGuidanceData(tuple.getT1());
              List<BrdRules> brdRulesList = parseBrdRules(tuple.getT2());

              return processRootEntityAndSites(brdRulesList, request.getBrdId())
                  .flatMap(
                      isSaved -> {
                        boolean saved = isSaved;
                        if (!saved) {
                          return Mono.error(new RuntimeException("Failed to save BRD data"));
                        }

                        Map<String, List<GuidanceData>> ruleNameToGuidanceMap =
                            createGuidanceDataMap(guidanceDataList);
                        List<BrdRules> newRules = findNewRules(brdRulesList, ruleNameToGuidanceMap);

                        return processNewRules(newRules, guidanceDataList)
                            .flatMap(
                                updatedGuidanceData ->
                                    createCombinedRulesResource(brdRulesList, updatedGuidanceData));
                      });
            });
  }

  private Mono<Void> updateGuidanceDocument(List<GuidanceData> guidanceData) {
    // Convert guidance data to pipe-separated format
    String newContent =
        guidanceData.stream()
            .map(
                data ->
                    String.join(
                        "|",
                        data.getRuleName(),
                        data.getMappingKey(),
                        data.getSimilarity(),
                        data.getExplanation(),
                        data.getQuestiondId()))
            .collect(Collectors.joining("\n"));

    log.info("Updating guidance document with {} total rules", guidanceData.size());

    // Update the guidance document in blob storage
    return blobStorageService
        .updateFile(franklinRulesFileName, newContent)
        .doOnSuccess(v -> log.info("Successfully updated guidance document"))
        .doOnError(e -> log.error("Error updating guidance document: {}", e.getMessage(), e));
  }

  /**
   * Prefills a legacy BRD by fetching BRD data and converting it to PrefillSections.
   *
   * @param request LegacyPrefillRequest containing brdId and documentName
   * @return Mono<Boolean> indicating success or failure
   */
  @Override
  public Mono<Boolean> prefillLegacyBRD(LegacyPrefillRequest request) {
    String brdId = request.getBrdId();
    String documentName = request.getDocumentName();

    log.info("Prefill request received - BRD ID: {}, Document: {}", brdId, documentName);

    return findLegacyBrdAndProcess(brdId, documentName);
  }

  /** Finds the LegacyBRD and processes it */
  private Mono<Boolean> findLegacyBrdAndProcess(String brdId, String documentName) {
    log.info("Searching for LegacyBRD with BRDId: {}", brdId);

    return legacyBRDRepository
        .findByBrdId(brdId)
        .flatMap(
            legacyBrd -> {
              log.info("Found LegacyBRD with BRDId: {}", legacyBrd.getBrdId());

              return fetchBrdResponseAndProcess(brdId, legacyBrd, documentName);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "No LegacyBRD found with BRDId: {}, proceeding without legacy data", brdId);
                  return fetchBrdResponseAndProcess(brdId, null, documentName);
                }))
        .onErrorResume(
            e -> {
              log.error("Error finding LegacyBRD with BRDId {}: {}", brdId, e.getMessage(), e);
              return Mono.just(false);
            });
  }

  /** Fetches BRD response and processes it */
  private Mono<Boolean> fetchBrdResponseAndProcess(
      String brdId, LegacyBRD legacyBrd, String documentName) {
    log.info("Fetching BRD data from BRD service for BRDId: {}", brdId);

    return brdService
        .getBrdById(brdId)
        .flatMap(
            responseEntity -> {
              log.info("BRD service response received");
              return processBrdResponse(responseEntity, brdId, legacyBrd, documentName);
            })
        .onErrorResume(
            e -> {
              log.error("Error fetching BRD with ID {}: {}", brdId, e.getMessage(), e);
              return Mono.just(false);
            });
  }

  /** Processes the BRD response to generate PrefillSections */
  private Mono<Boolean> processBrdResponse(
      ResponseEntity<Api<BRDResponse>> responseEntity,
      String brdId,
      LegacyBRD legacyBrd,
      String documentName) {
    Api<BRDResponse> apiResponse = responseEntity.getBody();

    if (apiResponse == null || apiResponse.getData().isEmpty()) {
      log.error("No BRD data found in the response for BRD ID: {}", brdId);
      return Mono.just(false);
    }

    BRDResponse brdResponse = apiResponse.getData().get();
    log.info("Processing BRD response for BRD ID: {}", brdResponse.getBrdId());
    return convertAndProcessPrefillSections(brdResponse, brdId, legacyBrd, documentName);
  }

  /** Converts BRDResponse to PrefillSections and processes it */
  private Mono<Boolean> convertAndProcessPrefillSections(
      BRDResponse brdResponse, String brdId, LegacyBRD legacyBrd, String documentName) {
    try {
      log.info("Converting BRDResponse to PrefillSections for BRD ID: {}", brdId);
      PrefillSections prefillSections = dtoModelMapper.mapResponseToPrefillSections(brdResponse);
      JsonNode prefillJson = objectMapper.valueToTree(prefillSections);
      List<String> documentNames = new ArrayList<>();
      documentNames.add(documentName);

      return legacyBrd != null && legacyBrd.getMain() != null
          ? processWithLegacyBrd(prefillJson, brdId, legacyBrd, documentNames)
          : processWithoutLegacyBrd(prefillJson, brdId, documentNames);
    } catch (Exception e) {
      return handleConversionError(e, brdId, legacyBrd);
    }
  }

  private Mono<Boolean> processWithLegacyBrd(
      JsonNode prefillJson, String brdId, LegacyBRD legacyBrd, List<String> documentNames) {
    log.info("Enhancing PrefillSections with LegacyBRD data - BRD ID: {}", brdId);
    String brdContext = BRD_CONTEXT_TEMPLATE.replace("{brdId}", legacyBrd.getMain().getId());

    return assistantService
        .prefillBRDProcessJson(prefillJson, documentNames, ContextName.LEGACY_PREFILL, brdContext)
        .flatMap(
            processedJson ->
                processJsonAndUpdateBrd(processedJson, brdId, legacyBrd, documentNames));
  }

  private Mono<Boolean> processWithoutLegacyBrd(
      JsonNode prefillJson, String brdId, List<String> documentNames) {
    log.info("No legacy BRD data available, calling AssistantService without additional context");
    return assistantService
        .prefillBRDProcessJson(prefillJson, documentNames)
        .flatMap(
            processedJson -> processJsonAndUpdateBrd(processedJson, brdId, null, documentNames));
  }

  private Mono<Boolean> processJsonAndUpdateBrd(
      JsonNode processedJson, String brdId, LegacyBRD legacyBrd, List<String> documentNames) {
    JsonNode cleanedJson = cleanupJsonForValidation(processedJson);
    Map<String, Object> fieldsToUpdate = convertJsonToMap(cleanedJson);
    if (fieldsToUpdate == null) return Mono.just(false);

    return updateBrdAndProcessSites(brdId, cleanedJson, fieldsToUpdate, legacyBrd, documentNames);
  }

  private Map<String, Object> convertJsonToMap(JsonNode cleanedJson) {
    try {
      return objectMapper.convertValue(cleanedJson, Map.class);
    } catch (Exception e) {
      log.error("Error converting processed JSON to Map: {}", e.getMessage(), e);
      return Collections.emptyMap();
    }
  }

  private Mono<Boolean> updateBrdAndProcessSites(
      String brdId,
      JsonNode cleanedJson,
      Map<String, Object> fieldsToUpdate,
      LegacyBRD legacyBrd,
      List<String> documentNames) {
    return brdService
        .updateBrdPartiallyWithOrderedOperations(brdId, fieldsToUpdate)
        .flatMap(
            response -> handleBrdUpdateResponse(response, cleanedJson, legacyBrd, documentNames));
  }

  private Mono<Boolean> handleBrdUpdateResponse(
      ResponseEntity<Api<BRDResponse>> response,
      JsonNode cleanedJson,
      LegacyBRD legacyBrd,
      List<String> documentNames) {
    if (response.getBody() == null || !response.getBody().getData().isPresent()) {
      log.warn("BRD response body or data is empty");
      return Mono.just(true);
    }

    String updatedBrdId = response.getBody().getData().get().getBrdId();
    return processSitesIfPresent(updatedBrdId, cleanedJson, legacyBrd, documentNames);
  }

  private Mono<Boolean> processSitesIfPresent(
      String updatedBrdId, JsonNode cleanedJson, LegacyBRD legacyBrd, List<String> documentNames) {
    if (legacyBrd == null || legacyBrd.getSites() == null || legacyBrd.getSites().isEmpty()) {
      log.info("No sites found in LegacyBRD to create for BRD ID: {}", updatedBrdId);
      return Mono.just(true);
    }

    BrdForm brdForm = convertJsonToBrdForm(cleanedJson);
    if (brdForm == null) return Mono.just(true);

    return createAndProcessSites(
        updatedBrdId, legacyBrd.getSites(), brdForm, cleanedJson, documentNames);
  }

  private BrdForm convertJsonToBrdForm(JsonNode cleanedJson) {
    try {
      return objectMapper.treeToValue(cleanedJson, BrdForm.class);
    } catch (Exception e) {
      log.error("Error converting cleanedJson to BrdForm: {}", e.getMessage(), e);
      return null;
    }
  }

  private Mono<Boolean> createAndProcessSites(
      String updatedBrdId,
      List<LegacyBRDInfo> sites,
      BrdForm brdForm,
      JsonNode cleanedJson,
      List<String> documentNames) {
    List<Mono<Site>> siteMonos =
        sites.stream().map(siteInfo -> createSiteMono(updatedBrdId, siteInfo, brdForm)).toList();

    return Flux.merge(siteMonos)
        .collectList()
        .flatMap(savedSites -> processSavedSites(savedSites, cleanedJson, documentNames));
  }

  private Mono<Boolean> processSavedSites(
      List<Site> savedSites, JsonNode cleanedJson, List<String> documentNames) {
    if (savedSites.isEmpty()) {
      log.warn("No sites were created/updated");
      return Mono.just(true);
    }

    return processPrefillForEachSite(savedSites, cleanedJson, documentNames);
  }

  private Mono<Boolean> handleConversionError(Exception e, String brdId, LegacyBRD legacyBrd) {
    log.error("Error converting/processing BRDResponse: {}", e.getMessage(), e);
    if (legacyBrd == null) {
      throw new LegacyBrdNotFoundException("Legacy BRD data is not available for BRD ID: " + brdId);
    }
    return Mono.just(false);
  }

  /**
   * Cleans up the JSON response from AI service to remove fields that don't exist in the model
   *
   * @param jsonNode The original JSON response
   * @return A cleaned up JSON node that will pass validation
   */
  private JsonNode cleanupJsonForValidation(JsonNode jsonNode) {
    try {
      JsonNode copy = jsonNode.deepCopy();
      ObjectNode objectNode = (ObjectNode) copy;

      cleanupGeneralImplementations(objectNode);
      cleanupAciInformation(objectNode);

      log.info("JSON cleanup complete");
      return objectNode;
    } catch (Exception e) {
      log.error("Error during JSON cleanup: {}", e.getMessage(), e);
      return jsonNode;
    }
  }

  private void cleanupGeneralImplementations(ObjectNode objectNode) {
    if (!objectNode.has("generalImplementations")) {
      return;
    }

    ObjectNode generalImplementations = (ObjectNode) objectNode.get("generalImplementations");
    if (generalImplementations.has("rules")) {
      generalImplementations.remove("rules");
    }

    Iterator<String> fieldIterator = generalImplementations.fieldNames();
    while (fieldIterator.hasNext()) {
      String fieldName = fieldIterator.next();
      if (!fieldName.equals("implementationNotes") && !fieldName.equals("sectionStatus")) {
        fieldIterator.remove();
      }
    }
  }

  private void cleanupAciInformation(ObjectNode objectNode) {
    if (!objectNode.has("aciInformation")) {
      return;
    }

    ObjectNode aciInformation = (ObjectNode) objectNode.get("aciInformation");
    Set<String> validAciFields =
        Set.of(
            "ITContactName",
            "ITContactTitle",
            "ITContactPhone",
            "ITContactEmail",
            "ITContactExtension",
            "sectionStatus");

    Iterator<String> fieldIterator = aciInformation.fieldNames();
    while (fieldIterator.hasNext()) {
      String fieldName = fieldIterator.next();
      if (!validAciFields.contains(fieldName)) {
        fieldIterator.remove();
      }
    }
  }

  /**
   * Processes prefill operations for each site with site-specific context
   *
   * @param sites List of sites to process
   * @param baseJsonNode The base JSON node to use for prefill
   * @param documentNames List of document names
   * @return Mono<Boolean> indicating success
   */
  private Mono<Boolean> processPrefillForEachSite(
      List<Site> sites, JsonNode baseJsonNode, List<String> documentNames) {
    log.info("Starting prefill processing for {} sites", sites.size());
    return Flux.fromIterable(sites)
        .concatMap(site -> processSingleSite(site, baseJsonNode, documentNames))
        .collectList()
        .map(this::calculateProcessingResults)
        .defaultIfEmpty(true)
        .doOnError(e -> log.error("Error in overall site processing: {}", e.getMessage(), e));
  }

  private Mono<Boolean> processSingleSite(
      Site site, JsonNode baseJsonNode, List<String> documentNames) {
    log.info("Processing prefill for site: {} ({})", site.getSiteName(), site.getSiteId());
    String siteContext = createSiteContext(site);
    JsonNode siteJsonNode = cloneJsonForSite(baseJsonNode);
    if (siteJsonNode == null) return Mono.just(false);

    return processSiteWithContext(site, siteJsonNode, documentNames, siteContext);
  }

  private String createSiteContext(Site site) {
    String siteContext = BRD_CONTEXT_TEMPLATE.replace("{brdId}", site.getSiteId());
    log.info("Created site context for site {}: {}", site.getSiteId(), siteContext);
    return siteContext;
  }

  private JsonNode cloneJsonForSite(JsonNode baseJsonNode) {
    try {
      return objectMapper.readTree(objectMapper.writeValueAsString(baseJsonNode));
    } catch (Exception e) {
      log.error("Error cloning JSON for site: {}", e.getMessage(), e);
      return null;
    }
  }

  private Mono<Boolean> processSiteWithContext(
      Site site, JsonNode siteJsonNode, List<String> documentNames, String siteContext) {
    log.info("Starting prefill processing for site {} with context", site.getSiteId());
    return assistantService
        .prefillBRDProcessJson(siteJsonNode, documentNames, ContextName.LEGACY_PREFILL, siteContext)
        .flatMap(processedJson -> updateSiteWithProcessedJson(site, processedJson));
  }

  private Mono<Boolean> updateSiteWithProcessedJson(Site site, JsonNode processedJson) {
    try {
      BrdForm updatedBrdForm = objectMapper.treeToValue(processedJson, BrdForm.class);
      log.info("Successfully converted processed JSON to BrdForm for site: {}", site.getSiteId());
      return updateSiteWithBrdForm(site, updatedBrdForm);
    } catch (Exception e) {
      log.error(
          "Error converting processed JSON to BrdForm for site {}: {}",
          site.getSiteId(),
          e.getMessage(),
          e);
      return Mono.just(false);
    }
  }

  private Mono<Boolean> updateSiteWithBrdForm(Site site, BrdForm updatedBrdForm) {
    site.setBrdForm(updatedBrdForm);
    site.setUpdatedAt(LocalDateTime.now());

    return siteRepository
        .save(site)
        .map(
            updatedSite -> {
              log.info("Successfully updated site {} with prefilled data", updatedSite.getSiteId());
              return true;
            })
        .onErrorResume(
            e -> {
              log.error(
                  "Error saving prefilled data for site {}: {}",
                  site.getSiteId(),
                  e.getMessage(),
                  e);
              return Mono.just(false);
            });
  }

  private boolean calculateProcessingResults(List<Boolean> results) {
    boolean allSuccessful = results.stream().allMatch(Boolean::booleanValue);
    log.info(
        "Completed prefill processing for all sites. Success rate: {}/{}",
        results.stream().filter(Boolean::booleanValue).count(),
        results.size());
    return allSuccessful;
  }

  /**
   * Creates a Mono<Site> for a given site info and BRD form
   *
   * @param updatedBrdId The BRD ID
   * @param siteInfo The legacy site information
   * @param brdForm The BRD form to be associated with the site
   * @return Mono<Site> representing the site operation
   */
  private Mono<Site> createSiteMono(String updatedBrdId, LegacyBRDInfo siteInfo, BrdForm brdForm) {
    log.info(
        "Creating site mono for brdId: {}, siteId: {}, siteName: {}",
        updatedBrdId,
        siteInfo.getId(),
        siteInfo.getName());

    return siteRepository
        .findByBrdIdAndSiteId(updatedBrdId, siteInfo.getId())
        .doOnNext(this::logExistingSite)
        .switchIfEmpty(Mono.defer(() -> createAndSaveNewSite(updatedBrdId, siteInfo, brdForm)))
        .flatMap(site -> updateExistingSite(site, brdForm))
        .doOnError(error -> logSiteError(siteInfo, error));
  }

  private void logExistingSite(Site existingSite) {
    if (existingSite != null) {
      log.info(
          "Found existing site: {} ({})", existingSite.getSiteName(), existingSite.getSiteId());
    }
  }

  private Mono<Site> createAndSaveNewSite(String brdId, LegacyBRDInfo siteInfo, BrdForm brdForm) {
    log.info(
        "No existing site found. Creating new site for brdId: {} and siteId: {}",
        brdId,
        siteInfo.getId());

    Site newSite = createNewSite(brdId, siteInfo, brdForm);
    return saveSite(newSite);
  }

  private Site createNewSite(String brdId, LegacyBRDInfo siteInfo, BrdForm brdForm) {
    Site newSite = new Site();
    newSite.setBrdId(brdId);
    newSite.setSiteId(siteInfo.getId());
    newSite.setSiteName(siteInfo.getName());
    newSite.setIdentifierCode(siteInfo.getId());
    newSite.setDescription("Site created from legacy BRD data");
    newSite.setBrdForm(brdForm);
    newSite.setCreatedAt(LocalDateTime.now());
    newSite.setUpdatedAt(LocalDateTime.now());
    return newSite;
  }

  private Mono<Site> saveSite(Site site) {
    log.info("Attempting to save new site: {} ({})", site.getSiteName(), site.getSiteId());
    return siteRepository
        .save(site)
        .doOnSuccess(
            saved ->
                log.info(
                    "Successfully saved new site: {} ({})", saved.getSiteName(), saved.getSiteId()))
        .doOnError(
            error ->
                log.error(
                    "Error saving new site {} ({}): {}",
                    site.getSiteName(),
                    site.getSiteId(),
                    error.getMessage(),
                    error));
  }

  private Mono<Site> updateExistingSite(Site site, BrdForm brdForm) {
    if (site.getId() != null) {
      log.info("Updating existing site: {} ({})", site.getSiteName(), site.getSiteId());
      site.setBrdForm(brdForm);
      site.setUpdatedAt(LocalDateTime.now());
      return saveSite(site);
    }
    return Mono.just(site);
  }

  private void logSiteError(LegacyBRDInfo siteInfo, Throwable error) {
    log.error(
        "Error in site creation/update process for site {} ({}): {}",
        siteInfo.getName(),
        siteInfo.getId(),
        error.getMessage(),
        error);
  }

  /** Custom exception for Legacy BRD related errors */
  public static class LegacyBrdNotFoundException extends RuntimeException {
    public LegacyBrdNotFoundException(String message) {
      super(message);
    }
  }
}
