package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holidays {
  private String holiday;

  @JsonProperty("ebppProviderTransmitFile")
  private String ebppProviderTransmitFile;
}
