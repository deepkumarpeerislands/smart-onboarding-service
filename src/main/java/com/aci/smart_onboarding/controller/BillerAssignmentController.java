package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBillerRequest;
import com.aci.smart_onboarding.dto.AssignBillerResponse;
import com.aci.smart_onboarding.dto.AuthorizationResponse;
import com.aci.smart_onboarding.dto.BRDCountDataResponse;
import com.aci.smart_onboarding.dto.BRDListResponse;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.UpdateBillerEmailRequest;
import com.aci.smart_onboarding.exception.BillerAssignmentException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BillerAssignment;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBillerAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller for managing biller assignments to BRDs. Provides endpoints for assigning billers and
 * managing their status.
 */
@RestController
@RequestMapping("/api/v1/brds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Biller Assignment", description = "APIs for managing biller assignments to BRDs")
@Validated
public class BillerAssignmentController {

  private static final String UNEXPECTED_ERROR_MESSAGE = "Unexpected error: {}";
  private static final String ACCESS_DENIED_MESSAGE = "Access denied: {}";

  private final IBillerAssignmentService billerAssignmentService;
  private final BRDRepository brdRepository;
  private final IBRDService brdService;
  private final BRDSecurityService brdSecurityService;
  private final DtoModelMapper dtoModelMapper;

  /**
   * Assigns a biller to a BRD and updates its status. Only users with PM role can access this
   * endpoint.
   *
   * @param brdId The ID of the BRD to assign the biller to
   * @param request The assignment request containing biller details and status
   * @return A Mono emitting the assignment response
   */
  @PostMapping("/{brdId}/assign-biller")
  @Operation(
      summary = "Assign a biller to a BRD",
      description = "Assigns a biller to a BRD and updates the BRD status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Biller assigned successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or biller assignment error",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<AssignBillerResponse>>> assignBiller(
      @PathVariable @NotBlank(message = "BRD ID cannot be empty") String brdId,
      @Valid @RequestBody AssignBillerRequest request) {
    return getBRDDetails(brdId)
        .flatMap(
            brdResponse -> {
              String status = getBRDStatus(brdResponse);

              return brdSecurityService
                  .withSecurityCheck(status)
                  .then(billerAssignmentService.assignBiller(brdId, request));
            })
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Biller assigned successfully",
                        Optional.of(response),
                        Optional.empty())))
        .onErrorResume(this::handleError);
  }

  /** Common error handling logic for controller endpoints */
  private <T> Mono<ResponseEntity<Api<T>>> handleError(Throwable e) {
    if (e instanceof AccessDeniedException) {
      log.warn(ACCESS_DENIED_MESSAGE, e.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    if (e instanceof NotFoundException) {
      log.warn("Not found error: {}", e.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    if (e instanceof BillerAssignmentException) {
      log.warn("Biller assignment error: {}", e.getMessage());
      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    log.error(UNEXPECTED_ERROR_MESSAGE, e.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    BrdConstants.FAILURE,
                    ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE + e.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  private Mono<ResponseEntity<Api<BRDResponse>>> getBRDDetails(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(String.format("BRD not found with id: %s", brdId))))
        .map(
            brd ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD Found",
                        Optional.of(dtoModelMapper.mapToBrdResponse(brd)),
                        Optional.empty())));
  }

  /**
   * Retrieves biller details for a specific BRD.
   *
   * @param brdId The ID of the BRD to get biller details for
   * @return A Mono emitting the biller details response
   */
  @GetMapping("/{brdId}/biller")
  @Operation(
      summary = "Get biller details for a BRD",
      description = "Retrieves the assigned biller details for a specific BRD")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Biller details retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found or biller not assigned",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<Map<String, String>>>> getBillerDetails(
      @PathVariable @NotBlank(message = "BRD ID cannot be empty") String brdId) {
    return getBRDDetails(brdId)
        .flatMap(
            brdResponse -> {
              String status = getBRDStatus(brdResponse);

              return brdSecurityService
                  .getCurrentUserRole()
                  .flatMap(
                      role -> {
                        if (!"ROLE_PM".equals(role)) {
                          return Mono.error(
                              new AccessDeniedException(
                                  "Only Project Managers (PM) can access biller details"));
                        }
                        return getBillerEmailDetails(brdId, status);
                      });
            })
        .map(BillerAssignmentController::billerDetailsToResponseEntity)
        .onErrorResume(this::handleError);
  }

  private static ResponseEntity<Api<Map<String, String>>> billerDetailsToResponseEntity(
      Map<String, String> billerDetails) {
    return ResponseEntity.ok(
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "Biller details retrieved successfully",
            Optional.of(billerDetails),
            Optional.empty()));
  }

  private Mono<Map<String, String>> getBillerEmailDetails(String brdId, String status) {
    return brdSecurityService
        .withSecurityCheck(status)
        .then(billerAssignmentService.getBillerDetails(brdId));
  }

  private static String getBRDStatus(ResponseEntity<Api<BRDResponse>> brdResponse) {
    if (brdResponse == null || brdResponse.getBody() == null) {
      throw new NotFoundException("BRD response or body is null");
    }
    Api<BRDResponse> body = brdResponse.getBody();

    Optional<BRDResponse> data = Optional.empty();
    if (body != null && body.getData().isPresent()) {
      data = body.getData();
    }
    return data.map(BRDResponse::getStatus)
        .orElseThrow(() -> new NotFoundException("BRD status not found"));
  }

  /**
   * Retrieves all unique biller email addresses from the biller assignments. Only accessible to
   * users with PM role.
   *
   * @return A Mono emitting a list of unique biller email addresses
   */
  @GetMapping("/biller-emails")
  @Operation(
      summary = "Get all biller emails",
      description =
          "Retrieves all unique biller email addresses from the biller assignments. Only accessible to PM role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Biller emails retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Only PM role can access this endpoint",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<List<String>>>> getBillerEmails() {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!"ROLE_PM".equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Only Project Managers (PM) can access biller emails"));
              }
              return billerAssignmentService.getAllBillerEmails();
            })
        .map(
            emails ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Biller emails retrieved successfully",
                        Optional.of(emails),
                        Optional.empty())))
        .onErrorResume(this::handleListError);
  }

  /** Error handler specifically for List response types */
  private <T> Mono<ResponseEntity<Api<T>>> handleListError(Throwable e) {
    if (e instanceof AccessDeniedException) {
      log.warn(ACCESS_DENIED_MESSAGE, e.getMessage());
      return Mono.just(
          ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(
                  new Api<>(
                      BrdConstants.FAILURE, e.getMessage(), Optional.empty(), Optional.empty())));
    }
    log.error(UNEXPECTED_ERROR_MESSAGE, e.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    BrdConstants.FAILURE,
                    ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE + e.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  /**
   * Updates or creates a biller assignment record for the given BRD ID. If a record exists, only
   * updates the updatedAt field. If no record exists, creates a new record with all required
   * fields.
   *
   * @param brdId The ID of the BRD to update/create biller assignment for
   * @param request The request containing the biller email
   * @return A Mono emitting the updated or created biller assignment
   */
  @PutMapping("/{brdId}/biller-email")
  @Operation(
      summary = "Update biller email for a BRD",
      description = "Updates or creates a biller assignment record for the given BRD ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Biller email updated successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<BillerAssignment>>> updateBillerEmail(
      @PathVariable @NotBlank String brdId, @Valid @RequestBody UpdateBillerEmailRequest request) {

    // Validate input parameters
    if (brdId == null || brdId.trim().isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<>(
                      BrdConstants.FAILURE,
                      "BRD ID cannot be empty",
                      Optional.empty(),
                      Optional.empty())));
    }

    if (request == null
        || request.getBillerEmail() == null
        || request.getBillerEmail().trim().isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<>(
                      BrdConstants.FAILURE,
                      "Biller email cannot be empty",
                      Optional.empty(),
                      Optional.empty())));
    }

    return billerAssignmentService
        .updateBillerEmail(brdId, request.getBillerEmail())
        .map(
            assignment ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Biller email updated successfully",
                        Optional.of(assignment),
                        Optional.empty())))
        .onErrorResume(
            e -> {
              log.error("Error updating biller email: {}", e.getMessage());
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE + e.getMessage(),
                              Optional.empty(),
                              Optional.empty())));
            });
  }

  /**
   * Check if the current user is authorized to access a BRD.
   *
   * @param brdId The ID of the BRD to check authorization for
   * @return A Mono emitting the authorization response
   */
  @Operation(
      summary = "Check BRD authorization",
      description = "Check if the current user is authorized to access a BRD")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authorization check successful",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthorizationResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = "{\"authorized\": false, \"message\": \"Not authorized\"}"))),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            value = "{\"authorized\": false, \"message\": \"BRD not found\"}")))
      })
  @GetMapping("/check-biller-authorization/{brdId}")
  public Mono<ResponseEntity<Api<AuthorizationResponse>>> checkBrdAuthorization(
      @Parameter(description = "BRD ID to check authorization for", required = true)
          @PathVariable("brdId")
          @NotBlank(message = ErrorValidationMessage.BRD_ID_REQUIRED)
          String brdId) {
    log.info("Entering method: BillerAssignmentController.checkBrdAuthorization");
    log.debug("Checking authorization for BRD: {}", brdId);

    return billerAssignmentService
        .checkBrdAuthorization(brdId)
        .map(
            apiResponse -> {
              HttpStatus status =
                  apiResponse
                      .getData()
                      .map(
                          authResponse ->
                              authResponse.isAuthorized() ? HttpStatus.OK : HttpStatus.FORBIDDEN)
                      .orElse(HttpStatus.FORBIDDEN);

              return ResponseEntity.status(status).body(apiResponse);
            })
        .doOnSuccess(
            response ->
                log.debug("Exiting method: BillerAssignmentController.checkBrdAuthorization"));
  }

  /**
   * Retrieves all BRDs assigned to the current logged-in biller. Only accessible to users with BILLER role.
   *
   * @return A Mono emitting a list of BRDs assigned to the current biller
   */
  @GetMapping("/biller-email")
  @Operation(
      summary = "Get BRDs by current biller email",
      description = "Retrieves all BRDs assigned to the current logged-in biller. Only accessible to BILLER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BRDs retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Only BILLER role can access this endpoint",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<BRDCountDataResponse>>> getBrdsByCurrentBillerEmail() {
    log.info("Getting BRDs for current biller email");
    
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.BILLER_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Only Billers (BILLER) can access BRD assignments by biller email"));
              }
              return billerAssignmentService.getBrdsByCurrentBillerEmail();
            })
        .map(
            brdCountDataResponse ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRDs retrieved successfully for current biller",
                        Optional.of(brdCountDataResponse),
                        Optional.empty())))
        .onErrorResume(this::handleListError);
  }
}
