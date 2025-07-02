package com.aci.smart_onboarding.util.walletron;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentUrls {
  private String testUrl;
  private String prodUrl;
  private String sectionStatus;
}
