package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for audit log requests. Contains information about entity changes and user
 * who made the changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequest {
  @NotBlank(message = "Entity type cannot be blank")
  private String entityType; // BRD

  @NotBlank(message = "Entity ID cannot be blank")
  private String entityId; // brdFormId

  @NotBlank(message = "Action cannot be blank")
  private String action; // "CREATE", "UPDATE"

  @NotBlank(message = "User ID cannot be blank")
  private String userId;

  @NotBlank(message = "User name cannot be blank")
  private String userName;

  private String comment;

  /**
   * The role of the user who made the change. Not required, but useful for tracking which type of
   * user performed the action.
   */
  private String userRole;

  @NotNull(message = "Event timestamp cannot be null")
  private LocalDateTime eventTimestamp;

  private Map<String, Object> oldValues; // Previous values (for updates)
  private Map<String, Object> newValues;
}
