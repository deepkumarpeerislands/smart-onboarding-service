package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAccountMaster {
  private String mamFile;
  private String indicator;
  private String siteNameIndicator;
  private String allSites;
  private String allSitesMam;
  private String approximateNumberOfRecords;
  private String estimatedMamFileSize;

  @JsonProperty("allSitesFile")
  private String allSitesFile;

  private String refreshFileName;
  private String refreshFlagFileName;
  private String updateFileName;
  private String updateFlagFileName;
  private String thirdPartyMamFile;
  private String thirdPartyName;
  private String transmissionProtocol;
  private String aciSFTPAddress;
  private String clientSFTPAddress;
  private String encryptionKey;
  private String encryptionKeyLocation;
  private String emailContact;

  @JsonProperty("mamTransmissionRefresh")
  private String mamTransmissionRefresh;

  private List<ViewValueAndSelected> specificDaysForRefresh;
  private String timeOfDayRefresh;

  @JsonProperty("mamTransmissionUpdate")
  private String mamTransmissionUpdate;

  private List<ViewValueAndSelected> specificDaysForUpdate;
  private String timeOfDayFirstUpdate;
  private String timeOfDaySecondUpdate;
  private String timeOfDayThirdUpdate;
  private String mamFileHoliday;
  private List<MamFileHoliday> mamFileHolidayTable;
  private String mamFields;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
