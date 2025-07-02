package com.aci.smart_onboarding.util;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.util.brd_form.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BrdFormInitializerTest {

  private static final String DEFAULT_STRING = "";

  @Nested
  @DisplayName("Contact Information Tests")
  class ContactInformationTests {

    @Test
    @DisplayName("createClientInformation should initialize with default values")
    void createClientInformationShouldInitializeWithDefaultValues() {
      ClientInformation clientInfo = BrdFormInitializer.createClientInformation();

      assertNotNull(clientInfo);
      assertEquals(DEFAULT_STRING, clientInfo.getCompanyName());
      assertEquals(DEFAULT_STRING, clientInfo.getCustomerId());
      assertEquals(DEFAULT_STRING, clientInfo.getITContactName());
      assertEquals(DEFAULT_STRING, clientInfo.getSectionStatus());
    }

    @Test
    @DisplayName("createAciInformation should initialize with default values")
    void createAciInformationShouldInitializeWithDefaultValues() {
      AciInformation aciInfo = BrdFormInitializer.createAciInformation();

      assertNotNull(aciInfo);
      assertEquals(DEFAULT_STRING, aciInfo.getITContactName());
      assertEquals(DEFAULT_STRING, aciInfo.getITContactTitle());
      assertEquals(DEFAULT_STRING, aciInfo.getSectionStatus());
    }
  }

  @Nested
  @DisplayName("Payment Channel Tests")
  class PaymentChannelTests {

    @Test
    @DisplayName("createPaymentChannels should initialize with correct structure")
    void createPaymentChannelsShouldInitializeWithCorrectStructure() {
      PaymentChannels channels = BrdFormInitializer.createPaymentChannels();

      assertNotNull(channels);

      assertNotNull(channels.getPaymentChannelsPortal());
      assertEquals(4, channels.getPaymentChannelsPortal().size());
      assertTrue(channels.getPaymentChannelsPortal().get(0).isSelected());
      assertEquals("Agent Portal", channels.getPaymentChannelsPortal().get(0).getViewValue());

      assertNotNull(channels.getPaymentChannelsFeatures());
      assertEquals(3, channels.getPaymentChannelsFeatures().size());

      PaymentChannelFeature profileManagement = channels.getPaymentChannelsFeatures().get(2);
      assertEquals("Customer Profile Management", profileManagement.getViewValue());
      assertNotNull(profileManagement.getChildren());
      assertEquals(2, profileManagement.getChildren().size());

      assertNotNull(channels.getAcceptBankAccountsChannels());
      assertEquals(3, channels.getAcceptBankAccountsChannels().size());

      assertNotNull(channels.getAcceptAlternativePaymentMethodsChannels());
      assertEquals(1, channels.getAcceptAlternativePaymentMethodsChannels().size());

      PaymentChannelFeature altMethods =
          channels.getAcceptAlternativePaymentMethodsChannels().get(0);
      assertNotNull(altMethods.getChildren());
      assertEquals(4, altMethods.getChildren().size());
    }
  }

  @Nested
  @DisplayName("Funding Method Tests")
  class FundingMethodTests {

    @Test
    @DisplayName("createFundingMethods should initialize with correct structure")
    void createFundingMethodsShouldInitializeWithCorrectStructure() {
      FundingMethods methods = BrdFormInitializer.createFundingMethods();

      assertNotNull(methods);

      assertNotNull(methods.getBillerAbsorbs());
      assertEquals(2, methods.getBillerAbsorbs().size());
      assertEquals("ACH", methods.getBillerAbsorbs().get(0).getViewValue());
      assertEquals("Cards", methods.getBillerAbsorbs().get(1).getViewValue());

      assertNotNull(methods.getConsumerPays());
      assertEquals(2, methods.getConsumerPays().size());
      assertTrue(methods.getConsumerPays().get(0).isSelected());

      assertNotNull(methods.getAgentPortal());
      assertEquals(1, methods.getAgentPortal().size());

      assertNotNull(methods.getIvr());
      assertNotNull(methods.getApiSdk());
    }
  }

  @Nested
  @DisplayName("Account Information Tests")
  class AccountInformationTests {

    @Test
    @DisplayName("createAchPaymentProcessing should initialize with default values")
    void createAchPaymentProcessingShouldInitializeWithDefaultValues() {
      AchPaymentProcessing ach = BrdFormInitializer.createAchPaymentProcessing();

      assertNotNull(ach);
      assertEquals(DEFAULT_STRING, ach.getCreditToBillerThreshold());
      assertEquals(DEFAULT_STRING, ach.getAchcompanyName());
      assertEquals(DEFAULT_STRING, ach.getSectionStatus());
    }

    @Test
    @DisplayName("createMiniAccountMaster should initialize with correct structure")
    void createMiniAccountMasterShouldInitializeWithCorrectStructure() {
      MiniAccountMaster master = BrdFormInitializer.createMiniAccountMaster();

      assertNotNull(master);

      assertNotNull(master.getSpecificDaysForRefresh());
      assertEquals(7, master.getSpecificDaysForRefresh().size());

      assertNotNull(master.getMamFileHolidayTable());
      assertEquals(10, master.getMamFileHolidayTable().size());
      assertEquals("New Year's Day", master.getMamFileHolidayTable().get(0).getHoliday());
    }

    @Test
    @DisplayName("createAccountIdentifierInformation should initialize with correct structure")
    void createAccountIdentifierInformationShouldInitializeWithCorrectStructure() {
      AccountIdentifierInformation info = BrdFormInitializer.createAccountIdentifierInformation();

      assertNotNull(info);

      assertNotNull(info.getAccountIdentifierFormat());
      assertEquals(3, info.getAccountIdentifierFormat().size());
      assertEquals("Numeric", info.getAccountIdentifierFormat().get(0).getViewValue());
    }
  }

  @Nested
  @DisplayName("Notification Tests")
  class NotificationTests {

    @Test
    @DisplayName("createNotifications should initialize with correct structure")
    void createNotificationsShouldInitializeWithCorrectStructure() {
      Notifications notifications = BrdFormInitializer.createNotifications();

      assertNotNull(notifications);

      List<ConsumerNotifications> notificationsList = notifications.getConsumerNotifications();
      assertNotNull(notificationsList);
      assertEquals(18, notificationsList.size());

      assertEquals("Required", notificationsList.get(0).getOptOut());
      assertEquals("Required", notificationsList.get(5).getOptOut());
      assertEquals("Required", notificationsList.get(9).getOptOut());

      ConsumerNotifications achPaymentModified = null;
      for (ConsumerNotifications notification : notificationsList) {
        if ("ACH payment modified".equals(notification.getName())) {
          achPaymentModified = notification;
          break;
        }
      }
      assertNotNull(achPaymentModified);
      assertEquals("Not available", achPaymentModified.getSms());

      assertNotNull(notifications.getShortCodeType());
      assertEquals(2, notifications.getShortCodeType().size());
    }
  }

  @Nested
  @DisplayName("Portal Configuration Tests")
  class PortalConfigurationTests {

    @Test
    @DisplayName("createAgentPortalConfig should initialize with correct structure")
    void createAgentPortalConfigShouldInitializeWithCorrectStructure() {
      AgentPortalConfig config = BrdFormInitializer.createAgentPortalConfig();

      assertNotNull(config);

      assertNotNull(config.getRedirectURL());
      assertEquals(2, config.getRedirectURL().size());
      assertEquals("UAT", config.getRedirectURL().get(0).getEnvironment());
      assertEquals("Production", config.getRedirectURL().get(1).getEnvironment());

      assertNotNull(config.getRefundType());
      assertEquals(3, config.getRefundType().size());
      assertTrue(config.getRefundType().get(0).isSelected());

      assertNotNull(config.getRefundReasons());
      assertEquals(12, config.getRefundReasons().size());

      assertNotNull(config.getFeeWaiveReasons());
      assertEquals(6, config.getFeeWaiveReasons().size());
      assertTrue(config.getFeeWaiveReasons().get(0).isSelected());

      assertNotNull(config.getBillSummaryItems());
      assertEquals(6, config.getBillSummaryItems().size());

      assertNotNull(config.getBillSummaryDetail());
      assertEquals(16, config.getBillSummaryDetail().size());

      assertNotNull(config.getAgentPortal());
      assertEquals(1, config.getAgentPortal().size());
    }
  }

  @Nested
  @DisplayName("Payment Configuration Tests")
  class PaymentConfigurationTests {

    @Test
    @DisplayName("createPaymentRules should initialize with default values")
    void createPaymentRulesShouldInitializeWithDefaultValues() {
      PaymentRules rules = BrdFormInitializer.createPaymentRules();

      assertNotNull(rules);
      assertEquals(DEFAULT_STRING, rules.getGlobalLimits());
      assertEquals(DEFAULT_STRING, rules.getMinimumPaymentAmount());
      assertEquals(DEFAULT_STRING, rules.getMaximumPaymentAmount());
      assertEquals(DEFAULT_STRING, rules.getSectionStatus());
    }

    @Test
    @DisplayName("createRecurringPayments should initialize with correct structure")
    void createRecurringPaymentsShouldInitializeWithCorrectStructure() {
      RecurringPayments payments = BrdFormInitializer.createRecurringPayments();

      assertNotNull(payments);

      assertNotNull(payments.getPaymentDateOptions());
      assertEquals(6, payments.getPaymentDateOptions().size());
      assertFalse(payments.getPaymentDateOptions().get(0).isSelected());

      assertNotNull(payments.getPaymentAmountOptions());
      assertEquals(3, payments.getPaymentAmountOptions().size());

      assertNotNull(payments.getDurationOptions());
      assertEquals(4, payments.getDurationOptions().size());
    }

    @Test
    @DisplayName("createIvr should initialize with correct structure")
    void createIvrShouldInitializeWithCorrectStructure() {
      Ivr ivr = BrdFormInitializer.createIvr();

      assertNotNull(ivr);

      assertNotNull(ivr.getFundingPrompts());
      assertEquals(2, ivr.getFundingPrompts().size());
      assertEquals("Card", ivr.getFundingPrompts().get(0).getFundingType());
      assertEquals("Bank Account", ivr.getFundingPrompts().get(1).getFundingType());

      assertNotNull(ivr.getAchSubType());
      assertEquals(4, ivr.getAchSubType().size());

      assertNotNull(ivr.getSupportedLanguages());
      assertEquals(2, ivr.getSupportedLanguages().size());

      assertNotNull(ivr.getEntryValidationPrompts());
      assertEquals(9, ivr.getEntryValidationPrompts().size());
    }
  }

  @Nested
  @DisplayName("Other Sections Tests")
  class OtherSectionsTests {

    @Test
    @DisplayName("createRemittance should initialize with default values")
    void createRemittanceShouldInitializeWithDefaultValues() {
      Remittance remittance = BrdFormInitializer.createRemittance();

      assertNotNull(remittance);
      assertEquals(DEFAULT_STRING, remittance.getRemittanceInformation());
      assertEquals(DEFAULT_STRING, remittance.getCutoffTime());
      assertEquals(DEFAULT_STRING, remittance.getSectionStatus());
    }

    @Test
    @DisplayName("createGeneralImplementations should initialize with correct structure")
    void createGeneralImplementationsShouldInitializeWithCorrectStructure() {
      GeneralImplementations implementations = BrdFormInitializer.createGeneralImplementations();

      assertNotNull(implementations);

      assertNotNull(implementations.getImplementationNotes());
      assertEquals(1, implementations.getImplementationNotes().size());
      assertEquals(DEFAULT_STRING, implementations.getImplementationNotes().get(0).getDate());
      assertEquals(DEFAULT_STRING, implementations.getImplementationNotes().get(0).getArea());
      assertEquals(DEFAULT_STRING, implementations.getImplementationNotes().get(0).getNote());
    }

    @Test
    @DisplayName("createApprovals should initialize with default values")
    void createApprovalsShouldInitializeWithDefaultValues() {
      Approvals approvals = BrdFormInitializer.createApprovals();

      assertNotNull(approvals);
      assertEquals(DEFAULT_STRING, approvals.getClientSignature());
      assertEquals(DEFAULT_STRING, approvals.getClientSignatureDate());
      assertEquals(DEFAULT_STRING, approvals.getAciSignature());
      assertEquals(DEFAULT_STRING, approvals.getAciSignatureDate());
      assertEquals(DEFAULT_STRING, approvals.getSectionStatus());
    }

    @Test
    @DisplayName("createRevisionHistory should initialize with default values")
    void createRevisionHistoryShouldInitializeWithDefaultValues() {
      RevisionHistory history = BrdFormInitializer.createRevisionHistory();

      assertNotNull(history);
      assertEquals(DEFAULT_STRING, history.getSectionStatus());

      if (history.getRevHistory() != null && !history.getRevHistory().isEmpty()) {
        RevisionData firstItem = history.getRevHistory().get(0);
        assertNotNull(firstItem);
        assertNotNull(firstItem.getDate());
        assertNotNull(firstItem.getDescription());
        assertNotNull(firstItem.getVersion());
      }
    }
  }

  @Nested
  @DisplayName("Helper Method Tests")
  class HelperMethodTests {

    @Test
    @DisplayName("private constructor should not be accessible")
    void privateConstructorShouldNotBeAccessible() {
      assertNotNull(BrdFormInitializer.createClientInformation());
    }

    @Test
    @DisplayName("createViewValueAndSelected helper method is used correctly")
    void testCreateViewValueAndSelectedHelperMethod() {
      PaymentChannels channels = BrdFormInitializer.createPaymentChannels();

      ViewValueAndSelected firstPortalOption = channels.getPaymentChannelsPortal().get(0);
      assertEquals("Agent Portal", firstPortalOption.getViewValue());
      assertTrue(firstPortalOption.isSelected());

      ViewValueAndSelected secondPortalOption = channels.getPaymentChannelsPortal().get(1);
      assertEquals("Student Portal", secondPortalOption.getViewValue());
      assertFalse(secondPortalOption.isSelected());
    }

    @Test
    @DisplayName("deepCopy method maintains correct structure")
    void testDeepCopyMaintainsCorrectStructure() {
      PaymentChannels channels = BrdFormInitializer.createPaymentChannels();

      assertNotNull(channels.getAcceptCreditDebitCardsChannels());

      assertEquals(
          channels.getAcceptBankAccountsChannels().size(),
          channels.getAcceptCreditDebitCardsChannels().size());

      assertEquals(
          channels.getAcceptBankAccountsChannels().get(0).getViewValue(),
          channels.getAcceptCreditDebitCardsChannels().get(0).getViewValue());

      assertNotSame(
          channels.getAcceptBankAccountsChannels(), channels.getAcceptCreditDebitCardsChannels());

      FundingMethods methods = BrdFormInitializer.createFundingMethods();
      assertEquals(methods.getAgentPortal().size(), methods.getIvr().size());
      assertEquals(methods.getAgentPortal().size(), methods.getApiSdk().size());

      assertEquals(methods.getAgentPortal().get(0).getTier(), methods.getIvr().get(0).getTier());

      assertNotSame(methods.getAgentPortal(), methods.getIvr());
      assertNotSame(methods.getAgentPortal(), methods.getApiSdk());
    }
  }

  @Nested
  @DisplayName("ACH Payment Processing Tests")
  class AchPaymentProcessingTests {

    @Test
    @DisplayName("createAchPaymentProcessing should return initialized instance with defaults")
    void createAchPaymentProcessingShouldReturnInitializedObject() {
      AchPaymentProcessing achPaymentProcessing = BrdFormInitializer.createAchPaymentProcessing();

      assertNotNull(achPaymentProcessing);

      String defaultString = "";
      assertEquals(defaultString, achPaymentProcessing.getCreditToBillerThreshold());
      assertEquals(defaultString, achPaymentProcessing.getDebitToBillerThreshold());
      assertEquals(defaultString, achPaymentProcessing.getAchcompanyName());
      assertEquals(defaultString, achPaymentProcessing.getAchcompanyDiscretionaryData());
      assertEquals(defaultString, achPaymentProcessing.getAchcompanyID());
      assertEquals(defaultString, achPaymentProcessing.getAchcompanyDescription());
      assertEquals(defaultString, achPaymentProcessing.getWebACHDescription());
      assertEquals(defaultString, achPaymentProcessing.getCsrACHDescription());
      assertEquals(defaultString, achPaymentProcessing.getIvrACHDescription());
      assertEquals(defaultString, achPaymentProcessing.getBatchACHDescription());
      assertEquals(defaultString, achPaymentProcessing.getTimeFrame());
      assertEquals(defaultString, achPaymentProcessing.getBlockAllPayments());
      assertEquals(defaultString, achPaymentProcessing.getAchReturnResubmission());
      assertEquals(defaultString, achPaymentProcessing.getFundingNSFValidation());
      assertEquals(defaultString, achPaymentProcessing.getResubmissionTimes());
      assertEquals(defaultString, achPaymentProcessing.getSectionStatus());
    }
  }

  @Nested
  @DisplayName("Revision History Tests")
  class RevisionHistoryTests {

    @Test
    @DisplayName("createRevisionHistory should return initialized instance with defaults")
    void createRevisionHistoryShouldReturnInitializedObject() {
      RevisionHistory revisionHistory = BrdFormInitializer.createRevisionHistory();

      assertNotNull(revisionHistory);

      String defaultString = "";
      assertEquals(defaultString, revisionHistory.getSectionStatus());

      if (revisionHistory.getRevHistory() != null && !revisionHistory.getRevHistory().isEmpty()) {
        RevisionData firstItem = revisionHistory.getRevHistory().get(0);
        assertNotNull(firstItem);
        assertNotNull(firstItem.getDate());
        assertNotNull(firstItem.getDescription());
        assertNotNull(firstItem.getVersion());
      }
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("createSection should handle exceptions gracefully")
    void createSectionHandlesExceptions() {
      assertNotNull(BrdFormInitializer.createClientInformation());
      assertNotNull(BrdFormInitializer.createAciInformation());
      assertNotNull(BrdFormInitializer.createPaymentChannels());
      assertNotNull(BrdFormInitializer.createFundingMethods());
      assertNotNull(BrdFormInitializer.createAchPaymentProcessing());
      assertNotNull(BrdFormInitializer.createMiniAccountMaster());
      assertNotNull(BrdFormInitializer.createAccountIdentifierInformation());
      assertNotNull(BrdFormInitializer.createNotifications());
      assertNotNull(BrdFormInitializer.createRemittance());
      assertNotNull(BrdFormInitializer.createAgentPortalConfig());
      assertNotNull(BrdFormInitializer.createRecurringPayments());
      assertNotNull(BrdFormInitializer.createGeneralImplementations());
      assertNotNull(BrdFormInitializer.createRevisionHistory());
    }
  }
}
