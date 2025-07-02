package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

  @NotBlank(message = "First name cannot be blank")
  private String firstName;

  private String lastName;

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Invalid email format")
  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "Invalid email format")
  private String email;

  @NotBlank(message = "Role cannot be blank")
  private String activeRole;

  @Builder.Default
  private List<String> roles = new ArrayList<>(); // All roles (with or without ROLE_ prefix)

  private UserStatus status;
}
