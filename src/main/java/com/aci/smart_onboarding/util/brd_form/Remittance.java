package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Remittance {
  private String remittanceInformation;
  private String cutoffTime;
  private String receiveRemittanceFile;
  private String aciId;
  private String additionalRemittanceData;
  private String additionalReversalData;
  private String transactionVolume;
  private String deliveryTime;
  private String remittanceFileName;
  private String reversalFileName;
  private String transmissionProtocol;
  private String aciSFTPAddress;
  private String clientSFTPAddress;
  private String encryptionKey;
  private String encryptionKeyLocation;

  @JsonProperty("transmissionFrequency")
  private String transmissionFrequency;

  private String observingHolidays;
  private List<Holidays> holidays;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
