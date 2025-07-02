package com.aci.smart_onboarding.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Represents the BRD template configuration document in MongoDB. Contains configuration settings
 * for BRD templates, including available industry verticals.
 */
@Document(collection = "brd_template_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrdTemplateConfig {
  @Id private String id;

  @Field("templateName")
  private String templateName;

  @Field("templateTypes")
  private String templateTypes;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;

  private String summary;
  private boolean clientInformation;
  private boolean aciInformation;
  private boolean paymentChannels;
  private boolean fundingMethods;
  private boolean achPaymentProcessing;
  private boolean miniAccountMaster;
  private boolean accountIdentifierInformation;
  private boolean paymentRules;
  private boolean notifications;
  private boolean remittance;
  private boolean agentPortal;
  private boolean recurringPayments;
  private boolean ivr;
  private boolean generalImplementations;
  private boolean approvals;
  private boolean revisionHistory;
}
