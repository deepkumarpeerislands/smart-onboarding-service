package com.aci.smart_onboarding.util.brd_form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevisionData {
  private String date;
  private String description;
  private String version;
}
