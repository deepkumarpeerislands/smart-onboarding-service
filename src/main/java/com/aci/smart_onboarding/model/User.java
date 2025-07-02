package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id private String id;

  private String firstName;

  private String lastName;

  @Indexed(unique = true)
  private String email;

  @Indexed
  private String activeRole;

  @Builder.Default private List<String> roles = new ArrayList<>();

  private char[] password;

  private boolean passwordChangeRequired;

  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime updatedAt;

  private UserStatus status;

  /** Override toString method to avoid exposing password in logs */
  @Override
  public String toString() {
    return "User(id="
        + id
        + ", firstName="
        + firstName
        + ", lastName="
        + lastName
        + ", email="
        + email
        + ", activeRole="
        + activeRole
        + ", roles="
        + roles
        + ", status="
        + status
        + ", password=[PROTECTED]"
        + ", createdAt="
        + createdAt
        + ", updatedAt="
        + updatedAt
        + ")";
  }
}
