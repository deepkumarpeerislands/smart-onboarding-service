package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditLogResponseTest {

  @Test
  void constructor_WithAllParameters_ShouldCreateValidInstance() {
    // Given
    String auditId = "audit123";
    String entityType = "BRD";
    String entityId = "brd123";
    String action = "CREATE";
    String userId = "user123";
    String userName = "John Doe";
    String role = "ROLE_PM";
    String comment = "comment";
    LocalDateTime eventTimestamp = LocalDateTime.now();
    Map<String, Object> oldValues = new HashMap<>();
    Map<String, Object> newValues = new HashMap<>();
    oldValues.put("status", "DRAFT");
    newValues.put("status", "SUBMITTED");

    // When
    AuditLogResponse response =
        new AuditLogResponse(
            auditId,
            entityType,
            entityId,
            action,
            userId,
            userName,
            role,
            comment,
            eventTimestamp,
            oldValues,
            newValues);

    // Then
    assertEquals(auditId, response.getAuditId());
    assertEquals(entityType, response.getEntityType());
    assertEquals(entityId, response.getEntityId());
    assertEquals(action, response.getAction());
    assertEquals(userId, response.getUserId());
    assertEquals(userName, response.getUserName());
    assertEquals(eventTimestamp, response.getEventTimestamp());
    assertEquals(oldValues, response.getOldValues());
    assertEquals(newValues, response.getNewValues());
  }

  @Test
  void noArgsConstructor_ShouldCreateEmptyInstance() {
    // When
    AuditLogResponse response = new AuditLogResponse();

    // Then
    assertNull(response.getAuditId());
    assertNull(response.getEntityType());
    assertNull(response.getEntityId());
    assertNull(response.getAction());
    assertNull(response.getUserId());
    assertNull(response.getUserName());
    assertNull(response.getEventTimestamp());
    assertNull(response.getOldValues());
    assertNull(response.getNewValues());
  }

  @Test
  void settersAndGetters_ShouldWorkCorrectly() {
    // Given
    AuditLogResponse response = new AuditLogResponse();
    String auditId = "audit123";
    String entityType = "BRD";
    String entityId = "brd123";
    String action = "UPDATE";
    String userId = "user123";
    String userName = "John Doe";
    LocalDateTime eventTimestamp = LocalDateTime.now();
    Map<String, Object> oldValues = new HashMap<>();
    Map<String, Object> newValues = new HashMap<>();

    // When
    response.setAuditId(auditId);
    response.setEntityType(entityType);
    response.setEntityId(entityId);
    response.setAction(action);
    response.setUserId(userId);
    response.setUserName(userName);
    response.setEventTimestamp(eventTimestamp);
    response.setOldValues(oldValues);
    response.setNewValues(newValues);

    // Then
    assertEquals(auditId, response.getAuditId());
    assertEquals(entityType, response.getEntityType());
    assertEquals(entityId, response.getEntityId());
    assertEquals(action, response.getAction());
    assertEquals(userId, response.getUserId());
    assertEquals(userName, response.getUserName());
    assertEquals(eventTimestamp, response.getEventTimestamp());
    assertEquals(oldValues, response.getOldValues());
    assertEquals(newValues, response.getNewValues());
  }

  @Test
  void equals_WithSameData_ShouldBeEqual() {
    // Given
    LocalDateTime timestamp = LocalDateTime.now();
    Map<String, Object> oldValues = new HashMap<>();
    Map<String, Object> newValues = new HashMap<>();

    AuditLogResponse response1 =
        new AuditLogResponse(
            "audit123",
            "BRD",
            "brd123",
            "CREATE",
            "user123",
            "John Doe",
            "comment",
            "role",
            timestamp,
            oldValues,
            newValues);

    AuditLogResponse response2 =
        new AuditLogResponse(
            "audit123",
            "BRD",
            "brd123",
            "CREATE",
            "user123",
            "John Doe",
            "comment",
            "role",
            timestamp,
            oldValues,
            newValues);

    // Then
    assertEquals(response1, response2);
    assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void equals_WithDifferentData_ShouldNotBeEqual() {
    // Given
    LocalDateTime timestamp = LocalDateTime.now();
    Map<String, Object> values = new HashMap<>();

    AuditLogResponse response1 =
        new AuditLogResponse(
            "audit123",
            "BRD",
            "brd123",
            "CREATE",
            "user123",
            "John Doe",
            "status",
            "role",
            timestamp,
            values,
            values);

    AuditLogResponse response2 =
        new AuditLogResponse(
            "audit456",
            "BRD",
            "brd123",
            "CREATE",
            "user123",
            "John Doe",
            "comment",
            "role",
            timestamp,
            values,
            values);

    // Then
    assertNotEquals(response1, response2);
    assertNotEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  void toString_ShouldContainAllFields() {
    // Given
    LocalDateTime timestamp = LocalDateTime.now();
    AuditLogResponse response =
        new AuditLogResponse(
            "audit123",
            "BRD",
            "brd123",
            "CREATE",
            "user123",
            "John Doe",
            "comment",
            "role",
            timestamp,
            new HashMap<>(),
            new HashMap<>());

    // When
    String toString = response.toString();

    // Then
    assertTrue(toString.contains("auditId=audit123"));
    assertTrue(toString.contains("entityType=BRD"));
    assertTrue(toString.contains("entityId=brd123"));
    assertTrue(toString.contains("action=CREATE"));
    assertTrue(toString.contains("userId=user123"));
    assertTrue(toString.contains("userName=John Doe"));
    assertTrue(toString.contains("eventTimestamp="));
    assertTrue(toString.contains("oldValues="));
    assertTrue(toString.contains("newValues="));
  }
}
