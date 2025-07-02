package com.aci.smart_onboarding.security.service;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.security.config.JwtConfig;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class JwtServiceTest {

  private JwtService jwtService;
  private JwtConfig jwtConfig;
  private final String secretKey = "thisIsATestSecretKeyThatIsLongEnoughForHS256Algorithm";
  private final long jwtExpiration = 3600000; // 1 hour

  @BeforeEach
  void setUp() {
    jwtConfig = new JwtConfig();
    jwtConfig.setSecret(secretKey);
    jwtConfig.setExpiration(jwtExpiration);
    jwtConfig.setIssuer("test-issuer");
    jwtConfig.setAudience("test-audience");
    jwtService = new JwtService(jwtConfig);
  }

  @Test
  void generateToken_WhenValidInput_ShouldCreateValidToken() {
    // Given
    String username = "testUser";
    String role = "ROLE_USER";
    List<String> roles = List.of(role);
    String jti = "testJti";

    // When
    String token = jwtService.generateToken(username, roles, role, jti);

    // Then
    assertNotNull(token);
    StepVerifier.create(jwtService.validateToken(token))
        .expectNextMatches(
            auth -> {
              assertTrue(auth instanceof JwtAuthenticationToken);
              JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
              assertEquals(username, jwtAuth.getName());
              assertEquals(role, jwtAuth.getActiveRole());
              assertEquals(roles, jwtAuth.getRoles());
              assertEquals(jti, jwtAuth.getJti());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void validateToken_WhenExpiredToken_ShouldReturnError() {
    // Given
    String username = "testUser";
    String role = "ROLE_USER";
    List<String> roles = List.of(role);
    String jti = "testJti";

    Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
    String token =
        Jwts.builder()
            .setSubject(username)
            .claim("active_role", role)
            .claim("roles", roles)
            .claim("jti", jti)
            .setIssuedAt(new Date(System.currentTimeMillis() - 2 * jwtExpiration))
            .setExpiration(new Date(System.currentTimeMillis() - jwtExpiration))
            .setIssuer(jwtConfig.getIssuer())
            .setAudience(jwtConfig.getAudience())
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

    // When & Then
    StepVerifier.create(jwtService.validateToken(token)).expectError().verify();
  }

  @Test
  void validateToken_WhenInvalidSignature_ShouldReturnError() {
    // Given
    String invalidToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiJ0ZXN0VXNlciIsInJvbGUiOiJST0xFX1VTRVIiLCJpYXQiOjE2MjM"
            + "zNDU2Nzh9.invalidSignature";

    // When & Then
    StepVerifier.create(jwtService.validateToken(invalidToken)).expectError().verify();
  }

  @Test
  void validateToken_WhenMalformedToken_ShouldReturnError() {
    // Given
    String malformedToken = "malformed.token.here";

    // When & Then
    StepVerifier.create(jwtService.validateToken(malformedToken)).expectError().verify();
  }

  @Test
  void validateToken_WhenWrongIssuer_ShouldReturnError() {
    // Given
    String username = "testUser";
    String role = "ROLE_USER";
    List<String> roles = List.of(role);
    String jti = "testJti";
    String token = jwtService.generateToken(username, roles, role, jti);

    // Create new service with different issuer
    JwtConfig newConfig = new JwtConfig();
    newConfig.setSecret(secretKey);
    newConfig.setExpiration(jwtExpiration);
    newConfig.setIssuer("wrong-issuer");
    newConfig.setAudience(jwtConfig.getAudience());
    JwtService newService = new JwtService(newConfig);

    // When & Then
    StepVerifier.create(newService.validateToken(token)).expectError().verify();
  }

  @Test
  void validateToken_WhenWrongAudience_ShouldReturnError() {
    // Given
    String username = "testUser";
    String role = "ROLE_USER";
    List<String> roles = List.of(role);
    String jti = "testJti";
    String token = jwtService.generateToken(username, roles, role, jti);

    // Create new service with different audience
    JwtConfig newConfig = new JwtConfig();
    newConfig.setSecret(secretKey);
    newConfig.setExpiration(jwtExpiration);
    newConfig.setIssuer(jwtConfig.getIssuer());
    newConfig.setAudience("wrong-audience");
    JwtService newService = new JwtService(newConfig);

    // When & Then
    StepVerifier.create(newService.validateToken(token)).expectError().verify();
  }
}
