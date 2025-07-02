package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Value;

/** Projection interface for User entity to fetch only required fields */
@Value
public class UserProjection {
  String id;
  String firstName;
  String lastName;
  String email;
  String activeRole;
  List<String> roles;
  UserStatus status;
  LocalDateTime createdAt;
}
