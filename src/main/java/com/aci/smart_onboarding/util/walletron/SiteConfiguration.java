package com.aci.smart_onboarding.util.walletron;

import com.aci.smart_onboarding.util.walletron.subsections.BackPassField;
import com.aci.smart_onboarding.util.walletron.subsections.FrontPassField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfiguration {

  private List<FeatureOption> features;
  private String numberOfFields;
  private List<VerificationField> fields;
  private List<FrontPassField> frontPassInformation;

  @NotBlank(message = "Brand logo is required")
  private String brandLogo;

  private String iconImage;

  private String stripImage;

  @NotBlank(message = "Thumbnail image is required")
  private String thumbnailImage;

  @NotBlank(message = "Client name is required")
  private String clientName;

  @NotBlank(message = "Logo is required")
  private String logo;

  private String notificationLogo;

  private String backgroundColor;
  private String labelsColor;

  @NotEmpty(message = "Back pass information must contain at least one payment field")
  private List<BackPassField> backPassInformation;

  private String sectionStatus;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FeatureOption {
    private String viewValue;
    private Boolean selected;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VerificationField {
    private String viewValue;
    private Boolean selected;
  }
}
