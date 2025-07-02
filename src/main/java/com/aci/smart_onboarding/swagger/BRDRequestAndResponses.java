package com.aci.smart_onboarding.swagger;

public class BRDRequestAndResponses {

  private BRDRequestAndResponses() {}

  public static final String BRD_REQUEST_EXAMPLE =
      "{"
          + "\"status\":\"DRAFT\","
          + "\"projectId\":\"PRJ-123456\","
          + "\"brdId\":\"BRD-1234\","
          + "\"clientInformation\":{"
          + "\"companyName\":\"Test Company\","
          + "\"organizationID\":\"ORG-123\","
          + "\"companyDisplayName\":\"Test Company Inc.\","
          + "\"siteName\":\"Main Office\","
          + "\"siteDisplayName\":\"Test Company HQ\","
          + "\"siteAbridgedName\":\"TCO\","
          + "\"healthCareIndustry\":\"Yes\","
          + "\"addressLine1\":\"123 Main Street\","
          + "\"addressLine2\":\"Suite 500\","
          + "\"companyWebsiteAddress\":\"https://testcompany.com\","
          + "\"customerServicePhoneNumber\":\"800-555-1234\","
          + "\"customerServiceEmailAddress\":\"support@testcompany.com\","
          + "\"customerServiceHours\":\"9AM-5PM EST\","
          + "\"city\":\"Boston\","
          + "\"state\":\"MA\","
          + "\"zipCode\":\"02110\""
          + "},"
          + "\"aciInformation\":{"
          + "\"ITContactName\":\"John Smith\","
          + "\"ITContactTitle\":\"IT Manager\","
          + "\"ITContactPhone\":\"555-123-4567\","
          + "\"ITContactEmail\":\"jsmith@aci.com\","
          + "\"ITContactExtension\":\"123\""
          + "},"
          + "\"paymentChannels\":{"
          + "\"acceptBankAccounts\":\"Yes\","
          + "\"acceptCreditDebitCards\":\"Yes\","
          + "\"acceptAlternativePaymentMethods\":\"No\","
          + "\"cvvValidation\":\"Required\","
          + "\"cvvValidationApiSdk\":\"Optional\""
          + "},"
          + "\"fundingMethods\":{"
          + "\"itmLeveraged\":\"Yes\""
          + "},"
          + "\"achPaymentProcessing\":{"
          + "\"creditToBillerThreshold\":\"1000.00\","
          + "\"debitToBillerThreshold\":\"500.00\","
          + "\"achcompanyName\":\"Test Company ACH\","
          + "\"achcompanyID\":\"1234567890\","
          + "\"timeFrame\":\"Same Day\","
          + "\"blockAllPayments\":\"No\","
          + "\"achReturnResubmission\":\"Automatic\""
          + "},"
          + "\"miniAccountMaster\":{"
          + "\"mamFile\":\"Yes\","
          + "\"indicator\":\"Account Number\","
          + "\"approximateNumberOfRecords\":\"10000\","
          + "\"refreshFileName\":\"refresh.txt\","
          + "\"updateFileName\":\"update.txt\","
          + "\"transmissionProtocol\":\"SFTP\","
          + "\"timeOfDayRefresh\":\"02:00\""
          + "},"
          + "\"accountIdentifierInformation\":{"
          + "\"minimumAccountIdentifierLength\":\"8\","
          + "\"maximumAccountIdentifierLength\":\"16\","
          + "\"sample1\":\"12345678\","
          + "\"accountIdentifierValidation\":\"Numeric\","
          + "\"useValidationFormat\":\"Yes\""
          + "},"
          + "\"paymentRules\":{"
          + "\"minimumPaymentAmount\":\"1.00\","
          + "\"maximumPaymentAmount\":\"10000.00\","
          + "\"futurePaymentDays\":\"30\","
          + "\"samePaymentDays\":\"Yes\","
          + "\"pastPaymentDays\":\"0\""
          + "},"
          + "\"notifications\":{"
          + "\"fromAddress\":\"notifications@testcompany.com\","
          + "\"shortCode\":\"12345\","
          + "\"primaryBrandingColor\":\"#0066CC\","
          + "\"secondaryBrandingColor\":\"#FFFFFF\""
          + "},"
          + "\"remittance\":{"
          + "\"remittanceFile\":\"Yes\","
          + "\"remittanceFormat\":\"CSV\","
          + "\"remittanceDeliveryMethod\":\"SFTP\","
          + "\"remittanceFrequency\":\"Daily\""
          + "},"
          + "\"agentPortal\":{"
          + "\"passwordExpiration\":\"90\","
          + "\"lockoutThreshold\":\"3\","
          + "\"selectLockoutDuration\":\"30 minutes\","
          + "\"ssoAgentPortal\":\"Yes\","
          + "\"mfa\":\"Required\","
          + "\"reporting\":\"Yes\","
          + "\"paymentSearch\":\"Yes\","
          + "\"agentPaymentEntry\":\"Yes\","
          + "\"modifyPayment\":\"Yes\","
          + "\"cancelPayment\":\"Yes\","
          + "\"seriesOfPayments\":\"No\","
          + "\"usePreviousFunding\":\"Yes\","
          + "\"blockPayment\":\"Yes\","
          + "\"refundPayment\":\"Yes\","
          + "\"feeWaivers\":\"Yes\""
          + "},"
          + "\"recurringPayments\":{"
          + "\"recurringPaymentsEnabled\":\"Yes\","
          + "\"maximumRecurringAmount\":\"5000.00\","
          + "\"maximumRecurringDuration\":\"12 months\","
          + "\"recurringFrequency\":\"Monthly\""
          + "},"
          + "\"ivr\":{"
          + "\"ivrInScope\":\"Yes\","
          + "\"promptForInput\":\"Account Number\","
          + "\"transferNumber\":\"800-555-9876\","
          + "\"interDigitTimeout\":\"3\","
          + "\"ivrTimeout\":\"60\","
          + "\"billSummary\":\"Yes\","
          + "\"presentAmountDue\":\"Yes\","
          + "\"presentDueDate\":\"Yes\","
          + "\"defaultPaymentAmount\":\"Amount Due\","
          + "\"defaultPaymentDate\":\"Current Date\","
          + "\"playTermsAndConditions\":\"Yes\","
          + "\"ivrNumber1\":\"800-555-4321\""
          + "},"
          + "\"generalImplementations\":{"
          + "\"implementationNotes\":[{"
          + "\"date\":\"2023-06-01\","
          + "\"area\":\"Payment Processing\","
          + "\"note\":\"Initial setup complete\""
          + "}]"
          + "},"
          + "\"approvals\":{"
          + "\"clientSignature\":\"Jane Doe\","
          + "\"clientSignatureDate\":\"2023-06-15\","
          + "\"aciSignature\":\"John Smith\","
          + "\"aciSignatureDate\":\"2023-06-16\""
          + "},"
          + "\"revisionHistory\":{"
          + "\"revisions\":[{"
          + "\"revisionNumber\":\"1.0\","
          + "\"revisionDate\":\"2023-06-01\","
          + "\"revisionDescription\":\"Initial document\","
          + "\"revisionAuthor\":\"John Smith\""
          + "}]"
          + "}"
          + "}";
  public static final String BRD_SUCCESS_RESPONSE =
      "{\"status\":\"Successful\","
          + "\"message\":\"BRD created successfully\","
          + "\"data\":{"
          + "\"brdFormId\":\"67c165dcc663646c50222b61\","
          + "\"status\":\"DRAFT\","
          + "\"projectId\":\"PRJ-123456\","
          + "\"brdId\":\"BRD-1234\","
          + "\"clientInformation\":{"
          + "\"companyName\":\"Test Company\","
          + "\"organizationID\":\"ORG-123\","
          + "\"companyDisplayName\":\"Test Company Inc.\","
          + "\"siteName\":\"Main Office\","
          + "\"siteDisplayName\":\"Test Company HQ\","
          + "\"siteAbridgedName\":\"TCO\","
          + "\"healthCareIndustry\":\"Yes\","
          + "\"addressLine1\":\"123 Main Street\","
          + "\"addressLine2\":\"Suite 500\","
          + "\"companyWebsiteAddress\":\"https://testcompany.com\","
          + "\"customerServicePhoneNumber\":\"800-555-1234\","
          + "\"customerServiceEmailAddress\":\"support@testcompany.com\","
          + "\"customerServiceHours\":\"9AM-5PM EST\","
          + "\"city\":\"Boston\","
          + "\"state\":\"MA\","
          + "\"zipCode\":\"02110\""
          + "},"
          + "\"aciInformation\":{"
          + "\"ITContactName\":\"John Smith\","
          + "\"ITContactTitle\":\"IT Manager\","
          + "\"ITContactPhone\":\"555-123-4567\","
          + "\"ITContactEmail\":\"jsmith@aci.com\","
          + "\"ITContactExtension\":\"123\""
          + "},"
          + "\"paymentChannels\":{"
          + "\"acceptBankAccounts\":\"Yes\","
          + "\"acceptCreditDebitCards\":\"Yes\","
          + "\"acceptAlternativePaymentMethods\":\"No\","
          + "\"cvvValidation\":\"Required\","
          + "\"cvvValidationApiSdk\":\"Optional\""
          + "},"
          + "\"fundingMethods\":{"
          + "\"itmLeveraged\":\"Yes\""
          + "},"
          + "\"achPaymentProcessing\":{"
          + "\"creditToBillerThreshold\":\"1000.00\","
          + "\"debitToBillerThreshold\":\"500.00\","
          + "\"achcompanyName\":\"Test Company ACH\","
          + "\"achcompanyID\":\"1234567890\","
          + "\"timeFrame\":\"Same Day\","
          + "\"blockAllPayments\":\"No\","
          + "\"achReturnResubmission\":\"Automatic\""
          + "},"
          + "\"miniAccountMaster\":{"
          + "\"mamFile\":\"Yes\","
          + "\"indicator\":\"Account Number\","
          + "\"approximateNumberOfRecords\":\"10000\","
          + "\"refreshFileName\":\"refresh.txt\","
          + "\"updateFileName\":\"update.txt\","
          + "\"transmissionProtocol\":\"SFTP\","
          + "\"timeOfDayRefresh\":\"02:00\""
          + "},"
          + "\"accountIdentifierInformation\":{"
          + "\"minimumAccountIdentifierLength\":\"8\","
          + "\"maximumAccountIdentifierLength\":\"16\","
          + "\"sample1\":\"12345678\","
          + "\"accountIdentifierValidation\":\"Numeric\","
          + "\"useValidationFormat\":\"Yes\""
          + "},"
          + "\"paymentRules\":{"
          + "\"minimumPaymentAmount\":\"1.00\","
          + "\"maximumPaymentAmount\":\"10000.00\","
          + "\"futurePaymentDays\":\"30\","
          + "\"samePaymentDays\":\"Yes\","
          + "\"pastPaymentDays\":\"0\""
          + "},"
          + "\"notifications\":{"
          + "\"fromAddress\":\"notifications@testcompany.com\","
          + "\"shortCode\":\"12345\","
          + "\"primaryBrandingColor\":\"#0066CC\","
          + "\"secondaryBrandingColor\":\"#FFFFFF\""
          + "},"
          + "\"remittance\":{"
          + "\"remittanceFile\":\"Yes\","
          + "\"remittanceFormat\":\"CSV\","
          + "\"remittanceDeliveryMethod\":\"SFTP\","
          + "\"remittanceFrequency\":\"Daily\""
          + "},"
          + "\"agentPortal\":{"
          + "\"passwordExpiration\":\"90\","
          + "\"lockoutThreshold\":\"3\","
          + "\"selectLockoutDuration\":\"30 minutes\","
          + "\"ssoAgentPortal\":\"Yes\","
          + "\"mfa\":\"Required\","
          + "\"reporting\":\"Yes\","
          + "\"paymentSearch\":\"Yes\","
          + "\"agentPaymentEntry\":\"Yes\","
          + "\"modifyPayment\":\"Yes\","
          + "\"cancelPayment\":\"Yes\","
          + "\"seriesOfPayments\":\"No\","
          + "\"usePreviousFunding\":\"Yes\","
          + "\"blockPayment\":\"Yes\","
          + "\"refundPayment\":\"Yes\","
          + "\"feeWaivers\":\"Yes\""
          + "},"
          + "\"recurringPayments\":{"
          + "\"recurringPaymentsEnabled\":\"Yes\","
          + "\"maximumRecurringAmount\":\"5000.00\","
          + "\"maximumRecurringDuration\":\"12 months\","
          + "\"recurringFrequency\":\"Monthly\""
          + "},"
          + "\"ivr\":{"
          + "\"ivrInScope\":\"Yes\","
          + "\"promptForInput\":\"Account Number\","
          + "\"transferNumber\":\"800-555-9876\","
          + "\"interDigitTimeout\":\"3\","
          + "\"ivrTimeout\":\"60\","
          + "\"billSummary\":\"Yes\","
          + "\"presentAmountDue\":\"Yes\","
          + "\"presentDueDate\":\"Yes\","
          + "\"defaultPaymentAmount\":\"Amount Due\","
          + "\"defaultPaymentDate\":\"Current Date\","
          + "\"playTermsAndConditions\":\"Yes\","
          + "\"ivrNumber1\":\"800-555-4321\""
          + "},"
          + "\"generalImplementations\":{"
          + "\"implementationNotes\":[{"
          + "\"date\":\"2023-06-01\","
          + "\"area\":\"Payment Processing\","
          + "\"note\":\"Initial setup complete\""
          + "}]"
          + "},"
          + "\"approvals\":{"
          + "\"clientSignature\":\"Jane Doe\","
          + "\"clientSignatureDate\":\"2023-06-15\","
          + "\"aciSignature\":\"John Smith\","
          + "\"aciSignatureDate\":\"2023-06-16\""
          + "},"
          + "\"revisionHistory\":{"
          + "\"revisions\":[{"
          + "\"revisionNumber\":\"1.0\","
          + "\"revisionDate\":\"2023-06-01\","
          + "\"revisionDescription\":\"Initial document\","
          + "\"revisionAuthor\":\"John Smith\""
          + "}]"
          + "}"
          + "},"
          + "\"errors\":null}";

  public static final String BRD_VALIDATION_ERROR =
      "{\"status\":\"failure\","
          + "\"message\":\"Invalid request\","
          + "\"data\":null,"
          + "\"errors\":{\"status\":\"Invalid status value\",\"projectId\":\"Project ID is required\"}}";
  public static final String ALREADY_EXISTS =
      "{\"status\":\"failure\",\"message\":\"BRD already exists with id: BRD-1234\",\"data\":null,\"errors\":null}";

  public static final String INTERNAL_SERVER_ERROR =
      "{\"status\":\"failure\",\"message\":\"Something went wrong\",\"data\":null,\"errors\":null}";

  public static final String BRD_ID_VALIDATION_ERROR =
      "{\"status\":\"failure\","
          + "\"message\":\"Invalid request\","
          + "\"data\":null,"
          + "\"errors\":{\"brdId\":\"BRD ID cannot be blank\"}}";

  public static final String BRD_NOT_FOUND =
      "{\"status\":\"failure\","
          + "\"message\":\"BRD not found with id: BRD-9999\","
          + "\"data\":null,"
          + "\"errors\":null}";

  public static final String BRD_PATCH_REQUEST =
      """
{
    "status": "DRAFT",
    "brdId": "BRD0001",
    "clientInformation": {
        "companyName": "Deep",
        "operationContactEmail": "deep@gmail.com",
        "operationContactExtension": "96",
        "customerServiceHours": "67"
    },
    "paymentRules": {
        "minimumPaymentAmount": "5.00",
        "maximumPaymentAmount": "15000.00"
    }
}
""";
  public static final String BRD_UPDATE_SUCCESS_RESPONSE =
      """
    {
        "status": "Successful",
        "message": "BRD updated successfully",
        "data": {
            "brdFormId": "67c165dcc663646c50222b61",
            "status": "DRAFT",
            "projectId": "P0002",
            "brdId": "BRD0001",
            "clientInformation": {
                "companyName": "Deep",
                "organizationID": null,
                "companyDisplayName": null,
                "siteName": null,
                "siteDisplayName": null,
                "siteAbridgedName": null,
                "healthCareIndustry": null,
                "addressLine1": null,
                "addressLine2": null,
                "companyWebsiteAddress": null,
                "customerServicePhoneNumber": null,
                "customerServicePhoneExtension": null,
                "customerServiceEmailAddress": null,
                "clientPhoneNumber": null,
                "clientPhoneExtension": null,
                "city": null,
                "state": null,
                "zipCode": null,
                "primaryBusinessContactName": null,
                "primaryBusinessContactTitle": null,
                "primaryBusinessContactPhone": null,
                "primaryBusinessContactEmail": null,
                "primaryBusinessContactExtension": null,
                "projectManagerName": null,
                "projectManagerTitle": null,
                "projectManagerPhone": null,
                "projectManagerEmail": null,
                "projectManagerExtension": null,
                "operationsContactName": null,
                "operationsContactTitle": null,
                "operationsContactPhone": null,
                "customerServiceHours": "67",
                "operationContactEmail": "deep@gmail.com",
                "operationContactExtension": "96",
                "iTContactName": null,
                "ITContactTitle": null,
                "ITContactPhone": null,
                "ITContactEmail": null,
                "ITContactExtension": null
            },
            "aciInformation": {
                "iTContactName": null,
                "ITContactTitle": null,
                "ITContactPhone": null,
                "ITContactEmail": null,
                "ITContactExtension": null
            },
            "paymentChannels": {
                "paymentChannelsPortal": [
                    {
                        "viewValue": "Agent Portal",
                        "selected": false
                    },
                    {
                        "viewValue": "Student Portal",
                        "selected": false
                    },
                    {
                        "viewValue": "IVR (one-time pay)",
                        "selected": false
                    },
                    {
                        "viewValue": "API/SDK",
                        "selected": false
                    }
                ],
                "paymentChannelsFeatures": [
                    {
                        "viewValue": "One Time Pay",
                        "selected": false,
                        "children": null
                    },
                    {
                        "viewValue": "Future Dated Payments",
                        "selected": false,
                        "children": null
                    },
                    {
                        "viewValue": "Customer Profile Management",
                        "selected": false,
                        "children": [
                            {
                                "viewValue": "Wallets",
                                "selected": false,
                                "children": null
                            },
                            {
                                "viewValue": "Recurring Payments",
                                "selected": false,
                                "children": null
                            }
                        ]
                    }
                ],
                "acceptBankAccounts": "No",
                "acceptBankAccountsChannels": [
                    {
                        "viewValue": "Agent Portal",
                        "selected": false
                    },
                    {
                        "viewValue": "API and/or SDK (Web or CSR)",
                        "selected": false
                    },
                    {
                        "viewValue": "IVR",
                        "selected": false
                    }
                ],
                "acceptCreditDebitCards": "No",
                "acceptCreditDebitCardsChannels": [
                    {
                        "viewValue": "Agent Portal",
                        "selected": false
                    },
                    {
                        "viewValue": "API and/or SDK (Web or CSR)",
                        "selected": false
                    },
                    {
                        "viewValue": "IVR",
                        "selected": false
                    }
                ],
                "acceptAlternativePaymentMethods": "No",
                "acceptAlternativePaymentMethodsChannels": [
                    {
                        "viewValue": "Alternative Payment Methods (Requires SDKs)",
                        "selected": false,
                        "indeterminate": true,
                        "children": [
                            {
                                "viewValue": "ApplePay",
                                "selected": false
                            },
                            {
                                "viewValue": "GooglePay",
                                "selected": true
                            },
                            {
                                "viewValue": "Venmo",
                                "selected": false
                            },
                            {
                                "viewValue": "PayPal",
                                "selected": false
                            }
                        ]
                    }
                ],
                "cvvValidation": "No",
                "cvvValidationApiSdk": "No"
            },
            "fundingMethods": {
                "itmLeveraged": "No",
                "billerAbsorbs": [
                    {
                        "viewValue": "ACH",
                        "selected": false
                    },
                    {
                        "viewValue": "Cards",
                        "selected": false
                    }
                ],
                "agentPortal": [
                    {
                        "tier": "",
                        "startEndStart": "",
                        "startEndEnd": "",
                        "achFlat": "",
                        "achPercent": "",
                        "creditFlat": "",
                        "creditPercent": "",
                        "sigDebitFlat": "",
                        "sigDebitPercent": "",
                        "atmFlat": "",
                        "atmPercent": ""
                    }
                ],
                "ivr": [
                    {
                        "tier": "",
                        "startEndStart": "",
                        "startEndEnd": "",
                        "achFlat": "",
                        "achPercent": "",
                        "creditFlat": "",
                        "creditPercent": "",
                        "sigDebitFlat": "",
                        "sigDebitPercent": "",
                        "atmFlat": "",
                        "atmPercent": ""
                    }
                ],
                "apiSdk": [
                    {
                        "tier": "",
                        "startEndStart": "",
                        "startEndEnd": "",
                        "achFlat": "",
                        "achPercent": "",
                        "creditFlat": "",
                        "creditPercent": "",
                        "sigDebitFlat": "",
                        "sigDebitPercent": "",
                        "atmFlat": "",
                        "atmPercent": ""
                    }
                ]
            },
            "achPaymentProcessing": {
                "creditToBillerThreshold": "",
                "debitToBillerThreshold": "",
                "achcompanyName": "",
                "achcompanyDiscretionaryData": "",
                "achcompanyID": "",
                "achcompanyDescription": "",
                "webACHDescription": "",
                "csrACHDescription": "",
                "ivrACHDescription": "",
                "batchACHDescription": "",
                "timeFrame": "",
                "blockAllPayments": "",
                "achReturnResubmission": ""
            },
            "miniAccountMaster": {
                "mamFile": "No",
                "indicator": "No",
                "siteNameIndicator": "No",
                "allSites": "",
                "allSitesMam": "",
                "approximateNumberOfRecords": "",
                "estimatedMamFileSize": "",
                "refreshFileName": "",
                "refreshFlagFileName": "",
                "updateFileName": "",
                "updateFlagFileName": "",
                "thirdPartyMamFile": "No",
                "thirdPartyName": "",
                "transmissionProtocol": "",
                "aciSFTPAddress": "",
                "clientSFTPAddress": "",
                "encryptionKey": "",
                "encryptionKeyLocation": "",
                "emailContact": "",
                "specificDaysForRefresh": [
                    {
                        "viewValue": "Sunday",
                        "selected": false
                    },
                    {
                        "viewValue": "Monday",
                        "selected": false
                    },
                    {
                        "viewValue": "Tuesday",
                        "selected": false
                    },
                    {
                        "viewValue": "Wednesday",
                        "selected": false
                    },
                    {
                        "viewValue": "Thursday",
                        "selected": false
                    },
                    {
                        "viewValue": "Friday",
                        "selected": false
                    },
                    {
                        "viewValue": "Saturday",
                        "selected": false
                    }
                ],
                "timeOfDayRefresh": "",
                "specificDaysForUpdate": [
                    {
                        "viewValue": "Sunday",
                        "selected": false
                    },
                    {
                        "viewValue": "Monday",
                        "selected": false
                    },
                    {
                        "viewValue": "Tuesday",
                        "selected": false
                    },
                    {
                        "viewValue": "Wednesday",
                        "selected": false
                    },
                    {
                        "viewValue": "Thursday",
                        "selected": false
                    },
                    {
                        "viewValue": "Friday",
                        "selected": false
                    },
                    {
                        "viewValue": "Saturday",
                        "selected": false
                    }
                ],
                "timeOfDayFirstUpdate": "",
                "timeOfDaySecondUpdate": "",
                "timeOfDayThirdUpdate": "",
                "mamFileHoliday": "Yes",
                "mamFileHolidayTable": [
                    {
                        "holiday": "New Year's Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Martin Luther King Jr. Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Presidents' Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Memorial Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Independence Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Labor Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Columbus Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Veterans' Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Thanksgiving Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    },
                    {
                        "holiday": "Christmas Day",
                        "mamUpdateFile": "",
                        "nextProcessingDayUpdate": "",
                        "mamRefreshFile": "",
                        "nextProcessingDayRefresh": ""
                    }
                ],
                "mamFields": ""
            },
            "accountIdentifierInformation": {
                "accountIdentifierFormat": [
                    {
                        "viewValue": "Numeric",
                        "selected": false
                    },
                    {
                        "viewValue": "Alphabetic",
                        "selected": false
                    },
                    {
                        "viewValue": "Special Characters",
                        "selected": false
                    }
                ],
                "minimumAccountIdentifierLength": "",
                "maximumAccountIdentifierLength": "",
                "sample1": "",
                "sample2": "",
                "sample3": "",
                "sample4": "",
                "sample5": "",
                "accountIdentifierValidation": "Yes",
                "useValidationFormat": ""
            },
            "paymentRules": {
                "minimumPaymentAmount": "5.00",
                "maximumPaymentAmount": "15000.00",
                "futureDatedPaymentsDays": null
            },
            "notifications": {
                "consumerNotifications": [
                    {
                        "name": "Payment confirmation",
                        "purpose": "A transaction notification that is sent upon a successful payment transaction.",
                        "purposeInput": null,
                        "optOut": "Required",
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Payment voided",
                        "purpose": "A transaction notification that is sent when a payment is voided before being settled.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Payment refunded",
                        "purpose": "A transaction notification that is sent when a payment is refunded.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Payment returned",
                        "purpose": "A transaction notification that is sent when an ACH payment is returned.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Payment charged back",
                        "purpose": "A transaction notification that is sent when a payment is charged back.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Payment failed",
                        "purpose": "A transaction notification that is sent if a scheduled payment fails.",
                        "purposeInput": null,
                        "optOut": "Required",
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Future-dated payment reminder",
                        "purpose": "Reminder that a future dated payment will process in N days (default is 10 days).",
                        "purposeInput": "",
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Future-dated payment modified",
                        "purpose": "A transaction notification that is sent when the future dated payment is modified before it is processed.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "ACH payment modified",
                        "purpose": "A transaction notification that is sent when an ACH payment is modified before it is processed.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": "Not available"
                    },
                    {
                        "name": "Recurring payment scheduled/created",
                        "purpose": "A recurring payment notification that is sent when recurring payments are scheduled.",
                        "purposeInput": null,
                        "optOut": "Required",
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment modified",
                        "purpose": "A recurring payment notification that is sent when a recurring payment schedule is modified.",
                        "purposeInput": null,
                        "optOut": "Required",
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment paused",
                        "purpose": "A recurring payment notification that is sent when a recurring payment schedule is paused.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment resumed",
                        "purpose": "A recurring payment notification that is sent when a recurring payment schedule is resumed.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment skipped",
                        "purpose": "A recurring payment notification that is sent when a recurring payment occurrence is skipped.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment canceled",
                        "purpose": "A recurring payment notification that is sent when the recurring payment is canceled.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment completed",
                        "purpose": "A recurring payment notification that is sent when a recurring payment has reached the end date.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Recurring payment card expiring",
                        "purpose": "A recurring payment notification that is sent on the first of the month when a card funding account is expiring at the end of the current month, and that funding account is used for a recurring payment.",
                        "purposeInput": null,
                        "optOut": false,
                        "email": false,
                        "sms": false
                    },
                    {
                        "name": "Upcoming recurring payment notice",
                        "purpose": "A recurring payment notification that is sent N days (default is 10 days) before a recurring payment is scheduled to be paid as a reminder to the payer. Specify the desired number of days below: ",
                        "purposeInput": "",
                        "optOut": false,
                        "email": false,
                        "sms": false
                    }
                ],
                "fromAddress": "",
                "shortCode": "",
                "shortCodeType": [
                    {
                        "viewValue": "Client provided short code",
                        "selected": false
                    },
                    {
                        "viewValue": "ACI provided short code",
                        "selected": false
                    }
                ],
                "primaryBrandingColor": "",
                "secondaryBrandingColor": ""
            },
            "remittance": {
                "remittanceInformation": "File",
                "cutoffTime": "",
                "receiveRemittanceFile": "No",
                "aciId": "",
                "additionalRemittanceData": "",
                "additionalReversalData": "",
                "transactionVolume": "",
                "deliveryTime": "",
                "remittanceFileName": "",
                "reversalFileName": "",
                "transmissionProtocol": "",
                "aciSFTPAddress": "",
                "clientSFTPAddress": "",
                "encryptionKey": "",
                "encryptionKeyLocation": "",
                "observingHolidays": "No",
                "holidays": [
                    {
                        "holiday": "New Year's Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Martin Luther King Jr. Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Presidents' Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Memorial Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Independence Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Labor Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Columbus Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Veterans' Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Thanksgiving Day",
                        "mamUpdateFile": ""
                    },
                    {
                        "holiday": "Christmas Day",
                        "mamUpdateFile": ""
                    }
                ]
            },
            "agentPortal": {
                "proxyForMultipleAddresses": "",
                "passwordExpiration": "",
                "lockoutThreshold": "",
                "selectLockoutDuration": "",
                "ssoAgentPortal": "Yes",
                "redirectURL": [
                    {
                        "viewValue": null,
                        "selected": false
                    },
                    {
                        "viewValue": null,
                        "selected": false
                    }
                ],
                "metadataFile": "",
                "ssoClientCertificate": "",
                "idpOrSp": "",
                "samlEndPoint": "",
                "mfa": "",
                "logoFileName": "",
                "logoFileType": "",
                "reporting": "",
                "paymentSearch": "",
                "agentPaymentEntry": "",
                "modifyPayment": "",
                "cancelPayment": "",
                "seriesOfPayments": "",
                "usePreviousFunding": "",
                "blockPayment": "",
                "refundPayment": "",
                "refundType": [
                    {
                        "viewValue": "Courtesy",
                        "selected": false
                    },
                    {
                        "viewValue": "Dispute",
                        "selected": false
                    },
                    {
                        "viewValue": "Manager Approval",
                        "selected": false
                    },
                    {
                        "viewValue": "Payment Misapplication",
                        "selected": false
                    },
                    {
                        "viewValue": "Statement Error / Billing Dispute",
                        "selected": false
                    },
                    {
                        "viewValue": "Other",
                        "selected": false
                    }
                ],
                "refundReasons": [
                    {
                        "viewValue": "Payment made in error",
                        "selected": false
                    },
                    {
                        "viewValue": "Refund for fees",
                        "selected": false
                    },
                    {
                        "viewValue": "Collector error",
                        "selected": false
                    },
                    {
                        "viewValue": "Customer satisfaction",
                        "selected": false
                    },
                    {
                        "viewValue": "System error",
                        "selected": false
                    },
                    {
                        "viewValue": "Fraudulent Payment",
                        "selected": false
                    },
                    {
                        "viewValue": "Customer Requested",
                        "selected": false
                    },
                    {
                        "viewValue": "SR Error",
                        "selected": false
                    },
                    {
                        "viewValue": "Duplicate Payment",
                        "selected": false
                    },
                    {
                        "viewValue": "Unauthorized Transaction",
                        "selected": false
                    },
                    {
                        "viewValue": "Customer Error",
                        "selected": false
                    },
                    {
                        "viewValue": "Processor Late",
                        "selected": false
                    }
                ],
                "feeWaivers": "",
                "agentPortal": [
                    {
                        "visibleAgentConsumer": "",
                        "dataEntryType": "",
                        "requiredYorN": "",
                        "validationExpression": ""
                    }
                ]
            },
            "recurringPayments": {
                "paymentDateOptions": [
                    {
                        "viewValue": "On Due Date (requires MAM file)",
                        "selected": false
                    },
                    {
                        "viewValue": "N Number of Days Before Due Date (up to 10 days) (requires MAM file)",
                        "selected": false
                    },
                    {
                        "viewValue": "Fixed Day of the Month (ex 1st Friday of every month)",
                        "selected": false
                    },
                    {
                        "viewValue": "Fixed Date of the Month (ex 15th of every month)",
                        "selected": false
                    },
                    {
                        "viewValue": "Semi Monthly (ex 1st and 15th of the month)",
                        "selected": false
                    },
                    {
                        "viewValue": "Weekly / Biweekly by Day (ex Occurs every week on Friday / Occurs every two weeks on Friday)",
                        "selected": false
                    }
                ],
                "paymentAmountOptions": [
                    {
                        "viewValue": "Amount Due (requires MAM file)",
                        "selected": false
                    },
                    {
                        "viewValue": "Minimum Amount Due (requires MAM data file load)",
                        "selected": false
                    },
                    {
                        "viewValue": "Fixed Amount",
                        "selected": false
                    }
                ],
                "durationOptions": [
                    {
                        "viewValue": "Until Canceled",
                        "selected": false
                    },
                    {
                        "viewValue": "Until Selected End Date",
                        "selected": false
                    },
                    {
                        "viewValue": "Until Total Amount of Payments Threshold is reached",
                        "selected": false
                    },
                    {
                        "viewValue": "Until Set Number of Payment Transactions are reached",
                        "selected": false
                    }
                ],
                "recurringPaymentThreshold": "",
                "recurringPaymentThresholdValue": ""
            },
            "ivr": {
                "ivrInScope": "",
                "fundingPrompts": [
                    {
                        "fundingType": "Card",
                        "orderPresented": ""
                    },
                    {
                        "fundingType": "Band Account",
                        "orderPresented": ""
                    }
                ],
                "achSubType": [
                    {
                        "achOrderPresented": "",
                        "bankAcountSubType": "Personal Checking (includes mutual fund draft & credit unions)"
                    },
                    {
                        "achOrderPresented": "",
                        "bankAcountSubType": "Personal Savings"
                    },
                    {
                        "achOrderPresented": "",
                        "bankAcountSubType": "Business Checking"
                    },
                    {
                        "achOrderPresented": "",
                        "bankAcountSubType": "Business Savings"
                    }
                ],
                "promptForInput": "",
                "transferNumber": "",
                "supportedLanguages": [
                    {
                        "viewValue": "English",
                        "selected": false
                    },
                    {
                        "viewValue": "Spanish",
                        "selected": false
                    }
                ],
                "interDigitTimeout": "",
                "ivrTimeout": "No",
                "securityKeys": "",
                "securityKey1": "",
                "securityKey2": "",
                "billSummary": "",
                "presentAmountDue": "",
                "presentDueDate": "",
                "presentMinimumAmountDue": "",
                "presentBillDate": "",
                "entryValidationPrompts": [
                    {
                        "field": "Billing Account Number",
                        "validation": ""
                    },
                    {
                        "field": "Security Key 1",
                        "validation": ""
                    },
                    {
                        "field": "Security Key 2",
                        "validation": ""
                    },
                    {
                        "field": "Payment (Remit) Amount",
                        "validation": ""
                    },
                    {
                        "field": "Payment Date",
                        "validation": ""
                    },
                    {
                        "field": "Card Number (and other card funding data)",
                        "validation": ""
                    },
                    {
                        "field": "ACH Routing Number",
                        "validation": ""
                    },
                    {
                        "field": "ACH Account Number",
                        "validation": ""
                    },
                    {
                        "field": "Payment Authorization",
                        "validation": ""
                    }
                ],
                "defaultPaymentAmount": "Other MAM Field",
                "defaultPaymentDate": "None",
                "paymentInputPromptBehavior": "",
                "fundingInputPromptBehavior": "",
                "playTermsAndConditions": "No",
                "ivrNumber1": "",
                "dnis1": "",
                "ivrNumber2": "",
                "dnis2": "",
                "ivrNumber3": "",
                "dnis3": "",
                "ivrNumber4": "",
                "dnis4": "",
                "ivrNumber5": "",
                "dnis5": ""
            },
            "generalImplementations": {
                "implementationNotes": [
                    {
                        "date": "",
                        "area": "",
                        "note": ""
                    }
                ]
            },
            "approvals": {
                "clientSignature": "",
                "clientSignatureDate": "",
                "aciSignature": "",
                "aciSignatureDate": ""
            },
            "revisionHistory": {
                "revisionHistory": [
                    {
                        "date": "",
                        "description": "",
                        "version": ""
                    }
                ]
            }
        },
        "errors": null
    }
    """;

  public static final String BRD_SECTION_SUCCESS_RESPONSE =
      """
    {
      "status": "Successful",
      "message": "BRD section found successfully",
      "data": {
        "brdFormId": "BRD-1234",
        "brdId": "BRD-2023-001",
        "sectionName": "fundingMethods",
        "sectionData": {
          "acceptBankAccounts": "Yes",
          "acceptCreditDebitCards": "Yes",
          "acceptAlternativePaymentMethods": "No"
        }
      },
      "errors": null
    }
    """;

  public static final String BRD_SECTION_VALIDATION_ERROR =
      """
    {
      "status": "Failed",
      "message": "Validation Error",
      "data": null,
      "errors": {
        "sectionName": "Invalid section name: invalidSection. Valid sections are: clientInformation, aciInformation, paymentChannels, fundingMethods, achPaymentProcessing, miniAccountMaster, accountIdentifierInformation, paymentRules, notifications, remittance, agentPortal, recurringPayments, ivr, generalImplementations, approvals, revisionHistory"
      }
    }
    """;
  public static final String BRD_SEARCH_SUCCESS_RESPONSE =
      """
{
    "status": "Successful",
    "message": "Found 4 results",
    "data": {
        "content": [
            {
                "brdFormId": "67d1120bede69672a8c90354",
                "brdId": "BRD0001",
                "brdName": "BRD NAME",
                "creator": "Deep",
                "type": "BRD Types",
                "status": "Draft",
                "testRigorTimeStamp": "2024-03-20T10:30:00",
                "testRigorFlag": true
            }
        ],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 1,
            "sort": {
                "empty": false,
                "unsorted": false,
                "sorted": true
            },
            "offset": 0,
            "paged": true,
            "unpaged": false
        },
        "numberOfElements": 1,
        "totalPages": 4,
        "first": true,
        "last": false,
        "totalElements": 4,
        "size": 1,
        "number": 0,
        "sort": {
            "empty": false,
            "unsorted": false,
            "sorted": true
        },
        "empty": false
    },
    "errors": null
}
""";

  public static final String SEARCH_QUERY_VALIDATION_ERROR =
      """
{
    "status": "failure",
    "message": "Validation errors occurred",
    "data": null,
    "errors": {
        "searchBRDs.page": "Page number cannot be negative"
    }
}
""";

  public static final String BRD_LIST_VALIDATION_ERROR =
      """
                          {
                                "status": "failure",
                                "message": "Bad Request",
                                "data": null,
                                "errors": {
                                    "error": "Page number must be non-negative and size must be greater than 0."
                                }
                            }
                          """;

  public static final String BRD_LIST_NOT_FOUND_ERROR =
      """
                          {
                                 "status": "failure",
                                 "message": "Not Found Exception",
                                 "data": null,
                                 "errors": {
                                     "message": "No BRD sections found for the given pagination parameters."
                                 }
                             }
                          """;

  public static final String BRD_LIST_SUCCESS_RESPONSE =
      """
{
    "status": "Successful",
    "message": "BRD list retrieved successfully",
    "data": {
        "totalCount": 5,
        "brdList": [
            {
                "brdId": "BRD0003",
                "brdFormId": "67d01b13484aaf60a61db452",
                "creator": "Olivia Martin",
                "type": "V4-Configuration",
                "notes": "Pending review for additional input",
                "status": "Draft",
                "testRigorTimeStamp": "2024-03-20T10:30:00",
                "testRigorFlag": true
            },
            {
                "brdId": "BRD0003",
                "brdFormId": "67d01a4f08e7975ed539f474",
                "creator": "Olivia Martin",
                "type": "V4-Configuration",
                "notes": "Pending review for additional input",
                "status": "Draft",
                "testRigorTimeStamp": "2024-03-20T10:30:00",
                "testRigorFlag": false
            },
            {
                "brdId": "BRD0003",
                "brdFormId": "67d0119352f2d00d0bc82680",
                "creator": "Olivia Martin",
                "type": "V4-Configuration",
                "notes": "Pending review for additional input",
                "status": "Draft",
                "testRigorTimeStamp": "2024-03-20T10:30:00",
                "testRigorFlag": true
            },
            {
                "brdId": "BRD0003",
                "brdFormId": "67d00e1265d5b80bc2e0263c",
                "creator": "Olivia Martin",
                "type": "V4-Configuration",
                "notes": "Pending review for additional input",
                "status": "Draft",
                "testRigorTimeStamp": "2024-03-20T10:30:00",
                "testRigorFlag": false
            }
        ]
    },
    "errors": null
}
""";

  public static final String INVALID_FILE_FORMAT_SIZE =
      """
                              {
                                     "status": "failure",
                                     "message": "Invalid file format or size",
                                     "data": null,
                                     "errors": {
                                         "message": "No BRD sections found for the given pagination parameters."
                                     }
                                 }
                              """;

  public static final String FILE_UPLOAD_SUCCESS_RESPONSE =
      """
                              {
                                "status": "Successful",
                                "message": "File uploaded successfully",
                                "data": null,
                                "errors": null
                              }
                              """;

  public static final String BRD_STATUS_UPDATE_SUCCESS =
      """
      {
          "status": "Successful",
          "message": "BRD status updated successfully",
          "data": {
              "brdFormId": "67c165dcc663646c50222b61",
              "status": "IN_REVIEW",
              "projectId": "PRJ-123456",
              "brdId": "BRD-1234"
          },
          "errors": null
      }
      """;

  public static final String BRD_STATUS_UPDATE_BAD_REQUEST =
      """
      {
          "status": "failure",
          "message": "Invalid status value",
          "data": null,
          "errors": {
              "status": "Status must be one of: DRAFT, IN_REVIEW, APPROVED, REJECTED"
          }
      }
      """;

  public static final String BRD_STATUS_UPDATE_NOT_FOUND =
      """
      {
          "status": "failure",
          "message": "BRD not found with id: BRD-9999",
          "data": null,
          "errors": null
      }
      """;

  public static final String BRD_STATUS_UPDATE_FORBIDDEN =
      """
      {
          "status": "failure",
          "message": "Access denied. You do not have permission to update this BRD's status",
          "data": null,
          "errors": null
      }
      """;

  public static final String BRD_STATUS_UPDATE_UNAUTHORIZED =
      """
      {
          "status": "failure",
          "message": "Authentication required to perform this operation",
          "data": null,
          "errors": null
      }
      """;

  public static final String BRD_STATUS_UPDATE_SERVER_ERROR =
      """
      {
          "status": "failure",
          "message": "An internal server error occurred while updating the BRD status",
          "data": null,
          "errors": null
      }
      """;
}
