package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BRDPrefillRequest;
import com.aci.smart_onboarding.dto.BRDSummaryResponse;
import com.aci.smart_onboarding.dto.QuestionRequest;
import com.aci.smart_onboarding.dto.StreamApi;
import com.aci.smart_onboarding.enums.ContextName;
import com.aci.smart_onboarding.service.IAssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "${api.default.path}/assistant", name = "Smart Assistant")
@Tag(name = "Smart Assistant", description = "Operations pertaining to Smart Assistant")
@Validated
public class AssistantController {

  private final IAssistantService assistantService;
  private static final String SUCCESS_STATUS = "success";
  private static final String ERROR_STATUS = "error";

  public AssistantController(IAssistantService assistantService) {
    this.assistantService = assistantService;
  }

  @PostMapping(value = "/ask", name = "Ask Assistant", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "Ask Assistant (Streaming)",
      description = "Ask Assistant for help with streaming response")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Assistant not found",
            content = @Content(schema = @Schema(implementation = StreamApi.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = StreamApi.class)))
      })
  public ResponseEntity<Flux<StreamApi<String>>> ask(@Valid @RequestBody QuestionRequest question) {
    ContextName contextEnum = ContextName.valueOf(question.getContextName().toUpperCase());
    Flux<String> responseStream =
        assistantService.askStreamingAssistant(
            question.getQuestion(), contextEnum, question.getDocumentName());

    Flux<StreamApi<String>> apiResponseStream =
        responseStream.map(
            response ->
                new StreamApi<>(SUCCESS_STATUS, "Streaming response", response, Optional.empty()));

    return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(apiResponseStream);
  }

  @PostMapping(value = "/askAI", name = "Ask AI Assistant")
  @Operation(
      summary = "Ask AI Assistant (Non-streaming)",
      description = "Ask AI Assistant for help with non-streaming response")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Assistant not found",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<String>>> askAI(@Valid @RequestBody QuestionRequest question) {
    ContextName contextEnum = ContextName.valueOf(question.getContextName().toUpperCase());
    return assistantService
        .askAssistant(question.getQuestion(), contextEnum, question.getDocumentName())
        .map(responses -> String.join(" ", responses))
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        SUCCESS_STATUS,
                        "AI response generated successfully",
                        Optional.of(response),
                        Optional.empty())));
  }

  @PostMapping("/prefill-brd")
  @Operation(
      summary = "Pre-fill BRD",
      description = "Pre-fill BRD sections with AI-generated content")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<JsonNode>>> prefillBRD(
      @Valid @RequestBody BRDPrefillRequest requestBody) {
    return assistantService
        .prefillBRDProcessJson(
            requestBody.getSections(), requestBody.getDocumentNames(), ContextName.PREFILL, null)
        .map(
            jsonNode ->
                ResponseEntity.ok(
                    new Api<>(
                        SUCCESS_STATUS,
                        "BRD pre-filled successfully",
                        Optional.of(jsonNode),
                        Optional.empty())))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.ok(
                        new Api<>(
                            ERROR_STATUS,
                            "Failed to pre-fill BRD",
                            Optional.empty(),
                            Optional.of(Map.of(ERROR_STATUS, e.getMessage()))))));
  }

  @GetMapping(value = "/brd/summary", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "Generate BRD Summary",
      description = "Generates a summary of the BRD sections using AI")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(schema = @Schema(implementation = StreamApi.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = StreamApi.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = StreamApi.class)))
      })
  public ResponseEntity<Flux<StreamApi<BRDSummaryResponse>>> generateBRDSummary(
      @RequestParam @Valid String brdId) {
    Flux<BRDSummaryResponse> summaryFlux = assistantService.generateBRDSummary(brdId);

    Flux<StreamApi<BRDSummaryResponse>> apiResponseStream =
        summaryFlux.map(
            response ->
                new StreamApi<>(
                    SUCCESS_STATUS, "Streaming BRD summary", response, Optional.empty()));

    return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(apiResponseStream);
  }

  @GetMapping(value = "/walletron/summary")
  @Operation(
      summary = "Generate Walletron Summary",
      description = "Generates a summary of the Walletron configuration using AI")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<JsonNode>>> generateWalletronSummary(
      @RequestParam @Valid String brdId) {
    return assistantService
        .generateWalletronSummary(brdId)
        .map(
            jsonNode ->
                ResponseEntity.ok(
                    new Api<>(
                        SUCCESS_STATUS,
                        "Walletron summary generated successfully",
                        Optional.of(jsonNode),
                        Optional.empty())))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.ok(
                        new Api<>(
                            ERROR_STATUS,
                            "Failed to generate Walletron summary",
                            Optional.empty(),
                            Optional.of(Map.of(ERROR_STATUS, e.getMessage()))))));
  }
}
