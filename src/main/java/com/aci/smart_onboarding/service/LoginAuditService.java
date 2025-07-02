package com.aci.smart_onboarding.service;

public interface LoginAuditService {
  void logLoginAttempt(String username, String clientIp, boolean success, String message);
}
