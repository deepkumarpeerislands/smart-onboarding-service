package com.aci.smart_onboarding.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.validation.annotation.Validated;

/**
 * Entity class for audit logs that tracks changes to entities in the system. Stores information
 * about what changed, who made the change, and when.
 */
@Document(collection = "audit_logs")
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
  @Id private String auditId;
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
