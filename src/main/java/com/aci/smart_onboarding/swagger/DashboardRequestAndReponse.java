package com.aci.smart_onboarding.swagger;

public class DashboardRequestAndReponse {
  private DashboardRequestAndReponse() {}

  public static class Descriptions {
    private Descriptions() {}

    public static final String GET_UNRESOLVED_COMMENT_GROUPS_COUNT =
        """
            Retrieves the count of unresolved comment groups for Business Requirement Documents (BRDs)
            that are not in the 'Submitted' state. This endpoint is restricted to users with PM role and
            has a 30-second timeout to prevent long-running requests. The response includes the total count
            of unresolved comment groups across all BRDs and per-BRD counts with BRD details.

            The data can be filtered using the scope parameter:
            - scope: Use 'ME' to show only comment groups created by the current user, or 'TEAM' to show all comment groups regardless of creator.
            """;

    public static final String GET_BRD_COUNTS_BY_TYPE =
        """
            Retrieves counts of BRDs by type (NEW, UPDATE, TRIAGE) for the past 52 weeks.

            The data can be filtered using the scope parameter:
            - scope: Use 'ME' to show only BRDs created by the current user, or 'TEAM' to show all BRDs regardless of creator.

            The response includes weekly metrics for the past 52 weeks, with weeks numbered sequentially:
            - Week 1 = last week of previous month (most recent)
            - Week 2 = second last week of previous month
            - ...and so on until Week 52 = oldest week (52 weeks ago)

            This provides trend data for BRD counts by type over the past year.
            """;

    public static final String GET_BRD_UPLOAD_METRICS =
        """
            Returns metrics about how many SSD and Contract files are uploaded for BRDs.

            The data can be filtered using two parameters:
            - filter: Use 'OPEN' to include only open BRDs (excluding 'Submitted' status) or 'ALL' to include all BRDs regardless of status.
            - scope: Use 'ME' to show only BRDs created by the current user, or 'TEAM' to show all BRDs regardless of creator.

            Role-based behavior for the scope parameter:
            - PM role: scope parameter defaults to 'ME' if not provided
            - Manager role: scope parameter is ignored, always using 'TEAM' scope regardless of what's provided

            When filter=ALL, weekly metrics for the past 52 weeks will be included. The weeks are numbered sequentially:
            - Week 1 = last week of previous month (most recent)
            - Week 2 = second last week of previous month
            - ...and so on until Week 52 = oldest week (52 weeks ago)

            This provides trend data for uploads over the past year, starting from the previous month and going backwards.
            """;

    public static final String GET_AVERAGE_COMMENT_RESOLUTION_TIME =
        """
            Returns the average time to resolve comments (updatedAt - createdAt) for all three periods in a single response:

            - Month: Returns a single figure in days for the current month
            - Quarter: Returns trend data across months in the quarter
            - Year: Returns trend data across all 12 months of the past year

            No request parameters are needed. This endpoint is only accessible to users with MANAGER role.
            """;

    public static final String GET_COMMENT_GROUP_STATS =
        """
            Returns statistics about comment groups in open BRDs, including total count, resolved count, and pending comment details.

            The statistics include:
            - Total number of comment groups across all non-submitted BRDs
            - Number of resolved comment groups
            - Pending comment statistics:
              - Total number of pending comment groups
              - Number of pending groups with at least one PM comment
              - Number of pending groups without any PM comments

            No request parameters are needed. This endpoint is only accessible to users with MANAGER role.
            """;
  }

  public static class Response {
    private Response() {}

    // Success responses
    public static final String UNRESOLVED_COMMENT_GROUPS_COUNT_SUCCESS =
        """
            {
                "status": "200",
                "message": "Successfully retrieved unresolved comment groups count",
                "data": {
                    "totalCount": 42,
                    "brdCounts": {
                        "6123f2b7e8d6a1234567890a": {
                            "brdId": "BRD-123",
                            "brdName": "New Feature Implementation",
                            "status": "Draft",
                            "count": 5
                        },
                        "6123f2b7e8d6a1234567890b": {
                            "brdId": "BRD-124",
                            "brdName": "API Integration Project",
                            "status": "In Progress",
                            "count": 12
                        },
                        "6123f2b7e8d6a1234567890c": {
                            "brdId": "BRD-125",
                            "brdName": "Security Enhancement",
                            "status": "Internal Review",
                            "count": 25
                        }
                    }
                },
                "pagination": null
            }
            """;

    public static final String BRD_COUNTS_BY_TYPE_SUCCESS =
        """
            {
                "status": "200",
                "message": "Successfully retrieved BRD counts by type",
                "data": {
                    "scope": "TEAM",
                    "weeklyMetrics": {
                        "weeks": ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7", "Week 8", "Week 9", "Week 10", "Week 11", "Week 12"],
                        "counts": {
                            "newCounts": [5, 8, 10, 7, 12, 9, 11, 6, 8, 9, 11, 7],
                            "updateCounts": [8, 12, 15, 10, 16, 14, 18, 12, 14, 16, 13, 15],
                            "triageCounts": [2, 3, 4, 2, 5, 4, 6, 3, 4, 3, 5, 2],
                            "totalCounts": [15, 23, 29, 19, 33, 27, 35, 21, 26, 28, 29, 24]
                        }
                    }
                },
                "pagination": null
            }
            """;

    public static final String BRD_UPLOAD_METRICS_SUCCESS =
        """
            {
                "status": "Successful",
                "message": "Successfully retrieved BRD upload metrics",
                "data": {
                    "filterType": "ALL",
                    "scope": "TEAM",
                    "ssdUploads": {
                        "newBrds": {
                            "totalCount": 100,
                            "uploadedCount": 70,
                            "uploadedPercentage": 70,
                            "notUploadedCount": 30,
                            "notUploadedPercentage": 30
                        },
                        "updateBrds": {
                            "totalCount": 80,
                            "uploadedCount": 72,
                            "uploadedPercentage": 90,
                            "notUploadedCount": 8,
                            "notUploadedPercentage": 10
                        }
                    },
                    "contractUploads": {
                        "newBrds": {
                            "totalCount": 100,
                            "uploadedCount": 60,
                            "uploadedPercentage": 60,
                            "notUploadedCount": 40,
                            "notUploadedPercentage": 40
                        },
                        "updateBrds": {
                            "totalCount": 80,
                            "uploadedCount": 64,
                            "uploadedPercentage": 80,
                            "notUploadedCount": 16,
                            "notUploadedPercentage": 20
                        }
                    },
                    "weeklyMetrics": {
                        "weeks": ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7"],
                        "newBrds": {
                            "totalCounts": [5, 8, 10, 7, 12, 9, 11],
                            "ssdUploadedCounts": [3, 6, 7, 5, 9, 7, 8],
                            "contractUploadedCounts": [2, 5, 6, 4, 8, 6, 7]
                        },
                        "updateBrds": {
                            "totalCounts": [4, 6, 8, 9, 7, 8, 10],
                            "ssdUploadedCounts": [3, 5, 7, 8, 7, 7, 9],
                            "contractUploadedCounts": [3, 5, 6, 7, 6, 6, 8]
                        }
                    }
                },
                "pagination": null
            }
            """;

    public static final String COMMENT_RESOLUTION_TIME_SUCCESS =
        """
            {
                "status": "SUCCESS",
                "message": "Successfully retrieved average comment resolution time",
                "data": {
                    "monthPeriod": {
                        "month": "APRIL 2025",
                        "averageResolutionDays": 4.2,
                        "resolvedCommentsCount": 12
                    },
                    "quarterPeriod": {
                        "monthlyData": [
                            {
                                "month": "FEBRUARY 2025",
                                "averageResolutionDays": 3.8,
                                "resolvedCommentsCount": 15
                            },
                            {
                                "month": "MARCH 2025",
                                "averageResolutionDays": 4.5,
                                "resolvedCommentsCount": 10
                            },
                            {
                                "month": "APRIL 2025",
                                "averageResolutionDays": 4.2,
                                "resolvedCommentsCount": 12
                            }
                        ]
                    },
                    "yearPeriod": {
                        "monthlyData": [
                            {
                                "month": "MAY 2024",
                                "averageResolutionDays": 5.1,
                                "resolvedCommentsCount": 8
                            },
                            {
                                "month": "JUNE 2024",
                                "averageResolutionDays": 4.8,
                                "resolvedCommentsCount": 12
                            },
                            {
                                "month": "JULY 2024",
                                "averageResolutionDays": 4.2,
                                "resolvedCommentsCount": 14
                            },
                            {
                                "month": "AUGUST 2024",
                                "averageResolutionDays": 3.9,
                                "resolvedCommentsCount": 11
                            },
                            {
                                "month": "SEPTEMBER 2024",
                                "averageResolutionDays": 4.1,
                                "resolvedCommentsCount": 9
                            },
                            {
                                "month": "OCTOBER 2024",
                                "averageResolutionDays": 4.3,
                                "resolvedCommentsCount": 10
                            },
                            {
                                "month": "NOVEMBER 2024",
                                "averageResolutionDays": 4.7,
                                "resolvedCommentsCount": 13
                            },
                            {
                                "month": "DECEMBER 2024",
                                "averageResolutionDays": 5.2,
                                "resolvedCommentsCount": 7
                            },
                            {
                                "month": "JANUARY 2025",
                                "averageResolutionDays": 4.6,
                                "resolvedCommentsCount": 11
                            },
                            {
                                "month": "FEBRUARY 2025",
                                "averageResolutionDays": 3.8,
                                "resolvedCommentsCount": 15
                            },
                            {
                                "month": "MARCH 2025",
                                "averageResolutionDays": 4.5,
                                "resolvedCommentsCount": 10
                            },
                            {
                                "month": "APRIL 2025",
                                "averageResolutionDays": 4.2,
                                "resolvedCommentsCount": 12
                            }
                        ]
                    }
                },
                "errors": null
            }
            """;

    public static final String COMMENT_GROUP_STATS_SUCCESS =
        """
            {
                "status": "Successful",
                "message": "Comment group statistics retrieved successfully",
                "data": {
                    "totalCommentGroups": 45,
                    "resolvedCommentGroups": 30,
                    "pendingCommentStats": {
                        "totalPendingGroups": 15,
                        "groupsWithPmComment": 8,
                        "groupsWithoutPmComment": 7
                    }
                },
                "errors": null
            }
            """;

    // Error responses
    public static final String FORBIDDEN_ERROR =
        """
            {
                "status": "403",
                "message": "This endpoint is only accessible to users with PM or Manager role",
                "data": null,
                "pagination": null
            }
            """;

    public static final String SERVER_ERROR =
        """
            {
                "status": "500",
                "message": "An error occurred while retrieving comment group counts: Database connection error",
                "data": null,
                "pagination": null
            }
            """;

    public static final String TIMEOUT_ERROR =
        """
            {
                "status": "504",
                "message": "Request timed out. Please try again later.",
                "data": null,
                "pagination": null
            }
            """;

    public static final String INVALID_TIME_PERIOD_ERROR =
        """
            {
                "status": "400",
                "message": "Invalid time period. Must be one of: MONTH, QUARTER, YEAR",
                "data": null,
                "pagination": null
            }
            """;

    public static final String INVALID_SCOPE_ERROR =
        """
            {
                "status": "400",
                "message": "Invalid scope parameter. Must be either ME or TEAM.",
                "data": null,
                "pagination": null
            }
            """;
  }
}
