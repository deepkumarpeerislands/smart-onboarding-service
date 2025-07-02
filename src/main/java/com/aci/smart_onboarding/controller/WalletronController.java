package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.WalletronImagesResponseDTO;
import com.aci.smart_onboarding.dto.WalletronRequest;
import com.aci.smart_onboarding.dto.WalletronResponse;
import com.aci.smart_onboarding.dto.WalletronUsersRequest;
import com.aci.smart_onboarding.dto.WalletronUsersResponse;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IWalletronService;
import com.aci.smart_onboarding.swagger.WalletronRequestAndResponses;
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
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "Walletron Management", description = "APIs for managing Walletron onboarding data")
@RequestMapping(value = "${api.default.path}/walletron", name = "Walletron Management")
public class WalletronController {

  private final IWalletronService walletronService;
  private final BRDSecurityService securityService;

  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_ERROR = "ERROR";
  private static final String WAL_SAVE_SUCCESS = "Walletron data saved successfully";
  private static final String WAL_SAVE_ERROR = "Failed to save Walletron data";

  @Operation(
      summary = "Create a new Walletron form",
      description = "Creates a new Walletron onboarding form with the provided details",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Walletron form details",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = WalletronRequest.class),
                      examples =
                          @ExampleObject(
                              name = "WalletronRequest",
                              value =
                                  com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                      .WALLETRON_REQUEST_EXAMPLE))))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Walletron form created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Success",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "ValidationError",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_VALIDATION_ERROR))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "Forbidden",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "409",
            description = "Already exists",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "AlreadyExists",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_ALREADY_EXISTS))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "ServerError",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_INTERNAL_SERVER_ERROR)))
      })
  @PostMapping
  public Mono<ResponseEntity<Api<WalletronResponse>>> createWalletron(
      @Valid @RequestBody WalletronRequest walletronRequest) {
    return Mono.justOrEmpty(walletronRequest)
        .flatMap(
            request ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    "Only Project Managers (PM) can create new Walletron forms"));
                          }
                          return walletronService.createWalletron(request);
                        }));
  }

  @Operation(
      summary = "Update Walletron partially",
      description = "Updates specific fields of an existing Walletron onboarding form",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Walletron fields to update",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = WalletronRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Sample Partial Update",
                              value =
                                  com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                      .WALLETRON_REQUEST_EXAMPLE))))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Walletron updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Successful",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Validation Error",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_VALIDATION_ERROR))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Forbidden",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "404",
            description = "Walletron not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Not Found",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_NOT_FOUND))),
        @ApiResponse(
            responseCode = "409",
            description = "Walletron already exists",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Already exists",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_ALREADY_EXISTS))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Server Error",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_INTERNAL_SERVER_ERROR)))
      })
  @PatchMapping("/{walletronId}")
  public Mono<ResponseEntity<Api<WalletronResponse>>> updateWalletronPartially(
      @Parameter(description = "Walletron identifier", example = "WAL-1234", required = true)
          @NotBlank
          @PathVariable
          String walletronId,
      @RequestBody Mono<Map<String, Object>> patchFields) {

    return walletronService
        .getWalletronById(walletronId)
        .flatMap(
            response ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    "Only Project Managers (PM) can update Walletron forms"));
                          }
                          return patchFields.flatMap(
                              f ->
                                  walletronService.updateWalletronPartiallyWithOrderedOperations(
                                      walletronId, f));
                        }))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                STATUS_ERROR, e.getMessage(), Optional.empty(), Optional.empty())));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get Walletron by walletronId",
      description = "Fetches a Walletron onboarding form by its walletronId",
      parameters = {
        @Parameter(
            name = "walletronId",
            description = "Walletron identifier",
            required = true,
            example = "WALLET-001")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Walletron found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Success",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "404",
            description = "Walletron not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "Not Found",
                            value =
                                com.aci.smart_onboarding.swagger.WalletronRequestAndResponses
                                    .WALLETRON_NOT_FOUND)))
      })
  @GetMapping("/{walletronId}")
  public Mono<ResponseEntity<Api<WalletronResponse>>> getWalletronById(
      @Parameter(description = "Walletron identifier", example = "WALLET-001", required = true)
          @NotBlank
          @PathVariable
          String walletronId) {
    return walletronService.getWalletronById(walletronId);
  }

  @Operation(
      summary = "Get a specific section of Walletron by walletronId and section name",
      description =
          "Fetches a specific section (e.g., siteConfiguration, notificationsOptions) of a Walletron onboarding form by its walletronId",
      parameters = {
        @Parameter(
            name = "walletronId",
            description = "Walletron identifier",
            required = true,
            example = "WALLET-001"),
        @Parameter(
            name = "section",
            description =
                "Section name (e.g., siteConfiguration, notificationsOptions, aciCash, etc.)",
            required = true)
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Section found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "Section",
                            value = "{ \"siteConfiguration\": { ... } }"))),
        @ApiResponse(
            responseCode = "404",
            description = "Walletron or section not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "Not Found",
                            value =
                                "{ \"status\": \"failure\", \"message\": \"Section not found\" }")))
      })
  @GetMapping("/{walletronId}/section/{section}")
  public Mono<ResponseEntity<Map<String, Object>>> getWalletronSection(
      @Parameter(description = "Walletron identifier", example = "WALLET-001", required = true)
          @NotBlank
          @PathVariable
          String walletronId,
      @Parameter(description = "Section name", required = true) @NotBlank @PathVariable
          String section) {
    return walletronService.getWalletronSectionById(walletronId, section);
  }

  @Operation(
      summary = "Get Walletron images",
      description = "Retrieves all images associated with a Walletron by its ID",
      parameters = {
        @Parameter(
            name = "walletronId",
            description = "Walletron identifier",
            required = true,
            example = "WALLET-001")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Images retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Success",
                            value =
                                """
    {
      "status": "SUCCESS",
      "message": "Walletron images retrieved successfully",
      "data": {
        "images": {
          "Brand Logo": [base64 bytes],
          "Icon Image": [base64 bytes],
          "Strip Image": [base64 bytes],
          "Thumbnail Image": [base64 bytes],
          "Agent Portal Logo": [base64 bytes],
          "Notification Logo": [base64 bytes]
        }
      },
      "error": null
    }
    """))),
        @ApiResponse(
            responseCode = "404",
            description = "Walletron not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples =
                        @ExampleObject(
                            name = "Not Found",
                            value =
                                """
    {
      "status": "ERROR",
      "message": "Walletron not found with id: WALLET-001",
      "data": null,
      "error": {
        "error": "Walletron not found with id: WALLET-001"
      }
    }
    """)))
      })
  @GetMapping("/{walletronId}/images")
  public Mono<ResponseEntity<Api<WalletronImagesResponseDTO>>> getWalletronImages(
      @Parameter(description = "Walletron identifier", example = "WALLET-001", required = true)
          @NotBlank
          @PathVariable
          String walletronId) {
    return walletronService.getWalletronImages(walletronId);
  }

  @Operation(
      summary = "Create Walletron Users (JSON)",
      description =
          "Creates multiple Walletron users from JSON request with duplicate email filtering. Only the first occurrence of each email will be saved.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "JSON request with users list",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = WalletronUsersRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Create Walletron Users Request",
                              summary = "Example JSON request to create multiple Walletron users",
                              description =
                                  "Request body with walletronId, brdId, and list of users",
                              value = WalletronRequestAndResponses.WALLETRON_USERS_JSON_REQUEST))),
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Users processed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Success Response",
                            summary = "Successful creation with duplicate filtering",
                            description =
                                "Shows saved users, duplicate emails, and processing summary",
                            value =
                                WalletronRequestAndResponses.WALLETRON_USERS_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation errors",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Empty Users List",
                          summary = "Users list cannot be empty",
                          value = WalletronRequestAndResponses.WALLETRON_USERS_VALIDATION_ERROR),
                      @ExampleObject(
                          name = "Invalid Email Format",
                          summary = "Email validation failed",
                          value = WalletronRequestAndResponses.WALLETRON_USERS_INVALID_EMAIL_ERROR),
                      @ExampleObject(
                          name = "Missing Required Field",
                          summary = "Required field validation failed",
                          value =
                              WalletronRequestAndResponses
                                  .WALLETRON_USERS_MISSING_REQUIRED_FIELD_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Only PM role can create users",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Access Denied",
                            summary = "Only Project Managers can create Walletron users",
                            value = WalletronRequestAndResponses.WALLETRON_USERS_FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Server Error",
                            summary = "Internal server error occurred",
                            value =
                                WalletronRequestAndResponses
                                    .WALLETRON_USERS_INTERNAL_SERVER_ERROR)))
      })
  @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Api<WalletronUsersResponse>>> createWalletronUsers(
      @Valid @RequestBody WalletronUsersRequest walletronUsersRequest) {

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Only Project Managers (PM) can create Walletron users"));
              }

              return walletronService.createWalletronUsers(walletronUsersRequest);
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                STATUS_ERROR, e.getMessage(), Optional.empty(), Optional.empty())));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Upload Walletron Users (File)",
      description =
          "Uploads and processes a CSV or Excel file containing Walletron users. Supports case-insensitive headers and BOM removal. File size limit: 2MB.",
      parameters = {
        @Parameter(
            name = "walletronId",
            description = "Walletron identifier",
            required = true,
            example = "WAL-002",
            schema = @Schema(type = "string")),
        @Parameter(
            name = "brdId",
            description = "BRD identifier",
            required = true,
            example = "BRD-002",
            schema = @Schema(type = "string"))
      },
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "File processed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "File Upload Success",
                            summary = "Successful file processing with user creation",
                            description = "Shows all users created from the uploaded file",
                            value =
                                WalletronRequestAndResponses
                                    .WALLETRON_USERS_FILE_UPLOAD_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - File validation errors",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "File Too Large",
                          summary = "File exceeds 2MB limit",
                          value =
                              WalletronRequestAndResponses.WALLETRON_USERS_FILE_TOO_LARGE_ERROR),
                      @ExampleObject(
                          name = "Missing Headers",
                          summary = "Required headers not found in file",
                          value =
                              WalletronRequestAndResponses.WALLETRON_USERS_MISSING_HEADERS_ERROR),
                      @ExampleObject(
                          name = "Unsupported File Format",
                          summary = "File format not supported",
                          value =
                              WalletronRequestAndResponses
                                  .WALLETRON_USERS_UNSUPPORTED_FILE_FORMAT_ERROR),
                      @ExampleObject(
                          name = "Invalid Email Format",
                          summary = "Email validation failed in file data",
                          value = WalletronRequestAndResponses.WALLETRON_USERS_INVALID_EMAIL_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Only PM role can upload users",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Access Denied",
                            summary = "Only Project Managers can upload Walletron users",
                            value = WalletronRequestAndResponses.WALLETRON_USERS_FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - All users already exist for this walletronId",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "All Users Already Exist",
                            summary = "All user details already exist for this walletronId",
                            value = WalletronRequestAndResponses.WALLETRON_USERS_ALL_EXIST_ERROR))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - WalletronId not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Walletron Not Found",
                            summary = "Walletron not found with the provided ID",
                            value = WalletronRequestAndResponses.WALLETRON_NOT_FOUND))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Server Error",
                            summary = "Internal server error during file processing",
                            value =
                                WalletronRequestAndResponses
                                    .WALLETRON_USERS_INTERNAL_SERVER_ERROR)))
      })
  @PostMapping(value = "/users/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<Api<WalletronUsersResponse>>> uploadWalletronUsers(
      @Parameter(
              description =
                  "CSV or Excel file with user data. Required headers: name, email, role (case-insensitive)",
              required = true)
          @RequestPart("file")
          FilePart filePart,
      @Parameter(description = "Walletron ID", required = true)
          @RequestParam("walletronId")
          @NotBlank
          String walletronId,
      @Parameter(description = "BRD ID", required = true) @RequestParam("brdId") @NotBlank
          String brdId) {

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Only Project Managers (PM) can create Walletron users"));
              }

              // First validate that walletronId and brdId exist in walletron collection
              return walletronService
                  .validateWalletronExists(walletronId, brdId)
                  .flatMap(
                      validated ->
                          walletronService.createWalletronUsersFromFile(
                              filePart, walletronId, brdId));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                STATUS_ERROR, e.getMessage(), Optional.empty(), Optional.empty())));
              } else if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                STATUS_ERROR, e.getMessage(), Optional.empty(), Optional.empty())));
              }
              return Mono.error(e);
            });
  }
}
