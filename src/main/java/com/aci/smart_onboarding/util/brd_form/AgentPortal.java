package com.aci.smart_onboarding.util.brd_form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPortal {
  private String visibleAgentConsumer;
  private String dataEntryType;
  private String requiredYorN;
  private String validationExpression;
}
