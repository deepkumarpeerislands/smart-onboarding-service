package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.dto.LegacyBRDInfo;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "legacy_brds")
public class LegacyBRD {
  @Id private String id;
  private String brdId;
  private LegacyBRDInfo main;
  private List<LegacyBRDInfo> sites;
}
