package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdFormResponse;
import com.aci.smart_onboarding.dto.BrdTemplateReq;
import com.aci.smart_onboarding.dto.BrdTemplateRes;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.implementation.BrdTemplateService;
import com.aci.smart_onboarding.swagger.BrdTemplateRequestAndResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for managing BRD (Business Requirements Document) templates. Provides endpoints for
 * creating, updating, and retrieving BRD templates. All operations require MANAGER role access.
 *
 * @author Smart Onboarding Team
 * @version 1.0
 */
@RestController
@Validated
@RequiredArgsConstructor
@Tag(
    name = "BRD templates management",
    description = "APIs for managing BRD templates in the system")
@RequestMapping(value = "${api.default.path}/brd/template")
public class BrdTemplateConfigurationController {

  private static final String ACCESS_DENIED_MESSAGE =
      "Access denied. Only Manager role can access templates";
  private static final String FAILURE_STATUS = "failure";
  private static final String SUCCESS_STATUS = "success";

  private final BrdTemplateService brdTemplateService;
  private final BRDSecurityService securityService;

  /**
   * Creates a new BRD template with specified configurations. Only accessible by users with MANAGER
   * role.
   *
   * @param brdTemplateReq The template configuration request
   * @return ResponseEntity containing the created template or error details
   */
  @Operation(
      summary = "Create a new BRD template",
      description =
          "Creates a new BRD template configuration with specified field enablement flags. Only accessible by Manager role.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Only Manager role can create templates"),
        @ApiResponse(responseCode = "409", description = "Template type already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> saveBrdForm(
      @Valid @RequestBody Mono<BrdTemplateReq> brdTemplateReq) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                return this.<BrdTemplateRes>createForbiddenResponse();
              }
              return brdTemplateReq.flatMap(brdTemplateService::createTemplate);
            })
        .switchIfEmpty(this.<BrdTemplateRes>createForbiddenResponse());
  }

  @Operation(
      summary = "Update an existing BRD template",
      description = "Updates an existing BRD template configuration by its ID",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(value = BrdTemplateRequestAndResponses.TEMPLATE_REQUEST))))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                BrdTemplateRequestAndResponses.TEMPLATE_UPDATE_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.TEMPLATE_VALIDATION_ERROR))),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(value = BrdTemplateRequestAndResponses.TEMPLATE_NOT_FOUND))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.INTERNAL_SERVER_ERROR)))
      })
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> updateTemplate(
      @Parameter(
              description = "Template ID",
              example = BrdTemplateRequestAndResponses.EXAMPLE_TEMPLATE_ID)
          @NotBlank
          @PathVariable
          String id,
      @Valid @RequestBody Mono<BrdTemplateReq> brdTemplateReq) {
    return brdTemplateReq.flatMap(req -> brdTemplateService.updateTemplate(id, req));
  }

  @Operation(
      summary = "Get all BRD templates",
      description = "Retrieves all available BRD template configurations")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Templates retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value =
                                BrdTemplateRequestAndResponses.TEMPLATE_LIST_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "404",
            description = "No templates found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(value = BrdTemplateRequestAndResponses.TEMPLATE_NOT_FOUND))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.INTERNAL_SERVER_ERROR)))
      })
  @GetMapping
  public Mono<ResponseEntity<Api<List<BrdTemplateRes>>>> getAllTemplates() {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                return this.<List<BrdTemplateRes>>createForbiddenResponse();
              }
              return brdTemplateService.getAllTemplates();
            })
        .switchIfEmpty(this.<List<BrdTemplateRes>>createForbiddenResponse());
  }

  @Operation(
      summary = "Get BRD template by type",
      description = "Retrieves a specific BRD template configuration by its type")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template found successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.TEMPLATE_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid template type",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.TEMPLATE_VALIDATION_ERROR))),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(value = BrdTemplateRequestAndResponses.TEMPLATE_NOT_FOUND))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = BrdTemplateRequestAndResponses.INTERNAL_SERVER_ERROR)))
      })
  @GetMapping("/{brdTemplateType}")
  public Mono<ResponseEntity<Api<BrdTemplateRes>>> getTemplateByType(
      @Parameter(
              description = "Template type",
              example = BrdTemplateRequestAndResponses.EXAMPLE_TEMPLATE_TYPE)
          @NotBlank
          @PathVariable
          String brdTemplateType) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                return this.<BrdTemplateRes>createForbiddenResponse();
              }
              return brdTemplateService.getTemplateByType(brdTemplateType);
            })
        .switchIfEmpty(this.<BrdTemplateRes>createForbiddenResponse());
  }

  @Operation(
      summary = "Get BRD form by brdId and templateType",
      description =
          "Retrieves a BRD form that's dynamically created based on template configuration stored in brd_template_config collection and basic BRD information from brd collection")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD form retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "404",
            description = "BRD or template not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @GetMapping("/{brdId}/{templateType}")
  public Mono<ResponseEntity<Api<BrdFormResponse>>> getBrdForm(
      @Parameter(description = "BRD identifier", required = true) @PathVariable String brdId,
      @Parameter(description = "Template type", required = true) @PathVariable
          String templateType) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)
                  && !SecurityConstants.PM_ROLE.equals(role)) {
                return this.<BrdFormResponse>createForbiddenResponse();
              }
              return brdTemplateService.getBrdFormByIdAndTemplateType(brdId, templateType);
            })
        .switchIfEmpty(this.<BrdFormResponse>createForbiddenResponse());
  }

  /**
   * Helper method to create a forbidden response.
   *
   * @param <T> The type of response data
   * @return A Mono containing the forbidden response
   */
  private <T> Mono<ResponseEntity<Api<T>>> createForbiddenResponse() {
    return Mono.just(
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                new Api<>(
                    FAILURE_STATUS,
                    ACCESS_DENIED_MESSAGE,
                    Optional.empty(),
                    Optional.of(Collections.singletonMap("error", ACCESS_DENIED_MESSAGE)))));
  }
}
