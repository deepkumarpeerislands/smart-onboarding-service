package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.util.walletron.*;
import com.aci.smart_onboarding.util.walletron.EnrollmentUrls;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletronRequest {
  @JsonProperty("brdId")
  private String brdId;

  @JsonProperty("brdName")
  private String brdName;

  @JsonProperty("walletronEnabled")
  private Boolean walletronEnabled;

  @JsonProperty("siteConfiguration")
  private SiteConfiguration siteConfiguration;

  @JsonProperty("notificationsOptions")
  private NotificationsOptions notificationsOptions;

  @JsonProperty("aciWalletronAgentPortal")
  private ACIWalletronAgentPortal aciWalletronAgentPortal;

  @JsonProperty("aciWalletronDataExchange")
  private DataExchange aciWalletronDataExchange;

  @JsonProperty("aciWalletronEnrollmentStrategy")
  private EnrollmentStrategy aciWalletronEnrollmentStrategy;

  @JsonProperty("enrollmentUrls")
  private EnrollmentUrls enrollmentUrls;

  @JsonProperty("targetedCommunication")
  private TargetedCommunication targetedCommunication;

  @JsonProperty("aciCash")
  private AciCash aciCash;

  @JsonProperty("walletronApprovals")
  private WalletronApprovals walletronApprovals;

  @JsonProperty("walletronId")
  private String walletronId;
}
