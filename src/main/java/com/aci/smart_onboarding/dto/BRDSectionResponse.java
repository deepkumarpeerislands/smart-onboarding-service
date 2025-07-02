package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BRDSectionResponse<T> {
  private String brdFormId;
  private String brdId;
  private String sectionName;
  private T sectionData;
}
