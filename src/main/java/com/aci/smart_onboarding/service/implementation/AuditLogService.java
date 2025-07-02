package com.aci.smart_onboarding.service.implementation;

import static org.springframework.data.domain.Sort.*;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.dto.AuditLogResponse;
import com.aci.smart_onboarding.dto.StatusHistoryResponse;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.repository.AuditLogRepository;
import com.aci.smart_onboarding.service.IAuditLogService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuditLogService implements IAuditLogService {

  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
  private final AuditLogRepository auditLogRepository;
  private final DtoModelMapper dtoModelMapper;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  @Override
  public Mono<ResponseEntity<Api<AuditLogResponse>>> logCreation(AuditLogRequest auditLogRequest) {
    return Mono.defer(() -> Mono.just(auditLogRequest))
        .map(dtoModelMapper::mapToAuditLog)
        .flatMap(auditLogRepository::save)
        .map(dtoModelMapper::mapToAuditLogResponse)
        .map(
            response ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Api<>(
                            "SUCCESS",
                            "Audit log created successfully",
                            Optional.of(response),
                            Optional.empty())))
        .onErrorMap(ex -> new Exception("Something went wrong : " + ex.getMessage()));
  }

  @Override
  public Flux<AuditLogResponse> findByEntityIdAndAction(String entityId, String action) {
    Query query = new Query();
    query.addCriteria(Criteria.where("entityId").is(entityId));
    query.addCriteria(Criteria.where("action").is(action));
    query.with(by(Direction.ASC, "eventTimestamp"));

    return reactiveMongoTemplate
        .find(query, AuditLog.class)
        .map(dtoModelMapper::mapToAuditLogResponse);
  }

  /**
   * Fetches status history for multiple action types and combines the results into a single sorted
   * list. This method handles both CREATE and STATUS_UPDATE actions, displaying them in a unified
   * chronological view.
   *
   * @param entityId The ID of the entity (BRD) to fetch history for
   * @param actions A list of action types to include in the history (e.g., CREATE, STATUS_UPDATE)
   * @return A Mono containing a chronologically sorted list of StatusHistoryResponse objects
   */
  @Override
  public Mono<List<StatusHistoryResponse>> fetchStatusHistory(
      String entityId, List<String> actions) {
    // Skip processing if empty actions list
    if (actions == null || actions.isEmpty()) {
      return Mono.just(Collections.emptyList());
    }

    return Flux.fromIterable(actions)
        // Use concatMap to preserve order and potential error handling
        .concatMap(
            action ->
                findByEntityIdAndAction(entityId, action)
                    // Add error handling for each action type
                    .onErrorResume(
                        e -> {
                          log.warn(
                              "Error fetching history for action {}: {}", action, e.getMessage());
                          return Flux.empty();
                        }))
        .map(auditLog -> new Object[] {auditLog, getStatusHistoryResponse(auditLog)})
        // Sort by the original eventTimestamp, not the formatted string
        .collectSortedList(
            (a, b) -> {
              AuditLogResponse auditLog1 = (AuditLogResponse) a[0];
              AuditLogResponse auditLog2 = (AuditLogResponse) b[0];
              return auditLog1.getEventTimestamp().compareTo(auditLog2.getEventTimestamp());
            })
        // Extract just the StatusHistoryResponse objects from the sorted list
        .map(list -> list.stream().map(pair -> (StatusHistoryResponse) pair[1]).toList());
  }

  private static StatusHistoryResponse getStatusHistoryResponse(AuditLogResponse auditLog) {
    StatusHistoryResponse historyItem = new StatusHistoryResponse();
    historyItem.mapWithAuditLog(auditLog);
    return historyItem;
  }
}
