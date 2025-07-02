package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import java.util.List;
import lombok.Data;

@Data
public class UATTestCaseGroupedResponseDTO {
  private String featureName;
  private PortalTypes uatType;
  private List<TestDetailDTO> testDetails;

  @Data
  public static class TestDetailDTO {
    private String testName;
    private String scenario;
    private TestStatus status;
  }
}
