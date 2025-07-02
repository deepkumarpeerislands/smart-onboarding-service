package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.FileConstants;
import com.aci.smart_onboarding.dto.ImageResponseDto;
import com.aci.smart_onboarding.dto.ImageUploadRequest;
import com.aci.smart_onboarding.dto.ImageUploadResponse;
import com.aci.smart_onboarding.exception.BlobStorageException;
import com.aci.smart_onboarding.model.WalletronExampleImages;
import com.aci.smart_onboarding.repository.WalletronExampleImagesRepository;
import com.aci.smart_onboarding.service.IBlobStorageService;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class BlobStorageService implements IBlobStorageService {

  private final BlobContainerClient blobContainerClient;
  private final WalletronExampleImagesRepository walletronExampleImagesRepository;

  public BlobStorageService(
      BlobContainerClient blobContainerClient,
      WalletronExampleImagesRepository walletronExampleImagesRepository) {
    this.blobContainerClient = blobContainerClient;
    this.walletronExampleImagesRepository = walletronExampleImagesRepository;
  }

  @Override
  public Mono<byte[]> fetchFile(String fileName) {
    log.info("Fetching file from blob storage: {}", fileName);
    return Mono.fromCallable(
            () -> {
              try {
                BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
                if (Boolean.FALSE.equals(blobClient.exists())) {
                  log.error("File does not exist in blob storage: {}", fileName);
                  throw new IOException(FileConstants.FILE_NOT_FOUND + fileName);
                }

                long blobSize = blobClient.getProperties().getBlobSize();
                log.info(
                    "File exists in blob storage, properties: size={}, lastModified={}",
                    blobSize,
                    blobClient.getProperties().getLastModified());

                if (blobSize == 0) {
                  log.warn("Blob size is zero for file: {}", fileName);
                  return new byte[0];
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) blobSize);
                blobClient.downloadStream(outputStream);
                byte[] fileContent = outputStream.toByteArray();

                if (fileContent.length == 0 && blobSize > 0) {
                  log.warn(
                      "First download method returned empty content for {}. Trying alternative...",
                      fileName);

                  ByteArrayOutputStream alternativeStream =
                      new ByteArrayOutputStream((int) blobSize);
                  blobClient.downloadStream(alternativeStream);
                  fileContent = alternativeStream.toByteArray();

                  log.info("Alternative download method result: {} bytes", fileContent.length);
                }

                log.info("Downloaded file {} with size: {} bytes", fileName, fileContent.length);
                if (fileContent.length == 0) {
                  log.warn("Downloaded file has zero bytes after multiple attempts: {}", fileName);
                }

                return fileContent;
              } catch (IOException e) {
                log.error("IO Exception while fetching file {}: {}", fileName, e.getMessage());
                throw e;
              } catch (Exception e) {
                log.error(
                    "Unexpected error while fetching file {}: {}", fileName, e.getMessage(), e);
                throw new IOException("Error downloading file: " + e.getMessage(), e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("Error fetching file {}: {}", fileName, e.getMessage()));
  }

  @Override
  public Mono<String> getFileUrl(String fileName) {
    return Mono.fromCallable(
            () -> {
              BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
              if (Boolean.FALSE.equals(blobClient.exists())) {
                throw new IOException("File not found: " + fileName);
              }
              return blobClient.getBlobUrl();
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<byte[]> fetchFileFromUrl(String blobUrl) {
    return Mono.fromCallable(
            () -> {
              try {
                URI uri = new URI(blobUrl);
                String path = uri.getPath();
                if (path.startsWith("/")) {
                  path = path.substring(1);
                }

                String[] parts = path.split("/", 2);
                if (parts.length != 2) {
                  throw new IOException("Invalid blob URL format");
                }

                String containerName = parts[0];
                String blobName = parts[1];

                BlobClient blobClient =
                    blobContainerClient
                        .getServiceClient()
                        .getBlobContainerClient(containerName)
                        .getBlobClient(blobName);

                if (Boolean.FALSE.equals(blobClient.exists())) {
                  throw new IOException("File not found at URL: " + blobUrl);
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobClient.downloadStream(outputStream);
                return outputStream.toByteArray();

              } catch (URISyntaxException e) {
                throw new IOException("Invalid blob URL: " + blobUrl, e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<Void> updateFile(String filePath, String content) {
    return Mono.fromCallable(
            () -> {
              try {
                BlobClient blobClient = blobContainerClient.getBlobClient(filePath);
                boolean exists = blobClient.exists();
                if (!exists) {
                  throw new BlobStorageException("File not found: " + filePath);
                }
                blobClient.upload(BinaryData.fromString(content), true);
                return null;
              } catch (Exception e) {
                log.error("Error updating file in blob storage: {}", e.getMessage());
                throw new BlobStorageException("Failed to update file in blob storage", e);
              }
            })
        .then();
  }

  @Override
  public Mono<String> uploadFile(String fileName, byte[] fileContent) {
    return Mono.fromCallable(
            () -> {
              BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
              try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
                blobClient.upload(inputStream, fileContent.length, true);
                return blobClient.getBlobUrl();
              } catch (IOException e) {
                throw new IOException("Failed to upload file: " + e.getMessage(), e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Flux<String> listFiles() {
    return Flux.defer(
            () -> {
              List<String> fileNames = new ArrayList<>();
              blobContainerClient
                  .listBlobs()
                  .forEach(blobItem -> fileNames.add(blobItem.getName()));
              return Flux.fromIterable(fileNames);
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<Void> deleteFile(String fileName) {
    log.info("Deleting file from blob storage: {}", fileName);
    return Mono.fromCallable(
            () -> {
              try {
                BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
                if (Boolean.TRUE.equals(blobClient.exists())) {
                  blobClient.delete();
                  log.info("Successfully deleted file: {}", fileName);
                } else {
                  log.warn("File not found for deletion: {}", fileName);
                }
                return null;
              } catch (Exception e) {
                log.error("Error deleting file {}: {}", fileName, e.getMessage());
                throw new BlobStorageException("Failed to delete file from blob storage", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  @Override
  public Flux<String> uploadImages(List<ImageUploadData> images) {
    if (images == null || images.isEmpty()) {
      log.warn("No images provided for upload");
      return Flux.empty();
    }

    return Flux.fromIterable(images)
        .flatMap(
            image ->
                Mono.fromCallable(
                        () -> {
                          log.info(
                              "Starting image upload with SAS token for file: {}",
                              image.getFileName());
                          BlobClient blobClient =
                              blobContainerClient.getBlobClient(image.getFileName());

                          // Set content type
                          BlobHttpHeaders headers =
                              new BlobHttpHeaders().setContentType(image.getContentType());
                          log.info("Set content type to: {}", image.getContentType());

                          // Upload the file
                          try (InputStream inputStream =
                              new ByteArrayInputStream(image.getFileContent())) {
                            log.info(
                                "Uploading file with size: {} bytes",
                                image.getFileContent().length);
                            blobClient.upload(inputStream, image.getFileContent().length, true);
                            blobClient.setHttpHeaders(headers);
                            log.info("File uploaded successfully");
                          }

                          // Generate SAS token with all necessary permissions
                          OffsetDateTime expiryTime =
                              OffsetDateTime.now().plusYears(FileConstants.SAS_TOKEN_EXPIRY_YEARS);
                          BlobSasPermission permission =
                              new BlobSasPermission()
                                  .setReadPermission(true)
                                  .setWritePermission(true)
                                  .setDeletePermission(true)
                                  .setListPermission(true)
                                  .setCreatePermission(true);

                          // Set SAS token properties
                          BlobServiceSasSignatureValues values =
                              new BlobServiceSasSignatureValues(expiryTime, permission)
                                  .setStartTime(OffsetDateTime.now())
                                  .setProtocol(com.azure.storage.common.sas.SasProtocol.HTTPS_ONLY);

                          // Generate SAS token
                          String sasToken = blobClient.generateSas(values);
                          String fullUrl = blobClient.getBlobUrl() + "?" + sasToken;
                          log.info("Generated SAS token URL for file: {}", image.getFileName());

                          // Verify the blob exists and is accessible
                          boolean exists = blobClient.exists();
                          if (exists) {
                            log.info(
                                "Blob exists and is accessible. Size: {} bytes",
                                blobClient.getProperties().getBlobSize());
                          } else {
                            log.error("Blob does not exist after upload!");
                            throw new BlobStorageException("Blob upload failed");
                          }

                          return fullUrl;
                        })
                    .subscribeOn(Schedulers.boundedElastic()))
        .doOnComplete(() -> log.info("Completed uploading all images"))
        .doOnError(error -> log.error("Error uploading images: ", error));
  }

  // Image Management Business Logic Methods

  /**
   * Upload multiple images with individual documents Each file creates a separate document with
   * unique auto-generated ID
   */
  public Mono<List<ImageResponseDto>> uploadImagesWithDocuments(
      List<ImageUploadRequest> requests, Flux<FilePart> files) {
    return files
        .collectList()
        .flatMap(
            fileList -> {
              if (fileList.isEmpty() || requests.isEmpty() || fileList.size() != requests.size()) {
                return Mono.error(
                    new IllegalArgumentException("Invalid request: files and requests must match"));
              }

              List<Mono<ImageResponseDto>> uploadMonos = new ArrayList<>();

              for (int i = 0; i < fileList.size(); i++) {
                FilePart file = fileList.get(i);
                ImageUploadRequest request = requests.get(i);

                Mono<ImageResponseDto> uploadMono =
                    processSingleFileWithDocument(file, request)
                        .doOnSuccess(
                            response ->
                                log.info(
                                    "Successfully processed file {} with title: {}",
                                    file.filename(),
                                    request.getTitle()))
                        .doOnError(
                            error ->
                                log.error(
                                    "Error processing file {}: {}",
                                    file.filename(),
                                    error.getMessage()));

                uploadMonos.add(uploadMono);
              }

              return Flux.fromIterable(uploadMonos).flatMap(mono -> mono).collectList();
            });
  }

  private Mono<ImageResponseDto> processSingleFileWithDocument(
      FilePart file, ImageUploadRequest request) {
    String originalFileName = file.filename();
    String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

    // If ID is provided, it means this is an existing document - UPDATE it with new data
    if (request.getId() != null && !request.getId().trim().isEmpty()) {
      log.info(
          "ID provided for file {}: {}. Updating existing document with new data.",
          originalFileName,
          request.getId());
      return walletronExampleImagesRepository
          .findById(request.getId())
          .switchIfEmpty(
              Mono.error(
                  new IllegalArgumentException("Document not found with ID: " + request.getId())))
          .flatMap(
              existingDocument ->
                  file.content()
                      .reduce(
                          new byte[0],
                          (acc, dataBuffer) -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            byte[] newAcc = new byte[acc.length + bytes.length];
                            System.arraycopy(acc, 0, newAcc, 0, acc.length);
                            System.arraycopy(bytes, 0, newAcc, acc.length, bytes.length);
                            return newAcc;
                          })
                      .flatMap(
                          bytes -> {
                            ImageUploadData uploadData =
                                new ImageUploadData(
                                    uniqueFileName,
                                    bytes,
                                    file.headers().getContentType() != null
                                        ? file.headers().getContentType().toString()
                                        : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    0);

                            return uploadImages(List.of(uploadData))
                                .next()
                                .flatMap(
                                    url -> {
                                      ImageResponseDto updatedImage =
                                          ImageResponseDto.builder()
                                              .fileName(originalFileName)
                                              .url(url)
                                              .title(request.getTitle())
                                              .build();

                                      existingDocument.setImage(updatedImage);
                                      existingDocument.setUpdatedAt(LocalDateTime.now());

                                      return walletronExampleImagesRepository
                                          .save(existingDocument)
                                          .map(
                                              savedDocument ->
                                                  ImageResponseDto.builder()
                                                      .id(savedDocument.getId())
                                                      .fileName(updatedImage.getFileName())
                                                      .url(updatedImage.getUrl())
                                                      .title(updatedImage.getTitle())
                                                      .build());
                                    });
                          }));
    }

    // If no ID provided, create new document
    log.info("No ID provided for file {}. Creating new document.", originalFileName);
    return file.content()
        .reduce(
            new byte[0],
            (acc, dataBuffer) -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              byte[] newAcc = new byte[acc.length + bytes.length];
              System.arraycopy(acc, 0, newAcc, 0, acc.length);
              System.arraycopy(bytes, 0, newAcc, acc.length, bytes.length);
              return newAcc;
            })
        .flatMap(
            bytes -> {
              ImageUploadData uploadData =
                  new ImageUploadData(
                      uniqueFileName,
                      bytes,
                      file.headers().getContentType() != null
                          ? file.headers().getContentType().toString()
                          : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                      0);

              return uploadImages(List.of(uploadData))
                  .next()
                  .flatMap(
                      url -> {
                        ImageResponseDto imageResponse =
                            ImageResponseDto.builder()
                                .fileName(originalFileName)
                                .url(url)
                                .title(request.getTitle())
                                .build();

                        return createNewDocument(imageResponse);
                      });
            });
  }

  private Mono<ImageResponseDto> createNewDocument(ImageResponseDto imageResponse) {
    WalletronExampleImages newDocument =
        WalletronExampleImages.builder()
            .image(imageResponse) // Single image instead of array
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    return walletronExampleImagesRepository
        .save(newDocument)
        .map(
            savedDocument ->
                ImageResponseDto.builder()
                    .id(savedDocument.getId())
                    .fileName(imageResponse.getFileName())
                    .url(imageResponse.getUrl())
                    .title(imageResponse.getTitle())
                    .build());
  }

  /** Get a specific image document by its ID */
  public Mono<WalletronExampleImages> getDocumentById(String id) {
    return walletronExampleImagesRepository.findById(id);
  }

  /** Get all images from all documents */
  public Mono<List<ImageResponseDto>> getAllImages() {
    return walletronExampleImagesRepository
        .findAll()
        .collectList()
        .map(
            documents ->
                documents.stream()
                    .filter(doc -> doc.getImage() != null)
                    .sorted(
                        (doc1, doc2) -> {
                          String createdBy1 =
                              doc1.getCreatedBy() != null ? doc1.getCreatedBy() : "";
                          String createdBy2 =
                              doc2.getCreatedBy() != null ? doc2.getCreatedBy() : "";
                          return createdBy1.compareTo(createdBy2);
                        })
                    .map(
                        doc ->
                            ImageResponseDto.builder()
                                .id(doc.getId())
                                .fileName(doc.getImage().getFileName())
                                .url(doc.getImage().getUrl())
                                .title(doc.getImage().getTitle())
                                .build())
                    .toList())
        .defaultIfEmpty(List.of());
  }

  /**
   * Delete an image by document ID First checks if document exists, then deletes from blob storage,
   * then deletes from database
   */
  public Mono<Void> deleteImageById(String id) {
    log.info("Deleting image document with ID: {}", id);

    return walletronExampleImagesRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("Document not found with ID: " + id)))
        .flatMap(
            document -> {
              if (document.getImage() == null) {
                log.warn("Document with ID {} has no image, deleting document only", id);
                return walletronExampleImagesRepository.delete(document);
              }

              // Get the image from the document (each document contains only one image)
              ImageResponseDto imageToDelete = document.getImage();
              String url = imageToDelete.getUrl();

              // Extract storage filename from URL
              String storageFileName = extractStorageFileName(url);
              log.info("Extracted storage filename: {} from URL", storageFileName);

              // Delete from blob storage first, then from database
              return deleteFile(storageFileName)
                  .doOnSuccess(
                      v ->
                          log.info(
                              "Successfully deleted file from blob storage: {}", storageFileName))
                  .doOnError(
                      error ->
                          log.error(
                              "Failed to delete file from blob storage: {}",
                              storageFileName,
                              error))
                  .then(
                      Mono.defer(
                          () -> {
                            log.info("Deleting document from database with ID: {}", id);
                            return walletronExampleImagesRepository.delete(document);
                          }))
                  .doOnSuccess(
                      v -> log.info("Successfully deleted document from database with ID: {}", id))
                  .doOnError(
                      error ->
                          log.error(
                              "Failed to delete document from database with ID: {}", id, error));
            });
  }

  /** Extract storage filename from blob URL */
  private String extractStorageFileName(String url) {
    try {
      // URL format: https://storage.blob.core.windows.net/container/uuid_filename.ext?sastoken
      String urlWithoutParams = url.substring(0, url.indexOf('?'));
      return urlWithoutParams.substring(urlWithoutParams.lastIndexOf('/') + 1);
    } catch (Exception e) {
      log.error("Failed to extract storage filename from URL: {}", url, e);
      throw new IllegalArgumentException("Invalid blob URL format: " + url);
    }
  }

  public static class ImageUploadData {
    private final String fileName;
    private final byte[] fileContent;
    private final String contentType;
    private final int index;

    public ImageUploadData(String fileName, byte[] fileContent, String contentType, int index) {
      this.fileName = fileName;
      this.fileContent = fileContent;
      this.contentType = contentType;
      this.index = index;
    }

    public String getFileName() {
      return fileName;
    }

    public byte[] getFileContent() {
      return fileContent;
    }

    public String getContentType() {
      return contentType;
    }

    public int getIndex() {
      return index;
    }
  }

  /** Generate appropriate message based on uploaded images */
  private String generateUploadMessage(List<ImageResponseDto> uploadedImages) {
    // Count how many were created vs updated
    if (uploadedImages.size() == 1) {
      return "Image processed successfully";
    } else {
      return uploadedImages.size() + " images processed successfully";
    }
  }

  /** Upload multiple images with individual documents and return response with message */
  public Mono<ImageUploadResponse> uploadImagesWithDocumentsAndMessage(
      List<ImageUploadRequest> requests, Flux<FilePart> files) {
    return uploadImagesWithDocuments(requests, files)
        .map(
            uploadedImages -> {
              String message = generateUploadMessage(uploadedImages);
              return ImageUploadResponse.builder()
                  .message(message)
                  .data(uploadedImages)
                  .success(true)
                  .build();
            });
  }

  @Override
  public Mono<ResponseEntity<byte[]>> proxyFileFromUrl(String url) {
    return fetchFileFromUrl(url)
        .map(
            bytes ->
                ResponseEntity.ok()
                    .header("Content-Disposition", "inline")
                    .header("Content-Type", "application/octet-stream")
                    .body(bytes))
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
