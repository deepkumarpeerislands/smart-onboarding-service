package com.aci.smart_onboarding.service.implementation;

import com.aci.ai.services.IContextProvider;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.exception.AIServiceException;
import com.aci.smart_onboarding.exception.ResourceNotFoundException;
import com.aci.smart_onboarding.exception.TokenLimitExceededException;
import com.aci.smart_onboarding.model.Artifact;
import com.aci.smart_onboarding.model.Walletron;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.WalletronRepository;
import com.aci.smart_onboarding.service.IAIService;
import com.aci.smart_onboarding.service.IArtifactService;
import com.aci.smart_onboarding.service.IAssistantService;
import com.aci.smart_onboarding.service.IBrdTemplateService;
import com.aci.smart_onboarding.util.walletron.TargetedCommunication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Service implementation for handling AI assistant interactions. Provides functionality for
 * template analysis, decision making, and general chat.
 */
@Slf4j
@Service
public class AssistantService implements IAssistantService, DisposableBean {

  private static final String DEFAULT_CHAT_CONTEXT = "AI assistant to help you with your queries";
  private static final String RESULTS = "results";
  private static final String EXPLANATION = "explanation";
    private static final String SITE_CONFIGURATION = "siteConfiguration";
  private static final String NOTIFICATIONS_OPTIONS = "notificationsOptions";
  private static final String AGENT_PORTAL = "agentPortal";
  private static final String DATA_EXCHANGE = "dataExchange";
  private static final String ENROLLMENT_STRATEGY = "enrollmentStrategy";
  private static final String ENROLLMENT_URLS = "enrollmentUrls";
  private static final String TARGETED_COMMUNICATION = "targetedCommunication";
  private static final String ACI_CASH = "aciCash";
  private static final String APPROVALS = "approvals";
    private static final int VECTOR_SEARCH_LIMIT = 5;
  private static final String SSD_SUMMARY =
      """
      Please provide short, clear details under each of the following headings.
      Each section must begin with a bold heading, followed by its relevant content, which should be crisp and short as points.
      Only include applicable sections—omit any unrelated or empty ones.
      Do not display items side by side.
      At the end, add a section titled "Missing Information" with this sentence:
      'The following sections had no available data: [list missing section names separated by commas]'.

      Headings to cover:
      Payment Channels
      Payment Methods
      Additional Service
      Fees
      Transaction Fees
      Other Processing Fees
      Additional Fees
      Funds Management
      Assumptions
      Miscellaneous
      Not in Scope
      Implementation Timeline and Dependency""";

  private static final String CONTRACT_SUMMARY =
      """
      Please provide clear and concise information under each of the following headings.
      Each section must start with a bold heading followed by its relevant content, which should be crisp and short as points without explanation.If you can make sub points make it
      Include only applicable sections—omit any that are not relevant or have no data.
      Do not display multiple sections side by side.
      At the end, add a section titled "Missing Information" with the sentence:
      'The following sections had no available data: [list missing section names separated by commas]'.

      Headings to cover:
      Provision of Application Services
      Schedule Term
      Implementation Fee
      Services Provided
      Dates""";

  @Value("${ai.vector.search.limit:10}")
  private int vectorSearchLimit;

  private static final String NO_ARTIFACTS_ERROR =
      "Error: No relevant artifacts found for your question.";
  private static final String TEMPLATE_FIELD1 = "templateTypes";
  private static final String TEMPLATE_FIELD2 = "summary";

  private final IAIService aiService;
  private final IContextProvider contextProvider;
  private final IArtifactService artifactService;
  private final IBrdTemplateService brdTemplateService;
  private final BRDRepository brdRepository;
  private final WalletronRepository walletronRepository;
  private final ModelMapper modelMapper;
  private final ObjectMapper objectMapper;
  private final Scheduler boundedElasticScheduler;

  public AssistantService(
      IAIService aiService,
      IArtifactService artifactService,
      IContextProvider contextProvider,
      IBrdTemplateService brdTemplateService,
      BRDRepository brdRepository,
      WalletronRepository walletronRepository,
      ModelMapper dtoModelMapper) {
    this.aiService = aiService;
    this.artifactService = artifactService;
    this.contextProvider = contextProvider;
    this.brdTemplateService = brdTemplateService;
    this.brdRepository = brdRepository;
    this.walletronRepository = walletronRepository;
    this.modelMapper = dtoModelMapper;

    // Configure custom mapping for TargetedCommunication
    Converter<ArrayList<?>, TargetedCommunication> targetedCommunicationConverter =
        ctx -> {
          if (ctx.getSource() instanceof ArrayList) {
            ArrayList<?> list = ctx.getSource();
            if (!list.isEmpty()) {
              // Create a new instance with the data
              return new TargetedCommunication();
            }
          }
          return null;
        };

    this.modelMapper.addConverter(targetedCommunicationConverter);

    this.objectMapper =
        new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    this.boundedElasticScheduler = Schedulers.boundedElastic();
  }

  @Override
  public Flux<String> askStreamingAssistant(
      String question, ContextName contextName, String documentName) {
    contextProvider.setContextName(contextName.name());

    if (contextName == ContextName.SUMMARY) {
      return handleSummaryStreaming(question, documentName);
    }

    return aiService.generateAnswerAsStream(
        question, DEFAULT_CHAT_CONTEXT, ContextName.CHAT.getPrompt());
  }

  @Override
  public Mono<String> askAssistant(String question, ContextName contextName, String documentName) {
    contextProvider.setContextName(contextName.name());

    return switch (contextName) {
      case TEMPLATE -> handleTemplateAnalysis(documentName);
      case DECISION -> handleDecisionMaking(question, documentName);
      default -> handleChat(question);
    };
  }

  /** Handles streaming responses for summary context */
  private Flux<String> handleSummaryStreaming(String question, String documentName) {
    return artifactService
        .findByDocumentName(documentName)
        .flatMapMany(
            textChunks -> {
              if (textChunks.isEmpty()) {
                return Flux.just("Error: No artifacts found for the given documentId.");
              }
              String additionalContext =
                  documentName.contains("SSD")
                      ? String.join("\n", SSD_SUMMARY, String.join(" ", textChunks))
                      : String.join(" ", CONTRACT_SUMMARY, String.join(" ", textChunks));

              return aiService.generateAnswerAsStream(
                  question, additionalContext, ContextName.SUMMARY.getPrompt());
            });
  }

  /** Handles template analysis requests */
  private Mono<String> handleTemplateAnalysis(String documentName) {
    return Mono.zip(convertBRDTemplateToJson(), getContextForTemplate(documentName))
        .flatMap(
            tuple ->
                aiService.generateAnswer(
                    tuple.getT1(), tuple.getT2(), ContextName.TEMPLATE.getPrompt()));
  }

  /** Handles decision-making requests using vector search */
  private Mono<String> handleDecisionMaking(String question, String documentName) {
    return aiService
        .getEmbeddings(question)
        .flatMap(
            embeddingList ->
                artifactService
                    .performVectorSearch(embeddingList, vectorSearchLimit, documentName)
                    .collectList()
                    .flatMap(
                        artifacts -> {
                          if (artifacts.isEmpty()) {
                            return Mono.just(NO_ARTIFACTS_ERROR);
                          }
                          String artifactContext = buildArtifactContext(artifacts);
                          return aiService.generateAnswer(
                              question, artifactContext, ContextName.DECISION.getPrompt());
                        }));
  }

  /** Handles general chat requests */
  private Mono<String> handleChat(String question) {
    return aiService.generateAnswer(question, DEFAULT_CHAT_CONTEXT, ContextName.CHAT.getPrompt());
  }

  /** Builds context string from artifacts */
  private String buildArtifactContext(List<Artifact> artifacts) {
    return artifacts.stream().map(Artifact::getText).reduce((a, b) -> a + "\n\n" + b).orElse("");
  }

  /** Converts BRD templates to JSON format */
  Mono<String> convertBRDTemplateToJson() {
    return brdTemplateService
        .getAllTemplates()
        .flatMap(this::processTemplateResponse)
        .onErrorResume(
            e -> {
              log.error("Error processing template data: {}", e.getMessage(), e);
              return Mono.just("[]");
            });
  }

  /**
   * Processes the template response and converts it to JSON.
   *
   * @param response The API response containing template data
   * @return Mono containing the processed JSON string
   */
  private Mono<String> processTemplateResponse(ResponseEntity<Api<List<BrdTemplateRes>>> response) {
    try {
      JsonNode responseNode = objectMapper.convertValue(response.getBody(), JsonNode.class);
      return processResponseNode(responseNode);
    } catch (Exception e) {
      return Mono.error(new RuntimeException("Failed to process template data", e));
    }
  }

  private Mono<String> processResponseNode(JsonNode responseNode) {
    if (!responseNode.has("data") || responseNode.get("data").isNull()) {
      return Mono.just("[]");
    }
    JsonNode dataNode = responseNode.get("data");
    if (!dataNode.isArray()) {
      return Mono.just("[]");
    }
    return processTemplateArray(dataNode);
  }

  /** Processes an array of templates and converts them to JSON */
  private Mono<String> processTemplateArray(JsonNode dataNode) {
    return Flux.fromIterable(dataNode::elements)
        .map(this::filterTemplateFields)
        .collectList()
        .flatMap(
            list -> {
              try {
                return Mono.just(objectMapper.writeValueAsString(list));
              } catch (JsonProcessingException e) {
                return Mono.error(new RuntimeException("Failed to serialize templates", e));
              }
            });
  }

  /** Filters and extracts relevant fields from a template */
  private ObjectNode filterTemplateFields(JsonNode template) {
    ObjectNode filteredTemplate = objectMapper.createObjectNode();
    if (template.has(TEMPLATE_FIELD1)) {
      filteredTemplate.set(TEMPLATE_FIELD1, template.get(TEMPLATE_FIELD1));
    }
    if (template.has(TEMPLATE_FIELD2)) {
      filteredTemplate.set(TEMPLATE_FIELD2, template.get(TEMPLATE_FIELD2));
    }
    return filteredTemplate;
  }

  /** Gets context for template analysis */
  private Mono<String> getContextForTemplate(String documentName) {
    String question =
        "Analyze this document and Focus on identifying the business sector, key requirements, and specific industry needs. "
            + "Structure the summary to highlight sector-specific characteristics and requirements.";

    return artifactService
        .findByDocumentName(documentName)
        .flatMap(
            artifacts -> {
              if (artifacts.isEmpty()) {
                return Mono.just("Error: No artifacts found for the given documentId.");
              }
              String additionalContext = String.join(" ", artifacts);
              return aiService.generateAnswer(
                  question, additionalContext, ContextName.SUMMARY.getPrompt());
            });
  }

  @Override
  public Mono<JsonNode> prefillBRDProcessJson(
      JsonNode sections,
      List<String> documentNames,
      ContextName contextName,
      String additionalContext) {
    if (sections == null) {
      return Mono.empty();
    }
    if (!sections.isObject()) {
      return Mono.just(sections);
    }

    ObjectNode updatedJson = sections.deepCopy();
    List<String> fieldNames = new ArrayList<>();
    sections.fieldNames().forEachRemaining(fieldNames::add);

    return Flux.fromIterable(fieldNames)
        .flatMap(
            section -> {
              JsonNode sectionNode = updatedJson.get(section);
              return processSection(
                      section, sectionNode, documentNames, contextName, additionalContext)
                  .map(
                      updatedSection -> {
                        updatedJson.set(section, updatedSection);
                        return (JsonNode) updatedJson;
                      });
            })
        .last()
        .map(JsonNode.class::cast)
        .subscribeOn(boundedElasticScheduler);
  }

  private Mono<JsonNode> processSection(
      String section,
      JsonNode sectionNode,
      List<String> documentNames,
      ContextName contextName,
      String additionalContext) {
    if (sectionNode == null || !sectionNode.isObject()) {
      return Mono.just(sectionNode);
    }

    String sectionAsString = sectionNode.toString();
    log.debug("Processing section: {}", section);

    return aiService
        .getEmbeddings(sectionAsString)
        .flatMap(
            embeddingList ->
                buildContextFromDocuments(
                    embeddingList,
                    documentNames,
                    sectionNode,
                    sectionAsString,
                    contextName,
                    additionalContext))
        .subscribeOn(boundedElasticScheduler);
  }

  private Mono<JsonNode> buildContextFromDocuments(
      List<Double> embeddingList,
      List<String> documentNames,
      JsonNode sectionNode,
      String sectionAsString,
      ContextName contextName,
      String additionalContext) {

    return Flux.fromIterable(documentNames)
        .flatMap(documentName -> fetchRelevantContextFromDocument(embeddingList, documentName))
        .collectList()
        .flatMap(
            contexts ->
                processContextsAndGenerateAnswer(
                    contexts, sectionNode, sectionAsString, contextName, additionalContext));
  }

  private Mono<String> fetchRelevantContextFromDocument(
      List<Double> embeddingList, String documentName) {
    return artifactService
        .performVectorSearch(embeddingList, vectorSearchLimit, documentName)
        .collectList()
        .flatMap(
            artifacts -> {
              if (artifacts.isEmpty()) {
                return Mono.empty();
              }
              String artifactContext = buildArtifactContext(artifacts);
              return Mono.just(artifactContext);
            });
  }

  private Mono<JsonNode> processContextsAndGenerateAnswer(
      List<String> contexts,
      JsonNode sectionNode,
      String sectionAsString,
      ContextName contextName,
      String additionalContext) {

    if (contexts.isEmpty() && (additionalContext == null || additionalContext.isEmpty())) {
      return Mono.just(sectionNode);
    }

    String combinedContext = buildCombinedContext(contexts, additionalContext);

    return aiService
        .generateAnswer(sectionAsString, combinedContext, contextName.getPrompt())
        .map(this::convertStringToJson);
  }

  private String buildCombinedContext(List<String> contexts, String additionalContext) {
    StringBuilder contextBuilder = new StringBuilder();

    // Add artifact contexts if available
    if (!contexts.isEmpty()) {
      contextBuilder.append(String.join("\n\n", contexts));
    }

    // Add additional context if available
    if (additionalContext != null && !additionalContext.isEmpty()) {
      if (contextBuilder.length() > 0) {
        contextBuilder.append("\n\n");
      }
      contextBuilder.append(additionalContext);
    }

    return contextBuilder.toString();
  }

  private JsonNode convertStringToJson(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      throw new AIServiceException("Error parsing AI response JSON");
    }
  }

  @Override
  public Flux<BRDSummaryResponse> generateBRDSummary(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new AIServiceException("BRD not found")))
        .map(brd -> modelMapper.map(brd, BRDSummaryRequest.class))
        .flatMapMany(
            brdRequest -> {
              String context =
                  "Generate a comprehensive summary by taking important notes as bullet points.";

              try {
                String brdJson = objectMapper.writeValueAsString(brdRequest);

                return aiService
                    .generateAnswerAsStream(brdJson, context, ContextName.SUMMARY.getPrompt())
                    .map(
                        summaryPart -> {
                          BRDSummaryResponse response = new BRDSummaryResponse();
                          response.setBrdId(brdId);
                          response.setSummary(summaryPart);
                          response.setStatus("SUCCESS");
                          return response;
                        });
              } catch (JsonProcessingException e) {
                throw new AIServiceException(
                    "Error converting BRDResponse to JSON: " + e.getMessage());
              } catch (TokenLimitExceededException e) { // Handle token limit exception
                if (brdRequest.getAgentPortal() != null) {
                  brdRequest.getAgentPortal().setBillSummaryItems(null);
                  brdRequest.getAgentPortal().setBillSummaryDetail(null);
                  brdRequest.getAgentPortal().setAgentPortal(null);

                  try {
                    String cleanedJson = objectMapper.writeValueAsString(brdRequest);
                    return aiService
                        .generateAnswerAsStream(
                            cleanedJson, context, ContextName.SUMMARY.getPrompt())
                        .map(
                            summaryPart -> {
                              BRDSummaryResponse response = new BRDSummaryResponse();
                              response.setBrdId(brdId);
                              response.setSummary(summaryPart);
                              response.setStatus("SUCCESS");
                              return response;
                            });
                  } catch (JsonProcessingException ex) {
                    return Flux.error(
                        new AIServiceException(
                            "Error converting cleaned BRDResponse to JSON: " + ex.getMessage()));
                  }
                }
                BRDSummaryResponse response = new BRDSummaryResponse();
                response.setBrdId(brdId);
                response.setSummary("Token limit exceeded. Fields set to null.");
                response.setStatus("FAILED");
                return Flux.just(response);
              }
            });
  }

  @Override
  public void destroy() {
    log.info("Cleaning up resources in AssistantService");
    try {
      boundedElasticScheduler.dispose();
    } catch (Exception e) {
      log.error("Error while cleaning up resources", e);
    }
  }

  // Backward compatibility method
  @Override
  public Mono<JsonNode> prefillBRDProcessJson(JsonNode sections, List<String> documentNames) {
    return prefillBRDProcessJson(sections, documentNames, ContextName.PREFILL, null);
  }

  public Mono<List<GuidanceData>> findSemanticMatches(List<BrdRules> rules) {
    // Get a unique list of rule names from BrdRules, filtering out null values
    List<String> uniqueRuleNames =
        rules.stream().map(BrdRules::getRuleName).filter(Objects::nonNull).distinct().toList();

    log.debug(
        "Processing {} unique rule names from {} rules", uniqueRuleNames.size(), rules.size());

    // Define batch size
    final int batchSize = 20;

    // Create a list of Monos, each processing one batch
    List<Mono<List<GuidanceData>>> batchMonos = new ArrayList<>();

    // Process each batch of rules
    for (int i = 0; i < uniqueRuleNames.size(); i += batchSize) {
      final int startIndex = i;
      int endIndex = Math.min(i + batchSize, uniqueRuleNames.size());

      // Extract batch of rule names
      List<String> batchRuleNames = uniqueRuleNames.subList(startIndex, endIndex);

      // Process this batch
      Mono<List<GuidanceData>> batchMono = processBatch(batchRuleNames);
      batchMonos.add(batchMono);
    }

    // Combine all batch results
    return Flux.concat(batchMonos)
        .flatMap(Flux::fromIterable)
        .collectList()
        .doOnNext(
            allGuidanceData ->
                log.info(
                    "Processed {} rules into {} guidance data items",
                    uniqueRuleNames.size(),
                    allGuidanceData.size()));
  }

  private Mono<List<GuidanceData>> processBatch(List<String> batchRuleNames) {
    return aiService
        .getEmbeddingsForBatch(batchRuleNames)
        .flatMap(embeddingsList -> processEmbeddings(embeddingsList, batchRuleNames))
        .flatMap(this::generateGuidanceData);
  }

  private Mono<List<RuleArtifactsDTO>> processEmbeddings(
      List<List<Double>> embeddingsList, List<String> batchRuleNames) {
    List<Mono<RuleArtifactsDTO>> artifactDTOMonos = new ArrayList<>();

    for (int j = 0; j < embeddingsList.size(); j++) {
      List<Double> embedding = embeddingsList.get(j);
      String ruleName = batchRuleNames.get(j);

      Mono<RuleArtifactsDTO> dtoMono = createRuleArtifactsDTO(embedding, ruleName);
      artifactDTOMonos.add(dtoMono);
    }

    return Flux.fromIterable(artifactDTOMonos).flatMap(mono -> mono).collectList();
  }

  private Mono<RuleArtifactsDTO> createRuleArtifactsDTO(List<Double> embedding, String ruleName) {
    return artifactService
        .performVectorSearch(embedding, VECTOR_SEARCH_LIMIT, "BRD")
        .map(Artifact::getText)
        .collectList()
        .map(artifactTexts -> new RuleArtifactsDTO(ruleName, artifactTexts));
  }

  private Mono<List<GuidanceData>> generateGuidanceData(List<RuleArtifactsDTO> dtoList) {
    try {
      String jsonResult = objectMapper.writeValueAsString(dtoList);
      String question =
          "Make sure you are mapping the rule to the correct key. Don't provide any extra text or explanation.";

      return aiService
          .generateAnswer(question, jsonResult, ContextName.MAPPED_RULE.getPrompt())
          .map(this::parseAIResponseToGuidanceData)
          .onErrorResume(
              e -> {
                log.error("Error in AI processing: {}", e.getMessage());
                return Mono.just(new ArrayList<>());
              });
    } catch (JsonProcessingException e) {
      log.error("Error converting batch result to JSON: {}", e.getMessage());
      return Mono.just(new ArrayList<>());
    }
  }

  private List<GuidanceData> parseAIResponseToGuidanceData(String aiResponse) {
    if (isEmptyOrNullResponse(aiResponse)) {
      return new ArrayList<>();
    }

    try {
      JsonNode rootNode = objectMapper.readTree(aiResponse);
      return processRootNode(rootNode);
    } catch (Exception e) {
      log.error("Error parsing AI response: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  private boolean isEmptyOrNullResponse(String response) {
    if (response == null || response.trim().isEmpty()) {
      log.error("AI response is empty or null");
      return true;
    }
    return false;
  }

  private List<GuidanceData> processRootNode(JsonNode rootNode) {
    List<GuidanceData> guidanceDataList = new ArrayList<>();

    if (rootNode.isArray()) {
      processArrayResponse(rootNode, guidanceDataList);
    } else if (rootNode.has(RESULTS) && rootNode.get(RESULTS).isArray()) {
      processResultsResponse(rootNode.get(RESULTS), guidanceDataList);
    } else {
      log.error("Unexpected AI response format");
    }

    log.info("Total guidanceDataList size: {}", guidanceDataList.size());
    return guidanceDataList;
  }

  private void processArrayResponse(JsonNode arrayNode, List<GuidanceData> guidanceDataList) {
    for (JsonNode itemNode : arrayNode) {
      GuidanceData guidanceData = new GuidanceData();
      extractStandardFields(itemNode, guidanceData);
      guidanceDataList.add(guidanceData);
    }
  }

  private void processResultsResponse(JsonNode resultsNode, List<GuidanceData> guidanceDataList) {
    for (JsonNode itemNode : resultsNode) {
      GuidanceData guidanceData = new GuidanceData();
      extractResultFields(itemNode, guidanceData);
      guidanceDataList.add(guidanceData);
    }
  }

  private void extractStandardFields(JsonNode itemNode, GuidanceData guidanceData) {
    if (itemNode.has("ruleName")) {
      guidanceData.setRuleName(itemNode.get("ruleName").asText());
    }

    if (itemNode.has("mappingKey")) {
      guidanceData.setMappingKey(itemNode.get("mappingKey").asText());
    }

    if (itemNode.has("similarity")) {
      guidanceData.setSimilarity(String.valueOf(itemNode.get("similarity").asInt()));
    }

    if (itemNode.has(EXPLANATION)) {
      guidanceData.setExplanation(itemNode.get(EXPLANATION).asText());
    }
  }

  private void extractResultFields(JsonNode itemNode, GuidanceData guidanceData) {
    if (itemNode.has("rule")) {
      guidanceData.setRuleName(itemNode.get("rule").asText());
    }

    if (itemNode.has("field")) {
      guidanceData.setMappingKey(itemNode.get("field").asText());
    }

    if (itemNode.has("isRelevant")) {
      boolean isRelevant = itemNode.get("isRelevant").asBoolean();
      guidanceData.setSimilarity(isRelevant ? "100" : "0");
    }

    if (itemNode.has(EXPLANATION)) {
      guidanceData.setExplanation(itemNode.get(EXPLANATION).asText());
    }
  }

  @Override
  public Mono<JsonNode> generateWalletronSummary(String brdId) {
    return walletronRepository
        .findByBrdId(brdId)
        .flatMap(this::processWalletronData)
        .switchIfEmpty(
            Mono.error(
                new ResourceNotFoundException(
                    "Walletron configuration not found for BRD ID: " + brdId)));
  }

  private Mono<JsonNode> processWalletronData(Walletron walletron) {
    try {
      Map<String, Object> sections = createWalletronSections(walletron);
      String walletronJson = objectMapper.writeValueAsString(sections);
      String additionalContext =
          "Don't provide any text or explanation, just return the json don't have json in markdown format";

      return aiService
          .generateAnswer(
              walletronJson, additionalContext, ContextName.WALLETRON_SUMMARY.getPrompt())
          .map(this::parseJsonResponse);
    } catch (JsonProcessingException e) {
      return Mono.error(
          new AIServiceException("Error converting sections to JSON: " + e.getMessage()));
    }
  }

  private Map<String, Object> createWalletronSections(Walletron walletron) {
    Map<String, Object> sections = new HashMap<>();
    sections.put(SITE_CONFIGURATION, walletron.getSiteConfiguration());
    sections.put(NOTIFICATIONS_OPTIONS, walletron.getNotificationsOptions());
    sections.put(AGENT_PORTAL, walletron.getAciWalletronAgentPortal());
    sections.put(DATA_EXCHANGE, walletron.getAciWalletronDataExchange());
    sections.put(ENROLLMENT_STRATEGY, walletron.getAciWalletronEnrollmentStrategy());
    sections.put(ENROLLMENT_URLS, walletron.getEnrollmentUrl());
    sections.put(TARGETED_COMMUNICATION, walletron.getTargetedCommunication());
    sections.put(ACI_CASH, walletron.getAciCash());
    sections.put(APPROVALS, walletron.getWalletronApprovals());
    return sections;
  }

  private JsonNode parseJsonResponse(String response) {
    try {
      return objectMapper.readTree(response);
    } catch (JsonProcessingException e) {
      throw new AIServiceException("Error parsing AI response to JSON: " + e.getMessage());
    }
  }
}
