package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class BRDPrefillRequest {
  @NotNull private JsonNode sections;
  @NotEmpty private List<String> documentNames;
}
