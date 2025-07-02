package com.aci.smart_onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteUpdateRequest {
  @NotBlank(message = "Site ID cannot be blank")
  private String siteId;

  @Valid private SiteRequest.SiteDetails siteDetails;
}
