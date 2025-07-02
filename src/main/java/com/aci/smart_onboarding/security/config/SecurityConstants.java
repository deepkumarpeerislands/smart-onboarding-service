package com.aci.smart_onboarding.security.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Centralized security constants used across the application. */
@Getter
@Component
public class SecurityConstants {
  @Value("${security.login.max-attempts}")
  private int maxAttempts;

  @Value("${security.login.block-duration-seconds}")
  private long blockDurationSeconds;

  public static final String ATTEMPTS_KEY = "login_attempts";
  public static final String BLOCKED_UNTIL_KEY = "blocked_until";
  public static final String LAST_USERNAME_KEY = "LAST_USERNAME_KEY";
}
