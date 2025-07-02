package com.aci.smart_onboarding.constants;

/**
 * Constants class for security-related values. Centralizes all security configuration constants to
 * maintain consistency.
 */
public final class SecurityConstants {
  public static final String ROLE_PM = "PM";
  public static final String PM_ROLE = "ROLE_PM";
  public static final String BA_ROLE = "ROLE_BA";
  public static final String ROLE_BA = "BA";
  public static final String ROLE_BILLER = "BILLER";
  public static final String ROLE_PREFIX = "ROLE_";
  public static final String ROLE_MANAGER = "MANAGER";
  public static final String ERROR = "error";
  public static final String MANAGER_ROLE = "ROLE_MANAGER";
  public static final String BILLER_ROLE = "ROLE_BILLER";

  @SuppressWarnings("java:S2386")
  public static final String[] BRD_ACCESS_ROLES = {ROLE_PM, ROLE_BA, ROLE_BILLER, ROLE_MANAGER};

  @SuppressWarnings("java:S2386")
  public static final String[] BILLER_ACCESS_ROLES = {ROLE_PM, ROLE_BA, ROLE_BILLER, ROLE_MANAGER};

  public static final String UNKNOWN_ROLE = "UNKNOWN_ROLE";
  public static final String ROLES = "roles";
  public static final String INVALID_CREDENTIALS = "Invalid credentials";
  public static final String FAILED_SESSION_CREATION = "Failed to create session";
  public static final String AUTHENTICATION_SUCCESS = "Authentication successful";
  public static final String ROLE_USER = "User";
  public static final String ROLE_OTHER = "Other";
  public static final String LOGOUT_SUCCESS = "Logout successful";


  private SecurityConstants() {
    // Private constructor to prevent instantiation
  }

  // Session attribute keys
  public static final String ATTEMPTS_KEY = "login_attempts";
  public static final String BLOCKED_UNTIL_KEY = "blocked_until";
  public static final String LAST_USERNAME_KEY = "last_username";
}
