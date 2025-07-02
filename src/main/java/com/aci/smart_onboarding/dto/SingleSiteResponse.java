package com.aci.smart_onboarding.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleSiteResponse {
  private String id;
  private String brdId;
  private String siteId;
  private String siteName;
  private String identifierCode;
  private String description;
  private BrdForm brdForm;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
