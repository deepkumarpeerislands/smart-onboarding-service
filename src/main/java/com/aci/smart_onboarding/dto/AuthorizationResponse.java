package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for authorization response containing authorization status and message. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {
  private boolean authorized;
  private String message;
}
