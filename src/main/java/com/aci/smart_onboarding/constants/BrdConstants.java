package com.aci.smart_onboarding.constants;

public class BrdConstants {

  public static final String SUCCESSFUL = "Successful";
  public static final String DRAFT = "Draft";
  public static final String BRD_FORM_ID = "brdFormId";
  public static final String BRD_ID = "brdId";
  public static final String VALIDATION_ERRORS_OCCURRED = "Validation error occurred";
  public static final String FAILURE = "failure";
  public static final String SYSTEM = "system";
  public static final String STATUS_FIELD = "status";
  public static final String STATUS_IN_PROGRESS = "In Progress";
  public static final String STATUS_EDIT_COMPLETE = "Edit Complete";
  public static final String BRD_NOT_FOUND = "BRD not found with id: ";
  public static final String STATUS_INTERNAL_REVIEW = "Internal Review";
  public static final String UAT_CONFIG_NOT_FOUND = "Configuration not found with id: ";
  public static final String UAT_TEST_CASE_NOT_FOUND = "Test case not found with id: ";

  // Source type constants
  public static final String SOURCE_TYPE_BRD = "BRD";
  public static final String SOURCE_TYPE_SITE = "SITE";
  public static final String SOURCE_TYPE_ERROR_MESSAGE =
      "Source type must be either 'BRD' or 'SITE'";
  public static final String SITE_ID_REQUIRED_ERROR =
      "Site ID is required when source type is SITE";

  // Comment status constants
  public static final String COMMENT_STATUS_PENDING = "Pending";
  public static final String COMMENT_STATUS_RESOLVED = "Resolved";

  // Success messages
  public static final String COMMENT_GROUPS_RETRIEVED_SUCCESS =
      "Field comment groups retrieved successfully";
  public static final String COMMENT_GROUP_RETRIEVED_SUCCESS =
      "Field comment group retrieved successfully";
  public static final String COMMENT_ADDED_SUCCESS = "Comment added successfully";
  public static final String COMMENT_GROUP_CREATED_SUCCESS =
      "Field comment group created/updated successfully";
  public static final String PENDING_COMMENTS_RETRIEVED_SUCCESS =
      "Pending comment groups retrieved successfully";

  // Audit log actions
  public static final String ACTION_STATUS_UPDATE = "STATUS_UPDATE";
  public static final String ACTION_CREATE = "CREATE";
  public static final String STATUS_HISTORY_RETRIEVED_SUCCESS =
      "BRD status history retrieved successfully";

  // Error messages
  public static final String FIELD_COMMENT_GROUP_NOT_FOUND = "Field comment group not found";
  public static final String COMMENT_GROUP_NOT_FOUND_PARAMETERS =
      "Comment group not found for the specified parameters";
  public static final String PARENT_COMMENT_NOT_FOUND = "Parent comment not found";
  public static final String NO_COMMENT_GROUPS_BRD = "No comment groups found for this BRD";
  public static final String NO_COMMENT_GROUPS_SITE = "No comment groups found for this Site";
  public static final String NO_COMMENT_GROUPS_SECTION = "No comment groups found for this section";
  public static final String NO_SHADOW_VALUE = "No shadow value exists for this field";
  public static final String NO_PENDING_COMMENTS = "No pending comment groups found";

  // Database field names for queries
  public static final String FIELD_SOURCE_TYPE = "sourceType";
  public static final String FIELD_SITE_ID = "siteId";
  public static final String FIELD_SECTION_NAME = "sectionName";
  public static final String FIELD_FIELD_PATH = "fieldPath";
  public static final String FIELD_ID = "_id";

  // Collection names and document fields
  public static final String COLLECTION_BRD = "brd";
  public static final String COLLECTION_SITES = "sites";
  public static final String FIELD_BRD_FORM = "brdForm";
  public static final String FIELD_MODIFIED_COUNT = "modifiedCount";
  public static final String FIELD_ENTITY_TYPE = "entityType";
  public static final String FIELD_ENTITY_ID = "entityId";
  public static final String FIELD_ACTION = "action";
  public static final String CREATOR = "creator";
  public static final String SUBMIT = "Submit";

  private BrdConstants() {}
}
