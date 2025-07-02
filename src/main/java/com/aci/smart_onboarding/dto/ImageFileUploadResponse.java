package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for image file upload operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Image file upload response containing file details and walletron information")
public class ImageFileUploadResponse {

  @Schema(description = "Generated filename stored in blob storage", example = "WALLET123-brand")
  private String fileName;

  @Schema(description = "Walletron ID associated with the image", example = "WALLET123")
  private String walletronId;

  @Schema(description = "Type of image uploaded", example = "BRAND")
  private String imageType;

  @Schema(description = "Original filename of the uploaded image", example = "logo.png")
  private String originalFileName;

  @Schema(description = "Size of the uploaded file in bytes", example = "512000")
  private long fileSize;

  @Schema(
      description = "URL of the uploaded file in blob storage",
      example = "https://storage.blob.core.windows.net/container/WALLET123-brand")
  private String fileUrl;
}
