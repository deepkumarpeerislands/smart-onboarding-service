package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.dto.BrdFormRequest.ClientInformation;
import com.aci.smart_onboarding.util.brd_form.AgentPortalConfig;
import com.aci.smart_onboarding.util.brd_form.PaymentChannels;
import com.aci.smart_onboarding.util.brd_form.PaymentRules;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BRDSummaryRequestTest {

  private Validator validator;
  private BRDSummaryRequest validRequest;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();

    validRequest = new BRDSummaryRequest();
    validRequest.setBrdId("BRD123");
    validRequest.setClientInformation(new ClientInformation());
    validRequest.setPaymentChannels(new PaymentChannels());
    validRequest.setAgentPortal(new AgentPortalConfig());
    validRequest.setPaymentRules(new PaymentRules());
  }

  @Test
  void whenAllFieldsValid_ShouldHaveNoViolations() {
    Set<ConstraintViolation<BRDSummaryRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void whenBrdIdIsNull_ShouldHaveViolation() {
    validRequest.setBrdId(null);
    Set<ConstraintViolation<BRDSummaryRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals("BRD ID is required", violations.iterator().next().getMessage());
  }

  @Test
  void whenBrdIdIsBlank_ShouldHaveViolation() {
    validRequest.setBrdId("");
    Set<ConstraintViolation<BRDSummaryRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals("BRD ID is required", violations.iterator().next().getMessage());
  }

  @Test
  void whenOptionalFieldsAreNull_ShouldHaveNoViolations() {
    validRequest.setClientInformation(null);
    validRequest.setPaymentChannels(null);
    validRequest.setAgentPortal(null);
    validRequest.setPaymentRules(null);

    Set<ConstraintViolation<BRDSummaryRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void testEqualsAndHashCode() {
    BRDSummaryRequest request1 = new BRDSummaryRequest();
    request1.setBrdId("BRD123");

    BRDSummaryRequest request2 = new BRDSummaryRequest();
    request2.setBrdId("BRD123");

    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());
  }

  @Test
  void testToString() {
    String toString = validRequest.toString();

    assertTrue(toString.contains("brdId=BRD123"));
    assertTrue(toString.contains("clientInformation="));
    assertTrue(toString.contains("paymentChannels="));
    assertTrue(toString.contains("agentPortal="));
    assertTrue(toString.contains("paymentRules="));
  }

  @Test
  void testBuilder() {
    ClientInformation clientInfo = new ClientInformation();
    PaymentChannels paymentChannels = new PaymentChannels();
    AgentPortalConfig agentPortal = new AgentPortalConfig();
    PaymentRules paymentRules = new PaymentRules();

    BRDSummaryRequest request = new BRDSummaryRequest();
    request.setBrdId("BRD123");
    request.setClientInformation(clientInfo);
    request.setPaymentChannels(paymentChannels);
    request.setAgentPortal(agentPortal);
    request.setPaymentRules(paymentRules);

    assertNotNull(request);
    assertEquals("BRD123", request.getBrdId());
    assertEquals(clientInfo, request.getClientInformation());
    assertEquals(paymentChannels, request.getPaymentChannels());
    assertEquals(agentPortal, request.getAgentPortal());
    assertEquals(paymentRules, request.getPaymentRules());
  }
}
