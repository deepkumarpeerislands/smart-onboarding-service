package com.aci.smart_onboarding.dto;

import java.util.List;
import lombok.Data;

@Data
public class BrdFormRequest {
  private String projectId;
  private String status;
  private String brdId;
  private String customerId;
  private String brdName;
  private String description;
  private ClientInformation clientInformation;
  private AciInformation aciInformation;
  private PaymentChannels paymentChannels;
  private FundingMethods fundingMethods;
  private AchPaymentProcessing achPaymentProcessing;
  private MiniAccountMaster miniAccountMaster;
  private AccountIdentifierInformation accountIdentifierInformation;
  private PaymentRules paymentRules;
  private Notifications notifications;
  private Remittance remittance;
  private AgentPortal agentPortal;
  private RecurringPayments recurringPayments;
  private Ivr ivr;
  private GeneralImplementations generalImplementations;
  private Approvals approvals;
  private RevisionHistoryField revisionHistory;
  private String creator;
  private String type;
  private String notes;

  @Data
  public static class ClientInformation {
    private String companyName;
    private String organizationID;
    private String companyDisplayName;
    private String siteName;
    private String siteDisplayName;
    private String siteAbridgedName;
    private String healthCareIndustry;
    private String addressLine1;
    private String addressLine2;
    private String companyWebsiteAddress;
    private String customerServicePhoneNumber;
    private String customerServicePhoneExtension;
    private String customerServiceEmailAddress;
    private String customerServiceHours;
    private String clientPhoneNumber;
    private String clientPhoneExtension;
    private String city;
    private String state;
    private String zipCode;
    private String primaryBusinessContactName;
    private String primaryBusinessContactTitle;
    private String primaryBusinessContactPhone;
    private String primaryBusinessContactEmail;
    private String primaryBusinessContactExtension;
    private String projectManagerName;
    private String projectManagerTitle;
    private String projectManagerPhone;
    private String projectManagerEmail;
    private String projectManagerExtension;
    private String operationsContactName;
    private String operationsContactTitle;
    private String operationsContactPhone;
    private String operationContactEmail;
    private String operationContactExtension;
    private String iTContactName;
    private String iTContactTitle;
    private String iTContactPhone;
    private String iTContactEmail;
    private String iTContactExtension;
    private String status;
  }

  @Data
  public static class AciInformation {
    private String iTContactName;
    private String iTContactTitle;
    private String iTContactPhone;
    private String iTContactEmail;
    private String iTContactExtension;
  }

  @Data
  public static class PaymentChannels {
    private List<PaymentChannel> paymentChannelsPortal;
    private List<PaymentChannel> paymentChannelsFeatures;
    private String acceptBankAccounts;
    private List<PaymentChannel> acceptBankAccountsChannels;
    private String acceptCreditDebitCards;
    private List<PaymentChannel> acceptCreditDebitCardsChannels;
    private String acceptAlternativePaymentMethods;
    private List<PaymentChannel> acceptAlternativePaymentMethodsChannels;
    private String cvvValidation;
    private String cvvValidationApiSdk;
  }

  @Data
  public static class PaymentChannel {
    private String viewValue;
    private boolean selected;
    private boolean indeterminate;
    private List<PaymentChannel> children;
  }

  @Data
  public static class FundingMethods {
    private String itmLeveraged;
    private String processingFeeHandling;
    private List<PaymentChannel> billerAbsorbs;
    private List<PaymentChannel> consumerPays;
    private List<FundingTier> agentPortal;
    private List<FundingTier> ivr;
    private List<FundingTier> apiSdk;
  }

  @Data
  public static class FundingTier {
    private String tier;
    private String startEndStart;
    private String startEndEnd;
    private String achFlat;
    private String achPercent;
    private String creditFlat;
    private String creditPercent;
    private String sigDebitFlat;
    private String sigDebitPercent;
    private String atmFlat;
    private String atmPercent;
  }

  @Data
  public static class AchPaymentProcessing {
    private String creditToBillerThreshold;
    private String debitToBillerThreshold;
    private String achcompanyName;
    private String achcompanyDiscretionaryData;
    private String achcompanyID;
    private String achcompanyDescription;
    private String webACHDescription;
    private String csrACHDescription;
    private String ivrACHDescription;
    private String batchACHDescription;
    private String fundingNSFValidation;
    private String timeFrame;
    private String blockAllPayments;
    private String achReturnResubmission;
    private String resubmissionTimes;
  }

  @Data
  public static class MiniAccountMaster {
    private String mamFile;
    private String indicator;
    private String siteNameIndicator;
    private String allSites;
    private String allSitesMam;
    private String approximateNumberOfRecords;
    private String estimatedMamFileSize;
    private String refreshFileName;
    private String refreshFlagFileName;
    private String updateFileName;
    private String updateFlagFileName;
    private String thirdPartyMamFile;
    private String thirdPartyName;
    private String transmissionProtocol;
    private String aciSFTPAddress;
    private String clientSFTPAddress;
    private String encryptionKey;
    private String encryptionKeyLocation;
    private String emailContact;
    private List<PaymentChannel> specificDaysForRefresh;
    private String timeOfDayRefresh;
    private List<PaymentChannel> specificDaysForUpdate;
    private String timeOfDayFirstUpdate;
    private String timeOfDaySecondUpdate;
    private String timeOfDayThirdUpdate;
    private String mamFileHoliday;
    private List<MamFileHoliday> mamFileHolidayTable;
    private String mamFields;
  }

  @Data
  public static class MamFileHoliday {
    private String holiday;
    private String mamUpdateFile;
    private String nextProcessingDayUpdate;
    private String mamRefreshFile;
    private String nextProcessingDayRefresh;
  }

  @Data
  public static class AccountIdentifierInformation {
    private List<PaymentChannel> accountIdentifierFormat;
    private String minimumAccountIdentifierLength;
    private String maximumAccountIdentifierLength;
    private String sample1;
    private String sample2;
    private String sample3;
    private String sample4;
    private String sample5;
    private String accountIdentifierValidation;
    private String useValidationFormat;
  }

  @Data
  public static class PaymentRules {
    private String minimumPaymentAmount;
    private String maximumPaymentAmount;
    private String futureDatedPaymentsDays;
  }

  @Data
  public static class Notifications {
    private List<ConsumerNotification> consumerNotifications;
    private String fromAddress;
    private String shortCode;
    private List<PaymentChannel> shortCodeType;
    private String primaryBrandingColor;
    private String secondaryBrandingColor;
  }

  @Data
  public static class ConsumerNotification {
    private String name;
    private String purpose;
    private Object optOut;
    private boolean email;
    private boolean sms;
    private String purposeInput;
  }

  @Data
  public static class Remittance {
    private String remittanceInformation;
    private String cutoffTime;
    private String receiveRemittanceFile;
    private String aciId;
    private String additionalRemittanceData;
    private String additionalReversalData;
    private String transactionVolume;
    private String deliveryTime;
    private String remittanceFileName;
    private String reversalFileName;
    private String transmissionProtocol;
    private String aciSFTPAddress;
    private String clientSFTPAddress;
    private String encryptionKey;
    private String encryptionKeyLocation;
    private String observingHolidays;
    private List<HolidayFields> holidays;
  }

  @Data
  public static class HolidayFields {
    private String holiday;
    private String mamUpdateFile;
  }

  @Data
  public static class AgentPortal {
    private String proxyForMultipleAddresses;
    private String passwordExpiration;
    private String lockoutThreshold;
    private String selectLockoutDuration;
    private String lockoutDuration;
    private String ssoAgentPortal;
    private List<RedirectURL> redirectURL;
    private String metadataFile;
    private String ssoClientCertificate;
    private String idpOrSp;
    private String samlEndPoint;
    private String mfa;
    private String customBillerLogo;
    private String logoFileName;
    private String logoFileType;
    private String fromAddress;
    private String replyToAddress;
    private String reporting;
    private String paymentSearch;
    private String agentPaymentEntry;
    private String modifyPayment;
    private String cancelPayment;
    private String seriesOfPayments;
    private String usePreviousFunding;
    private String blockPayment;
    private String refundPayment;
    private List<PaymentChannel> refundType;
    private List<PaymentChannel> refundReasons;
    private String feeWaivers;
    private List<PaymentChannel> feeWaiveReasons;
    private List<AgentPortalField> agentPortalFields;
  }

  @Data
  public static class RedirectURL {
    private String environment;
    private String value;
  }

  @Data
  public static class AgentPortalField {
    private String visibleAgentConsumer;
    private String dataEntryType;
    private String requiredYorN;
    private String validationExpression;
  }

  @Data
  public static class RecurringPayments {
    private List<PaymentChannel> paymentDateOptions;
    private List<PaymentChannel> paymentAmountOptions;
    private List<PaymentChannel> durationOptions;
    private String recurringPaymentThreshold;
    private String recurringPaymentThresholdValue;
  }

  @Data
  public static class Ivr {
    private String ivrInScope;
    private List<FundingPrompt> fundingPrompts;
    private List<AchSubType> achSubType;
    private String promptForInput;
    private String transferNumber;
    private List<PaymentChannel> supportedLanguages;
    private String interDigitTimeout;
    private String ivrTimeout;
    private String securityKeys;
    private String securityKey1;
    private String securityKey2;
    private String billSummary;
    private String presentAmountDue;
    private String presentDueDate;
    private String presentMinimumAmountDue;
    private String presentBillDate;
    private List<EntryValidationPrompt> entryValidationPrompts;
    private String defaultPaymentAmount;
    private String defaultPaymentDate;
    private String paymentInputPromptBehavior;
    private String fundingInputPromptBehavior;
    private String playTermsAndConditions;
    private String ivrNumber1;
    private String dnis1;
    private String ivrNumber2;
    private String dnis2;
    private String ivrNumber3;
    private String dnis3;
    private String ivrNumber4;
    private String dnis4;
    private String ivrNumber5;
    private String dnis5;
  }

  @Data
  public static class FundingPrompt {
    private String fundingType;
    private String orderPresented;
  }

  @Data
  public static class AchSubType {
    private String achOrderPresented;
    private String bankAcountSubType;
  }

  @Data
  public static class EntryValidationPrompt {
    private String field;
    private String validation;
  }

  @Data
  public static class GeneralImplementations {
    private List<ImplementationNote> implementationNotes;
  }

  @Data
  public static class ImplementationNote {
    private String date;
    private String area;
    private String note;
  }

  @Data
  public static class Approvals {
    private String clientSignature;
    private String clientSignatureDate;
    private String aciSignature;
    private String aciSignatureDate;
  }

  @Data
  public static class RevisionHistoryField {
    private List<Revision> revisionHistory;
  }

  @Data
  public static class Revision {
    private String date;
    private String description;
    private String version;
  }
}
