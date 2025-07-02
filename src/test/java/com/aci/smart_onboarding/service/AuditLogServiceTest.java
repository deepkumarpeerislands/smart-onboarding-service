package com.aci.smart_onboarding.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.dto.AuditLogResponse;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.repository.AuditLogRepository;
import com.aci.smart_onboarding.service.implementation.AuditLogService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock private AuditLogRepository auditLogRepository;

  @Mock private DtoModelMapper dtoModelMapper;

  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;

  @InjectMocks private AuditLogService auditLogService;

  private AuditLogRequest validRequest;
  private AuditLog auditLog;
  private AuditLogResponse auditLogResponse;
  private Map<String, Object> oldValues;
  private Map<String, Object> newValues;
  private LocalDateTime testTimestamp;

  @BeforeEach
  void setUp() {
    testTimestamp = LocalDateTime.now();

    // Initialize test data maps
    oldValues = new HashMap<>();
    oldValues.put("status", "DRAFT");
    oldValues.put("version", 1);

    newValues = new HashMap<>();
    newValues.put("status", "SUBMITTED");
    newValues.put("version", 2);

    // Create valid request
    validRequest =
        AuditLogRequest.builder()
            .entityType("BRD")
            .entityId("BRD123")
            .action("UPDATE")
            .userId("USER123")
            .userName("John Doe")
            .eventTimestamp(testTimestamp)
            .oldValues(oldValues)
            .newValues(newValues)
            .build();

    // Setup AuditLog
    auditLog = new AuditLog();
    auditLog.setAuditId("AUDIT123");
    auditLog.setEntityType("BRD");
    auditLog.setEntityId("BRD123");
    auditLog.setAction("UPDATE");
    auditLog.setUserId("USER123");
    auditLog.setUserName("John Doe");
    auditLog.setEventTimestamp(testTimestamp);
    auditLog.setOldValues(oldValues);
    auditLog.setNewValues(newValues);

    // Setup AuditLogResponse
    auditLogResponse = new AuditLogResponse();
    auditLogResponse.setAuditId("AUDIT123");
    auditLogResponse.setEntityType("BRD");
    auditLogResponse.setEntityId("BRD123");
    auditLogResponse.setAction("UPDATE");
    auditLogResponse.setUserId("USER123");
    auditLogResponse.setUserName("John Doe");
    auditLogResponse.setEventTimestamp(testTimestamp);
    auditLogResponse.setOldValues(oldValues);
    auditLogResponse.setNewValues(newValues);
  }

  @Test
  void logCreation_WithValidRequest_ShouldReturn201() {
    // Arrange
    when(dtoModelMapper.mapToAuditLog(any(AuditLogRequest.class))).thenReturn(auditLog);
    when(auditLogRepository.save(any(AuditLog.class))).thenReturn(Mono.just(auditLog));
    when(dtoModelMapper.mapToAuditLogResponse(any(AuditLog.class))).thenReturn(auditLogResponse);

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(validRequest))
        .expectNext(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    new Api<>(
                        "SUCCESS",
                        "Audit log created successfully",
                        Optional.of(auditLogResponse),
                        Optional.empty())))
        .verifyComplete();
  }

  @Test
  void logCreation_WithNullRequest_ShouldReturn400() {
    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().contains("Something went wrong"))
        .verify();
  }

  @Test
  void logCreation_WithNullEntityType_ShouldReturn400() {
    // Arrange
    AuditLogRequest invalidRequest =
        AuditLogRequest.builder()
            .entityType(null)
            .entityId("BRD123")
            .action("UPDATE")
            .userId("USER123")
            .userName("John Doe")
            .eventTimestamp(testTimestamp)
            .build();

    when(dtoModelMapper.mapToAuditLog(invalidRequest)).thenReturn(null);

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(invalidRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().contains("Something went wrong")
                    && throwable.getMessage().contains("null value"))
        .verify();
  }

  @Test
  void logCreation_WithDatabaseError_ShouldReturn500() {
    // Arrange
    when(dtoModelMapper.mapToAuditLog(any(AuditLogRequest.class))).thenReturn(auditLog);
    when(auditLogRepository.save(any(AuditLog.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(validRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().contains("Something went wrong")
                    && throwable.getMessage().contains("Database error"))
        .verify();
  }

  @Test
  void logCreation_WithMapperError_ShouldReturn500() {
    // Arrange
    when(dtoModelMapper.mapToAuditLog(any(AuditLogRequest.class)))
        .thenThrow(new RuntimeException("Mapping error"));

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(validRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().contains("Something went wrong")
                    && throwable.getMessage().contains("Mapping error"))
        .verify();
  }

  @Test
  void logCreation_WithResponseMappingError_ShouldReturn500() {
    // Arrange
    when(dtoModelMapper.mapToAuditLog(any(AuditLogRequest.class))).thenReturn(auditLog);
    when(auditLogRepository.save(any(AuditLog.class))).thenReturn(Mono.just(auditLog));
    when(dtoModelMapper.mapToAuditLogResponse(any(AuditLog.class)))
        .thenThrow(new RuntimeException("Response mapping error"));

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(validRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().contains("Something went wrong")
                    && throwable.getMessage().contains("Response mapping error"))
        .verify();
  }

  @Test
  void logCreation_WithOptionalFieldsNull_Returns201() {
    // Arrange
    AuditLogRequest requestWithoutOptionals =
        AuditLogRequest.builder()
            .entityType("BRD")
            .entityId("BRD123")
            .action("UPDATE")
            .userId("USER123")
            .userName("John Doe")
            .eventTimestamp(testTimestamp)
            .build();

    when(dtoModelMapper.mapToAuditLog(any(AuditLogRequest.class))).thenReturn(auditLog);
    when(auditLogRepository.save(any(AuditLog.class))).thenReturn(Mono.just(auditLog));
    when(dtoModelMapper.mapToAuditLogResponse(any(AuditLog.class))).thenReturn(auditLogResponse);

    // Act & Assert
    StepVerifier.create(auditLogService.logCreation(requestWithoutOptionals))
        .expectNext(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    new Api<>(
                        "SUCCESS",
                        "Audit log created successfully",
                        Optional.of(auditLogResponse),
                        Optional.empty())))
        .verifyComplete();
  }

  @Test
  void fetchStatusHistory_ShouldSortByEventTimestamp() {
    // Arrange - Create audit logs with different timestamps
    LocalDateTime oldTime = LocalDateTime.of(2022, 1, 1, 10, 0);
    LocalDateTime midTime = LocalDateTime.of(2022, 6, 15, 12, 0);
    LocalDateTime newTime = LocalDateTime.of(2023, 1, 1, 14, 0);

    // Create audit logs with timestamps in mixed order to verify sorting
    AuditLog oldAuditLog = new AuditLog();
    oldAuditLog.setEntityId("BRD123");
    oldAuditLog.setAction(BrdConstants.ACTION_STATUS_UPDATE);
    oldAuditLog.setEventTimestamp(oldTime);
    oldAuditLog.setUserName("User 1");
    oldAuditLog.setUserRole("Role 1");

    AuditLog midAuditLog = new AuditLog();
    midAuditLog.setEntityId("BRD123");
    midAuditLog.setAction(BrdConstants.ACTION_CREATE);
    midAuditLog.setEventTimestamp(midTime);
    midAuditLog.setUserName("User 2");
    midAuditLog.setUserRole("Role 2");

    AuditLog newAuditLog = new AuditLog();
    newAuditLog.setEntityId("BRD123");
    newAuditLog.setAction(BrdConstants.ACTION_STATUS_UPDATE);
    newAuditLog.setEventTimestamp(newTime);
    newAuditLog.setUserName("User 3");
    newAuditLog.setUserRole("Role 3");

    // Create corresponding responses
    AuditLogResponse oldResponse = new AuditLogResponse();
    oldResponse.setEntityId("BRD123");
    oldResponse.setAction(BrdConstants.ACTION_STATUS_UPDATE);
    oldResponse.setEventTimestamp(oldTime);
    oldResponse.setUserName("User 1");
    oldResponse.setUserRole("Role 1");
    Map<String, Object> oldNewValues = new HashMap<>();
    oldNewValues.put("status", "DRAFT");
    oldResponse.setNewValues(oldNewValues);

    AuditLogResponse midResponse = new AuditLogResponse();
    midResponse.setEntityId("BRD123");
    midResponse.setAction(BrdConstants.ACTION_CREATE);
    midResponse.setEventTimestamp(midTime);
    midResponse.setUserName("User 2");
    midResponse.setUserRole("Role 2");

    AuditLogResponse newResponse = new AuditLogResponse();
    newResponse.setEntityId("BRD123");
    newResponse.setAction(BrdConstants.ACTION_STATUS_UPDATE);
    newResponse.setEventTimestamp(newTime);
    newResponse.setUserName("User 3");
    newResponse.setUserRole("Role 3");
    Map<String, Object> newNewValues = new HashMap<>();
    newNewValues.put("status", "APPROVED");
    newResponse.setNewValues(newNewValues);

    // Mock the ReactiveMongoTemplate find method to return our audit logs
    // Use any() matcher instead of trying to match on Query toString()
    when(reactiveMongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.just(newAuditLog, oldAuditLog), Flux.just(midAuditLog));

    // Mock the mapper to return our prepared responses
    when(dtoModelMapper.mapToAuditLogResponse(oldAuditLog)).thenReturn(oldResponse);
    when(dtoModelMapper.mapToAuditLogResponse(midAuditLog)).thenReturn(midResponse);
    when(dtoModelMapper.mapToAuditLogResponse(newAuditLog)).thenReturn(newResponse);

    // Format the expected timestamps the same way as in StatusHistoryResponse
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");
    String oldTimeFormatted = oldTime.format(formatter);
    String midTimeFormatted = midTime.format(formatter);
    String newTimeFormatted = newTime.format(formatter);

    // Act & Assert
    StepVerifier.create(
            auditLogService.fetchStatusHistory(
                "BRD123", List.of(BrdConstants.ACTION_CREATE, BrdConstants.ACTION_STATUS_UPDATE)))
        .expectNextMatches(
            historyList -> {
              // Verify we got 3 results
              if (historyList.size() != 3) {
                System.out.println("Wrong number of items: " + historyList.size());
                return false;
              }

              // Get the timestamps from the sorted list
              String timestamp1 = historyList.get(0).getTimestamp();
              String timestamp2 = historyList.get(1).getTimestamp();
              String timestamp3 = historyList.get(2).getTimestamp();

              System.out.println(
                  "Actual timestamps: " + timestamp1 + ", " + timestamp2 + ", " + timestamp3);
              System.out.println(
                  "Expected timestamps: "
                      + oldTimeFormatted
                      + ", "
                      + midTimeFormatted
                      + ", "
                      + newTimeFormatted);

              // Check the timestamps match our expectations (oldest first)
              return timestamp1.equals(oldTimeFormatted)
                  && timestamp2.equals(midTimeFormatted)
                  && timestamp3.equals(newTimeFormatted);
            })
        .verifyComplete();
  }
}
