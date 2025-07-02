package com.aci.smart_onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.exception.InvalidFileException;
import com.aci.smart_onboarding.util.FileValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FileValidateControllerTest {

  @Mock private FileValidator fileValidator;
  @InjectMocks private FileValidateController fileUploadController;

  @Test
  @DisplayName("Should successfully upload a valid PDF file")
  void uploadFile_WithValidPdf_ShouldReturnSuccess() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(fileValidator.validateFile(any())).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<String>>> response = fileUploadController.uploadFile(filePart);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.OK
                    && result.getBody() != null
                    && result.getBody().getStatus().equals("200 OK")
                    && result.getBody().getMessage().equals("File validated successfully")
                    && result.getBody().getData().isPresent()
                    && result.getBody().getData().get().equals("Success"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return error when file validation fails")
  void uploadFile_WithInvalidFile_ShouldReturnError() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(fileValidator.validateFile(any()))
        .thenReturn(Mono.error(new InvalidFileException("Invalid file")));

    // Act
    Mono<ResponseEntity<Api<String>>> response = fileUploadController.uploadFile(filePart);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && result.getBody().getStatus().equals("400 BAD_REQUEST")
                    && result.getBody().getMessage().equals("Invalid file")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @ParameterizedTest
  @ValueSource(strings = {"test.doc", "test.xls", "test.ppt", "test.rtf"})
  @DisplayName("Should return error for invalid file extensions")
  void uploadFile_WithInvalidExtension_ShouldReturnBadRequest(String fileName) {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(fileValidator.validateFile(any()))
        .thenReturn(
            Mono.error(
                new InvalidFileException(
                    "Invalid file extension. Only PDF and TXT files are allowed.")));

    // Act
    Mono<ResponseEntity<Api<String>>> response = fileUploadController.uploadFile(filePart);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.BAD_REQUEST
                    && result.getBody() != null
                    && result.getBody().getStatus().equals("400 BAD_REQUEST")
                    && result
                        .getBody()
                        .getMessage()
                        .equals("Invalid file extension. Only PDF and TXT files are allowed.")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return generic error when encryption fails")
  void uploadFile_WithEncryptionError_ShouldReturnServerError() {
    // Arrange
    FilePart filePart = mock(FilePart.class);
    when(fileValidator.validateFile(any()))
        .thenReturn(Mono.error(new RuntimeException("Encryption error")));

    // Act
    Mono<ResponseEntity<Api<String>>> response = fileUploadController.uploadFile(filePart);

    // Assert
    StepVerifier.create(response)
        .expectNextMatches(
            result ->
                result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && result.getBody() != null
                    && result.getBody().getStatus().equals("500 INTERNAL_SERVER_ERROR")
                    && result.getBody().getMessage().equals("Error processing file")
                    && result.getBody().getData().isEmpty())
        .verifyComplete();
  }
}
