package com.aci.smart_onboarding.security.config;

import com.aci.smart_onboarding.security.filter.BruteForceProtectionFilter;
import com.aci.smart_onboarding.security.filter.JwtAuthenticationFilter;
import com.aci.smart_onboarding.security.handler.AuthenticationFailureHandler;
import com.aci.smart_onboarding.security.handler.AuthenticationSuccessHandler;
import com.aci.smart_onboarding.security.service.CustomAuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@Profile("dev")
public class DevSecurityConfig extends BaseSecurityConfig {
  private final CustomAuthenticationManager customAuthenticationManager;

  public DevSecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      BruteForceProtectionFilter bruteForceProtectionFilter,
      AuthenticationSuccessHandler authenticationSuccessHandler,
      AuthenticationFailureHandler authenticationFailureHandler,
      CustomAuthenticationManager customAuthenticationManager) {
    super(
        jwtAuthenticationFilter,
        bruteForceProtectionFilter,
        authenticationSuccessHandler,
        authenticationFailureHandler);
    this.customAuthenticationManager = customAuthenticationManager;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return configureSecurity(http, customAuthenticationManager);
  }
}
