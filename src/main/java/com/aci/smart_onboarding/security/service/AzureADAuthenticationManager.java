package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.security.config.AzureADConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Azure AD Authentication Manager implementation. Validates JWT tokens and extracts roles for
 * authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureADAuthenticationManager implements ReactiveAuthenticationManager {

  private final AzureADConfig azureADConfig;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    if (authentication.getCredentials() instanceof String token && !token.isEmpty()) {
      return validateToken(token);
    }

    // Handle username/password authentication
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();

    if (username == null || password == null) {
      log.warn("Username or password is null");
      return Mono.empty();
    }

    // In production, this would validate against Azure AD
    // For testing, we'll simulate successful authentication
    if ("pm".equals(username)) {
      return Mono.just(
          new UsernamePasswordAuthenticationToken(
              username,
              password,
              Collections.singletonList(new SimpleGrantedAuthority("ROLE_PM"))));
    } else if ("ba".equals(username)) {
      return Mono.just(
          new UsernamePasswordAuthenticationToken(
              username,
              password,
              Collections.singletonList(new SimpleGrantedAuthority("ROLE_BA"))));
    } else if ("biller".equals(username)) {
      return Mono.just(
          new UsernamePasswordAuthenticationToken(
              username,
              password,
              Collections.singletonList(new SimpleGrantedAuthority("ROLE_BILLER"))));
    }

    log.warn("Invalid username: {}", username);
    return Mono.empty();
  }

  private Mono<Authentication> validateToken(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(Keys.hmacShaKeyFor(azureADConfig.getClientSecret().getBytes()))
              .build()
              .parseClaimsJws(token)
              .getBody();

      @SuppressWarnings("unchecked")
      List<String> roles = claims.get(azureADConfig.getRoleClaim(), List.class);

      if (roles == null || roles.isEmpty()) {
        log.warn("No roles found in token");
        return Mono.empty();
      }

      List<SimpleGrantedAuthority> authorities =
          roles.stream()
              .map(role -> new SimpleGrantedAuthority(azureADConfig.getRolePrefix() + role))
              .toList();

      return Mono.just(
          new UsernamePasswordAuthenticationToken(claims.getSubject(), token, authorities));

    } catch (ExpiredJwtException e) {
      log.warn("Token is expired: {}", e.getMessage());
      return Mono.empty();
    } catch (SignatureException e) {
      log.warn("Invalid token signature: {}", e.getMessage());
      return Mono.empty();
    } catch (MalformedJwtException e) {
      log.warn("Malformed token: {}", e.getMessage());
      return Mono.empty();
    } catch (Exception e) {
      log.error("Error validating token: {}", e.getMessage());
      return Mono.empty();
    }
  }
}
