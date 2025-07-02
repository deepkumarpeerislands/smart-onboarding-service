package com.aci.smart_onboarding.swagger;

public class BrdTemplateRequestAndResponses {

  private BrdTemplateRequestAndResponses() {}

  public static final String EXAMPLE_TEMPLATE_ID = "67d826890dd57e7a493d9440";
  public static final String EXAMPLE_TEMPLATE_TYPE = "Utility";

  public static final String TEMPLATE_REQUEST =
      """
            {
                "templateTypes": "Utility",
                "summary": "Entities delivering essential public services such as electricity, water, natural gas, or waste management. Their billing involves recurring payments based on service usage or flat rates for access",
                "clientInformation": true,
                "aciInformation": true,
                "paymentChannels": true,
                "fundingMethods": true,
                "achPaymentProcessing": true,
                "miniAccountMaster": false,
                "accountIdentifierInformation": true,
                "paymentRules": true,
                "notifications": true,
                "remittance": false,
                "agentPortal": true,
                "recurringPayments": true,
                "ivr": false,
                "generalImplementations": true,
                "approvals": true,
                "revisionHistory": true
            }
            """;

  public static final String TEMPLATE_SUCCESS_RESPONSE =
      """
            {
                "status": "Successful",
                "message": "Template created successfully",
                "data": {
                    "id": "67d826890dd57e7a493d9440",
                    "templateTypes": "Utility",
                    "summary": "Entities delivering essential public services such as electricity, water, natural gas, or waste management. Their billing involves recurring payments based on service usage or flat rates for access",
                    "clientInformation": true,
                    "aciInformation": true,
                    "paymentChannels": true,
                    "fundingMethods": true,
                    "achPaymentProcessing": true,
                    "miniAccountMaster": false,
                    "accountIdentifierInformation": true,
                    "paymentRules": true,
                    "notifications": true,
                    "remittance": false,
                    "agentPortal": true,
                    "recurringPayments": true,
                    "ivr": false,
                    "generalImplementations": true,
                    "approvals": true,
                    "revisionHistory": true
                },
                "errors": null
            }
            """;

  public static final String TEMPLATE_UPDATE_SUCCESS_RESPONSE =
      """
            {
                "status": "Successful",
                "message": "Template updated successfully",
                "data": {
                    "id": "67d826890dd57e7a493d9440",
                    "templateTypes": "Utility",
                    "summary": "Entities delivering essential public services such as electricity, water, natural gas, or waste management. Their billing involves recurring payments based on service usage or flat rates for access",
                    "clientInformation": true,
                    "aciInformation": true,
                    "paymentChannels": true,
                    "fundingMethods": true,
                    "achPaymentProcessing": true,
                    "miniAccountMaster": true,
                    "accountIdentifierInformation": true,
                    "paymentRules": true,
                    "notifications": true,
                    "remittance": true,
                    "agentPortal": true,
                    "recurringPayments": true,
                    "ivr": true,
                    "generalImplementations": true,
                    "approvals": true,
                    "revisionHistory": true
                },
                "errors": null
            }
            """;

  public static final String TEMPLATE_LIST_SUCCESS_RESPONSE =
      """
            {
                "status": "Successful",
                "message": "Templates retrieved successfully",
                "data": [
                    {
                        "id": "67d826890dd57e7a493d9440",
                        "templateType": "Utility",
                        "summary": "Template for utility payment processing",
                        "clientInformation": true,
                        "aciInformation": true,
                        "paymentChannels": true,
                        "fundingMethods": true,
                        "achPaymentProcessing": true,
                        "miniAccountMaster": false,
                        "accountIdentifierInformation": true,
                        "paymentRules": true,
                        "notifications": true,
                        "remittance": false,
                        "agentPortal": true,
                        "recurringPayments": true,
                        "ivr": false,
                        "generalImplementations": true,
                        "approvals": true,
                        "revisionHistory": true
                    },
                    {
                        "id": "67d826890dd57e7a493d9441",
                        "templateType": "Healthcare",
                        "summary": "Template for healthcare payment processing",
                        "clientInformation": true,
                        "aciInformation": false,
                        "paymentChannels": true,
                        "fundingMethods": true,
                        "achPaymentProcessing": true,
                        "miniAccountMaster": false,
                        "accountIdentifierInformation": true,
                        "paymentRules": true,
                        "notifications": false,
                        "remittance": false,
                        "agentPortal": true,
                        "recurringPayments": false,
                        "ivr": false,
                        "generalImplementations": true,
                        "approvals": true,
                        "revisionHistory": true
                    }
                ],
                "errors": null
            }
            """;

  public static final String TEMPLATE_VALIDATION_ERROR =
      """
            {
                "status": "failure",
                "message": "Validation errors occurred",
                "data": null,
                "errors": {
                    "templateType": "Template type is required"
                }
            }
            """;

  public static final String TEMPLATE_NOT_FOUND =
      """
            {
                "status": "failure",
                "message": "Template not found with type: Utility",
                "data": null,
                "errors": null
            }
            """;

  public static final String TEMPLATE_ALREADY_EXISTS =
      """
            {
                "status": "failure",
                "message": "Template already exists with type: Utility",
                "data": null,
                "errors": null
            }
            """;

  public static final String INTERNAL_SERVER_ERROR =
      """
            {
                "status": "failure",
                "message": "Something went wrong",
                "data": null,
                "errors": null
            }
            """;
}
