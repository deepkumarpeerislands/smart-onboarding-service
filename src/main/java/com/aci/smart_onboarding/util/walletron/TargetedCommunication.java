package com.aci.smart_onboarding.util.walletron;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetedCommunication {
  private List<WebsiteCommunication> website;
  private String sectionStatus;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WebsiteCommunication {
    private String dataPoints;
    private String dataType;
    private String options;
  }
}
