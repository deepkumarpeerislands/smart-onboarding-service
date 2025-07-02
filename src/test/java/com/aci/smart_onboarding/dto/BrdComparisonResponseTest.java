package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.util.brd_form.AccountIdentifierInformation;
import com.aci.smart_onboarding.util.brd_form.AchPaymentProcessing;
import com.aci.smart_onboarding.util.brd_form.AciInformation;
import com.aci.smart_onboarding.util.brd_form.AgentPortalConfig;
import com.aci.smart_onboarding.util.brd_form.Approvals;
import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import com.aci.smart_onboarding.util.brd_form.FundingMethods;
import com.aci.smart_onboarding.util.brd_form.GeneralImplementations;
import com.aci.smart_onboarding.util.brd_form.Ivr;
import com.aci.smart_onboarding.util.brd_form.MiniAccountMaster;
import com.aci.smart_onboarding.util.brd_form.Notifications;
import com.aci.smart_onboarding.util.brd_form.PaymentChannels;
import com.aci.smart_onboarding.util.brd_form.PaymentRules;
import com.aci.smart_onboarding.util.brd_form.RecurringPayments;
import com.aci.smart_onboarding.util.brd_form.Remittance;
import com.aci.smart_onboarding.util.brd_form.RevisionHistory;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrdComparisonResponseTest {

  @Test
  @DisplayName("Should test builder pattern properly")
  void testBuilderPattern() {
    // Basic builder usage
    BrdComparisonResponse response =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    assertEquals("BRD123", response.getBrdId());
    assertEquals("SITE456", response.getSiteId());
    assertEquals("Test Site", response.getSiteName());
    assertEquals(2, response.getDifferences().size());
    assertEquals("value1", response.getDifferences().get("key1"));

    // Test builder with all fields
    ClientInformation clientInfo = new ClientInformation();
    clientInfo.setCompanyName("Test Company");

    AciInformation aciInfo = new AciInformation();
    aciInfo.setITContactName("IT Contact");

    PaymentChannels paymentChannels = new PaymentChannels();
    paymentChannels.setAcceptBankAccounts("Yes");

    FundingMethods fundingMethods = new FundingMethods();
    fundingMethods.setItmLeveraged("Yes");

    AchPaymentProcessing achProcessing = new AchPaymentProcessing();
    achProcessing.setAchcompanyName("ACH Company");

    MiniAccountMaster mam = new MiniAccountMaster();
    mam.setMamFile("Yes");

    AccountIdentifierInformation accInfo = new AccountIdentifierInformation();
    accInfo.setMinimumAccountIdentifierLength("5");

    PaymentRules paymentRules = new PaymentRules();
    paymentRules.setMinimumPaymentAmount("10.00");

    Notifications notifications = new Notifications();
    notifications.setFromAddress("test@example.com");

    Remittance remittance = new Remittance();
    remittance.setRemittanceInformation("Standard");

    AgentPortalConfig agentPortal = new AgentPortalConfig();
    agentPortal.setPasswordExpiration("90");

    RecurringPayments recurringPayments = new RecurringPayments();
    recurringPayments.setRecurringPaymentThreshold("Yes");

    Ivr ivr = new Ivr();
    ivr.setIvrInScope("Yes");

    GeneralImplementations generalImpl = new GeneralImplementations();

    Approvals approvals = new Approvals();
    approvals.setClientSignature("John Doe");

    RevisionHistory revisionHistory = new RevisionHistory();

    BrdComparisonResponse fullResponse =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1"))
            .clientInformationDiff(clientInfo)
            .aciInformationDiff(aciInfo)
            .paymentChannelsDiff(paymentChannels)
            .fundingMethodsDiff(fundingMethods)
            .achPaymentProcessingDiff(achProcessing)
            .miniAccountMasterDiff(mam)
            .accountIdentifierInformationDiff(accInfo)
            .paymentRulesDiff(paymentRules)
            .notificationsDiff(notifications)
            .remittanceDiff(remittance)
            .agentPortalDiff(agentPortal)
            .recurringPaymentsDiff(recurringPayments)
            .ivrDiff(ivr)
            .generalImplementationsDiff(generalImpl)
            .approvalsDiff(approvals)
            .revisionHistoryDiff(revisionHistory)
            .build();

    // Verify all fields were set
    assertEquals("BRD123", fullResponse.getBrdId());
    assertEquals("Test Company", fullResponse.getClientInformationDiff().getCompanyName());
    assertEquals("IT Contact", fullResponse.getAciInformationDiff().getITContactName());
    assertEquals("Yes", fullResponse.getPaymentChannelsDiff().getAcceptBankAccounts());
    assertEquals("Yes", fullResponse.getFundingMethodsDiff().getItmLeveraged());
    assertEquals("ACH Company", fullResponse.getAchPaymentProcessingDiff().getAchcompanyName());
    assertEquals("Yes", fullResponse.getMiniAccountMasterDiff().getMamFile());
    assertEquals(
        "5",
        fullResponse.getAccountIdentifierInformationDiff().getMinimumAccountIdentifierLength());
    assertEquals("10.00", fullResponse.getPaymentRulesDiff().getMinimumPaymentAmount());
    assertEquals("test@example.com", fullResponse.getNotificationsDiff().getFromAddress());
    assertEquals("Standard", fullResponse.getRemittanceDiff().getRemittanceInformation());
    assertEquals("90", fullResponse.getAgentPortalDiff().getPasswordExpiration());
    assertEquals("Yes", fullResponse.getRecurringPaymentsDiff().getRecurringPaymentThreshold());
    assertEquals("Yes", fullResponse.getIvrDiff().getIvrInScope());
    assertNotNull(fullResponse.getGeneralImplementationsDiff());
    assertEquals("John Doe", fullResponse.getApprovalsDiff().getClientSignature());
    assertNotNull(fullResponse.getRevisionHistoryDiff());
  }

  @Test
  @DisplayName("Should test equals and hashCode methods")
  void testEqualsAndHashCode() {
    // Create a response
    BrdComparisonResponse response1 =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    // Create an identical response
    BrdComparisonResponse response2 =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    // Create a different response
    BrdComparisonResponse differentResponse =
        BrdComparisonResponse.builder()
            .brdId("BRD999")
            .siteId("SITE888")
            .siteName("Different Site")
            .differences(Map.of("key3", "value3", "key4", "value4"))
            .build();

    // Test reflexivity
    assertEquals(response1, response1);

    // Test symmetry
    assertEquals(response1, response2);
    assertEquals(response2, response1);

    // Test transitivity
    BrdComparisonResponse response3 =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    assertEquals(response1, response2);
    assertEquals(response2, response3);
    assertEquals(response1, response3);

    // Test inequality
    assertNotEquals(response1, differentResponse);
    assertNotEquals(response2, differentResponse);

    // Test null and different type comparison
    assertNotEquals(null, response1);
    assertNotEquals("Not a BrdComparisonResponse", response1);

    // Test hashCode consistency
    assertEquals(response1.hashCode(), response2.hashCode());
    assertEquals(response2.hashCode(), response3.hashCode());
    assertEquals(response1.hashCode(), response3.hashCode());
    assertNotEquals(response1.hashCode(), differentResponse.hashCode());

    // Test with different field values
    BrdComparisonResponse diffBrdId =
        BrdComparisonResponse.builder()
            .brdId("OTHER_BRD")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    BrdComparisonResponse diffSiteId =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("OTHER_SITE")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    BrdComparisonResponse diffSiteName =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Different Site Name")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    assertNotEquals(response1, diffBrdId);
    assertNotEquals(response1, diffSiteId);
    assertNotEquals(response1, diffSiteName);
  }

  @Test
  @DisplayName("Should test toString method properly")
  void testToString() {
    BrdComparisonResponse response =
        BrdComparisonResponse.builder()
            .brdId("BRD123")
            .siteId("SITE456")
            .siteName("Test Site")
            .differences(Map.of("key1", "value1", "key2", "value2"))
            .build();

    String toString = response.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("brdId=BRD123"));
    assertTrue(toString.contains("siteId=SITE456"));
    assertTrue(toString.contains("siteName=Test Site"));
    assertTrue(toString.contains("differences={"));
    assertTrue(toString.contains("key1=value1"));
    assertTrue(toString.contains("key2=value2"));
  }

  @Test
  @DisplayName("Should test constructors properly")
  void testConstructors() {
    // Test no-args constructor
    BrdComparisonResponse noArgsResponse = new BrdComparisonResponse();
    assertNotNull(noArgsResponse);
    assertNull(noArgsResponse.getBrdId());
    assertNull(noArgsResponse.getSiteId());
    assertNull(noArgsResponse.getSiteName());
    assertNull(noArgsResponse.getDifferences());

    // Test all-args constructor
    BrdComparisonResponse allArgsResponse =
        new BrdComparisonResponse(
            "BRD123",
            "SITE456",
            "Test Site",
            Map.of("key1", "value1"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertNotNull(allArgsResponse);
    assertEquals("BRD123", allArgsResponse.getBrdId());
    assertEquals("SITE456", allArgsResponse.getSiteId());
    assertEquals("Test Site", allArgsResponse.getSiteName());
    assertEquals(1, allArgsResponse.getDifferences().size());
    assertEquals("value1", allArgsResponse.getDifferences().get("key1"));
  }

  @Test
  @DisplayName("Should test edge cases properly")
  void testEdgeCases() {
    // Test with null fields
    BrdComparisonResponse nullFieldsResponse =
        BrdComparisonResponse.builder().brdId("BRD123").build();

    assertNotNull(nullFieldsResponse);
    assertEquals("BRD123", nullFieldsResponse.getBrdId());
    assertNull(nullFieldsResponse.getSiteId());
    assertNull(nullFieldsResponse.getSiteName());
    assertNull(nullFieldsResponse.getDifferences());

    // Test with empty maps
    BrdComparisonResponse emptyMapResponse =
        BrdComparisonResponse.builder().brdId("BRD123").differences(Map.of()).build();

    assertNotNull(emptyMapResponse);
    assertEquals("BRD123", emptyMapResponse.getBrdId());
    assertEquals(0, emptyMapResponse.getDifferences().size());

    // Test setters
    BrdComparisonResponse response = new BrdComparisonResponse();
    response.setBrdId("BRD123");
    response.setSiteId("SITE456");
    response.setSiteName("Test Site");
    response.setDifferences(Map.of("key1", "value1"));

    assertEquals("BRD123", response.getBrdId());
    assertEquals("SITE456", response.getSiteId());
    assertEquals("Test Site", response.getSiteName());
    assertEquals(1, response.getDifferences().size());
    assertEquals("value1", response.getDifferences().get("key1"));
  }
}
