package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteDifferencesResponse {
  private String brdId;
  private List<SiteDifference> sites;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SiteDifference {
    private String siteId;
    private String siteName;
    private List<FieldDifference> differences;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FieldDifference {
    private String fieldName;
    private Object orgBrdValue;
    private Object siteBrdValue;
  }
}
