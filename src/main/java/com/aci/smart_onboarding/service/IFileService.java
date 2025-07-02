package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ImageFileUploadResponse;
import com.aci.smart_onboarding.dto.JsonFileUploadResponse;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/** Service interface for file operations including upload, download, encryption and decryption. */
public interface IFileService {

  /**
   * Encrypts and uploads a file to blob storage
   *
   * @param filePart The file to upload
   * @param fileContent The raw file content as byte array
   * @param fileType The type of file uploaded (ACH or WALLETRON)
   * @param brdId The ID of the BRD document
   * @return URL of the uploaded file
   */
  Mono<String> uploadEncryptedFile(
      FilePart filePart, byte[] fileContent, String fileType, String brdId);

  /**
   * Downloads a file from blob storage and decrypts it if needed
   *
   * @param originalFileName The original name of the file to download
   * @return The file content as a Resource
   */
  Mono<Resource> downloadFile(String originalFileName);

  /**
   * Lists all files available in blob storage
   *
   * @return a list of file names
   */
  Mono<List<String>> listFiles();

  /**
   * Encrypts and uploads a JSON file to blob storage
   *
   * @param filePart The JSON file to upload
   * @param fileContent The raw file content as byte array
   * @param templateName The template name associated with the JSON file
   * @return ResponseEntity with Api wrapper containing JsonFileUploadResponse
   */
  Mono<ResponseEntity<Api<JsonFileUploadResponse>>> uploadJsonFile(
      FilePart filePart, byte[] fileContent, String templateName);

  /**
   * Uploads an image file to blob storage with validation
   *
   * @param filePart The image file to upload
   * @param fileContent The raw file content as byte array
   * @param imageType The type of image (BRAND, LOGO, ICON, STRIP, THUMBNAIL)
   * @param walletronId The walletron ID to use in the filename
   * @return ResponseEntity with Api wrapper containing ImageFileUploadResponse
   */
  Mono<ResponseEntity<Api<ImageFileUploadResponse>>> uploadImageFile(
      FilePart filePart, byte[] fileContent, String imageType, String walletronId);

  /**
   * Deletes an image file from blob storage by fileName
   *
   * @param fileName The name of the image file to delete
   * @return ResponseEntity with Api wrapper containing deletion result
   */
  Mono<ResponseEntity<Api<String>>> deleteImageFileByName(String fileName);

  /**
   * Fetches images from blob storage URLs and returns them as base64 encoded byte arrays
   *
   * @param blobUrls List of blob storage URLs to fetch and encode
   * @return Mono containing a Map with blob URLs as keys and base64 encoded byte arrays as values
   */
  Mono<Map<String, byte[]>> getBase64EncodedImages(List<String> blobUrls);
}
