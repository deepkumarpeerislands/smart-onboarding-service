package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInformation {

  private String companyName;
  private String customerId;
  private String companyDisplayName;
  private String siteName;
  private String siteDisplayName;
  private String siteAbridgedName;
  private String healthCareIndustry;
  private String addressLine1;
  private String addressLine2;
  private String companyWebsiteAddress;
  private String customerServicePhoneNumber;
  private String customerServicePhoneExtension;

  @Email(message = "Invalid email address")
  private String customerServiceEmailAddress;

  @JsonProperty("customerServiceHours")
  private String customerServiceHours;

  private String clientPhoneNumber;
  private String clientPhoneExtension;
  private String city;
  private String state;
  private String zipCode;
  private String primaryBusinessContactName;
  private String primaryBusinessContactTitle;
  private String primaryBusinessContactPhone;

  @Email(message = "Invalid email address")
  private String primaryBusinessContactEmail;

  private String primaryBusinessContactExtension;
  private String projectManagerName;
  private String projectManagerTitle;
  private String projectManagerPhone;

  @Email(message = "Invalid email address")
  private String projectManagerEmail;

  private String projectManagerExtension;
  private String operationsContactName;
  private String operationsContactTitle;
  private String operationsContactPhone;

  @Email(message = "Invalid email address")
  @JsonProperty("operationContactEmail")
  private String operationContactEmail;

  @JsonProperty("operationContactExtension")
  private String operationContactExtension;

  @Field("ITContactName")
  @JsonProperty("ITContactName")
  private String iTContactName;

  @Field("ITContactTitle")
  @JsonProperty("ITContactTitle")
  private String iTContactTitle;

  @Field("ITContactPhone")
  @JsonProperty("ITContactPhone")
  private String iTContactPhone;

  @Field("ITContactEmail")
  @Email(message = "Invalid email address")
  @JsonProperty("ITContactEmail")
  private String iTContactEmail;

  @Field("ITContactExtension")
  @JsonProperty("ITContactExtension")
  private String iTContactExtension;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
