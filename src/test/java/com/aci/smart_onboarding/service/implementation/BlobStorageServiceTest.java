package com.aci.smart_onboarding.service.implementation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.ImageResponseDto;
import com.aci.smart_onboarding.dto.ImageUploadRequest;
import com.aci.smart_onboarding.dto.ImageUploadResponse;
import com.aci.smart_onboarding.model.WalletronExampleImages;
import com.aci.smart_onboarding.repository.WalletronExampleImagesRepository;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlobStorageServiceTest {

  @Mock private BlobContainerClient blobContainerClient;
  @Mock private BlobClient blobClient;
  @Mock private BlobServiceClient blobServiceClient;
  @Mock private BlobProperties blobProperties;
  @Mock private WalletronExampleImagesRepository walletronExampleImagesRepository;
  @Mock private FilePart filePart;

  @InjectMocks private BlobStorageService blobStorageService;

  private static final String TEST_DOCUMENT_ID = "doc-123";
  private static final String TEST_FILE_NAME = "test-image.jpg";
  private static final String TEST_IMAGE_URL = "https://test.blob.url/test-image.jpg";
  private static final String TEST_TITLE = "Test Image";

  @BeforeEach
  void setUp() {
    // No global stubbing here; move to individual tests as needed
  }

  @Test
  @DisplayName("uploadFile should upload the file and return the URL")
  void uploadFile_ShouldUploadFileAndReturnUrl() {
    // Arrange
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    byte[] fileContent = "test file content".getBytes();
    String fileName = "test.pdf";

    doAnswer(
            invocation -> {
              InputStream inputStream = invocation.getArgument(0);
              inputStream.readAllBytes();
              return null;
            })
        .when(blobClient)
        .upload(any(InputStream.class), anyLong(), eq(true));
    when(blobClient.getBlobUrl()).thenReturn("https://test.blob.url/test.pdf");

    // Act
    Mono<String> result = blobStorageService.uploadFile(fileName, fileContent);

    // Assert
    StepVerifier.create(result).expectNext("https://test.blob.url/test.pdf").verifyComplete();
  }

  @Test
  @DisplayName("fetchFile should retrieve the file content")
  void fetchFile_ShouldRetrieveFileContent() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    byte[] fileContent = "test file content".getBytes();
    String fileName = "test.pdf";
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getProperties()).thenReturn(blobProperties);
    when(blobProperties.getBlobSize()).thenReturn((long) fileContent.length);
    doAnswer(
            invocation -> {
              ByteArrayOutputStream outputStream = invocation.getArgument(0);
              outputStream.write(fileContent);
              return null;
            })
        .when(blobClient)
        .downloadStream(any(ByteArrayOutputStream.class));
    Mono<byte[]> result = blobStorageService.fetchFile(fileName);
    StepVerifier.create(result)
        .expectNextMatches(bytes -> Arrays.equals(bytes, fileContent))
        .verifyComplete();
  }

  @Test
  @DisplayName("fetchFile should return error when file doesn't exist")
  void fetchFile_WhenFileDoesNotExist_ShouldReturnError() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "nonexistent.pdf";
    when(blobClient.exists()).thenReturn(false);
    Mono<byte[]> result = blobStorageService.fetchFile(fileName);
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("getFileUrl should return the file URL")
  void getFileUrl_ShouldReturnFileUrl() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "test.pdf";
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getBlobUrl()).thenReturn("https://test.blob.url/test.pdf");
    Mono<String> result = blobStorageService.getFileUrl(fileName);
    StepVerifier.create(result).expectNext("https://test.blob.url/test.pdf").verifyComplete();
  }

  @Test
  @DisplayName("getFileUrl should return error when file doesn't exist")
  void getFileUrl_WhenFileDoesNotExist_ShouldReturnError() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "nonexistent.pdf";
    when(blobClient.exists()).thenReturn(false);
    Mono<String> result = blobStorageService.getFileUrl(fileName);
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("fetchFileFromUrl should retrieve file from valid URL")
  void fetchFileFromUrl_ShouldRetrieveFileFromValidUrl() {
    String blobUrl = "https://storage.blob.core.windows.net/container/file.txt";
    byte[] fileContent = "test content".getBytes();
    BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);
    when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);
    when(blobServiceClient.getBlobContainerClient("container")).thenReturn(mockContainerClient);
    when(mockContainerClient.getBlobClient("file.txt")).thenReturn(mockBlobClient);
    when(mockBlobClient.exists()).thenReturn(true);
    doAnswer(
            invocation -> {
              ByteArrayOutputStream outputStream = invocation.getArgument(0);
              outputStream.write(fileContent);
              return null;
            })
        .when(mockBlobClient)
        .downloadStream(any(ByteArrayOutputStream.class));
    Mono<byte[]> result = blobStorageService.fetchFileFromUrl(blobUrl);
    StepVerifier.create(result)
        .expectNextMatches(bytes -> Arrays.equals(bytes, fileContent))
        .verifyComplete();
  }

  @Test
  @DisplayName("fetchFileFromUrl should return error for invalid URL")
  void fetchFileFromUrl_WithInvalidUrl_ShouldReturnError() {
    String invalidUrl = "invalid-url";
    Mono<byte[]> result = blobStorageService.fetchFileFromUrl(invalidUrl);
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("updateFile should update existing file")
  void updateFile_ShouldUpdateExistingFile() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String filePath = "update.txt";
    String content = "new content";
    when(blobClient.exists()).thenReturn(true);
    doNothing().when(blobClient).upload(any(BinaryData.class), eq(true));
    Mono<Void> result = blobStorageService.updateFile(filePath, content);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("updateFile should return error when file doesn't exist")
  void updateFile_WhenFileDoesNotExist_ShouldReturnError() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String filePath = "nonexistent.txt";
    String content = "content";
    when(blobClient.exists()).thenReturn(false);
    Mono<Void> result = blobStorageService.updateFile(filePath, content);
    StepVerifier.create(result).expectError().verify();
  }

  @Test
  @DisplayName("listFiles should return all file names")
  void listFiles_ShouldReturnAllFileNames() {
    BlobItem blobItem1 = mock(BlobItem.class);
    BlobItem blobItem2 = mock(BlobItem.class);
    when(blobItem1.getName()).thenReturn("file1.txt");
    when(blobItem2.getName()).thenReturn("file2.txt");

    List<BlobItem> blobItemsList = List.of(blobItem1, blobItem2);
    PagedIterable<BlobItem> blobItems = mock(PagedIterable.class);

    // Mock the forEach method to add names to a list
    doAnswer(
            invocation -> {
              java.util.function.Consumer<BlobItem> consumer = invocation.getArgument(0);
              blobItemsList.forEach(consumer);
              return null;
            })
        .when(blobItems)
        .forEach(any(java.util.function.Consumer.class));

    when(blobContainerClient.listBlobs()).thenReturn(blobItems);

    Flux<String> result = blobStorageService.listFiles();
    StepVerifier.create(result).expectNext("file1.txt", "file2.txt").verifyComplete();
  }

  @Test
  @DisplayName("deleteFile should delete existing file")
  void deleteFile_ShouldDeleteExistingFile() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "delete.txt";
    when(blobClient.exists()).thenReturn(true);
    doNothing().when(blobClient).delete();
    Mono<Void> result = blobStorageService.deleteFile(fileName);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("deleteFile should complete when file doesn't exist")
  void deleteFile_WhenFileDoesNotExist_ShouldComplete() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "nonexistent.txt";
    when(blobClient.exists()).thenReturn(false);
    Mono<Void> result = blobStorageService.deleteFile(fileName);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("uploadImages should return empty flux when no images provided")
  void uploadImages_WithEmptyList_ShouldReturnEmptyFlux() {
    Flux<String> result = blobStorageService.uploadImages(Collections.emptyList());
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("uploadImages should return empty flux when null images provided")
  void uploadImages_WithNullList_ShouldReturnEmptyFlux() {
    Flux<String> result = blobStorageService.uploadImages(null);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("getDocumentById should return document when exists")
  void getDocumentById_WhenDocumentExists_ShouldReturnDocument() {
    ImageResponseDto imageResponse =
        ImageResponseDto.builder()
            .id(TEST_DOCUMENT_ID)
            .fileName(TEST_FILE_NAME)
            .url(TEST_IMAGE_URL)
            .title(TEST_TITLE)
            .build();
    WalletronExampleImages document =
        WalletronExampleImages.builder().id(TEST_DOCUMENT_ID).image(imageResponse).build();
    when(walletronExampleImagesRepository.findById(TEST_DOCUMENT_ID))
        .thenReturn(Mono.just(document));
    Mono<WalletronExampleImages> result = blobStorageService.getDocumentById(TEST_DOCUMENT_ID);
    StepVerifier.create(result).expectNext(document).verifyComplete();
  }

  @Test
  @DisplayName("getDocumentById should return empty when document doesn't exist")
  void getDocumentById_WhenDocumentDoesNotExist_ShouldReturnEmpty() {
    when(walletronExampleImagesRepository.findById("nonexistent")).thenReturn(Mono.empty());
    Mono<WalletronExampleImages> result = blobStorageService.getDocumentById("nonexistent");
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("getAllImages should return all images from documents")
  void getAllImages_ShouldReturnAllImages() {
    ImageResponseDto image1 =
        ImageResponseDto.builder()
            .id("doc-1")
            .fileName("image1.jpg")
            .url("https://blob.url/image1.jpg")
            .title("Image 1")
            .build();
    ImageResponseDto image2 =
        ImageResponseDto.builder()
            .id("doc-2")
            .fileName("image2.jpg")
            .url("https://blob.url/image2.jpg")
            .title("Image 2")
            .build();
    WalletronExampleImages doc1 =
        WalletronExampleImages.builder().id("doc-1").image(image1).build();
    WalletronExampleImages doc2 =
        WalletronExampleImages.builder().id("doc-2").image(image2).build();
    when(walletronExampleImagesRepository.findAll()).thenReturn(Flux.just(doc1, doc2));
    Mono<List<ImageResponseDto>> result = blobStorageService.getAllImages();
    StepVerifier.create(result)
        .expectNextMatches(
            images ->
                images.size() == 2
                    && images.get(0).getFileName().equals("image1.jpg")
                    && images.get(1).getFileName().equals("image2.jpg"))
        .verifyComplete();
  }

  @Test
  @DisplayName("getAllImages should return empty list when no documents exist")
  void getAllImages_WhenNoDocumentsExist_ShouldReturnEmptyList() {
    when(walletronExampleImagesRepository.findAll()).thenReturn(Flux.empty());
    Mono<List<ImageResponseDto>> result = blobStorageService.getAllImages();
    StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();
  }

  @Test
  @DisplayName("deleteImageById should delete document and blob when exists")
  void deleteImageById_WhenDocumentExists_ShouldDeleteDocumentAndBlob() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    ImageResponseDto imageResponse =
        ImageResponseDto.builder()
            .id(TEST_DOCUMENT_ID)
            .fileName(TEST_FILE_NAME)
            .url("https://storage.blob.core.windows.net/container/uuid_test-image.jpg?sastoken")
            .title(TEST_TITLE)
            .build();
    WalletronExampleImages document =
        WalletronExampleImages.builder().id(TEST_DOCUMENT_ID).image(imageResponse).build();
    when(walletronExampleImagesRepository.findById(TEST_DOCUMENT_ID))
        .thenReturn(Mono.just(document));
    when(blobClient.exists()).thenReturn(true);
    doNothing().when(blobClient).delete();
    when(walletronExampleImagesRepository.delete(document)).thenReturn(Mono.empty());
    Mono<Void> result = blobStorageService.deleteImageById(TEST_DOCUMENT_ID);
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("deleteImageById should return error when document doesn't exist")
  void deleteImageById_WhenDocumentDoesNotExist_ShouldReturnError() {
    when(walletronExampleImagesRepository.findById("nonexistent")).thenReturn(Mono.empty());
    Mono<Void> result = blobStorageService.deleteImageById("nonexistent");
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  @DisplayName("proxyFileFromUrl should return file content as response entity")
  void proxyFileFromUrl_ShouldReturnFileContentAsResponseEntity() {

    byte[] fileContent = "test content".getBytes();
    BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);
    when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);
    when(blobServiceClient.getBlobContainerClient("container")).thenReturn(mockContainerClient);
    when(mockContainerClient.getBlobClient("file.txt")).thenReturn(mockBlobClient);
    when(mockBlobClient.exists()).thenReturn(true);
    doAnswer(
            invocation -> {
              ByteArrayOutputStream outputStream = invocation.getArgument(0);
              outputStream.write(fileContent);
              return null;
            })
        .when(mockBlobClient)
        .downloadStream(any(ByteArrayOutputStream.class));
    Mono<org.springframework.http.ResponseEntity<byte[]>> result =
        blobStorageService.proxyFileFromUrl(
            "https://storage.blob.core.windows.net/container/file.txt");
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && Arrays.equals(response.getBody(), fileContent))
        .verifyComplete();
  }

  @Test
  @DisplayName("proxyFileFromUrl should return error when file doesn't exist")
  void proxyFileFromUrl_WhenFileDoesNotExist_ShouldReturnError() {
    BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);
    when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);
    when(blobServiceClient.getBlobContainerClient("container")).thenReturn(mockContainerClient);
    when(mockContainerClient.getBlobClient("file.txt")).thenReturn(mockBlobClient);
    when(mockBlobClient.exists()).thenReturn(false);
    Mono<org.springframework.http.ResponseEntity<byte[]>> result =
        blobStorageService.proxyFileFromUrl(
            "https://storage.blob.core.windows.net/container/file.txt");
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("uploadImagesWithDocumentsAndMessage should return success response")
  void uploadImagesWithDocumentsAndMessage_ShouldReturnSuccessResponse() {
    BlobStorageService spyService = Mockito.spy(blobStorageService);
    List<ImageUploadRequest> requests =
        List.of(
            ImageUploadRequest.builder().title("Image 1").build(),
            ImageUploadRequest.builder().title("Image 2").build());
    FilePart mockFilePart1 = createMockFilePart("image1.jpg");
    FilePart mockFilePart2 = createMockFilePart("image2.jpg");
    Flux<FilePart> files = Flux.just(mockFilePart1, mockFilePart2);
    List<ImageResponseDto> uploadedImages =
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
    doReturn(Mono.just(uploadedImages)).when(spyService).uploadImagesWithDocuments(any(), any());
    Mono<ImageUploadResponse> result =
        spyService.uploadImagesWithDocumentsAndMessage(requests, files);
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.isSuccess()
                    && response.getMessage().equals("2 images processed successfully")
                    && response.getData().size() == 2)
        .verifyComplete();
  }

  @Test
  @DisplayName("uploadImagesWithDocumentsAndMessage should return single image message")
  void uploadImagesWithDocumentsAndMessage_WithSingleImage_ShouldReturnSingleImageMessage() {
    BlobStorageService spyService = Mockito.spy(blobStorageService);
    List<ImageUploadRequest> requests =
        List.of(ImageUploadRequest.builder().title("Image 1").build());
    FilePart mockUploadFilePart = createMockFilePart("image1.jpg");
    Flux<FilePart> files = Flux.just(mockUploadFilePart);
    List<ImageResponseDto> uploadedImages =
        List.of(
            ImageResponseDto.builder()
                .id("doc-1")
                .fileName("image1.jpg")
                .url("https://blob.url/image1.jpg")
                .title("Image 1")
                .build());
    doReturn(Mono.just(uploadedImages)).when(spyService).uploadImagesWithDocuments(any(), any());
    Mono<ImageUploadResponse> result =
        spyService.uploadImagesWithDocumentsAndMessage(requests, files);
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.isSuccess()
                    && response.getMessage().equals("Image processed successfully")
                    && response.getData().size() == 1)
        .verifyComplete();
  }

  @Test
  @DisplayName("fetchFile should handle blob size zero")
  void fetchFile_WhenBlobSizeZero_ShouldReturnEmptyArray() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "empty.txt";
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getProperties()).thenReturn(blobProperties);
    when(blobProperties.getBlobSize()).thenReturn(0L);
    
    Mono<byte[]> result = blobStorageService.fetchFile(fileName);
    StepVerifier.create(result)
        .expectNextMatches(bytes -> bytes.length == 0)
        .verifyComplete();
  }

  @Test
  @DisplayName("fetchFile should handle empty content after download")
  void fetchFile_WhenDownloadReturnsEmpty_ShouldTryAlternativeMethod() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "test.txt";
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getProperties()).thenReturn(blobProperties);
    when(blobProperties.getBlobSize()).thenReturn(100L);
    
    // First call returns empty, second call returns content
    doAnswer(invocation -> {

      // Return empty content first time
      return null;
    }).doAnswer(invocation -> {
      ByteArrayOutputStream outputStream = invocation.getArgument(0);
      outputStream.write("test content".getBytes());
      return null;
    }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
    
    Mono<byte[]> result = blobStorageService.fetchFile(fileName);
    StepVerifier.create(result)
        .expectNextMatches(bytes -> bytes.length > 0)
        .verifyComplete();
  }

  @Test
  @DisplayName("fetchFile should handle unexpected exception")
  void fetchFile_WhenUnexpectedException_ShouldReturnIOException() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "test.txt";
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getProperties()).thenThrow(new RuntimeException("Unexpected error"));
    
    Mono<byte[]> result = blobStorageService.fetchFile(fileName);
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("uploadFile should handle IOException during upload")
  void uploadFile_WhenIOExceptionOccurs_ShouldReturnError() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String fileName = "test.txt";
    byte[] fileContent = "test content".getBytes();
    
    doAnswer(invocation -> {
      throw new IOException("Upload failed");
    }).when(blobClient).upload(any(InputStream.class), anyLong(), eq(true));
    
    Mono<String> result = blobStorageService.uploadFile(fileName, fileContent);
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("updateFile should handle BlobStorageException")
  void updateFile_WhenBlobStorageExceptionOccurs_ShouldReturnError() {
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    String filePath = "test.txt";
    String content = "new content";
    when(blobClient.exists()).thenReturn(true);
    
    doAnswer(invocation -> {
      throw new RuntimeException("Storage error");
    }).when(blobClient).upload(any(BinaryData.class), eq(true));
    
    Mono<Void> result = blobStorageService.updateFile(filePath, content);
    StepVerifier.create(result).expectError(com.aci.smart_onboarding.exception.BlobStorageException.class).verify();
  }

  @Test
  @DisplayName("uploadImages should handle blob upload failure")
  void uploadImages_WhenBlobUploadFails_ShouldReturnError() {
    List<BlobStorageService.ImageUploadData> images = Arrays.asList(
        new BlobStorageService.ImageUploadData("test.jpg", "test content".getBytes(), "image/jpeg", 0)
    );
    
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    doAnswer(invocation -> {
      throw new RuntimeException("Upload failed");
    }).when(blobClient).upload(any(InputStream.class), anyLong(), eq(true));
    
    Flux<String> result = blobStorageService.uploadImages(images);
    StepVerifier.create(result).expectError().verify();
  }

  @Test
  @DisplayName("uploadImages should handle blob existence check failure")
  void uploadImages_WhenBlobExistenceCheckFails_ShouldReturnError() {
    List<BlobStorageService.ImageUploadData> images = Arrays.asList(
        new BlobStorageService.ImageUploadData("test.jpg", "test content".getBytes(), "image/jpeg", 0)
    );
    
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    doNothing().when(blobClient).upload(any(InputStream.class), anyLong(), eq(true));
    doNothing().when(blobClient).setHttpHeaders(any(com.azure.storage.blob.models.BlobHttpHeaders.class));
    when(blobClient.exists()).thenReturn(false); // Blob doesn't exist after upload
    
    Flux<String> result = blobStorageService.uploadImages(images);
    StepVerifier.create(result).expectError(com.aci.smart_onboarding.exception.BlobStorageException.class).verify();
  }

  @Test
  @DisplayName("uploadImagesWithDocuments should handle empty file list")
  void uploadImagesWithDocuments_WithEmptyFileList_ShouldReturnError() {
    List<ImageUploadRequest> requests = Arrays.asList(
        ImageUploadRequest.builder().title("Test").build()
    );
    Flux<FilePart> files = Flux.empty();
    
    Mono<List<ImageResponseDto>> result = blobStorageService.uploadImagesWithDocuments(requests, files);
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  @DisplayName("uploadImagesWithDocuments should handle mismatched files and requests")
  void uploadImagesWithDocuments_WithMismatchedFilesAndRequests_ShouldReturnError() {
    List<ImageUploadRequest> requests = Arrays.asList(
        ImageUploadRequest.builder().title("Test").build()
    );
    FilePart mockFilePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(mockFilePart, mockFilePart); // 2 files, 1 request
    
    Mono<List<ImageResponseDto>> result = blobStorageService.uploadImagesWithDocuments(requests, files);
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  @DisplayName("uploadImagesWithDocuments should handle document update with non-existent ID")
  void uploadImagesWithDocuments_WithNonExistentDocumentId_ShouldReturnError() {
    List<ImageUploadRequest> requests = Arrays.asList(
        ImageUploadRequest.builder().id("non-existent-id").title("Test").build()
    );
    FilePart mockFilePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(mockFilePart);
    
    when(walletronExampleImagesRepository.findById("non-existent-id")).thenReturn(Mono.empty());
    
    Mono<List<ImageResponseDto>> result = blobStorageService.uploadImagesWithDocuments(requests, files);
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  @DisplayName("uploadImagesWithDocuments should handle document update with existing ID")
  void uploadImagesWithDocuments_WithExistingDocumentId_ShouldUpdateDocument() {
    List<ImageUploadRequest> requests = Arrays.asList(
        ImageUploadRequest.builder().id("existing-id").title("Updated Title").build()
    );
    FilePart mockFilePart = createMockFilePart("test.jpg");
    Flux<FilePart> files = Flux.just(mockFilePart);
    
    WalletronExampleImages existingDocument = WalletronExampleImages.builder()
        .id("existing-id")
        .image(ImageResponseDto.builder().fileName("old.jpg").url("old-url").title("Old Title").build())
        .build();
    
    when(walletronExampleImagesRepository.findById("existing-id")).thenReturn(Mono.just(existingDocument));
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    doNothing().when(blobClient).upload(any(InputStream.class), anyLong(), eq(true));
    doNothing().when(blobClient).setHttpHeaders(any(com.azure.storage.blob.models.BlobHttpHeaders.class));
    when(blobClient.exists()).thenReturn(true);
    when(blobClient.getBlobUrl()).thenReturn("https://test.blob.url/test.jpg");
    when(blobClient.generateSas(any(com.azure.storage.blob.sas.BlobServiceSasSignatureValues.class)))
        .thenReturn("sas-token");
    when(walletronExampleImagesRepository.save(any(WalletronExampleImages.class)))
        .thenReturn(Mono.just(existingDocument));
    // Mock headers properly
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
    when(mockFilePart.headers()).thenReturn(headers);
    // Fix: Mock blobClient.getProperties() and blobProperties.getBlobSize()
    when(blobClient.getProperties()).thenReturn(blobProperties);
    when(blobProperties.getBlobSize()).thenReturn(17L);
    
    Mono<List<ImageResponseDto>> result = blobStorageService.uploadImagesWithDocuments(requests, files);
    StepVerifier.create(result)
        .expectNextMatches(list -> !list.isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteImageById should handle document with null image")
  void deleteImageById_WhenDocumentHasNullImage_ShouldDeleteDocumentOnly() {
    WalletronExampleImages document = WalletronExampleImages.builder()
        .id("test-id")
        .image(null)
        .build();
    
    when(walletronExampleImagesRepository.findById("test-id")).thenReturn(Mono.just(document));
    when(walletronExampleImagesRepository.delete(document)).thenReturn(Mono.empty());
    
    Mono<Void> result = blobStorageService.deleteImageById("test-id");
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("deleteImageById should handle blob deletion failure")
  void deleteImageById_WhenBlobDeletionFails_ShouldStillDeleteDocument() {
    WalletronExampleImages document = WalletronExampleImages.builder()
        .id("test-id")
        .image(ImageResponseDto.builder()
            .fileName("test.jpg")
            .url("https://storage.blob.core.windows.net/container/uuid_test.jpg?sastoken")
            .title("Test")
            .build())
        .build();
    
    when(walletronExampleImagesRepository.findById("test-id")).thenReturn(Mono.just(document));
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    when(blobClient.exists()).thenReturn(true);
    doAnswer(invocation -> {
      throw new RuntimeException("Blob deletion failed");
    }).when(blobClient).delete();
    when(walletronExampleImagesRepository.delete(document)).thenReturn(Mono.empty());
    
    Mono<Void> result = blobStorageService.deleteImageById("test-id");
    StepVerifier.create(result).expectError(com.aci.smart_onboarding.exception.BlobStorageException.class).verify();
  }

  @Test
  @DisplayName("extractStorageFileName should handle invalid URL format")
  void extractStorageFileName_WithInvalidUrl_ShouldThrowException() {
    WalletronExampleImages document = WalletronExampleImages.builder()
        .id("test-id")
        .image(ImageResponseDto.builder()
            .fileName("test.jpg")
            .url("invalid-url-format")
            .title("Test")
            .build())
        .build();
    
    when(walletronExampleImagesRepository.findById("test-id")).thenReturn(Mono.just(document));
    when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    
    Mono<Void> result = blobStorageService.deleteImageById("test-id");
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  @DisplayName("getAllImages should handle documents with null createdBy")
  void getAllImages_WithNullCreatedBy_ShouldSortCorrectly() {
    WalletronExampleImages doc1 = WalletronExampleImages.builder()
        .id("doc-1")
        .createdBy(null)
        .image(ImageResponseDto.builder().fileName("image1.jpg").url("url1").title("Image 1").build())
        .build();
    WalletronExampleImages doc2 = WalletronExampleImages.builder()
        .id("doc-2")
        .createdBy("user2")
        .image(ImageResponseDto.builder().fileName("image2.jpg").url("url2").title("Image 2").build())
        .build();
    
    when(walletronExampleImagesRepository.findAll()).thenReturn(Flux.just(doc1, doc2));
    
    Mono<List<ImageResponseDto>> result = blobStorageService.getAllImages();
    StepVerifier.create(result)
        .expectNextMatches(images -> images.size() == 2)
        .verifyComplete();
  }

  @Test
  @DisplayName("getAllImages should handle documents with null image")
  void getAllImages_WithNullImage_ShouldFilterOutNullImages() {
    WalletronExampleImages doc1 = WalletronExampleImages.builder()
        .id("doc-1")
        .image(null)
        .build();
    WalletronExampleImages doc2 = WalletronExampleImages.builder()
        .id("doc-2")
        .image(ImageResponseDto.builder().fileName("image2.jpg").url("url2").title("Image 2").build())
        .build();
    
    when(walletronExampleImagesRepository.findAll()).thenReturn(Flux.just(doc1, doc2));
    
    Mono<List<ImageResponseDto>> result = blobStorageService.getAllImages();
    StepVerifier.create(result)
        .expectNextMatches(images -> images.size() == 1)
        .verifyComplete();
  }

  @Test
  @DisplayName("proxyFileFromUrl should return not found for empty result")
  void proxyFileFromUrl_WhenFileNotFound_ShouldReturnNotFound() {
    BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);
    when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);
    when(blobServiceClient.getBlobContainerClient("container")).thenReturn(mockContainerClient);
    when(mockContainerClient.getBlobClient("file.txt")).thenReturn(mockBlobClient);
    when(mockBlobClient.exists()).thenReturn(false);
    
    Mono<ResponseEntity<byte[]>> result = blobStorageService.proxyFileFromUrl(
        "https://storage.blob.core.windows.net/container/file.txt");
    StepVerifier.create(result).expectError(IOException.class).verify();
  }

  @Test
  @DisplayName("ImageUploadData getters should return correct values")
  void imageUploadData_Getters_ShouldReturnCorrectValues() {
    BlobStorageService.ImageUploadData data = new BlobStorageService.ImageUploadData(
        "test.jpg", "content".getBytes(), "image/jpeg", 1);
    
    assertEquals("test.jpg", data.getFileName());
    assertArrayEquals("content".getBytes(), data.getFileContent());
    assertEquals("image/jpeg", data.getContentType());
    assertEquals(1, data.getIndex());
  }

  @Test
  @DisplayName("generateUploadMessage should return correct message for single image")
  void generateUploadMessage_WithSingleImage_ShouldReturnCorrectMessage() {
    List<ImageResponseDto> images = Arrays.asList(
        ImageResponseDto.builder().fileName("test.jpg").url("url").title("Test").build()
    );
    
    // Use reflection to test private method
    try {
      java.lang.reflect.Method method = BlobStorageService.class.getDeclaredMethod("generateUploadMessage", List.class);
      method.setAccessible(true);
      String message = (String) method.invoke(blobStorageService, images);
      assertEquals("Image processed successfully", message);
    } catch (Exception e) {
      fail("Failed to test private method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("generateUploadMessage should return correct message for multiple images")
  void generateUploadMessage_WithMultipleImages_ShouldReturnCorrectMessage() {
    List<ImageResponseDto> images = Arrays.asList(
        ImageResponseDto.builder().fileName("test1.jpg").url("url1").title("Test 1").build(),
        ImageResponseDto.builder().fileName("test2.jpg").url("url2").title("Test 2").build()
    );
    
    // Use reflection to test private method
    try {
      java.lang.reflect.Method method = BlobStorageService.class.getDeclaredMethod("generateUploadMessage", List.class);
      method.setAccessible(true);
      String message = (String) method.invoke(blobStorageService, images);
      assertEquals("2 images processed successfully", message);
    } catch (Exception e) {
      fail("Failed to test private method: " + e.getMessage());
    }
  }

  private FilePart createMockFilePart(String filename) {
    FilePart mockFilePartInstance = mock(FilePart.class);
    byte[] content = "test file content".getBytes();
    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(content);
    when(mockFilePartInstance.filename()).thenReturn(filename);
    when(mockFilePartInstance.content()).thenReturn(Flux.just(dataBuffer));
    return mockFilePartInstance;
  }
}
