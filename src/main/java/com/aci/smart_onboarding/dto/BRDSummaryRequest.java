package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.dto.BrdFormRequest.ClientInformation;
import com.aci.smart_onboarding.util.brd_form.AgentPortalConfig;
import com.aci.smart_onboarding.util.brd_form.PaymentChannels;
import com.aci.smart_onboarding.util.brd_form.PaymentRules;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BRDSummaryRequest {
  @NotBlank(message = "BRD ID is required")
  private String brdId;

  private ClientInformation clientInformation;
  private PaymentChannels paymentChannels;
  private AgentPortalConfig agentPortal;
  private PaymentRules paymentRules;
}
