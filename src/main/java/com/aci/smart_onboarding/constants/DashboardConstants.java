package com.aci.smart_onboarding.constants;

import java.util.List;

/**
 * Constants used for Dashboard functionality
 */
public final class DashboardConstants {

    // Field and property names
    public static final String BRD_ID = "brdId";
    public static final String BRD_FORM_ID = "brdFormId";
    public static final String EVENT_TIMESTAMP = "eventTimestamp";
    public static final String CREATED_AT = "createdAt";
    public static final String CREATOR = "creator";
    public static final String BRD_STATUS = "status";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";
    public static final String ENTITY_TYPE_BRD = "BRD";
    public static final String BRD_TYPE = "type";
    public static final String CREATED_BY = "createdBy";

    // Error message templates
    public static final String INVALID_PARAM = "Invalid parameter value: %s. Valid values are: %s";

    private DashboardConstants() {
        // Private constructor to prevent instantiation
    }

    // Status constants
    public static final String SUBMITTED_STATUS = "Submit";
    public static final String DRAFT_STATUS = "Draft";
    public static final String IN_PROGRESS_STATUS = "In Progress";
    public static final String EDIT_COMPLETE_STATUS = "Edit Complete";
    public static final String INTERNAL_REVIEW_STATUS = "Internal Review";
    public static final String REVIEWED_STATUS = "Reviewed";
    public static final String READY_FOR_SIGNOFF_STATUS = "Ready for Sign-Off";
    public static final String SIGNED_OFF_STATUS = "Signed Off";

    public static final List<String> ORDERED_BRD_STATUSES =
            List.of(
                    DRAFT_STATUS,
                    IN_PROGRESS_STATUS,
                    EDIT_COMPLETE_STATUS,
                    INTERNAL_REVIEW_STATUS,
                    REVIEWED_STATUS,
                    READY_FOR_SIGNOFF_STATUS,
                    SIGNED_OFF_STATUS);

    // Using ORDERED_BRD_STATUSES for open BRDs (all statuses except Submitted)
    public static final List<String> OPEN_BRD_STATUSES = ORDERED_BRD_STATUSES;

    // Scope constants
    public static final String SCOPE_ME = "me";
    public static final String SCOPE_TEAM = "team";
    public static final List<String> VALID_SCOPES = List.of(SCOPE_ME, SCOPE_TEAM);

    // BRD scope constants
    public static final String BRD_SCOPE_OPEN = "open";
    public static final String BRD_SCOPE_ALL = "all";
    public static final List<String> VALID_BRD_SCOPES = List.of(BRD_SCOPE_OPEN, BRD_SCOPE_ALL);

    // Period constants
    public static final String MONTH_PERIOD = "month";
    public static final String QUARTER_PERIOD = "quarter";
    public static final String YEAR_PERIOD = "year";
    public static final List<String> VALID_PERIODS =
            List.of(MONTH_PERIOD, QUARTER_PERIOD, YEAR_PERIOD);

    // BRD type constants
    public static final String BRD_TYPE_NEW = "NEW";
    public static final String BRD_TYPE_UPDATE = "UPDATE";
    public static final String BRD_TYPE_TRIAGE = "TRIAGE";

    // Week format constants
    public static final String WEEK_PREFIX = "Week ";
    public static final String STATUS_UPDATE_ACTION = "STATUS_UPDATE";

    // Other constants
    public static final String OTHER_VERTICAL = "Other";
    public static final String TRANSITION_ARROW = " ➔ ";
    public static final String AI_PREFILL_RATE = "aiPrefillRate";
    public static final String EMPTY_STRING = "";

    // Query operators
    public static final String EXISTS = "exists";
    public static final String NE = "ne";

    // File fields
    public static final String ORIGINAL_SSD_FILENAME = "originalSSDFileName";
    public static final String ORIGINAL_CONTRACT_FILENAME = "originalContractFileName";

    // API response constants
    public static final String ERROR = "ERROR";
    public static final String SUCCESS = "SUCCESS";
    public static final String SUCCESSFUL = "Successful";

    // Role constants
    public static final String PM_ROLE = "ROLE_PM";
    public static final String BA_ROLE = "ROLE_BA";
    public static final String MANAGER_ROLE = "ROLE_MANAGER";

    // Error messages
    public static final String PERIOD_REQUIRED_MESSAGE =
            "Period parameter is required when brdScope=all.";
    public static final String PM_ONLY_MESSAGE =
            "This endpoint is only accessible to users with PM role";
    public static final String PM_MANAGER_ONLY_MESSAGE =
            "Access denied. This operation is allowed for PMs and Managers only.";
    public static final String MANAGER_ONLY_MESSAGE =
            "This endpoint is only accessible to users with MANAGER role";
    public static final String BA_MANAGER_ONLY_MESSAGE =
            "This endpoint is only accessible to users with MANAGER or BA role";
    public static final String PM_MANAGER_ROLE =
            "User {} does not have required role (PM or Manager)";
    public static final String PM_ONLY_ROLE = "User {} is not a PM";
    public static final String ERROR_MESSAGE = "An error occurred";

    // Audit log fields
    public static final String AUDIT_NEW_VALUES = "newValues";
    public static final String AUDIT_STATUS = "status";
    public static final String AUDIT_ACTION = "action";

    // Success messages
    public static final String BRD_STATUS_SUCCESS_MESSAGE =
            "Successfully retrieved BRD counts by status";
    public static final String BRD_VERTICAL_SUCCESS_MESSAGE =
            "Successfully retrieved BRDs by industry vertical";
    public static final String ADDITIONAL_FACTORS_SUCCESS_MESSAGE =
            "Successfully retrieved additional factors statistics";
    public static final String BRD_SNAPSHOT_SUCCESS_MESSAGE =
            "Successfully retrieved BRDs snapshot metrics";
    public static final String BRD_AI_PREFILL_ACCURACY_SUCCESS_MESSAGE =
            "Successfully retrieved BRDs AI Prefill accuracy";
    public static final String BRD_STATUS_TRANSITION_TIME_SUCCESS_MESSAGE =
            "Successfully retrieved average status transition times";
    public static final String BRD_AI_PREFILL_RATE_SUCCESS_MESSAGE =
            "Successfully retrieved BRDs AI Prefill rate over time";
    public static final String BRD_UPLOAD_METRICS_SUCCESS_MESSAGE =
            "Successfully retrieved BRD upload metrics";
    public static final String UNRESOLVED_COMMENT_GROUPS_SUCCESS_MESSAGE =
            "Successfully retrieved unresolved comment groups count";
    public static final String BRD_COUNTS_BY_TYPE_SUCCESS_MESSAGE =
            "Successfully retrieved BRD counts by type";
    public static final String COMMENT_RESOLUTION_TIME_SUCCESS_MESSAGE =
            "Successfully retrieved average comment resolution time";

    // Comment status constants
    public static final String COMMENT_STATUS_RESOLVED = "Resolved";
}
