package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.ImageResponseDto;
import com.aci.smart_onboarding.dto.ImageUploadRequest;
import com.aci.smart_onboarding.dto.ImageUploadResponse;
import com.aci.smart_onboarding.model.WalletronExampleImages;
import com.aci.smart_onboarding.service.implementation.BlobStorageService.ImageUploadData;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Interface for blob storage operations */
public interface IBlobStorageService {
  /**
   * Fetches a file from blob storage by its name
   *
   * @param fileName The name of the file to fetch
   * @return A Mono containing the file content as byte array
   */
  Mono<byte[]> fetchFile(String fileName);

  /**
   * Gets the URL for a file in blob storage
   *
   * @param fileName The name of the file
   * @return A Mono containing the file URL
   */
  Mono<String> getFileUrl(String fileName);

  /**
   * Fetches a file from blob storage using its URL
   *
   * @param blobUrl The URL of the blob
   * @return A Mono containing the file content as byte array
   */
  Mono<byte[]> fetchFileFromUrl(String blobUrl);

  /**
   * Uploads a file to blob storage
   *
   * @param fileName The name to save the file as
   * @param fileContent The content of the file as byte array
   * @return A Mono containing the URL of the uploaded file
   */
  Mono<String> uploadFile(String fileName, byte[] fileContent);

  /**
   * Updates an existing file in blob storage with new content
   *
   * @param filePath The path of the file to update
   * @param content The new content of the file as a string
   * @return A Mono<Void> indicating completion
   */
  Mono<Void> updateFile(String filePath, String content);

  /**
   * Lists all files in the blob storage
   *
   * @return A Flux containing all file names
   */
  Flux<String> listFiles();

  /**
   * Deletes a file from blob storage
   *
   * @param fileName The name of the file to delete
   * @return A Mono<Void> indicating completion
   */
  Mono<Void> deleteFile(String fileName);

  /**
   * Uploads multiple images to blob storage with SAS tokens
   *
   * @param images List of image upload data
   * @return A Flux containing URLs with SAS tokens
   */
  Flux<String> uploadImages(List<ImageUploadData> images);

  // Image Management Business Logic Methods

  /**
   * Upload multiple images with individual documents Each file creates a separate document with
   * unique auto-generated ID
   *
   * @param requests List of upload requests containing titles
   * @param files Flux of files to upload
   * @return List of image responses
   */
  Mono<List<ImageResponseDto>> uploadImagesWithDocuments(
      List<ImageUploadRequest> requests, Flux<FilePart> files);

  /**
   * Upload multiple images with individual documents and return complete response with message
   *
   * @param requests List of upload requests containing titles
   * @param files Flux of files to upload
   * @return Complete upload response with message and data
   */
  Mono<ImageUploadResponse> uploadImagesWithDocumentsAndMessage(
      List<ImageUploadRequest> requests, Flux<FilePart> files);

  /**
   * Get a specific image document by its ID
   *
   * @param id Document ID
   * @return Image document
   */
  Mono<WalletronExampleImages> getDocumentById(String id);

  /**
   * Get all images from all documents
   *
   * @return List of all images
   */
  Mono<List<ImageResponseDto>> getAllImages();

  /**
   * Delete an image by document ID First checks if document exists, then deletes from blob storage,
   * then deletes from database
   *
   * @param id Document ID
   * @return Void on successful deletion
   */
  Mono<Void> deleteImageById(String id);

  Mono<ResponseEntity<byte[]>> proxyFileFromUrl(String url);
}
