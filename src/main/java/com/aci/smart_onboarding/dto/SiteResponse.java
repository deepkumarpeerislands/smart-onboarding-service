package com.aci.smart_onboarding.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteResponse {
  private String brdFormId;
  private String brdId;
  private String brdName;
  private String description;
  private String customerId;
  private boolean wallentronIncluded;
  private boolean achEncrypted;
  private boolean ssdAvailable;
  private boolean contractAvailable;
  private String originalSSDFileName;
  private String originalContractFileName;
  private String originalACHFileName;
  private String originalOtherFileName;
  private List<DivisionDetails> siteList;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private double score;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DivisionDetails {
    private String id;
    private String siteId;
    private String siteName;
    private String identifierCode;
    private String description;
    private BrdForm brdForm;
    private double score;
  }
}
