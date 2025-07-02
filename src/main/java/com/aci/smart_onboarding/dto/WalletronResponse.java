package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.walletron.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletronResponse {

  private String brdId;
  private String brdName;
  private Boolean walletronEnabled;
  private String walletronId;

  private SiteConfiguration siteConfiguration;
  private NotificationsOptions notificationsOptions;
  private ACIWalletronAgentPortal aciWalletronAgentPortal;
  private DataExchange aciWalletronDataExchange;
  private EnrollmentStrategy aciWalletronEnrollmentStrategy;
  private EnrollmentUrls enrollmentUrl;
  private TargetedCommunication targetedCommunication;
  private AciCash aciCash;
  private WalletronApprovals walletronApprovals;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
