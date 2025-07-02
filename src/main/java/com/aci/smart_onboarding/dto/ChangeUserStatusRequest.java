package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserStatusRequest {
  @NotBlank(message = "User ID cannot be blank")
  private String userId;

  @NotNull(message = "New status cannot be null")
  private UserStatus newStatus;
}
