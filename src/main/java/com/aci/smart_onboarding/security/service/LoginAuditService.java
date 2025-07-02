package com.aci.smart_onboarding.security.service;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginAuditService {
  private static final Pattern SENSITIVE_DATA_PATTERN =
      Pattern.compile("(?i)(password|secret|token|key|credential)[0-9a-z]*");
  private static final String MASK = "*****";

  private String sanitizeMessage(String message) {
    if (message == null) {
      return null;
    }

    // First, check if the message contains any sensitive data
    if (SENSITIVE_DATA_PATTERN.matcher(message).find()) {
      return "Failed: " + MASK;
    }

    return message;
  }

  public void logLoginAttempt(String username, String ipAddress, boolean success, String reason) {
    String sanitizedReason = sanitizeMessage(reason);

    if (success) {
      log.info("Successful login attempt - User: {}, IP: {}", username, ipAddress);
    } else {
      log.warn(
          "Failed login attempt - User: {}, IP: {}, Reason: {}",
          username,
          ipAddress,
          sanitizedReason);
    }
  }

  public void logSuspiciousActivity(String username, String ipAddress, String reason) {
    String sanitizedReason = sanitizeMessage(reason);
    log.error(
        "Suspicious login activity detected - User: {}, IP: {}, Reason: {}",
        username,
        ipAddress,
        sanitizedReason);
  }
}
