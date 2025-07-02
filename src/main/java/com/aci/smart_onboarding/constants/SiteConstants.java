package com.aci.smart_onboarding.constants;

public class SiteConstants {
  public static final String SUCCESSFUL = "SUCCESS";
  public static final String SYSTEM = "SYSTEM";
  public static final String DIVISION_ID = "divisionId";
  public static final String TIMESTAMP = "timestamp";
  public static final String ERROR_MESSAGE = "errorMessage";
  public static final String SITE_ID = "siteId";
  public static final String BRD_ID = "brdId";

  // Error Messages
  public static final String BRD_NOT_FOUND = "BRD not found with ID: ";
  public static final String BRD_STATUS_NOT_FOUND = "BRD status not found: ";
  public static final String SITE_NOT_FOUND = "Site not found with ID: %s";
  public static final String SITE_ALREADY_EXISTS = "Site already exists for this BRD";
  public static final String DUPLICATE_IDENTIFIER_CODE = "Duplicate identifier code found: %s";
  public static final String IDENTIFIER_CODE_EXISTS = "Identifier code already exists: %s";
  public static final String REQUEST_NULL = "request cannot be null or empty";
  public static final String SITE_ID_NULL = "siteId cannot be null or empty";
  public static final String NOT_NULL = "cannot be null or empty";
  public static final String SITE_DETAILS_NULL = "siteDetails cannot be null";
  public static final String BRD_ALREADY_EXISTS = "Brd already exist for given brdId";
  public static final String BRD_COMPARISON_FAILED = "Failed to compare BRD and Site BRD form";
  public static final String FAILURE = "Failure";

  // Bulk Site Operations
  public static final String BULK_CLONE_NAME_FORMAT = "Bulk Clone %d";
  public static final String BULK_CLONE_IDENTIFIER_FORMAT = "BC%03d";
  public static final String MAX_BULK_SITES_EXCEEDED =
      "Maximum of 100 sites can be created in a single bulk operation";
  public static final String BULK_SITES_CREATED = "Successfully created %d sites";
  public static final int MAX_BULK_SITES = 100;

  // Site Differences Messages
  public static final String SITE_DIFFERENCES_RETRIEVED = "Site differences retrieved successfully";
  public static final String BRD_ID_EMPTY = "BRD ID cannot be empty";
  public static final String NO_DIFFERENCES_FOUND = "No differences found between BRD and sites";
  public static final String SITE_BRD_FORM_NULL = "Site BRD form is null for site: %s";
  public static final String SECTION_COMPARISON_FAILED = "Failed to compare section: %s";
  public static final Object VIEW_VALUE = "viewValue";
  public static final Object SELECTED = "selected";

  private SiteConstants() {}
}
