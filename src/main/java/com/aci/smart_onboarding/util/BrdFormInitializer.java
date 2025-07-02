package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.util.brd_form.*;
import java.util.ArrayList;
import java.util.List;

public final class BrdFormInitializer {

  private BrdFormInitializer() {}

  private static final String DEFAULT_STRING = "";
  private static final String REQUIRED_VALUE = "Required";
  private static final String NOT_AVAILABLE = "Not available";
  private static final String TRANSACTION_NOTIFICATION = "A transaction notification that is sent ";
  private static final String RECURRING_NOTIFICATION =
      "A recurring payment notification that is sent ";
  private static final String PAYMENT_NOTIFICATION_PREFIX = "Payment ";
  private static final String RECURRING_PAYMENT_PREFIX = "Recurring payment ";

  private static final String ACH_PAYMENT_MODIFIED = "ACH payment modified";
  private static final String FUTURE_DATED_REMINDER = "Future-dated payment reminder";
  private static final String FUTURE_DATED_MODIFIED = "Future-dated payment modified";
  private static final String UPCOMING_RECURRING_NOTICE = "Upcoming recurring payment notice";

  public static ClientInformation createClientInformation() {
    ClientInformation clientInfo = new ClientInformation();
    clientInfo.setCompanyName(DEFAULT_STRING);
    clientInfo.setCustomerId(DEFAULT_STRING);
    clientInfo.setCompanyDisplayName(DEFAULT_STRING);
    clientInfo.setSiteName(DEFAULT_STRING);
    clientInfo.setSiteDisplayName(DEFAULT_STRING);
    clientInfo.setSiteAbridgedName(DEFAULT_STRING);
    clientInfo.setHealthCareIndustry(DEFAULT_STRING);
    clientInfo.setAddressLine1(DEFAULT_STRING);
    clientInfo.setAddressLine2(DEFAULT_STRING);
    clientInfo.setCompanyWebsiteAddress(DEFAULT_STRING);
    clientInfo.setCustomerServicePhoneNumber(DEFAULT_STRING);
    clientInfo.setCustomerServicePhoneExtension(DEFAULT_STRING);
    clientInfo.setCustomerServiceEmailAddress(DEFAULT_STRING);
    clientInfo.setCustomerServiceHours(DEFAULT_STRING);
    clientInfo.setClientPhoneNumber(DEFAULT_STRING);
    clientInfo.setClientPhoneExtension(DEFAULT_STRING);
    clientInfo.setCity(DEFAULT_STRING);
    clientInfo.setState(DEFAULT_STRING);
    clientInfo.setZipCode(DEFAULT_STRING);
    clientInfo.setPrimaryBusinessContactName(DEFAULT_STRING);
    clientInfo.setPrimaryBusinessContactTitle(DEFAULT_STRING);
    clientInfo.setPrimaryBusinessContactPhone(DEFAULT_STRING);
    clientInfo.setPrimaryBusinessContactEmail(DEFAULT_STRING);
    clientInfo.setPrimaryBusinessContactExtension(DEFAULT_STRING);
    clientInfo.setProjectManagerName(DEFAULT_STRING);
    clientInfo.setProjectManagerTitle(DEFAULT_STRING);
    clientInfo.setProjectManagerPhone(DEFAULT_STRING);
    clientInfo.setProjectManagerEmail(DEFAULT_STRING);
    clientInfo.setProjectManagerExtension(DEFAULT_STRING);
    clientInfo.setOperationsContactName(DEFAULT_STRING);
    clientInfo.setOperationsContactTitle(DEFAULT_STRING);
    clientInfo.setOperationsContactPhone(DEFAULT_STRING);
    clientInfo.setOperationContactEmail(DEFAULT_STRING);
    clientInfo.setOperationContactExtension(DEFAULT_STRING);
    clientInfo.setITContactName(DEFAULT_STRING);
    clientInfo.setITContactTitle(DEFAULT_STRING);
    clientInfo.setITContactPhone(DEFAULT_STRING);
    clientInfo.setITContactEmail(DEFAULT_STRING);
    clientInfo.setITContactExtension(DEFAULT_STRING);
    clientInfo.setSectionStatus(DEFAULT_STRING);
    return clientInfo;
  }

  public static AciInformation createAciInformation() {
    AciInformation aciInfo = new AciInformation();
    aciInfo.setITContactName(DEFAULT_STRING);
    aciInfo.setITContactTitle(DEFAULT_STRING);
    aciInfo.setITContactPhone(DEFAULT_STRING);
    aciInfo.setITContactEmail(DEFAULT_STRING);
    aciInfo.setITContactExtension(DEFAULT_STRING);
    aciInfo.setSectionStatus(DEFAULT_STRING);
    return aciInfo;
  }

  public static PaymentChannels createPaymentChannels() {
    PaymentChannels channels = new PaymentChannels();

    List<ViewValueAndSelected> portalList = new ArrayList<>();
    portalList.add(createViewValueAndSelected("Agent Portal", true));
    portalList.add(createViewValueAndSelected("Student Portal", false));
    portalList.add(createViewValueAndSelected("IVR (one-time pay)", false));
    portalList.add(createViewValueAndSelected("API/SDK", false));
    channels.setPaymentChannelsPortal(portalList);

    List<PaymentChannelFeature> featuresList = new ArrayList<>();

    PaymentChannelFeature oneTimePay = new PaymentChannelFeature();
    oneTimePay.setViewValue("One Time Pay");
    oneTimePay.setSelected(true);
    oneTimePay.setIndeterminate(false);
    featuresList.add(oneTimePay);

    PaymentChannelFeature futureDated = new PaymentChannelFeature();
    futureDated.setViewValue("Future Dated Payments");
    futureDated.setSelected(true);
    futureDated.setIndeterminate(false);
    featuresList.add(futureDated);

    PaymentChannelFeature profileManagement = new PaymentChannelFeature();
    profileManagement.setViewValue("Customer Profile Management");
    profileManagement.setSelected(true);
    profileManagement.setIndeterminate(false);

    List<ViewValueAndSelected> profileChildren = new ArrayList<>();
    profileChildren.add(createViewValueAndSelected("Wallets", true));
    profileChildren.add(createViewValueAndSelected("Recurring Payments", true));
    profileManagement.setChildren(profileChildren);
    featuresList.add(profileManagement);

    channels.setPaymentChannelsFeatures(featuresList);

    channels.setAcceptBankAccounts(DEFAULT_STRING);

    List<PaymentChannelFeature> bankAccountChannels = new ArrayList<>();

    PaymentChannelFeature agentPortal = new PaymentChannelFeature();
    agentPortal.setViewValue("Agent Portal");
    agentPortal.setSelected(true);
    agentPortal.setIndeterminate(false);
    bankAccountChannels.add(agentPortal);

    PaymentChannelFeature apiSdk = new PaymentChannelFeature();
    apiSdk.setViewValue("API and/or SDK (Web or CSR)");
    apiSdk.setSelected(true);
    apiSdk.setIndeterminate(false);

    List<ViewValueAndSelected> apiSdkChildren = new ArrayList<>();
    apiSdkChildren.add(createViewValueAndSelected("Credit Cards", true));
    apiSdkChildren.add(createViewValueAndSelected("Signature Debit Cards", true));
    apiSdkChildren.add(createViewValueAndSelected("ATM Pinless Debit Cards", true));
    apiSdkChildren.add(createViewValueAndSelected("Visa", true));
    apiSdkChildren.add(createViewValueAndSelected("Mastercard", true));
    apiSdkChildren.add(createViewValueAndSelected("Discover", true));
    apiSdkChildren.add(createViewValueAndSelected("American Express", true));
    apiSdk.setChildren(apiSdkChildren);
    bankAccountChannels.add(apiSdk);

    PaymentChannelFeature ivr = new PaymentChannelFeature();
    ivr.setViewValue("IVR");
    ivr.setSelected(true);
    ivr.setIndeterminate(false);
    bankAccountChannels.add(ivr);

    channels.setAcceptBankAccountsChannels(bankAccountChannels);

    channels.setAcceptCreditDebitCards(DEFAULT_STRING);
    channels.setAcceptCreditDebitCardsChannels(
        deepCopy(bankAccountChannels)); // Use the same structure

    channels.setAcceptAlternativePaymentMethods(DEFAULT_STRING);

    List<PaymentChannelFeature> altPaymentMethodsChannels = new ArrayList<>();
    PaymentChannelFeature altMethods = new PaymentChannelFeature();
    altMethods.setViewValue("Alternative Payment Methods (Requires SDKs)");
    altMethods.setSelected(true);
    altMethods.setIndeterminate(false);

    List<ViewValueAndSelected> altMethodsChildren = new ArrayList<>();
    altMethodsChildren.add(createViewValueAndSelected("ApplePay", true));
    altMethodsChildren.add(createViewValueAndSelected("GooglePay", true));
    altMethodsChildren.add(createViewValueAndSelected("Venmo", true));
    altMethodsChildren.add(createViewValueAndSelected("PayPal", true));
    altMethods.setChildren(altMethodsChildren);

    altPaymentMethodsChannels.add(altMethods);
    channels.setAcceptAlternativePaymentMethodsChannels(altPaymentMethodsChannels);

    channels.setCvvValidation(DEFAULT_STRING);
    channels.setCvvValidationApiSdk(DEFAULT_STRING);
    channels.setSectionStatus(DEFAULT_STRING);
    return channels;
  }

  private static ViewValueAndSelected createViewValueAndSelected(
      String viewValue, boolean selected) {
    ViewValueAndSelected item = new ViewValueAndSelected();
    item.setViewValue(viewValue);
    item.setSelected(selected);
    return item;
  }

  private static List<PaymentChannelFeature> deepCopy(List<PaymentChannelFeature> original) {
    List<PaymentChannelFeature> copy = new ArrayList<>();

    for (PaymentChannelFeature feature : original) {
      PaymentChannelFeature featureCopy = new PaymentChannelFeature();
      featureCopy.setViewValue(feature.getViewValue());
      featureCopy.setSelected(feature.isSelected());
      featureCopy.setIndeterminate(feature.isIndeterminate());

      if (feature.getChildren() != null) {
        List<ViewValueAndSelected> childrenCopy = new ArrayList<>();
        for (ViewValueAndSelected child : feature.getChildren()) {
          childrenCopy.add(createViewValueAndSelected(child.getViewValue(), child.isSelected()));
        }
        featureCopy.setChildren(childrenCopy);
      }

      copy.add(featureCopy);
    }

    return copy;
  }

  public static FundingMethods createFundingMethods() {
    FundingMethods methods = new FundingMethods();
    methods.setItmLeveraged(DEFAULT_STRING);
    methods.setProcessingFeeHandling(DEFAULT_STRING);

    List<BillerAbsorbs> billerAbsorbs = new ArrayList<>();
    BillerAbsorbs billerAbsorb1 = new BillerAbsorbs();
    billerAbsorb1.setViewValue("ACH");
    billerAbsorb1.setSelected(false);
    billerAbsorbs.add(billerAbsorb1);

    BillerAbsorbs billerAbsorb2 = new BillerAbsorbs();
    billerAbsorb2.setViewValue("Cards");
    billerAbsorb2.setSelected(false);
    billerAbsorbs.add(billerAbsorb2);

    methods.setBillerAbsorbs(billerAbsorbs);

    List<ViewValueAndSelected> consumerPays = new ArrayList<>();

    ViewValueAndSelected consumerPay1 = new ViewValueAndSelected();
    consumerPay1.setViewValue("ACH");
    consumerPay1.setSelected(true);
    consumerPays.add(consumerPay1);

    ViewValueAndSelected consumerPay2 = new ViewValueAndSelected();
    consumerPay2.setViewValue("Cards");
    consumerPay2.setSelected(true);
    consumerPays.add(consumerPay2);

    methods.setConsumerPays(consumerPays);

    List<CommonFundingDetails> agentPortal = new ArrayList<>();
    CommonFundingDetails agentPortalDetails = new CommonFundingDetails();
    agentPortalDetails.setTier(DEFAULT_STRING);
    agentPortalDetails.setStartEndStart(DEFAULT_STRING);
    agentPortalDetails.setStartEndEnd(DEFAULT_STRING);
    agentPortalDetails.setAchFlat(DEFAULT_STRING);
    agentPortalDetails.setAchPercent(DEFAULT_STRING);
    agentPortalDetails.setCreditFlat(DEFAULT_STRING);
    agentPortalDetails.setCreditPercent(DEFAULT_STRING);
    agentPortalDetails.setSigDebitFlat(DEFAULT_STRING);
    agentPortalDetails.setSigDebitPercent(DEFAULT_STRING);
    agentPortalDetails.setAtmFlat(DEFAULT_STRING);
    agentPortalDetails.setAtmPercent(DEFAULT_STRING);
    agentPortal.add(agentPortalDetails);
    methods.setAgentPortal(agentPortal);

    methods.setIvr(deepCopyCommonFundingDetails(agentPortal));
    methods.setApiSdk(deepCopyCommonFundingDetails(agentPortal));

    methods.setSectionStatus(DEFAULT_STRING);
    return methods;
  }

  private static List<CommonFundingDetails> deepCopyCommonFundingDetails(
      List<CommonFundingDetails> original) {
    List<CommonFundingDetails> copy = new ArrayList<>();

    for (CommonFundingDetails details : original) {
      CommonFundingDetails detailsCopy = new CommonFundingDetails();
      detailsCopy.setTier(details.getTier());
      detailsCopy.setStartEndStart(details.getStartEndStart());
      detailsCopy.setStartEndEnd(details.getStartEndEnd());
      detailsCopy.setAchFlat(details.getAchFlat());
      detailsCopy.setAchPercent(details.getAchPercent());
      detailsCopy.setCreditFlat(details.getCreditFlat());
      detailsCopy.setCreditPercent(details.getCreditPercent());
      detailsCopy.setSigDebitFlat(details.getSigDebitFlat());
      detailsCopy.setSigDebitPercent(details.getSigDebitPercent());
      detailsCopy.setAtmFlat(details.getAtmFlat());
      detailsCopy.setAtmPercent(details.getAtmPercent());
      copy.add(detailsCopy);
    }

    return copy;
  }

  public static AchPaymentProcessing createAchPaymentProcessing() {
    AchPaymentProcessing ach = new AchPaymentProcessing();
    ach.setCreditToBillerThreshold(DEFAULT_STRING);
    ach.setDebitToBillerThreshold(DEFAULT_STRING);
    ach.setAchcompanyName(DEFAULT_STRING);
    ach.setAchcompanyDiscretionaryData(DEFAULT_STRING);
    ach.setAchcompanyID(DEFAULT_STRING);
    ach.setAchcompanyDescription(DEFAULT_STRING);
    ach.setWebACHDescription(DEFAULT_STRING);
    ach.setCsrACHDescription(DEFAULT_STRING);
    ach.setIvrACHDescription(DEFAULT_STRING);
    ach.setBatchACHDescription(DEFAULT_STRING);
    ach.setTimeFrame(DEFAULT_STRING);
    ach.setBlockAllPayments(DEFAULT_STRING);
    ach.setAchReturnResubmission(DEFAULT_STRING);
    ach.setFundingNSFValidation(DEFAULT_STRING);
    ach.setResubmissionTimes(DEFAULT_STRING);
    ach.setSectionStatus(DEFAULT_STRING);
    return ach;
  }

  public static MiniAccountMaster createMiniAccountMaster() {
    MiniAccountMaster master = new MiniAccountMaster();
    master.setMamFile(DEFAULT_STRING);
    master.setIndicator(DEFAULT_STRING);
    master.setSiteNameIndicator(DEFAULT_STRING);
    master.setAllSites(DEFAULT_STRING);
    master.setAllSitesMam(DEFAULT_STRING);
    master.setApproximateNumberOfRecords(DEFAULT_STRING);
    master.setEstimatedMamFileSize(DEFAULT_STRING);
    master.setRefreshFileName(DEFAULT_STRING);
    master.setRefreshFlagFileName(DEFAULT_STRING);
    master.setUpdateFileName(DEFAULT_STRING);
    master.setUpdateFlagFileName(DEFAULT_STRING);
    master.setThirdPartyMamFile(DEFAULT_STRING);
    master.setThirdPartyName(DEFAULT_STRING);
    master.setTransmissionProtocol(DEFAULT_STRING);
    master.setAciSFTPAddress(DEFAULT_STRING);
    master.setClientSFTPAddress(DEFAULT_STRING);
    master.setEncryptionKey(DEFAULT_STRING);
    master.setEncryptionKeyLocation(DEFAULT_STRING);
    master.setEmailContact(DEFAULT_STRING);
    master.setMamTransmissionRefresh(DEFAULT_STRING);

    // Initialize specificDaysForRefresh with values from JSON
    List<ViewValueAndSelected> daysForRefresh = new ArrayList<>();
    daysForRefresh.add(createViewValueAndSelected("Sunday", false));
    daysForRefresh.add(createViewValueAndSelected("Monday", false));
    daysForRefresh.add(createViewValueAndSelected("Tuesday", false));
    daysForRefresh.add(createViewValueAndSelected("Wednesday", false));
    daysForRefresh.add(createViewValueAndSelected("Thursday", false));
    daysForRefresh.add(createViewValueAndSelected("Friday", false));
    daysForRefresh.add(createViewValueAndSelected("Saturday", false));
    master.setSpecificDaysForRefresh(daysForRefresh);

    master.setTimeOfDayRefresh(DEFAULT_STRING);
    master.setMamTransmissionUpdate(DEFAULT_STRING);

    master.setSpecificDaysForUpdate(new ArrayList<>(daysForRefresh));

    master.setTimeOfDayFirstUpdate(DEFAULT_STRING);
    master.setTimeOfDaySecondUpdate(DEFAULT_STRING);
    master.setTimeOfDayThirdUpdate(DEFAULT_STRING);
    master.setMamFileHoliday(DEFAULT_STRING);

    List<MamFileHoliday> holidays = new ArrayList<>();

    String[] holidayNames = {
      "New Year's Day",
      "Martin Luther King Jr. Day",
      "Presidents' Day",
      "Memorial Day",
      "Independence Day",
      "Labor Day",
      "Columbus Day",
      "Veterans' Day",
      "Thanksgiving Day",
      "Christmas Day"
    };

    for (String holidayName : holidayNames) {
      MamFileHoliday holiday = new MamFileHoliday();
      holiday.setHoliday(holidayName);
      holiday.setMamUpdateFile(DEFAULT_STRING);
      holiday.setNextProcessingDayUpdate(DEFAULT_STRING);
      holiday.setMamRefreshFile(DEFAULT_STRING);
      holiday.setNextProcessingDayRefresh(DEFAULT_STRING);
      holidays.add(holiday);
    }

    master.setMamFileHolidayTable(holidays);

    master.setMamFields(DEFAULT_STRING);
    master.setAllSitesFile(DEFAULT_STRING);
    master.setSectionStatus(DEFAULT_STRING);
    return master;
  }

  public static AccountIdentifierInformation createAccountIdentifierInformation() {
    AccountIdentifierInformation info = new AccountIdentifierInformation();

    // Initialize accountIdentifierFormat with values from JSON
    List<ViewValueAndSelected> formats = new ArrayList<>();
    formats.add(createViewValueAndSelected("Numeric", false));
    formats.add(createViewValueAndSelected("Alphabetic", false));
    formats.add(createViewValueAndSelected("Special Characters", false));
    info.setAccountIdentifierFormat(formats);

    info.setMinimumAccountIdentifierLength(DEFAULT_STRING);
    info.setMaximumAccountIdentifierLength(DEFAULT_STRING);
    info.setSample1(DEFAULT_STRING);
    info.setSample2(DEFAULT_STRING);
    info.setSample3(DEFAULT_STRING);
    info.setSample4(DEFAULT_STRING);
    info.setSample5(DEFAULT_STRING);
    info.setAccountIdentifierValidation(DEFAULT_STRING);
    info.setUseValidationFormat(DEFAULT_STRING);
    info.setSectionStatus(DEFAULT_STRING);
    return info;
  }

  public static PaymentRules createPaymentRules() {
    PaymentRules rules = new PaymentRules();
    rules.setGlobalLimits(DEFAULT_STRING);
    rules.setMinimumPaymentAmount(DEFAULT_STRING);
    rules.setMaximumPaymentAmount(DEFAULT_STRING);
    rules.setFutureDatedPayments(DEFAULT_STRING);
    rules.setFutureDatedPaymentsDays(DEFAULT_STRING);
    rules.setSectionStatus(DEFAULT_STRING);
    return rules;
  }

  public static Notifications createNotifications() {
    Notifications notifications = new Notifications();
    notifications.setSendNotifications(DEFAULT_STRING);

    List<ConsumerNotifications> notificationsList = new ArrayList<>();

    String[][] notificationData = {
      {
        PAYMENT_NOTIFICATION_PREFIX + "confirmation",
        TRANSACTION_NOTIFICATION + "upon a successful payment transaction.",
        REQUIRED_VALUE
      },
      {
        PAYMENT_NOTIFICATION_PREFIX + "voided",
        TRANSACTION_NOTIFICATION + "when a payment is voided before being settled.",
        null
      },
      {
        PAYMENT_NOTIFICATION_PREFIX + "refunded",
        TRANSACTION_NOTIFICATION + "when a payment is refunded.",
        null
      },
      {
        PAYMENT_NOTIFICATION_PREFIX + "returned",
        TRANSACTION_NOTIFICATION + "when an ACH payment is returned.",
        null
      },
      {
        PAYMENT_NOTIFICATION_PREFIX + "charged back",
        TRANSACTION_NOTIFICATION + "when a payment is charged back.",
        null
      },
      {
        PAYMENT_NOTIFICATION_PREFIX + "failed",
        TRANSACTION_NOTIFICATION + "if a scheduled payment fails.",
        REQUIRED_VALUE
      },
      {
        FUTURE_DATED_REMINDER,
        "Reminder that a future dated payment will process in N days (default is 10 days).",
        null
      },
      {
        FUTURE_DATED_MODIFIED,
        TRANSACTION_NOTIFICATION
            + "when the future dated payment is modified before it is processed.",
        null
      },
      {
        ACH_PAYMENT_MODIFIED,
        TRANSACTION_NOTIFICATION + "when an ACH payment is modified before it is processed.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "scheduled/created",
        RECURRING_NOTIFICATION + "when recurring payments are scheduled.",
        REQUIRED_VALUE
      },
      {
        RECURRING_PAYMENT_PREFIX + "modified",
        RECURRING_NOTIFICATION + "when a recurring payment schedule is modified.",
        REQUIRED_VALUE
      },
      {
        RECURRING_PAYMENT_PREFIX + "paused",
        RECURRING_NOTIFICATION + "when a recurring payment schedule is paused.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "resumed",
        RECURRING_NOTIFICATION + "when a recurring payment schedule is resumed.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "skipped",
        RECURRING_NOTIFICATION + "when a recurring payment occurrence is skipped.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "canceled",
        RECURRING_NOTIFICATION + "when the recurring payment is canceled.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "completed",
        RECURRING_NOTIFICATION + "when a recurring payment has reached the end date.",
        null
      },
      {
        RECURRING_PAYMENT_PREFIX + "card expiring",
        RECURRING_NOTIFICATION
            + "on the first of the month when a card funding account is expiring at the end of the current month, and that funding account is used for a recurring payment.",
        null
      },
      {
        UPCOMING_RECURRING_NOTICE,
        RECURRING_NOTIFICATION
            + "N days (default is 10 days) before a recurring payment is scheduled to be paid as a reminder to the payer. Specify the desired number of days below: ",
        null
      }
    };

    for (String[] data : notificationData) {
      ConsumerNotifications notification = new ConsumerNotifications();
      notification.setName(data[0]);
      notification.setPurpose(data[1]);

      if (data[0].equals(ACH_PAYMENT_MODIFIED)) {
        notification.setSms(NOT_AVAILABLE);
      } else {
        notification.setSms(false);
      }

      notification.setEmail(false);

      if (data[2] != null) {
        notification.setOptOut(data[2]);
      } else {
        notification.setOptOut(false);
      }

      if (data[0].equals(FUTURE_DATED_REMINDER) || data[0].equals(UPCOMING_RECURRING_NOTICE)) {
        notification.setPurposeInput(DEFAULT_STRING);
      }

      notificationsList.add(notification);
    }

    notifications.setConsumerNotifications(notificationsList);

    notifications.setFromAddress(DEFAULT_STRING);
    notifications.setShortCode(DEFAULT_STRING);

    List<ViewValueAndSelected> shortCodeTypes = new ArrayList<>();
    shortCodeTypes.add(createViewValueAndSelected("Client provided short code", false));
    shortCodeTypes.add(createViewValueAndSelected("ACI provided short code", false));
    notifications.setShortCodeType(shortCodeTypes);

    notifications.setPrimaryBrandingColor(DEFAULT_STRING);
    notifications.setSecondaryBrandingColor(DEFAULT_STRING);
    notifications.setSectionStatus(DEFAULT_STRING);
    return notifications;
  }

  public static Remittance createRemittance() {
    Remittance remittance = new Remittance();
    remittance.setRemittanceInformation(DEFAULT_STRING);
    remittance.setCutoffTime(DEFAULT_STRING);
    remittance.setReceiveRemittanceFile(DEFAULT_STRING);
    remittance.setAciId(DEFAULT_STRING);
    remittance.setAdditionalRemittanceData(DEFAULT_STRING);
    remittance.setAdditionalReversalData(DEFAULT_STRING);
    remittance.setTransactionVolume(DEFAULT_STRING);
    remittance.setDeliveryTime(DEFAULT_STRING);
    remittance.setRemittanceFileName(DEFAULT_STRING);
    remittance.setReversalFileName(DEFAULT_STRING);
    remittance.setTransmissionProtocol(DEFAULT_STRING);
    remittance.setAciSFTPAddress(DEFAULT_STRING);
    remittance.setClientSFTPAddress(DEFAULT_STRING);
    remittance.setEncryptionKey(DEFAULT_STRING);
    remittance.setEncryptionKeyLocation(DEFAULT_STRING);
    remittance.setTransmissionFrequency(DEFAULT_STRING);
    remittance.setObservingHolidays(DEFAULT_STRING);

    remittance.setSectionStatus(DEFAULT_STRING);
    return remittance;
  }

  public static AgentPortalConfig createAgentPortalConfig() {
    AgentPortalConfig config = new AgentPortalConfig();
    config.setProxyForMultipleAddresses(DEFAULT_STRING);
    config.setPasswordExpiration(DEFAULT_STRING);
    config.setLockoutThreshold(DEFAULT_STRING);
    config.setSelectLockoutDuration(DEFAULT_STRING);
    config.setLockoutDuration(DEFAULT_STRING);
    config.setSsoAgentPortal(DEFAULT_STRING);

    List<EnvValue> redirectURL = new ArrayList<>();
    EnvValue uat = new EnvValue();
    uat.setEnvironment("UAT");
    uat.setValue(DEFAULT_STRING);
    redirectURL.add(uat);

    EnvValue prod = new EnvValue();
    prod.setEnvironment("Production");
    prod.setValue(DEFAULT_STRING);
    redirectURL.add(prod);

    config.setRedirectURL(redirectURL);

    config.setMetadataFile(DEFAULT_STRING);
    config.setSsoClientCertificate(DEFAULT_STRING);
    config.setIdpOrSp(DEFAULT_STRING);
    config.setSamlEndPoint(DEFAULT_STRING);
    config.setMfa(DEFAULT_STRING);
    config.setCustomBillerLogo(DEFAULT_STRING);
    config.setLogoFileName(DEFAULT_STRING);
    config.setLogoFileType(DEFAULT_STRING);
    config.setFromAddress(DEFAULT_STRING);
    config.setReplyToAddress(DEFAULT_STRING);
    config.setReporting(DEFAULT_STRING);
    config.setPaymentSearch(DEFAULT_STRING);
    config.setAgentPaymentEntry(DEFAULT_STRING);
    config.setModifyPayment(DEFAULT_STRING);
    config.setCancelPayment(DEFAULT_STRING);
    config.setSeriesOfPayments(DEFAULT_STRING);
    config.setUsePreviousFunding(DEFAULT_STRING);
    config.setBlockPayment(DEFAULT_STRING);
    config.setRefundPayment(DEFAULT_STRING);

    List<ViewValueAndSelected> refundTypes = new ArrayList<>();
    refundTypes.add(
        createViewValueAndSelected("Single Full Refund (both principal and fee)", true));
    refundTypes.add(
        createViewValueAndSelected(
            "Split Full Refund (full refund of principal and separate full refund of fee)", false));
    refundTypes.add(
        createViewValueAndSelected(
            "Split Partial Refund (partial or full refund of principal and separate full refund of fee)",
            false));
    config.setRefundType(refundTypes);

    List<ViewValueAndSelected> refundReasons = new ArrayList<>();
    String[] reasons = {
      "Payment made in error", "Refund for fees", "Collector error",
      "Customer satisfaction", "System error", "Fraudulent Payment",
      "Customer Requested", "SR Error", "Duplicate Payment",
      "Unauthorized Transaction", "Customer Error", "Processor Late"
    };

    for (String reason : reasons) {
      refundReasons.add(createViewValueAndSelected(reason, false));
    }
    config.setRefundReasons(refundReasons);

    config.setFeeWaivers(DEFAULT_STRING);

    List<ViewValueAndSelected> feeWaiveReasons = new ArrayList<>();
    feeWaiveReasons.add(createViewValueAndSelected("Courtesy", true));
    feeWaiveReasons.add(createViewValueAndSelected("Dispute", false));
    feeWaiveReasons.add(createViewValueAndSelected("Manager Approval", false));
    feeWaiveReasons.add(createViewValueAndSelected("Payment Misapplication", false));
    feeWaiveReasons.add(createViewValueAndSelected("Statement Error / Billing Dispute", false));
    feeWaiveReasons.add(createViewValueAndSelected("Other", false));
    config.setFeeWaiveReasons(feeWaiveReasons);

    List<BillSummary> billSummaryItems = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      BillSummary item = new BillSummary();
      item.setBillSummaryItem(String.valueOf(i));
      item.setMamField("Field" + i);
      item.setItemLable("Label" + i);
      billSummaryItems.add(item);
    }
    config.setBillSummaryItems(billSummaryItems);

    List<BillSummary> billSummaryDetail = new ArrayList<>();
    for (int i = 1; i <= 16; i++) {
      BillSummary detail = new BillSummary();
      detail.setBillSummaryItem(String.valueOf(i));
      detail.setMamField("DetailField" + i);
      detail.setItemLable("DetailLabel" + i);
      billSummaryDetail.add(detail);
    }
    config.setBillSummaryDetail(billSummaryDetail);

    List<AgentPortal> agentPortal = new ArrayList<>();
    AgentPortal portal = new AgentPortal();
    portal.setVisibleAgentConsumer(DEFAULT_STRING);
    portal.setDataEntryType(DEFAULT_STRING);
    portal.setRequiredYorN(DEFAULT_STRING);
    portal.setValidationExpression(DEFAULT_STRING);
    agentPortal.add(portal);
    config.setAgentPortal(agentPortal);

    config.setSectionStatus(DEFAULT_STRING);
    return config;
  }

  public static RecurringPayments createRecurringPayments() {
    RecurringPayments payments = new RecurringPayments();

    List<ViewValueAndSelected> dateOptions = new ArrayList<>();
    ViewValueAndSelected option1 = new ViewValueAndSelected();
    option1.setViewValue("On Due Date (requires MAM file)");
    option1.setSelected(false);
    dateOptions.add(option1);

    ViewValueAndSelected option2 = new ViewValueAndSelected();
    option2.setViewValue("N Number of Days Before Due Date (up to 10 days) (requires MAM file)");
    option2.setSelected(false);
    dateOptions.add(option2);

    ViewValueAndSelected option3 = new ViewValueAndSelected();
    option3.setViewValue("Fixed Day of the Month (ex 1st Friday of every month)");
    option3.setSelected(false);
    dateOptions.add(option3);

    ViewValueAndSelected option4 = new ViewValueAndSelected();
    option4.setViewValue("Fixed Date of the Month (ex 15th of every month)");
    option4.setSelected(false);
    dateOptions.add(option4);

    ViewValueAndSelected option5 = new ViewValueAndSelected();
    option5.setViewValue("Semi Monthly (ex 1st and 15th of the month)");
    option5.setSelected(false);
    dateOptions.add(option5);

    ViewValueAndSelected option6 = new ViewValueAndSelected();
    option6.setViewValue(
        "Weekly / Biweekly by Day (ex Occurs every week on Friday / Occurs every two weeks on Friday)");
    option6.setSelected(false);
    dateOptions.add(option6);

    payments.setPaymentDateOptions(dateOptions);

    List<ViewValueAndSelected> amountOptions = new ArrayList<>();

    ViewValueAndSelected amountOption1 = new ViewValueAndSelected();
    amountOption1.setViewValue("Amount Due (requires MAM file)");
    amountOption1.setSelected(false);
    amountOptions.add(amountOption1);

    ViewValueAndSelected amountOption2 = new ViewValueAndSelected();
    amountOption2.setViewValue("Minimum Amount Due (requires MAM data file load)");
    amountOption2.setSelected(false);
    amountOptions.add(amountOption2);

    ViewValueAndSelected amountOption3 = new ViewValueAndSelected();
    amountOption3.setViewValue("Fixed Amount");
    amountOption3.setSelected(false);
    amountOptions.add(amountOption3);

    payments.setPaymentAmountOptions(amountOptions);

    List<ViewValueAndSelected> durationOptions = new ArrayList<>();

    ViewValueAndSelected durationOption1 = new ViewValueAndSelected();
    durationOption1.setViewValue("Until Canceled");
    durationOption1.setSelected(false);
    durationOptions.add(durationOption1);

    ViewValueAndSelected durationOption2 = new ViewValueAndSelected();
    durationOption2.setViewValue("Until Selected End Date");
    durationOption2.setSelected(false);
    durationOptions.add(durationOption2);

    ViewValueAndSelected durationOption3 = new ViewValueAndSelected();
    durationOption3.setViewValue("Until Total Amount of Payments Threshold is reached");
    durationOption3.setSelected(false);
    durationOptions.add(durationOption3);

    ViewValueAndSelected durationOption4 = new ViewValueAndSelected();
    durationOption4.setViewValue("Until Set Number of Payment Transactions are reached");
    durationOption4.setSelected(false);
    durationOptions.add(durationOption4);

    payments.setDurationOptions(durationOptions);

    payments.setRecurringPaymentThreshold(DEFAULT_STRING);
    payments.setRecurringPaymentThresholdValue(DEFAULT_STRING);
    payments.setSectionStatus(DEFAULT_STRING);
    return payments;
  }

  public static Ivr createIvr() {
    Ivr ivr = new Ivr();
    ivr.setIvrInScope(DEFAULT_STRING);

    List<FundingPrompt> fundingPrompts = new ArrayList<>();

    FundingPrompt cardPrompt = new FundingPrompt();
    cardPrompt.setFundingType("Card");
    cardPrompt.setOrderPresented(DEFAULT_STRING);
    fundingPrompts.add(cardPrompt);

    FundingPrompt bankPrompt = new FundingPrompt();
    bankPrompt.setFundingType("Bank Account");
    bankPrompt.setOrderPresented(DEFAULT_STRING);
    fundingPrompts.add(bankPrompt);

    ivr.setFundingPrompts(fundingPrompts);

    List<AchSubType> achSubTypes = new ArrayList<>();

    AchSubType type1 = new AchSubType();
    type1.setAchOrderPresented(DEFAULT_STRING);
    type1.setBankAcountSubType("Personal Checking (includes mutual fund draft & credit unions)");
    achSubTypes.add(type1);

    AchSubType type2 = new AchSubType();
    type2.setAchOrderPresented(DEFAULT_STRING);
    type2.setBankAcountSubType("Personal Savings");
    achSubTypes.add(type2);

    AchSubType type3 = new AchSubType();
    type3.setAchOrderPresented(DEFAULT_STRING);
    type3.setBankAcountSubType("Business Checking");
    achSubTypes.add(type3);

    AchSubType type4 = new AchSubType();
    type4.setAchOrderPresented(DEFAULT_STRING);
    type4.setBankAcountSubType("Business Savings");
    achSubTypes.add(type4);

    ivr.setAchSubType(achSubTypes);

    ivr.setPromptForInput(DEFAULT_STRING);
    ivr.setTransferNumber(DEFAULT_STRING);

    List<ViewValueAndSelected> languages = new ArrayList<>();

    ViewValueAndSelected lang1 = new ViewValueAndSelected();
    lang1.setViewValue("English");
    lang1.setSelected(false);
    languages.add(lang1);

    ViewValueAndSelected lang2 = new ViewValueAndSelected();
    lang2.setViewValue("Spanish");
    lang2.setSelected(false);
    languages.add(lang2);

    ivr.setSupportedLanguages(languages);

    ivr.setInterDigitTimeout(DEFAULT_STRING);
    ivr.setIvrTimeout(DEFAULT_STRING);
    ivr.setSecurityKeys(DEFAULT_STRING);
    ivr.setSecurityKey1(DEFAULT_STRING);
    ivr.setSecurityKey2(DEFAULT_STRING);
    ivr.setBillSummary(DEFAULT_STRING);
    ivr.setPresentAmountDue(DEFAULT_STRING);
    ivr.setPresentDueDate(DEFAULT_STRING);
    ivr.setPresentMinimumAmountDue(DEFAULT_STRING);
    ivr.setPresentBillDate(DEFAULT_STRING);

    List<EntryValidationPrompt> validationPrompts = new ArrayList<>();

    EntryValidationPrompt prompt1 = new EntryValidationPrompt();
    prompt1.setField("Billing Account Number");
    prompt1.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt1);

    EntryValidationPrompt prompt2 = new EntryValidationPrompt();
    prompt2.setField("Security Key 1");
    prompt2.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt2);

    EntryValidationPrompt prompt3 = new EntryValidationPrompt();
    prompt3.setField("Security Key 2");
    prompt3.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt3);

    EntryValidationPrompt prompt4 = new EntryValidationPrompt();
    prompt4.setField("Payment (Remit) Amount");
    prompt4.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt4);

    EntryValidationPrompt prompt5 = new EntryValidationPrompt();
    prompt5.setField("Payment Date");
    prompt5.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt5);

    EntryValidationPrompt prompt6 = new EntryValidationPrompt();
    prompt6.setField("Card Number (and other card funding data)");
    prompt6.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt6);

    EntryValidationPrompt prompt7 = new EntryValidationPrompt();
    prompt7.setField("ACH Routing Number");
    prompt7.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt7);

    EntryValidationPrompt prompt8 = new EntryValidationPrompt();
    prompt8.setField("ACH Account Number");
    prompt8.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt8);

    EntryValidationPrompt prompt9 = new EntryValidationPrompt();
    prompt9.setField("Payment Authorization");
    prompt9.setValidation(DEFAULT_STRING);
    validationPrompts.add(prompt9);

    ivr.setEntryValidationPrompts(validationPrompts);

    ivr.setDefaultPaymentAmount(DEFAULT_STRING);
    ivr.setOtherMAMFieldMethod(DEFAULT_STRING);
    ivr.setDefaultPaymentDate(DEFAULT_STRING);
    ivr.setPaymentInputPromptBehavior(DEFAULT_STRING);
    ivr.setFundingInputPromptBehavior(DEFAULT_STRING);
    ivr.setPlayTermsAndConditions(DEFAULT_STRING);
    ivr.setIvrNumber1(DEFAULT_STRING);
    ivr.setDnis1(DEFAULT_STRING);
    ivr.setIvrNumber2(DEFAULT_STRING);
    ivr.setDnis2(DEFAULT_STRING);
    ivr.setIvrNumber3(DEFAULT_STRING);
    ivr.setDnis3(DEFAULT_STRING);
    ivr.setIvrNumber4(DEFAULT_STRING);
    ivr.setDnis4(DEFAULT_STRING);
    ivr.setIvrNumber5(DEFAULT_STRING);
    ivr.setDnis5(DEFAULT_STRING);
    ivr.setSectionStatus(DEFAULT_STRING);
    return ivr;
  }

  public static GeneralImplementations createGeneralImplementations() {
    GeneralImplementations implementations = new GeneralImplementations();

    List<ImplementationNote> notes = new ArrayList<>();

    ImplementationNote note = new ImplementationNote();
    note.setDate(DEFAULT_STRING);
    note.setArea(DEFAULT_STRING);
    note.setNote(DEFAULT_STRING);
    notes.add(note);

    implementations.setImplementationNotes(notes);
    implementations.setSectionStatus(DEFAULT_STRING);
    return implementations;
  }

  public static Approvals createApprovals() {
    Approvals approvals = new Approvals();
    approvals.setClientSignature(DEFAULT_STRING);
    approvals.setClientSignatureDate(DEFAULT_STRING);
    approvals.setAciSignature(DEFAULT_STRING);
    approvals.setAciSignatureDate(DEFAULT_STRING);
    approvals.setSectionStatus(DEFAULT_STRING);
    return approvals;
  }

  public static RevisionHistory createRevisionHistory() {
    RevisionHistory history = new RevisionHistory();

    history.setSectionStatus(DEFAULT_STRING);
    return history;
  }
}
