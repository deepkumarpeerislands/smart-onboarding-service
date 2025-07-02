package com.aci.smart_onboarding.constants;

/**
 * Constants class for API paths. All paths should be defined here to maintain consistency and avoid
 * hardcoding.
 */
public final class ApiPaths {

  private ApiPaths() {
    // Private constructor to prevent instantiation
  }

  // Base paths
  public static final String API_BASE = "/api";
  public static final String API_VERSION = "/v1";
  public static final String API_V1_BASE = API_BASE + API_VERSION;

  // Auth endpoints
  public static final String AUTH_BASE = API_V1_BASE + "/auth";
  public static final String AUTH_LOGIN = AUTH_BASE + "/login";
  public static final String AUTH_LOGOUT = AUTH_BASE + "/logout";
  public static final String AUTH_ME = AUTH_BASE + "/me";
  public static final String AUTH_USER_INFO = AUTH_BASE + "/user-info";

  // Other endpoints can be added here in their respective sections
}
