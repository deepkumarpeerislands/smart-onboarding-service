package com.aci.smart_onboarding.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PMReassignmentResponse {
  private String brdId;
  private String oldPmUsername;
  private String newPmUsername;
  private String status;
  private String reason;
}
