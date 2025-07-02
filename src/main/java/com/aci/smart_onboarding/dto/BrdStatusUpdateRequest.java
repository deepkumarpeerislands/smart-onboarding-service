package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrdStatusUpdateRequest {

  @NotBlank(message = "Status cannot be blank")
  @Pattern(
      regexp =
          "^(Draft|In Progress|Edit Complete|Internal Review|Reviewed|Ready for Sign-Off|Signed Off|Submit)$",
      message = "Status must be  of valid types")
  private String status;

  private String comment;
}
