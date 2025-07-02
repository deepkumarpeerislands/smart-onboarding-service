package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentifierInformation {
  private List<ViewValueAndSelected> accountIdentifierFormat;
  private String minimumAccountIdentifierLength;
  private String maximumAccountIdentifierLength;
  private String sample1;
  private String sample2;
  private String sample3;
  private String sample4;
  private String sample5;
  private String accountIdentifierValidation;
  private String useValidationFormat;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
