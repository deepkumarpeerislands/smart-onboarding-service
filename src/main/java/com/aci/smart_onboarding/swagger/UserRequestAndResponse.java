package com.aci.smart_onboarding.swagger;

public class UserRequestAndResponse {

  private UserRequestAndResponse() {}

  // Description for user details endpoint
  public static final String USER_DETAILS_ENDPOINT_DESCRIPTION =
      """
        Retrieves detailed user information by their email address. \
        This endpoint is only accessible to users with MANAGER role.

        **Authentication Required:**
        - Bearer JWT token must be provided in the Authorization header
        - Format: `Authorization: Bearer {token}`
        - User must have the MANAGER role

        The response includes username (email), first name, last name, role, \
        creation date, and last modification date.
        """;

  // User creation request example
  public static final String USER_REQUEST_EXAMPLE =
      """
        {
            "firstName": "John",
            "lastName": "Smith",
            "email": "john.smith@example.com",
            "role": "PM"
        }
        """;

  // Success response for user creation
  public static final String USER_CREATION_SUCCESS_RESPONSE =
      """
        {
            "status": "SUCCESS",
            "message": "User created successfully with default password (firstName_role)",
            "data": {
                "id": "65f3a12b7d8c9e001a2b3c4d",
                "firstName": "John",
                "lastName": "Smith",
                "email": "john.smith@example.com",
                "role": "PM",
                "createdAt": "2024-04-10T15:30:45.123Z",
                "updatedAt": null
            },
            "errors": null
        }
        """;

  // Email already exists error response
  public static final String EMAIL_ALREADY_EXISTS_ERROR =
      """
        {
            "status": "ERROR",
            "message": "User with email already exists: john.smith@example.com",
            "data": null,
            "errors": {
                "error": "User with email already exists: john.smith@example.com"
            }
        }
        """;

  // Validation error response
  public static final String VALIDATION_ERROR_RESPONSE =
      """
        {
            "status": "ERROR",
            "message": "Validation errors occurred",
            "data": null,
            "errors": {
                "firstName": "First name cannot be blank",
                "email": "Invalid email format",
                "role": "Role must be either PM or BA"
            }
        }
        """;

  // Access denied error response
  public static final String ACCESS_DENIED_RESPONSE =
      """
        {
            "status": "ERROR",
            "message": "This endpoint is only accessible to users with MANAGER role",
            "data": null,
            "errors": {
                "error": "This endpoint is only accessible to users with MANAGER role"
            }
        }
        """;

  // Get user by email success response
  public static final String GET_USER_SUCCESS_RESPONSE =
      """
        {
            "status": "SUCCESS",
            "message": "User retrieved successfully",
            "data": {
                "id": "65f3a12b7d8c9e001a2b3c4d",
                "firstName": "John",
                "lastName": "Smith",
                "email": "john.smith@example.com",
                "role": "PM",
                "createdAt": "2024-04-10T15:30:45.123Z",
                "updatedAt": null
            },
            "errors": null
        }
        """;

  // User not found error response
  public static final String USER_NOT_FOUND_RESPONSE =
      """
        {
            "status": "ERROR",
            "message": "User not found with email: nonexistent@example.com",
            "data": null,
            "errors": {
                "error": "User not found with email: nonexistent@example.com"
            }
        }
        """;

  // Get all users success response
  public static final String GET_ALL_USERS_SUCCESS_RESPONSE =
      """
        {
            "status": "SUCCESS",
            "message": "Users retrieved successfully",
            "data": [
                {
                    "id": "65f3a12b7d8c9e001a2b3c4d",
                    "firstName": "John",
                    "lastName": "Smith",
                    "email": "john.smith@example.com",
                    "role": "PM",
                    "createdAt": "2024-04-10T15:30:45.123Z",
                    "updatedAt": null
                },
                {
                    "id": "65f3a12b7d8c9e001a2b3c4e",
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "jane.doe@example.com",
                    "role": "BA",
                    "createdAt": "2024-04-10T16:45:22.456Z",
                    "updatedAt": null
                }
            ],
            "errors": null
        }
        """;

  public static final String USER_DETAIL_RESPONSE =
      """
        {
          "status": "success",
          "message": "User details retrieved successfully",
          "data": {
            "id": "6572a1b5c3f4d83127895432",
            "firstName": "John",
            "lastName": "Doe",
            "username": "john.doe@example.com",
            "role": "PM",
            "dateCreated": "2023-12-08 10:15:34",
            "dateLastModified": "2023-12-10 14:22:45"
          },
          "errors": null
        }
        """;

  // Description for PM and BA users endpoint
  public static final String PM_BA_USERS_ENDPOINT_DESCRIPTION =
      """
      Retrieves all PM and BA users with their details, separated by role. \
      This endpoint is only accessible to users with MANAGER role.

      **Authentication Required:**
      - Bearer JWT token must be provided in the Authorization header
      - Format: `Authorization: Bearer {token}`
      - User must have the MANAGER role
      """;

  // PM and BA users unauthorized error response
  public static final String PM_BA_USERS_UNAUTHORIZED_RESPONSE =
      """
      {
          "status": "error",
          "message": "Unauthorized: Missing or invalid authentication token",
          "data": null,
          "errors": {
              "error": "Full authentication is required to access this resource"
          }
      }
      """;

  // PM and BA users server error response
  public static final String PM_BA_USERS_SERVER_ERROR_RESPONSE =
      """
      {
          "status": "error",
          "message": "Some thing went wrong",
          "data": null,
          "errors": {
              "error": "An unexpected error occurred while processing the request"
          }
      }
      """;

  // PM and BA users success response
  public static final String PM_BA_USERS_SUCCESS_RESPONSE =
      """
      {
          "status": "success",
          "message": "Users retrieved successfully",
          "data": {
              "pmUsers": [
                  {
                      "id": "64a1b2c3d4e5f6789012345a",
                      "fullName": "John Smith",
                      "email": "john.smith@example.com",
                      "role": "PM"
                  }
              ],
              "baUsers": [
                  {
                      "id": "64a1b2c3d4e5f6789012345b",
                      "fullName": "Jane Doe",
                      "email": "jane.doe@example.com",
                      "role": "BA"
                  }
              ]
          },
          "errors": null
      }
      """;

  // PM and BA users empty response
  public static final String PM_BA_USERS_EMPTY_RESPONSE =
      """
      {
          "status": "success",
          "message": "Users retrieved successfully",
          "data": {
              "pmUsers": [],
              "baUsers": []
          },
          "errors": null
      }
      """;

  // Search parameter description
  public static final String SEARCH_PARAM_DESC =
      "Optional text to search in firstName, lastName, and email fields. "
          + "Supports full name search (e.g., 'John Smith'). "
          + "If empty or null, returns all PM and BA users.";

  // Search endpoint literals
  public static final String SEARCH_FUNCTIONALITY_DESC =
      """
      - Searches across firstName, lastName, and email fields
      - Case-insensitive search
      - Supports full name search (e.g., "John Smith")
      - Partial matches are supported for each field
      - If search term is empty or null, returns all PM and BA users
      - Results are grouped into pmUsers and baUsers arrays
      """;

  public static final String SEARCH_QUERY_PARAMS_DESC =
      """
      **Query Parameters:**
      - searchTerm (optional): Text to search for in user fields
        - Examples:
          - "john" (matches firstName, lastName, or email)
          - "john smith" (matches full name)
          - "example.com" (matches email)
      """;

  public static final String SEARCH_AUTH_DESC =
      """
      **Authentication Required:**
      - Bearer JWT token must be provided in the Authorization header
      - Format: `Authorization: Bearer {token}`
      - User must have the MANAGER role
      """;

  public static final String SEARCH_RESPONSE_FORMAT_DESC =
      """
      **Response Format:**
      - pmUsers: Array of PM users matching the search criteria
      - baUsers: Array of BA users matching the search criteria
      - Each user object contains: id, fullName, email, and role
      """;

  // Description for search PM and BA users endpoint
  public static final String SEARCH_PM_BA_USERS_ENDPOINT_DESCRIPTION =
      "Searches PM and BA users by name or email, returning results separated by role. "
          + "This endpoint is only accessible to users with MANAGER role.\n\n"
          + "**Search Functionality:**\n"
          + SEARCH_FUNCTIONALITY_DESC
          + "\n\n"
          + SEARCH_QUERY_PARAMS_DESC
          + "\n\n"
          + SEARCH_AUTH_DESC
          + "\n\n"
          + SEARCH_RESPONSE_FORMAT_DESC;

  // Search response examples
  public static final String SEARCH_SUCCESS_PM_USER1 =
      """
          {
              "id": "64a1b2c3d4e5f6789012345c",
              "fullName": "John Smith",
              "email": "john.smith@example.com",
              "role": "PM"
          }
      """;

  public static final String SEARCH_SUCCESS_PM_USER2 =
      """
          {
              "id": "64a1b2c3d4e5f6789012345d",
              "fullName": "Johnny Walker",
              "email": "j.walker@example.com",
              "role": "PM"
          }
      """;

  public static final String SEARCH_SUCCESS_BA_USER =
      """
          {
              "id": "64a1b2c3d4e5f6789012345e",
              "fullName": "Jane Johnson",
              "email": "jane.johnson@example.com",
              "role": "BA"
          }
      """;

  // Search PM and BA users success response
  public static final String SEARCH_PM_BA_USERS_SUCCESS_RESPONSE =
      """
      {
          "status": "success",
          "message": "Users searched successfully",
          "data": {
              "pmUsers": [
      """
          + SEARCH_SUCCESS_PM_USER1
          + ","
          + SEARCH_SUCCESS_PM_USER2
          + """
              ],
              "baUsers": [
      """
          + SEARCH_SUCCESS_BA_USER
          + """
              ]
          },
          "errors": null
      }
      """;

  // Search PM and BA users no results response
  public static final String SEARCH_PM_BA_USERS_NO_RESULTS_RESPONSE =
      """
      {
          "status": "success",
          "message": "Users searched successfully",
          "data": {
              "pmUsers": [],
              "baUsers": []
          },
          "errors": null
      }
      """;

  // Search error responses
  public static final String SEARCH_ERROR_BAD_REQUEST =
      """
      {
          "error": "Search term contains invalid characters or exceeds maximum length"
      }
      """;

  public static final String SEARCH_ERROR_UNAUTHORIZED =
      """
      {
          "error": "Full authentication is required to access this resource"
      }
      """;

  public static final String SEARCH_ERROR_FORBIDDEN =
      """
      {
          "error": "This endpoint is only accessible to users with MANAGER role"
      }
      """;

  // Search PM and BA users bad request response
  public static final String SEARCH_PM_BA_USERS_BAD_REQUEST_RESPONSE =
      """
      {
          "status": "error",
          "message": "Invalid search parameter",
          "data": null,
          "errors": """
          + SEARCH_ERROR_BAD_REQUEST
          + """
      }
      """;
}
