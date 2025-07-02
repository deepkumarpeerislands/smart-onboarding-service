package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BRDPrefillRequest;
import com.aci.smart_onboarding.dto.BRDSummaryResponse;
import com.aci.smart_onboarding.dto.QuestionRequest;
import com.aci.smart_onboarding.dto.StreamApi;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.service.IAssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AssistantControllerTest {

  @Mock private IAssistantService assistantService;

  @InjectMocks private AssistantController assistantController;

  private QuestionRequest mockQuestionRequest;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockQuestionRequest = new QuestionRequest("test question", "SUMMARY", "test-doc");
    objectMapper = new ObjectMapper();
  }

  @Test
  void ask_WithSummaryContext_ReturnsSummaryStream() {
    // Given
    mockQuestionRequest.setQuestion("Can you summarize this document?");

    when(assistantService.askStreamingAssistant(
            "Can you summarize this document?", ContextName.SUMMARY, "test-doc"))
        .thenReturn(Flux.just("Summary of the document", "Key points"));

    // When
    ResponseEntity<Flux<StreamApi<String>>> result = assistantController.ask(mockQuestionRequest);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody())
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming response", response.getMessage());
              assertEquals("Summary of the document", response.getData());
            })
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming response", response.getMessage());
              assertEquals("Key points", response.getData());
            })
        .verifyComplete();
  }

  @Test
  void ask_WithChatContext_ReturnsChatStream() {
    // Given
    mockQuestionRequest.setQuestion("What is ACI?");
    mockQuestionRequest.setContextName("CHAT");

    when(assistantService.askStreamingAssistant("What is ACI?", ContextName.CHAT, "test-doc"))
        .thenReturn(Flux.just("ACI is a global", "payment solutions provider"));

    // When
    ResponseEntity<Flux<StreamApi<String>>> result = assistantController.ask(mockQuestionRequest);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody())
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming response", response.getMessage());
              assertEquals("ACI is a global", response.getData());
            })
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming response", response.getMessage());
              assertEquals("payment solutions provider", response.getData());
            })
        .verifyComplete();
  }

  @Test
  void askAI_WithTemplateContext_ReturnsTemplateAnalysis() {
    // Given
    mockQuestionRequest.setQuestion("");
    mockQuestionRequest.setContextName("TEMPLATE");
    String expectedResponse =
        "[{\"templateTypes\":\"Finance\",\"summary\":\"Financial services template\",\"percentage\":85}]";

    when(assistantService.askAssistant("", ContextName.TEMPLATE, "test-doc"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<String>>> result = assistantController.askAI(mockQuestionRequest);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("AI response generated successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void askAI_WithDecisionContext_ReturnsDecisionResponse() {
    // Given
    mockQuestionRequest.setQuestion("What are the key requirements?");
    mockQuestionRequest.setContextName("DECISION");
    String expectedResponse = "Based on the requirements...";

    when(assistantService.askAssistant(
            "What are the key requirements?", ContextName.DECISION, "test-doc"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<String>>> result = assistantController.askAI(mockQuestionRequest);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("AI response generated successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void askAI_WithChatContext_ReturnsChatResponse() {
    // Given
    mockQuestionRequest.setQuestion("What is ACI?");
    mockQuestionRequest.setContextName("CHAT");
    String expectedResponse = "ACI is a global payment solutions provider";

    when(assistantService.askAssistant("What is ACI?", ContextName.CHAT, "test-doc"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<String>>> result = assistantController.askAI(mockQuestionRequest);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("AI response generated successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void askAI_WithEmptyQuestion_ReturnsEmptyResponse() {
    // Given
    mockQuestionRequest.setQuestion("");
    mockQuestionRequest.setContextName("CHAT");
    String expectedResponse = "";

    when(assistantService.askAssistant("", ContextName.CHAT, "test-doc"))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<String>>> result = assistantController.askAI(mockQuestionRequest);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("AI response generated successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void prefillBRD_WithValidInput_ReturnsPrefilledSections() {
    // Given
    BRDPrefillRequest requestBody = new BRDPrefillRequest();
    ObjectNode sections = objectMapper.createObjectNode();
    sections.put("section1", "value1");
    sections.put("section2", "value2");
    requestBody.setSections(sections);
    requestBody.setDocumentNames(Arrays.asList("doc1", "doc2"));

    ObjectNode expectedResponse = objectMapper.createObjectNode();
    expectedResponse.put("section1", "updated_value1");
    expectedResponse.put("section2", "value2");

    when(assistantService.prefillBRDProcessJson(
            sections, Arrays.asList("doc1", "doc2"), ContextName.PREFILL, null))
        .thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result = assistantController.prefillBRD(requestBody);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("BRD pre-filled successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void prefillBRD_WithError_ReturnsErrorResponse() {
    // Given
    BRDPrefillRequest requestBody = new BRDPrefillRequest();
    ObjectNode sections = objectMapper.createObjectNode();
    sections.put("section1", "value1");
    requestBody.setSections(sections);
    requestBody.setDocumentNames(Arrays.asList("doc1"));

    when(assistantService.prefillBRDProcessJson(
            sections, Arrays.asList("doc1"), ContextName.PREFILL, null))
        .thenReturn(Mono.error(new RuntimeException("Processing error")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result = assistantController.prefillBRD(requestBody);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to pre-fill BRD", response.getBody().getMessage());
              assertEquals("Processing error", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }

  @Test
  void prefillBRD_WithNullSections_ReturnsErrorResponse() {
    // Given
    BRDPrefillRequest requestBody = new BRDPrefillRequest();
    requestBody.setSections(null);
    requestBody.setDocumentNames(Arrays.asList("doc1"));

    when(assistantService.prefillBRDProcessJson(
            null, Arrays.asList("doc1"), ContextName.PREFILL, null))
        .thenReturn(Mono.error(new NullPointerException("Sections cannot be null")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result = assistantController.prefillBRD(requestBody);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to pre-fill BRD", response.getBody().getMessage());
              assertEquals(
                  "Sections cannot be null", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }

  @Test
  void prefillBRD_WithEmptyDocumentNames_ReturnsOriginalSections() {
    // Given
    BRDPrefillRequest requestBody = new BRDPrefillRequest();
    ObjectNode sections = objectMapper.createObjectNode();
    sections.put("section1", "value1");
    sections.put("section2", "value2");
    requestBody.setSections(sections);
    requestBody.setDocumentNames(Arrays.asList());

    when(assistantService.prefillBRDProcessJson(
            sections, Arrays.asList(), ContextName.PREFILL, null))
        .thenReturn(Mono.just(sections));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result = assistantController.prefillBRD(requestBody);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals("BRD pre-filled successfully", response.getBody().getMessage());
              assertEquals(sections, response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void prefillBRD_WithInvalidJsonResponse_ReturnsErrorResponse() {
    // Given
    BRDPrefillRequest requestBody = new BRDPrefillRequest();
    ObjectNode sections = objectMapper.createObjectNode();
    sections.put("section1", "value1");
    requestBody.setSections(sections);
    requestBody.setDocumentNames(Arrays.asList("doc1"));

    when(assistantService.prefillBRDProcessJson(
            sections, Arrays.asList("doc1"), ContextName.PREFILL, null))
        .thenReturn(Mono.error(new RuntimeException("Invalid JSON response")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result = assistantController.prefillBRD(requestBody);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to pre-fill BRD", response.getBody().getMessage());
              assertEquals(
                  "Invalid JSON response", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }

  @Test
  void ask_WithServiceError_ReturnsErrorResponse() {
    // Given
    mockQuestionRequest.setQuestion("test question");
    mockQuestionRequest.setContextName("SUMMARY");

    when(assistantService.askStreamingAssistant(anyString(), any(ContextName.class), anyString()))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    // When
    ResponseEntity<Flux<StreamApi<String>>> result = assistantController.ask(mockQuestionRequest);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody()).expectError(RuntimeException.class).verify();
  }

  @Test
  void generateBRDSummary_WithValidBrdId_ReturnsSummaryStream() {
    // Given
    String brdId = "test-brd-id";
    BRDSummaryResponse mockResponse = new BRDSummaryResponse();
    mockResponse.setBrdId(brdId);
    mockResponse.setSummary("Test summary");
    mockResponse.setStatus("SUCCESS");

    when(assistantService.generateBRDSummary(brdId)).thenReturn(Flux.just(mockResponse));

    // When
    ResponseEntity<Flux<StreamApi<BRDSummaryResponse>>> result =
        assistantController.generateBRDSummary(brdId);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody())
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming BRD summary", response.getMessage());
              assertEquals(mockResponse, response.getData());
              assertTrue(response.getErrors().isEmpty());
            })
        .verifyComplete();
  }

  @Test
  void generateBRDSummary_WithServiceError_ReturnsErrorResponse() {
    // Given
    String brdId = "test-brd-id";
    when(assistantService.generateBRDSummary(brdId))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    // When
    ResponseEntity<Flux<StreamApi<BRDSummaryResponse>>> result =
        assistantController.generateBRDSummary(brdId);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody()).expectError(RuntimeException.class).verify();
  }

  @Test
  void generateBRDSummary_WithEmptyBrdId_ReturnsErrorResponse() {
    // Given
    String brdId = "";
    BRDSummaryResponse errorResponse = new BRDSummaryResponse();
    errorResponse.setBrdId("");
    errorResponse.setStatus("error");
    errorResponse.setErrorMessage("BRD ID is required");

    when(assistantService.generateBRDSummary(brdId)).thenReturn(Flux.just(errorResponse));

    // When
    ResponseEntity<Flux<StreamApi<BRDSummaryResponse>>> result =
        assistantController.generateBRDSummary(brdId);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody())
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming BRD summary", response.getMessage());
              assertEquals(errorResponse, response.getData());
            })
        .verifyComplete();
  }

  @Test
  void ask_WithInvalidContext_ReturnsErrorResponse() {
    // Given
    mockQuestionRequest.setQuestion("test question");
    mockQuestionRequest.setContextName("INVALID");

    // When & Then
    try {
      assistantController.ask(mockQuestionRequest);
    } catch (IllegalArgumentException e) {
      assertEquals(
          "No enum constant com.aci.smart_onboarding.enums.ContextName.INVALID", e.getMessage());
    }
  }

  @Test
  void askAI_WithInvalidContext_ReturnsErrorResponse() {
    // Given
    mockQuestionRequest.setQuestion("test question");
    mockQuestionRequest.setContextName("INVALID");

    // When & Then
    try {
      assistantController.askAI(mockQuestionRequest).block();
    } catch (IllegalArgumentException e) {
      assertEquals(
          "No enum constant com.aci.smart_onboarding.enums.ContextName.INVALID", e.getMessage());
    }
  }

  @Test
  void askAI_WithServiceError_ReturnsErrorResponse() {
    // Given
    mockQuestionRequest.setQuestion("test question");
    mockQuestionRequest.setContextName("CHAT");

    when(assistantService.askAssistant(anyString(), any(ContextName.class), anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When & Then
    StepVerifier.create(assistantController.askAI(mockQuestionRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Service error"))
        .verify();
  }

  @Test
  void generateBRDSummary_WithNullBrdId_ReturnsErrorResponse() {
    // Given
    String brdId = null;
    BRDSummaryResponse errorResponse = new BRDSummaryResponse();
    errorResponse.setBrdId(null);
    errorResponse.setStatus("error");
    errorResponse.setErrorMessage("BRD ID is required");

    when(assistantService.generateBRDSummary(brdId)).thenReturn(Flux.just(errorResponse));

    // When
    ResponseEntity<Flux<StreamApi<BRDSummaryResponse>>> result =
        assistantController.generateBRDSummary(brdId);

    // Then
    assertEquals(OK, result.getStatusCode());
    assertEquals(MediaType.TEXT_EVENT_STREAM, result.getHeaders().getContentType());

    StepVerifier.create(result.getBody())
        .assertNext(
            response -> {
              assertEquals("success", response.getStatus());
              assertEquals("Streaming BRD summary", response.getMessage());
              assertEquals(errorResponse, response.getData());
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithValidBrdId_ReturnsSummary() {
    // Given
    String brdId = "test-brd-id";
    ObjectNode expectedResponse = objectMapper.createObjectNode();
    expectedResponse.put("summary", "Test Walletron summary");
    expectedResponse.put("status", "SUCCESS");

    when(assistantService.generateWalletronSummary(brdId)).thenReturn(Mono.just(expectedResponse));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result =
        assistantController.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("success", response.getBody().getStatus());
              assertEquals(
                  "Walletron summary generated successfully", response.getBody().getMessage());
              assertEquals(expectedResponse, response.getBody().getData().get());
              assertTrue(response.getBody().getErrors().isEmpty());
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithServiceError_ReturnsErrorResponse() {
    // Given
    String brdId = "test-brd-id";
    when(assistantService.generateWalletronSummary(brdId))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result =
        assistantController.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to generate Walletron summary", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isEmpty());
              assertEquals("Service error", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithEmptyBrdId_ReturnsErrorResponse() {
    // Given
    String brdId = "";
    when(assistantService.generateWalletronSummary(brdId))
        .thenReturn(Mono.error(new IllegalArgumentException("BRD ID is required")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result =
        assistantController.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to generate Walletron summary", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isEmpty());
              assertEquals("BRD ID is required", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }

  @Test
  void generateWalletronSummary_WithNullBrdId_ReturnsErrorResponse() {
    // Given
    String brdId = null;
    when(assistantService.generateWalletronSummary(brdId))
        .thenReturn(Mono.error(new IllegalArgumentException("BRD ID is required")));

    // When
    Mono<ResponseEntity<Api<JsonNode>>> result =
        assistantController.generateWalletronSummary(brdId);

    // Then
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(OK, response.getStatusCode());
              assertEquals("error", response.getBody().getStatus());
              assertEquals("Failed to generate Walletron summary", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isEmpty());
              assertEquals("BRD ID is required", response.getBody().getErrors().get().get("error"));
            })
        .verifyComplete();
  }
}
