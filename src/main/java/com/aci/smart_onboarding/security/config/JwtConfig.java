package com.aci.smart_onboarding.security.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
  private String secret;
  private long expiration;
  private String issuer;
  private String audience;
  private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
}
