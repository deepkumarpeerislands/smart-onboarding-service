package com.aci.smart_onboarding.security.service;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoginAuditServiceTest {
  private LoginAuditService auditService;
  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    auditService = new LoginAuditService();

    // Set up logger capture
    logger = (Logger) LoggerFactory.getLogger(LoginAuditService.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @Test
  void logLoginAttempt_WithSuccessfulLogin_ShouldLogInfoMessage() {
    // Given
    String username = "testuser";
    String ipAddress = "127.0.0.1";

    // When
    auditService.logLoginAttempt(username, ipAddress, true, "Authentication successful");

    // Then
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertEquals(Level.INFO, logEvent.getLevel());
    assertTrue(logEvent.getFormattedMessage().contains("Successful login attempt"));
    assertTrue(logEvent.getFormattedMessage().contains(username));
    assertTrue(logEvent.getFormattedMessage().contains(ipAddress));
  }

  @Test
  void logLoginAttempt_WithFailedLogin_ShouldLogWarningMessage() {
    // Given
    String username = "testuser";
    String ipAddress = "127.0.0.1";
    String reason = "Authentication failed";

    // When
    auditService.logLoginAttempt(username, ipAddress, false, reason);

    // Then
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertEquals(Level.WARN, logEvent.getLevel());
    String actualMessage = logEvent.getFormattedMessage();

    // Verify each part of the message
    assertTrue(
        actualMessage.contains("Failed login attempt"),
        "Message should contain 'Failed login attempt'");
    assertTrue(actualMessage.contains(username), "Message should contain username");
    assertTrue(actualMessage.contains(ipAddress), "Message should contain IP address");
    assertTrue(actualMessage.contains(reason), "Message should contain reason");
  }

  @Test
  void logSuspiciousActivity_WithMultipleFailedAttempts_ShouldLogErrorMessage() {
    // Given
    String username = "testuser";
    String ipAddress = "127.0.0.1";
    String reason = "Multiple failed attempts detected";

    // When
    auditService.logSuspiciousActivity(username, ipAddress, reason);

    // Then
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertEquals(Level.ERROR, logEvent.getLevel());
    assertTrue(logEvent.getFormattedMessage().contains("Suspicious login activity detected"));
    assertTrue(logEvent.getFormattedMessage().contains(username));
    assertTrue(logEvent.getFormattedMessage().contains(ipAddress));
    assertTrue(logEvent.getFormattedMessage().contains(reason));
  }

  @Test
  void logLoginAttempt_WithSensitiveData_ShouldMaskSensitiveInformation() {
    // Given
    String username = "testuser";
    String ipAddress = "127.0.0.1";
    String sensitiveData = "password123";

    // When
    auditService.logLoginAttempt(username, ipAddress, false, "Failed: " + sensitiveData);

    // Then
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertFalse(logEvent.getFormattedMessage().contains(sensitiveData));
    assertTrue(logEvent.getFormattedMessage().contains("Failed: *****"));
  }
}
