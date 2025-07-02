package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.SiteConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.swagger.BRDRequestAndResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "BRD Management", description = "APIs for managing BRD in the system")
@RequestMapping(value = "${api.default.path}/brd", name = "BRD Management")
public class BRDController {

  private final IBRDService brdService;
  private final BRDSecurityService securityService;
  private final IAuditLogService auditLogService;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final DtoModelMapper dtoModelMapper;
  private static final Logger log = LoggerFactory.getLogger(BRDController.class);

  @Operation(
      summary = "Create a new BRD form",
      description = "Creates a new Business Requirement Document form with the provided details")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "BRD form details",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BRDRequest.class),
              examples = {
                @ExampleObject(
                    name = "Sample BRD Request",
                    value = BRDRequestAndResponses.BRD_REQUEST_EXAMPLE)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "BRD form created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BRDResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.BRD_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation errors occurred",
                          value = BRDRequestAndResponses.BRD_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "409",
            description = "BRD already exists",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Already exists",
                          value = BRDRequestAndResponses.ALREADY_EXISTS)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @PostMapping
  public Mono<ResponseEntity<Api<BRDResponse>>> saveBrdForm(
      @Valid @RequestBody Mono<BRDRequest> brdRequest) {
    log.debug("Received request to create new BRD");
    return brdRequest
        .doOnNext(request -> log.debug("Processing BRD request: {}", request))
        .flatMap(
            request ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    "Only Project Managers (PM) can create new BRDs"));
                          }
                          return brdService.createBrdForm(request);
                        }))
        .doOnNext(response -> log.debug("BRD created successfully: {}", response))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get BRD by ID",
      description =
          "Retrieves a specific Business Requirement Document using its unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD found successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BRDResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.BRD_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid BRD ID",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.BRD_ID_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_NOT_FOUND)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping("/{brdFormId}")
  public Mono<ResponseEntity<Api<BRDResponse>>> getBrdByID(
      @NotBlank @PathVariable String brdFormId) {
    return brdService
        .getBrdById(brdFormId)
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  response
                      .getBody()
                      .getData()
                      .orElseThrow(
                          () -> new NotFoundException(BrdConstants.BRD_NOT_FOUND + brdFormId));
              return securityService
                  .withSecurityCheck(brdResponse.getStatus())
                  .thenReturn(response);
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                log.debug("Controller handling AccessDeniedException: {}", e.getMessage());
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              "An error occurred while processing your request",
                              Optional.empty(),
                              Optional.empty())));
            });
  }

  @Operation(
      summary = "Update BRD partially",
      description = "Updates specific fields of an existing Business Requirement Document")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "BRD fields to update",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BRDRequest.class),
              examples = {
                @ExampleObject(
                    name = "Sample Partial Update",
                    value = BRDRequestAndResponses.BRD_PATCH_REQUEST)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BRDResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.BRD_UPDATE_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.BRD_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_NOT_FOUND)
                    })),
        @ApiResponse(
            responseCode = "409",
            description = "BRD already exists",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Already exists",
                          value = BRDRequestAndResponses.ALREADY_EXISTS)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @PatchMapping("/{brdFormId}")
  public Mono<ResponseEntity<Api<BRDResponse>>> updateBrdPartially(
      @Parameter(description = "BRD identifier", example = "BRD-1234", required = true)
          @NotBlank
          @PathVariable
          String brdFormId,
      @RequestBody Mono<Map<String, Object>> patchFields) {
    return brdService
        .getBrdById(brdFormId)
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  response
                      .getBody()
                      .getData()
                      .orElseThrow(
                          () -> new NotFoundException(SiteConstants.BRD_NOT_FOUND + brdFormId));
              return securityService
                  .withSecurityCheck(brdResponse.getStatus())
                  .then(
                      patchFields.flatMap(
                          f -> brdService.updateBrdPartiallyWithOrderedOperations(brdFormId, f)));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get BRD section by ID",
      description =
          "Retrieves a specific section of a Business Requirement Document using its unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD section found successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.BRD_SECTION_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid BRD ID or section name",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.BRD_SECTION_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD or section not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_NOT_FOUND)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping("/{brdFormId}/{sectionName}")
  public Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> getBrdSectionByID(
      @Parameter(
              description = "BRD identifier",
              example = "67c7cd1578f9b1356be858f9",
              required = true)
          @NotBlank
          @PathVariable
          String brdFormId,
      @Parameter(
              description =
                  "Section name (e.g., fundingMethods, paymentChannels, clientInformation)",
              example = "fundingMethods",
              required = true)
          @NotBlank
          @PathVariable
          String sectionName) {
    return brdService
        .getBrdById(brdFormId)
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  response
                      .getBody()
                      .getData()
                      .orElseThrow(
                          () -> new NotFoundException("BRD not found with id: " + brdFormId));
              return securityService
                  .withSecurityCheck(brdResponse.getStatus())
                  .then(brdService.getBrdSectionById(brdFormId, sectionName));
            });
  }

  @Operation(
      summary = "Retrieve Paginated BRD List Ordered by Recent Updates",
      description =
          "Fetches a paginated list of Business Requirement Documents (BRDs), sorted to display the most recently updated or newly created records first. The response includes relevant details based on the provided page number and page size.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD list retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.BRD_LIST_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid pagination parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.BRD_LIST_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD or section not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_LIST_NOT_FOUND_ERROR)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping
  public Mono<ResponseEntity<Api<BRDCountDataResponse>>> getBrdList(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

    return brdService
        .getBrdList(page, size)
        .flatMap(
            response -> {
              if (!response.getBody().getData().isPresent()
                  || response.getBody().getData().get().getBrdList().isEmpty()) {
                return Mono.just(
                    ResponseEntity.ok(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "No BRDs found",
                            Optional.of(new BRDCountDataResponse(0, Collections.emptyList())),
                            Optional.empty())));
              }

              List<BRDListResponse> brds = response.getBody().getData().get().getBrdList();
              return Mono.just(brds)
                  .flatMapMany(Flux::fromIterable)
                  .filterWhen(
                      brd ->
                          securityService
                              .withSecurityCheck(brd.getStatus())
                              .thenReturn(true)
                              .onErrorResume(e -> Mono.just(false)))
                  .collectList()
                  .map(
                      filteredBrds -> {
                        // Get total count from the response metadata if available
                        int totalCount =
                            response
                                .getBody()
                                .getData()
                                .map(BRDCountDataResponse::getTotalCount)
                                .orElse(filteredBrds.size());

                        BRDCountDataResponse filteredResponse =
                            new BRDCountDataResponse(totalCount, filteredBrds);
                        return ResponseEntity.ok(
                            new Api<>(
                                BrdConstants.SUCCESSFUL,
                                "BRD list retrieved successfully",
                                Optional.of(filteredResponse),
                                Optional.empty()));
                      });
            });
  }

  @Operation(
      summary = "Search BRDs",
      description =
          "Search BRDs using comma-separated search terms. Returns all BRDs if no search text is provided.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                      @ExampleObject(
                          name = "Successful Search Response",
                          value = BRDRequestAndResponses.BRD_SEARCH_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.SEARCH_QUERY_VALIDATION_ERROR)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping("/search")
  public Mono<ResponseEntity<Api<Page<BRDSearchResponse>>>> searchBRDs(
      @Parameter(
              description =
                  "Comma-separated search terms. If not provided, returns all BRDs. "
                      + "For brdId/brdName: partial match, "
                      + "For creator/type/status: exact match",
              example = "BRD001,Draft,John")
          @RequestParam(required = false)
          String searchText,
      @Parameter(description = "Page number (0-based)", example = "0")
          @Min(value = 0, message = "Page number cannot be negative")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Number of records per page", example = "5")
          @Min(value = 1, message = "Page size must be at least 1")
          @Max(value = 100, message = "Page size must not exceed 100")
          @RequestParam(defaultValue = "5")
          int size,
      @Parameter(
              description = "Field to sort by",
              example = "brdId",
              schema = @Schema(allowableValues = {"brdId", "brdName", "creator", "type", "status"}))
          @Pattern(
              regexp = "^(brdId|brdName|creator|type|status)$",
              message = "Sort field must be one of: brdId, brdName, creator, type, status")
          @RequestParam(defaultValue = "brdId")
          String sortBy,
      @Parameter(
              description = "Sort direction",
              example = "ASC",
              schema = @Schema(allowableValues = {"ASC", "DESC"}))
          @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be either ASC or DESC")
          @RequestParam(defaultValue = "ASC")
          String sortDirection) {

    return brdService.searchBRDs(searchText, page, size, sortBy, sortDirection);
  }

  @Operation(
      summary = "Update BRD Status",
      description =
          "Updates the status field of an existing Business Requirement Document. "
              + "Only authorized users with appropriate permissions can update the status.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Status update request",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BrdStatusUpdateRequest.class),
              examples = {
                @ExampleObject(
                    name = "Status Update Request",
                    value =
                        """
                          {
                              "status": "IN_REVIEW"
                          }
                          """)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRD status updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BRDResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid status value or request format",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_BAD_REQUEST)
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication is required",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Unauthorized",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_UNAUTHORIZED)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have permission to update the BRD status",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Forbidden",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_FORBIDDEN)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - BRD with the specified ID does not exist",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_NOT_FOUND)
                    })),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error - An unexpected error occurred while processing the request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.BRD_STATUS_UPDATE_SERVER_ERROR)
                    }))
      })
  @PutMapping("/{brdFormId}/status")
  public Mono<ResponseEntity<Api<BRDResponse>>> updateBrdStatus(
      @Parameter(description = "BRD identifier", example = "BRD-1234", required = true)
          @NotBlank
          @PathVariable
          String brdFormId,
      @Valid @RequestBody BrdStatusUpdateRequest statusRequest) {
    return brdService
        .getBrdById(brdFormId)
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  response
                      .getBody()
                      .getData()
                      .orElseThrow(
                          () -> new NotFoundException("BRD not found with id: " + brdFormId));

              return securityService
                  .getCurrentUserRole()
                  .flatMap(
                      role ->
                          validateRoleAccess(role, brdResponse.getStatus())
                              .then(
                                  brdService.updateBrdStatus(
                                      brdFormId,
                                      statusRequest.getStatus(),
                                      statusRequest.getComment())));
            })
        .onErrorResume(this::handleErrorResponse);
  }

  private Mono<Void> validateRoleAccess(String role, String currentStatus) {
    if ("ROLE_PM".equals(role)) {
      return validatePMAccess(currentStatus);
    }

    if ("ROLE_BILLER".equals(role)) {
      return validateBillerAccess(currentStatus);
    }

    if ("ROLE_BA".equals(role)) {
      return validateBAAccess(currentStatus);
    }

    return Mono.error(
        new AccessDeniedException(
            "Only PM, Biller, and BA roles are allowed to update BRD status"));
  }

  private Mono<Void> validateBillerAccess(String currentStatus) {
    if (!"In Progress".equals(currentStatus) && !"Ready for Sign-Off".equals(currentStatus)) {
      return Mono.error(
          new AccessDeniedException(
              "Biller can only update BRD status when it is in 'In Progress/Ready for Sign-Off' state"));
    }
    return Mono.empty();
  }

  private Mono<Void> validateBAAccess(String currentStatus) {
    if (!"Internal Review".equals(currentStatus)) {
      return Mono.error(
          new AccessDeniedException(
              "BA can only update BRD status when it is in 'Internal Review' state"));
    }
    return Mono.empty();
  }

  private Mono<Void> validatePMAccess(String currentStatus) {
    if ("Internal Review".equals(currentStatus)
        || "In Progress".equals(currentStatus)
        || "Ready for Sign-Off".equals(currentStatus)) {
      return Mono.error(
          new AccessDeniedException(
              "PM can only update BRD status when it is not in 'Internal Review' or 'In Progress' or 'Ready for Sign-Off' state"));
    } else if ("Submit".equals(currentStatus)) {
      return Mono.error(
          new AccessDeniedException("BRD is already submitted, can't change the status of BRD"));
    }
    return Mono.empty();
  }

  private Mono<ResponseEntity<Api<BRDResponse>>> handleErrorResponse(Throwable e) {
    if (e instanceof AccessDeniedException) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    if (e instanceof NotFoundException) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    return Mono.just(
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                new Api<>(
                    BrdConstants.FAILURE,
                    "Invalid status update request",
                    Optional.empty(),
                    Optional.empty())));
  }

  @Operation(
      summary = "Get BRD status history",
      description =
          "Retrieves the complete status history for a specific BRD, including its creation event and all subsequent status changes. Results are ordered chronologically by timestamp.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status history retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value =
                              "{\"status\":\"Successful\",\"message\":\"BRD status history retrieved successfully\",\"data\":[{\"status\":\"Created\",\"timestamp\":\"Jan 8, 2025, 10:15 AM\",\"changedBy\":\"John Smith\"},{\"status\":\"In Review\",\"timestamp\":\"Jan 9, 2025, 3:30 PM\",\"changedBy\":\"Jane Doe\"}]}")
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found",
                          value = BRDRequestAndResponses.BRD_NOT_FOUND)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have permission to access BRD status history",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Forbidden",
                          value =
                              "{\"status\":\"failure\",\"message\":\"Access denied. Only authorized users can view BRD status history.\",\"data\":null}")
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Server Error",
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping("/{brdFormId}/status-history")
  public Mono<ResponseEntity<Api<List<StatusHistoryResponse>>>> getBrdStatusHistory(
      @Parameter(description = "BRD identifier", example = "BRD-1234", required = true)
          @NotBlank
          @PathVariable
          String brdFormId) {

    log.debug("Fetching status history for BRD with ID: {}", brdFormId);

    return brdService
        .getBrdById(brdFormId)
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  Objects.requireNonNull(response.getBody())
                      .getData()
                      .orElseThrow(
                          () -> new NotFoundException(BrdConstants.BRD_NOT_FOUND + brdFormId));

              log.debug("Found BRD with status: {}", brdResponse.getStatus());

              // Check if the user has permission to access this BRD
              return securityService
                  .withSecurityCheck(brdResponse.getStatus())
                  .then(
                      auditLogService
                          .fetchStatusHistory(
                              brdFormId,
                              List.of(
                                  BrdConstants.ACTION_STATUS_UPDATE, BrdConstants.ACTION_CREATE))
                          .map(
                              historyItems ->
                                  ResponseEntity.ok(
                                      new Api<>(
                                          BrdConstants.SUCCESSFUL,
                                          BrdConstants.STATUS_HISTORY_RETRIEVED_SUCCESS,
                                          Optional.of(historyItems),
                                          Optional.empty()))));
            })
        .doOnNext(
            response ->
                log.debug(
                    "Retrieved {} status history records for BRD {}",
                    Objects.requireNonNull(response.getBody()).getData().map(List::size).orElse(0),
                    brdFormId))
        .onErrorResume(this::handleStatusHistoryError);
  }

  private Mono<ResponseEntity<Api<List<StatusHistoryResponse>>>> handleStatusHistoryError(
      Throwable e) {
    log.debug("Handling error in status history retrieval: {}", e.getMessage());

    if (e instanceof AccessDeniedException) {
      log.debug("Access denied exception caught: {}", e.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    if (e instanceof NotFoundException) {
      log.debug("Not found exception caught: {}", e.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }

    log.error("Unexpected error occurred while retrieving status history: {}", e.getMessage(), e);
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    BrdConstants.FAILURE,
                    "An error occurred while processing your request: " + e.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  @Operation(
      summary = "Get Industry Verticals",
      description =
          "Retrieves all available industry verticals from the system. Only accessible by users with PM role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Industry verticals retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have PM role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  @GetMapping("/industry-verticals")
  public Mono<ResponseEntity<Api<List<String>>>> getIndustryVerticals() {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "Error",
                                "Access denied. Only PM role is allowed.",
                                Optional.empty(),
                                Optional.empty())));
              }
              return brdService
                  .getIndustryVerticals()
                  .map(
                      verticals ->
                          ResponseEntity.ok(
                              new Api<>(
                                  "Success",
                                  "Industry verticals retrieved successfully",
                                  Optional.of(verticals),
                                  Optional.empty())))
                  .onErrorResume(
                      error ->
                          Mono.just(
                              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(
                                      new Api<>(
                                          "Error",
                                          "Failed to retrieve industry verticals",
                                          Optional.empty(),
                                          Optional.empty()))));
            });
  }

  @Operation(
      summary = "Get BRDs by PM Username",
      description =
          "Retrieves all BRDs where the creator field matches the provided username. Only accessible by users with MANAGER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRDs retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BRDResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER role",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "404",
            description = "No BRDs found for the given PM username",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  @GetMapping("/assigned-to-pm/{username}")
  public Mono<ResponseEntity<Api<List<BRDResponse>>>> getBrdsByPmUsername(
      @Parameter(description = "PM's username", example = "john.doe@example.com", required = true)
          @NotBlank(message = "Username cannot be blank")
          @PathVariable
          String username) {

    return brdService
        .getBrdsByPmUsername(username)
        .map(
            brds ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRDs retrieved successfully",
                        Optional.of(brds),
                        Optional.empty())))
        .onErrorResume(
            error -> {
              if (error instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                error.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              } else if (error instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                error.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              "An error occurred while retrieving BRDs",
                              Optional.empty(),
                              Optional.empty())));
            });
  }
}
