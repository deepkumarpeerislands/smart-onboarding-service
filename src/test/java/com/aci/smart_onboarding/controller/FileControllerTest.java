package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ImageFileUploadResponse;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.DecryptionException;
import com.aci.smart_onboarding.exception.EncryptionException;
import com.aci.smart_onboarding.exception.InvalidFileException;
import com.aci.smart_onboarding.exception.JsonFileValidationException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IFileService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileControllerTest {

  @Mock private IFileService fileService;
  @Mock private BRDSecurityService securityService;

  @InjectMocks private FileController fileController;

  private static final String TEST_BRD_ID = "BRD-123";
  private static final String TEST_FILE_TYPE = "ACH";
  private static final String TEST_WALLETRON_ID = "WALLET123";
  private static final String TEST_USERNAME = "testuser";

  @BeforeEach
  void setUp() {
    // Reset all mocks before each test
    reset(fileService, securityService);
  }

  private void mockSecurityContextForPM() {
    // Mock the security service
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
  }

  @Test
  @DisplayName("Should successfully upload a valid file")
  void uploadFile_WithValidFile_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    byte[] fileContent =
        new byte[] {
          0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A,
          0x74, 0x65, 0x73, 0x74, 0x20, 0x63, 0x6F, 0x6E, 0x74
        };
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    when(fileService.uploadEncryptedFile(
            eq(filePart), any(byte[].class), eq(TEST_FILE_TYPE), eq(TEST_BRD_ID)))
        .thenReturn(Mono.just("https://test.blob.url/test.pdf"));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, TEST_FILE_TYPE, TEST_BRD_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().getMessage().equals("File uploaded successfully")
                    && result.getBody().getData().isPresent()
                    && result.getBody().getData().get().equals("https://test.blob.url/test.pdf"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return error when file validation fails")
  void uploadFile_WithInvalidFile_ShouldReturnBadRequest() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(10L);

    byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("invalid.xyz");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    when(fileService.uploadEncryptedFile(
            eq(filePart), any(byte[].class), eq(TEST_FILE_TYPE), eq(TEST_BRD_ID)))
        .thenReturn(
            Mono.error(
                new InvalidFileException("Invalid file extension. Only PDF files are allowed.")));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, TEST_FILE_TYPE, TEST_BRD_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && result.getBody().getMessage().contains("Invalid file extension"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return error when blob storage upload fails")
  void uploadFile_WithStorageError_ShouldReturnServerError() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    byte[] fileContent =
        new byte[] {
          0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A,
          0x74, 0x65, 0x73, 0x74, 0x20, 0x63, 0x6F, 0x6E, 0x74,
          0x25, 0x25, 0x45, 0x4F, 0x46
        };
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    when(fileService.uploadEncryptedFile(
            eq(filePart), any(byte[].class), eq(TEST_FILE_TYPE), eq(TEST_BRD_ID)))
        .thenReturn(Mono.error(new IOException("Storage error")));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, TEST_FILE_TYPE, TEST_BRD_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && result.getBody() != null
                    && result.getBody().getMessage().contains("Error processing file")
                    && result.getBody().getData().isEmpty()
                    && result.getBody().getErrors().isPresent())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return generic error when encryption fails")
  void uploadFile_WithEncryptionError_ShouldReturnServerError() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    byte[] fileContent =
        new byte[] {
          0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A,
          0x74, 0x65, 0x73, 0x74, 0x20, 0x63, 0x6F, 0x6E, 0x74,
          0x25, 0x25, 0x45, 0x4F, 0x46
        };
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    when(fileService.uploadEncryptedFile(
            eq(filePart), any(byte[].class), eq(TEST_FILE_TYPE), eq(TEST_BRD_ID)))
        .thenReturn(Mono.error(new EncryptionException("Encryption operation failed")));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, TEST_FILE_TYPE, TEST_BRD_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && result.getBody() != null
                    && result.getBody().getMessage().equals("Error processing file")
                    && result.getBody().getData().isEmpty()
                    && result.getBody().getErrors().isPresent())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully download a file")
  void downloadFile_WithValidFileName_ReturnsFile() {
    // Arrange
    String originalFileName = "test-document.pdf";
    byte[] fileContent = "PDF document content".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(fileContent);

    when(fileService.downloadFile(originalFileName)).thenReturn(Mono.just(resource));

    // Act
    Mono<ResponseEntity<Resource>> response = fileController.downloadFile(originalFileName);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result -> {
              // Verify status code and headers
              assertEquals(HttpStatus.OK, result.getStatusCode());

              // Verify content type
              assertEquals(MediaType.APPLICATION_PDF, result.getHeaders().getContentType());

              // Verify content disposition
              String disposition = result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
              assertNotNull(disposition);
              assertTrue(disposition.contains("attachment"));
              assertTrue(disposition.contains(originalFileName));

              // Verify body is the expected resource
              assertEquals(resource, result.getBody());

              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 404 when file not found")
  void downloadFile_WithNonexistentFile_Returns404() {
    // Arrange
    String nonExistentFile = "missing-file.pdf";
    when(fileService.downloadFile(nonExistentFile))
        .thenReturn(Mono.error(new RuntimeException("File not found: " + nonExistentFile)));

    // Act
    Mono<ResponseEntity<Resource>> response = fileController.downloadFile(nonExistentFile);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result -> {
              // Verify status code
              assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

              // Verify content type is JSON (for error response)
              assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());

              // Verify body contains error message
              Resource body = result.getBody();
              byte[] content = new byte[0];
              try {
                content = body.getInputStream().readAllBytes();
                String errorResponse = new String(content, StandardCharsets.UTF_8);
                return errorResponse.contains("File not found")
                    && errorResponse.contains(nonExistentFile);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 500 when decryption fails")
  void downloadFile_WithDecryptionError_Returns500() {
    // Arrange
    String fileName = "encrypted-file.pdf";
    when(fileService.downloadFile(fileName))
        .thenReturn(Mono.error(new DecryptionException("Error decrypting file")));

    // Act
    Mono<ResponseEntity<Resource>> response = fileController.downloadFile(fileName);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result -> {
              // Verify status code
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());

              // Verify content type is JSON (for error response)
              assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());

              // Verify body contains error message
              Resource body = result.getBody();
              byte[] content = new byte[0];
              try {
                content = body.getInputStream().readAllBytes();
                String errorResponse = new String(content, StandardCharsets.UTF_8);
                return errorResponse.contains("Error decrypting file");
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should list all files successfully")
  void listFiles_ShouldReturnAllFiles() {
    // Arrange
    List<String> fileList = Arrays.asList("file1.pdf", "file2.pdf", "file3.txt");
    when(fileService.listFiles()).thenReturn(Mono.just(fileList));

    // Act
    Mono<ResponseEntity<Api<List<String>>>> response = fileController.listFiles();

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result -> {
              // Verify status code
              assertEquals(HttpStatus.OK, result.getStatusCode());

              // Verify response body
              Api<List<String>> body = result.getBody();
              assertNotNull(body);
              assertEquals("Files retrieved successfully", body.getMessage());
              assertTrue(body.getData().isPresent());
              assertEquals(fileList, body.getData().get());

              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should validate file type parameter")
  void uploadFile_WithInvalidFileType_ShouldReturnBadRequest() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, "INVALID_TYPE", "BRD-123");

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && result.getBody().getMessage().contains("Invalid file type")
                    && result.getBody().getData().isEmpty()
                    && result.getBody().getErrors().isPresent())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should update BRD timestamp for ACH file")
  void uploadFile_WithACHFile_ShouldUpdateTimestamp() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    byte[] fileContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    String expectedUrl = "https://test.blob.url/test.pdf";
    when(fileService.uploadEncryptedFile(eq(filePart), any(byte[].class), eq("ACH"), eq("BRD-123")))
        .thenReturn(Mono.just(expectedUrl));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, "ACH", "BRD-123");

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().getMessage().contains("File uploaded successfully")
                    && result.getBody().getData().isPresent()
                    && result.getBody().getData().get().equals(expectedUrl))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should update BRD timestamp for WALLETRON file")
  void uploadFile_WithWalletronFile_ShouldUpdateTimestamp() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentLength(100L);

    byte[] fileContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

    when(filePart.filename()).thenReturn("test.pdf");
    when(filePart.headers()).thenReturn(headers);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    String expectedUrl = "https://test.blob.url/test.pdf";
    when(fileService.uploadEncryptedFile(
            eq(filePart), any(byte[].class), eq("WALLETRON"), eq("BRD-123")))
        .thenReturn(Mono.just(expectedUrl));

    // Act
    Mono<ResponseEntity<Api<String>>> response =
        fileController.uploadFile(filePart, "WALLETRON", "BRD-123");

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().getMessage().contains("File uploaded successfully")
                    && result.getBody().getData().isPresent()
                    && result.getBody().getData().get().equals(expectedUrl))
        .verifyComplete();
  }

  // New Test Cases for uploadImageFile endpoint

  @Test
  @DisplayName("Should successfully upload LOGO image file")
  void uploadImageFile_WithValidLogoImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = createMockFilePart("logo.png");

    ImageFileUploadResponse expectedResponse =
        ImageFileUploadResponse.builder()
            .fileName(TEST_WALLETRON_ID + "-logo")
            .walletronId(TEST_WALLETRON_ID)
            .imageType("LOGO")
            .originalFileName("logo.png")
            .fileSize(1000) // approximate size
            .fileUrl("https://storage/logo-url")
            .build();

    ResponseEntity<Api<ImageFileUploadResponse>> expectedApiResponse =
        ResponseEntity.ok(
            new Api<>(
                "200",
                "Image file uploaded successfully",
                Optional.of(expectedResponse),
                Optional.empty()));

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.just(expectedApiResponse));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Image file uploaded successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData().isPresent());
                assertEquals(
                    TEST_WALLETRON_ID + "-logo", response.getBody().getData().get().getFileName());
                assertEquals(
                    "https://storage/logo-url", response.getBody().getData().get().getFileUrl());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should successfully upload ICON image file")
  void uploadImageFile_WithValidIconImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = createMockFilePart("icon.png");

    ImageFileUploadResponse expectedResponse =
        ImageFileUploadResponse.builder()
            .fileName(TEST_WALLETRON_ID + "-icon")
            .walletronId(TEST_WALLETRON_ID)
            .imageType("ICON")
            .originalFileName("icon.png")
            .fileSize(1000) // approximate size
            .fileUrl("https://storage/icon-url")
            .build();

    ResponseEntity<Api<ImageFileUploadResponse>> expectedApiResponse =
        ResponseEntity.ok(
            new Api<>(
                "200",
                "Image file uploaded successfully",
                Optional.of(expectedResponse),
                Optional.empty()));

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("ICON"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.just(expectedApiResponse));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "ICON", TEST_WALLETRON_ID))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Image file uploaded successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData().isPresent());
                assertEquals(
                    TEST_WALLETRON_ID + "-icon", response.getBody().getData().get().getFileName());
                assertEquals(
                    "https://storage/icon-url", response.getBody().getData().get().getFileUrl());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return validation error for invalid image type")
  void uploadImageFile_WithInvalidImageType_ShouldReturnValidationError() {
    // Arrange
    FilePart filePart = createMockFilePart("test.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("INVALID"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.error(new JsonFileValidationException("Invalid image type")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "INVALID", TEST_WALLETRON_ID))
          .expectError(JsonFileValidationException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should return validation error for non-PNG file")
  void uploadImageFile_WithNonPngFile_ShouldReturnValidationError() {
    // Arrange
    FilePart filePart = createMockFilePart("test.jpg");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(
            Mono.error(new JsonFileValidationException("Only PNG files are allowed for images")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(JsonFileValidationException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should return conflict error when image already exists")
  void uploadImageFile_WithExistingImage_ShouldReturnConflictError() {
    // Arrange
    FilePart filePart = createMockFilePart("logo.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(
            Mono.error(
                new AlreadyExistException(
                    "Image with walletronId '"
                        + TEST_WALLETRON_ID
                        + "' and type 'LOGO' already exists")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(AlreadyExistException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should return server error when blob storage fails")
  void uploadImageFile_WithStorageFailure_ShouldReturnServerError() {
    // Arrange
    FilePart filePart = createMockFilePart("logo.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.error(new RuntimeException("Storage failure")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  // New Test Cases for deleteImage endpoint

  @Test
  @DisplayName("Should successfully delete existing image file")
  void deleteImage_WithExistingFile_ShouldReturnSuccess() {
    // Arrange
    String fileName = TEST_WALLETRON_ID + "-logo";

    ResponseEntity<Api<String>> expectedResponse =
        ResponseEntity.ok(
            new Api<>(
                "200",
                "Image file deleted successfully",
                Optional.of("Image deleted: " + fileName),
                Optional.empty()));

    when(fileService.deleteImageFileByName(fileName)).thenReturn(Mono.just(expectedResponse));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.deleteImage(fileName))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Image file deleted successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData().isPresent());
                assertEquals("Image deleted: " + fileName, response.getBody().getData().get());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle blob storage error during deletion")
  void deleteImage_WithStorageError_ShouldReturnServerError() {
    // Arrange
    String fileName = TEST_WALLETRON_ID + "-logo";

    when(fileService.deleteImageFileByName(fileName))
        .thenReturn(Mono.error(new RuntimeException("Storage error")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.deleteImage(fileName))
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should upload STRIP image with correct dimensions")
  void uploadImageFile_WithValidStripImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = createMockFilePart("strip.png");

    ImageFileUploadResponse expectedResponse =
        ImageFileUploadResponse.builder()
            .fileName(TEST_WALLETRON_ID + "-strip")
            .walletronId(TEST_WALLETRON_ID)
            .imageType("STRIP")
            .originalFileName("strip.png")
            .fileSize(1000) // approximate size
            .fileUrl("https://storage/strip-url")
            .build();

    ResponseEntity<Api<ImageFileUploadResponse>> expectedApiResponse =
        ResponseEntity.ok(
            new Api<>(
                "200",
                "Image file uploaded successfully",
                Optional.of(expectedResponse),
                Optional.empty()));

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("STRIP"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.just(expectedApiResponse));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "STRIP", TEST_WALLETRON_ID))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Image file uploaded successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData().isPresent());
                assertEquals(
                    TEST_WALLETRON_ID + "-strip", response.getBody().getData().get().getFileName());
                assertEquals(
                    "https://storage/strip-url", response.getBody().getData().get().getFileUrl());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should upload THUMBNAIL image with correct dimensions")
  void uploadImageFile_WithValidThumbnailImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = createMockFilePart("thumbnail.png");

    ImageFileUploadResponse expectedResponse =
        ImageFileUploadResponse.builder()
            .fileName(TEST_WALLETRON_ID + "-thumbnail")
            .walletronId(TEST_WALLETRON_ID)
            .imageType("THUMBNAIL")
            .originalFileName("thumbnail.png")
            .fileSize(1000) // approximate size
            .fileUrl("https://storage/thumbnail-url")
            .build();

    ResponseEntity<Api<ImageFileUploadResponse>> expectedApiResponse =
        ResponseEntity.ok(
            new Api<>(
                "200",
                "Image file uploaded successfully",
                Optional.of(expectedResponse),
                Optional.empty()));

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("THUMBNAIL"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.just(expectedApiResponse));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "THUMBNAIL", TEST_WALLETRON_ID))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Image file uploaded successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData().isPresent());
                assertEquals(
                    TEST_WALLETRON_ID + "-thumbnail",
                    response.getBody().getData().get().getFileName());
                assertEquals(
                    "https://storage/thumbnail-url",
                    response.getBody().getData().get().getFileUrl());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle empty file content for image upload")
  void uploadImageFile_WithEmptyContent_ShouldReturnValidationError() {
    // Arrange
    FilePart filePart = createMockFilePart("empty.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.error(new JsonFileValidationException("File content is empty")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(JsonFileValidationException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should handle file size exceeded for image upload")
  void uploadImageFile_WithOversizedFile_ShouldReturnValidationError() {
    // Arrange
    FilePart filePart = createMockFilePart("large.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(
            Mono.error(
                new JsonFileValidationException(
                    "Image file size exceeds maximum allowed size of 1 MB")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(JsonFileValidationException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should handle invalid PNG signature")
  void uploadImageFile_WithInvalidPngSignature_ShouldReturnValidationError() {
    // Arrange
    FilePart filePart = createMockFilePart("invalid.png");

    when(fileService.uploadImageFile(
            any(FilePart.class), any(byte[].class), eq("LOGO"), eq(TEST_WALLETRON_ID)))
        .thenReturn(Mono.error(new JsonFileValidationException("Invalid PNG signature")));

    // Mock security context
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      mockSecurityContextForPM();

      // Act & Assert
      StepVerifier.create(fileController.uploadImageFile(filePart, "LOGO", TEST_WALLETRON_ID))
          .expectError(JsonFileValidationException.class)
          .verify();
    }
  }

  // Helper method to create valid PNG content with specified dimensions
  private byte[] createValidPngContent(int width, int height) {
    // Create a minimal valid PNG with proper signature and basic structure
    byte[] pngContent = new byte[width * height * 4 + 100]; // RGBA + header space

    // PNG signature
    pngContent[0] = (byte) 0x89;
    pngContent[1] = 0x50; // P
    pngContent[2] = 0x4E; // N
    pngContent[3] = 0x47; // G
    pngContent[4] = 0x0D; // \r
    pngContent[5] = 0x0A; // \n
    pngContent[6] = 0x1A; // \032
    pngContent[7] = 0x0A; // \n

    // Fill with some basic PNG structure data (simplified)
    // This is a mock PNG that will pass basic validation but may not be a fully valid image
    for (int i = 8; i < pngContent.length; i++) {
      pngContent[i] = (byte) (i % 256);
    }

    return pngContent;
  }

  private FilePart createMockFilePart(String filename) {
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn(filename);

    // Create actual PNG content based on filename
    byte[] content;
    if (filename.contains("logo")) {
      content = createValidPngContent(150, 150);
    } else if (filename.contains("icon")) {
      content = createValidPngContent(380, 126);
    } else if (filename.contains("strip")) {
      content = createValidPngContent(936, 330);
    } else if (filename.contains("thumbnail")) {
      content = createValidPngContent(250, 250);
    } else {
      content = createValidPngContent(150, 150); // default
    }

    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(content);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    return filePart;
  }
}
