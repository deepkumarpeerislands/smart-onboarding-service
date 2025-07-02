package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for JSON template operations. Contains all template details for API responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON Template Response with complete template details")
public class JsonTemplateResponse {

  @Schema(description = "Unique template identifier", example = "64a7b8c9d1e2f3a4b5c6d7e8")
  private String id;

  @Schema(description = "Unique template name", example = "UserOnboardingTemplate")
  private String templateName;

  @Schema(
      description = "Generated file name in storage",
      example = "uuid-useronboardingtemplate-config.json")
  private String fileName;

  @Schema(description = "Original uploaded file name", example = "config.json")
  private String originalFileName;

  @Schema(description = "Username who uploaded the template", example = "john.doe@company.com")
  private String uploadedBy;

  @Schema(
      description = "Current template status",
      example = "InActive",
      allowableValues = {"Active", "InActive"})
  private String status;

  @Schema(description = "Template creation timestamp", example = "2023-12-01 10:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @Schema(description = "Template last update timestamp", example = "2023-12-01 10:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;
}
