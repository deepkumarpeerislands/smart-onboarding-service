package com.aci.smart_onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteRequest {
  @NotBlank(message = "BRD ID cannot be blank")
  private String brdId;

  private boolean wallentronIncluded;
  private boolean achEncrypted;

  @NotEmpty(message = "Site list cannot be empty")
  @Valid
  private List<SiteDetails> siteList;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SiteDetails {
    @NotBlank(message = "Site name cannot be blank")
    private String siteName;

    @NotBlank(message = "Identifier code cannot be blank")
    private String identifierCode;

    private String description;

    private BrdForm brdForm;
  }
}
