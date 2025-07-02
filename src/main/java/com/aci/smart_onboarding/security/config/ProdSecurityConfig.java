package com.aci.smart_onboarding.security.config;

import com.aci.smart_onboarding.security.filter.BruteForceProtectionFilter;
import com.aci.smart_onboarding.security.filter.JwtAuthenticationFilter;
import com.aci.smart_onboarding.security.handler.AuthenticationFailureHandler;
import com.aci.smart_onboarding.security.handler.AuthenticationSuccessHandler;
import com.aci.smart_onboarding.security.service.AzureADAuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@Profile("prod")
public class ProdSecurityConfig extends BaseSecurityConfig {
  private final AzureADAuthenticationManager azureADAuthenticationManager;

  public ProdSecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      BruteForceProtectionFilter bruteForceProtectionFilter,
      AuthenticationSuccessHandler authenticationSuccessHandler,
      AuthenticationFailureHandler authenticationFailureHandler,
      AzureADAuthenticationManager azureADAuthenticationManager) {
    super(
        jwtAuthenticationFilter,
        bruteForceProtectionFilter,
        authenticationSuccessHandler,
        authenticationFailureHandler);
    this.azureADAuthenticationManager = azureADAuthenticationManager;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return configureSecurity(http, azureADAuthenticationManager);
  }
}
