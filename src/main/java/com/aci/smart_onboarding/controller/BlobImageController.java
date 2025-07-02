package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.ImageResponseDto;
import com.aci.smart_onboarding.dto.ImageUploadRequest;
import com.aci.smart_onboarding.dto.ImageUploadResponse;
import com.aci.smart_onboarding.model.WalletronExampleImages;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Image Management", description = "APIs for managing image upload and retrieval")
public class BlobImageController {

  private final IBlobStorageService blobStorageService;
  private final ObjectMapper objectMapper;
  private final BRDSecurityService securityService;

  private Mono<ResponseEntity<ImageUploadResponse>> createErrorResponse() {
    ImageUploadResponse errorResponse =
        ImageUploadResponse.builder()
            .message("Failed to process images")
            .data(Collections.emptyList())
            .success(false)
            .build();
    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> context.getAuthentication().getName());
  }

  @Operation(
      summary = "Upload images",
      description =
          "Upload multiple images with titles to blob storage. If ID is provided, updates existing document with new image data. If no ID, creates new document with unique auto-generated ID.")
  @ApiResponse(responseCode = "200", description = "Images uploaded successfully")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - Only MANAGER role can upload images")
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<ImageUploadResponse>> uploadImages(
      @RequestPart("requests") String requestsJson, @RequestPart("files") Flux<FilePart> files) {

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to upload images but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException("Only MANAGER role can upload images"));
                          }

                          log.info(
                              "Authorized user {} with role {} is uploading images",
                              username,
                              role);
                          return Mono.fromCallable(
                                  () ->
                                      objectMapper.readValue(
                                          requestsJson,
                                          new TypeReference<List<ImageUploadRequest>>() {}))
                              .flatMap(
                                  requests ->
                                      blobStorageService.uploadImagesWithDocumentsAndMessage(
                                          requests, files))
                              .map(ResponseEntity::ok);
                        }))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                log.error("Access denied for image upload: {}", e.getMessage());
                return Mono.error(e);
              }
              log.error("Error processing upload request: ", e);
              return createErrorResponse();
            });
  }

  @Operation(
      summary = "Get document by ID",
      description = "Get a specific image document by its ID")
  @ApiResponse(responseCode = "200", description = "Document retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @GetMapping("/documents/{id}")
  public Mono<ResponseEntity<WalletronExampleImages>> getDocumentById(@PathVariable String id) {
    return blobStorageService
        .getDocumentById(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build())
        .onErrorResume(
            e -> {
              log.error("Error fetching document by ID {}: ", id, e);
              return Mono.just(ResponseEntity.internalServerError().build());
            });
  }

  @Operation(
      summary = "Get all images",
      description = "Get all images from all documents in the database")
  @ApiResponse(responseCode = "200", description = "All images retrieved successfully")
  @GetMapping
  public Mono<ResponseEntity<List<ImageResponseDto>>> getAllImages() {
    return blobStorageService
        .getAllImages()
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.ok(List.of()))
        .onErrorResume(
            e -> {
              log.error("Error fetching all images: ", e);
              return Mono.just(ResponseEntity.internalServerError().build());
            });
  }

  @Operation(
      summary = "Delete an image by document ID",
      description =
          "Delete an image from blob storage and its corresponding document from database using document ID")
  @ApiResponse(responseCode = "200", description = "Image deleted successfully")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - Only MANAGER role can delete images")
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<ImageUploadResponse>> deleteImageById(@PathVariable String id) {
    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to delete image but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException("Only MANAGER role can delete images"));
                          }

                          log.info(
                              "Authorized user {} with role {} is deleting image with ID {}",
                              username,
                              role,
                              id);
                          return blobStorageService
                              .deleteImageById(id)
                              .thenReturn(
                                  ImageUploadResponse.builder()
                                      .message("Image deleted successfully")
                                      .data(List.of())
                                      .success(true)
                                      .build())
                              .map(ResponseEntity::ok);
                        }))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                log.error("Access denied for image deletion: {}", e.getMessage());
                return Mono.error(e);
              }
              log.error("Error deleting image with ID {}: ", id, e);
              if (e instanceof IllegalArgumentException) {
                ImageUploadResponse errorResponse =
                    ImageUploadResponse.builder()
                        .message("Document not found with ID: " + id)
                        .data(List.of())
                        .success(false)
                        .build();
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
              }
              ImageUploadResponse errorResponse =
                  ImageUploadResponse.builder()
                      .message("Failed to delete image")
                      .data(List.of())
                      .success(false)
                      .build();
              return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
  }

  @GetMapping("/proxy")
  public Mono<ResponseEntity<byte[]>> proxyFile(@RequestParam("url") String url) {
    return blobStorageService.proxyFileFromUrl(url);
  }
}
