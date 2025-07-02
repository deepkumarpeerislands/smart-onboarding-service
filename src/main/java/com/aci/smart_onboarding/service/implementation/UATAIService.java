package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.dto.UATConfiguratorResponseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestType;
import com.aci.smart_onboarding.model.Artifact;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.service.IAIService;
import com.aci.smart_onboarding.service.IArtifactService;
import com.aci.smart_onboarding.service.IPortalConfigurationService;
import com.aci.smart_onboarding.service.IUATAIService;
import com.aci.smart_onboarding.service.IUATConfiguratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

@Service
@Slf4j
public class UATAIService implements IUATAIService {

  private final BRDRepository brdRepository;
  private final IUATConfiguratorService uatConfiguratorService;
  private final ObjectMapper objectMapper;
  private final IAIService aiService;
  private final String fieldMatchingPrompt;
  private final UATTestCaseService uatTestCaseService;
  private final IArtifactService artifactService;
  private final IPortalConfigurationService portalConfigurationService;
  private static final int SEARCH_LIMIT = 5;
  private static final int ARTIFACT_SEARCH_LIMIT = 10;
  private static final String TEST_RIGOR_DOC = "TestRigor";
  private static final String TESTRIGOR_ERROR_DOC = "TestRigor_Fix";
  private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^}]*}");
  private static final String CONTEXT_TEMPLATE =
      "Here we have multiple contexts. Context 1 is {documentation}, which provides basic guidance for "
          + "writing TestRigor test cases. Context 2 is {relevantTestCases}, which has a list of test cases "
          + "that may match the scenario. Context 3 is {data}, which has data related to the test cases that "
          + "you need to write. Context 4 is {testCases}, which contains previous test cases. Use these test "
          + "cases to write new test cases where you need to get to a position to navigate to a page. Context 5 is {mistakes}, these are some common mistakes "
          + "that are done by AI while writing test cases and make sure those are not repeated. Write test cases based on "
          + "this information and your knowledge";

  private static final String QUESTION_TEMPLATE =
      "Here we have a scenario: {scenario} and the position of the scenario is {position}. "
          + "Write test cases based on this information and your knowledge. Use context 4 testcases "
          + "to write new test cases where you need to get to a position.";

  private static final String ANSWER_FIELD = "answer";
  private static final String CONFIGURATION_TEST_CASES = "configurationTestCases";
  private static final String CONFIGURATION_NAME = "configurationName";
  private static final String RELEVANT_TEST_CASES = "relevantTestCases";
  private static final String DOCUMENTATION = "documentation";
  private static final String ERROR_DOC = "error_doc";
  private static final String TEST_CASES = "testCases";
  private static final String DATA = "data";
  private static final String VARIABLES = "variables";
  private static final String TEST_NAME = "testname";
  private static final String BRD_NOT_FOUND = "BRD not found with ID: ";

  public UATAIService(
      BRDRepository brdRepository,
      IUATConfiguratorService uatConfiguratorService,
      ObjectMapper objectMapper,
      IAIService aiService,
      UATTestCaseService uatTestCaseService,
      IArtifactService artifactService,
      IPortalConfigurationService portalConfigurationService,
      ResourceLoader resourceLoader)
      throws IOException {
    this.brdRepository = brdRepository;
    this.uatConfiguratorService = uatConfiguratorService;
    this.objectMapper = objectMapper;
    this.aiService = aiService;
    this.uatTestCaseService = uatTestCaseService;
    this.artifactService = artifactService;
    this.portalConfigurationService = portalConfigurationService;

    Resource resource = resourceLoader.getResource("classpath:prompts.json");
    JsonNode prompts = objectMapper.readTree(resource.getInputStream());
    this.fieldMatchingPrompt = prompts.get("fieldMatching").asText();
  }

  @Override
  public Flux<UATTestCaseRequestResponseDTO> generateUATTestCases(
      String brdId, List<String> configurationNames, PortalTypes uatType) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new RuntimeException(BRD_NOT_FOUND + brdId)))
        .flatMapMany(
            brd -> processConfigurationsAndGenerateTestCases(brd, configurationNames, uatType));
  }

  private Flux<UATTestCaseRequestResponseDTO> processConfigurationsAndGenerateTestCases(
      BRD brd, List<String> configurationNames, PortalTypes uatType) {
    return uatConfiguratorService
        .getConfigurationsByNames(configurationNames)
        .flatMap(config -> findMatchingFieldsUsingAI(brd, config))
        .collectList()
        .flatMapMany(
            jsonNodes ->
                generateTestCasesFromFields(
                    jsonNodes, configurationNames, brd.getBrdId(), uatType));
  }

  private Flux<UATTestCaseRequestResponseDTO> generateTestCasesFromFields(
      List<JsonNode> jsonNodes,
      List<String> configurationNames,
      String brdId,
      PortalTypes uatType) {
    ObjectNode combinedNode = objectMapper.createObjectNode();
    jsonNodes.forEach(node -> combinedNode.setAll((ObjectNode) node));

    return uatConfiguratorService
        .getConfigurationsByNames(configurationNames)
        .collectList()
        .flatMapMany(configs -> generatingTestCases(configs, brdId, uatType));
  }

  private Flux<UATTestCaseRequestResponseDTO> generatingTestCases(
      List<UATConfiguratorResponseDTO> configurations, String brdId, PortalTypes uatType) {
    return Mono.zip(
            getRelevantTestCases(configurations, uatType),
            getExistingTestCases(brdId, uatType),
            getTestRigorDocumentation(configurations),
            getTestRigorErrorFixData(configurations),
            getBrdAndPortalData(configurations, brdId, uatType))
        .flatMapMany(tuple -> processTestCaseGeneration(tuple, configurations, brdId, uatType));
  }

  private Flux<UATTestCaseRequestResponseDTO> processTestCaseGeneration(
      Tuple5<JsonNode, JsonNode, JsonNode, JsonNode, JsonNode> contextData,
      List<UATConfiguratorResponseDTO> configurations,
      String brdId,
      PortalTypes uatType) {
    return Flux.fromIterable(configurations)
        .concatMap(config -> generateTestCasesFromLLM(contextData, config, brdId, uatType));
  }

  private Flux<UATTestCaseRequestResponseDTO> generateTestCasesFromLLM(
      Tuple5<JsonNode, JsonNode, JsonNode, JsonNode, JsonNode> contextData,
      UATConfiguratorResponseDTO config,
      String brdId,
      PortalTypes uatType) {
    String context = buildLLMContext(contextData, config);
    String question = buildLLMQuestion(config);
    return aiService
        .generateAnswer(question, context, ContextName.UAT_TEST_GENERATION.getPrompt())
        .flatMapMany(response -> processLLMResponse(response, config, brdId, uatType));
  }

  private String buildLLMContext(
      Tuple5<JsonNode, JsonNode, JsonNode, JsonNode, JsonNode> contextData,
      UATConfiguratorResponseDTO config) {
    JsonNode configTestCases =
        extractConfigurationTestCases(contextData.getT1(), config.getConfigurationName());
    ObjectNode combinedNode = createCombinedContextNode(contextData, configTestCases);

    return CONTEXT_TEMPLATE
        .replace("{documentation}", combinedNode.get(DOCUMENTATION).asText())
        .replace("{relevantTestCases}", combinedNode.get(RELEVANT_TEST_CASES).toString())
        .replace("{data}", combinedNode.get(DATA).toString())
        .replace("{testCases}", combinedNode.get(TEST_CASES).toString())
        .replace("{mistakes}", combinedNode.get(ERROR_DOC).asText());
  }

  private String buildLLMQuestion(UATConfiguratorResponseDTO config) {
    return QUESTION_TEMPLATE
        .replace("{scenario}", config.getScenario())
        .replace("{position}", config.getPosition());
  }

  private Flux<UATTestCaseRequestResponseDTO> processLLMResponse(
      String response, UATConfiguratorResponseDTO config, String brdId, PortalTypes uatType) {
    if (response == null || response.trim().isEmpty()) {
      log.error("Received null or empty response from LLM");
      return Flux.empty();
    }

    try {
      String cleanedResponse = cleanLLMResponse(response);
      JsonNode aiResponse = objectMapper.readTree(cleanedResponse);

      if (!aiResponse.isArray()) {
        log.error("AI response is not in expected array format");
        return Flux.empty();
      }

      return Flux.fromIterable(aiResponse::elements)
          .flatMap(testCase -> createAndSaveTestCase(testCase, config, brdId, uatType))
          .filter(Objects::nonNull);
    } catch (Exception e) {
      log.error("Error processing AI response: {}", e.getMessage());
      return Flux.empty();
    }
  }

  private Mono<UATTestCaseRequestResponseDTO> createAndSaveTestCase(
      JsonNode testCase, UATConfiguratorResponseDTO config, String brdId, PortalTypes uatType) {
    try {
      if (testCase == null || !testCase.has(TEST_NAME) || !testCase.has(ANSWER_FIELD)) {
        log.error("Invalid test case format: missing required fields");
        return Mono.empty();
      }

      Map<String, String> variables = extractVariables(testCase);
      UATTestCaseRequestResponseDTO testCaseDTO =
          buildTestCaseDTO(testCase, config, brdId, uatType, variables);

      return uatTestCaseService
          .createTestCase(testCaseDTO)
          .onErrorResume(
              error -> {
                log.error("Error creating test case: {}", error.getMessage());
                return Mono.empty();
              });
    } catch (Exception e) {
      log.error("Error processing test case: {}", e.getMessage());
      return Mono.empty();
    }
  }

  private Map<String, String> extractVariables(JsonNode testCase) {
    Map<String, String> variables = new HashMap<>();
    try {
      JsonNode variablesNode = testCase.get(VARIABLES);
      if (variablesNode != null && variablesNode.isObject()) {
        variablesNode
            .fields()
            .forEachRemaining(
                entry -> {
                  JsonNode value = entry.getValue();
                  if (value != null && !value.isNull()) {
                    variables.put(entry.getKey(), value.asText());
                  }
                });
      }
    } catch (Exception e) {
      log.error("Error extracting variables: {}", e.getMessage());
    }
    return variables;
  }

  private UATTestCaseRequestResponseDTO buildTestCaseDTO(
      JsonNode testCase,
      UATConfiguratorResponseDTO config,
      String brdId,
      PortalTypes uatType,
      Map<String, String> variables) {
    try {
      return UATTestCaseRequestResponseDTO.builder()
          .brdId(brdId)
          .testName(testCase.get(TEST_NAME).asText())
          .scenario(config.getScenario())
          .position(config.getPosition())
          .answer(testCase.get(ANSWER_FIELD).asText())
          .uatType(uatType)
          .testType(TestType.NORMAL)
          .featureName(config.getConfigurationName())
          .fields(variables)
          .build();
    } catch (Exception e) {
      log.error("Error building test case DTO: {}", e.getMessage());
      return null;
    }
  }

  private JsonNode extractConfigurationTestCases(JsonNode allTestCases, String configurationName) {
    if (allTestCases != null && allTestCases.get(CONFIGURATION_TEST_CASES).isArray()) {
      for (JsonNode configNode : allTestCases.get(CONFIGURATION_TEST_CASES)) {
        if (configNode.get(CONFIGURATION_NAME).asText().equals(configurationName)) {
          return configNode.get(RELEVANT_TEST_CASES);
        }
      }
    }
    return objectMapper.createArrayNode();
  }

  private ObjectNode createCombinedContextNode(
      Tuple5<JsonNode, JsonNode, JsonNode, JsonNode, JsonNode> contextData,
      JsonNode configTestCases) {
    ObjectNode combinedNode = objectMapper.createObjectNode();
    combinedNode.setAll((ObjectNode) contextData.getT2()); // existing test cases
    combinedNode.setAll((ObjectNode) contextData.getT3()); // documentation
    combinedNode.setAll((ObjectNode) contextData.getT4()); // error documentation
    combinedNode.setAll((ObjectNode) contextData.getT5()); // data
    combinedNode.set(RELEVANT_TEST_CASES, configTestCases);
    return combinedNode;
  }

  /** Cleans the LLM response by removing markdown formatting and other unwanted characters. */
  private String cleanLLMResponse(String response) {
    if (response == null || response.trim().isEmpty()) {
      return "";
    }

    // Remove markdown code block indicators
    response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

    // Find the first occurrence of '[' to get the start of the JSON array
    int startIndex = response.indexOf('[');
    if (startIndex >= 0) {
      response = response.substring(startIndex);
    }

    // Find the last occurrence of ']' to get the end of the JSON array
    int endIndex = response.lastIndexOf(']');
    if (endIndex >= 0) {
      response = response.substring(0, endIndex + 1);
    }

    return response.trim();
  }

  /** Gets relevant test cases using vector search. */
  private Mono<JsonNode> getRelevantTestCases(
      List<UATConfiguratorResponseDTO> configurations, PortalTypes uatType) {
    return Flux.fromIterable(configurations)
        .flatMap(
            config -> {
              String context = buildTestContext(config);
              return aiService
                  .getEmbeddings(context)
                  .flatMapMany(
                      embedding ->
                          uatTestCaseService.performVectorSearch(embedding, SEARCH_LIMIT, uatType))
                  .map(this::extractRelevantFields)
                  .collectList()
                  .map(
                      relevantCases -> {
                        ObjectNode configNode = objectMapper.createObjectNode();
                        configNode.put(CONFIGURATION_NAME, config.getConfigurationName());
                        configNode.set(
                            RELEVANT_TEST_CASES, objectMapper.valueToTree(relevantCases));
                        return configNode;
                      });
            })
        .collectList()
        .map(
            configResults -> {
              ObjectNode resultNode = objectMapper.createObjectNode();
              resultNode.set(CONFIGURATION_TEST_CASES, objectMapper.valueToTree(configResults));
              return resultNode;
            });
  }

  /** Gets existing test cases for the BRD. */
  private Mono<JsonNode> getExistingTestCases(String brdId, PortalTypes uatType) {
    return uatTestCaseService
        .getTestCasesByBrdIdAndUatType(brdId, uatType)
        .map(this::extractRelevantFields)
        .collectList()
        .map(
            testCases -> {
              ObjectNode resultNode = objectMapper.createObjectNode();
              resultNode.set(TEST_CASES, objectMapper.valueToTree(testCases));
              return resultNode;
            });
  }

  /** Gets TestRigor documentation using vector search. */
  private Mono<JsonNode> getTestRigorDocumentation(
      List<UATConfiguratorResponseDTO> configurations) {
    return Flux.fromIterable(configurations)
        .flatMap(
            config -> {
              String context = buildTestContext(config);
              return aiService.getEmbeddings(context);
            })
        .collectList()
        .flatMap(
            embeddings ->
                Flux.fromIterable(embeddings)
                    .flatMap(
                        embedding ->
                            artifactService.performVectorSearch(
                                embedding, ARTIFACT_SEARCH_LIMIT, TEST_RIGOR_DOC))
                    .map(Artifact::getText)
                    .collectList()
                    .map(
                        texts -> {
                          ObjectNode resultNode = objectMapper.createObjectNode();
                          resultNode.put(DOCUMENTATION, String.join("\n", texts));
                          return resultNode;
                        }));
  }

  /** Gets TestRigor error documentation using vector search. */
  private Mono<JsonNode> getTestRigorErrorFixData(List<UATConfiguratorResponseDTO> configurations) {
    return Flux.fromIterable(configurations)
        .flatMap(
            config -> {
              String context = buildTestContext(config);
              return aiService.getEmbeddings(context);
            })
        .collectList()
        .flatMap(
            embeddings ->
                Flux.fromIterable(embeddings)
                    .flatMap(
                        embedding ->
                            artifactService.performVectorSearch(
                                embedding, ARTIFACT_SEARCH_LIMIT, TESTRIGOR_ERROR_DOC))
                    .map(Artifact::getText)
                    .collectList()
                    .map(
                        texts -> {
                          ObjectNode resultNode = objectMapper.createObjectNode();
                          resultNode.put(ERROR_DOC, String.join("\n", texts));
                          return resultNode;
                        }));
  }

  /** Gets BRD fields and portal configuration data. */
  private Mono<JsonNode> getBrdAndPortalData(
      List<UATConfiguratorResponseDTO> configurations, String brdId, PortalTypes uatType) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new RuntimeException(BRD_NOT_FOUND + brdId)))
        .flatMap(
            brd ->
                Flux.fromIterable(configurations)
                    .flatMap(config -> findMatchingFieldsUsingAI(brd, config))
                    .collectList()
                    .flatMap(brdFields -> getPortalData(brdId, uatType, brdFields))
                    .map(
                        dataNode -> {
                          ObjectNode resultNode = objectMapper.createObjectNode();
                          resultNode.set(DATA, dataNode);
                          return resultNode;
                        }));
  }

  /** Gets portal configuration data and combines with BRD fields. */
  private Mono<JsonNode> getPortalData(
      String brdId, PortalTypes uatType, List<JsonNode> brdFields) {
    return portalConfigurationService
        .getPortalConfigurationByIdAndType(brdId, uatType)
        .map(
            portalConfig -> {
              ObjectNode dataNode = objectMapper.createObjectNode();
              brdFields.forEach(field -> dataNode.setAll((ObjectNode) field));
              dataNode.put("username", portalConfig.getUsername());
              dataNode.put("password", portalConfig.getPassword());
              return dataNode;
            });
  }

  /** Builds test context from configuration. */
  private String buildTestContext(UATConfiguratorResponseDTO config) {
    return String.format("Position: %s, Scenario: %s", config.getPosition(), config.getScenario());
  }

  /** Extracts relevant fields from test case. */
  private JsonNode extractRelevantFields(UATTestCaseDTO testCase) {
    ObjectNode node = objectMapper.createObjectNode();
    node.set("position", objectMapper.valueToTree(testCase.getPosition()));
    node.set("scenario", objectMapper.valueToTree(testCase.getScenario()));
    node.set(ANSWER_FIELD, objectMapper.valueToTree(testCase.getAnswer()));
    return node;
  }

  @Override
  public Flux<UATTestCaseRequestResponseDTO> retestFeatures(
      String brdId, List<String> configurationNames, PortalTypes uatType) {
    log.info(
        "Retesting features for BRD: {}, configurations: {}, type: {}",
        brdId,
        configurationNames,
        uatType);

    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new RuntimeException(BRD_NOT_FOUND + brdId)))
        .flatMapMany(
            brd -> processConfigurationsAndGenerateTestCases(brd, configurationNames, uatType));
  }

  private Flux<JsonNode> findMatchingFieldsUsingAI(BRD brd, UATConfiguratorResponseDTO config) {
    if (config == null || config.getFields() == null || config.getFields().isEmpty()) {
      log.warn("No fields to process in configuration");
      return Flux.empty();
    }

    JsonNode brdNode = objectMapper.valueToTree(brd);
    return Flux.fromIterable(config.getFields())
        .flatMap(fieldName -> processFieldMatching(fieldName, brd, brdNode))
        .onErrorResume(
            e -> {
              log.error("Error in findMatchingFieldsUsingAI: {}", e.getMessage());
              return Flux.empty();
            });
  }

  private Mono<JsonNode> processFieldMatching(String fieldName, BRD brd, JsonNode brdNode) {
    if (fieldName == null || fieldName.trim().isEmpty()) {
      return Mono.empty();
    }

    String question = String.format(fieldMatchingPrompt, fieldName);
    String context = createContextFromBrdSections(brd);

    return aiService
        .generateAnswer(question, context, "uat_field_search")
        .flatMap(answer -> Mono.justOrEmpty(extractAndMapField(answer, fieldName, brdNode)))
        .onErrorResume(
            e -> {
              log.error("Error processing field {}: {}", fieldName, e.getMessage());
              return Mono.empty();
            });
  }

  private JsonNode extractAndMapField(String answer, String fieldName, JsonNode brdNode) {
    if (answer == null || answer.trim().isEmpty()) {
      log.warn("Empty answer received for field: {}", fieldName);
      return null;
    }

    try {
      // Try to find a JSON object in the response
      String jsonStr = extractJsonFromResponse(answer);
      if (jsonStr == null) {
        log.warn("No valid JSON found in response for field: {}", fieldName);
        return null;
      }

      JsonNode response = objectMapper.readTree(jsonStr);
      if (!response.has("matchedField")) {
        log.warn("Response missing matchedField for field: {}", fieldName);
        return null;
      }

      String matchedField = response.get("matchedField").asText();
      JsonNode value = findValueInBrd(brdNode, matchedField);

      if (value != null) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put(fieldName.toLowerCase(), value.asText());
        log.debug("Field mapping: {} -> {} = {}", fieldName, matchedField, value.asText());
        return result;
      } else {
        log.warn("No value found in BRD for matched field: {} -> {}", fieldName, matchedField);
      }
    } catch (Exception e) {
      log.error("Error parsing AI response for field {}: {}", fieldName, e.getMessage());
    }
    return null;
  }

  private String extractJsonFromResponse(String response) {
    try {
      Matcher matcher = JSON_PATTERN.matcher(response);
      if (matcher.find()) {
        String jsonStr = matcher.group();
        // Validate if it's a complete JSON object
        objectMapper.readTree(jsonStr); // This will throw if JSON is invalid
        return jsonStr;
      }
    } catch (Exception e) {
      log.debug("Invalid JSON in response: {}", e.getMessage());
    }

    // If no valid JSON found with regex, try to fix common issues
    try {
      String cleaned = response.trim();
      if (!cleaned.endsWith("}")) {
        cleaned = cleaned + "}";
      }
      // Validate the fixed JSON
      objectMapper.readTree(cleaned);
      return cleaned;
    } catch (Exception e) {
      log.debug("Could not fix JSON: {}", e.getMessage());
    }

    return null;
  }

  private JsonNode findValueInBrd(JsonNode brdNode, String fieldName) {
    return getBrdSections().stream()
        .map(brdNode::get)
        .filter(sectionNode -> sectionNode != null && !sectionNode.isEmpty())
        .map(sectionNode -> findFieldInSection(sectionNode, fieldName))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private JsonNode findFieldInSection(JsonNode sectionNode, String fieldName) {
    if (sectionNode.has(fieldName)) {
      return sectionNode.get(fieldName);
    }
    for (JsonNode node : sectionNode) {
      if (node.isObject() && node.has(fieldName)) {
        return node.get(fieldName);
      }
    }
    return null;
  }

  private String createContextFromBrdSections(BRD brd) {
    JsonNode brdNode = objectMapper.valueToTree(brd);
    return getBrdSections().stream()
        .map(section -> formatSectionContext(section, brdNode.get(section)))
        .filter(Objects::nonNull)
        .collect(Collectors.joining("%n", "%nBRD Sections:%n", ""));
  }

  private String formatSectionContext(String section, JsonNode sectionNode) {
    if (sectionNode != null && !sectionNode.isEmpty()) {
      return String.format("%nSection: %s%nStructure: %s", section, sectionNode);
    }
    return null;
  }

  private List<String> getBrdSections() {
    return Arrays.stream(BRD.class.getDeclaredFields()).map(Field::getName).toList();
  }
}
