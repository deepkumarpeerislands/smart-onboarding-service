package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ImageFileUploadResponse;
import com.aci.smart_onboarding.dto.JsonFileUploadResponse;
import com.aci.smart_onboarding.enums.FileType;
import com.aci.smart_onboarding.exception.DecryptionException;
import com.aci.smart_onboarding.exception.EncryptionException;
import com.aci.smart_onboarding.exception.InvalidFileException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IFileService;
import com.aci.smart_onboarding.swagger.FileRequestAndResponse;
import com.aci.smart_onboarding.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Controller for file operations including upload, download, and listing. */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "File Management", description = "APIs for file operations")
@RequestMapping(value = "${api.default.path}/files", name = "File Management")
public class FileController {

  private final FileValidator fileValidator;
  private final IFileService fileService;
  private final BRDSecurityService securityService;

  @Operation(
      summary = "Upload a file",
      description = "Allows users to upload files to blob storage after validation and encryption",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File uploaded successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format or size",
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
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<Api<String>>> uploadFile(
      @Parameter(description = "File to upload", required = true) @RequestPart("file") @NotNull
          FilePart filePart,
      @Parameter(description = "Type of file being uploaded", required = true)
          @RequestPart("fileType")
          @NotNull
          String fileType,
      @Parameter(description = "BRD ID to associate the file with", required = true)
          @RequestPart("brdId")
          @NotNull
          String brdId) {

    log.info(
        "File upload request received: {}, type: {}, brdId: {}, size reported: {}",
        filePart.filename(),
        fileType,
        brdId,
        filePart.headers().getContentLength());

    // Validate file type
    try {
      FileType.valueOf(fileType.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Mono.just(
          buildResponse(
              HttpStatus.BAD_REQUEST,
              "Invalid file type. Allowed types: " + Arrays.toString(FileType.values()),
              Optional.empty(),
              Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, "Invalid file type"))));
    }

    // Read the file content first, then validate and process
    return readFileContent(filePart)
        .doOnNext(
            fileBytes -> {
              // Log file size and first bytes for debugging
              StringBuilder firstBytesHex = new StringBuilder();
              for (int i = 0; i < Math.min(fileBytes.length, 8); i++) {
                firstBytesHex.append(String.format("%02X ", fileBytes[i] & 0xFF));
              }
              log.info(
                  "File content read successfully: {}, actual size: {}, first bytes: {}",
                  filePart.filename(),
                  fileBytes.length,
                  firstBytesHex.toString());
            })
        .flatMap(
            fileBytes ->
                validateFileContent(filePart, fileBytes)
                    // Then upload if validation passes
                    .then(fileService.uploadEncryptedFile(filePart, fileBytes, fileType, brdId)))
        .map(
            url -> {
              log.info("File uploaded successfully: {}, URL: {}", filePart.filename(), url);
              return buildResponse(
                  HttpStatus.OK, "File uploaded successfully", Optional.of(url), Optional.empty());
            })
        .onErrorResume(
            InvalidFileException.class,
            e -> {
              log.warn(
                  "File validation failed: {}, reason: {}", filePart.filename(), e.getMessage());
              return Mono.just(
                  buildResponse(
                      HttpStatus.BAD_REQUEST,
                      e.getMessage(),
                      Optional.empty(),
                      Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage()))));
            })
        .onErrorResume(
            Exception.class,
            e -> {
              String errorMessage = "Error processing file";

              // Don't expose encryption details in the response
              if (!(e instanceof EncryptionException)) {
                errorMessage += ": " + e.getMessage();
              }

              log.error("File upload error: {}, error: {}", filePart.filename(), errorMessage, e);

              return Mono.just(
                  buildResponse(
                      HttpStatus.INTERNAL_SERVER_ERROR,
                      errorMessage,
                      Optional.empty(),
                      Optional.of(
                          Map.of(ErrorValidationMessage.ERROR_KEY, "File processing failed"))));
            });
  }

  /** Helper method to validate file content */
  private Mono<Void> validateFileContent(FilePart filePart, byte[] fileBytes) {
    if (fileBytes == null || fileBytes.length == 0) {
      log.warn("Upload attempted with empty file content: {}", filePart.filename());
      return Mono.error(new InvalidFileException("File content is empty"));
    }

    try {
      // Perform validation directly instead of reading the file again
      FileValidator.validateFileName(filePart.filename());
      FileValidator.validateFileExtension(filePart.filename());
      FileValidator.validateFileSize(fileBytes.length);
      FileValidator.validateMimeType(fileBytes);
      FileValidator.validatePdfSignature(fileBytes);
      return Mono.empty();
    } catch (Exception e) {
      // Catch any exceptions and wrap them consistently
      String errorMessage = e.getMessage();
      if (errorMessage == null || errorMessage.isEmpty()) {
        errorMessage = "PDF file validation failed";
      }
      log.warn("PDF file validation failed: {}", errorMessage);

      // Return a consistent InvalidFileException
      if (e instanceof InvalidFileException) {
        return Mono.error(e);
      } else {
        return Mono.error(new InvalidFileException(errorMessage));
      }
    }
  }

  /** Downloads a file by its original name */
  @Operation(
      summary = "Download a file",
      description = "Downloads and decrypts a file from blob storage using the original file name",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
        @ApiResponse(
            responseCode = "404",
            description = "File not found",
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
  @GetMapping("/download/{originalFileName}")
  public Mono<ResponseEntity<Resource>> downloadFile(
      @Parameter(description = "Original name of the file to download", required = true)
          @PathVariable
          @NotBlank
          String originalFileName) {

    log.info("Download request received for file: {}", originalFileName);

    return fileService
        .downloadFile(originalFileName)
        .map(
            resource -> {
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(getMediaType(originalFileName));
              headers.setContentDispositionFormData("attachment", originalFileName);

              return ResponseEntity.ok().headers(headers).body(resource);
            })
        .onErrorResume(e -> handleDownloadError(e, originalFileName));
  }

  /** Lists all files in blob storage */
  @Operation(
      summary = "List all files in storage",
      description = "Retrieves a list of all files available in blob storage",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @GetMapping("/list")
  public Mono<ResponseEntity<Api<List<String>>>> listFiles() {
    return fileService
        .listFiles()
        .map(
            files -> {
              Api<List<String>> response =
                  new Api<>(
                      HttpStatus.OK.toString(),
                      "Files retrieved successfully",
                      Optional.of(files),
                      Optional.empty());
              return ResponseEntity.ok(response);
            })
        .onErrorResume(
            e -> {
              log.error("Error retrieving file list", e);
              Api<List<String>> errorResponse =
                  new Api<>(
                      HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                      "Error retrieving file list: " + e.getMessage(),
                      Optional.empty(),
                      Optional.of(Map.of("error", "Failed to retrieve file list")));
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
            });
  }

  /** Determines the appropriate MediaType for a file */
  private MediaType getMediaType(String fileName) {
    String extension = getFileExtension(fileName).toLowerCase();

    switch (extension) {
      case "pdf":
        return MediaType.APPLICATION_PDF;
      case "jpg", "jpeg":
        return MediaType.IMAGE_JPEG;
      case "png":
        return MediaType.IMAGE_PNG;
      case "txt":
        return MediaType.TEXT_PLAIN;
      case "html":
        return MediaType.TEXT_HTML;
      default:
        return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  /** Extracts file extension */
  private String getFileExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
      return fileName.substring(dotIndex + 1);
    }
    return "";
  }

  /** Handles errors during download */
  private Mono<ResponseEntity<Resource>> handleDownloadError(Throwable e, String fileName) {
    log.error("Error downloading file: {}", fileName, e);

    if (e.getMessage() != null
        && (e.getMessage().contains("File not found")
            || e.getMessage().contains("No file found with original name")
            || e.getMessage().contains("File content is empty"))) {
      // Convert to a JSON response for Not Found errors
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  new ByteArrayResource(
                      String.format(
                              "{\"status\":\"%s\",\"message\":\"File not found: %s\",\"error\":\"%s\"}",
                              HttpStatus.NOT_FOUND, fileName, e.getMessage())
                          .getBytes())));
    } else {
      // Convert to a JSON response for server errors
      String message =
          e instanceof DecryptionException
              ? "Error decrypting file"
              : "Error downloading file: " + e.getMessage();

      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  new ByteArrayResource(
                      String.format(
                              "{\"status\":\"%s\",\"message\":\"%s\",\"error\":\"File download failed\"}",
                              HttpStatus.INTERNAL_SERVER_ERROR, message)
                          .getBytes())));
    }
  }

  /** Reads file content from a FilePart */
  private Mono<byte[]> readFileContent(FilePart filePart) {
    return filePart
        .content()
        .collectList()
        .map(
            dataBuffers -> {
              int totalSize = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();

              byte[] result = new byte[totalSize];
              int pos = 0;
              for (DataBuffer buffer : dataBuffers) {
                int readableByteCount = buffer.readableByteCount();
                buffer.read(result, pos, readableByteCount);
                pos += readableByteCount;
              }
              return result;
            });
  }

  /** Builds a standard API response */
  private ResponseEntity<Api<String>> buildResponse(
      HttpStatus status,
      String message,
      Optional<String> data,
      Optional<Map<String, String>> errors) {
    Api<String> response = new Api<>(status.toString(), message, data, errors);
    return ResponseEntity.status(status).body(response);
  }

  /**
   * Gets username from the security context.
   *
   * @return Mono containing the username
   */
  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> context.getAuthentication().getName());
  }

  @Operation(
      summary = "Upload a JSON template file",
      description = FileRequestAndResponse.JSON_UPLOAD_ENDPOINT_DESCRIPTION,
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "JSON file uploaded successfully and template created",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_SUCCESS_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_SUCCESS_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - JSON file validation failed or invalid template name",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_VALIDATION_ERROR_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_VALIDATION_ERROR_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_VALIDATION_ERROR_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_INVALID_FILE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_INVALID_FILE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_INVALID_FILE_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_EMPTY_FILE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_EMPTY_FILE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_EMPTY_FILE_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a Manager",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = FileRequestAndResponse.JSON_UPLOAD_ACCESS_DENIED_NAME,
                            description = FileRequestAndResponse.JSON_UPLOAD_ACCESS_DENIED_DESC,
                            value = FileRequestAndResponse.JSON_UPLOAD_ACCESS_DENIED_RESPONSE))),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - Template name already exists",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = FileRequestAndResponse.JSON_UPLOAD_CONFLICT_NAME,
                            description = FileRequestAndResponse.JSON_UPLOAD_CONFLICT_DESC,
                            value = FileRequestAndResponse.JSON_UPLOAD_CONFLICT_RESPONSE))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - File upload or template creation failed",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_SERVER_ERROR_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_SERVER_ERROR_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_SERVER_ERROR_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_TRANSACTION_ERROR_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_TRANSACTION_ERROR_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_TRANSACTION_ERROR_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_BLOB_FAILURE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_BLOB_FAILURE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_BLOB_FAILURE_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_PARALLEL_FAILURE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_PARALLEL_FAILURE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_PARALLEL_FAILURE_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_PARTIAL_FAILURE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_PARTIAL_FAILURE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_PARTIAL_FAILURE_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = FileRequestAndResponse.JSON_UPLOAD_FILE_FAILURE_NAME,
                          description = FileRequestAndResponse.JSON_UPLOAD_FILE_FAILURE_DESC,
                          value = FileRequestAndResponse.JSON_UPLOAD_FILE_FAILURE_RESPONSE)
                    }))
      })
  @PostMapping(value = "/upload-json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<Api<JsonFileUploadResponse>>> uploadJsonFile(
      @Parameter(
              description = FileRequestAndResponse.JSON_FILE_PARAM_DESC,
              required = true,
              schema = @Schema(type = "string", format = "binary"))
          @RequestPart("file")
          @NotNull
          FilePart filePart,
      @Parameter(
              description = FileRequestAndResponse.TEMPLATE_NAME_PARAM_DESC,
              required = true,
              example = "UserOnboardingTemplate",
              schema = @Schema(type = "string", minLength = 1, maxLength = 100))
          @RequestPart("templateName")
          @NotBlank
          String templateName) {

    log.info("JSON file upload request: {}, templateName: {}", filePart.filename(), templateName);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow managers to upload JSON templates
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to upload JSON template but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }
                          return readFileContent(filePart)
                              .flatMap(
                                  fileBytes ->
                                      fileService.uploadJsonFile(
                                          filePart, fileBytes, templateName));
                        }));
  }

  @Operation(
      summary = "Upload an image file",
      description =
          "Uploads a PNG image file to blob storage with strict validation for image types, dimensions, and file format. "
              + "Each image type has specific dimension requirements: "
              + "LOGO (150x150px), ICON (380x126px), STRIP (936x330px), THUMBNAIL (250x250px), "
              + "PASS_LOGO (380x120px), ACI_WALLETRON_NOTIFICATION_LOGO (150x150px). "
              + "Only PM role users can upload images. Maximum file size is 1MB.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Image file uploaded successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Successful Upload",
                            description = "Image uploaded successfully with all details",
                            value =
                                """
                            {
                              "status": "200",
                              "message": "Image file uploaded successfully",
                              "data": {
                                "fileName": "68384c8998b48830443352df-logo",
                                "walletronId": "68384c8998b48830443352df",
                                "imageType": "LOGO",
                                "originalFileName": "company-logo.png",
                                "fileSize": 8594,
                                "fileUrl": "https://storage/68384c8998b48830443352df-logo"
                              }
                            }"""))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid image file or validation failed",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = "Invalid File Format",
                          description = "Non-PNG file uploaded",
                          value =
                              """
                                {
                                  "status": "400",
                                  "message": "Only PNG files are allowed for images. Found: jpg",
                                  "errors": {
                                    "error": "Image file validation failed"
                                  }
                                }"""),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = "Invalid Dimensions",
                          description = "Image dimensions don't match requirements",
                          value =
                              """
                                {
                                  "status": "400",
                                  "message": "Invalid dimensions for PASS_LOGO image. Required: 380x120, Found: 400x150",
                                  "errors": {
                                    "error": "Image file validation failed"
                                  }
                                }"""),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = "File Too Large",
                          description = "Image file exceeds size limit",
                          value =
                              """
                                {
                                  "status": "400",
                                  "message": "Image file size exceeds maximum allowed size of 1 MB",
                                  "errors": {
                                    "error": "Image file validation failed"
                                  }
                                }"""),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = "Invalid Image Type",
                          description = "Unsupported image type specified",
                          value =
                              """
                                {
                                  "status": "400",
                                  "message": "Invalid image type. Allowed types: LOGO, ICON, STRIP, THUMBNAIL, PASS_LOGO, ACI_WALLETRON_NOTIFICATION_LOGO",
                                  "errors": {
                                    "error": "Image file validation failed"
                                  }
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Access Denied",
                            description = "Non-PM user attempted to upload image",
                            value =
                                """
                            {
                              "status": "403",
                              "message": "Access denied. Only PM role can upload images.",
                              "errors": {
                                "error": "Access denied"
                              }
                            }"""))),
        @ApiResponse(
            responseCode = "409",
            description = "Image file already exists for this walletronId and imageType",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "File Already Exists",
                            description = "Image with same walletronId and type already exists",
                            value =
                                """
                            {
                              "status": "409",
                              "message": "Image with walletronId '68384c8998b48830443352df' and type 'LOGO' already exists",
                              "errors": {
                                "error": "Image already exists"
                              }
                            }"""))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during upload or processing",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Server Error",
                            description = "Internal error during image processing or upload",
                            value =
                                """
                            {
                              "status": "500",
                              "message": "Something went wrong: Failed to upload image to blob storage",
                              "errors": {
                                "error": "Internal server error"
                              }
                            }""")))
      })
  @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<Api<ImageFileUploadResponse>>> uploadImageFile(
      @Parameter(
              description =
                  "PNG image file to upload. Must be a valid PNG format with correct dimensions based on imageType. "
                      + "Maximum file size: 1MB. Required dimensions: LOGO (150x150px), ICON (380x126px), STRIP (936x330px), "
                      + "THUMBNAIL (250x250px), PASS_LOGO (380x120px), ACI_WALLETRON_NOTIFICATION_LOGO (150x150px)",
              required = true,
              schema = @Schema(type = "string", format = "binary"))
          @RequestPart("file")
          @NotNull
          FilePart filePart,
      @Parameter(
              description =
                  "Type of image being uploaded. Each type has specific dimension requirements: "
                      + "LOGO requires 150x150px, ICON requires 380x126px, STRIP requires 936x330px, THUMBNAIL requires 250x250px, "
                      + "PASS_LOGO requires 380x120px, ACI_WALLETRON_NOTIFICATION_LOGO requires 150x150px",
              required = true,
              example = "LOGO",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {
                        "LOGO",
                        "ICON",
                        "STRIP",
                        "THUMBNAIL",
                        "PASS_LOGO",
                        "ACI_WALLETRON_NOTIFICATION_LOGO"
                      }))
          @RequestPart("imageType")
          @NotBlank
          String imageType,
      @Parameter(
              description =
                  "Unique Walletron ID to associate with the image. Used to generate the filename as '{walletronId}-{imageType}'. "
                      + "Only one image per walletronId-imageType combination is allowed.",
              required = true,
              example = "68384c8998b48830443352df",
              schema =
                  @Schema(
                      type = "string",
                      minLength = 1,
                      maxLength = 50,
                      pattern = "^[a-zA-Z0-9_-]+$"))
          @RequestPart("walletronId")
          @NotBlank
          String walletronId) {

    log.info(
        "Image file upload request: {}, imageType: {}, walletronId: {}",
        filePart.filename(),
        imageType,
        walletronId);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow PM role to upload images
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to upload image but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(
                                    "Access denied. Only PM role can upload images."));
                          }
                          return readFileContent(filePart)
                              .flatMap(
                                  fileBytes ->
                                      fileService.uploadImageFile(
                                          filePart, fileBytes, imageType, walletronId));
                        }));
  }

  @Operation(
      summary = "Delete an image file",
      description =
          "Deletes an image file from blob storage based on the exact fileName. "
              + "The fileName should be in the format '{walletronId}-{imageType}' (e.g., '68384c8998b48830443352df-logo'). "
              + "Only PM role users can delete images. Returns detailed success or error information.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Image file deleted successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Successful Deletion",
                            description = "Image file deleted successfully",
                            value =
                                """
                            {
                              "status": "200",
                              "message": "Image file deleted successfully",
                              "data": "Image deleted: 68384c8998b48830443352df-logo"
                            }"""))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Access Denied",
                            description = "Non-PM user attempted to delete image",
                            value =
                                """
                            {
                              "status": "403",
                              "message": "Access denied. Only PM role can delete images.",
                              "errors": {
                                "error": "Access denied"
                              }
                            }"""))),
        @ApiResponse(
            responseCode = "404",
            description = "Image file not found in blob storage",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "File Not Found",
                            description = "Specified image file does not exist",
                            value =
                                """
                            {
                              "status": "404",
                              "message": "Image file not found: 68384c8998b48830443352df-logo",
                              "errors": {
                                "error": "Image not found: 68384c8998b48830443352df-logo"
                              }
                            }"""))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during deletion process",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Server Error",
                            description = "Internal error during image deletion",
                            value =
                                """
                            {
                              "status": "500",
                              "message": "Something went wrong: Failed to delete image from blob storage",
                              "errors": {
                                "error": "Internal server error"
                              }
                            }""")))
      })
  @DeleteMapping("/delete-image/{fileName}")
  public Mono<ResponseEntity<Api<String>>> deleteImage(
      @Parameter(
              description =
                  "Exact name of the image file to delete from blob storage. "
                      + "Should be in the format '{walletronId}-{imageType}' (e.g., '68384c8998b48830443352df-logo'). "
                      + "This is the fileName returned from the upload response.",
              required = true,
              example = "68384c8998b48830443352df-logo",
              schema = @Schema(type = "string", pattern = "^[a-zA-Z0-9_-]+-[a-zA-Z]+$"))
          @PathVariable
          @NotBlank
          String fileName) {

    log.info("Delete request received for image file: {}", fileName);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow PM role to delete images
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to delete image but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(
                                    "Access denied. Only PM role can delete images."));
                          }
                          return fileService.deleteImageFileByName(fileName);
                        }));
  }
}
