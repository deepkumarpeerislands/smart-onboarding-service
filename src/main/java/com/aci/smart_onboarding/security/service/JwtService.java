package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.security.config.JwtConfig;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class JwtService {
  private final JwtConfig jwtConfig;
  private volatile Key key;
  private static final Logger log = LoggerFactory.getLogger(JwtService.class);
  private static final String ROLE_PREFIX = "ROLE_";
  private static final Duration KEY_CACHE_TTL = Duration.ofHours(24);

  private Key getKey() {
    Key currentKey = key;
    if (currentKey == null) {
      synchronized (this) {
        currentKey = key;
        if (currentKey == null) {
          key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
          log.debug("Generated JWT key with secret length: {}", jwtConfig.getSecret().length());
          currentKey = key;
        }
      }
    }
    return currentKey;
  }

  public String generateToken(String username, List<String> roles, String activeRole, String jti) {
    // Pre-compute role prefixes more efficiently
    List<String> prefixedRoles = new ArrayList<>(roles.size());
    for (String role : roles) {
      prefixedRoles.add(role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role);
    }

    // Ensure active role has ROLE_ prefix
    String prefixedActiveRole =
        activeRole.startsWith(ROLE_PREFIX) ? activeRole : ROLE_PREFIX + activeRole;

    Map<String, Object> claims = new HashMap<>(4); // Pre-size map for known claims
    claims.put("roles", prefixedRoles);
    claims.put("active_role", prefixedActiveRole);
    claims.put("jti", jti);
    
    String token = createToken(claims, username);
    log.debug(
        "Generated token for user: {} with roles: {} and active role: {}",
        username,
        prefixedRoles,
        prefixedActiveRole);
    return token;
  }

  private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
        .setIssuer(jwtConfig.getIssuer())
        .setAudience(jwtConfig.getAudience())
        .signWith(getKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public Mono<Authentication> validateToken(String token) {
    log.debug("Entering method: JwtService.validateToken");
    log.debug(
        "JWT Configuration - Secret length: {}, Issuer: {}, Audience: {}",
        jwtConfig.getSecret().length(),
        jwtConfig.getIssuer(),
        jwtConfig.getAudience());
    log.debug("Validating token: {}", token);

    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(getKey())
              .requireIssuer(jwtConfig.getIssuer())
              .requireAudience(jwtConfig.getAudience())
              .build()
              .parseClaimsJws(token)
              .getBody();

      String username = claims.getSubject();
      String activeRole = claims.get("active_role", String.class);
      String jti = claims.get("jti", String.class);
      @SuppressWarnings("unchecked")
      List<String> roles = claims.get("roles", List.class);

      log.debug(
          "Token claims - Username: {}, Active Role: {}, JTI: {}, Roles: {}",
          username,
          activeRole,
          jti,
          roles);

      // Ensure roles have ROLE_ prefix
      if (roles != null) {
        roles =
            roles.stream()
                .map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role)
                .toList();
      }

      // Ensure active role has ROLE_ prefix
      if (activeRole != null && !activeRole.startsWith(ROLE_PREFIX)) {
        activeRole = ROLE_PREFIX + activeRole;
      }

      log.debug(
          "Token validated successfully for user: {} with active role: {} and roles: {}",
          username,
          activeRole,
          roles);

      return Mono.just(new JwtAuthenticationToken(username, activeRole, roles, jti));
    } catch (ExpiredJwtException e) {
      log.error("Token validation failed - Token expired: {}", e.getMessage());
      return Mono.error(e);
    } catch (Exception e) {
      log.error("Token validation failed - Error: {}", e.getMessage(), e);
      return Mono.error(e);
    } finally {
      log.debug("Exiting method: JwtService.validateToken");
    }
  }
}
