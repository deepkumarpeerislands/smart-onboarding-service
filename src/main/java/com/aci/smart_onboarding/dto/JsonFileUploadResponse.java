package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for JSON file upload operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON file upload response containing file details and template information")
public class JsonFileUploadResponse {

  @Schema(
      description = "Template name associated with the JSON file",
      example = "customer-onboarding-template")
  private String templateName;

  @Schema(description = "Original filename of the uploaded JSON file", example = "template.json")
  private String originalFileName;

  @Schema(
      description = "Generated filename stored in blob storage",
      example = "uuid-templateName-template.json")
  private String generatedFileName;

  @Schema(description = "Size of the uploaded file in bytes", example = "2048")
  private long fileSize;

  @Schema(description = "Complete template details created during upload")
  private JsonTemplateResponse templateDetails;
}
