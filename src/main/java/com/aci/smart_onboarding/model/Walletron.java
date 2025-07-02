package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.util.walletron.*;
import com.aci.smart_onboarding.util.walletron.EnrollmentUrls;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "walletron")
public class Walletron {
  @JsonProperty("walletronId")
  @Id
  @Indexed(unique = true)
  private String walletronId;

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

  @JsonProperty("enrollmentUrl")
  private EnrollmentUrls enrollmentUrl;

  @JsonProperty("targetedCommunication")
  private TargetedCommunication targetedCommunication;

  @JsonProperty("aciCash")
  private AciCash aciCash;

  @JsonProperty("walletronApprovals")
  private WalletronApprovals walletronApprovals;

  @JsonProperty("createdAt")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  private LocalDateTime updatedAt;
}
