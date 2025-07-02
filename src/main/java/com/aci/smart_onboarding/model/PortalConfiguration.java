package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.enums.PortalTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "portal_configurations")
public class PortalConfiguration {

  @Id private String id;

  private String brdId;
  private String url;
  private PortalTypes type;
  private String username;
  private String password;
}
