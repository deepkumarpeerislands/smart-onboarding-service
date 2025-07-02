package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.enums.PortalTypes;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "uat_configurator")
public class UATConfigurator {

  @Id private String uatId;

  private PortalTypes type;

  private String configurationName;

  private List<String> fields;

  private String position;

  private String scenario;

  private String createdBy;

  private LocalDateTime createdAt;

  private String updatedBy;

  private LocalDateTime updatedAt;
}
