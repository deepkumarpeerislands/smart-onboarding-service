package com.aci.smart_onboarding.security.token;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtAuthenticationTokenTest {

  @Test
  void constructor_WhenValidInput_ShouldCreateToken() {
    // Given
    String username = "testUser";
    String activeRole = "ROLE_USER";
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    String jti = "testJti";

    // When
    JwtAuthenticationToken token = new JwtAuthenticationToken(username, activeRole, roles, jti);

    // Then
    assertEquals(username, token.getName());
    assertEquals(activeRole, token.getActiveRole());
    assertEquals(roles, token.getRoles());
    assertEquals(jti, token.getJti());
    assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority(activeRole)));
  }

  @Test
  void isAuthenticated_ShouldReturnTrue() {
    // Given
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken("testUser", "ROLE_USER", roles, "testJti");

    // When & Then
    assertTrue(token.isAuthenticated());
  }

  @Test
  void setAuthenticated_ShouldThrowException() {
    // Given
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken("testUser", "ROLE_USER", roles, "testJti");

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> token.setAuthenticated(false));
  }

  @Test
  void getCredentials_ShouldReturnNull() {
    // Given
    String username = "testUser";
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken(username, "ROLE_USER", roles, "testJti");

    // When & Then
    assertNull(token.getCredentials());
  }

  @Test
  void getPrincipal_ShouldReturnUsername() {
    // Given
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken("testUser", "ROLE_USER", roles, "testJti");

    // When & Then
    assertEquals("testUser", token.getPrincipal());
  }

  @Test
  void getName_ShouldReturnUsername() {
    // Given
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken("testUser", "ROLE_USER", roles, "testJti");

    // When & Then
    assertEquals("testUser", token.getName());
  }

  @Test
  void getAuthorities_ShouldReturnAllRolesAsAuthorities() {
    // Given
    String username = "testUser";
    String activeRole = "ROLE_USER";
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    JwtAuthenticationToken token =
        new JwtAuthenticationToken(username, activeRole, roles, "testJti");

    // When & Then
    assertEquals(2, token.getAuthorities().size());
    assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  @Test
  void toString_ShouldContainRelevantInfo() {
    // Given
    String username = "testUser";
    String activeRole = "ROLE_USER";
    List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
    String jti = "testJti";
    JwtAuthenticationToken token = new JwtAuthenticationToken(username, activeRole, roles, jti);

    // When
    String toString = token.toString();

    // Then
    assertTrue(toString.contains(username));
    assertTrue(toString.contains(activeRole));
    assertTrue(toString.contains(jti));
    assertTrue(toString.contains(roles.toString()));
  }
}
