package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.constants.BrdConstants;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.*;

/**
 * Data Transfer Object (DTO) representing a single entry in a BRD's status history. This class is
 * used to provide a formatted view of status changes, including creation events, with consistent
 * timestamp formatting and user attribution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryResponse {
  /**
   * The status value. For status updates, this will be the actual status value. For creation
   * events, this will be "Created".
   */
  @Setter private String status;

  /**
   * The formatted timestamp when the status change occurred, displayed in the format "MMM d, yyyy,
   * h:mm a" (e.g., "Jan 8, 2025, 10:15 AM").
   */
  private String timestamp; // formatted timestamp string

  /** The user who made the status change. */
  private String changedBy;

  /** The role of the user who made the status change. */
  private String userRole;

  private String comment;

  public void mapWithAuditLog(AuditLogResponse auditLog) {
    // Handle different action types appropriately
    switch (auditLog.getAction()) {
      case BrdConstants.ACTION_STATUS_UPDATE:
        if (auditLog.getNewValues() != null
            && auditLog.getNewValues().containsKey(BrdConstants.STATUS_FIELD)) {
          this.status = (String) auditLog.getNewValues().get(BrdConstants.STATUS_FIELD);
        }
        break;
      case BrdConstants.ACTION_CREATE:
        this.status = BrdConstants.DRAFT;
        break;
      default:
        // For unknown action types, use action as status
        this.status = auditLog.getAction();
    }

    this.changedBy = auditLog.getUserName();
    this.userRole = auditLog.getUserRole();
    this.comment = auditLog.getComment();
    this.setTimestampFromLocalDateTime(auditLog.getEventTimestamp());
  }

  /**
   * Sets the timestamp value by formatting a LocalDateTime object.
   *
   * @param dateTime The LocalDateTime to format and set as the timestamp
   */
  public void setTimestampFromLocalDateTime(LocalDateTime dateTime) {
    if (dateTime != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");
      this.timestamp = dateTime.format(formatter);
    }
  }
}
