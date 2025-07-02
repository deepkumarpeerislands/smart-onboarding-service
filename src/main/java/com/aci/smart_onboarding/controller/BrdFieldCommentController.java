package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBrdFieldCommentService;
import com.aci.smart_onboarding.service.IBAAssignmentService;
import com.aci.smart_onboarding.service.IBillerAssignmentService;
import com.aci.smart_onboarding.swagger.BrdFieldCommentRequestAndResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Validated
@RestController
@Tag(
    name = "Entity Field Comment Management",
    description = "APIs for managing field comments for BRDs and Sites")
@RequestMapping(value = "${api.default.path}/brd-field/comment", name = "Field Comment Management")
@RequiredArgsConstructor
public class BrdFieldCommentController {
  private final IBrdFieldCommentService fieldCommentService;
  private final IBRDService brdService;
  private final BRDSecurityService securityService;
  private final IBAAssignmentService baAssignmentService;
  private final IBillerAssignmentService billerAssignmentService;

  /**
   * Comprehensive method to validate fine-grained role and assignment-based access control for BRD comment operations.
   * Implements specific access rules for PM, Manager, Biller, and BA roles.
   *
   * @param brdResponse The BRD response containing the brdId, status, and creator
   * @return Mono that completes with the BRD response if access is granted, or errors with AccessDeniedException if denied
   */
  private Mono<BRDResponse> validateAccess(BRDResponse brdResponse) {
    return securityService
        .withSecurityCheck(brdResponse.getStatus())
        .then(
            securityService.getCurrentUserRole()
                .flatMap(role -> {
                  switch (role) {
                    case SecurityConstants.PM_ROLE:
                      return validatePMAccess(brdResponse);
                    case SecurityConstants.MANAGER_ROLE:
                      return validateManagerAccess();
                    case SecurityConstants.BILLER_ROLE:
                      return validateBillerAccess(brdResponse);
                    case SecurityConstants.BA_ROLE:
                      return validateBAAccessForRead(brdResponse);
                    default:
                      return Mono.error(new AccessDeniedException("Access denied: User role not authorized for comment operations"));
                  }
                }));
  }

  /**
   * Validates PM access - PM must be the creator of the BRD.
   */
  private Mono<BRDResponse> validatePMAccess(BRDResponse brdResponse) {
    return securityService.getCurrentUserEmail()
        .flatMap(email -> {
          if (email == null || !email.equalsIgnoreCase(brdResponse.getCreator())) {
            return Mono.error(new AccessDeniedException("Access denied: PM can only access BRDs they created"));
          }
          return Mono.just(brdResponse);
        });
  }

  /**
   * Validates Manager access - Managers are not allowed.
   */
  private Mono<BRDResponse> validateManagerAccess() {
    return Mono.error(new AccessDeniedException("Access denied: Managers are not allowed to access comment operations"));
  }

  /**
   * Validates Biller access - Biller must be assigned and BRD status must be 'in_progress' or 'ready_for_signoff'.
   */
  private Mono<BRDResponse> validateBillerAccess(BRDResponse brdResponse) {
    return securityService.getCurrentUserEmail()
        .flatMap(email -> billerAssignmentService.isBrdAssignedToBiller(brdResponse.getBrdId(), email)
            .flatMap(isAssigned -> {
              if (Boolean.FALSE.equals(isAssigned)) {
                return Mono.error(new AccessDeniedException("Access denied: Biller is not assigned to this BRD"));
              }
              
              // Check if BRD status is 'in_progress' or 'ready_for_signoff'
              String status = brdResponse.getStatus();
              if (!"In Progress".equals(status) && !"Ready for Sign-Off".equals(status)) {
                return Mono.error(new AccessDeniedException("Access denied: Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'"));
              }
              
              return Mono.just(brdResponse);
            }));
  }

  /**
   * Validates BA access for read operations - BA must be assigned and BRD status must be 'Internal Review'.
   */
  private Mono<BRDResponse> validateBAAccessForRead(BRDResponse brdResponse) {
    // Check if BRD status is "Internal Review"
    if (!"Internal Review".equals(brdResponse.getStatus())) {
      return Mono.error(new AccessDeniedException("Access denied: BRD status must be 'Internal Review' for BA operations"));
    }
    
    return securityService.getCurrentUserEmail()
        .flatMap(email -> baAssignmentService.isBAAssignedToUser(brdResponse.getBrdId(), email)
            .flatMap(isAssigned -> {
              if (Boolean.FALSE.equals(isAssigned)) {
                return Mono.error(new AccessDeniedException("Access denied: BA is not assigned to this BRD"));
              }
              return Mono.just(brdResponse);
            }));
  }

  @Operation(
      summary = "Create or update a field comment group",
      description =
          "Creates a new field comment group or updates an existing one if it already exists. Status can be either 'Pending' or 'Resolved'. " +
          "Access is controlled by fine-grained role and assignment-based rules: " +
          "PM: Must be the creator of the BRD. " +
          "Manager: Not allowed. " +
          "Biller: Must be assigned to the BRD and BRD status must be 'in_progress' or 'ready_for_signoff'. " +
          "BA: Must be assigned to the BRD and BRD status must be 'Internal Review'.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Field comment group details",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BrdFieldCommentGroupReq.class),
                      examples = {
                        @ExampleObject(
                            name = "BRD Request",
                            value = BrdFieldCommentRequestAndResponse.Request.GROUP_REQ),
                        @ExampleObject(
                            name = "SITE Request",
                            value = BrdFieldCommentRequestAndResponse.Request.GROUP_REQ_SITE)
                      })),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Field comment group created/updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "BRD Success Response",
                          value = BrdFieldCommentRequestAndResponse.Response.CREATE_GROUP_SUCCESS),
                      @ExampleObject(
                          name = "SITE Success Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.CREATE_GROUP_SUCCESS_SITE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid data provided",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.BAD_REQUEST_ERROR),
                      @ExampleObject(
                          name = "Missing SITE ID Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.MISSING_PARAMETER_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions based on role and assignment",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "PM Not Creator Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: PM can only access BRDs they created\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Manager Not Allowed Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Managers are not allowed to access comment operations\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BA is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BRD status must be 'Internal Review' for BA operations\",\"data\":null,\"errors\":null}")
                    }))
      })
  @PostMapping(value = "/group")
  public Mono<ResponseEntity<Api<BrdFieldCommentGroupResp>>> createOrUpdateFieldCommentGroup(
      @Valid @RequestBody BrdFieldCommentGroupReq groupReq) {

    return brdService
        .getBrdById(groupReq.getBrdFormId())
        .flatMap(
            response -> {
              BRDResponse brdResponse =
                  response
                      .getBody()
                      .getData()
                      .orElseThrow(
                          () ->
                              new NotFoundException(
                                  BrdConstants.BRD_NOT_FOUND + groupReq.getBrdFormId()));
              return validateAccess(brdResponse)
                  .flatMap(validatedBrd -> 
                      ReactiveSecurityContextHolder.getContext()
                          .map(context -> context.getAuthentication().getName())
                          .flatMap(
                              username -> {
                                // Set the createdBy field with the current username
                                groupReq.setCreatedBy(username);
                                return fieldCommentService.createOrUpdateFieldCommentGroup(
                                    groupReq);
                              }));
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
      summary = "Add a comment to a field",
      description =
          "Adds a new comment to an existing field comment group. If parentCommentId is provided, it will be added as a reply to that comment. Each userType is limited to a maximum of 5 comments per field. " +
          "Access is controlled by fine-grained role and assignment-based rules: " +
          "PM: Must be the creator of the BRD. " +
          "Manager: Not allowed. " +
          "Biller: Must be assigned to the BRD and BRD status must be 'in_progress' or 'ready_for_signoff'. " +
          "BA: Must be assigned to the BRD and BRD status must be 'Internal Review'.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Comment details",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = CommentEntryReq.class),
                      examples = {
                        @ExampleObject(
                            name = "New Comment",
                            value = BrdFieldCommentRequestAndResponse.Request.COMMENT_REQ),
                        @ExampleObject(
                            name = "Reply Comment",
                            value = BrdFieldCommentRequestAndResponse.Request.REPLY_REQ)
                      })),
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Comment added successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = BrdFieldCommentRequestAndResponse.Response.ADD_COMMENT_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Missing required fields or validation error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Missing Field Error",
                          value = BrdFieldCommentRequestAndResponse.Response.BAD_REQUEST_ERROR),
                      @ExampleObject(
                          name = "Comment Limit Error",
                          value = BrdFieldCommentRequestAndResponse.Response.COMMENT_LIMIT_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions based on role and assignment",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "PM Not Creator Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: PM can only access BRDs they created\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Manager Not Allowed Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Managers are not allowed to access comment operations\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BA is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BRD status must be 'Internal Review' for BA operations\",\"data\":null,\"errors\":null}")
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Field comment group not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR)
                    }))
      })
  @PostMapping("/{brdFormId}/{sourceType}/{sectionName}/{fieldPath}")
  public Mono<ResponseEntity<Api<CommentEntryResp>>> addComment(
      @Parameter(description = "BRD Form ID", required = true) @NotBlank @PathVariable
          String brdFormId,
      @Parameter(description = "Source type (BRD or SITE)", required = true)
          @NotBlank
          @Pattern(regexp = "^(BRD|SITE)$", message = "Source type must be either 'BRD' or 'SITE'")
          @PathVariable
          String sourceType,
      @Parameter(description = "Section name", required = true) @NotBlank @PathVariable
          String sectionName,
      @Parameter(description = "Field path", required = true) @NotBlank @PathVariable
          String fieldPath,
      @Parameter(description = "Site ID (required if sourceType is SITE)", in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String siteId,
      @Valid @RequestBody CommentEntryReq commentReq) {

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
              return validateAccess(brdResponse)
                  .flatMap(validatedBrd ->
                      fieldCommentService.addComment(
                          brdFormId, sectionName, fieldPath, commentReq, sourceType, siteId));
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
      summary = "Get comments with flexible filtering",
      description = "Retrieves comments with flexible filtering options. Access is controlled by fine-grained role and assignment-based rules: " +
          "PM: Must be the creator of the BRD. " +
          "Manager: Not allowed. " +
          "Biller: Must be assigned to the BRD and BRD status must be 'in_progress' or 'ready_for_signoff'. " +
          "BA: Must be assigned to the BRD and BRD status must be 'Internal Review'.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved comments",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = BrdFieldCommentRequestAndResponse.Response.GET_COMMENTS_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Missing sourceType parameter or other validation error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Missing Parameter Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.MISSING_PARAMETER_ERROR),
                      @ExampleObject(
                          name = "Invalid Parameter Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.INVALID_SOURCE_TYPE_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions based on role and assignment",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "PM Not Creator Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: PM can only access BRDs they created\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Manager Not Allowed Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Managers are not allowed to access comment operations\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BA is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BRD status must be 'Internal Review' for BA operations\",\"data\":null,\"errors\":null}")
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Comments not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR)
                    }))
      })
  @GetMapping("/{brdFormId}")
  public Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getComments(
      @Parameter(description = "BRD Form ID", required = true) @NotBlank @PathVariable
          String brdFormId,
      @Parameter(
              description = "Source type (must be either 'BRD' or 'SITE')",
              in = ParameterIn.QUERY,
              required = true)
          @NotBlank
          @RequestParam
          String sourceType,
      @Parameter(description = "Site ID (required if sourceType is SITE)", in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String siteId,
      @Parameter(description = "Section name (optional)", in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String sectionName,
      @Parameter(
              description = "Field path (optional, requires sectionName)",
              in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String fieldPath) {

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
              return validateAccess(brdResponse)
                  .flatMap(validatedBrd ->
                      fieldCommentService.getComments(
                          brdFormId, sourceType, siteId, sectionName, fieldPath));
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
      summary = "Get comment groups with statistics by status",
      description = BrdFieldCommentRequestAndResponse.Descriptions.GET_PENDING_COMMENTS,
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved comments with statistics by status",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.PENDING_COMMENTS_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Missing Parameter Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.MISSING_PARAMETER_ERROR)
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Authentication Error",
                          value = BrdFieldCommentRequestAndResponse.Response.UNAUTHORIZED_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions (PM role required)",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Permission Error",
                          value = BrdFieldCommentRequestAndResponse.Response.FORBIDDEN_ERROR)
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
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR)
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
                          value = BrdFieldCommentRequestAndResponse.Response.SERVER_ERROR)
                    }))
      })
  @GetMapping("/{status}/{brdFormId}")
  public Mono<ResponseEntity<Api<CommentStatsByStatusResponse>>> getCommentStatsByStatus(
      @Parameter(description = "Status (Pending or Resolved)", required = true)
          @NotBlank
          @PathVariable
          String status,
      @Parameter(description = "BRD Form ID", required = true) @NotBlank @PathVariable
          String brdFormId) {

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
                  .then(fieldCommentService.getCommentStatsByStatus(brdFormId, status));
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
      summary = "Get comment groups by source type and status",
      description = BrdFieldCommentRequestAndResponse.Descriptions.GET_COMMENTS_BY_SOURCE,
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved comments by source type",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "BRD Source Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response
                                  .COMMENTS_BY_SOURCE_SUCCESS),
                      @ExampleObject(
                          name = "SITE Source Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response
                                  .COMMENTS_BY_SOURCE_SUCCESS_SITE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Invalid Source Type Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.INVALID_SOURCE_TYPE_ERROR),
                      @ExampleObject(
                          name = "Missing Site ID Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.MISSING_PARAMETER_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Permission Error",
                          value = BrdFieldCommentRequestAndResponse.Response.FORBIDDEN_ERROR)
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
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR)
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
                          value = BrdFieldCommentRequestAndResponse.Response.SERVER_ERROR)
                    }))
      })
  @GetMapping("/source/{sourceType}/{brdFormId}")
  public Mono<ResponseEntity<Api<CommentsBySourceResponse>>> getCommentsBySource(
      @Parameter(description = "Source type (BRD or SITE)", required = true)
          @NotBlank
          @Pattern(regexp = "^(BRD|SITE)$", message = "Source type must be either 'BRD' or 'SITE'")
          @PathVariable
          String sourceType,
      @Parameter(description = "BRD Form ID", required = true) @NotBlank @PathVariable
          String brdFormId,
      @Parameter(description = "Site ID (required if sourceType is SITE)", in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String siteId,
      @Parameter(
              description = "Comment status (Pending or Resolved)",
              required = false,
              in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String status) {

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
                  .then(
                      fieldCommentService.getCommentsBySource(
                          brdFormId, sourceType, siteId, status));
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
      summary = "Update comment read status",
      description = BrdFieldCommentRequestAndResponse.Descriptions.UPDATE_COMMENT_READ_STATUS + " " +
          "Access is controlled by fine-grained role and assignment-based rules: " +
          "PM: Must be the creator of the BRD. " +
          "Manager: Not allowed. " +
          "Biller: Must be assigned to the BRD and BRD status must be 'in_progress' or 'ready_for_signoff'. " +
          "BA: Must be assigned to the BRD and BRD status must be 'Internal Review'.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Comment read status update details",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = UpdateCommentReadStatusRequest.class),
                      examples = {
                        @ExampleObject(
                            name = "Read Status Update Request",
                            value =
                                BrdFieldCommentRequestAndResponse.Request.READ_STATUS_UPDATE_REQ)
                      })),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Comment read status updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.READ_STATUS_UPDATE_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Invalid Parameter Error",
                          value = BrdFieldCommentRequestAndResponse.Response.BAD_REQUEST_ERROR),
                      @ExampleObject(
                          name = "Missing Site ID Error",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.MISSING_PARAMETER_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions based on role and assignment",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "PM Not Creator Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: PM can only access BRDs they created\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Manager Not Allowed Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Managers are not allowed to access comment operations\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "Biller Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Not Assigned Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BA is not assigned to this BRD\",\"data\":null,\"errors\":null}"),
                      @ExampleObject(
                          name = "BA Status Error",
                          value = "{\"status\":\"FAILURE\",\"message\":\"Access denied: BRD status must be 'Internal Review' for BA operations\",\"data\":null,\"errors\":null}")
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Comment or comment group not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Comment Group Not Found",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR),
                      @ExampleObject(
                          name = "Comment Not Found",
                          value =
                              BrdFieldCommentRequestAndResponse.Response.COMMENT_NOT_FOUND_ERROR)
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
                          value = BrdFieldCommentRequestAndResponse.Response.SERVER_ERROR)
                    }))
      })
  @PutMapping("/{sourceType}/{brdFormId}/{sectionName}/{fieldPath}/read-status")
  public Mono<ResponseEntity<Api<Boolean>>> updateCommentReadStatus(
      @Parameter(description = "Source type (BRD or SITE)", required = true)
          @NotBlank
          @Pattern(regexp = "^(BRD|SITE)$", message = "Source type must be either 'BRD' or 'SITE'")
          @PathVariable
          String sourceType,
      @Parameter(description = "BRD Form ID", required = true) @NotBlank @PathVariable
          String brdFormId,
      @Parameter(description = "Section name", required = true) @NotBlank @PathVariable
          String sectionName,
      @Parameter(description = "Field path", required = true) @NotBlank @PathVariable
          String fieldPath,
      @Parameter(description = "Site ID (required if sourceType is SITE)", in = ParameterIn.QUERY)
          @RequestParam(required = false)
          String siteId,
      @Valid @RequestBody UpdateCommentReadStatusRequest request) {

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
              return validateAccess(brdResponse)
                  .flatMap(validatedBrd ->
                      fieldCommentService.updateCommentReadStatus(
                          brdFormId, sourceType, siteId, sectionName, fieldPath, request));
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
      summary = "Update entity field from shadow value",
      description =
          "Updates a single field in a BRD or Site using the shadow value stored in the comment group.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Shadow value update details",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ShadowValueUpdateDTO.class),
                      examples = {
                        @ExampleObject(
                            name = "BRD Update Request",
                            value =
                                BrdFieldCommentRequestAndResponse.Request.SHADOW_VALUE_UPDATE_REQ),
                        @ExampleObject(
                            name = "SITE Update Request",
                            value =
                                BrdFieldCommentRequestAndResponse.Request
                                    .SHADOW_VALUE_UPDATE_REQ_SITE)
                      })),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Field updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              BrdFieldCommentRequestAndResponse.Response
                                  .UPDATE_SHADOW_VALUE_SUCCESS)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid parameters or shadow value",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.SHADOW_VALUE_ERROR)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Entity, field, or comment group not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.NOT_FOUND_ERROR)
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
                          name = "Error Response",
                          value = BrdFieldCommentRequestAndResponse.Response.BAD_REQUEST_ERROR)
                    }))
      })
  @PutMapping("/update-from-shadow")
  public Mono<ResponseEntity<Api<Boolean>>> updateEntityFieldFromShadowValue(
      @Valid @RequestBody Mono<ShadowValueUpdateDTO> updateDTO) {
    return updateDTO
        .flatMap(
            request ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!"ROLE_PM".equals(role)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    "Only Project Managers (PM) can update shadow value to BRD"));
                          }
                          return fieldCommentService.updateEntityFieldFromShadowValue(request);
                        }))
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
}
