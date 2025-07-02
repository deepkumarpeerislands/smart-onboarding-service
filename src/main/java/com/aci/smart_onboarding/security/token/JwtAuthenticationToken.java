package com.aci.smart_onboarding.security.token;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
public class JwtAuthenticationToken implements Authentication {
  private final String username;
  private final String activeRole;
  private final List<String> roles;
  private final String jti;
  private final Collection<? extends GrantedAuthority> authorities;
  private boolean authenticated;

  public JwtAuthenticationToken(
      String username, String activeRole, List<String> roles, String jti) {
    this.username = username;
    this.activeRole = activeRole;
    this.roles = roles;
    this.jti = jti;
    // Use all roles as authorities, not just the active role
    this.authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    this.authenticated = true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return username;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot alter authentication status");
  }

  @Override
  public String getName() {
    return (String) getPrincipal();
  }

  @Override
  public String toString() {
    return String.format(
        "JwtAuthenticationToken{username='%s', activeRole='%s', roles=%s, jti='%s'}",
        username, activeRole, roles, jti);
  }
}
