package com.aci.smart_onboarding.swagger;

/**
 * Constants for JSON Template API request and response examples used in Swagger documentation.
 * Centralizes all API examples to maintain consistency across documentation.
 */
public class JsonTemplateRequestAndResponse {

  // ================================
  // GET TEMPLATE BY NAME ENDPOINT
  // ================================

  public static final String GET_TEMPLATE_ENDPOINT_DESCRIPTION =
      """
        Retrieves a JSON template by its unique template name with optimized reactive performance.
        This endpoint returns complete template metadata including file information and upload details.

        🚀 Performance Features:
        - **Reactive MongoDB Query**: Uses indexed template name for fast retrieval
        - **DTO Response Mapping**: Returns optimized JsonTemplateResponse instead of raw entity
        - **Cached Database Connection**: Leverages connection pooling for optimal performance
        - **Single Database Hit**: One query operation with early validation

        🔍 Query Optimization:
        - **Indexed Search**: Template name field uses unique MongoDB index for O(1) lookup
        - **Selective Field Projection**: Returns only necessary fields in response DTO
        - **Memory Efficient**: Uses reactive streams for minimal memory footprint

        🛡️ Error Handling:
        - **Input Validation**: Template name validation before database query
        - **Not Found Handling**: Graceful handling of non-existent templates
        - **Exception Mapping**: Proper HTTP status codes for different error scenarios

        📊 Response Structure:
        - Returns JsonTemplateResponse DTO with complete template metadata
        - Includes creation and update timestamps
        - Contains file information and upload details
        - Shows current template status (Active/InActive)

        Path Parameter Requirements:
        - Template name must not be null or empty
        - Template name is case-sensitive
        - Must match existing template name exactly
        """;

  // Path parameter description
  public static final String TEMPLATE_NAME_PATH_PARAM_DESC =
      "Unique template name to retrieve. Must be an exact match (case-sensitive) of an existing template name in the system. Template names are indexed for fast retrieval.";

  // Success Response (200)
  public static final String GET_TEMPLATE_SUCCESS_RESPONSE =
      """
        {
          "status": "200",
          "message": "Template details retrieved successfully",
          "data": {
            "id": "64f7b8c9d1e2f3a4b5c6d7e8",
            "templateName": "UserOnboardingTemplate",
            "fileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-useronboardingtemplate-config.json",
            "originalFileName": "config.json",
            "uploadedBy": "manager@company.com",
            "status": "Active",
            "createdAt": "2024-01-15 10:30:00",
            "updatedAt": "2024-01-20 14:45:30"
          },
          "errors": null
        }
        """;

  public static final String GET_TEMPLATE_INACTIVE_RESPONSE =
      """
        {
          "status": "200",
          "message": "Template details retrieved successfully",
          "data": {
            "id": "64a8c9b1d2e3f4a5b6c7d8e9",
            "templateName": "CustomerProfileTemplate",
            "fileName": "b2c3d4e5-f6g7-8901-bcde-fg2345678901-customerprofiletemplate-profile.json",
            "originalFileName": "profile.json",
            "uploadedBy": "admin@company.com",
            "status": "InActive",
            "createdAt": "2024-01-10 09:15:22",
            "updatedAt": "2024-01-10 09:15:22"
          },
          "errors": null
        }
        """;

  // Validation Error Response (400)
  public static final String GET_TEMPLATE_VALIDATION_ERROR_RESPONSE =
      """
        {
          "status": "400",
          "message": "Template name cannot be null or empty",
          "data": null,
          "errors": {
            "error": "Invalid input parameters"
          }
        }
        """;

  public static final String GET_TEMPLATE_EMPTY_NAME_RESPONSE =
      """
        {
          "status": "400",
          "message": "Template name cannot be null or empty",
          "data": null,
          "errors": {
            "error": "Invalid input parameters"
          }
        }
        """;

  // Unauthorized Error Response (401)
  public static final String GET_TEMPLATE_UNAUTHORIZED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "Unauthorized: Missing or invalid authentication token",
          "data": null,
          "errors": {
            "error": "Full authentication is required to access this resource"
          }
        }
        """;

  // Access Denied Error Response (403)
  public static final String GET_TEMPLATE_ACCESS_DENIED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "This endpoint is only accessible to users with MANAGER role",
          "data": null,
          "errors": {
            "error": "Access denied - MANAGER role required"
          }
        }
        """;

  // Not Found Response (404)
  public static final String GET_TEMPLATE_NOT_FOUND_RESPONSE =
      """
        {
          "status": "404",
          "message": "Template not found: NonExistentTemplate",
          "data": null,
          "errors": {
            "error": "Template not found"
          }
        }
        """;

  // Server Error Response (500)
  public static final String GET_TEMPLATE_SERVER_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Database connection failed",
          "data": null,
          "errors": {
            "error": "Internal server error"
          }
        }
        """;

  public static final String GET_TEMPLATE_DATABASE_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Failed to retrieve template from database",
          "data": null,
          "errors": {
            "error": "Internal server error"
          }
        }
        """;

  // ================================
  // RESPONSE EXAMPLE CONSTANTS
  // ================================

  public static final String GET_TEMPLATE_SUCCESS_NAME = "Success Response";
  public static final String GET_TEMPLATE_SUCCESS_DESC =
      "Template found and details retrieved successfully";

  public static final String GET_TEMPLATE_INACTIVE_NAME = "Success Response - Inactive Template";
  public static final String GET_TEMPLATE_INACTIVE_DESC =
      "Inactive template found and details retrieved successfully";

  public static final String GET_TEMPLATE_VALIDATION_ERROR_NAME = "Validation Error";
  public static final String GET_TEMPLATE_VALIDATION_ERROR_DESC = "Invalid template name provided";

  public static final String GET_TEMPLATE_EMPTY_NAME_NAME = "Empty Template Name";
  public static final String GET_TEMPLATE_EMPTY_NAME_DESC = "Template name is null or empty";

  public static final String GET_TEMPLATE_UNAUTHORIZED_NAME = "Unauthorized Access";
  public static final String GET_TEMPLATE_UNAUTHORIZED_DESC =
      "Missing or invalid authentication token";

  public static final String GET_TEMPLATE_ACCESS_DENIED_NAME = "Access Denied";
  public static final String GET_TEMPLATE_ACCESS_DENIED_DESC = "User does not have MANAGER role";

  public static final String GET_TEMPLATE_NOT_FOUND_NAME = "Template Not Found";
  public static final String GET_TEMPLATE_NOT_FOUND_DESC =
      "No template exists with the specified name";

  public static final String GET_TEMPLATE_SERVER_ERROR_NAME = "Server Error";
  public static final String GET_TEMPLATE_SERVER_ERROR_DESC =
      "Internal server error during template retrieval";

  public static final String GET_TEMPLATE_DATABASE_ERROR_NAME = "Database Error";
  public static final String GET_TEMPLATE_DATABASE_ERROR_DESC =
      "Database operation failed during template retrieval";

  // ================================
  // EXAMPLE TEMPLATE NAMES
  // ================================

  public static final String EXAMPLE_TEMPLATE_NAME = "UserOnboardingTemplate";
  public static final String EXAMPLE_TEMPLATE_NAME_2 = "CustomerProfileTemplate";
  public static final String EXAMPLE_TEMPLATE_NAME_3 = "EmployeeDataTemplate";

  // ================================
  // GET ALL TEMPLATES ENDPOINT
  // ================================

  public static final String GET_ALL_TEMPLATES_ENDPOINT_DESCRIPTION =
      """
        Retrieves all JSON templates with their complete details using optimized reactive performance.
        This endpoint returns a list of all template metadata including file information and upload details.

        🚀 Performance Features:
        - **Reactive MongoDB Query**: Uses findAll() with reactive streams for efficient data retrieval
        - **DTO Response Mapping**: Returns optimized List<JsonTemplateResponse> instead of raw entities
        - **Memory Efficient**: Uses reactive streams to minimize memory footprint during list building
        - **Single Database Hit**: One query operation to retrieve all templates

        🔍 Query Optimization:
        - **Efficient Collection Scan**: MongoDB findAll() optimized for full collection retrieval
        - **Selective Field Projection**: Returns only necessary fields in response DTOs
        - **Reactive Stream Processing**: Templates are mapped to DTOs in a reactive pipeline

        🛡️ Error Handling:
        - **Comprehensive Exception Mapping**: Proper HTTP status codes for different error scenarios
        - **Empty Result Handling**: Graceful handling when no templates exist (returns empty list)
        - **Database Error Recovery**: Proper error responses for database connectivity issues

        📊 Response Structure:
        - Returns List<JsonTemplateResponse> with complete template metadata for each template
        - Includes creation and update timestamps for all templates
        - Contains file information and upload details for each template
        - Shows current template status (Active/InActive) for each template

        🔒 Access Control:
        - Accessible to all authenticated users
        - Full authentication validation before data retrieval
        """;

  // Success Responses (200)
  public static final String GET_ALL_TEMPLATES_SUCCESS_RESPONSE =
      """
        {
          "status": "200",
          "message": "Found 3 templates",
          "data": [
            {
              "id": "64f7b8c9d1e2f3a4b5c6d7e8",
              "templateName": "UserOnboardingTemplate",
              "fileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-useronboardingtemplate-config.json",
              "originalFileName": "config.json",
              "uploadedBy": "manager@company.com",
              "status": "Active",
              "createdAt": "2024-01-15 10:30:00",
              "updatedAt": "2024-01-20 14:45:30"
            },
            {
              "id": "64a8c9b1d2e3f4a5b6c7d8e9",
              "templateName": "CustomerProfileTemplate",
              "fileName": "b2c3d4e5-f6g7-8901-bcde-fg2345678901-customerprofiletemplate-profile.json",
              "originalFileName": "profile.json",
              "uploadedBy": "admin@company.com",
              "status": "InActive",
              "createdAt": "2024-01-10 09:15:22",
              "updatedAt": "2024-01-10 09:15:22"
            },
            {
              "id": "64b9d0c2e3f4g5h6i7j8k9l0",
              "templateName": "EmployeeDataTemplate",
              "fileName": "c3d4e5f6-g7h8-9012-cdef-gh3456789012-employeedatatemplate-employee.json",
              "originalFileName": "employee.json",
              "uploadedBy": "hr.manager@company.com",
              "status": "Active",
              "createdAt": "2024-01-05 08:45:10",
              "updatedAt": "2024-01-18 16:20:45"
            }
          ],
          "errors": null
        }
        """;

  public static final String GET_ALL_TEMPLATES_EMPTY_RESPONSE =
      """
        {
          "status": "200",
          "message": "No templates found",
          "data": [],
          "errors": null
        }
        """;

  // Unauthorized Error Response (401)
  public static final String GET_ALL_TEMPLATES_UNAUTHORIZED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "Unauthorized: Missing or invalid authentication token",
          "data": null,
          "errors": {
            "error": "Full authentication is required to access this resource"
          }
        }
        """;

  // Access Denied Error Response (403)
  public static final String GET_ALL_TEMPLATES_ACCESS_DENIED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "This endpoint is only accessible to users with MANAGER role",
          "data": null,
          "errors": {
            "error": "Access denied - MANAGER role required"
          }
        }
        """;

  // Server Error Response (500)
  public static final String GET_ALL_TEMPLATES_SERVER_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Database connection failed",
          "data": null,
          "errors": {
            "error": "Internal server error"
          }
        }
        """;

  public static final String GET_ALL_TEMPLATES_DATABASE_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Failed to retrieve templates from database",
          "data": null,
          "errors": {
            "error": "Internal server error"
          }
        }
        """;

  // ================================
  // GET ALL TEMPLATES RESPONSE CONSTANTS
  // ================================

  public static final String GET_ALL_TEMPLATES_SUCCESS_NAME = "Success Response";
  public static final String GET_ALL_TEMPLATES_SUCCESS_DESC =
      "All templates retrieved successfully";

  public static final String GET_ALL_TEMPLATES_EMPTY_NAME = "Empty Result";
  public static final String GET_ALL_TEMPLATES_EMPTY_DESC = "No templates found in the system";

  public static final String GET_ALL_TEMPLATES_UNAUTHORIZED_NAME = "Unauthorized Access";
  public static final String GET_ALL_TEMPLATES_UNAUTHORIZED_DESC =
      "Missing or invalid authentication token";

  public static final String GET_ALL_TEMPLATES_ACCESS_DENIED_NAME = "Access Denied";
  public static final String GET_ALL_TEMPLATES_ACCESS_DENIED_DESC =
      "User does not have MANAGER role";

  public static final String GET_ALL_TEMPLATES_SERVER_ERROR_NAME = "Server Error";
  public static final String GET_ALL_TEMPLATES_SERVER_ERROR_DESC =
      "Internal server error during templates retrieval";

  public static final String GET_ALL_TEMPLATES_DATABASE_ERROR_NAME = "Database Error";
  public static final String GET_ALL_TEMPLATES_DATABASE_ERROR_DESC =
      "Database operation failed during templates retrieval";

  // ================================
  // UPDATE TEMPLATE STATUS ENDPOINT
  // ================================

  public static final String UPDATE_STATUS_ENDPOINT_DESCRIPTION =
      """
        Updates a JSON template status using optimized reactive operations with single active template constraint.
        This endpoint ensures data consistency by allowing only one template to be 'Active' at any time.

        🚀 Performance Features:
        - **Atomic Operations**: Uses MongoDB bulk operations for consistency
        - **Indexed Queries**: Leverages templateName index for O(1) lookup performance
        - **Reactive Streams**: Non-blocking operations with reactive MongoDB
        - **Minimal Response**: Returns only boolean success indicator for reduced payload

        ⚡ Single Active Constraint:
        - **Automatic Deactivation**: When setting a template to 'Active', all other templates are automatically set to 'InActive'
        - **Data Consistency**: Ensures only one template can be active at any time across the system
        - **Atomic Updates**: All status changes happen atomically to prevent race conditions
        - **Optimized for Active**: InActive updates use simple single-document operations

        🔍 Operation Flow:
        - **For InActive Status**: Simple findAndModify operation (single database hit)
        - **For Active Status**: Multi-step atomic operation:
          1. Verify target template exists
          2. Deactivate all currently active templates (bulk update)
          3. Activate the target template (findAndModify)

        🛡️ Jakarta Validation (Controller Level):
        - **@NotBlank**: Validates templateName and status are not null/empty
        - **@Pattern**: Enforces status values to exactly "Active" or "InActive" using regex
        - **Early Validation**: Input validation happens before service layer for faster failure
        - **Standardized Errors**: Consistent validation error responses via Spring Boot

        📊 Response Structure:
        - Returns Boolean true for successful update
        - Enhanced success message indicating single active constraint
        - Standard Api wrapper for consistency
        - Clear success/error messaging

        Request Requirements:
        - Template name must not be null or empty
        - Status must be exactly "Active" or "InActive" (validated by regex pattern)
        - Template must exist in the database
        """;

  // Path and request parameter descriptions
  public static final String UPDATE_STATUS_TEMPLATE_NAME_PARAM_DESC =
      "Unique template name to update. Must match an existing template name exactly (case-sensitive).";

  public static final String UPDATE_STATUS_STATUS_PARAM_DESC =
      "New status value for the template. Must be exactly 'Active' or 'InActive' (case-sensitive).";

  // Success Response (200)
  public static final String UPDATE_STATUS_SUCCESS_RESPONSE =
      """
        {
          "status": "200",
          "message": "Template status updated successfully. Only one template is now active.",
          "data": true,
          "errors": null
        }
        """;

  // Validation Error Responses (400)
  public static final String UPDATE_STATUS_VALIDATION_ERROR_RESPONSE =
      """
        {
          "status": "400",
          "message": "Template name cannot be null or empty",
          "data": null,
          "errors": {
            "error": "Invalid input parameters"
          }
        }
        """;

  public static final String UPDATE_STATUS_INVALID_STATUS_RESPONSE =
      """
        {
          "status": "400",
          "message": "Status must be either 'Active' or 'InActive'",
          "data": null,
          "errors": {
            "error": "Invalid input parameters"
          }
        }
        """;

  public static final String UPDATE_STATUS_EMPTY_STATUS_RESPONSE =
      """
        {
          "status": "400",
          "message": "Status cannot be null or empty",
          "data": null,
          "errors": {
            "error": "Invalid input parameters"
          }
        }
        """;

  // Unauthorized Error Response (401)
  public static final String UPDATE_STATUS_UNAUTHORIZED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "Unauthorized: Missing or invalid authentication token",
          "data": null,
          "errors": {
            "error": "Full authentication is required to access this resource"
          }
        }
        """;

  // Access Denied Error Response (403)
  public static final String UPDATE_STATUS_ACCESS_DENIED_RESPONSE =
      """
        {
          "status": "failure",
          "message": "This endpoint is only accessible to users with MANAGER role",
          "data": null,
          "errors": {
            "error": "Access denied - MANAGER role required"
          }
        }
        """;

  // Not Found Response (404)
  public static final String UPDATE_STATUS_NOT_FOUND_RESPONSE =
      """
        {
          "status": "404",
          "message": "Template not found: NonExistentTemplate",
          "data": null,
          "errors": {
            "error": "Template not found"
          }
        }
        """;

  // Server Error Response (500)
  public static final String UPDATE_STATUS_SERVER_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Database operation failed",
          "data": null,
          "errors": {
            "error": "Internal server error"
          }
        }
        """;

  // ================================
  // UPDATE STATUS RESPONSE CONSTANTS
  // ================================

  public static final String UPDATE_STATUS_SUCCESS_NAME = "Success Response";
  public static final String UPDATE_STATUS_SUCCESS_DESC = "Template status updated successfully";

  public static final String UPDATE_STATUS_VALIDATION_ERROR_NAME = "Validation Error";
  public static final String UPDATE_STATUS_VALIDATION_ERROR_DESC = "Invalid template name provided";

  public static final String UPDATE_STATUS_INVALID_STATUS_NAME = "Invalid Status Value";
  public static final String UPDATE_STATUS_INVALID_STATUS_DESC =
      "Status value is not Active or InActive";

  public static final String UPDATE_STATUS_EMPTY_STATUS_NAME = "Empty Status Value";
  public static final String UPDATE_STATUS_EMPTY_STATUS_DESC = "Status value is null or empty";

  public static final String UPDATE_STATUS_UNAUTHORIZED_NAME = "Unauthorized Access";
  public static final String UPDATE_STATUS_UNAUTHORIZED_DESC =
      "Missing or invalid authentication token";

  public static final String UPDATE_STATUS_ACCESS_DENIED_NAME = "Access Denied";
  public static final String UPDATE_STATUS_ACCESS_DENIED_DESC = "User does not have MANAGER role";

  public static final String UPDATE_STATUS_NOT_FOUND_NAME = "Template Not Found";
  public static final String UPDATE_STATUS_NOT_FOUND_DESC =
      "No template exists with the specified name";

  public static final String UPDATE_STATUS_SERVER_ERROR_NAME = "Server Error";
  public static final String UPDATE_STATUS_SERVER_ERROR_DESC =
      "Internal server error during status update";

  // ================================
  // EXAMPLE STATUS VALUES
  // ================================

  public static final String EXAMPLE_STATUS_ACTIVE = "Active";
  public static final String EXAMPLE_STATUS_INACTIVE = "InActive";

  private JsonTemplateRequestAndResponse() {
    // Private constructor to prevent instantiation
  }
}
