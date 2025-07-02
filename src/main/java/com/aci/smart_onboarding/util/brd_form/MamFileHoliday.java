package com.aci.smart_onboarding.util.brd_form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MamFileHoliday {
  private String holiday;
  private String mamUpdateFile;
  private String nextProcessingDayUpdate;
  private String mamRefreshFile;
  private String nextProcessingDayRefresh;
}
