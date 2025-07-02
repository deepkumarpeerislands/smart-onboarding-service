package com.aci.smart_onboarding.util.walletron;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletronApprovals {
  private String clientRepresentativeNameAndTitle;
  private String clientRepresentativeSignature;
  private LocalDateTime clientRepresentativeDate;
  private String aciWorldwideProjectManagerName;
  private String aciWorldwideProjectManagerSignature;
  private LocalDateTime aciWorldwideProjectManagerDate;
  private String sectionStatus;
}
