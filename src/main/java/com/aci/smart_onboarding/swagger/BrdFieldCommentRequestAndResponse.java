package com.aci.smart_onboarding.swagger;

public class BrdFieldCommentRequestAndResponse {
  private BrdFieldCommentRequestAndResponse() {}

  public static class Descriptions {
    private Descriptions() {}

    public static final String GET_ENTITY_COMMENTS =
        "Returns field comment groups based on the provided parameters. "
            + "Required parameters: brdFormId and sourceType. "
            + "If sourceType is 'SITE', siteId is also required. "
            + "Optional parameters: sectionName, fieldPath (requires sectionName). "
            + "Use these to filter results by section or specific field.";

    public static final String GET_PENDING_COMMENTS =
        "Returns all pending comment groups for a BRD with detailed statistics. "
            + "The response includes counts and details of pending comments for both the BRD source type "
            + "and for each related site. This helps track unresolved discussions across the entire BRD "
            + "and all its related sites in a single API call.";

    public static final String GET_COMMENTS_BY_SOURCE =
        "Retrieves all comment groups for a specific source type (BRD or SITE) with optional status filtering. "
            + "Required parameters: brdFormId and sourceType. "
            + "If sourceType is 'SITE', siteId is also required. "
            + "Optional parameter: status (can be 'Pending' or 'Resolved'). "
            + "The response includes total counts, section-wise statistics, and all comments "
            + "for the specified source type.";

    public static final String UPDATE_COMMENT_READ_STATUS =
        "Updates the read status (isRead field) and updatedAt timestamp of a specific comment. "
            + "Required parameters: sourceType, brdFormId, sectionName, fieldPath, and commentId in the request body. "
            + "If sourceType is 'SITE', siteId is also required as a query parameter. "
            + "The endpoint returns success (true) when the comment is updated or "
            + "a 404 error when the comment is not found.";
  }

  public static class Request {
    private Request() {}

    public static final String GROUP_REQ =
        """
                {
                    "brdFormId": "6123f2b7e8d6a1234567890a",
                    "sourceType": "BRD",
                    "sectionName": "clientInformation",
                    "fieldPath": "customerInfo.name",
                    "fieldPathShadowValue": "John Doe",
                    "status": "Pending"
                }
                """;

    public static final String GROUP_REQ_SITE =
        """
                {
                    "brdFormId": "6123f2b7e8d6a1234567890a",
                    "siteId": "site123",
                    "sourceType": "SITE",
                    "sectionName": "clientInformation",
                    "fieldPath": "customerInfo.name",
                    "fieldPathShadowValue": "John Doe",
                    "status": "Pending"
                }
                """;

    public static final String COMMENT_REQ =
        """
                {
                    "content": "This needs to be reviewed.",
                    "createdBy": "user123",
                    "userType": "ADMIN",
                    "parentCommentId": null
                }
                """;

    public static final String REPLY_REQ =
        """
                {
                    "content": "I've reviewed this and it looks good.",
                    "createdBy": "user456",
                    "userType": "CLIENT",
                    "parentCommentId": "COMMENT123"
                }
                """;

    public static final String SHADOW_VALUE_UPDATE_REQ =
        """
                {
                    "brdFormId": "6123f2b7e8d6a1234567890a",
                    "sourceType": "BRD",
                    "sectionName": "clientInformation",
                    "fieldPath": "customerInfo.name"
                }
                """;

    public static final String SHADOW_VALUE_UPDATE_REQ_SITE =
        """
                {
                    "brdFormId": "6123f2b7e8d6a1234567890a",
                    "siteId": "site123",
                    "sourceType": "SITE",
                    "sectionName": "clientInformation",
                    "fieldPath": "customerInfo.name"
                }
                """;

    public static final String READ_STATUS_UPDATE_REQ =
        """
                {
                    "commentId": "COMMENT123",
                    "isRead": true
                }
                """;
  }

  public static class Response {
    private Response() {}

    // Success responses
    public static final String CREATE_GROUP_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Field comment group created/updated successfully",
                    "data": {
                        "id": "6123f2b7e8d6a1234567890a",
                        "brdFormId": "6123f2b7e8d6a1234567890a",
                        "sourceType": "BRD",
                        "sectionName": "clientInformation",
                        "fieldPath": "customerInfo.name",
                        "fieldPathShadowValue": "John Doe",
                        "status": "Pending",
                        "comments": [],
                        "createdAt": "2023-10-10T10:15:30",
                        "updatedAt": "2023-10-10T10:15:30"
                    },
                    "errors": null
                }
                """;

    public static final String CREATE_GROUP_SUCCESS_SITE =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Field comment group created/updated successfully",
                    "data": {
                        "id": "6123f2b7e8d6a1234567890a",
                        "brdFormId": "6123f2b7e8d6a1234567890a",
                        "siteId": "site123",
                        "sourceType": "SITE",
                        "sectionName": "clientInformation",
                        "fieldPath": "customerInfo.name",
                        "fieldPathShadowValue": "John Doe",
                        "status": "Pending",
                        "comments": [],
                        "createdAt": "2023-10-10T10:15:30",
                        "updatedAt": "2023-10-10T10:15:30"
                    },
                    "errors": null
                }
                """;

    public static final String ADD_COMMENT_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Comment added successfully",
                    "data": {
                        "id": "COMMENT123",
                        "content": "This needs to be reviewed.",
                        "createdBy": "user123",
                        "userType": "ADMIN",
                        "parentCommentId": null,
                        "createdAt": "2023-10-10T10:20:30",
                        "updatedAt": "2023-10-10T10:20:30"
                    },
                    "errors": null
                }
                """;

    public static final String GET_COMMENTS_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Field comment groups retrieved successfully",
                    "data": [
                        {
                            "id": "6123f2b7e8d6a1234567890a",
                            "brdFormId": "6123f2b7e8d6a1234567890a",
                            "sourceType": "BRD",
                            "sectionName": "clientInformation",
                            "fieldPath": "customerInfo.name",
                            "fieldPathShadowValue": "John Doe",
                            "status": "Pending",
                            "comments": [
                                {
                                    "id": "COMMENT123",
                                    "content": "This needs to be reviewed.",
                                    "createdBy": "user123",
                                    "userType": "ADMIN",
                                    "parentCommentId": null,
                                    "createdAt": "2023-10-10T10:20:30",
                                    "updatedAt": "2023-10-10T10:20:30"
                                }
                            ],
                            "createdAt": "2023-10-10T10:15:30",
                            "updatedAt": "2023-10-10T10:20:30"
                        }
                    ],
                    "errors": null
                }
                """;

    public static final String UPDATE_SHADOW_VALUE_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Field updated with shadow value and comment group marked as resolved",
                    "data": true,
                    "errors": null
                }
                """;

    public static final String READ_STATUS_UPDATE_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Comment read status updated successfully",
                    "data": true,
                    "errors": null
                }
                """;

    public static final String COMMENTS_BY_SOURCE_SUCCESS =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Comments retrieved successfully for BRD: 6123f2b7e8d6a1234567890a",
                    "data": {
                        "totalCount": 2,
                        "sourceType": "BRD",
                        "brdFormId": "6123f2b7e8d6a1234567890a",
                        "siteId": null,
                        "comments": [
                            {
                                "id": "6123f2b7e8d6a1234567890a",
                                "brdFormId": "6123f2b7e8d6a1234567890a",
                                "sourceType": "BRD",
                                "sectionName": "clientInformation",
                                "fieldPath": "customerInfo.name",
                                "fieldPathShadowValue": "John Doe",
                                "status": "Pending",
                                "comments": [
                                    {
                                        "id": "COMMENT123",
                                        "content": "This needs to be reviewed.",
                                        "createdBy": "user123",
                                        "userType": "ADMIN",
                                        "parentCommentId": null,
                                        "isRead": false,
                                        "createdAt": "2023-10-10T10:20:30",
                                        "updatedAt": "2023-10-10T10:20:30"
                                    }
                                ],
                                "createdAt": "2023-10-10T10:15:30",
                                "updatedAt": "2023-10-10T10:20:30"
                            },
                            {
                                "id": "6123f2b7e8d6a1234567891b",
                                "brdFormId": "6123f2b7e8d6a1234567890a",
                                "sourceType": "BRD",
                                "sectionName": "paymentChannels",
                                "fieldPath": "channels.primaryChannel",
                                "fieldPathShadowValue": "ACH",
                                "status": "Pending",
                                "comments": [
                                    {
                                        "id": "COMMENT456",
                                        "content": "This should be Credit Card instead.",
                                        "createdBy": "user456",
                                        "userType": "PM",
                                        "parentCommentId": null,
                                        "isRead": true,
                                        "createdAt": "2023-10-11T09:30:45",
                                        "updatedAt": "2023-10-12T14:20:15"
                                    }
                                ],
                                "createdAt": "2023-10-11T09:30:45",
                                "updatedAt": "2023-10-11T09:30:45"
                            }
                        ],
                        "sectionCounts": {
                            "clientInformation": 1,
                            "paymentChannels": 1
                        },
                        "sectionFieldPaths": {
                            "clientInformation": ["customerInfo.name"],
                            "paymentChannels": ["channels.primaryChannel"]
                        }
                    },
                    "errors": null
                }
                """;

    public static final String COMMENTS_BY_SOURCE_SUCCESS_SITE =
        """
                {
                    "status": "SUCCESSFUL",
                    "message": "Comments retrieved successfully for Site: site123",
                    "data": {
                        "totalCount": 1,
                        "sourceType": "SITE",
                        "brdFormId": "6123f2b7e8d6a1234567890a",
                        "siteId": "site123",
                        "comments": [
                            {
                                "id": "6123f2b7e8d6a1234567892c",
                                "brdFormId": "6123f2b7e8d6a1234567890a",
                                "siteId": "site123",
                                "sourceType": "SITE",
                                "sectionName": "clientInformation",
                                "fieldPath": "customerInfo.name",
                                "fieldPathShadowValue": "Jane Smith",
                                "status": "Pending",
                                "comments": [
                                    {
                                        "id": "COMMENT789",
                                        "content": "This needs to be updated for this site.",
                                        "createdBy": "user789",
                                        "userType": "PM",
                                        "parentCommentId": null,
                                        "isRead": false,
                                        "createdAt": "2023-10-12T11:45:30",
                                        "updatedAt": "2023-10-12T11:45:30"
                                    }
                                ],
                                "createdAt": "2023-10-12T11:45:30",
                                "updatedAt": "2023-10-12T11:45:30"
                            }
                        ],
                        "sectionCounts": {
                            "clientInformation": 1
                        },
                        "sectionFieldPaths": {
                            "clientInformation": ["customerInfo.name"]
                        }
                    },
                    "errors": null
                }
                """;

    // Error responses
    public static final String NOT_FOUND_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Field comment group not found",
                    "data": null,
                    "errors": {
                        "error": "Field comment group not found"
                    }
                }
                """;

    public static final String COMMENT_NOT_FOUND_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Comment not found with ID: COMMENT999",
                    "data": null,
                    "errors": {
                        "error": "Comment not found with ID: COMMENT999"
                    }
                }
                """;

    public static final String BAD_REQUEST_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Invalid request",
                    "data": null,
                    "errors": {
                        "error": "Invalid input parameters"
                    }
                }
                """;

    public static final String UNAUTHORIZED_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Full authentication is required to access this resource",
                    "data": null,
                    "errors": {
                        "error": "Authentication required"
                    }
                }
                """;

    public static final String FORBIDDEN_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Only Project Managers (PM) can view pending comment statistics",
                    "data": null,
                    "errors": {
                        "error": "Insufficient permissions"
                    }
                }
                """;

    public static final String SERVER_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "An unexpected error occurred",
                    "data": null,
                    "errors": {
                        "error": "Internal server error"
                    }
                }
                """;

    public static final String SHADOW_VALUE_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Failed to update field from shadow value",
                    "data": null,
                    "errors": {
                        "error": "Shadow value is null or invalid"
                    }
                }
                """;

    public static final String INVALID_SOURCE_TYPE_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Invalid sourceType provided",
                    "data": null,
                    "errors": {
                        "error": "sourceType must be either 'BRD' or 'SITE'"
                    }
                }
                """;

    public static final String MISSING_PARAMETER_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Required parameter 'sourceType' is missing",
                    "data": null,
                    "errors": {
                        "error": "Required parameter 'sourceType' is missing"
                    }
                }
                """;

    public static final String COMMENT_LIMIT_ERROR =
        """
                {
                    "status": "FAILURE",
                    "message": "Comment threshold reached for userType 'ADMIN' (maximum 5 comments allowed per userType)",
                    "data": null,
                    "errors": {
                        "error": "Comment threshold reached for userType 'ADMIN' (maximum 5 comments allowed per userType)"
                    }
                }
                """;

    public static final String PENDING_COMMENTS_SUCCESS =
        """
        {
          "status": "Successful",
          "message": "Pending comment groups retrieved successfully for BRD: 60f8a1b3c7e8f42a94d5e8c2",
          "data": {
            "totalPendingCount": 8,
            "brdPendingCount": 3,
            "brdPendingComments": [
              {
                "id": "61a2b3c4d5e6f7g8h9i0j1k2",
                "brdFormId": "60f8a1b3c7e8f42a94d5e8c2",
                "sourceType": "BRD",
                "sectionName": "clientInformation",
                "fieldPath": "contactPhone",
                "status": "Pending",
                "commentsCount": 2,
                "comments": [
                  {
                    "id": "comment1",
                    "message": "Please verify this phone number",
                    "userType": "PM",
                    "userId": "user123",
                    "userName": "John Smith",
                    "createdAt": "2023-06-15T14:30:00Z"
                  },
                  {
                    "id": "comment2",
                    "parentCommentId": "comment1",
                    "message": "Will check and update",
                    "userType": "CM",
                    "userId": "user456",
                    "userName": "Jane Doe",
                    "createdAt": "2023-06-15T15:45:00Z"
                  }
                ],
                "createdAt": "2023-06-15T14:30:00Z",
                "updatedAt": "2023-06-15T15:45:00Z"
              },
              // More BRD comment groups...
            ],
            "sitePendingCounts": {
              "site123": 3,
              "site456": 2
            },
            "sitePendingComments": {
              "site123": [
                {
                  "id": "61a2b3c4d5e6f7g8h9i0j1k3",
                  "brdFormId": "60f8a1b3c7e8f42a94d5e8c2",
                  "siteId": "site123",
                  "sourceType": "SITE",
                  "sectionName": "fundingMethods",
                  "fieldPath": "preferredMethod",
                  "status": "Pending",
                  "commentsCount": 1,
                  "comments": [
                    {
                      "id": "comment3",
                      "message": "Please update with correct funding method",
                      "userType": "PM",
                      "userId": "user123",
                      "userName": "John Smith",
                      "createdAt": "2023-06-16T10:15:00Z"
                    }
                  ],
                  "createdAt": "2023-06-16T10:15:00Z",
                  "updatedAt": "2023-06-16T10:15:00Z"
                },
                // More site comment groups...
              ],
              "site456": [
                // Site 456 comment groups...
              ]
            }
          },
          "error": null
        }
        """;
  }
}
