package com.aci.smart_onboarding.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user information returned in authentication responses. Contains user
 * details and authentication information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
  private String firstName;
  private String lastName;
  private String username;
  private String activeRole; // Active role with ROLE_ prefix

  @Builder.Default private List<String> roles = new ArrayList<>(); // All roles with ROLE_ prefix

  private String token;
  private String email;
}
