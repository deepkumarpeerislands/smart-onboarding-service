package com.aci.smart_onboarding.service.implementation;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ImageFileUploadResponse;
import com.aci.smart_onboarding.dto.JsonFileUploadResponse;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.DecryptionException;
import com.aci.smart_onboarding.exception.JsonFileValidationException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.JsonTemplateRepository;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.aci.smart_onboarding.service.IJsonTemplateService;
import com.aci.smart_onboarding.util.EncryptionUtil;
import com.mongodb.client.result.UpdateResult;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceTest {

  @Mock private IBlobStorageService blobStorageService;
  @Mock private EncryptionUtil encryptionUtil;
  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;
  @Mock private IJsonTemplateService jsonTemplateService;
  @Mock private JsonTemplateRepository jsonTemplateRepository;
  @InjectMocks private FileService fileService;

  private static final String TEST_BRD_ID = "BRD-123";
  private static final String TEST_FILE_TYPE = "ACH";

  private static Stream<Arguments> provideUploadTestCases() {
    return Stream.of(
        // Case 1: ACH file upload
        Arguments.of(
            "ACH file upload",
            "test-ach.pdf",
            new byte[] {0x25, 0x50, 0x44, 0x46}, // %PDF signature
            "ACH",
            "BRD-123",
            "https://storage/ach-file"),
        // Case 2: WALLETRON file upload
        Arguments.of(
            "WALLETRON file upload",
            "test-walletron.pdf",
            new byte[] {0x25, 0x50, 0x44, 0x46}, // %PDF signature
            "WALLETRON",
            "BRD-456",
            "https://storage/walletron-file"),
        // Case 3: Large PDF file
        Arguments.of(
            "Large PDF file",
            "large-file.pdf",
            new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}, // %PDF-1.4
            "ACH",
            "BRD-789",
            "https://storage/large-file"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideUploadTestCases")
  @DisplayName("Should handle various valid PDF file uploads")
  void uploadEncryptedFile_WithValidPDF_ReturnsUrl(
      String testCase,
      String filename,
      byte[] fileContent,
      String fileType,
      String brdId,
      String expectedUrl) {

    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn(filename);

    byte[] encryptedContent = "Encrypted content".getBytes(StandardCharsets.UTF_8);
    when(encryptionUtil.encrypt(any())).thenReturn(encryptedContent);
    when(blobStorageService.uploadFile(any(), any())).thenReturn(Mono.just(expectedUrl));
    when(reactiveMongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(BRD.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

    // Act & Assert
    StepVerifier.create(fileService.uploadEncryptedFile(filePart, fileContent, fileType, brdId))
        .expectNext(expectedUrl)
        .verifyComplete();

    verify(encryptionUtil).encrypt(fileContent);
    verify(blobStorageService).uploadFile(any(), eq(encryptedContent));
    verify(reactiveMongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(BRD.class));
  }

  @Test
  @DisplayName("Should fail to upload non-PDF file")
  void uploadEncryptedFile_WithNonPDF_ReturnsError() {
    // Arrange
    byte[] nonPdfContent = "Not a PDF".getBytes(StandardCharsets.UTF_8);
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("test.pdf");

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, nonPdfContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error
                        .getMessage()
                        .contains("File with .pdf extension does not have a valid PDF signature"))
        .verify();

    verify(encryptionUtil, never()).encrypt(any());
    verify(blobStorageService, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("Should download encrypted file successfully")
  void downloadFile_WithEncryptedFile_ReturnsDecryptedContent() {
    // Arrange
    String fileName = "test.pdf";
    String encryptedFileName = "uuid-test.pdf.enc";
    byte[] encryptedContent = new byte[32]; // Minimum size for decryption
    byte[] decryptedContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName)).thenReturn(Mono.just(encryptedContent));
    when(encryptionUtil.decryptWithFallback(any(), any())).thenReturn(decryptedContent);

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                return resource.getInputStream().readAllBytes().length > 0;
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle decryption failure gracefully")
  void downloadFile_WithDecryptionFailure_ReturnsOriginalContent() {
    // Arrange
    String fileName = "test.pdf";
    String encryptedFileName = "uuid-test.pdf.enc";
    byte[] encryptedContent = new byte[32]; // Minimum size for decryption

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName)).thenReturn(Mono.just(encryptedContent));
    when(encryptionUtil.decryptWithFallback(any(), any()))
        .thenThrow(new DecryptionException("Failed to decrypt"));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(encryptedContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should list all files successfully")
  void listFiles_ShouldReturnAllFileNames() {
    // Arrange
    List<String> expectedFiles = Arrays.asList("file1.pdf", "file2.pdf.enc", "file3.pdf");
    when(blobStorageService.listFiles()).thenReturn(Flux.fromIterable(expectedFiles));

    // Act & Assert
    StepVerifier.create(fileService.listFiles()).expectNext(expectedFiles).verifyComplete();
  }

  @Test
  @DisplayName("Should handle JPEG file upload attempt")
  void uploadEncryptedFile_WithJPEGFile_ReturnsError() {
    // Arrange - Create JPEG signature
    byte[] jpegContent = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("image.pdf"); // Trying to upload JPEG as PDF

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, jpegContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error
                        .getMessage()
                        .contains("File with .pdf extension does not have a valid PDF signature"))
        .verify();

    verify(encryptionUtil, never()).encrypt(any());
    verify(blobStorageService, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("Should handle text file masquerading as PDF")
  void uploadEncryptedFile_WithTextContentAsPDF_ReturnsError() {
    // Arrange - Create text content
    byte[] textContent = "This is a text file with PDF extension".getBytes(StandardCharsets.UTF_8);
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("document.pdf");

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, textContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error
                        .getMessage()
                        .contains("File with .pdf extension does not have a valid PDF signature"))
        .verify();

    verify(encryptionUtil, never()).encrypt(any());
    verify(blobStorageService, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("Should handle corrupted PDF content")
  void downloadFile_WithCorruptedPDFContent_ReturnsOriginalContent() {
    // Arrange
    String fileName = "corrupted.pdf";
    String encryptedFileName = "uuid-corrupted.pdf.enc";
    // Create content that looks like PDF but is corrupted
    byte[] encryptedContent = new byte[50]; // Large enough for decryption attempt
    encryptedContent[0] = 0x25; // %
    encryptedContent[1] = 0x50; // P
    encryptedContent[2] = 0x44; // D
    encryptedContent[3] = 0x46; // F
    encryptedContent[4] = 0x00; // Corruption

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName)).thenReturn(Mono.just(encryptedContent));
    when(encryptionUtil.decryptWithFallback(any(), any())).thenReturn(encryptedContent);

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                // Should return the content even if corrupted
                return content.length == encryptedContent.length;
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty file upload attempt")
  void uploadEncryptedFile_WithEmptyContent_ReturnsError() {
    // Arrange
    byte[] emptyContent = new byte[0];
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("empty.pdf");

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, emptyContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error.getMessage().contains("File content is empty"))
        .verify();

    verify(encryptionUtil, never()).encrypt(any());
    verify(blobStorageService, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("Should handle small file that looks like PDF")
  void uploadEncryptedFile_WithTooSmallPDFContent_ReturnsError() {
    // Arrange - Create minimal PDF signature that's too small
    byte[] tinyContent = new byte[] {0x25, 0x50, 0x44}; // %PD only
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("tiny.pdf");

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, tinyContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error.getMessage().contains("File is too small"))
        .verify();

    verify(encryptionUtil, never()).encrypt(any());
    verify(blobStorageService, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("Should find file by pattern match")
  void downloadFile_WithPatternMatch_ReturnsContent() {
    // Arrange
    String fileName = "document.pdf";
    String storedFileName = "some-document.pdf"; // Removed year to match pattern better
    byte[] fileContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

    when(blobStorageService.listFiles())
        .thenReturn(Flux.just("other.txt", storedFileName, "another.pdf"));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(fileContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(fileContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should find file by unencrypted prefix match")
  void downloadFile_WithUnencryptedPrefixMatch_ReturnsContent() {
    // Arrange
    String fileName = "document.pdf";
    String storedFileName = "uuid-document.pdf";
    byte[] fileContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

    when(blobStorageService.listFiles()).thenReturn(Flux.just("other.txt", storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(fileContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(fileContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should parse alternative keys with empty input")
  void uploadEncryptedFile_WithEmptyAlternativeKeys_EncryptsWithDefaultKey() {
    // Arrange
    byte[] validPdfContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("test.pdf");

    byte[] encryptedContent = "Encrypted".getBytes(StandardCharsets.UTF_8);
    when(encryptionUtil.encrypt(any())).thenReturn(encryptedContent);

    String expectedUrl = "https://storage/test-file";
    when(blobStorageService.uploadFile(any(), any())).thenReturn(Mono.just(expectedUrl));
    when(reactiveMongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(BRD.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

    // Act & Assert
    StepVerifier.create(
            fileService.uploadEncryptedFile(filePart, validPdfContent, TEST_FILE_TYPE, TEST_BRD_ID))
        .expectNext(expectedUrl)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle various file types during download")
  void downloadFile_WithVariousFileTypes_HandlesCorrectly() {
    // Test PNG file
    byte[] pngContent = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
    testFileDownload("image.png", pngContent);

    // Test JPEG file
    byte[] jpegContent = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    testFileDownload("image.jpg", jpegContent);

    // Test ZIP file
    byte[] zipContent = new byte[] {0x50, 0x4B, 0x03, 0x04};
    testFileDownload("archive.zip", zipContent);
  }

  private void testFileDownload(String fileName, byte[] content) {
    String storedFileName = "uuid-" + fileName;
    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(content));

    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] downloadedContent = resource.getInputStream().readAllBytes();
                return Arrays.equals(content, downloadedContent);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle text content during download")
  void downloadFile_WithTextContent_HandlesCorrectly() {
    // Arrange
    String fileName = "test.txt";
    String storedFileName = "uuid-test.txt";
    byte[] textContent =
        "This is a text file\nWith multiple lines\nAnd some special chars: !@#$"
            .getBytes(StandardCharsets.UTF_8);

    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(textContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(textContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle encrypted file with zero byte content")
  void downloadFile_WithZeroByteEncryptedContent_ReturnsOriginalContent() {
    // Arrange
    String fileName = "empty.pdf.enc";
    String encryptedFileName = "uuid-empty.pdf.enc";
    byte[] zeroByteContent = new byte[] {0x00};

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName)).thenReturn(Mono.just(zeroByteContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(zeroByteContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle file with hex content needing logging")
  void downloadFile_WithContentRequiringHexLogging_HandlesCorrectly() {
    // Arrange
    String fileName = "binary.dat";
    String storedFileName = "uuid-binary.dat";
    byte[] binaryContent = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};

    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(binaryContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(binaryContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle file with binary content")
  void downloadFile_WithBinaryContent_HandlesCorrectly() {
    // Arrange
    String fileName = "data.bin";
    String storedFileName = "uuid-data.bin";
    byte[] binaryContent = new byte[100];
    for (int i = 0; i < binaryContent.length; i++) {
      binaryContent[i] = (byte) (i & 0xFF);
    }

    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(binaryContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(binaryContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle file with mixed content")
  void downloadFile_WithMixedContent_HandlesCorrectly() {
    // Arrange
    String fileName = "mixed.dat";
    String storedFileName = "uuid-mixed.dat";
    // Create content with both text and binary data
    byte[] mixedContent = new byte[100];
    System.arraycopy("Text part: ".getBytes(StandardCharsets.UTF_8), 0, mixedContent, 0, 10);
    for (int i = 10; i < mixedContent.length; i++) {
      mixedContent[i] = (byte) (i & 0xFF);
    }

    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(mixedContent));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(mixedContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle file with special characters in name")
  void downloadFile_WithSpecialCharsInName_HandlesCorrectly() {
    // Arrange
    String fileName = "special!@#$%^&*.pdf";
    String storedFileName = "uuid-special!@#$%^&*.pdf";
    byte[] content = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

    when(blobStorageService.listFiles()).thenReturn(Flux.just(storedFileName));
    when(blobStorageService.fetchFile(storedFileName)).thenReturn(Mono.just(content));

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] downloadedContent = resource.getInputStream().readAllBytes();
                return Arrays.equals(content, downloadedContent);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle encrypted non-PDF files with various content types")
  void downloadFile_WithEncryptedNonPDFFiles_HandlesVariousContentTypes() {
    // Test cases for different content types
    testEncryptedFileDownload("document.txt", "Text content".getBytes(StandardCharsets.UTF_8));
    testEncryptedFileDownload(
        "image.jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    testEncryptedFileDownload("archive.zip", new byte[] {0x50, 0x4B, 0x03, 0x04});
    testEncryptedFileDownload("binary.dat", generateBinaryContent(100));
  }

  private void testEncryptedFileDownload(String fileName, byte[] originalContent) {
    // Arrange
    String encryptedFileName = "uuid-" + fileName + ".enc";
    byte[] encryptedContent = new byte[Math.max(32, originalContent.length)];
    System.arraycopy(originalContent, 0, encryptedContent, 0, originalContent.length);

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName)).thenReturn(Mono.just(encryptedContent));
    when(encryptionUtil.decryptWithFallback(any(), any())).thenReturn(originalContent);

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(originalContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty content in encrypted non-PDF file")
  void downloadFile_WithEmptyContentInEncryptedNonPDF_HandlesCorrectly() {
    // Arrange
    String fileName = "empty.txt";
    String encryptedFileName = "uuid-empty.txt.enc";
    byte[] emptyEncryptedContent = new byte[32]; // Minimum size for encrypted content
    byte[] emptyDecryptedContent = new byte[0];

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName))
        .thenReturn(Mono.just(emptyEncryptedContent));
    when(encryptionUtil.decryptWithFallback(any(), any())).thenReturn(emptyDecryptedContent);

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(emptyDecryptedContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle large content in encrypted non-PDF file")
  void downloadFile_WithLargeContentInEncryptedNonPDF_HandlesCorrectly() {
    // Arrange
    String fileName = "large.txt";
    String encryptedFileName = "uuid-large.txt.enc";
    byte[] largeContent = generateLargeContent();
    byte[] encryptedLargeContent = new byte[largeContent.length + 32]; // Add padding for encryption
    System.arraycopy(largeContent, 0, encryptedLargeContent, 0, largeContent.length);

    when(blobStorageService.listFiles()).thenReturn(Flux.just(encryptedFileName));
    when(blobStorageService.fetchFile(encryptedFileName))
        .thenReturn(Mono.just(encryptedLargeContent));
    when(encryptionUtil.decryptWithFallback(any(), any())).thenReturn(largeContent);

    // Act & Assert
    StepVerifier.create(fileService.downloadFile(fileName))
        .expectNextMatches(
            resource -> {
              try {
                byte[] content = resource.getInputStream().readAllBytes();
                return Arrays.equals(largeContent, content);
              } catch (IOException e) {
                return false;
              }
            })
        .verifyComplete();
  }

  private static Stream<Arguments> provideFileTypeTestCases() {
    return Stream.of(
        Arguments.of(
            "ACH file upload", "test-ach.pdf", "ACH", "BRD-123", "https://storage/ach-file"),
        Arguments.of(
            "WALLETRON file upload",
            "test-walletron.pdf",
            "WALLETRON",
            "BRD-456",
            "https://storage/walletron-file"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideFileTypeTestCases")
  @DisplayName("Should update BRD with correct timestamp based on file type")
  void uploadEncryptedFile_UpdatesCorrectTimestamp(
      String testCase, String filename, String fileType, String brdId, String expectedUrl) {
    // Arrange
    byte[] validPdfContent = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E}; // %PDF-1.
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn(filename);

    byte[] encryptedContent = "Encrypted content".getBytes(StandardCharsets.UTF_8);
    when(encryptionUtil.encrypt(any())).thenReturn(encryptedContent);
    when(blobStorageService.uploadFile(any(), any())).thenReturn(Mono.just(expectedUrl));
    when(reactiveMongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(BRD.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

    // Act & Assert
    StepVerifier.create(fileService.uploadEncryptedFile(filePart, validPdfContent, fileType, brdId))
        .expectNext(expectedUrl)
        .verifyComplete();

    // Verify common behavior
    verify(encryptionUtil).encrypt(validPdfContent);
    verify(blobStorageService).uploadFile(any(), eq(encryptedContent));

    // Verify timestamp update based on file type
    if (fileType.equals("ACH")) {
      verify(reactiveMongoTemplate)
          .updateFirst(
              argThat(
                  query -> query.toString().contains("brdId") && query.toString().contains(brdId)),
              argThat(update -> update.toString().contains("achUploadedOn")),
              eq(BRD.class));
    } else {
      verify(reactiveMongoTemplate)
          .updateFirst(
              argThat(
                  query -> query.toString().contains("brdId") && query.toString().contains(brdId)),
              argThat(update -> update.toString().contains("walletronUploadedOn")),
              eq(BRD.class));
    }
  }

  private byte[] generateBinaryContent(int size) {
    byte[] content = new byte[size];
    for (int i = 0; i < size; i++) {
      content[i] = (byte) (i & 0xFF);
    }
    return content;
  }

  private byte[] generateLargeContent() {
    // Generate 1MB of test data
    int size = 1024 * 1024;
    byte[] content = new byte[size];
    for (int i = 0; i < size; i++) {
      // Create a repeating pattern
      content[i] = (byte) ((i % 256) & 0xFF);
    }
    return content;
  }

  // New Test Cases for uploadImageFile method

  @Test
  @DisplayName("Should successfully upload LOGO image with correct dimensions")
  void uploadImageFile_WithValidLogoImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("logo.png");

    // Create valid PNG content with correct signature
    byte[] logoContent = createValidPngContent(150, 150);

    when(blobStorageService.fetchFile("WALLET123-logo"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET123-logo", logoContent))
        .thenReturn(Mono.just("https://storage/WALLET123-logo"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, logoContent, "LOGO", "WALLET123");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Image file uploaded successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("WALLET123-logo", data.getFileName());
              assertEquals("WALLET123", data.getWalletronId());
              assertEquals("LOGO", data.getImageType());
              assertEquals("logo.png", data.getOriginalFileName());
              assertEquals(logoContent.length, data.getFileSize());
              assertEquals("https://storage/WALLET123-logo", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully upload ICON image with correct dimensions")
  void uploadImageFile_WithValidIconImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("icon.png");

    byte[] iconContent = createValidPngContent(380, 126);

    when(blobStorageService.fetchFile("WALLET456-icon"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET456-icon", iconContent))
        .thenReturn(Mono.just("https://storage/WALLET456-icon"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, iconContent, "ICON", "WALLET456");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("WALLET456-icon", data.getFileName());
              assertEquals("ICON", data.getImageType());
              assertEquals("WALLET456", data.getWalletronId());
              assertEquals("icon.png", data.getOriginalFileName());
              assertEquals(iconContent.length, data.getFileSize());
              assertEquals("https://storage/WALLET456-icon", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully upload STRIP image with correct dimensions")
  void uploadImageFile_WithValidStripImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("strip.png");

    byte[] stripContent = createValidPngContent(936, 330);

    when(blobStorageService.fetchFile("WALLET789-strip"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET789-strip", stripContent))
        .thenReturn(Mono.just("https://storage/WALLET789-strip"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, stripContent, "STRIP", "WALLET789");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("STRIP", data.getImageType());
              assertEquals("WALLET789", data.getWalletronId());
              assertEquals("strip.png", data.getOriginalFileName());
              assertEquals(stripContent.length, data.getFileSize());
              assertEquals("https://storage/WALLET789-strip", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully upload THUMBNAIL image with correct dimensions")
  void uploadImageFile_WithValidThumbnailImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("thumbnail.png");

    byte[] thumbnailContent = createValidPngContent(250, 250);

    when(blobStorageService.fetchFile("WALLET101-thumbnail"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET101-thumbnail", thumbnailContent))
        .thenReturn(Mono.just("https://storage/WALLET101-thumbnail"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, thumbnailContent, "THUMBNAIL", "WALLET101");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("WALLET101-thumbnail", data.getFileName());
              assertEquals("THUMBNAIL", data.getImageType());
              assertEquals("WALLET101", data.getWalletronId());
              assertEquals("thumbnail.png", data.getOriginalFileName());
              assertEquals(thumbnailContent.length, data.getFileSize());
              assertEquals("https://storage/WALLET101-thumbnail", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully upload PASS_LOGO image with correct dimensions")
  void uploadImageFile_WithValidPassLogoImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("pass-logo.png");

    byte[] passLogoContent = createValidPngContent(380, 120);

    when(blobStorageService.fetchFile("WALLET202-pass_logo"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET202-pass_logo", passLogoContent))
        .thenReturn(Mono.just("https://storage/WALLET202-pass_logo"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, passLogoContent, "PASS_LOGO", "WALLET202");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("WALLET202-pass_logo", data.getFileName());
              assertEquals("PASS_LOGO", data.getImageType());
              assertEquals("WALLET202", data.getWalletronId());
              assertEquals("pass-logo.png", data.getOriginalFileName());
              assertEquals(passLogoContent.length, data.getFileSize());
              assertEquals("https://storage/WALLET202-pass_logo", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should successfully upload ACI_WALLETRON_NOTIFICATION_LOGO image with correct dimensions")
  void uploadImageFile_WithValidAciWalletronNotificationLogoImage_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("notification-logo.png");

    byte[] notificationLogoContent = createValidPngContent(150, 150);

    when(blobStorageService.fetchFile("WALLET303-aci_walletron_notification_logo"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile(
            "WALLET303-aci_walletron_notification_logo", notificationLogoContent))
        .thenReturn(Mono.just("https://storage/WALLET303-aci_walletron_notification_logo"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(
            filePart, notificationLogoContent, "ACI_WALLETRON_NOTIFICATION_LOGO", "WALLET303");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              ImageFileUploadResponse data = response.getBody().getData().get();
              assertEquals("WALLET303-aci_walletron_notification_logo", data.getFileName());
              assertEquals("ACI_WALLETRON_NOTIFICATION_LOGO", data.getImageType());
              assertEquals("WALLET303", data.getWalletronId());
              assertEquals("notification-logo.png", data.getOriginalFileName());
              assertEquals(notificationLogoContent.length, data.getFileSize());
              assertEquals(
                  "https://storage/WALLET303-aci_walletron_notification_logo", data.getFileUrl());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should fail validation for PASS_LOGO with invalid dimensions")
  void uploadImageFile_WithInvalidPassLogoDimensions_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("wrong-pass-logo.png");

    // Create PNG with wrong dimensions for PASS_LOGO (should be 380x120)
    byte[] wrongSizeContent = createValidPngContent(400, 150);

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, wrongSizeContent, "PASS_LOGO", "WALLET202"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error
                        .getMessage()
                        .contains(
                            "Invalid dimensions for PASS_LOGO image. Required: 380x120, Found: 400x150"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for ACI_WALLETRON_NOTIFICATION_LOGO with invalid dimensions")
  void uploadImageFile_WithInvalidAciWalletronNotificationLogoDimensions_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("wrong-notification-logo.png");

    // Create PNG with wrong dimensions for ACI_WALLETRON_NOTIFICATION_LOGO (should be 150x150)
    byte[] wrongSizeContent = createValidPngContent(200, 200);

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(
                filePart, wrongSizeContent, "ACI_WALLETRON_NOTIFICATION_LOGO", "WALLET303"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error
                        .getMessage()
                        .contains(
                            "Invalid dimensions for ACI_WALLETRON_NOTIFICATION_LOGO image. Required: 150x150, Found: 200x200"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for empty image content")
  void uploadImageFile_WithEmptyContent_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("empty.png");

    byte[] emptyContent = new byte[0];

    // Act & Assert
    StepVerifier.create(fileService.uploadImageFile(filePart, emptyContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Image file content is empty"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for invalid image type")
  void uploadImageFile_WithInvalidImageType_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("test.png");

    byte[] pngContent = createValidPngContent(150, 150);

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, pngContent, "INVALID_TYPE", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Invalid image type"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for non-PNG file extension")
  void uploadImageFile_WithNonPngExtension_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("test.jpg");

    byte[] jpegContent = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    // Act & Assert
    StepVerifier.create(fileService.uploadImageFile(filePart, jpegContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Only PNG files are allowed"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for invalid PNG signature")
  void uploadImageFile_WithInvalidPngSignature_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("fake.png");

    byte[] invalidPngContent = new byte[100];
    invalidPngContent[0] = 0x25; // Not PNG signature

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, invalidPngContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error
                        .getMessage()
                        .contains("File with .png extension does not have a valid PNG signature"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for oversized file")
  void uploadImageFile_WithOversizedFile_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("large.png");

    byte[] oversizedContent = new byte[2 * 1024 * 1024]; // 2MB
    // Add PNG signature
    oversizedContent[0] = (byte) 0x89;
    oversizedContent[1] = 0x50;
    oversizedContent[2] = 0x4E;
    oversizedContent[3] = 0x47;
    oversizedContent[4] = 0x0D;
    oversizedContent[5] = 0x0A;
    oversizedContent[6] = 0x1A;
    oversizedContent[7] = 0x0A;

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, oversizedContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Image file size exceeds maximum allowed size"))
        .verify();
  }

  @Test
  @DisplayName("Should fail validation for invalid dimensions")
  void uploadImageFile_WithInvalidDimensions_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("wrong-size.png");

    // Create PNG with wrong dimensions for LOGO (should be 150x150)
    byte[] wrongSizeContent = createValidPngContent(100, 100);

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, wrongSizeContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Invalid dimensions"))
        .verify();
  }

  @Test
  @DisplayName("Should fail when image already exists")
  void uploadImageFile_WithExistingImage_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("logo.png");

    byte[] logoContent = createValidPngContent(150, 150);

    // Mock that file already exists
    when(blobStorageService.fetchFile("WALLET123-logo")).thenReturn(Mono.just(new byte[100]));

    // Act & Assert
    StepVerifier.create(fileService.uploadImageFile(filePart, logoContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof AlreadyExistException
                    && error
                        .getMessage()
                        .contains(
                            "Image with walletronId 'WALLET123' and type 'LOGO' already exists"))
        .verify();
  }

  @Test
  @DisplayName("Should handle blob storage upload failure")
  void uploadImageFile_WithStorageFailure_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("logo.png");

    byte[] logoContent = createValidPngContent(150, 150);

    when(blobStorageService.fetchFile("WALLET123-logo"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET123-logo", logoContent))
        .thenReturn(Mono.error(new RuntimeException("Storage failure")));

    // Act & Assert
    StepVerifier.create(fileService.uploadImageFile(filePart, logoContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof Exception && error.getMessage().contains("Something went wrong"))
        .verify();
  }

  @Test
  @DisplayName("Should handle corrupted image content during dimension validation")
  void uploadImageFile_WithCorruptedImageContent_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("corrupted.png");

    // Create content with PNG signature but corrupted image data
    byte[] corruptedContent = new byte[1000];
    corruptedContent[0] = (byte) 0x89;
    corruptedContent[1] = 0x50;
    corruptedContent[2] = 0x4E;
    corruptedContent[3] = 0x47;
    corruptedContent[4] = 0x0D;
    corruptedContent[5] = 0x0A;
    corruptedContent[6] = 0x1A;
    corruptedContent[7] = 0x0A;
    // Rest is corrupted/invalid image data

    // Act & Assert
    StepVerifier.create(
            fileService.uploadImageFile(filePart, corruptedContent, "LOGO", "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && (error.getMessage().contains("Failed to read image dimensions")
                        || error.getMessage().contains("I/O error reading PNG header")))
        .verify();
  }

  // New Test Cases for deleteImageFileByName method

  @Test
  @DisplayName("Should successfully delete existing image file")
  void deleteImageFileByName_WithExistingFile_ShouldReturnSuccess() {
    // Arrange
    String fileName = "WALLET123-logo";

    when(blobStorageService.listFiles()).thenReturn(Flux.just(fileName, "other-file.txt"));
    when(blobStorageService.deleteFile(fileName)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Image file deleted successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().contains("Image deleted: " + fileName));
              return true;
            })
        .verifyComplete();

    verify(blobStorageService).deleteFile(fileName);
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent file")
  void deleteImageFileByName_WithNonExistentFile_ShouldReturn404() {
    // Arrange
    String fileName = "WALLET999-logo";

    when(blobStorageService.listFiles())
        .thenReturn(Flux.just("other-file.txt", "another-file.png"));

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals("Image file not found: " + fileName, response.getBody().getMessage());
              assertTrue(response.getBody().getData().isEmpty());
              assertTrue(response.getBody().getErrors().isPresent());
              return true;
            })
        .verifyComplete();

    verify(blobStorageService, never()).deleteFile(fileName);
  }

  @Test
  @DisplayName("Should handle blob storage error during file listing")
  void deleteImageFileByName_WithListFilesError_ShouldReturnError() {
    // Arrange
    String fileName = "WALLET123-logo";

    when(blobStorageService.listFiles())
        .thenReturn(Flux.error(new RuntimeException("Storage error")));

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof Exception && error.getMessage().contains("Something went wrong"))
        .verify();
  }

  @Test
  @DisplayName("Should handle blob storage error during file deletion")
  void deleteImageFileByName_WithDeleteError_ShouldReturnError() {
    // Arrange
    String fileName = "WALLET123-logo";

    when(blobStorageService.listFiles()).thenReturn(Flux.just(fileName));
    when(blobStorageService.deleteFile(fileName))
        .thenReturn(Mono.error(new RuntimeException("Delete failed")));

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof Exception && error.getMessage().contains("Something went wrong"))
        .verify();
  }

  @Test
  @DisplayName("Should handle empty file list during deletion")
  void deleteImageFileByName_WithEmptyFileList_ShouldReturn404() {
    // Arrange
    String fileName = "WALLET123-logo";

    when(blobStorageService.listFiles()).thenReturn(Flux.empty());

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle file name with special characters")
  void deleteImageFileByName_WithSpecialCharacters_ShouldWork() {
    // Arrange
    String fileName = "WALLET-123_test-logo";

    when(blobStorageService.listFiles()).thenReturn(Flux.just(fileName));
    when(blobStorageService.deleteFile(fileName)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle null imageType in image requirements")
  void uploadImageFile_WithNullImageType_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("test.png");

    byte[] pngContent = createValidPngContent(150, 150);

    // Act & Assert
    StepVerifier.create(fileService.uploadImageFile(filePart, pngContent, null, "WALLET123"))
        .expectErrorMatches(
            error ->
                error instanceof BadRequestException
                    && error.getMessage().contains("Invalid image type"))
        .verify();
  }

  @Test
  @DisplayName("Should sanitize imageType in filename")
  void uploadImageFile_WithImageTypeThatNeedsSanitization_ShouldSanitize() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("logo.png");

    byte[] logoContent = createValidPngContent(150, 150);

    // Test with imageType that might need sanitization (though current types are clean)
    when(blobStorageService.fetchFile("WALLET123-logo"))
        .thenReturn(Mono.error(new RuntimeException("File not found")));
    when(blobStorageService.uploadFile("WALLET123-logo", logoContent))
        .thenReturn(Mono.just("https://storage/WALLET123-logo"));

    // Act
    Mono<ResponseEntity<Api<ImageFileUploadResponse>>> result =
        fileService.uploadImageFile(filePart, logoContent, "LOGO", "WALLET123");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("WALLET123-logo", response.getBody().getData().get().getFileName());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle case where exact file match exists in list")
  void deleteImageFileByName_WithExactMatch_ShouldDelete() {
    // Arrange
    String fileName = "WALLET123-logo";
    List<String> fileList = Arrays.asList("other-file.txt", fileName, "another-file.png");

    when(blobStorageService.listFiles()).thenReturn(Flux.fromIterable(fileList));
    when(blobStorageService.deleteFile(fileName)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<String>>> result = fileService.deleteImageFileByName(fileName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();
  }

  // Helper method to create valid PNG content with specified dimensions
  private byte[] createValidPngContent(int width, int height) {
    try {
      // Create a BufferedImage with the specified dimensions
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      // Fill with a simple pattern to make it a valid image
      Graphics2D g2d = image.createGraphics();
      g2d.setColor(Color.BLUE);
      g2d.fillRect(0, 0, width, height);
      g2d.setColor(Color.WHITE);
      g2d.fillRect(10, 10, width - 20, height - 20);
      g2d.dispose();

      // Convert to PNG byte array
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", baos);
      return baos.toByteArray();
    } catch (IOException e) {
      // Fallback to minimal PNG structure if ImageIO fails
      return createMinimalPngContent();
    }
  }

  private byte[] createMinimalPngContent() {
    // Create a minimal valid PNG with proper signature and basic structure
    // This is much smaller than a full image but still valid for basic validation
    byte[] pngContent = new byte[200]; // Fixed small size

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
    for (int i = 8; i < pngContent.length; i++) {
      pngContent[i] = (byte) (i % 256);
    }

    return pngContent;
  }

  // New Test Cases for uploadJsonFile method

  @Test
  @DisplayName("Should successfully upload JSON file and create template")
  void uploadJsonFile_WithValidJson_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("template.json");
    
    String jsonContent = "{\"name\": \"test\", \"value\": 123}";
    byte[] fileContent = jsonContent.getBytes(StandardCharsets.UTF_8);
    String templateName = "test-template";
    
    when(blobStorageService.uploadFile(any(), any())).thenReturn(Mono.just("https://storage/test-file"));
    
    // Mock security context
    org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    
    // Mock the security context holder
    try (var mockStatic = mockStatic(org.springframework.security.core.context.ReactiveSecurityContextHolder.class)) {
      mockStatic.when(org.springframework.security.core.context.ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));
      
      // Mock JSON template service response
      com.aci.smart_onboarding.dto.JsonTemplateResponse templateResponse = 
          com.aci.smart_onboarding.dto.JsonTemplateResponse.builder()
              .templateName(templateName)
              .fileName("test-file")
              .build();
      
      org.springframework.http.ResponseEntity<com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.JsonTemplateResponse>> templateApiResponse = 
          org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
              .body(new com.aci.smart_onboarding.dto.Api<>("SUCCESS", "Template created", 
                  java.util.Optional.of(templateResponse), java.util.Optional.empty()));
      
      when(jsonTemplateService.createTemplate(eq(templateName), any(), eq("template.json"), eq("testuser")))
          .thenReturn(Mono.just(templateApiResponse));
      
      // Act
      Mono<ResponseEntity<Api<JsonFileUploadResponse>>> result = 
          fileService.uploadJsonFile(filePart, fileContent, templateName);
      
      // Assert
      StepVerifier.create(result)
          .expectNextMatches(response -> {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("JSON file uploaded and template created successfully", response.getBody().getMessage());
            assertTrue(response.getBody().getData().isPresent());
            JsonFileUploadResponse data = response.getBody().getData().get();
            assertEquals(templateName, data.getTemplateName());
            assertEquals("template.json", data.getOriginalFileName());
            assertEquals(fileContent.length, data.getFileSize());
            assertNotNull(data.getTemplateDetails());
            return true;
          })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should fail when JSON file content is empty")
  void uploadJsonFile_WithEmptyContent_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("empty.json");
    byte[] emptyContent = new byte[0];
    
    // Act & Assert
    StepVerifier.create(fileService.uploadJsonFile(filePart, emptyContent, "test-template"))
        .expectErrorMatches(error -> 
            error instanceof JsonFileValidationException && 
            error.getMessage().contains("JSON file content is empty"))
        .verify();
  }

  @Test
  @DisplayName("Should fail when JSON file has invalid extension")
  void uploadJsonFile_WithInvalidExtension_ShouldFail() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("template.txt");
    byte[] content = "{\"test\": \"value\"}".getBytes(StandardCharsets.UTF_8);
    
    // Act & Assert
    StepVerifier.create(fileService.uploadJsonFile(filePart, content, "test-template"))
        .expectErrorMatches(error -> 
            error instanceof JsonFileValidationException && 
            error.getMessage().contains("Only JSON files are allowed"))
        .verify();
  }

  @Test
  @DisplayName("Should fail when JSON content is invalid")
  void uploadJsonFile_WithInvalidJsonContent_ShouldFail() {
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("invalid.json");
    byte[] content = "not a json".getBytes(StandardCharsets.UTF_8);
    String templateName = "test-template";

    // Mock uploadFile to avoid NPE
    when(blobStorageService.uploadFile(any(), any())).thenReturn(Mono.just("dummy"));

    StepVerifier.create(fileService.uploadJsonFile(filePart, content, templateName))
        .expectError(JsonFileValidationException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle template creation failure and cleanup file")
  void uploadJsonFile_WithTemplateCreationFailure_ShouldCleanupFile() {
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("template.json");
    byte[] content = "{\"test\": \"value\"}".getBytes(StandardCharsets.UTF_8);
    String templateName = "test-template";

    // Mock security context
    org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    try (var mockStatic = mockStatic(org.springframework.security.core.context.ReactiveSecurityContextHolder.class)) {
      mockStatic.when(org.springframework.security.core.context.ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock successful file upload
      when(blobStorageService.uploadFile(any(), any()))
          .thenReturn(Mono.just("https://storage/test-file"));

      // Mock template creation failure
      when(jsonTemplateService.createTemplate(eq(templateName), any(), eq("template.json"), eq("testuser")))
          .thenReturn(Mono.error(new RuntimeException("Template creation failed")));

      // Mock file cleanup
      when(blobStorageService.deleteFile(any()))
          .thenReturn(Mono.empty());

      StepVerifier.create(fileService.uploadJsonFile(filePart, content, templateName))
          .expectErrorMatches(error ->
              error instanceof Exception &&
              error.getMessage() != null &&
              error.getMessage().contains("Something went wrong: Template creation failed"))
          .verify();
    }
  }

  // Test cases for getBase64EncodedImages method

  @Test
  @DisplayName("Should return empty map when blobUrls list is null")
  void getBase64EncodedImages_WithNullUrls_ShouldReturnEmptyMap() {
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(null))
        .expectNext(Map.of())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return empty map when blobUrls list is empty")
  void getBase64EncodedImages_WithEmptyUrls_ShouldReturnEmptyMap() {
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(List.of()))
        .expectNext(Map.of())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully encode images from blob URLs")
  void getBase64EncodedImages_WithValidUrls_ShouldReturnEncodedImages() {
    // Arrange
    List<String> blobUrls = Arrays.asList("https://storage/image1.png", "https://storage/image2.png");
    byte[] imageContent1 = "image1-content".getBytes(StandardCharsets.UTF_8);
    byte[] imageContent2 = "image2-content".getBytes(StandardCharsets.UTF_8);
    
    when(blobStorageService.fetchFile("image1.png"))
        .thenReturn(Mono.just(imageContent1));
    when(blobStorageService.fetchFile("image2.png"))
        .thenReturn(Mono.just(imageContent2));
    
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(blobUrls))
        .expectNextMatches(result -> {
          assertEquals(2, result.size());
          assertTrue(result.containsKey("https://storage/image1.png"));
          assertTrue(result.containsKey("https://storage/image2.png"));
          
          // Verify base64 encoding
          byte[] encoded1 = result.get("https://storage/image1.png");
          byte[] encoded2 = result.get("https://storage/image2.png");
          
          assertNotNull(encoded1);
          assertNotNull(encoded2);
          
          // Decode and verify content
          String decoded1 = new String(Base64.getDecoder().decode(encoded1));
          String decoded2 = new String(Base64.getDecoder().decode(encoded2));
          
          assertEquals("image1-content", decoded1);
          assertEquals("image2-content", decoded2);
          
          return true;
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle missing files gracefully")
  void getBase64EncodedImages_WithMissingFiles_ShouldHandleGracefully() {
    // Arrange
    List<String> blobUrls = Arrays.asList("https://storage/missing.png", "https://storage/existing.png");
    byte[] existingContent = "existing-content".getBytes(StandardCharsets.UTF_8);
    
    when(blobStorageService.fetchFile("missing.png"))
        .thenReturn(Mono.empty());
    when(blobStorageService.fetchFile("existing.png"))
        .thenReturn(Mono.just(existingContent));
    
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(blobUrls))
        .expectNextMatches(result -> {
          assertEquals(1, result.size());
          assertTrue(result.containsKey("https://storage/existing.png"));
          assertFalse(result.containsKey("https://storage/missing.png"));
          
          byte[] encoded = result.get("https://storage/existing.png");
          String decoded = new String(Base64.getDecoder().decode(encoded));
          assertEquals("existing-content", decoded);
          
          return true;
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty file content")
  void getBase64EncodedImages_WithEmptyFileContent_ShouldSkipFile() {
    // Arrange
    List<String> blobUrls = Arrays.asList("https://storage/empty.png");
    byte[] emptyContent = new byte[0];
    
    when(blobStorageService.fetchFile("empty.png"))
        .thenReturn(Mono.just(emptyContent));
    
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(blobUrls))
        .expectNext(Map.of())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle URL extraction failure")
  void getBase64EncodedImages_WithInvalidUrl_ShouldHandleGracefully() {
    // Arrange
    List<String> blobUrls = Arrays.asList("invalid-url", "https://storage/valid.png");
    byte[] validContent = "valid-content".getBytes(StandardCharsets.UTF_8);
    
    when(blobStorageService.fetchFile("valid.png"))
        .thenReturn(Mono.just(validContent));
    
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(blobUrls))
        .expectNextMatches(result -> {
          assertEquals(1, result.size());
          assertTrue(result.containsKey("https://storage/valid.png"));
          assertFalse(result.containsKey("invalid-url"));
          
          return true;
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle blob storage exceptions gracefully")
  void getBase64EncodedImages_WithBlobStorageException_ShouldHandleGracefully() {
    // Arrange
    List<String> blobUrls = Arrays.asList("https://storage/error.png", "https://storage/valid.png");
    byte[] validContent = "valid-content".getBytes(StandardCharsets.UTF_8);
    
    when(blobStorageService.fetchFile("error.png"))
        .thenReturn(Mono.error(new RuntimeException("Storage error")));
    when(blobStorageService.fetchFile("valid.png"))
        .thenReturn(Mono.just(validContent));
    
    // Act & Assert
    StepVerifier.create(fileService.getBase64EncodedImages(blobUrls))
        .expectNextMatches(result -> {
          assertEquals(1, result.size());
          assertTrue(result.containsKey("https://storage/valid.png"));
          assertFalse(result.containsKey("https://storage/error.png"));
          
          return true;
        })
        .verifyComplete();
  }
}
