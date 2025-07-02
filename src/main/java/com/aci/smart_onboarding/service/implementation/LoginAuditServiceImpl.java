package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.service.LoginAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginAuditServiceImpl implements LoginAuditService {

  @Override
  public void logLoginAttempt(String username, String clientIp, boolean success, String message) {
    log.info(
        "Login attempt - User: {}, IP: {}, Success: {}, Message: {}",
        username,
        clientIp,
        success,
        message);
  }
}
