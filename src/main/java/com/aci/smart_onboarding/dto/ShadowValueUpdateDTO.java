package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowValueUpdateDTO {
  /** The ID of the BRD form - required for both BRD and SITE source types */
  @NotBlank(message = "BRD Form ID is required")
  private String brdFormId;

  /** The ID of the site - required only for SITE source type */
  private String siteId;

  /** The type of entity (BRD or SITE) */
  @NotBlank(message = "Source type is required")
  private String sourceType;

  /** Section name within the entity */
  @NotBlank(message = "Section name is required")
  private String sectionName;

  /** Field path within the section */
  @NotBlank(message = "Field path is required")
  private String fieldPath;
}
