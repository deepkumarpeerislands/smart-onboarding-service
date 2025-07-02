package com.aci.smart_onboarding.util.walletron;

import jakarta.validation.constraints.Email;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStrategy {
  private List<WebsiteOrMobileAppStrategy> websiteOrMobileApp;
  private List<EmailOrStatementQRCode> emailOrStatementQRCode;
  private String sectionStatus;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WebsiteOrMobileAppStrategy {
    private String strategy;
    private String btnPresent;
    private String additionalAuthPage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EmailOrStatementQRCode {
    @Email(message = "Invalid email address")
    private String channel;

    private String campaign;
    private String oneTimeOrContinuing;
  }
}
