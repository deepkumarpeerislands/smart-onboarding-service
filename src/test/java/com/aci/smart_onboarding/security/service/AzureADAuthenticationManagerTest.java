package com.aci.smart_onboarding.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.security.config.AzureADConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("AzureADAuthenticationManagerTest")
class AzureADAuthenticationManagerTest {

  @Mock private AzureADConfig azureADConfig;

  private AzureADAuthenticationManager authenticationManager;
  private final String clientSecret = "testClientSecret123456789012345678901234567890";
  private final String roleClaim = "roles";
  private final String rolePrefix = "ROLE_";

  @BeforeEach
  void setUp() {
    authenticationManager = new AzureADAuthenticationManager(azureADConfig);
  }

  @Test
  @DisplayName("Should authenticate with valid token and PM role")
  void authenticate_WithValidTokenAndPmRole_ShouldReturnAuthentication() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);
    when(azureADConfig.getRolePrefix()).thenReturn(rolePrefix);

    String username = "testuser";
    List<String> roles = List.of("PM");
    String token = createJwtToken(username, roles);
    Authentication auth = new UsernamePasswordAuthenticationToken(username, token);

    // When
    Mono<Authentication> result = authenticationManager.authenticate(auth);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            authentication -> {
              return authentication.getName().equals(username)
                  && authentication.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_PM"));
            })
        .verifyComplete();
  }

  @Test
  void authenticate_WithValidTokenAndMultipleRoles_ShouldReturnAuthentication() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);
    when(azureADConfig.getRolePrefix()).thenReturn(rolePrefix);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN", "USER");
    String token = createJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result)
        .assertNext(
            auth -> {
              assertEquals(username, auth.getPrincipal());
              assertEquals(token, auth.getCredentials());
              assertEquals(2, auth.getAuthorities().size());
              assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
              assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
            })
        .verifyComplete();
  }

  @Test
  void authenticate_WithInvalidToken_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);

    String invalidToken = "invalid.token.here";
    Authentication authentication = new UsernamePasswordAuthenticationToken("user", invalidToken);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithTokenHavingNoRoles_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);

    String username = "testuser@example.com";
    String token = createJwtToken(username, List.of());

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithTokenHavingNullRoles_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);

    String username = "testuser@example.com";
    String token = createJwtToken(username, null);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithExpiredToken_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN");
    String token = createExpiredJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithNullCredentials_ShouldReturnEmpty() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("user", "");

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithInvalidClientSecret_ShouldReturnEmpty() {
    // Given
    String wrongClientSecret = "wrongClientSecret123456789012345678901234567890";
    when(azureADConfig.getClientSecret()).thenReturn(wrongClientSecret);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN");
    String token = createJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithMalformedToken_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);

    String malformedToken = "header.payload"; // Missing signature part
    Authentication authentication = new UsernamePasswordAuthenticationToken("user", malformedToken);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithFutureToken_ShouldReturnEmpty() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN");
    String token = createFutureJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void authenticate_WithSpecialCharacterRoles_ShouldReturnAuthentication() {
    // Given
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);
    when(azureADConfig.getRolePrefix()).thenReturn(rolePrefix);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN-123", "USER_TEST@DOMAIN");
    String token = createJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result)
        .assertNext(
            auth -> {
              assertEquals(username, auth.getPrincipal());
              assertEquals(token, auth.getCredentials());
              assertEquals(2, auth.getAuthorities().size());
              assertTrue(
                  auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN-123")));
              assertTrue(
                  auth.getAuthorities()
                      .contains(new SimpleGrantedAuthority("ROLE_USER_TEST@DOMAIN")));
            })
        .verifyComplete();
  }

  @Test
  void authenticate_WithCustomRolePrefix_ShouldReturnAuthentication() {
    // Given
    String customPrefix = "CUSTOM_PREFIX_";
    when(azureADConfig.getClientSecret()).thenReturn(clientSecret);
    when(azureADConfig.getRoleClaim()).thenReturn(roleClaim);
    when(azureADConfig.getRolePrefix()).thenReturn(customPrefix);

    String username = "testuser@example.com";
    List<String> roles = List.of("ADMIN", "USER");
    String token = createJwtToken(username, roles);

    Authentication authentication = new UsernamePasswordAuthenticationToken(username, token);

    // When
    var result = authenticationManager.authenticate(authentication);

    // Then
    StepVerifier.create(result)
        .assertNext(
            auth -> {
              assertEquals(username, auth.getPrincipal());
              assertEquals(token, auth.getCredentials());
              assertEquals(2, auth.getAuthorities().size());
              assertTrue(
                  auth.getAuthorities()
                      .contains(new SimpleGrantedAuthority("CUSTOM_PREFIX_ADMIN")));
              assertTrue(
                  auth.getAuthorities().contains(new SimpleGrantedAuthority("CUSTOM_PREFIX_USER")));
            })
        .verifyComplete();
  }

  private String createJwtToken(String username, List<String> roles) {
    return Jwts.builder()
        .setSubject(username)
        .claim(roleClaim, roles)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
        .signWith(Keys.hmacShaKeyFor(clientSecret.getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }

  private String createExpiredJwtToken(String username, List<String> roles) {
    return Jwts.builder()
        .setSubject(username)
        .claim(roleClaim, roles)
        .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
        .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
        .signWith(Keys.hmacShaKeyFor(clientSecret.getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }

  private String createFutureJwtToken(String username, List<String> roles) {
    return Jwts.builder()
        .setSubject(username)
        .claim(roleClaim, roles)
        .setIssuedAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour in future
        .setExpiration(new Date(System.currentTimeMillis() + 7200000)) // 2 hours in future
        .signWith(Keys.hmacShaKeyFor(clientSecret.getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }
}
