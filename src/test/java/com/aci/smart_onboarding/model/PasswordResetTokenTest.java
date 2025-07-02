package com.aci.smart_onboarding.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Password Reset Token Tests")
class PasswordResetTokenTest {

  @Test
  @DisplayName("Should create token with builder")
  void createToken_WithBuilder_ShouldSetAllFields() {
    // Given
    String id = "token123";
    String userId = "user123";
    String token = "reset-token";
    Instant expiryDate = Instant.now().plusSeconds(1800);
    boolean used = false;

    // When
    PasswordResetToken resetToken =
        PasswordResetToken.builder()
            .id(id)
            .userId(userId)
            .token(token)
            .expiryDate(expiryDate)
            .used(used)
            .build();

    // Then
    assertEquals(id, resetToken.getId());
    assertEquals(userId, resetToken.getUserId());
    assertEquals(token, resetToken.getToken());
    assertEquals(expiryDate, resetToken.getExpiryDate());
    assertFalse(resetToken.isUsed());
    assertNotNull(resetToken.getCreatedAt());
  }

  @Test
  @DisplayName("Should create token with default values")
  void createToken_WithDefaultValues_ShouldSetDefaults() {
    // When
    PasswordResetToken resetToken = new PasswordResetToken();

    // Then
    assertNull(resetToken.getId());
    assertNull(resetToken.getUserId());
    assertNull(resetToken.getToken());
    assertNull(resetToken.getExpiryDate());
    assertFalse(resetToken.isUsed());
    assertNotNull(resetToken.getCreatedAt());
  }

  @Test
  @DisplayName("Should create token with all args constructor")
  void createToken_WithAllArgsConstructor_ShouldSetAllFields() {
    // Given
    String id = "token123";
    String userId = "user123";
    String token = "reset-token";
    Instant expiryDate = Instant.now().plusSeconds(1800);
    boolean used = true;
    Instant createdAt = Instant.now();

    // When
    PasswordResetToken resetToken =
        new PasswordResetToken(id, userId, token, expiryDate, used, createdAt);

    // Then
    assertEquals(id, resetToken.getId());
    assertEquals(userId, resetToken.getUserId());
    assertEquals(token, resetToken.getToken());
    assertEquals(expiryDate, resetToken.getExpiryDate());
    assertTrue(resetToken.isUsed());
    assertEquals(createdAt, resetToken.getCreatedAt());
  }

  @Test
  @DisplayName("Should implement equals and hashCode")
  void token_ShouldImplementEqualsAndHashCode() {
    // Given
    Instant expiryDate = Instant.now().plusSeconds(1800);
    Instant createdAt = Instant.now();

    PasswordResetToken token1 =
        PasswordResetToken.builder()
            .id("token123")
            .userId("user123")
            .token("reset-token")
            .expiryDate(expiryDate)
            .used(false)
            .createdAt(createdAt)
            .build();

    PasswordResetToken token2 =
        PasswordResetToken.builder()
            .id("token123")
            .userId("user123")
            .token("reset-token")
            .expiryDate(expiryDate)
            .used(false)
            .createdAt(createdAt)
            .build();

    PasswordResetToken differentToken =
        PasswordResetToken.builder()
            .id("token456")
            .userId("user456")
            .token("different-token")
            .expiryDate(Instant.now().plusSeconds(1800))
            .used(true)
            .build();

    // Then
    assertEquals(token1, token2);
    assertEquals(token1.hashCode(), token2.hashCode());
    assertNotEquals(token1, differentToken);
    assertNotEquals(token1.hashCode(), differentToken.hashCode());
  }

  @Test
  @DisplayName("Should implement toString")
  void token_ShouldImplementToString() {
    // Given
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id("token123")
            .userId("user123")
            .token("reset-token")
            .expiryDate(Instant.now().plusSeconds(1800))
            .used(false)
            .build();

    // When
    String toString = token.toString();

    // Then
    assertTrue(toString.contains("token123"));
    assertTrue(toString.contains("user123"));
    assertTrue(toString.contains("reset-token"));
    assertTrue(toString.contains("false"));
  }
}
