package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.exception.InvalidFileException;
import com.aci.smart_onboarding.swagger.BRDRequestAndResponses;
import com.aci.smart_onboarding.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Tag(
    name = "Validate File",
    description = "APIs for pre validation before file is uploaded to SmartOnBoardingAI")
@RequestMapping(value = "${api.default.path}/validate", name = "File Validation")
public class FileValidateController {

  private final FileValidator fileValidator;

  @Operation(
      summary = "Upload a file",
      description = "Allows users to validate PDF or TXT files on size, type, and security")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File validated successfully",
            content =
                @Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = @Schema(type = "string"),
                    examples = {
                      @ExampleObject(
                          name = "Successful",
                          value = BRDRequestAndResponses.FILE_UPLOAD_SUCCESS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format or size",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Validation Error",
                          value = BRDRequestAndResponses.INVALID_FILE_FORMAT_SIZE)
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
                          value = BRDRequestAndResponses.INTERNAL_SERVER_ERROR)
                    }))
      })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<Api<String>>> uploadFile(
      @Parameter(description = "File to validate", required = true) @RequestPart("file") @NotNull
          FilePart filePart) {

    return fileValidator
        .validateFile(filePart)
        .then(
            Mono.just(
                buildResponse(
                    HttpStatus.OK,
                    "File validated successfully",
                    Optional.of("Success"),
                    Optional.empty())))
        .onErrorResume(
            InvalidFileException.class,
            e ->
                Mono.just(
                    buildResponse(
                        HttpStatus.BAD_REQUEST,
                        e.getMessage(),
                        Optional.empty(),
                        Optional.of(Map.of("error", e.getMessage())))))
        .onErrorResume(
            Exception.class,
            e ->
                Mono.just(
                    buildResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error processing file",
                        Optional.empty(),
                        Optional.of(Map.of("error", e.getMessage())))));
  }

  private ResponseEntity<Api<String>> buildResponse(
      HttpStatus status,
      String message,
      Optional<String> data,
      Optional<Map<String, String>> errors) {
    Api<String> response = new Api<>(status.toString(), message, data, errors);
    return ResponseEntity.status(status).body(response);
  }
}
