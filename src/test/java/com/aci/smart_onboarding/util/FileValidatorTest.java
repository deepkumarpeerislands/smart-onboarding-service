package com.aci.smart_onboarding.util;

import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.exception.InvalidFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

  @Mock private FilePart filePart;

  private FileValidator fileValidator;

  private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

  @BeforeEach
  void setUp() {
    // Create a new instance of FileValidator using reflection since the constructor is private
    try {
      java.lang.reflect.Constructor<FileValidator> constructor =
          FileValidator.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      fileValidator = constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate FileValidator", e);
    }
  }

  @Test
  void validateFile_ValidPdfFile_ShouldComplete() {
    // Arrange
    String filename = "test.pdf";
    when(filePart.filename()).thenReturn(filename);

    // Create a small valid PDF file content
    byte[] pdfBytes = createMinimalPdfBytes();
    DataBuffer dataBuffer = bufferFactory.wrap(pdfBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart)).expectComplete().verify();
  }

  @Test
  void validateFile_ValidTxtFile_ShouldComplete() {
    // Arrange
    String filename = "test.txt";
    when(filePart.filename()).thenReturn(filename);

    // Create a text file content
    byte[] txtBytes = "This is a test text file".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = bufferFactory.wrap(txtBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert - txt files should now be allowed
    StepVerifier.create(fileValidator.validateFile(filePart)).expectComplete().verify();
  }

  @Test
  void validateFile_WithTestResourceFile_ShouldComplete() throws IOException {
    // Arrange
    String filename = "test-resource.txt";
    when(filePart.filename()).thenReturn(filename);

    // Load the test resource file
    ClassPathResource resource = new ClassPathResource("test-sample.txt");
    byte[] fileBytes = resource.getInputStream().readAllBytes();
    DataBuffer dataBuffer = bufferFactory.wrap(fileBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert - txt files should now be allowed
    StepVerifier.create(fileValidator.validateFile(filePart)).expectComplete().verify();
  }

  @Test
  void validateFile_InvalidFilename_ShouldThrowException() {
    // Arrange
    String invalidFilename = "test..pdf"; // Contains ".."
    when(filePart.filename()).thenReturn(invalidFilename);

    byte[] pdfBytes = createMinimalPdfBytes();
    DataBuffer dataBuffer = bufferFactory.wrap(pdfBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart))
        .expectError(InvalidFileException.class)
        .verify();
  }

  @Test
  void validateFile_InvalidExtension_ShouldThrowException() {
    // Arrange
    String invalidExtension = "test.docx"; // Not pdf or txt
    when(filePart.filename()).thenReturn(invalidExtension);

    byte[] docBytes = "fake document content".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = bufferFactory.wrap(docBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart))
        .expectError(InvalidFileException.class)
        .verify();
  }

  @Test
  void validateFile_EmptyFilename_ShouldThrowException() {
    // Arrange
    String emptyFilename = "";
    when(filePart.filename()).thenReturn(emptyFilename);

    byte[] contentBytes = "some content".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = bufferFactory.wrap(contentBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart))
        .expectError(InvalidFileException.class)
        .verify();
  }

  @Test
  void validateFile_FileTooLarge_ShouldThrowException() {
    // Arrange
    String filename = "test.pdf";
    when(filePart.filename()).thenReturn(filename);

    // Create a file that exceeds the 1.2MB limit
    byte[] largeFileBytes = new byte[1_300_000]; // Larger than MAX_FILE_SIZE
    DataBuffer dataBuffer = bufferFactory.wrap(largeFileBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart))
        .expectError(InvalidFileException.class)
        .verify();
  }

  @Test
  void validateFile_InvalidMimeType_ShouldThrowException() {
    // Arrange
    String filename = "test.pdf"; // PDF extension but actually a text file
    when(filePart.filename()).thenReturn(filename);

    // Create a file with PDF extension but HTML content
    // This will be detected as text/html by Tika, not application/pdf
    byte[] fakeContentBytes =
        "<!DOCTYPE html><html><body><h1>This is HTML not PDF</h1></body></html>"
            .getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = bufferFactory.wrap(fakeContentBytes);
    when(filePart.content()).thenReturn(Flux.just(dataBuffer));

    // Act & Assert
    StepVerifier.create(fileValidator.validateFile(filePart))
        .expectError(InvalidFileException.class)
        .verify();
  }

  @Test
  void validateFile_MultipleDataBuffers_ShouldCompleteForTxtFile() {
    // Arrange
    String filename = "test.txt";
    when(filePart.filename()).thenReturn(filename);

    // Create multiple data buffers
    byte[] part1 = "This is part 1 of the content. ".getBytes(StandardCharsets.UTF_8);
    byte[] part2 = "This is part 2 of the content.".getBytes(StandardCharsets.UTF_8);

    DataBuffer buffer1 = bufferFactory.wrap(part1);
    DataBuffer buffer2 = bufferFactory.wrap(part2);

    when(filePart.content()).thenReturn(Flux.just(buffer1, buffer2));

    // Act & Assert - txt files should now be allowed
    StepVerifier.create(fileValidator.validateFile(filePart)).expectComplete().verify();
  }

  @Test
  void validateFile_MultipleDataBuffers_ShouldCompleteForPdfFile() {
    // Arrange
    String filename = "test.pdf";
    when(filePart.filename()).thenReturn(filename);

    // Create multiple data buffers with valid PDF content
    byte[] pdfBytes = createMinimalPdfBytes();
    byte[] part1 = new byte[pdfBytes.length / 2];
    byte[] part2 = new byte[pdfBytes.length - part1.length];

    System.arraycopy(pdfBytes, 0, part1, 0, part1.length);
    System.arraycopy(pdfBytes, part1.length, part2, 0, part2.length);

    DataBuffer buffer1 = bufferFactory.wrap(part1);
    DataBuffer buffer2 = bufferFactory.wrap(part2);

    when(filePart.content()).thenReturn(Flux.just(buffer1, buffer2));

    // Act & Assert - PDF files should pass validation
    StepVerifier.create(fileValidator.validateFile(filePart)).expectComplete().verify();
  }

  /**
   * Creates the minimal bytes required for a valid PDF file. This creates a tiny but valid PDF file
   * for testing.
   */
  private byte[] createMinimalPdfBytes() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      // PDF Header
      baos.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
      // Object 1 - Catalog
      baos.write(
          "1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n".getBytes(StandardCharsets.UTF_8));
      // Object 2 - Pages
      baos.write(
          "2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n"
              .getBytes(StandardCharsets.UTF_8));
      // Object 3 - Page
      baos.write(
          "3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R>>\nendobj\n"
              .getBytes(StandardCharsets.UTF_8));
      // Object 4 - Content
      baos.write(
          "4 0 obj\n<</Length 21>>\nstream\nBT /F1 12 Tf 100 700 Td (Test PDF) Tj ET\nendstream\nendobj\n"
              .getBytes(StandardCharsets.UTF_8));
      // xref
      baos.write(
          "xref\n0 5\n0000000000 65535 f\n0000000010 00000 n\n0000000056 00000 n\n0000000111 00000 n\n0000000188 00000 n\n"
              .getBytes(StandardCharsets.UTF_8));
      // trailer
      baos.write(
          "trailer\n<</Size 5 /Root 1 0 R>>\nstartxref\n259\n%%EOF"
              .getBytes(StandardCharsets.UTF_8));
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to create minimal PDF", e);
    }
  }
}
