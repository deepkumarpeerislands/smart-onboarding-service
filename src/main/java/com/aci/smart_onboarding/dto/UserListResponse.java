package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {
  private List<UserInfo> pmUsers;
  private List<UserInfo> baUsers;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private String id;
    private String fullName;
    private String email;
    private String activeRole;
    private List<String> roles;
    private UserStatus status;
    private LocalDateTime createdAt;
  }
}
