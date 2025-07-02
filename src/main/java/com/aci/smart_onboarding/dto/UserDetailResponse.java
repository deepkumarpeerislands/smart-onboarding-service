package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for detailed user information. Contains additional fields like username (email),
 * creation and modification dates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

  private String id;

  private String firstName;

  private String lastName;

  private String username; // Email ID

  private String activeRole;

  private List<String> roles;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime dateCreated;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime dateLastModified;

  private UserStatus status;
}
