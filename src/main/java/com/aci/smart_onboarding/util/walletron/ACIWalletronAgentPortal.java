package com.aci.smart_onboarding.util.walletron;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACIWalletronAgentPortal {
  private List<WalletronAgentPortal> walletronAgentPortal;
  private String uploadFile;
  private String sectionStatus;

  @AssertTrue(message = "Either walletronAgentPortal or uploadFile must be provided")
  private boolean isEitherWalletronAgentPortalOrUploadFileProvided() {
    return (walletronAgentPortal != null && !walletronAgentPortal.isEmpty())
        || (uploadFile != null && !uploadFile.trim().isEmpty());
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WalletronAgentPortal {
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email address")
    private String emailAddress;

    private String role;
  }
}
