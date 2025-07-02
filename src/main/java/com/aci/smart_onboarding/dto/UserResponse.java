package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

  private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String activeRole;
  private List<String> roles;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UserStatus status;
}
