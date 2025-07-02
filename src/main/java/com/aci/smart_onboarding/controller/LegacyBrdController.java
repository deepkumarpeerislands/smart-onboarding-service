package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.service.implementation.LegacyBrdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(
    name = "Legacy BRD Processing",
    description = "APIs for processing legacy BRD data in the system")
@RequestMapping(value = "${api.default.path}/legacy", name = "Legacy BRD Management")
public class LegacyBrdController {

  private final LegacyBrdService legacyBrdService;

  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_ERROR = "ERROR";
  private static final String STANDARD_DATA_SUCCESS_MESSAGE =
      "Standard data retrieved successfully";
  private static final String USER_RULES_SUCCESS_MESSAGE = "User rules retrieved successfully";
  private static final String PREFILL_SUCCESS_MESSAGE =
      "BRD prefill operation completed successfully";
  private static final String PREFILL_ERROR_MESSAGE =
      "BRD prefill operation failed - BRD not found or conversion issues";
  private static final String PREFILL_FAILED_MESSAGE = "Failed to prefill BRD";

  @Operation(
      summary = "Get standard data mapping",
      description = "Retrieves the standard data mapping from the specified URL")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Standard data retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @GetMapping("/standard-data")
  public Mono<ResponseEntity<Api<List<GuidanceData>>>> getStandardData(
      @Parameter(description = "URL of the standard data file", required = true) @RequestParam
          String fileUrl) {
    return legacyBrdService
        .getStandardData(fileUrl)
        .map(
            data ->
                ResponseEntity.ok(
                    new Api<>(
                        STATUS_SUCCESS,
                        STANDARD_DATA_SUCCESS_MESSAGE,
                        Optional.of(data),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get user rules data",
      description = "Retrieves the user rules data from the specified URL")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User rules retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @GetMapping("/user-rules")
  public Mono<ResponseEntity<Api<List<BrdRules>>>> getUserRules(
      @Parameter(description = "URL of the user rules file", required = true) @RequestParam
          String fileUrl) {
    return legacyBrdService
        .getUserRules(fileUrl)
        .map(
            data ->
                ResponseEntity.ok(
                    new Api<>(
                        STATUS_SUCCESS,
                        USER_RULES_SUCCESS_MESSAGE,
                        Optional.of(data),
                        Optional.empty())));
  }

  @Operation(
      summary = "Generate combined rules data file",
      description =
          "Processes and combines the standard data and user rules to create a downloadable JSON file")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Combined rules file created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @PostMapping("/rules-with-data")
  public Mono<ResponseEntity<Resource>> getRulesWithData(
      @Parameter(description = "Legacy BRD processing details", required = true) @Valid @RequestBody
          LegacyBrdRequest request) {
    return legacyBrdService
        .getRulesWithData(request)
        .map(
            resource -> {
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);
              headers.setContentDispositionFormData("attachment", "combined_rules.json");
              return ResponseEntity.ok().headers(headers).body(resource);
            });
  }

  @Operation(
      summary = "Prefill legacy BRD",
      description = "Prefills legacy BRD based on the provided BRD ID and document name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Prefill operation completed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @PostMapping("/prefill")
  public Mono<ResponseEntity<Api<Boolean>>> prefillLegacyBrd(
      @Parameter(description = "Legacy BRD prefill request details", required = true)
          @Valid
          @RequestBody
          LegacyPrefillRequest request) {
    return legacyBrdService
        .prefillLegacyBRD(request)
        .map(
            success -> {
              boolean isSuccessful = success;
              if (isSuccessful) {
                return ResponseEntity.ok(
                    new Api<>(
                        STATUS_SUCCESS,
                        PREFILL_SUCCESS_MESSAGE,
                        Optional.of(true),
                        Optional.empty()));
              } else {
                Map<String, String> errors = new HashMap<>();
                errors.put(
                    "error", "Unable to locate or process BRD with ID: " + request.getBrdId());
                return ResponseEntity.ok(
                    new Api<>(
                        STATUS_ERROR,
                        PREFILL_ERROR_MESSAGE,
                        Optional.of(false),
                        Optional.of(errors)));
              }
            })
        .onErrorResume(
            e -> {
              Map<String, String> errors = new HashMap<>();
              errors.put("error", e.getMessage());
              return Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          STATUS_ERROR,
                          PREFILL_FAILED_MESSAGE,
                          Optional.empty(),
                          Optional.of(errors))));
            });
  }
}
