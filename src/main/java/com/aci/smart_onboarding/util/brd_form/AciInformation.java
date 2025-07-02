package com.aci.smart_onboarding.util.brd_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AciInformation {
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
  @Email
  @JsonProperty("ITContactEmail")
  private String iTContactEmail;

  @Field("ITContactExtension")
  @JsonProperty("ITContactExtension")
  private String iTContactExtension;

  @JsonProperty("sectionStatus")
  private String sectionStatus;
}
