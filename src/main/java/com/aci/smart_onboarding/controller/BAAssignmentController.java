package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBARequest;
import com.aci.smart_onboarding.dto.AssignBAResponse;
import com.aci.smart_onboarding.dto.BAReassignmentRequest;
import com.aci.smart_onboarding.dto.BAAssignmentStatusResponse;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.UpdateBAEmailRequest;
import com.aci.smart_onboarding.exception.BAAssignmentException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BAAssignment;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBAAssignmentService;
import com.aci.smart_onboarding.service.IBRDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for managing BA assignments to BRDs. Provides endpoints for assigning BAs and managing
 * their status.
 */
@RestController
@RequestMapping("/api/v1/brds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BA Assignment", description = "APIs for managing BA assignments to BRDs")
public class BAAssignmentController {

  private final IBAAssignmentService baAssignmentService;
  private final BRDRepository brdRepository;
  private final IBRDService brdService;
  private final BRDSecurityService brdSecurityService;

  /**
   * Assigns a BA to a BRD and updates its status. Only users with BA role can access this endpoint.
   *
   * @param brdId The ID of the BRD to assign the BA to
   * @param request The assignment request containing BA details and status
   * @return A Mono emitting the assignment response
   */
  @PostMapping("/{brdId}/assign-ba")
  @Operation(
      summary = "Assign a BA to a BRD",
      description = "Assigns a BA to a BRD and updates the BRD status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BA assigned successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or BA assignment error",
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
  public Mono<ResponseEntity<Api<AssignBAResponse>>> assignBA(
      @PathVariable @NotBlank(message = "BRD ID cannot be empty") String brdId,
      @Valid @RequestBody AssignBARequest request) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(String.format("BRD not found with id: %s", brdId))))
        .flatMap(brd -> brdService.getBrdById(brd.getBrdFormId()))
        .flatMap(
            brdResponse -> {
              String status =
                  brdResponse
                      .getBody()
                      .getData()
                      .map(BRDResponse::getStatus)
                      .orElseThrow(() -> new NotFoundException("BRD status not found"));

              return brdSecurityService
                  .withSecurityCheck(status)
                  .then(baAssignmentService.assignBA(brdId, request));
            })
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BA assigned successfully",
                        Optional.of(response),
                        Optional.empty())))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                log.warn("Access denied: {}", e.getMessage());
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
                log.warn("Not found error: {}", e.getMessage());
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              if (e instanceof BAAssignmentException) {
                log.warn("BA assignment error: {}", e.getMessage());
                return Mono.just(
                    ResponseEntity.badRequest()
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              log.error("Unexpected error: {}", e.getMessage());
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
   * Retrieves all unique BA email addresses. Only accessible to users with PM role.
   *
   * @return A Mono emitting a list of unique BA email addresses
   */
  @GetMapping("/ba-emails")
  @Operation(
      summary = "Get all BA email addresses",
      description =
          "Retrieves all unique BA email addresses. Only accessible to users with PM role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BA emails retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have PM role",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<List<String>>>> getBaEmails() {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!"ROLE_PM".equals(role)) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                "Only Project Managers (PM) can access BA emails",
                                Optional.empty(),
                                Optional.empty())));
              }
              return baAssignmentService
                  .getAllBaEmails()
                  .map(
                      emails ->
                          ResponseEntity.ok(
                              new Api<>(
                                  BrdConstants.SUCCESSFUL,
                                  "BA emails retrieved successfully",
                                  Optional.of(emails),
                                  Optional.empty())))
                  .onErrorResume(
                      e -> {
                        log.error("Error retrieving BA emails: {}", e.getMessage());
                        return Mono.just(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(
                                    new Api<>(
                                        BrdConstants.FAILURE,
                                        ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE
                                            + e.getMessage(),
                                        Optional.empty(),
                                        Optional.empty())));
                      });
            });
  }

  /**
   * Updates or creates a BA assignment record for the given BRD ID. If a record exists, only
   * updates the updatedAt field. If no record exists, creates a new record with all required
   * fields.
   *
   * @param brdId The ID of the BRD to update/create BA assignment for
   * @param request The request containing the BA email
   * @return A Mono emitting the updated or created BA assignment
   */
  @PutMapping("/{brdId}/ba-email")
  @Operation(
      summary = "Update BA email for a BRD",
      description = "Updates or creates a BA assignment record for the given BRD ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BA email updated successfully",
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
  public Mono<ResponseEntity<Api<BAAssignment>>> updateBaEmail(
      @PathVariable @NotBlank String brdId, @Valid @RequestBody UpdateBAEmailRequest request) {

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

    if (request == null || request.getBaEmail() == null || request.getBaEmail().trim().isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<>(
                      BrdConstants.FAILURE,
                      "BA email cannot be empty",
                      Optional.empty(),
                      Optional.empty())));
    }

    return baAssignmentService
        .updateBaEmail(brdId, request.getBaEmail())
        .map(
            assignment ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BA email updated successfully",
                        Optional.of(assignment),
                        Optional.empty())))
        .onErrorResume(
            e -> {
              log.error("Error updating BA email: {}", e.getMessage());
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
   * Retrieves all BA assignments for a given BA username (email). Only accessible by users with
   * MANAGER role.
   *
   * @param username The email of the BA to find assignments for
   * @return A Mono emitting a list of BA assignments
   */
  @GetMapping("/assigned-to-ba/{username}")
  @Operation(
      summary = "Get BA assignments by username",
      description =
          "Retrieves all BA assignments for a given BA username (email). Only accessible by users with MANAGER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BA assignments retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid username format",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have MANAGER role",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<List<BAAssignment>>>> getAssignmentsByBaUsername(
      @Parameter(description = "BA's username (email)", example = "ba@example.com", required = true)
          @NotBlank(message = "Username cannot be blank")
          @PathVariable
          String username) {

    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!"ROLE_MANAGER".equals(role)) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                "Only Managers can view BA assignments",
                                Optional.empty(),
                                Optional.empty())));
              }
              return baAssignmentService
                  .getAssignmentsByBaUsername(username)
                  .map(
                      assignments ->
                          ResponseEntity.ok(
                              new Api<>(
                                  BrdConstants.SUCCESSFUL,
                                  "BA assignments retrieved successfully",
                                  Optional.of(assignments),
                                  Optional.empty())))
                  .onErrorResume(
                      e -> {
                        if (e instanceof BadRequestException) {
                          return Mono.just(
                              ResponseEntity.badRequest()
                                  .body(
                                      new Api<>(
                                          BrdConstants.FAILURE,
                                          e.getMessage(),
                                          Optional.empty(),
                                          Optional.empty())));
                        }
                        log.error("Error retrieving BA assignments: {}", e.getMessage());
                        return Mono.just(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(
                                    new Api<>(
                                        BrdConstants.FAILURE,
                                        ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE
                                            + e.getMessage(),
                                        Optional.empty(),
                                        Optional.empty())));
                      });
            });
  }

  @PutMapping(value = "/reassign-ba")
  @Operation(
      summary = "Reassign Business Analyst for multiple BRDs",
      description =
          "Updates the baEmail field in the ba_assignment collection with new Business Analysts for multiple BRDs. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Business Analysts reassigned successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value =
                          """
                  {
                      "status": "success",
                      "message": "All BAs reassigned successfully",
                      "data": null,
                      "errors": null
                  }
                  """)))
  public Mono<ResponseEntity<Api<List<String>>>> reassignBA(
      @Valid @RequestBody List<BAReassignmentRequest> requests) {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                "This endpoint is only accessible to users with MANAGER role",
                                Optional.empty(),
                                Optional.empty())));
              }

              return baAssignmentService
                  .reassignBAs(requests)
                  .map(
                      response -> {
                        if (BrdConstants.FAILURE.equals(response.getStatus())) {
                          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                        }
                        return ResponseEntity.ok(response);
                      });
            });
  }

  /**
   * Checks if the current BA user is assigned to a specific BRD.
   * Only accessible to users with BA role.
   *
   * @param brdId The ID of the BRD to check for BA assignment
   * @return A Mono emitting the assignment status response
   */
  @GetMapping("/{brdId}/is-ba-assigned")
  @Operation(
      summary = "Check if current BA is assigned to BRD",
      description = "Checks if the current Business Analyst user is assigned to the specified BRD. Only accessible to users with BA role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Assignment status retrieved successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid BRD ID",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have BA role",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> isBAAssigned(
      @PathVariable @NotBlank(message = "BRD ID cannot be empty") String brdId) {
    
    // Validate input parameters
    if (brdId == null || brdId.trim().isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest()
              .body(
                  new Api<BAAssignmentStatusResponse>(
                      BrdConstants.FAILURE,
                      "BRD ID cannot be empty",
                      Optional.<BAAssignmentStatusResponse>empty(),
                      Optional.<Map<String, String>>empty())));
    }

    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.BA_ROLE.equals(role)) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<BAAssignmentStatusResponse>(
                                BrdConstants.FAILURE,
                                "This endpoint is only accessible to users with BA role",
                                Optional.<BAAssignmentStatusResponse>empty(),
                                Optional.<Map<String, String>>empty())));
              }
              
              return brdSecurityService
                  .getCurrentUserEmail()
                  .flatMap(
                      username ->
                          baAssignmentService.isBAAssignedToUser(brdId, username)
                              .map(
                                  isAssigned -> {
                                    BAAssignmentStatusResponse response = BAAssignmentStatusResponse.builder()
                                        .isAssigned(isAssigned)
                                        .build();
                                    
                                    return ResponseEntity.ok(
                                        new Api<BAAssignmentStatusResponse>(
                                            BrdConstants.SUCCESSFUL,
                                            "BA assignment status retrieved successfully",
                                            Optional.of(response),
                                            Optional.<Map<String, String>>empty()));
                                  }));
            })
        .onErrorResume(e -> {
          if (e instanceof BadRequestException) {
            log.warn("Bad request error: {}", e.getMessage());
            return Mono.just(
                ResponseEntity.badRequest()
                    .body(
                        new Api<BAAssignmentStatusResponse>(
                            BrdConstants.FAILURE,
                            e.getMessage(),
                            Optional.<BAAssignmentStatusResponse>empty(),
                            Optional.<Map<String, String>>empty())));
          }
          log.error("Error checking BA assignment: {}", e.getMessage());
          return Mono.just(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(
                      new Api<BAAssignmentStatusResponse>(
                          BrdConstants.FAILURE,
                          ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE + e.getMessage(),
                          Optional.<BAAssignmentStatusResponse>empty(),
                          Optional.<Map<String, String>>empty())));
        });
  }
}
