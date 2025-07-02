package com.aci.smart_onboarding.swagger;

/**
 * Constants for File API request and response examples used in Swagger documentation. Centralizes
 * all API examples to maintain consistency across documentation.
 */
public class FileRequestAndResponse {

  // ================================
  // JSON FILE UPLOAD ENDPOINT
  // ================================

  public static final String JSON_UPLOAD_ENDPOINT_DESCRIPTION =
      """
        Uploads a JSON file to blob storage and creates a template record in the database using optimized parallel execution.
        This operation requires MANAGER role access and ensures data consistency through comprehensive cleanup mechanisms.

        🚀 Performance Features:
        - **Parallel Execution**: File upload and template creation run simultaneously for optimal performance
        - **Cached Security Context**: User authentication data is cached and reused across operations
        - **Optimized Response Time**: Total time = MAX(upload_time, template_creation_time)

        🛡️ Enhanced Reliability Features:
        - **Precise Bidirectional Cleanup**: Handles all 4 operation scenarios individually:
          • Both Success → Normal response with template details
          • File Success + Template Failure → Delete uploaded blob file, return template error
          • Template Success + File Failure → Delete created template record, return file error
          • Both Failure → Return file upload error (takes precedence)
        - **Comprehensive Error Handling**: All failure scenarios result in proper cleanup of successful operations
        - **Data Consistency**: Zero tolerance for orphaned files in blob storage or orphaned templates in database
        - **Operation Result Tracking**: Each operation's success/failure state is individually tracked and handled
        - **Error Preservation**: Original error messages and types are preserved for proper exception handling

        🔐 Security & Validation:
        - **Manager Role Only**: Restricted to users with MANAGER role
        - **JSON Validation**: Strict validation of JSON file format and content structure
        - **Unique Constraints**: Template names must be unique across all templates
        - **File Size Limits**: Enforced as per system configuration

        📁 File Processing:
        - **UUID-based Naming**: Generates unique filename with UUID prefix for collision prevention
        - **No Encryption**: JSON files stored without encryption for direct accessibility
        - **Sanitized Names**: Template names are sanitized for safe filename usage
        - **Metadata Tracking**: Tracks uploaded by current authenticated user with timestamps
        - **Response Data**: Returns template details and file metadata (file URL not included in response)

        ⚡ Enhanced Operation Flow:
        1. Validate user has MANAGER role
        2. Validate JSON file format and content
        3. Execute file upload and template creation in parallel using Mono.zip()
        4. Wrap each operation to capture individual success/failure states
        5. Handle all 4 possible outcome combinations with precise cleanup logic
        6. On success: Return combined response with template details and file metadata
        7. On partial failure: Clean up successful operation and return appropriate error
        8. On complete failure: Return primary error without unnecessary cleanup

        File Requirements:
        - Must be a valid JSON file (.json extension)
        - Must contain valid JSON object or array (not plain text)
        - File size must be within system limits
        - Template name must be unique and contain only alphanumeric chars, hyphens, underscores
        """;

  // Request parameter descriptions
  public static final String JSON_FILE_PARAM_DESC =
      "JSON file to upload. Must be a valid JSON file with .json extension containing a valid JSON object or array. The file will be stored without encryption in blob storage with a UUID-prefixed filename for uniqueness. File size limits apply as per system configuration.";

  public static final String TEMPLATE_NAME_PARAM_DESC =
      "Unique template name for the JSON file. This name will be used to identify and retrieve the template in the system. Must be unique across all templates. Only alphanumeric characters, hyphens, and underscores are allowed. The name will be sanitized and used as part of the generated filename.";

  // Success Response (200)
  public static final String JSON_UPLOAD_SUCCESS_RESPONSE =
      """
        {
          "status": "200",
          "message": "JSON file uploaded and template created successfully",
          "data": {
            "templateName": "UserProfileTemplate",
            "originalFileName": "user-profile.json",
            "generatedFileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-userprofiletemplate-user-profile.json",
            "fileSize": 2048,
            "templateDetails": {
              "id": "64f7b8c9d1e2f3a4b5c6d7e8",
              "templateName": "UserProfileTemplate",
              "fileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-userprofiletemplate-user-profile.json",
              "originalFileName": "user-profile.json",
              "uploadedBy": "manager@company.com",
              "status": "InActive",
              "createdAt": "2024-01-15T10:30:45.123Z",
              "updatedAt": "2024-01-15T10:30:45.123Z"
            }
          },
          "errors": null
        }
        """;

  // Validation Error Response (400)
  public static final String JSON_UPLOAD_VALIDATION_ERROR_RESPONSE =
      """
        {
          "status": "400",
          "message": "JSON file validation failed: Invalid JSON format",
          "data": null,
          "errors": {
            "error": "Invalid JSON format - must be a valid JSON object or array"
          }
        }
        """;

  public static final String JSON_UPLOAD_INVALID_FILE_RESPONSE =
      """
        {
          "status": "400",
          "message": "JSON file validation failed: Only JSON files are allowed",
          "data": null,
          "errors": {
            "error": "Only JSON files are allowed. Found: txt"
          }
        }
        """;

  public static final String JSON_UPLOAD_EMPTY_FILE_RESPONSE =
      """
        {
          "status": "400",
          "message": "JSON file validation failed: JSON file content is empty",
          "data": null,
          "errors": {
            "error": "JSON file content is empty"
          }
        }
        """;

  // Access Denied Response (403)
  public static final String JSON_UPLOAD_ACCESS_DENIED_RESPONSE =
      """
        {
          "status": "403",
          "message": "This endpoint is only accessible to users with MANAGER role",
          "data": null,
          "errors": {
            "error": "Insufficient privileges"
          }
        }
        """;

  // Conflict Response (409) - Template name already exists
  public static final String JSON_UPLOAD_CONFLICT_RESPONSE =
      """
        {
          "status": "409",
          "message": "Template with this name already exists",
          "data": null,
          "errors": {
            "error": "Already exists"
          }
        }
        """;

  // Server Error Response (500)
  public static final String JSON_UPLOAD_SERVER_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Blob storage connection failed",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  public static final String JSON_UPLOAD_TRANSACTION_ERROR_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Template creation failed with status: 500",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  public static final String JSON_UPLOAD_BLOB_FAILURE_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Blob storage upload failed - Connection timeout",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  public static final String JSON_UPLOAD_PARALLEL_FAILURE_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Parallel operations failed, cleanup performed",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  public static final String JSON_UPLOAD_PARTIAL_FAILURE_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Template creation failed - Template name already exists, blob file cleaned up",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  public static final String JSON_UPLOAD_FILE_FAILURE_RESPONSE =
      """
        {
          "status": "500",
          "message": "Something went wrong: Blob storage upload failed - Connection timeout, template record cleaned up",
          "data": null,
          "errors": {
            "error": "File processing failed"
          }
        }
        """;

  // ================================
  // EXAMPLE JSON FILE CONTENT
  // ================================

  public static final String EXAMPLE_JSON_FILE_CONTENT =
      """
        Example JSON file content that can be uploaded:

        {
          "templateVersion": "1.0",
          "sections": [
            {
              "id": "personal_info",
              "title": "Personal Information",
              "fields": [
                {
                  "name": "firstName",
                  "type": "text",
                  "required": true,
                  "label": "First Name"
                },
                {
                  "name": "lastName",
                  "type": "text",
                  "required": true,
                  "label": "Last Name"
                },
                {
                  "name": "email",
                  "type": "email",
                  "required": true,
                  "label": "Email Address"
                }
              ]
            },
            {
              "id": "preferences",
              "title": "User Preferences",
              "fields": [
                {
                  "name": "notifications",
                  "type": "checkbox",
                  "required": false,
                  "label": "Enable Notifications"
                }
              ]
            }
          ]
        }
        """;

  // ================================
  // ADDITIONAL CONSTANTS
  // ================================

  public static final String JSON_UPLOAD_SUCCESS_NAME = "Success Response";
  public static final String JSON_UPLOAD_SUCCESS_DESC =
      "JSON file uploaded successfully and template created in database";

  public static final String JSON_UPLOAD_VALIDATION_ERROR_NAME = "Validation Error";
  public static final String JSON_UPLOAD_VALIDATION_ERROR_DESC =
      "Invalid JSON file format or content";

  public static final String JSON_UPLOAD_INVALID_FILE_NAME = "Invalid File Type";
  public static final String JSON_UPLOAD_INVALID_FILE_DESC = "File is not a valid JSON file";

  public static final String JSON_UPLOAD_EMPTY_FILE_NAME = "Empty File Error";
  public static final String JSON_UPLOAD_EMPTY_FILE_DESC = "Uploaded file is empty";

  public static final String JSON_UPLOAD_ACCESS_DENIED_NAME = "Access Denied";
  public static final String JSON_UPLOAD_ACCESS_DENIED_DESC = "User does not have MANAGER role";

  public static final String JSON_UPLOAD_CONFLICT_NAME = "Template Already Exists";
  public static final String JSON_UPLOAD_CONFLICT_DESC =
      "A template with the same name already exists";

  public static final String JSON_UPLOAD_SERVER_ERROR_NAME = "Server Error";
  public static final String JSON_UPLOAD_SERVER_ERROR_DESC =
      "Internal server error during processing";

  public static final String JSON_UPLOAD_TRANSACTION_ERROR_NAME = "Transaction Failed";
  public static final String JSON_UPLOAD_TRANSACTION_ERROR_DESC =
      "Template creation failed after file upload";

  public static final String JSON_UPLOAD_BLOB_FAILURE_NAME = "Blob Storage Failure";
  public static final String JSON_UPLOAD_BLOB_FAILURE_DESC = "File upload to blob storage failed";

  public static final String JSON_UPLOAD_PARALLEL_FAILURE_NAME = "Parallel Operation Failure";
  public static final String JSON_UPLOAD_PARALLEL_FAILURE_DESC =
      "One or both parallel operations failed, cleanup performed";

  public static final String JSON_UPLOAD_PARTIAL_FAILURE_NAME = "Partial Failure with Cleanup";
  public static final String JSON_UPLOAD_PARTIAL_FAILURE_DESC =
      "One operation succeeded but the other failed, automatic cleanup performed";

  public static final String JSON_UPLOAD_FILE_FAILURE_NAME =
      "File Upload Failure with Template Cleanup";
  public static final String JSON_UPLOAD_FILE_FAILURE_DESC =
      "File upload failed after template creation, template record cleaned up";

  private FileRequestAndResponse() {
    // Private constructor to prevent instantiation
  }
}
