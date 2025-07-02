package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.dto.AuditLogResponse;
import com.aci.smart_onboarding.dto.StatusHistoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for audit logging functionality. Provides methods for creating audit logs and
 * retrieving entity status history.
 */
@Validated
public interface IAuditLogService {
  /**
   * Creates a new audit log entry.
   *
   * @param auditLogRequest The request object containing audit log details
   * @return A Mono containing a ResponseEntity with the created audit log information
   */
  Mono<ResponseEntity<Api<AuditLogResponse>>> logCreation(
      @Valid @NotNull(message = "auditLogRequest can't be null") AuditLogRequest auditLogRequest);

  /**
   * Finds audit logs for a specific entity and action type.
   *
   * @param entityId The entity identifier (e.g., BRD ID)
   * @param action The action type to filter by (e.g., "CREATE", "STATUS_UPDATE")
   * @return A Flux of AuditLogResponse objects matching the criteria
   */
  Flux<AuditLogResponse> findByEntityIdAndAction(String entityId, String action);

  /**
   * Fetches and formats status history for multiple action types, combining them into a
   * chronological history. This is particularly useful for creating a complete history view that
   * includes both creation and update events.
   *
   * @param entityId The ID of the entity (BRD) to fetch history for
   * @param actions A list of action types to include in the history (e.g., "CREATE",
   *     "STATUS_UPDATE")
   * @return A Mono containing a list of StatusHistoryResponse objects
   */
  Mono<List<StatusHistoryResponse>> fetchStatusHistory(String entityId, List<String> actions);
}
