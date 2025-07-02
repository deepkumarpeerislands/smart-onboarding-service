package com.aci.smart_onboarding.constants;

/** Constants used for User Management functionality */
public final class UserConstants {

  private UserConstants() {
    // Private constructor to prevent instantiation
  }

  // Role constants
  public static final String PM_ROLE = "PM";
  public static final String BA_ROLE = "BA";
  public static final String MANAGER_ROLE = "ROLE_MANAGER";

  // API response constants
  public static final String SUCCESS = "SUCCESS";
  public static final String ERROR = "ERROR";

  // Audit log actions
  public static final String ACTION_PM_REASSIGNMENT = "PM_REASSIGNMENT";

  // Error messages
  public static final String MANAGER_ONLY_MESSAGE =
      "This endpoint is only accessible to users with MANAGER role";

  // Success messages
  public static final String USERS_RETRIEVED = "Users retrieved successfully";
  public static final String USERS_SEARCHED = "Users searched successfully";
  public static final String USER_CREATED = "User created successfully";
  public static final String USER_DETAILS_RETRIEVED = "User details retrieved successfully";
}
