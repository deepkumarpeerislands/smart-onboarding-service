package com.aci.smart_onboarding.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "password_reset_tokens")
@EqualsAndHashCode(exclude = "createdAt")
public class PasswordResetToken {

  @Id private String id;

  @Indexed private String userId;

  @Indexed(unique = true)
  private String token;

  @Indexed(expireAfterSeconds = 1800) // 30 minutes
  private Instant expiryDate;

  private boolean used;

  @Builder.Default private Instant createdAt = Instant.now();
}
