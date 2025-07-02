package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
public class BrdFieldCommentGroupReq {
  @NotBlank(message = "BRD Form ID cannot be blank")
  private String brdFormId;

  // Site ID is required only when sourceType is SITE
  private String siteId;

  // Source type defaults to BRD if not specified
  @NotBlank(message = "Source type cannot be blank")
  @Pattern(regexp = "^(BRD|SITE)$", message = "Source type must be either 'BRD' or 'SITE'")
  private String sourceType;

  @NotBlank(message = "Field path cannot be blank")
  @Pattern(
      regexp = "^[a-zA-Z0-9]++(?:\\.[a-zA-Z0-9]++)*+$",
      message = "Field path must be in dot notation format (e.g., 'field' or 'parent.child')")
  private String fieldPath;

  private Object fieldPathShadowValue;

  @NotBlank(message = "Status cannot be blank")
  @Pattern(
      regexp = "^(Pending|Resolved)$",
      message = "Status must be either 'Pending' or 'Resolved'")
  private String status;

  // Section name where the field belongs
  @NotBlank(message = "SectionName cannot be blank")
  private String sectionName;

  // Username of the person who created the comment group
  private String createdBy;
}
