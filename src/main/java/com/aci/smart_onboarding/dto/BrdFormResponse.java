package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dynamic BRD Form Response based on template configuration")
public class BrdFormResponse {
  private String brdFormId;
  private String status;
  private String projectId;
  private String brdId;
  private String brdName;
  private String description;
  private String organizationId;
  private String creator;
  private String type;
  private String notes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String templateType;
  private String summary;

  private Map<String, Object> sections;
}
