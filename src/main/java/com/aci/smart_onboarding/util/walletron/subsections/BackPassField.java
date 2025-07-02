package com.aci.smart_onboarding.util.walletron.subsections;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackPassField {
  @NotBlank(message = "Field selection status is required")
  @JsonProperty("isChecked")
  private boolean checked;

  @NotBlank(message = "Label number is required")
  private String labelNumber;

  @NotBlank(message = "Field name is required")
  private String fieldName;

  @NotBlank(message = "Data labels are required")
  private String dataLabels;

  @NotBlank(message = "Data points are required")
  private String dataPoints;

  private String comments;

  private String requiredDisplay;
}
