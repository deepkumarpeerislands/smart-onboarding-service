package com.aci.smart_onboarding.security.validator;

import com.aci.smart_onboarding.dto.LoginRequest;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class LoginRequestValidator {
  private static final int MAX_USERNAME_LENGTH = 100;
  private static final int MAX_PASSWORD_LENGTH = 128;
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9@._-]{3,100}$");
  private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'%;()&+]");

  public void validate(LoginRequest request) {
    if (request == null) {
      throw new BadCredentialsException("Login request cannot be null");
    }
    validateUsername(request.getUsername());
    validatePassword(request.getPassword());
  }

  private void validateUsername(String username) {
    if (username == null) {
      throw new BadCredentialsException("Username cannot be null");
    }

    if (StringUtils.isBlank(username)) {
      throw new BadCredentialsException("Username cannot be empty");
    }

    if (username.length() > MAX_USERNAME_LENGTH) {
      throw new BadCredentialsException("Username exceeds maximum length");
    }

    if (!USERNAME_PATTERN.matcher(username).matches()) {
      throw new BadCredentialsException("Invalid username format");
    }

    if (DANGEROUS_CHARS.matcher(username).find()) {
      throw new BadCredentialsException("Username contains invalid characters");
    }
  }

  public void validatePassword(String password) {
    if (password == null) {
      throw new BadCredentialsException("Password cannot be null");
    }

    if (StringUtils.isBlank(password)) {
      throw new BadCredentialsException("Password cannot be empty");
    }

    if (password.length() > MAX_PASSWORD_LENGTH) {
      throw new BadCredentialsException("Password exceeds maximum length");
    }

    if (DANGEROUS_CHARS.matcher(password).find()) {
      throw new BadCredentialsException("Password contains invalid characters");
    }
  }
}
