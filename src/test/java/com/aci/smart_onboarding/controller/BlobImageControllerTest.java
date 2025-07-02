package com.aci.smart_onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.ImageResponseDto;
import com.aci.smart_onboarding.dto.ImageUploadRequest;
import com.aci.smart_onboarding.dto.ImageUploadResponse;
import com.aci.smart_onboarding.model.WalletronExampleImages;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlobImageControllerTest {

  @Mock private IBlobStorageService blobStorageService;

  @Mock private ObjectMapper objectMapper;

  @Mock private BRDSecurityService securityService;

  @InjectMocks private BlobImageController blobImageController;

  private static final String TEST_DOCUMENT_ID = "doc-123";
  private static final String TEST_FILE_NAME = "test-image.jpg";
  private static final String TEST_IMAGE_URL = "https://test.blob.url/test-image.jpg";
  private static final String TEST_TITLE = "Test Image";
  private static final String TEST_URL = "https://example.com/test.jpg";

  @BeforeEach
  void setUp() {
    reset(blobStorageService, objectMapper, securityService);
    // Mock security service to return MANAGER role by default
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
  }

  @Test
  @DisplayName("Should successfully upload images")
  void uploadImages_WithValidRequest_ShouldReturnSuccess() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests =
        Arrays.asList(
            ImageUploadRequest.builder().title("Image 1").build(),
            ImageUploadRequest.builder().title("Image 2").build());
    String requestsJson = "[{\"title\":\"Image 1\"},{\"title\":\"Image 2\"}]";

    List<ImageResponseDto> imageResponses =
        Arrays.asList(
            ImageResponseDto.builder()
                .id("doc-1")
                .fileName("image1.jpg")
                .url("https://blob.url/image1.jpg")
                .title("Image 1")
                .build(),
            ImageResponseDto.builder()
                .id("doc-2")
                .fileName("image2.jpg")
                .url("https://blob.url/image2.jpg")
                .title("Image 2")
                .build());

    ImageUploadResponse expectedResponse =
        ImageUploadResponse.builder()
            .message("Images uploaded successfully")
            .data(imageResponses)
            .success(true)
            .build();

    FilePart filePart1 = createMockFilePart("image1.jpg");
    FilePart filePart2 = createMockFilePart("image2.jpg");
    Flux<FilePart> files = Flux.just(filePart1, filePart2);

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.just(expectedResponse));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().isSuccess()
                    && result.getBody().getData().size() == 2
                    && result.getBody().getMessage().equals("Images uploaded successfully"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle JSON parsing error during upload")
  void uploadImages_WithInvalidJson_ShouldReturnError() throws Exception {
    // Arrange
    String invalidJson = "invalid json";
    FilePart filePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(filePart);

    when(objectMapper.readValue(eq(invalidJson), any(TypeReference.class)))
        .thenThrow(new RuntimeException("Invalid JSON"));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(invalidJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Failed to process images")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle service error during upload")
  void uploadImages_WithServiceError_ShouldReturnError() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests =
        Arrays.asList(ImageUploadRequest.builder().title("Image 1").build());
    String requestsJson = "[{\"title\":\"Image 1\"}]";
    FilePart filePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(filePart);

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Failed to process images")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle access denied for upload images")
  void uploadImages_WithAccessDenied_ShouldReturnError()  {
    // Arrange

    String requestsJson = "[{\"title\":\"Image 1\"}]";
    FilePart filePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(filePart);

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just("USER")); // Non-manager role

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectError(AccessDeniedException.class)
        .verify();
  }

  @Test
  @DisplayName("Should successfully get document by ID")
  void getDocumentById_WithValidId_ShouldReturnDocument() {
    // Arrange
    ImageResponseDto imageResponse =
        ImageResponseDto.builder()
            .id(TEST_DOCUMENT_ID)
            .fileName(TEST_FILE_NAME)
            .url(TEST_IMAGE_URL)
            .title(TEST_TITLE)
            .build();

    WalletronExampleImages document =
        WalletronExampleImages.builder().id(TEST_DOCUMENT_ID).image(imageResponse).build();

    when(blobStorageService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(Mono.just(document));

    // Act
    Mono<ResponseEntity<WalletronExampleImages>> response =
        blobImageController.getDocumentById(TEST_DOCUMENT_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().getId().equals(TEST_DOCUMENT_ID)
                    && result.getBody().getImage().getFileName().equals(TEST_FILE_NAME))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 404 when document not found")
  void getDocumentById_WithInvalidId_ShouldReturnNotFound() {
    // Arrange
    when(blobStorageService.getDocumentById("invalid-id")).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<WalletronExampleImages>> response =
        blobImageController.getDocumentById("invalid-id");

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(result -> result.getStatusCode() == HttpStatus.NOT_FOUND)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle service error when getting document by ID")
  void getDocumentById_WithServiceError_ShouldReturnServerError() {
    // Arrange
    when(blobStorageService.getDocumentById(TEST_DOCUMENT_ID))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // Act
    Mono<ResponseEntity<WalletronExampleImages>> response =
        blobImageController.getDocumentById(TEST_DOCUMENT_ID);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(result -> result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully get all images")
  void getAllImages_WithValidRequest_ShouldReturnAllImages() {
    // Arrange
    List<ImageResponseDto> images =
        Arrays.asList(
            ImageResponseDto.builder()
                .id("doc-1")
                .fileName("image1.jpg")
                .url("https://blob.url/image1.jpg")
                .title("Image 1")
                .build(),
            ImageResponseDto.builder()
                .id("doc-2")
                .fileName("image2.jpg")
                .url("https://blob.url/image2.jpg")
                .title("Image 2")
                .build());

    when(blobStorageService.getAllImages()).thenReturn(Mono.just(images));

    // Act
    Mono<ResponseEntity<List<ImageResponseDto>>> response = blobImageController.getAllImages();

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().size() == 2
                    && result.getBody().get(0).getFileName().equals("image1.jpg"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return empty list when no images exist")
  void getAllImages_WithNoImages_ShouldReturnEmptyList() {
    // Arrange
    when(blobStorageService.getAllImages()).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<List<ImageResponseDto>>> response = blobImageController.getAllImages();

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle service error when getting all images")
  void getAllImages_WithServiceError_ShouldReturnServerError() {
    // Arrange
    when(blobStorageService.getAllImages())
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // Act
    Mono<ResponseEntity<List<ImageResponseDto>>> response = blobImageController.getAllImages();

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(result -> result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully delete image by ID")
  void deleteImageById_WithValidId_ShouldReturnSuccess() {
    // Arrange
    when(blobStorageService.deleteImageById(TEST_DOCUMENT_ID)).thenReturn(Mono.empty());

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.deleteImageById(TEST_DOCUMENT_ID))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Image deleted successfully")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent image")
  void deleteImageById_WithInvalidId_ShouldReturnNotFound() {
    // Arrange
    when(blobStorageService.deleteImageById("invalid-id"))
        .thenReturn(Mono.error(new IllegalArgumentException("Document not found")));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.deleteImageById("invalid-id"))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.NOT_FOUND
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result
                        .getBody()
                        .getMessage()
                        .equals("Document not found with ID: invalid-id"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle service error when deleting image")
  void deleteImageById_WithServiceError_ShouldReturnServerError() {
    // Arrange
    when(blobStorageService.deleteImageById(TEST_DOCUMENT_ID))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.deleteImageById(TEST_DOCUMENT_ID))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Failed to delete image"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle access denied for delete image")
  void deleteImageById_WithAccessDenied_ShouldReturnError() {
    // Arrange
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just("USER")); // Non-manager role

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.deleteImageById(TEST_DOCUMENT_ID))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectError(AccessDeniedException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle general error when deleting image")
  void deleteImageById_WithGeneralError_ShouldReturnServerError() {
    // Arrange
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(blobStorageService.deleteImageById(TEST_DOCUMENT_ID))
        .thenReturn(Mono.error(new RuntimeException("General error")));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.deleteImageById(TEST_DOCUMENT_ID))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Failed to delete image"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully proxy file from URL")
  void proxyFile_WithValidUrl_ShouldReturnFileContent() {
    // Arrange
    byte[] fileContent = "test file content".getBytes();
    ResponseEntity<byte[]> expectedResponse =
        ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .body(fileContent);

    when(blobStorageService.proxyFileFromUrl(TEST_URL)).thenReturn(Mono.just(expectedResponse));

    // Act
    Mono<ResponseEntity<byte[]>> response = blobImageController.proxyFile(TEST_URL);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().length == fileContent.length)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle proxy file error")
  void proxyFile_WithServiceError_ShouldPropagateError() {
    // Arrange
    String testUrl = "https://example.com/test.jpg";
    when(blobStorageService.proxyFileFromUrl(testUrl))
        .thenReturn(Mono.error(new RuntimeException("Proxy error")));

    // Act & Assert
    StepVerifier.create(blobImageController.proxyFile(testUrl))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle empty file content during upload")
  void uploadImages_WithEmptyFileContent_ShouldReturnError() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests =
        Arrays.asList(ImageUploadRequest.builder().title("Image 1").build());
    String requestsJson = "[{\"title\":\"Image 1\"}]";
    
    // Create empty file part
    FilePart emptyFilePart = mock(FilePart.class);
    when(emptyFilePart.filename()).thenReturn("empty.jpg");
    when(emptyFilePart.content()).thenReturn(Flux.empty());
    
    Flux<FilePart> files = Flux.just(emptyFilePart);

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.error(new RuntimeException("Empty file content")));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && !result.getBody().isSuccess()
                    && result.getBody().getMessage().equals("Failed to process images"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple files upload successfully")
  void uploadImages_WithMultipleFiles_ShouldReturnSuccess() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests =
        Arrays.asList(
            ImageUploadRequest.builder().title("Image 1").build(),
            ImageUploadRequest.builder().title("Image 2").build(),
            ImageUploadRequest.builder().title("Image 3").build());
    String requestsJson = "[{\"title\":\"Image 1\"},{\"title\":\"Image 2\"},{\"title\":\"Image 3\"}]";

    List<ImageResponseDto> imageResponses =
        Arrays.asList(
            ImageResponseDto.builder()
                .id("doc-1")
                .fileName("image1.jpg")
                .url("https://blob.url/image1.jpg")
                .title("Image 1")
                .build(),
            ImageResponseDto.builder()
                .id("doc-2")
                .fileName("image2.jpg")
                .url("https://blob.url/image2.jpg")
                .title("Image 2")
                .build(),
            ImageResponseDto.builder()
                .id("doc-3")
                .fileName("image3.jpg")
                .url("https://blob.url/image3.jpg")
                .title("Image 3")
                .build());

    ImageUploadResponse expectedResponse =
        ImageUploadResponse.builder()
            .message("Images uploaded successfully")
            .data(imageResponses)
            .success(true)
            .build();

    FilePart filePart1 = createMockFilePart("image1.jpg");
    FilePart filePart2 = createMockFilePart("image2.jpg");
    FilePart filePart3 = createMockFilePart("image3.jpg");
    Flux<FilePart> files = Flux.just(filePart1, filePart2, filePart3);

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.just(expectedResponse));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().isSuccess()
                    && result.getBody().getData().size() == 3)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty requests list")
  void uploadImages_WithEmptyRequests_ShouldReturnSuccess() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests = Collections.emptyList();
    String requestsJson = "[]";

    ImageUploadResponse expectedResponse =
        ImageUploadResponse.builder()
            .message("Images uploaded successfully")
            .data(Collections.emptyList())
            .success(true)
            .build();

    Flux<FilePart> files = Flux.empty();

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.just(expectedResponse));

    Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().isSuccess()
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle getUsernameFromContext error")
  void uploadImages_WithUsernameContextError_ShouldReturnError() throws Exception {
    // Arrange
    List<ImageUploadRequest> requests =
        Arrays.asList(ImageUploadRequest.builder().title("Image 1").build());
    String requestsJson = "[{\"title\":\"Image 1\"}]";
    FilePart filePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(filePart);

    when(objectMapper.readValue(eq(requestsJson), any(TypeReference.class))).thenReturn(requests);
    when(blobStorageService.uploadImagesWithDocumentsAndMessage(requests, files))
        .thenReturn(Mono.error(new RuntimeException("Context error")));

    // Create authentication with null name
    Authentication auth = new UsernamePasswordAuthenticationToken(null, "password");
    Mono<ResponseEntity<ImageUploadResponse>> response =
        Mono.deferContextual(ctx -> blobImageController.uploadImages(requestsJson, files))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && !result.getBody().isSuccess())
        .verifyComplete();
  }

  private FilePart createMockFilePart(String filename) {
    FilePart filePart = mock(FilePart.class);
    byte[] content = "test file content".getBytes();
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(content);

    when(filePart.filename()).thenReturn(filename);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    return filePart;
  }
}
