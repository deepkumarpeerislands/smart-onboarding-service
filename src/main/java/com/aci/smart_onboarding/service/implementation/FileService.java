package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.FileConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ImageFileUploadResponse;
import com.aci.smart_onboarding.dto.JsonFileUploadResponse;
import com.aci.smart_onboarding.dto.JsonTemplateResponse;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.EncryptionException;
import com.aci.smart_onboarding.exception.JsonFileValidationException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.JsonTemplateRepository;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.aci.smart_onboarding.service.IFileService;
import com.aci.smart_onboarding.service.IJsonTemplateService;
import com.aci.smart_onboarding.util.EncryptionUtil;
import com.aci.smart_onboarding.util.FileValidator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for file operations including upload, download, encryption, and decryption. Centralizes
 * file handling logic to maintain separation of concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

  private final IBlobStorageService blobStorageService;
  private final EncryptionUtil encryptionUtil;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final IJsonTemplateService jsonTemplateService;
  private final JsonTemplateRepository jsonTemplateRepository;

  @Value("${encryption.alternative.keys:LegacySmartOnboardingKey2023,SmartOnboardingLegacyKey2022}")
  private String alternativeKeysString;

  /**
   * Updates the BRD document with file upload timestamp based on file type
   *
   * @param brdId The ID of the BRD document
   * @param fileType The type of file uploaded (ACH or WALLETRON)
   * @return Mono<Void> indicating completion
   */
  private Mono<Void> updateBrdUploadTimestamp(String brdId, String fileType) {
    Query query = Query.query(Criteria.where("brdId").is(brdId));
    Update update = new Update();

    if (fileType.equalsIgnoreCase("ACH")) {
      update.set("achUploadedOn", LocalDateTime.now());
    } else if (fileType.equalsIgnoreCase("WALLETRON")) {
      update.set("walletronUploadedOn", LocalDateTime.now());
    }

    return reactiveMongoTemplate.updateFirst(query, update, BRD.class).then();
  }

  /**
   * Encrypts and uploads a file to blob storage
   *
   * @param filePart The file to upload
   * @param fileContent The raw file content as byte array
   * @param fileType The type of file being uploaded
   * @param brdId The ID of the BRD document
   * @return URL of the uploaded file
   */
  @Override
  public Mono<String> uploadEncryptedFile(
      FilePart filePart, byte[] fileContent, String fileType, String brdId) {
    // Log detailed information about the file being uploaded
    logFileDetails(filePart.filename(), fileContent);

    // Validate file content before encryption
    String validationResult = validateFileContent(filePart.filename(), fileContent);
    if (validationResult != null) {
      log.warn("File validation failed: {}", validationResult);
      return Mono.error(new RuntimeException("File validation failed: " + validationResult));
    }

    try {
      // Encrypt the file content with AES
      byte[] encryptedBytes = encryptionUtil.encrypt(fileContent);

      // Generate unique filename with .enc extension to indicate encryption
      String fileName = UUID.randomUUID() + "-" + filePart.filename() + ".enc";

      // Upload encrypted content to blob storage and update BRD timestamp
      return blobStorageService
          .uploadFile(fileName, encryptedBytes)
          .flatMap(url -> updateBrdUploadTimestamp(brdId, fileType).thenReturn(url));
    } catch (Exception e) {
      log.error("Error encrypting/uploading file: {}", filePart.filename(), e);
      return Mono.error(
          e instanceof EncryptionException
              ? e
              : new RuntimeException("Failed to process file: " + e.getMessage()));
    }
  }

  /**
   * Downloads a file from blob storage and decrypts it if needed
   *
   * @param originalFileName The original name of the file to download
   * @return The file content as a Resource
   */
  @Override
  public Mono<Resource> downloadFile(String originalFileName) {
    return findEncryptedFileNameByOriginalName(originalFileName)
        .flatMap(
            matchedFileName ->
                blobStorageService
                    .fetchFile(matchedFileName)
                    .flatMap(
                        fileBytes -> {
                          if (fileBytes == null || fileBytes.length == 0) {
                            return Mono.error(new RuntimeException("File content is empty"));
                          }

                          byte[] contentToServe =
                              processFileContent(fileBytes, matchedFileName, originalFileName);
                          return Mono.just(new ByteArrayResource(contentToServe));
                        }));
  }

  /** Finds the actual filename in storage that corresponds to the original filename */
  private Mono<String> findEncryptedFileNameByOriginalName(String originalFileName) {
    log.info("Searching for file matching: {}", originalFileName);

    return Mono.<String>defer(
            () ->
                // Strategy 1: Exact name match
                blobStorageService
                    .listFiles()
                    .filter(filename -> filename.equalsIgnoreCase(originalFileName))
                    .doOnNext(match -> log.info("Found exact filename match: {}", match))
                    .next()
                    // Strategy 2: UUID-originalFileName.enc format
                    .switchIfEmpty(findEncryptedFileMatch(originalFileName))
                    // Strategy 3: UUID-originalFileName format (without .enc)
                    .switchIfEmpty(findUnencryptedPrefixedMatch(originalFileName))
                    // Strategy 4: Fallback to pattern matching
                    .switchIfEmpty(findPatternMatch(originalFileName)))
        .doOnNext(filename -> log.info("Selected file for download: {}", filename))
        .switchIfEmpty(
            Mono.error(
                new RuntimeException("No file found with original name: " + originalFileName)));
  }

  /** Finds encrypted files with UUID prefix */
  private Mono<String> findEncryptedFileMatch(String originalFileName) {
    return blobStorageService
        .listFiles()
        .filter(
            filename -> {
              if (!filename.endsWith(".enc")) {
                return false;
              }

              String nameWithoutExt = filename.substring(0, filename.length() - 4);
              int dashIndex = nameWithoutExt.indexOf('-');
              if (dashIndex > 0 && dashIndex < nameWithoutExt.length() - 1) {
                String extractedName = nameWithoutExt.substring(dashIndex + 1);
                return extractedName.equalsIgnoreCase(originalFileName);
              }
              return false;
            })
        .doOnNext(match -> log.info("Found UUID-prefixed encrypted match: {}", match))
        .next();
  }

  /** Finds unencrypted files with UUID prefix */
  private Mono<String> findUnencryptedPrefixedMatch(String originalFileName) {
    return blobStorageService
        .listFiles()
        .filter(
            filename -> {
              int dashIndex = filename.indexOf('-');
              if (dashIndex > 0 && dashIndex < filename.length() - 1) {
                String extractedName = filename.substring(dashIndex + 1);
                return extractedName.equalsIgnoreCase(originalFileName);
              }
              return false;
            })
        .doOnNext(match -> log.info("Found UUID-prefixed non-encrypted match: {}", match))
        .next();
  }

  /** Finds files containing the original name */
  private Mono<String> findPatternMatch(String originalFileName) {
    return blobStorageService
        .listFiles()
        .filter(filename -> filename.toLowerCase().contains(originalFileName.toLowerCase()))
        .doOnNext(match -> log.info("Found pattern match: {}", match))
        .next();
  }

  /** Processes file content - decrypts if needed */
  private byte[] processFileContent(
      byte[] fileBytes, String matchedFileName, String originalFileName) {
    log.info("Downloaded file {} with size: {} bytes", matchedFileName, fileBytes.length);

    // Try to decrypt if file is encrypted
    if (matchedFileName.endsWith(".enc")) {
      return decryptFile(fileBytes, matchedFileName, originalFileName);
    } else {
      log.info("Serving non-encrypted file: {}", matchedFileName);
      return fileBytes;
    }
  }

  /** Attempts to decrypt file content */
  private byte[] decryptFile(byte[] fileBytes, String matchedFileName, String originalFileName) {
    log.info("Decrypting encrypted file: {}", matchedFileName);

    // Check if file size is sufficient for decryption
    if (fileBytes.length < 32) {
      log.warn("File too small for a valid encrypted file: {} bytes", fileBytes.length);
      return fileBytes; // Return original content
    }

    try {
      // Parse alternative keys from configuration
      List<String> alternativeKeys = parseAlternativeKeys();

      // Use the enhanced decryption with automatic fallback to alternative keys
      byte[] decryptedBytes = encryptionUtil.decryptWithFallback(fileBytes, alternativeKeys);

      if (decryptedBytes == null) {
        log.warn("Decryption returned null content, using original encrypted content");
        return fileBytes;
      } else if (decryptedBytes.length == 0) {
        log.info("Decryption returned empty content");
        return decryptedBytes;
      }

      // Validate content based on expected file type
      if (isPdfFile(originalFileName)) {
        validatePdfContent(decryptedBytes);
      } else {
        // For non-PDF files, just log some information about the content
        logFileContentInfo(decryptedBytes);
      }

      log.info("File decrypted successfully, final size: {} bytes", decryptedBytes.length);
      return decryptedBytes;

    } catch (Exception e) {
      log.warn("Decryption failed with all keys: {}", e.getMessage());
      log.debug("Decryption error details", e);
      return fileBytes; // Return the original encrypted content as fallback
    }
  }

  /** Logs general information about decrypted file content */
  private void logFileContentInfo(byte[] content) {
    if (content == null || content.length == 0) {
      log.warn("Empty file content");
      return;
    }

    StringBuilder firstBytesHex = new StringBuilder("First bytes of content: ");
    int bytesToLog = Math.min(content.length, 8);
    for (int i = 0; i < bytesToLog; i++) {
      firstBytesHex.append(String.format("%02x ", content[i] & 0xff));
    }
    log.info(firstBytesHex.toString());

    // Try to detect common file types by signature
    detectFileType(content);
  }

  /** Attempts to detect file type based on content signature */
  private void detectFileType(byte[] content) {
    if (content == null || content.length < 4) {
      return;
    }

    // Check for common file signatures
    if (content[0] == 0x25 && content[1] == 0x50 && content[2] == 0x44 && content[3] == 0x46) {
      log.info("Content appears to be a PDF file");
    } else if (content[0] == (byte) 0xFF
        && content[1] == (byte) 0xD8
        && content[2] == (byte) 0xFF) {
      log.info("Content appears to be a JPEG image");
    } else if (content[0] == (byte) 0x89
        && content[1] == 0x50
        && content[2] == 0x4E
        && content[3] == 0x47) {
      log.info("Content appears to be a PNG image");
    } else if (content[0] == 0x50
        && content[1] == 0x4B
        && (content[2] == 0x03 || content[2] == 0x05 || content[2] == 0x07)) {
      log.info("Content appears to be a ZIP/DOCX/XLSX file");
    } else {
      log.info("File type could not be determined from content signature");
    }
  }

  /** Checks if the file is a PDF based on filename */
  private boolean isPdfFile(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    String lowerCaseName = fileName.toLowerCase();
    return lowerCaseName.endsWith(".pdf");
  }

  /** Validates PDF file content and logs warnings if it appears invalid */
  private void validatePdfContent(byte[] decryptedBytes) {
    if (!hasPdfSignature(decryptedBytes)) {
      handleInvalidPdfSignature(decryptedBytes);
    } else {
      log.info("Validated PDF content with correct signature");
    }
  }

  /** Handles the case when PDF signature is invalid */
  private void handleInvalidPdfSignature(byte[] decryptedBytes) {
    log.warn("Decrypted content doesn't have PDF signature. May be corrupted or wrong key");

    if (isEmptyOrZeroByte(decryptedBytes)) {
      log.error("Decryption produced only a zero byte. Likely key mismatch or corrupted file");
    } else {
      handleNonEmptyInvalidPdf(decryptedBytes);
    }
  }

  /** Checks if content is empty or just a zero byte */
  private boolean isEmptyOrZeroByte(byte[] content) {
    return content.length == 1 && content[0] == 0;
  }

  /** Handles non-empty content that lacks PDF signature */
  private void handleNonEmptyInvalidPdf(byte[] content) {
    // Check if it's actually a text file with a PDF extension
    if (isProbablyText(content)) {
      String textContent = extractTextPreview(content);
      log.warn(
          "File appears to be a text file with a PDF extension. Content preview: '{}'",
          textContent);
    }

    // Log the first few bytes
    logHexBytes("Content may be a non-PDF file type (first bytes: {})", content, 16);
  }

  /** Logs the bytes of content in hexadecimal format */
  private void logHexBytes(String messagePattern, byte[] content, int maxBytes) {
    StringBuilder bytesHex = new StringBuilder();
    for (int i = 0; i < Math.min(content.length, maxBytes); i++) {
      bytesHex.append(String.format(FileConstants.FILE_CHAR_SEQUENCE, content[i] & 0xFF));
    }
    log.info(messagePattern, bytesHex.toString().trim());
  }

  /** Checks if content has a valid PDF signature */
  private boolean hasPdfSignature(byte[] content) {
    return content.length >= 4
        && content[0] == 0x25
        && // %
        content[1] == 0x50
        && // P
        content[2] == 0x44
        && // D
        content[3] == 0x46; // F
  }

  /** Determines if content appears to be readable text */
  private boolean isProbablyText(byte[] content) {
    if (content == null || content.length == 0) {
      return false;
    }

    // Check a sample of the content
    int checkSize = Math.min(content.length, 100);
    int textCount = 0;

    for (int i = 0; i < checkSize; i++) {
      byte b = content[i];
      // Count printable ASCII characters and common whitespace
      if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
        textCount++;
      }
    }

    // If more than 90% of the checked bytes are text, it's probably a text file
    return (textCount * 100 / checkSize) > 90;
  }

  /** Extracts a text preview from byte content */
  private String extractTextPreview(byte[] content) {
    if (content == null || content.length == 0) {
      return "(empty)";
    }

    // Convert bytes to string, handling potential encoding issues
    try {
      String text = new String(content, StandardCharsets.UTF_8);

      // Limit to first 100 characters
      if (text.length() > 100) {
        return text.substring(0, 97) + "...";
      }
      return text;
    } catch (Exception e) {
      return "(non-text content)";
    }
  }

  /**
   * Lists all files available in blob storage
   *
   * @return a list of file names
   */
  @Override
  public Mono<List<String>> listFiles() {
    log.info("Retrieving list of all files from blob storage");
    return blobStorageService.listFiles().collectList();
  }

  /**
   * Parses the alternative keys string from configuration
   *
   * @return List of alternative encryption keys
   */
  private List<String> parseAlternativeKeys() {
    if (alternativeKeysString == null || alternativeKeysString.trim().isEmpty()) {
      return List.of();
    }

    return Arrays.stream(alternativeKeysString.split(","))
        .map(String::trim)
        .filter(key -> !key.isEmpty())
        .toList();
  }

  /**
   * Validates that file content matches its claimed extension.
   *
   * @param filename The filename with extension
   * @param content The file content to validate
   * @return null if validation passes, error message if validation fails
   */
  private String validateFileContent(String filename, byte[] content) {
    if (content == null || content.length == 0) {
      return "File content is empty";
    }

    // Minimum size checks
    if (content.length < 4) {
      return "File is too small to be a valid document";
    }

    // We only allow PDF files
    if (!filename.toLowerCase().endsWith(".pdf")) {
      return "Only PDF files are allowed";
    }

    // Check PDF signature
    if (content[0] != 0x25
        || // %
        content[1] != 0x50
        || // P
        content[2] != 0x44
        || // D
        content[3] != 0x46) { // F

      StringBuilder foundSignature = new StringBuilder();
      for (int i = 0; i < Math.min(content.length, 4); i++) {
        foundSignature.append(String.format("%02X ", content[i] & 0xFF));
      }

      log.warn("Invalid PDF signature detected: {}", foundSignature.toString());
      return "File with .pdf extension does not have a valid PDF signature";
    }

    log.info("PDF file validation passed");
    return null; // Validation passed
  }

  /** Logs detailed information about a file for debugging purposes */
  private void logFileDetails(String filename, byte[] content) {
    log.info("Preparing to upload file: {}, size: {} bytes", filename, content.length);

    // Check file signature if it's a PDF
    if (filename.toLowerCase().endsWith(".pdf")) {
      logPdfSignatureStatus(content);
    }

    // Log content sample
    logContentSample(content);
  }

  /** Logs the status of PDF signature verification */
  private void logPdfSignatureStatus(byte[] content) {
    boolean hasPdfSignature = hasPdfSignature(content);

    if (hasPdfSignature) {
      log.info("File has valid PDF signature");
    } else {
      log.warn("File with .pdf extension does not have PDF signature");
      logFirstFourBytes(content);

      if (isProbablyBinary(content)) {
        log.warn("File appears to be binary but not a PDF");
      }
    }
  }

  /** Logs the first four bytes of content for debugging */
  private void logFirstFourBytes(byte[] content) {
    if (content.length >= 4) {
      log.warn(
          "First bytes: {} {} {} {}",
          String.format("%02X", content[0] & 0xFF),
          String.format("%02X", content[1] & 0xFF),
          String.format("%02X", content[2] & 0xFF),
          String.format("%02X", content[3] & 0xFF));
    }
  }

  /** Logs a hexadecimal sample of the file content */
  private void logContentSample(byte[] content) {
    if (content.length > 0) {
      StringBuilder hexDump = new StringBuilder("File content sample (hex): ");
      int bytesToShow = Math.min(content.length, 32);

      for (int i = 0; i < bytesToShow; i++) {
        hexDump.append(String.format("%02X ", content[i] & 0xFF));
        if (i == 15) hexDump.append(" | "); // Visual break at 16 bytes
      }

      log.info(hexDump.toString());
    }
  }

  /** Determines if file content appears to be binary data rather than text */
  private boolean isProbablyBinary(byte[] content) {
    if (content == null || content.length == 0) {
      return false;
    }

    // Check a sample of the content
    int checkSize = Math.min(content.length, 100);
    int binaryCount = 0;

    for (int i = 0; i < checkSize; i++) {
      byte b = content[i];
      // Count bytes outside ASCII range (excluding common whitespace)
      if ((b < 32 || b > 126) && b != 9 && b != 10 && b != 13) {
        binaryCount++;
      }
    }

    // If more than 10% of the checked bytes are binary, it's probably binary data
    return (binaryCount * 100 / checkSize) > 10;
  }

  @Override
  public Mono<ResponseEntity<Api<JsonFileUploadResponse>>> uploadJsonFile(
      FilePart filePart, byte[] fileContent, String templateName) {
    log.info("JSON file upload request: {}, templateName: {}", filePart.filename(), templateName);

    String validationResult = validateJsonFileContent(filePart.filename(), fileContent);
    if (validationResult != null) {
      return Mono.error(new JsonFileValidationException(validationResult));
    }

    String sanitizedTemplateName = sanitizeFileName(templateName);
    String fileName = UUID.randomUUID() + "-" + sanitizedTemplateName + "-" + filePart.filename();

    Mono<String> userContextMono =
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .defaultIfEmpty("system")
            .cache();

    // Wrap each operation to capture individual success/failure states
    Mono<OperationResult<String>> fileUploadMono =
        blobStorageService
            .uploadFile(fileName, fileContent)
            .map(url -> OperationResult.success("FILE_UPLOAD", url))
            .onErrorResume(
                error -> {
                  log.error("Blob storage upload failed: {}", error.getMessage());
                  return Mono.just(OperationResult.failure("FILE_UPLOAD", error));
                });

    Mono<OperationResult<ResponseEntity<Api<JsonTemplateResponse>>>> templateCreationMono =
        userContextMono
            .flatMap(
                username ->
                    jsonTemplateService.createTemplate(
                        templateName, fileName, filePart.filename(), username))
            .map(response -> OperationResult.success("TEMPLATE_CREATION", response))
            .onErrorResume(
                error -> {
                  log.error("Template creation failed: {}", error.getMessage());
                  return Mono.just(OperationResult.failure("TEMPLATE_CREATION", error));
                });

    return Mono.zip(fileUploadMono, templateCreationMono)
        .flatMap(
            tuple -> {
              OperationResult<String> fileResult = tuple.getT1();
              OperationResult<ResponseEntity<Api<JsonTemplateResponse>>> templateResult =
                  tuple.getT2();

              return handleParallelResults(
                  fileResult, templateResult, filePart, templateName, fileName, fileContent);
            })
        .onErrorMap(this::handleError);
  }

  /** Handles the results of parallel operations with proper cleanup logic */
  private Mono<ResponseEntity<Api<JsonFileUploadResponse>>> handleParallelResults(
      OperationResult<String> fileResult,
      OperationResult<ResponseEntity<Api<JsonTemplateResponse>>> templateResult,
      FilePart filePart,
      String templateName,
      String fileName,
      byte[] fileContent) {

    if (fileResult.isSuccess() && templateResult.isSuccess()) {
      return buildSuccessResponse(
          templateResult.getData(), filePart, templateName, fileName, fileContent);
    }

    if (fileResult.isSuccess() && !templateResult.isSuccess()) {
      log.warn("Template creation failed, cleaning up uploaded file: {}", fileName);
      return blobStorageService
          .deleteFile(fileName)
          .doOnSuccess(
              v ->
                  log.info(
                      "Successfully cleaned up blob file after template failure: {}", fileName))
          .doOnError(
              cleanupError ->
                  log.warn(
                      "Failed to cleanup blob file {}: {}", fileName, cleanupError.getMessage()))
          .then(Mono.error(templateResult.getError()));
    }

    if (!fileResult.isSuccess() && templateResult.isSuccess()) {
      log.warn("File upload failed, cleaning up created template: {}", templateName);
      return jsonTemplateRepository
          .deleteByTemplateName(templateName)
          .doOnNext(
              deletedCount -> {
                if (deletedCount > 0) {
                  log.info(
                      "Successfully cleaned up {} template record(s) after file upload failure: {}",
                      deletedCount,
                      templateName);
                }
              })
          .doOnError(
              cleanupError ->
                  log.warn(
                      "Failed to cleanup template {}: {}", templateName, cleanupError.getMessage()))
          .then(Mono.error(fileResult.getError()));
    }

    log.error("Both file upload and template creation failed for: {}", templateName);
    return Mono.error(fileResult.getError());
  }

  /** Builds successful response when both operations completed successfully */
  private Mono<ResponseEntity<Api<JsonFileUploadResponse>>> buildSuccessResponse(
      ResponseEntity<Api<JsonTemplateResponse>> templateResponse,
      FilePart filePart,
      String templateName,
      String fileName,
      byte[] fileContent) {

    // Validate template response status
    if (templateResponse.getStatusCode() != HttpStatus.CREATED) {
      return Mono.error(
          new RuntimeException(
              "Template creation returned unexpected status: " + templateResponse.getStatusCode()));
    }

    JsonTemplateResponse templateDetails =
        Optional.ofNullable(templateResponse.getBody()).flatMap(Api::getData).orElse(null);

    JsonFileUploadResponse responseData =
        JsonFileUploadResponse.builder()
            .templateName(templateName)
            .originalFileName(filePart.filename())
            .generatedFileName(fileName)
            .fileSize(fileContent.length)
            .templateDetails(templateDetails)
            .build();

    Api<JsonFileUploadResponse> apiResponse =
        new Api<>(
            HttpStatus.OK.toString(),
            "JSON file uploaded and template created successfully",
            Optional.of(responseData),
            Optional.empty());

    return Mono.just(ResponseEntity.ok(apiResponse));
  }

  /** Helper class to track operation results */
  private static class OperationResult<T> {
    private final String operationType;
    private final boolean success;
    private final T data;
    private final Throwable error;

    private OperationResult(String operationType, boolean success, T data, Throwable error) {
      this.operationType = operationType;
      this.success = success;
      this.data = data;
      this.error = error;
    }

    public static <T> OperationResult<T> success(String operationType, T data) {
      return new OperationResult<>(operationType, true, data, null);
    }

    public static <T> OperationResult<T> failure(String operationType, Throwable error) {
      return new OperationResult<>(operationType, false, null, error);
    }

    public boolean isSuccess() {
      return success;
    }

    public T getData() {
      return data;
    }

    public Throwable getError() {
      return error;
    }

    public String getOperationType() {
      return operationType;
    }
  }

  /** Sanitizes template name for use in filename */
  private String sanitizeFileName(String templateName) {
    if (templateName == null || templateName.trim().isEmpty()) {
      return "default";
    }
    return templateName
        .trim()
        .replaceAll("[^a-zA-Z0-9_-]", "_")
        .replaceAll("_{2,}", "_")
        .toLowerCase();
  }

  public Throwable handleError(Throwable throwable) {
    if (throwable instanceof JsonFileValidationException) {
      return throwable;
    } else if (throwable instanceof AlreadyExistException) {
      return throwable;
    } else if (throwable instanceof BadRequestException) {
      return throwable;
    } else {
      log.error("Something went wrong", throwable);
      return new Exception("Something went wrong: " + throwable.getMessage());
    }
  }

  /** Validates JSON file content */
  private String validateJsonFileContent(String fileName, byte[] content) {
    if (content == null || content.length == 0) {
      return "JSON file content is empty";
    }

    try {
      FileValidator.validateFileName(fileName);
      FileValidator.validateFileSize(content.length);

      int dotIndex = fileName.lastIndexOf('.');
      if (dotIndex <= 0 || dotIndex >= fileName.length() - 1) {
        return "File must have a valid extension";
      }

      String extension = fileName.substring(dotIndex + 1).toLowerCase();
      if (!"json".equals(extension)) {
        return "Only JSON files are allowed. Found: " + extension;
      }

      String jsonContent = new String(content, StandardCharsets.UTF_8);
      String trimmedContent = jsonContent.trim();

      if (trimmedContent.isEmpty()) {
        return "JSON file content is empty";
      }

      char firstChar = trimmedContent.charAt(0);
      char lastChar = trimmedContent.charAt(trimmedContent.length() - 1);

      if (!((firstChar == '{' && lastChar == '}') || (firstChar == '[' && lastChar == ']'))) {
        return "Invalid JSON format - must be a valid JSON object or array";
      }

      return null;

    } catch (Exception e) {
      return "JSON file validation failed: " + e.getMessage();
    }
  }

  @Override
  public Mono<ResponseEntity<Api<ImageFileUploadResponse>>> uploadImageFile(
      FilePart filePart, byte[] fileContent, String imageType, String walletronId) {
    log.info(
        "Image file upload request: {}, imageType: {}, walletronId: {}",
        filePart.filename(),
        imageType,
        walletronId);

    String sanitizedImageType = sanitizeFileName(imageType);
    String fileName = walletronId + "-" + sanitizedImageType;
    log.debug("Generated filename: {}", fileName);

    // Optimized reactive chain: validation -> uniqueness check -> upload -> build response
    return validateImageFileReactive(filePart.filename(), fileContent, imageType)
        .doOnNext(
            validationResult -> log.debug("Validation completed with result: {}", validationResult))
        .flatMap(
            validationResult -> {
              log.debug("Processing validation result: {}", validationResult);
              if (validationResult != null && !validationResult.isEmpty()) {
                log.error("Image validation failed: {}", validationResult);
                return Mono.error(new BadRequestException(validationResult));
              }

              log.debug("Image validation passed, checking file uniqueness");
              // Only check uniqueness if validation passes (optimization)
              return checkImageUniquenessOptimized(fileName)
                  .doOnNext(exists -> log.debug("Uniqueness check result: {}", exists))
                  .flatMap(
                      exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                          log.warn("Image already exists: {}", fileName);
                          return Mono.error(
                              new AlreadyExistException(
                                  String.format(
                                      "Image with walletronId '%s' and type '%s' already exists",
                                      walletronId, imageType)));
                        }

                        log.info("Image is unique and validated, uploading to blob storage");
                        // Upload image file to blob storage
                        return blobStorageService
                            .uploadFile(fileName, fileContent)
                            .doOnSuccess(url -> log.info("Image uploaded successfully to: {}", url))
                            .doOnError(
                                error ->
                                    log.error("Failed to upload image to blob storage: ", error))
                            .map(
                                url -> {
                                  log.debug("Building response entity for successful upload");
                                  return buildImageUploadResponseEntity(
                                      filePart, imageType, fileName, fileContent, walletronId, url);
                                });
                      });
            })
        .doOnSuccess(response -> log.info("Image upload completed successfully"))
        .doOnError(error -> log.error("Image upload failed: ", error))
        .onErrorMap(this::handleError);
  }

  /** Optimized uniqueness check using direct file existence check instead of listing all files */
  private Mono<Boolean> checkImageUniquenessOptimized(String fileName) {
    // Try to fetch the file directly - more efficient than listing all files
    return blobStorageService
        .fetchFile(fileName)
        .map(
            fileBytes -> {
              log.info("Image file already exists: {}", fileName);
              return Boolean.TRUE; // File exists
            })
        .onErrorReturn(Boolean.FALSE) // File doesn't exist or error occurred
        .doOnNext(
            exists -> {
              if (Boolean.FALSE.equals(exists)) {
                log.info("Image file is unique: {}", fileName);
              }
            });
  }

  /** Reactive validation chain for image files */
  private Mono<String> validateImageFileReactive(
      String originalFileName, byte[] content, String imageType) {
    log.debug(
        "Starting image validation for file: {}, type: {}, size: {} bytes",
        originalFileName,
        imageType,
        content.length);

    // First perform basic validation
    return Mono.fromCallable(
            () -> {
              log.debug("Executing basic validation callable for: {}", originalFileName);
              String result = validateBasicImageProperties(originalFileName, content, imageType);
              log.debug("Basic validation callable completed with result: {}", result);
              return result;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(
            basicValidation -> log.debug("Basic validation result received: {}", basicValidation))
        .doOnError(error -> log.error("Error in basic validation: ", error))
        .flatMap(
            basicValidation -> {
              log.debug("Basic validation result received in flatMap: {}", basicValidation);
              if (basicValidation != null && !basicValidation.isEmpty()) {
                log.warn("Basic validation failed: {}", basicValidation);
                return Mono.just(basicValidation);
              }
              log.debug("Basic validation passed, proceeding to dimension validation");
              // Perform blocking image dimension validation on bounded elastic scheduler
              return validateImageDimensionsReactive(content, imageType)
                  .doOnNext(
                      dimensionResult ->
                          log.debug("Dimension validation result: {}", dimensionResult));
            })
        .doOnNext(result -> log.debug("Final validation chain result: {}", result))
        .doOnError(error -> log.error("Error in validation chain: ", error))
        .doOnSuccess(
            result -> log.debug("Validation chain completed successfully with result: {}", result))
        .doOnTerminate(() -> log.debug("Validation chain terminated"));
  }

  /** Validates basic image properties (non-blocking operations) */
  private String validateBasicImageProperties(
      String originalFileName, byte[] content, String imageType) {
    log.debug("Validating basic properties for original file: {}", originalFileName);
    if (content == null || content.length == 0) {
      return "Image file content is empty";
    }

    try {
      FileValidator.validateFileName(originalFileName);

      // Validate file size
      if (content.length > FileConstants.MAX_IMAGE_FILE_SIZE) {
        return String.format(
            "Image file size exceeds maximum allowed size of %d MB",
            FileConstants.MAX_IMAGE_FILE_SIZE / (1024 * 1024));
      }

      // Validate file extension using original filename
      int dotIndex = originalFileName.lastIndexOf('.');
      if (dotIndex <= 0 || dotIndex >= originalFileName.length() - 1) {
        return "Image file must have a valid extension";
      }

      String extension = originalFileName.substring(dotIndex + 1).toLowerCase();
      if (!"png".equals(extension)) {
        return "Only PNG files are allowed for images. Found: " + extension;
      }

      // Validate PNG signature
      if (!hasPngSignature(content)) {
        return "File with .png extension does not have a valid PNG signature";
      }

      // Validate image type
      if (getImageTypeRequirements(imageType) == null) {
        return "Invalid image type. Allowed types: LOGO, ICON, STRIP, THUMBNAIL, PASS_LOGO, ACI_WALLETRON_NOTIFICATION_LOGO";
      }

      log.debug("Basic validation passed for original file: {}", originalFileName);
      return ""; // Return empty string instead of null for successful validation

    } catch (Exception e) {
      log.error("Exception during basic validation: ", e);
      return "Image file validation failed: " + e.getMessage();
    }
  }

  /** Reactive validation of image dimensions (blocking I/O on separate scheduler) */
  private Mono<String> validateImageDimensionsReactive(byte[] content, String imageType) {
    return Mono.fromCallable(
            () -> {
              log.debug("Starting dimension validation for image type: {}", imageType);
              try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                  log.error("Failed to read image content - ImageIO.read returned null");
                  return "Unable to read image content - file may be corrupted";
                }

                int width = image.getWidth();
                int height = image.getHeight();
                log.info("Image dimensions detected: {}x{} for type: {}", width, height, imageType);

                ImageTypeRequirements requirements = getImageTypeRequirements(imageType);
                log.debug(
                    "Required dimensions for {}: {}x{}",
                    imageType,
                    requirements.getRequiredWidth(),
                    requirements.getRequiredHeight());

                if (width != requirements.getRequiredWidth()
                    || height != requirements.getRequiredHeight()) {
                  String errorMsg =
                      String.format(
                          "Invalid dimensions for %s image. Required: %dx%d, Found: %dx%d",
                          imageType,
                          requirements.getRequiredWidth(),
                          requirements.getRequiredHeight(),
                          width,
                          height);
                  log.warn(errorMsg);
                  return errorMsg;
                }

                log.info(
                    "Image validation passed for type: {}, dimensions: {}x{}",
                    imageType,
                    width,
                    height);
                return ""; // Return empty string instead of null for successful validation

              } catch (IOException e) {
                log.error("IOException during dimension validation: ", e);
                return "Failed to read image dimensions: " + e.getMessage();
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  /** Builds ResponseEntity for successful image upload */
  private ResponseEntity<Api<ImageFileUploadResponse>> buildImageUploadResponseEntity(
      FilePart filePart,
      String imageType,
      String fileName,
      byte[] fileContent,
      String walletronId,
      String fileUrl) {

    ImageFileUploadResponse responseData =
        ImageFileUploadResponse.builder()
            .fileName(fileName)
            .walletronId(walletronId)
            .imageType(imageType)
            .originalFileName(filePart.filename())
            .fileSize(fileContent.length)
            .fileUrl(fileUrl)
            .build();

    Api<ImageFileUploadResponse> apiResponse =
        new Api<>(
            HttpStatus.OK.toString(),
            "Image file uploaded successfully",
            Optional.of(responseData),
            Optional.empty());

    return ResponseEntity.ok(apiResponse);
  }

  /** Gets image type requirements based on type */
  private ImageTypeRequirements getImageTypeRequirements(String imageType) {
    if (imageType == null) {
      return null;
    }

    String normalizedType = imageType.toUpperCase().trim();
    return switch (normalizedType) {
      case "LOGO" -> new ImageTypeRequirements(150, 150);
      case "ICON" -> new ImageTypeRequirements(380, 126);
      case "STRIP" -> new ImageTypeRequirements(936, 330);
      case "THUMBNAIL" -> new ImageTypeRequirements(250, 250);
      case "PASS_LOGO" -> new ImageTypeRequirements(380, 120);
      case "ACI_WALLETRON_NOTIFICATION_LOGO" -> new ImageTypeRequirements(150, 150);
      default -> null;
    };
  }

  /** Checks if content has a valid PNG signature */
  private boolean hasPngSignature(byte[] content) {
    return content.length >= 8
        && content[0] == (byte) 0x89
        && content[1] == 0x50 // P
        && content[2] == 0x4E // N
        && content[3] == 0x47 // G
        && content[4] == 0x0D // \r
        && content[5] == 0x0A // \n
        && content[6] == 0x1A // \032
        && content[7] == 0x0A; // \n
  }

  /** Inner class to hold image type requirements */
  private static class ImageTypeRequirements {
    private final int requiredWidth;
    private final int requiredHeight;

    public ImageTypeRequirements(int requiredWidth, int requiredHeight) {
      this.requiredWidth = requiredWidth;
      this.requiredHeight = requiredHeight;
    }

    public int getRequiredWidth() {
      return requiredWidth;
    }

    public int getRequiredHeight() {
      return requiredHeight;
    }
  }

  @Override
  public Mono<ResponseEntity<Api<String>>> deleteImageFileByName(String fileName) {
    log.info("Attempting to delete image file by name: {}", fileName);

    // First check if file exists, then delete it
    return blobStorageService
        .listFiles()
        .any(existingFile -> existingFile.equals(fileName))
        .flatMap(
            fileExists -> {
              if (Boolean.FALSE.equals(fileExists)) {
                log.info("Image file does not exist: {}", fileName);
                Api<String> response =
                    new Api<>(
                        HttpStatus.NOT_FOUND.toString(),
                        "Image file not found: " + fileName,
                        Optional.empty(),
                        Optional.of(Map.of("error", "Image not found: " + fileName)));
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
              }

              // File exists, proceed with deletion
              return blobStorageService
                  .deleteFile(fileName)
                  .then(
                      Mono.fromCallable(
                          () -> {
                            log.info("Successfully deleted image file: {}", fileName);
                            Api<String> response =
                                new Api<>(
                                    HttpStatus.OK.toString(),
                                    "Image file deleted successfully",
                                    Optional.of("Image deleted: " + fileName),
                                    Optional.empty());
                            return ResponseEntity.ok(response);
                          }));
            })
        .onErrorMap(this::handleError);
  }

  /**
   * Fetches images from blob storage URLs and returns them as base64 encoded byte arrays
   *
   * @param blobUrls List of blob storage URLs to fetch and encode
   * @return Mono containing a Map with blob URLs as keys and base64 encoded byte arrays as values
   */
  @Override
  public Mono<Map<String, byte[]>> getBase64EncodedImages(List<String> blobUrls) {
    if (blobUrls == null || blobUrls.isEmpty()) {
      return Mono.just(Map.of());
    }
    return Mono.fromCallable(() -> processBlobUrls(blobUrls))
        .subscribeOn(Schedulers.boundedElastic());
  }

  private Map<String, byte[]> processBlobUrls(List<String> blobUrls) {
    Map<String, byte[]> result = new HashMap<>();
    for (String blobUrl : blobUrls) {
      processSingleBlobUrl(blobUrl, result);
    }
    return result;
  }

  private void processSingleBlobUrl(String blobUrl, Map<String, byte[]> result) {
    try {
      String fileName = extractFileNameFromUrl(blobUrl);
      if (fileName == null) {
        log.warn("Could not extract filename from URL: {}", blobUrl);
        return;
      }
      byte[] fileBytes = blobStorageService.fetchFile(fileName).block();
      if (fileBytes == null || fileBytes.length == 0) {
        log.warn("No content found for URL: {}", blobUrl);
        return;
      }
      byte[] base64Bytes = Base64.getEncoder().encode(fileBytes);
      result.put(blobUrl, base64Bytes);
    } catch (Exception e) {
      log.error("Error processing URL {}: {}", blobUrl, e.getMessage());
    }
  }

  /**
   * Extracts filename from blob storage URL
   *
   * @param url The blob storage URL
   * @return The filename extracted from the URL, or null if extraction fails
   */
  private String extractFileNameFromUrl(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }

    try {
      // Try to extract filename from URL path
      String[] pathParts = url.split("/");
      if (pathParts.length > 0) {
        return pathParts[pathParts.length - 1];
      }
    } catch (Exception e) {
      log.warn("Error extracting filename from URL: {}", url, e);
    }

    return null;
  }
}
