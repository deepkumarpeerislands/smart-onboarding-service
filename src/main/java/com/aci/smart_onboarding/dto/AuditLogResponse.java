package com.aci.smart_onboarding.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
  private String auditId;
  private String entityType;
  private String entityId;
  private String action;
  private String userId;
  private String userName;
  private String comment;
  private String userRole;
  private LocalDateTime eventTimestamp;
  private Map<String, Object> oldValues;
  private Map<String, Object> newValues;
}
