package com.aci.smart_onboarding.swagger;

public class SiteRequestAndReponse {

  public static final String UPDATE_SITE_REQUEST_EXAMPLE =
      """
    {
      "siteName": "Updated Site Name",
      "identifierCode": "UPD123",
      "description": "This is an updated description for the site",
      "brdForm": {
        "clientInformation": {
          "companyName": "Global Company Inc.",
          "contactName": "John Doe",
          "contactEmail": "john.doe@example.com",
          "contactPhone": "123-456-7890"
        },
        "paymentChannels": {
          "webEnabled": true,
          "mobileEnabled": true,
          "ivruEnabled": false
        },
        "processingDetails": {
          "processingMode": "REAL_TIME",
          "cutoffTime": "23:59:59"
        },
        "security": {
          "encryptionRequired": true,
          "tokenizationEnabled": false
        }
      }
    }
    """;

  public static final String UPDATE_SITE_PARTIAL_REQUEST_EXAMPLE =
      """
    {
      "siteName": "Updated Site Name Only",
      "description": "Only updating name and description"
    }
    """;

  public static final String UPDATE_BRDFORM_PARTIAL_REQUEST_EXAMPLE =
      """
    {
      "brdForm": {
        "clientInformation": {
          "contactEmail": "new.email@example.com"
        },
        "processingDetails": {
          "processingMode": "BATCH"
        }
      }
    }
    """;

  public static final String UPDATE_SITE_RESPONSE_EXAMPLE =
      """
    {
      "status": "SUCCESS",
      "message": "Site updated successfully",
      "data": {
        "id": "65f8d13c93656f50db3a23a6",
        "brdId": "BRD0001",
        "siteId": "SITE_BRD0001_001",
        "siteName": "Updated Site Name",
        "identifierCode": "UPD123",
        "description": "This is an updated description for the site",
        "brdForm": {
          "clientInformation": {
            "companyName": "Global Company Inc.",
            "contactName": "John Doe",
            "contactEmail": "john.doe@example.com",
            "contactPhone": "123-456-7890"
          },
          "paymentChannels": {
            "webEnabled": true,
            "mobileEnabled": true,
            "ivruEnabled": false
          },
          "processingDetails": {
            "processingMode": "REAL_TIME",
            "cutoffTime": "23:59:59"
          },
          "security": {
            "encryptionRequired": true,
            "tokenizationEnabled": false
          }
        },
        "createdAt": "2024-03-18T12:30:45.123",
        "updatedAt": "2024-03-18T15:45:22.567"
      },
      "errors": null
    }
    """;

  public static final String UPDATE_SITE_ERROR_RESPONSE_EXAMPLE =
      """
    {
      "status": "FAILURE",
      "message": "Site not found with ID: 65f8d13c93656f50db3a23a7",
      "data": null,
      "errors": {
        "timestamp": "2024-03-18T15:45:22.567",
        "errorMessage": "Site not found with ID: 65f8d13c93656f50db3a23a7"
      }
    }
    """;

  public static final String UPDATE_SITE_BAD_REQUEST_ERROR_EXAMPLE =
      """
    {
      "status": "FAILURE",
      "message": "Invalid input data",
      "data": null,
      "errors": {
        "timestamp": "2024-03-18T15:45:22.567",
        "errorMessage": "Invalid input data",
        "siteName": "Site name cannot be blank",
        "identifierCode": "Identifier code cannot be blank"
      }
    }
    """;

  public static final String GET_SITES_BY_BRD_SUCCESS_EXAMPLE =
      """
    {
      "status": "SUCCESS",
      "message": "Divisions retrieved successfully",
      "data": {
        "brdId": "BRD0001",
        "brdName": "Client Onboarding Project",
        "description": "Primary onboarding project for ABC Corp",
        "customerId": "CUST001",
        "wallentronIncluded": true,
        "achEncrypted": false,
        "siteList": [
          {
            "id": "65f8d13c93656f50db3a23a6",
            "siteId": "SITE_BRD0001_001",
            "siteName": "Production Site",
            "identifierCode": "PROD",
            "description": "Main production environment",
            "brdForm": {
              "clientInformation": {
                "companyName": "ABC Corporation",
                "contactName": "John Smith"
              },
              "paymentChannels": {
                "webEnabled": true,
                "mobileEnabled": true
              }
            }
          },
          {
            "id": "65f8d13c93656f50db3a23a7",
            "siteId": "SITE_BRD0001_002",
            "siteName": "Test Site",
            "identifierCode": "TEST",
            "description": "Test environment",
            "brdForm": {
              "clientInformation": {
                "companyName": "ABC Corporation",
                "contactName": "Jane Doe"
              },
              "paymentChannels": {
                "webEnabled": true,
                "mobileEnabled": false
              }
            }
          }
        ],
        "createdAt": "2024-03-18T10:30:45.123",
        "updatedAt": "2024-03-18T11:45:22.567"
      },
      "errors": null
    }
    """;

  public static final String GET_SITES_BY_BRD_NOT_FOUND_EXAMPLE =
      """
    {
      "status": "FAILURE",
      "message": "BRD not found with ID: INVALID_BRD",
      "data": null,
      "errors": {
        "timestamp": "2024-03-18T15:45:22.567",
        "errorMessage": "BRD not found with ID: INVALID_BRD"
      }
    }
    """;

  public static final String GET_SITES_BY_BRD_BAD_REQUEST_EXAMPLE =
      """
    {
      "status": "FAILURE",
      "message": "BRD ID cannot be blank",
      "data": null,
      "errors": {
        "timestamp": "2024-03-18T15:45:22.567",
        "errorMessage": "BRD ID cannot be blank"
      }
    }
    """;

  public static final String INTERNAL_SERVER_ERROR =
      "{\"status\":\"failure\",\"message\":\"Something went wrong\",\"data\":null,\"errors\":null}";

  // Bulk Site Creation
  public static final String BULK_SITE_REQUEST_EXAMPLE =
      """
      {
        "brdId": "BRD0003",
        "numberOfSites": 5
      }
      """;

  public static final String BULK_SITE_RESPONSE_EXAMPLE =
      """
      {
        "status": "SUCCESS",
        "message": "Successfully created 5 sites",
        "data": {
          "siteCount": 5,
          "brdId": "BRD0003",
          "sites": [
            {
              "siteId": "SITE_BRD0003_001",
              "siteName": "Bulk Clone 1",
              "identifierCode": "BC001",
              "brdForm": {
                "clientInformation": {
                  "companyName": "Example Corp"
                }
              }
            },
            {
              "siteId": "SITE_BRD0003_002",
              "siteName": "Bulk Clone 2",
              "identifierCode": "BC002",
              "brdForm": {
                "clientInformation": {
                  "companyName": "Example Corp"
                }
              }
            },
            {
              "siteId": "SITE_BRD0003_003",
              "siteName": "Bulk Clone 3",
              "identifierCode": "BC003",
              "brdForm": {
                "clientInformation": {
                  "companyName": "Example Corp"
                }
              }
            },
            {
              "siteId": "SITE_BRD0003_004",
              "siteName": "Bulk Clone 4",
              "identifierCode": "BC004",
              "brdForm": {
                "clientInformation": {
                  "companyName": "Example Corp"
                }
              }
            },
            {
              "siteId": "SITE_BRD0003_005",
              "siteName": "Bulk Clone 5",
              "identifierCode": "BC005",
              "brdForm": {
                "clientInformation": {
                  "companyName": "Example Corp"
                }
              }
            }
          ]
        },
        "errors": null
      }
      """;

  public static final String BULK_SITE_BAD_REQUEST_EXAMPLE =
      """
      {
        "status": "FAILURE",
        "message": "Maximum of 100 sites can be created in a single bulk operation",
        "data": null,
        "errors": {
          "errorMessage": "Maximum of 100 sites can be created in a single bulk operation",
          "timestamp": "2024-03-26T10:30:00"
        }
      }
      """;

  public static final String BULK_SITE_NOT_FOUND_EXAMPLE =
      """
      {
        "status": "FAILURE",
        "message": "BRD not found with id: INVALID_BRD",
        "data": null,
        "errors": {
          "errorMessage": "BRD not found with id: INVALID_BRD",
          "timestamp": "2024-03-26T10:30:00"
        }
      }
      """;

  private SiteRequestAndReponse() {}

  public static SiteRequestAndReponse createSiteRequestAndReponse() {
    return new SiteRequestAndReponse();
  }
}
