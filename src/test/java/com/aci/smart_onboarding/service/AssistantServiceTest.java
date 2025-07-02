package com.aci.smart_onboarding.service;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.ai.services.IContextProvider;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.exception.AIServiceException;
import com.aci.smart_onboarding.exception.TokenLimitExceededException;
import com.aci.smart_onboarding.model.*;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.WalletronRepository;
import com.aci.smart_onboarding.service.implementation.AssistantService;
import com.aci.smart_onboarding.util.brd_form.AgentPortalConfig;
import com.aci.smart_onboarding.util.walletron.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantServiceTest {

  private static final String DEFAULT_CHAT_CONTEXT = "AI assistant to help you with your queries";

  @Mock private IAIService aiService;

  @Mock private IArtifactService artifactService;

  @Mock private IContextProvider contextProvider;

  @Mock private IBrdTemplateService brdTemplateService;

  @Mock private BRDRepository brdRepository;

  @Mock private WalletronRepository walletronRepository;

  @Mock private ModelMapper modelMapper;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private AssistantService assistantService;

  @BeforeEach
  void setUp() {
    ObjectNode mockObjectNode = new ObjectMapper().createObjectNode();
    given(objectMapper.createObjectNode()).willReturn(mockObjectNode);
  }

  @Test
  void askStreamingAssistant_WithChatContext_ShouldReturnAnswerStream() {
    // Given
    String question = "test question";
    String documentName = "test-doc";
    String additionalContext = "AI assistant to help you with your queries";

    when(aiService.generateAnswerAsStream(question, additionalContext, "chat"))
        .thenReturn(Flux.just("Answer", "stream", "response"));

    // When
    Flux<String> result =
        assistantService.askStreamingAssistant(question, ContextName.CHAT, documentName);

    // Then
    StepVerifier.create(result)
        .expectNext("Answer")
        .expectNext("stream")
        .expectNext("response")
        .verifyComplete();

    verify(contextProvider).setContextName(ContextName.CHAT.name());
  }

  @Test
  void askStreamingAssistant_WithSummaryContext_ShouldReturnSummaryStream() {
    // Given
    String question = "Can you summarize this document?";
    String documentName = "test-document-SSD";
    List<String> documentChunks =
        Arrays.asList(
            "This is the first part of the document.", "This is the second part of the document.");

    String ssdSummary =
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

    String expectedContext = String.join("\n", ssdSummary, String.join(" ", documentChunks));

    when(artifactService.findByDocumentName(documentName)).thenReturn(Mono.just(documentChunks));
    when(aiService.generateAnswerAsStream(
            question, expectedContext, ContextName.SUMMARY.getPrompt()))
        .thenReturn(Flux.just("Summary:", "Key points of the document"));

    // When
    Flux<String> result =
        assistantService.askStreamingAssistant(question, ContextName.SUMMARY, documentName);

    // Then
    StepVerifier.create(result)
        .expectNext("Summary:")
        .expectNext("Key points of the document")
        .verifyComplete();

    verify(contextProvider).setContextName(ContextName.SUMMARY.name());
    verify(artifactService).findByDocumentName(documentName);
    verify(aiService)
        .generateAnswerAsStream(question, expectedContext, ContextName.SUMMARY.getPrompt());
  }

  @Test
  void askAssistant_WithTemplateContext_ShouldReturnTemplateAnalysis() {
    // Arrange
    String templateContext = "Document analysis for financial services";
    String expectedResponse =
        "[{\"id\":\"67dbdbfc2607de76e85d0216\",\"templateTypes\":\"Consumer Finance\",\"summary\":\"Companies providing financial services directly to individual consumers\",\"percentage\":85},{\"id\":\"67dbdc282607de76e85d0217\",\"templateTypes\":\"Utility\",\"summary\":\"Entities delivering essential public services\",\"percentage\":75}]";

    BrdTemplateRes template1 = new BrdTemplateRes();
    template1.setId("67dbdbfc2607de76e85d0216");
    template1.setTemplateTypes("Consumer Finance");
    template1.setSummary("Companies providing financial services directly to individual consumers");
    template1.setClientInformation(true);
    template1.setPercentage(85);

    BrdTemplateRes template2 = new BrdTemplateRes();
    template2.setId("67dbdc282607de76e85d0217");
    template2.setTemplateTypes("Utility");
    template2.setSummary("Entities delivering essential public services");
    template2.setClientInformation(true);
    template2.setPercentage(75);

    Api<List<BrdTemplateRes>> apiResponse = new Api<>();
    apiResponse.setStatus("success");
    apiResponse.setMessage("Templates retrieved successfully");
    apiResponse.setData(Optional.of(Arrays.asList(template1, template2)));

    ResponseEntity<Api<List<BrdTemplateRes>>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdTemplateService.getAllTemplates()).thenReturn(Mono.just(responseEntity));
    when(artifactService.findByDocumentName("test-doc"))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(templateContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just(expectedResponse));

    // Act & Assert
    StepVerifier.create(
            assistantService.askAssistant("test question", ContextName.TEMPLATE, "test-doc"))
        .expectNext(expectedResponse)
        .verifyComplete();
  }

  @Test
  void askAssistant_WithChatContext_ShouldReturnChatResponse() {
    // Given
    String question = "test question";
    String documentName = "test-doc";
    String additionalContext = "AI assistant to help you with your queries";

    when(aiService.generateAnswer(question, additionalContext, "chat"))
        .thenReturn(Mono.just("Chat response"));

    // When
    Mono<String> result = assistantService.askAssistant(question, ContextName.CHAT, documentName);

    // Then
    StepVerifier.create(result).expectNext("Chat response").verifyComplete();

    verify(contextProvider).setContextName(ContextName.CHAT.name());
  }

  @Test
  void askAssistant_WithTemplateContextAndEmptyArtifacts_ShouldReturnEmptyArray() {
    // Given
    String documentName = "test-doc";

    Api<List<BrdTemplateRes>> apiResponse = new Api<>();
    apiResponse.setStatus("success");
    apiResponse.setMessage("No templates found");
    apiResponse.setData(Optional.empty());

    ResponseEntity<Api<List<BrdTemplateRes>>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdTemplateService.getAllTemplates()).thenReturn(Mono.just(responseEntity));
    when(artifactService.findByDocumentName(documentName)).thenReturn(Mono.just(List.of()));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void askAssistant_WithTemplateContextAndEmptyTemplates_ShouldReturnEmptyArray() {
    // Given
    String documentName = "test-doc";

    Api<List<BrdTemplateRes>> apiResponse = new Api<>();
    apiResponse.setStatus("success");
    apiResponse.setMessage("No templates found");
    apiResponse.setData(Optional.empty());

    ResponseEntity<Api<List<BrdTemplateRes>>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdTemplateService.getAllTemplates()).thenReturn(Mono.just(responseEntity));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just("Document analysis"));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void askAssistant_WithTemplateContextAndError_ShouldReturnEmptyArray() {
    // Given
    String documentName = "test-doc";

    when(brdTemplateService.getAllTemplates())
        .thenReturn(Mono.error(new RuntimeException("API Error")));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just("Document analysis"));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void askAssistant_WithDecisionContext_ShouldReturnDecisionResponse() {
    // Given
    String question = "What are the key requirements?";
    String documentName = "test-doc";
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);
    Artifact artifact1 = new Artifact();
    artifact1.setText("Key requirement 1");
    Artifact artifact2 = new Artifact();
    artifact2.setText("Key requirement 2");
    String expectedContext = "Key requirement 1\n\nKey requirement 2";

    // Set the vectorSearchLimit field
    ReflectionTestUtils.setField(assistantService, "vectorSearchLimit", 5);

    when(aiService.getEmbeddings(question)).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(embeddings, 5, documentName))
        .thenReturn(Flux.just(artifact1, artifact2));
    when(aiService.generateAnswer(question, expectedContext, "decision"))
        .thenReturn(Mono.just("Based on the requirements..."));

    // When
    Mono<String> result =
        assistantService.askAssistant(question, ContextName.DECISION, documentName);

    // Then
    StepVerifier.create(result).expectNext("Based on the requirements...").verifyComplete();

    // Verify all interactions
    verify(contextProvider).setContextName(ContextName.DECISION.name());
    verify(aiService).getEmbeddings(question);
    verify(artifactService).performVectorSearch(embeddings, 5, documentName);
    verify(aiService).generateAnswer(question, expectedContext, "decision");
  }

  @Test
  void askAssistant_WithDecisionContextAndNoArtifacts_ShouldReturnError() {
    // Given
    String question = "What are the key requirements?";
    String documentName = "test-doc";
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    // Set the vectorSearchLimit field
    ReflectionTestUtils.setField(assistantService, "vectorSearchLimit", 5);

    when(aiService.getEmbeddings(question)).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(embeddings, 5, documentName)).thenReturn(Flux.empty());

    // When
    Mono<String> result =
        assistantService.askAssistant(question, ContextName.DECISION, documentName);

    // Then
    StepVerifier.create(result)
        .expectNext("Error: No relevant artifacts found for your question.")
        .verifyComplete();

    // Verify all interactions
    verify(contextProvider).setContextName(ContextName.DECISION.name());
    verify(aiService).getEmbeddings(question);
    verify(artifactService).performVectorSearch(embeddings, 5, documentName);
  }

  @Test
  void handleSummaryStreaming_WithEmptyArtifacts_ReturnsError() {
    // Given
    String question = "Summarize this document";
    String documentName = "test-doc";

    when(artifactService.findByDocumentName(documentName)).thenReturn(Mono.just(List.of()));

    // When
    Flux<String> result =
        assistantService.askStreamingAssistant(question, ContextName.SUMMARY, documentName);

    // Then
    StepVerifier.create(result)
        .expectNext("Error: No artifacts found for the given documentId.")
        .verifyComplete();
  }

  @Test
  void handleSummaryStreaming_WithValidArtifacts_ReturnsSummary() {
    // Given
    String question = "Summarize this document";
    String documentName = "test-doc";
    List<String> artifacts = Arrays.asList("Document content 1", "Document content 2");

    String contractSummary =
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

    String expectedContext = String.join(" ", contractSummary, String.join(" ", artifacts));

    when(artifactService.findByDocumentName(documentName)).thenReturn(Mono.just(artifacts));
    when(aiService.generateAnswerAsStream(question, expectedContext, "summary"))
        .thenReturn(Flux.just("Summary of the document"));

    // When
    Flux<String> result =
        assistantService.askStreamingAssistant(question, ContextName.SUMMARY, documentName);

    // Then
    StepVerifier.create(result).expectNext("Summary of the document").verifyComplete();
  }

  @Test
  void handleTemplateAnalysis_WithValidInput_ReturnsTemplateAnalysis() {
    // Given
    String documentName = "test-doc";
    String documentContext = "Document analysis";
    String expectedResponse = "Template analysis result";

    when(brdTemplateService.getAllTemplates())
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "success",
                        "Templates retrieved",
                        Optional.of(List.of()),
                        Optional.empty()))));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(documentContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext(expectedResponse).verifyComplete();
  }

  @Test
  void handleTemplateAnalysis_WithEmptyTemplates_ReturnsEmptyArray() {
    // Given
    String documentName = "test-doc";
    String documentContext = "Document analysis";

    when(brdTemplateService.getAllTemplates())
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>("success", "No templates", Optional.empty(), Optional.empty()))));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(documentContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void handleTemplateAnalysis_WithNonArrayData_ReturnsEmptyArray() {
    // Given
    String documentName = "test-doc";
    String documentContext = "Document analysis";
    Api<List<BrdTemplateRes>> apiResponse = new Api<>();
    apiResponse.setStatus("success");
    apiResponse.setMessage("Templates retrieved");
    apiResponse.setData(Optional.empty());

    when(brdTemplateService.getAllTemplates())
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(documentContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void handleTemplateAnalysis_WithJsonProcessingException_ReturnsEmptyArray() {
    // Given
    String documentName = "test-doc";
    String documentContext = "Document analysis";
    Api<List<BrdTemplateRes>> apiResponse = new Api<>();
    apiResponse.setStatus("success");
    apiResponse.setMessage("Templates retrieved");
    apiResponse.setData(Optional.of(List.of()));

    when(brdTemplateService.getAllTemplates())
        .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(documentContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("invalid json response"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("invalid json response").verifyComplete();
  }

  @Test
  void handleTemplateAnalysis_WithGeneralException_ReturnsEmptyArray() {
    // Given
    String documentName = "test-doc";
    String documentContext = "Document analysis";

    when(brdTemplateService.getAllTemplates())
        .thenReturn(Mono.error(new RuntimeException("Failed to process template data")));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just(documentContext));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    // When
    Mono<String> result = assistantService.askAssistant("", ContextName.TEMPLATE, documentName);

    // Then
    StepVerifier.create(result).expectNext("[]").verifyComplete();
  }

  @Test
  void handleDecisionMaking_WithNoArtifacts_ReturnsError() {
    // Given
    String question = "What are the requirements?";
    String documentName = "test-doc";
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    // Set the vectorSearchLimit field
    ReflectionTestUtils.setField(assistantService, "vectorSearchLimit", 5);

    when(aiService.getEmbeddings(question)).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(embeddings, 5, documentName)).thenReturn(Flux.empty());

    // When
    Mono<String> result =
        assistantService.askAssistant(question, ContextName.DECISION, documentName);

    // Then
    StepVerifier.create(result)
        .expectNext("Error: No relevant artifacts found for your question.")
        .verifyComplete();

    // Verify all interactions
    verify(contextProvider).setContextName(ContextName.DECISION.name());
    verify(aiService).getEmbeddings(question);
    verify(artifactService).performVectorSearch(embeddings, 5, documentName);
  }

  @Test
  void handleDecisionMaking_WithValidArtifacts_ReturnsDecision() {
    // Given
    String question = "What are the requirements?";
    String documentName = "test-doc";
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);
    Artifact artifact = new Artifact();
    artifact.setText("Requirement 1");
    String expectedResponse = "Based on the requirements...";

    // Set the value for the injected property
    ReflectionTestUtils.setField(assistantService, "vectorSearchLimit", 5);

    when(aiService.getEmbeddings(question)).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(embeddings, 5, documentName))
        .thenReturn(Flux.just(artifact));
    when(aiService.generateAnswer(question, "Requirement 1", "decision"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<String> result =
        assistantService.askAssistant(question, ContextName.DECISION, documentName);

    // Then
    StepVerifier.create(result).expectNext(expectedResponse).verifyComplete();
  }

  @Test
  void handleChat_ReturnsChatResponse() {
    // Given
    String question = "What is ACI?";
    String expectedResponse = "ACI is a global payment solutions provider";

    when(aiService.generateAnswer(question, "AI assistant to help you with your queries", "chat"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<String> result = assistantService.askAssistant(question, ContextName.CHAT, "test-doc");

    // Then
    StepVerifier.create(result).expectNext(expectedResponse).verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithNullSections_ReturnsEmptyMono() {
    List<String> documentNames = List.of("doc1");
    StepVerifier.create(assistantService.prefillBRDProcessJson(null, documentNames))
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithNonObjectNode_ReturnsSameNode() {
    JsonNode arrayNode = new ObjectMapper().createArrayNode();
    List<String> documentNames = List.of("doc1");

    StepVerifier.create(assistantService.prefillBRDProcessJson(arrayNode, documentNames))
        .expectNext(arrayNode)
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithEmptyDocumentNames_ReturnsOriginalNode() {
    ObjectNode sections = new ObjectMapper().createObjectNode();
    sections.put("key", "value");

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, List.of()))
        .expectNext(sections)
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithServiceError_ReturnsError() {
    ObjectNode sections = new ObjectMapper().createObjectNode();
    sections.put("key", "value");
    List<String> documentNames = List.of("doc1");
    RuntimeException mockError = new RuntimeException("Service error");

    when(aiService.getEmbeddings(anyString())).thenReturn(Mono.error(mockError));
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.empty());

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectNext(sections)
        .verifyComplete();
  }

  @Test
  void generateBRDSummary_TokenLimitExceededWithRetry_Success() throws JsonProcessingException {
    String brdId = "test-brd-id";
    BRD mockBrd = new BRD();
    BRDSummaryRequest request = new BRDSummaryRequest();
    AgentPortalConfig agentPortal = new AgentPortalConfig();
    request.setAgentPortal(agentPortal);

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(modelMapper.map(mockBrd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any())).thenReturn("json");

    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenThrow(new TokenLimitExceededException("Token limit exceeded"))
        .thenReturn(Flux.just("Summary after retry"));

    StepVerifier.create(assistantService.generateBRDSummary(brdId))
        .assertNext(
            response -> {
              assertEquals(brdId, response.getBrdId());
              assertEquals("Summary after retry", response.getSummary());
              assertEquals("SUCCESS", response.getStatus());
            })
        .verifyComplete();
  }

  @Test
  void generateBRDSummary_TokenLimitExceededNoRetry_ReturnsFailedResponse()
      throws JsonProcessingException {
    String brdId = "test-brd-id";
    BRD mockBrd = new BRD();
    BRDSummaryRequest request = new BRDSummaryRequest();
    request.setAgentPortal(null);

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(modelMapper.map(mockBrd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any())).thenReturn("json");

    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenThrow(new TokenLimitExceededException("Token limit exceeded"));

    StepVerifier.create(assistantService.generateBRDSummary(brdId))
        .assertNext(
            response -> {
              assertEquals(brdId, response.getBrdId());
              assertEquals("Token limit exceeded. Fields set to null.", response.getSummary());
              assertEquals("FAILED", response.getStatus());
            })
        .verifyComplete();
  }

  @Test
  void generateBRDSummary_JsonProcessingException_ReturnsError() throws JsonProcessingException {
    String brdId = "test-brd-id";
    BRD mockBrd = new BRD();
    BRDSummaryRequest request = new BRDSummaryRequest();
    request.setAgentPortal(new AgentPortalConfig());

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(modelMapper.map(mockBrd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Error serializing") {});
    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenReturn(Flux.empty());

    StepVerifier.create(assistantService.generateBRDSummary(brdId)).verifyComplete();
  }

  @Test
  void generateBRDSummary_JsonProcessingExceptionDuringRetry_ReturnsError()
      throws JsonProcessingException {
    String brdId = "test-brd-id";
    BRD mockBrd = new BRD();
    BRDSummaryRequest request = new BRDSummaryRequest();
    AgentPortalConfig agentPortal = new AgentPortalConfig();
    request.setAgentPortal(agentPortal);

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(modelMapper.map(mockBrd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any()))
        .thenReturn("json")
        .thenThrow(new JsonProcessingException("Error serializing") {});

    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenThrow(new TokenLimitExceededException("Token limit exceeded"));

    StepVerifier.create(assistantService.generateBRDSummary(brdId))
        .expectError(TokenLimitExceededException.class)
        .verify();
  }

  @Test
  void generateBRDSummary_BrdNotFound_ReturnsError() {
    String brdId = "non-existent-brd";
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    StepVerifier.create(assistantService.generateBRDSummary(brdId))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AIServiceException
                    && throwable.getMessage().equals("BRD not found"))
        .verify();
  }

  @Test
  void askStreamingAssistant_WithNullQuestion_ReturnsError() {
    when(aiService.generateAnswerAsStream(null, DEFAULT_CHAT_CONTEXT, ContextName.CHAT.getPrompt()))
        .thenReturn(Flux.error(new IllegalArgumentException("Question cannot be null")));

    StepVerifier.create(assistantService.askStreamingAssistant(null, ContextName.CHAT, "doc"))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void askStreamingAssistant_WithNullContextName_ReturnsError() {
    assertThrows(NullPointerException.class, this::executeAskAssistantWithNullContext);
  }

  private void executeAskAssistantWithNullContext() {
    assistantService.askAssistant("question", null, "doc").block();
  }

  @Test
  void askStreamingAssistant_WithNullDocumentName_ReturnsError() {
    when(artifactService.findByDocumentName(null))
        .thenReturn(Mono.error(new IllegalArgumentException("Document name cannot be null")));

    StepVerifier.create(
            assistantService.askStreamingAssistant("question", ContextName.SUMMARY, null))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void askAssistant_WithNullQuestion_ReturnsError() {
    when(aiService.generateAnswer(null, DEFAULT_CHAT_CONTEXT, ContextName.CHAT.getPrompt()))
        .thenReturn(Mono.error(new IllegalArgumentException("Question cannot be null")));

    StepVerifier.create(assistantService.askAssistant(null, ContextName.CHAT, "doc"))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void askAssistant_WithNullContextName_ReturnsError() {
    assertThrows(NullPointerException.class, this::executeAskAssistantWithNullContext);
  }

  @Test
  void askAssistant_WithEmptyDocumentName_ReturnsError() {
    when(artifactService.findByDocumentName("")).thenReturn(Mono.just(List.of()));
    when(brdTemplateService.getAllTemplates())
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>("success", "No templates", Optional.empty(), Optional.empty()))));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    StepVerifier.create(assistantService.askAssistant("question", ContextName.TEMPLATE, ""))
        .expectNext("[]")
        .verifyComplete();
  }

  @Test
  void askAssistant_WithTemplateServiceError_ReturnsEmptyArray() {
    String documentName = "test-doc";
    RuntimeException mockError = new RuntimeException("Template service error");

    when(brdTemplateService.getAllTemplates()).thenReturn(Mono.error(mockError));
    when(artifactService.findByDocumentName(documentName))
        .thenReturn(Mono.just(List.of("Document content")));
    when(aiService.generateAnswer(anyString(), anyString(), eq("summary")))
        .thenReturn(Mono.just("Document analysis"));
    when(aiService.generateAnswer(anyString(), anyString(), eq("template")))
        .thenReturn(Mono.just("[]"));

    StepVerifier.create(assistantService.askAssistant("", ContextName.TEMPLATE, documentName))
        .expectNext("[]")
        .verifyComplete();
  }

  @Test
  void askAssistant_WithDecisionServiceError_ReturnsError() {
    String question = "test question";
    String documentName = "test-doc";
    RuntimeException mockError = new RuntimeException("AI service error");

    when(aiService.getEmbeddings(question)).thenReturn(Mono.error(mockError));

    StepVerifier.create(assistantService.askAssistant(question, ContextName.DECISION, documentName))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void askAssistant_WithChatServiceError_ReturnsError() {
    String question = "test question";
    RuntimeException mockError = new RuntimeException("Chat service error");

    when(aiService.generateAnswer(anyString(), anyString(), eq("chat")))
        .thenReturn(Mono.error(mockError));

    StepVerifier.create(assistantService.askAssistant(question, ContextName.CHAT, "doc"))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void generateBRDSummary_WithNullBrdId_ReturnsError() {
    when(brdRepository.findByBrdId(null))
        .thenReturn(Mono.error(new IllegalArgumentException("BRD ID cannot be null")));

    StepVerifier.create(assistantService.generateBRDSummary(null))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void prefillBRDProcessJson_WithEmptySections_ReturnsError() {
    ObjectNode sections = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(Collections.<String>emptyList().iterator());
    when(sections.isObject()).thenReturn(true);

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectError(NoSuchElementException.class)
        .verify();
  }

  @Test
  void prefillBRDProcessJson_WithNonObjectSectionNode_ReturnsSameNode() {
    ObjectNode sections = mock(ObjectNode.class);
    JsonNode arrayNode = mock(JsonNode.class);
    List<String> documentNames = List.of("doc1");

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(arrayNode);
    when(sections.isObject()).thenReturn(true);
    when(sections.set(Mockito.eq("section1"), any())).thenReturn(sections);

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectNext(sections)
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithNoMatchingArtifacts_ReturnsSameNode() {
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");

    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));

    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.empty());

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectNext(sections)
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithMatchingArtifacts_ReturnsUpdatedNode()
      throws JsonProcessingException {
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    Artifact artifact = new Artifact();
    artifact.setText("Matching content");

    ObjectNode updatedNode = mock(ObjectNode.class);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");
    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));

    // ✅ Loosen matching here to avoid NullPointerException
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.just(artifact));

    when(aiService.generateAnswer(
            Mockito.eq("{}"), Mockito.anyString(), Mockito.eq(ContextName.PREFILL.getPrompt())))
        .thenReturn(Mono.just("{\"updatedField\":\"updatedValue\"}"));
    try {
      when(objectMapper.readTree("{\"updatedField\":\"updatedValue\"}")).thenReturn(updatedNode);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectNext(sections)
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithJsonParsingError_ThrowsAIServiceException()
      throws JsonProcessingException {
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    Artifact artifact = new Artifact();
    artifact.setText("Matching content");

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");
    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));

    // ❗️ Looser matcher to avoid NullPointerException
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.just(artifact));

    when(aiService.generateAnswer(
            Mockito.eq("{}"), anyString(), Mockito.eq(ContextName.PREFILL.getPrompt())))
        .thenReturn(Mono.just("invalid json"));

    // Simulate JSON parsing failure
    when(objectMapper.readTree("invalid json")).thenThrow(new RuntimeException("Invalid JSON"));

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AIServiceException
                    && throwable.getMessage().equals("Error parsing AI response JSON"))
        .verify();
  }

  @Test
  void prefillBRDProcessJson_WithVectorSearchError_ReturnsError() {
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");
    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(embeddings, 5, "doc1"))
        .thenReturn(Flux.error(new RuntimeException("Vector search failed")));

    StepVerifier.create(assistantService.prefillBRDProcessJson(sections, documentNames))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void findSemanticMatches_WithEmptyRules_ShouldReturnEmptyList() {
    // Given
    List<BrdRules> rules = new ArrayList<>();

    // When
    Mono<List<GuidanceData>> result = assistantService.findSemanticMatches(rules);

    // Then
    StepVerifier.create(result).expectNextMatches(List::isEmpty).verifyComplete();
  }

  @Test
  void findSemanticMatches_WithErrorInAIProcessing_ShouldReturnEmptyList() {
    // Given
    BrdRules rule = new BrdRules();
    rule.setRuleName("rule1");
    rule.setRuleId("description1");
    List<BrdRules> rules = List.of(rule);

    when(aiService.getEmbeddingsForBatch(any()))
        .thenReturn(Mono.just(List.of(Arrays.asList(1.0, 2.0))));
    when(artifactService.performVectorSearch(any(), anyInt(), anyString()))
        .thenReturn(Flux.empty());
    when(aiService.generateAnswer(anyString(), anyString(), anyString()))
        .thenReturn(Mono.error(new RuntimeException("AI processing error")));

    // When
    Mono<List<GuidanceData>> result = assistantService.findSemanticMatches(rules);

    // Then
    StepVerifier.create(result).expectNextMatches(List::isEmpty).verifyComplete();
  }

  @Test
  void parseAIResponseToGuidanceData_WithResultsArray_ShouldParseCorrectly()
      throws JsonProcessingException {
    // Given
    String aiResponse =
        "{\"results\":[{\"rule\":\"rule1\",\"field\":\"field1\",\"isRelevant\":true,\"explanation\":\"test\"}]}";

    // Mock the ObjectMapper behavior
    JsonNode rootNode = mock(JsonNode.class);
    JsonNode resultsNode = mock(JsonNode.class);
    JsonNode itemNode = mock(JsonNode.class);
    JsonNode ruleNode = mock(JsonNode.class);
    JsonNode fieldNode = mock(JsonNode.class);
    JsonNode isRelevantNode = mock(JsonNode.class);
    JsonNode explanationNode = mock(JsonNode.class);

    when(objectMapper.readTree(aiResponse)).thenReturn(rootNode);
    when(rootNode.has("results")).thenReturn(true);
    when(rootNode.get("results")).thenReturn(resultsNode);
    when(resultsNode.isArray()).thenReturn(true);
    when(resultsNode.iterator()).thenReturn(Collections.singletonList(itemNode).iterator());

    when(itemNode.has("rule")).thenReturn(true);
    when(itemNode.get("rule")).thenReturn(ruleNode);
    when(ruleNode.asText()).thenReturn("rule1");

    when(itemNode.has("field")).thenReturn(true);
    when(itemNode.get("field")).thenReturn(fieldNode);
    when(fieldNode.asText()).thenReturn("field1");

    when(itemNode.has("isRelevant")).thenReturn(true);
    when(itemNode.get("isRelevant")).thenReturn(isRelevantNode);
    when(isRelevantNode.asBoolean()).thenReturn(true);

    when(itemNode.has("explanation")).thenReturn(true);
    when(itemNode.get("explanation")).thenReturn(explanationNode);
    when(explanationNode.asText()).thenReturn("test");

    // When
    List<GuidanceData> result =
        ReflectionTestUtils.invokeMethod(
            assistantService, "parseAIResponseToGuidanceData", aiResponse);

    // Then
    assertNotNull(result, "Result should not be null");
    assertEquals(1, result.size(), "Should have exactly one guidance data item");
    GuidanceData guidanceData = result.get(0);
    assertEquals("rule1", guidanceData.getRuleName(), "Rule name should match");
    assertEquals("field1", guidanceData.getMappingKey(), "Mapping key should match");
    assertEquals(
        "100", guidanceData.getSimilarity(), "Similarity should be 100 for relevant items");
    assertEquals("test", guidanceData.getExplanation(), "Explanation should match");
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("rule1", result.getFirst().getRuleName());
  }

  @Test
  void parseAIResponseToGuidanceData_WithEmptyResponse_ShouldReturnEmptyList() {
    // Given
    String aiResponse = "";

    // When
    List<GuidanceData> result =
        ReflectionTestUtils.invokeMethod(
            assistantService, "parseAIResponseToGuidanceData", aiResponse);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.isEmpty(), "Result should be an empty list");
    assertEquals(0, result.size(), "Result should have size 0");
  }

  @Test
  void prefillBRDProcessJson_WithErrorInSectionProcessing_ShouldHandleError() {
    // Given
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    // Set the vectorSearchLimit field
    ReflectionTestUtils.setField(assistantService, "vectorSearchLimit", 10);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.isObject()).thenReturn(true);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get(anyString())).thenReturn(sectionNode);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");

    // Mock embeddings call to succeed
    when(aiService.getEmbeddings(anyString())).thenReturn(Mono.just(embeddings));

    // Mock vector search to return empty results
    when(artifactService.performVectorSearch(embeddings, 10, "doc1")).thenReturn(Flux.empty());

    // When
    Mono<JsonNode> result =
        assistantService.prefillBRDProcessJson(sections, documentNames, ContextName.PREFILL, null);

    // Then
    StepVerifier.create(result).expectNext(sections).verifyComplete();

    // Verify interactions
    verify(sections).get("section1");
    verify(aiService).getEmbeddings(anyString());
    verify(artifactService).performVectorSearch(embeddings, 10, "doc1");
  }

  @Test
  void generateBRDSummary_WithEmptyAgentPortal_ShouldHandleGracefully()
      throws JsonProcessingException {
    // Given
    String brdId = "test-brd-id";
    BRD brd = new BRD();
    brd.setBrdId(brdId);
    BRDSummaryRequest request = new BRDSummaryRequest();
    request.setAgentPortal(null);

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
    when(modelMapper.map(brd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any())).thenReturn("json");
    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenReturn(Flux.just("summary-part1", "summary-part2"));

    // When
    Flux<BRDSummaryResponse> result = assistantService.generateBRDSummary(brdId);

    // Then
    StepVerifier.create(result).expectNextCount(2).verifyComplete();
  }

  @Test
  void generateBRDSummary_WithJsonProcessingExceptionInRetry_ShouldHandleError()
      throws JsonProcessingException {
    // Given
    String brdId = "test-brd-id";
    BRD brd = new BRD();
    brd.setBrdId(brdId);
    BRDSummaryRequest request = new BRDSummaryRequest();
    AgentPortalConfig agentPortal = new AgentPortalConfig();
    request.setAgentPortal(agentPortal);

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
    when(modelMapper.map(brd, BRDSummaryRequest.class)).thenReturn(request);
    when(objectMapper.writeValueAsString(any()))
        .thenReturn("json")
        .thenThrow(new JsonProcessingException("Error in retry") {});

    when(aiService.generateAnswerAsStream(anyString(), anyString(), anyString()))
        .thenThrow(new TokenLimitExceededException("Token limit exceeded"));

    // When
    Flux<BRDSummaryResponse> result = assistantService.generateBRDSummary(brdId);

    // Then
    StepVerifier.create(result).expectError(TokenLimitExceededException.class).verify();
  }

  @Test
  void prefillBRDProcessJson_WithEmbeddingsError_ShouldHandleError() {
    // Given
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.isObject()).thenReturn(true);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get(anyString())).thenReturn(sectionNode);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");

    // Mock embeddings call to fail
    when(aiService.getEmbeddings(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Error generating embeddings")));

    // When
    Mono<JsonNode> result =
        assistantService.prefillBRDProcessJson(sections, documentNames, ContextName.PREFILL, null);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Error generating embeddings"))
        .verify();

    // Verify interactions
    verify(sections).get("section1");
    verify(aiService).getEmbeddings(anyString());
    verify(artifactService, never()).performVectorSearch(any(), anyInt(), anyString());
  }

  @Test
  void destroy_ShouldDisposeSchedulerSuccessfully() {
    // Given
    Scheduler mockScheduler = mock(Scheduler.class);
    ReflectionTestUtils.setField(assistantService, "boundedElasticScheduler", mockScheduler);

    // When
    assistantService.destroy();

    // Then
    verify(mockScheduler).dispose();
  }

  @Test
  void destroy_ShouldHandleDisposeErrorGracefully() {
    // Given
    Scheduler mockScheduler = mock(Scheduler.class);
    ReflectionTestUtils.setField(assistantService, "boundedElasticScheduler", mockScheduler);

    // Mock the dispose method to throw an exception
    doThrow(new RuntimeException("Test error")).when(mockScheduler).dispose();

    // When
    assistantService.destroy();

    // Then
    verify(mockScheduler).dispose();
    // The method should not throw an exception and should handle the error gracefully
  }

  @Test
  void parseAIResponseToGuidanceData_WithArrayResponse_ShouldParseAllFields()
      throws JsonProcessingException {
    // Given
    String aiResponse =
        "[{\"ruleName\":\"rule1\",\"mappingKey\":\"key1\",\"similarity\":85,\"explanation\":\"test explanation\"}]";

    // Mock the ObjectMapper behavior
    JsonNode rootNode = mock(JsonNode.class);
    JsonNode itemNode = mock(JsonNode.class);
    JsonNode ruleNameNode = mock(JsonNode.class);
    JsonNode mappingKeyNode = mock(JsonNode.class);
    JsonNode similarityNode = mock(JsonNode.class);
    JsonNode explanationNode = mock(JsonNode.class);

    when(objectMapper.readTree(aiResponse)).thenReturn(rootNode);
    when(rootNode.isArray()).thenReturn(true);
    when(rootNode.iterator()).thenReturn(Collections.singletonList(itemNode).iterator());

    when(itemNode.has("ruleName")).thenReturn(true);
    when(itemNode.get("ruleName")).thenReturn(ruleNameNode);
    when(ruleNameNode.asText()).thenReturn("rule1");

    when(itemNode.has("mappingKey")).thenReturn(true);
    when(itemNode.get("mappingKey")).thenReturn(mappingKeyNode);
    when(mappingKeyNode.asText()).thenReturn("key1");

    when(itemNode.has("similarity")).thenReturn(true);
    when(itemNode.get("similarity")).thenReturn(similarityNode);
    when(similarityNode.asInt()).thenReturn(85);

    when(itemNode.has("explanation")).thenReturn(true);
    when(itemNode.get("explanation")).thenReturn(explanationNode);
    when(explanationNode.asText()).thenReturn("test explanation");

    // When
    List<GuidanceData> result =
        ReflectionTestUtils.invokeMethod(
            assistantService, "parseAIResponseToGuidanceData", aiResponse);

    // Then
    assert (Objects.requireNonNull(result).size() == 1);
    GuidanceData guidanceData = result.getFirst();
    assertEquals("rule1", guidanceData.getRuleName());
    assertEquals("key1", guidanceData.getMappingKey());
    assertEquals("85", guidanceData.getSimilarity());
    assertEquals("test explanation", guidanceData.getExplanation());
  }

  @Test
  void parseAIResponseToGuidanceData_WithArrayResponseAndMissingFields_ShouldHandleGracefully()
      throws JsonProcessingException {
    // Given
    String aiResponse = "[{\"ruleName\":\"rule1\"}]";

    // Mock the ObjectMapper behavior
    JsonNode rootNode = mock(JsonNode.class);
    JsonNode itemNode = mock(JsonNode.class);
    JsonNode ruleNameNode = mock(JsonNode.class);

    when(objectMapper.readTree(aiResponse)).thenReturn(rootNode);
    when(rootNode.isArray()).thenReturn(true);
    when(rootNode.iterator()).thenReturn(Collections.singletonList(itemNode).iterator());

    when(itemNode.has("ruleName")).thenReturn(true);
    when(itemNode.get("ruleName")).thenReturn(ruleNameNode);
    when(ruleNameNode.asText()).thenReturn("rule1");

    when(itemNode.has("mappingKey")).thenReturn(false);
    when(itemNode.has("similarity")).thenReturn(false);
    when(itemNode.has("explanation")).thenReturn(false);

    // When
    List<GuidanceData> result =
        ReflectionTestUtils.invokeMethod(
            assistantService, "parseAIResponseToGuidanceData", aiResponse);

    // Then
    assert (Objects.requireNonNull(result).size() == 1);
    GuidanceData guidanceData = result.getFirst();
    assertEquals("rule1", guidanceData.getRuleName());
    assertNull(guidanceData.getMappingKey());
    assertNull(guidanceData.getSimilarity());
    assertNull(guidanceData.getExplanation());
  }

  @Test
  void findSemanticMatches_WithValidRules_ShouldReturnGuidanceData() {
    // Given
    BrdRules rule1 = new BrdRules();
    rule1.setRuleName("rule1");
    rule1.setRuleId("description1");

    BrdRules rule2 = new BrdRules();
    rule2.setRuleName("rule2");
    rule2.setRuleId("description2");

    List<BrdRules> rules = List.of(rule1, rule2);

    List<List<Double>> embeddings = List.of(List.of(0.1, 0.2, 0.3), List.of(0.4, 0.5, 0.6));

    Artifact artifact1 = new Artifact();
    artifact1.setText("Matching content for rule1");
    Artifact artifact2 = new Artifact();
    artifact2.setText("Matching content for rule2");

    String aiResponse =
        """
        [{
            "ruleName": "rule1",
            "mappingKey": "key1",
            "similarity": 85,
            "explanation": "High relevance match"
        },
        {
            "ruleName": "rule2",
            "mappingKey": "key2",
            "similarity": 75,
            "explanation": "Medium relevance match"
        }]""";

    when(aiService.getEmbeddingsForBatch(anyList())).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.just(artifact1))
        .thenReturn(Flux.just(artifact2));
    when(aiService.generateAnswer(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(aiResponse));

    // When
    Mono<List<GuidanceData>> result = assistantService.findSemanticMatches(rules);

    // Then
    StepVerifier.create(result)
        .assertNext(
            guidanceDataList -> {
              assertEquals(2, guidanceDataList.size());

              GuidanceData firstGuidance = guidanceDataList.get(0);
              assertEquals("rule1", firstGuidance.getRuleName());
              assertEquals("key1", firstGuidance.getMappingKey());
              assertEquals("85", firstGuidance.getSimilarity());
              assertEquals("High relevance match", firstGuidance.getExplanation());

              GuidanceData secondGuidance = guidanceDataList.get(1);
              assertEquals("rule2", secondGuidance.getRuleName());
              assertEquals("key2", secondGuidance.getMappingKey());
              assertEquals("75", secondGuidance.getSimilarity());
              assertEquals("Medium relevance match", secondGuidance.getExplanation());
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithValidData_ShouldReturnSummary(){
  // Given
    String brdId = "test-brd-id";
    Walletron walletron = new Walletron();
    walletron.setBrdId(brdId);
    walletron.setSiteConfiguration(new SiteConfiguration());
    walletron.setNotificationsOptions(new NotificationsOptions());
    walletron.setAciWalletronAgentPortal(new ACIWalletronAgentPortal());
    walletron.setAciWalletronDataExchange(new DataExchange());
    walletron.setAciWalletronEnrollmentStrategy(new EnrollmentStrategy());
    walletron.setEnrollmentUrl(new EnrollmentUrls());
    walletron.setTargetedCommunication(new TargetedCommunication());
    walletron.setAciCash(new AciCash());
    walletron.setWalletronApprovals(new WalletronApprovals());

    String expectedResponse =
        """
        {
            "siteConfiguration": {},
            "notificationsOptions": {},
            "agentPortal": {},
            "dataExchange": {},
            "enrollmentStrategy": {},
            "enrollmentUrls": {},
            "targetedCommunication": {},
            "aciCash": {},
            "approvals": {}
        }""";

    when(walletronRepository.findByBrdId(brdId)).thenReturn(Mono.just(walletron));
    when(aiService.generateAnswer(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<JsonNode> result = assistantService.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(
            jsonNode -> {
              assertTrue(jsonNode.has("siteConfiguration"));
              assertTrue(jsonNode.has("notificationsOptions"));
              assertTrue(jsonNode.has("agentPortal"));
              assertTrue(jsonNode.has("dataExchange"));
              assertTrue(jsonNode.has("enrollmentStrategy"));
              assertTrue(jsonNode.has("enrollmentUrls"));
              assertTrue(jsonNode.has("targetedCommunication"));
              assertTrue(jsonNode.has("aciCash"));
              assertTrue(jsonNode.has("approvals"));
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithEmptyData_ShouldReturnEmptySummary() {
    // Given
    String brdId = "test-brd-id";
    Walletron walletron = new Walletron();
    walletron.setBrdId(brdId);

    String expectedResponse = "{}";

    when(walletronRepository.findByBrdId(brdId)).thenReturn(Mono.just(walletron));
    when(aiService.generateAnswer(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<JsonNode> result = assistantService.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(jsonNode -> assertTrue(jsonNode.isEmpty()))
        .verifyComplete();
  }

  @Test
  void prefillBRDProcessJson_WithAdditionalContext_ShouldIncludeContext()
      throws JsonProcessingException {
    // Given
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    String additionalContext = "Additional context for processing";
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    Artifact artifact = new Artifact();
    artifact.setText("Matching content");

    ObjectNode updatedNode = mock(ObjectNode.class);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");
    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.just(artifact));
    when(aiService.generateAnswer(
            Mockito.eq("{}"),
            Mockito.contains(additionalContext),
            Mockito.eq(ContextName.PREFILL.getPrompt())))
        .thenReturn(Mono.just("{\"updatedField\":\"updatedValue\"}"));
    try {
      when(objectMapper.readTree("{\"updatedField\":\"updatedValue\"}")).thenReturn(updatedNode);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    // When
    Mono<JsonNode> result =
        assistantService.prefillBRDProcessJson(
            sections, documentNames, ContextName.PREFILL, additionalContext);

    // Then
    StepVerifier.create(result).expectNext(sections).verifyComplete();

    verify(aiService)
        .generateAnswer(
            Mockito.eq("{}"),
            Mockito.contains(additionalContext),
            Mockito.eq(ContextName.PREFILL.getPrompt()));
  }

  @Test
  void prefillBRDProcessJson_WithNullAdditionalContext_ShouldProcessNormally() {
    // Given
    ObjectNode sections = mock(ObjectNode.class);
    ObjectNode sectionNode = mock(ObjectNode.class);
    List<String> documentNames = List.of("doc1");
    List<Double> embeddings = Arrays.asList(0.1, 0.2, 0.3);

    Artifact artifact = new Artifact();
    artifact.setText("Matching content");

    ObjectNode updatedNode = mock(ObjectNode.class);

    when(sections.deepCopy()).thenReturn(sections);
    when(sections.fieldNames()).thenReturn(List.of("section1").iterator());
    when(sections.get("section1")).thenReturn(sectionNode);
    when(sections.isObject()).thenReturn(true);
    when(sectionNode.isObject()).thenReturn(true);
    when(sectionNode.toString()).thenReturn("{}");
    when(aiService.getEmbeddings("{}")).thenReturn(Mono.just(embeddings));
    when(artifactService.performVectorSearch(anyList(), anyInt(), anyString()))
        .thenReturn(Flux.just(artifact));
    when(aiService.generateAnswer(
            Mockito.eq("{}"), Mockito.anyString(), Mockito.eq(ContextName.PREFILL.getPrompt())))
        .thenReturn(Mono.just("{\"updatedField\":\"updatedValue\"}"));
    try {
      when(objectMapper.readTree("{\"updatedField\":\"updatedValue\"}")).thenReturn(updatedNode);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    // When
    Mono<JsonNode> result =
        assistantService.prefillBRDProcessJson(sections, documentNames, ContextName.PREFILL, null);

    // Then
    StepVerifier.create(result).expectNext(sections).verifyComplete();

    verify(aiService)
        .generateAnswer(
            Mockito.eq("{}"), Mockito.anyString(), Mockito.eq(ContextName.PREFILL.getPrompt()));
  }
}
