package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BRDControllerTest {

  @Mock private IBRDService brdService;
  @Mock private BRDSecurityService securityService;
  @Mock private IAuditLogService auditLogService;
  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;
  @Mock private DtoModelMapper dtoModelMapper;
  @InjectMocks private BRDController brdController;

  private BRDRequest validBrdRequest;
  private BRDResponse validBrdResponse;
  private BRDSectionResponse<Object> validSectionResponse;
  private BRDListResponse validBrdListResponse;

  @BeforeEach
  void setUp() {
    // Initialize valid BRD request
    validBrdRequest = new BRDRequest();
    validBrdRequest.setBrdId("BRD123");
    validBrdRequest.setStatus("Draft");
    validBrdRequest.setProjectId("PRJ-123");
    validBrdRequest.setBrdName("Test BRD");
    validBrdRequest.setDescription("Test Description");
    validBrdRequest.setCustomerId("0010g00001imw8xAAA");
    validBrdRequest.setClientInformation(new ClientInformation());

    // Initialize valid BRD response
    validBrdResponse = new BRDResponse();
    validBrdResponse.setBrdFormId("123");
    validBrdResponse.setBrdId("BRD123");
    validBrdResponse.setStatus("Draft");
    validBrdResponse.setProjectId("PRJ-123");
    validBrdResponse.setBrdName("Test BRD");
    validBrdResponse.setDescription("Test Description");
    validBrdResponse.setCustomerId("0010g00001imw8xAAA");
    validBrdResponse.setAchUploadedOn(LocalDateTime.now().minusDays(1));
    validBrdResponse.setWalletronUploadedOn(LocalDateTime.now().minusHours(2));

    // Initialize valid section response
    validSectionResponse = new BRDSectionResponse<>();
    validSectionResponse.setBrdFormId("123");
    validSectionResponse.setBrdId("BRD123");
    validSectionResponse.setSectionName("clientInformation");
    validSectionResponse.setSectionData(new Object());

    // Initialize valid BRD list response
    validBrdListResponse = new BRDListResponse();
    validBrdListResponse.setBrdId("BRD123");
    validBrdListResponse.setBrdFormId("123");
    validBrdListResponse.setCreator("Test User");
    validBrdListResponse.setType("Test Type");
    validBrdListResponse.setNotes("Test Notes");
    validBrdListResponse.setStatus("Draft");

    BRDSearchResponse searchResponse = new BRDSearchResponse();
    searchResponse.setBrdFormId("123");
    searchResponse.setBrdId("BRD123");
    searchResponse.setCustomerId("0010g00001imw8xAAA");
    searchResponse.setBrdName("Test BRD");
    searchResponse.setCreator("Test User");
    searchResponse.setType("Test Type");
    searchResponse.setStatus("Draft");
    searchResponse.setNotes("Test Notes");
  }

  @Test
  @DisplayName("Should create BRD successfully when valid request is provided")
  void saveBrdForm_WithValidRequest_ShouldCreateBrd() {
    // Arrange
    Api<BRDResponse> apiResponse =
        new Api<>("Successful", "BRD created", Optional.of(validBrdResponse), Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity =
        ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.createBrdForm(any(BRDRequest.class))).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.saveBrdForm(Mono.just(validBrdRequest));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD created", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("BRD123", response.getBody().getData().get().getBrdId());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).createBrdForm(any(BRDRequest.class));
    verify(securityService, times(1)).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should return 409 when BRD with same ID already exists")
  void saveBrdForm_WithDuplicateBrdId_ShouldReturn409() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.createBrdForm(any(BRDRequest.class)))
        .thenReturn(Mono.error(new AlreadyExistException("BRD already exists with id: BRD-1234")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.saveBrdForm(Mono.just(validBrdRequest));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof AlreadyExistException
                    && throwable.getMessage().equals("BRD already exists with id: BRD-1234"))
        .verify();

    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).createBrdForm(any(BRDRequest.class));
    verifyNoMoreInteractions(brdService, securityService);
  }

  @Test
  @DisplayName("Should return 400 when invalid request is provided")
  void saveBrdForm_WithInvalidRequest_ShouldReturn400() {
    // Arrange
    BRDRequest invalidRequest = new BRDRequest();
    invalidRequest.setStatus("INVALID_STATUS");
    invalidRequest.setProjectId("PRJ-123456");
    invalidRequest.setBrdId("BRD-1234");

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.createBrdForm(any(BRDRequest.class)))
        .thenReturn(Mono.error(new BadRequestException("Invalid status value")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.saveBrdForm(Mono.just(invalidRequest));

    // Assert
    StepVerifier.create(result).expectError(BadRequestException.class).verify();

    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).createBrdForm(any(BRDRequest.class));
    verifyNoMoreInteractions(brdService, securityService);
  }

  @Test
  @DisplayName("Should return 403 when user does not have the required role")
  void saveBrdForm_WithInvalidRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.saveBrdForm(Mono.just(validBrdRequest));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, never()).createBrdForm(any(BRDRequest.class));
  }

  @Test
  @DisplayName("Should get BRD by ID successfully when valid ID is provided")
  void getBrdByID_WithValidId_ShouldReturnBrd() {
    // Arrange
    String brdId = "BRD-123";
    Api<BRDResponse> apiResponse =
        new Api<>("Successful", "BRD Found", Optional.of(validBrdResponse), Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdId)).thenReturn(Mono.just(responseEntity));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdController.getBrdByID(brdId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD Found", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("BRD123", response.getBody().getData().get().getBrdId());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdId);
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should return 404 when BRD with given ID does not exist")
  void getBrdByID_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    String brdId = "BRD-1234";
    when(brdService.getBrdById(brdId))
        .thenReturn(Mono.error(new NotFoundException("BRD not found with id: " + brdId)));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdController.getBrdByID(brdId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("BRD not found with id: " + brdId, response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should handle internal server error when getting BRD by ID")
  void getBrdByID_WithInternalServerError_ShouldPropagateError() {
    // Arrange
    String brdId = "BRD-1234";
    when(brdService.getBrdById(brdId))
        .thenReturn(Mono.error(new RuntimeException("Internal server error")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdController.getBrdByID(brdId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "An error occurred while processing your request",
                  response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should test direct controller method with error in request Mono")
  void saveBrdForm_DirectControllerMethod_WithErrorInRequestMono() {
    // Arrange
    ServerWebInputException expectedError = new ServerWebInputException("Invalid input");

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.saveBrdForm(Mono.error(expectedError));

    // Assert
    StepVerifier.create(result).expectError(ServerWebInputException.class).verify();

    verifyNoInteractions(brdService, securityService);
  }

  @Test
  @DisplayName("Should update BRD partially when valid fields are provided - 200 OK")
  void updateBrdPartially_WithValidFields_ShouldUpdateBrd() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_REVIEW");
    fields.put("clientInformation", Map.of("companyName", "Updated Company"));

    validBrdResponse.setBrdId("BRD-123");
    validBrdResponse.setStatus("IN_REVIEW");

    Api<BRDResponse> apiResponse =
        new Api<>(
            "Successful",
            "BRD updated successfully",
            Optional.of(validBrdResponse),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("BRD-123", response.getBody().getData().get().getBrdId());
              assertEquals("IN_REVIEW", response.getBody().getData().get().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should return 400 when invalid fields are provided - Bad Request")
  void updateBrdPartially_WithInvalidFields_ShouldReturn400() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> invalidFields = new HashMap<>();
    invalidFields.put("invalidField", "value");

    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(brdService.updateBrdPartiallyWithOrderedOperations(brdFormId, invalidFields))
        .thenReturn(Mono.error(new BadRequestException("Invalid field: invalidField")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(invalidFields));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Invalid field: invalidField"))
        .verify();

    verify(securityService, times(1)).withSecurityCheck(anyString());
    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).updateBrdPartiallyWithOrderedOperations(brdFormId, invalidFields);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should return 404 when BRD does not exist - Not Found")
  void updateBrdPartially_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_REVIEW");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD not found",
                        Optional.empty(),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with ID: " + brdFormId))
        .verify();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should return 409 when update causes conflict - Conflict")
  void updateBrdPartially_WithConflict_ShouldReturn409() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("brdId", "BRD-5678"); // Trying to update to an existing BRD ID

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.error(new AlreadyExistException("BRD already exists with id: BRD-5678")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result).expectError(AlreadyExistException.class).verify();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
  }

  @Test
  @DisplayName("Should handle internal server error - 500 Internal Server Error")
  void updateBrdPartially_WithInternalServerError_ShouldReturn500() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_REVIEW");

    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(brdService.updateBrdPartiallyWithOrderedOperations(brdFormId, fields))
        .thenReturn(Mono.error(new RuntimeException("Internal server error")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Internal server error"))
        .verify();

    verify(securityService, times(1)).withSecurityCheck(anyString());
    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).updateBrdPartiallyWithOrderedOperations(brdFormId, fields);
  }

  @Test
  @DisplayName("Should handle error in request Mono")
  void updateBrdPartially_WithErrorInRequestMono_ShouldPropagateError() {
    // Arrange
    String brdFormId = "12345";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD not found",
                        Optional.empty(),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(new HashMap<>()));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with ID: " + brdFormId))
        .verify();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
    verifyNoInteractions(securityService);
  }

  @Test
  @DisplayName("Should handle empty fields map")
  void updateBrdPartially_WithEmptyFieldsMap_ShouldPassEmptyMapToService() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> emptyFields = new HashMap<>();

    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(brdService.updateBrdPartiallyWithOrderedOperations(brdFormId, emptyFields))
        .thenReturn(Mono.error(new BadRequestException("No fields to update")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(emptyFields));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("No fields to update"))
        .verify();

    verify(securityService, times(1)).withSecurityCheck(anyString());
    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).updateBrdPartiallyWithOrderedOperations(brdFormId, emptyFields);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should handle null fields in map")
  void updateBrdPartially_WithNullFieldsInMap_ShouldPassMapWithNullsToService() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fieldsWithNulls = new HashMap<>();
    fieldsWithNulls.put("status", "IN_REVIEW");
    fieldsWithNulls.put("projectId", null);

    validBrdResponse.setBrdId("BRD-123");
    validBrdResponse.setStatus("IN_REVIEW");
    validBrdResponse.setProjectId(null);

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD updated successfully",
            Optional.of(validBrdResponse),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fieldsWithNulls));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("BRD-123", response.getBody().getData().get().getBrdId());
              assertEquals("IN_REVIEW", response.getBody().getData().get().getStatus());
              assertNull(response.getBody().getData().get().getProjectId());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName(
      "Should get BRD section successfully when valid ID and section name are provided - 200 OK")
  void getBrdSectionByID_WithValidIdAndSection_ShouldReturnSection() {
    // Arrange
    String brdFormId = "507f1f77bcf86cd799439011";
    String sectionName = "clientInformation";

    Map<String, Object> clientInfoMap = new HashMap<>();
    clientInfoMap.put("companyName", "Test Company");
    clientInfoMap.put("customerServicePhoneNumber", "800-555-1234");

    BRDSectionResponse<Object> sectionResponse =
        new BRDSectionResponse<>(brdFormId, "BRD-1234", sectionName, clientInfoMap);

    Api<BRDSectionResponse<Object>> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "Section retrieved successfully",
            Optional.of(sectionResponse),
            Optional.empty());
    ResponseEntity<Api<BRDSectionResponse<Object>>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdSectionById(brdFormId, sectionName))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> result =
        brdController.getBrdSectionByID(brdFormId, sectionName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("Section retrieved successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(brdFormId, response.getBody().getData().get().getBrdFormId());
              assertEquals(sectionName, response.getBody().getData().get().getSectionName());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).getBrdSectionById(brdFormId, sectionName);
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should return 400 when invalid section name is provided - Bad Request")
  void getBrdSectionByID_WithInvalidSectionName_ShouldReturn400() {
    // Arrange
    String brdFormId = "507f1f77bcf86cd799439011";
    String invalidSectionName = "invalidSection";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdSectionById(brdFormId, invalidSectionName))
        .thenReturn(
            Mono.error(new BadRequestException("Invalid section name: " + invalidSectionName)));

    // Act
    Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> result =
        brdController.getBrdSectionByID(brdFormId, invalidSectionName);

    // Assert
    StepVerifier.create(result).expectError(BadRequestException.class).verify();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).getBrdSectionById(brdFormId, invalidSectionName);
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should return 404 when BRD does not exist - Not Found")
  void getBrdSectionByID_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    String brdFormId = "507f1f77bcf86cd799439011";
    String sectionName = "clientInformation";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.empty(),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> result =
        brdController.getBrdSectionByID(brdFormId, sectionName);

    // Assert
    StepVerifier.create(result).expectError(NotFoundException.class).verify();

    verify(brdService).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should handle internal server error - 500 Internal Server Error")
  void getBrdSectionByID_WithInternalServerError_ShouldReturn500() {
    // Arrange
    String brdFormId = "507f1f77bcf86cd799439011";
    String sectionName = "clientInformation";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdSectionById(brdFormId, sectionName))
        .thenReturn(Mono.error(new RuntimeException("Internal server error")));

    // Act
    Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> result =
        brdController.getBrdSectionByID(brdFormId, sectionName);

    // Assert
    StepVerifier.create(result).expectError(RuntimeException.class).verify();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1)).getBrdSectionById(brdFormId, sectionName);
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should handle forbidden access - 403 Forbidden")
  void updateBrdPartially_WithForbiddenAccess_ShouldReturn403() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_REVIEW");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.error(new AccessDeniedException("Access denied")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("Access denied", response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdById(brdFormId);
    verify(securityService).withSecurityCheck(anyString());
    verifyNoMoreInteractions(brdService, securityService);
  }

  @Test
  @DisplayName("Should update BRD status successfully when valid request is provided by PM role")
  void updateBrdStatus_WithValidRequestAndPMRole_ShouldUpdateStatus() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");
    statusRequest.setComment("Status update comment");

    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId(brdFormId);
    updatedResponse.setBrdId("BRD-123");
    updatedResponse.setStatus("In Review");

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD status updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.updateBrdStatus(brdFormId, "In Review", "Status update comment"))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD status updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("In Review", response.getBody().getData().get().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).updateBrdStatus(brdFormId, "In Review", "Status update comment");
  }

  @Test
  @DisplayName(
      "Should update BRD status successfully when valid request is provided by Biller role")
  void updateBrdStatus_WithValidRequestAndBillerRole_ShouldUpdateStatus() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");
    statusRequest.setComment("");

    validBrdResponse.setStatus("In Progress"); // Current status that Biller can update

    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId(brdFormId);
    updatedResponse.setBrdId("BRD-123");
    updatedResponse.setStatus("In Review");

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD status updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BILLER"));
    when(brdService.updateBrdStatus(brdFormId, "In Review", ""))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD status updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("In Review", response.getBody().getData().get().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).updateBrdStatus(brdFormId, "In Review", "");
  }

  @Test
  @DisplayName("Should update BRD status successfully when valid request is provided by BA role")
  void updateBrdStatus_WithValidRequestAndBARole_ShouldUpdateStatus() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");
    statusRequest.setComment(null);

    validBrdResponse.setStatus("Internal Review"); // Current status that BA can update

    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId(brdFormId);
    updatedResponse.setBrdId("BRD-123");
    updatedResponse.setStatus("In Review");

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD status updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));
    when(brdService.updateBrdStatus(brdFormId, "In Review", null))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD status updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("In Review", response.getBody().getData().get().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).updateBrdStatus(brdFormId, "In Review", null);
  }

  @Test
  @DisplayName("Should return 404 when BRD does not exist")
  void updateBrdStatus_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD not found",
                        Optional.empty(),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(response.getBody().getMessage().contains("BRD not found"));
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(securityService, brdService);
  }

  @Test
  @DisplayName("Should return 403 when user role is not allowed to update BRD status")
  void updateBrdStatus_WithUnauthorizedRole_ShouldReturn403() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole())
        .thenReturn(
            Mono.error(
                new AccessDeniedException(
                    "Only PM, Biller, and BA roles are allowed to update BRD status")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(response.getBody().getMessage().contains("Only PM, Biller, and BA roles"));
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should return 403 when Biller tries to update a BRD not in In Progress state")
  void updateBrdStatus_WithBillerRoleAndInvalidState_ShouldReturn403() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");

    validBrdResponse.setStatus("Draft"); // Current status that Biller CANNOT update

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole())
        .thenReturn(
            Mono.error(
                new AccessDeniedException(
                    "Biller can only update BRD status when it is in 'In Progress' state")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(
                  response
                      .getBody()
                      .getMessage()
                      .contains("only update BRD status when it is in 'In Progress'"));
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should return 403 when BA tries to update a BRD not in Internal Review state")
  void updateBrdStatus_WithBARoleAndInvalidState_ShouldReturn403() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");

    validBrdResponse.setStatus("Draft"); // Current status that BA CANNOT update

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole())
        .thenReturn(
            Mono.error(
                new AccessDeniedException(
                    "BA can only update BRD status when it is in 'Internal Review' state")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(
                  response
                      .getBody()
                      .getMessage()
                      .contains("only update BRD status when it is in 'Internal Review'"));
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoMoreInteractions(brdService);
  }

  @Test
  @DisplayName("Should return 400 when invalid status is provided")
  void updateBrdStatus_WithInvalidStatus_ShouldReturn400() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("INVALID_STATUS");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.updateBrdStatus(brdFormId, "INVALID_STATUS", null))
        .thenReturn(Mono.error(new BadRequestException("Invalid status value: INVALID_STATUS")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("Invalid status update request", response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).updateBrdStatus(brdFormId, "INVALID_STATUS", null);
  }

  @Test
  @DisplayName("Should handle internal server error")
  void updateBrdStatus_WithInternalServerError_ShouldHandleError() {
    // Arrange
    String brdFormId = "12345";
    BrdStatusUpdateRequest statusRequest = new BrdStatusUpdateRequest();
    statusRequest.setStatus("In Review");

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.updateBrdStatus(brdFormId, "In Review", null))
        .thenReturn(Mono.error(new RuntimeException("Database connection error")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdStatus(brdFormId, statusRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("Invalid status update request", response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdService, times(1)).updateBrdStatus(brdFormId, "In Review", null);
  }

  @Test
  @DisplayName("Should retrieve paginated BRD list successfully")
  void getBrdList_WithValidParameters_ShouldReturnBrdList() {
    // Arrange
    int page = 0;
    int size = 10;

    List<BRDListResponse> brdList = Arrays.asList(validBrdListResponse);
    BRDCountDataResponse countDataResponse = new BRDCountDataResponse(1, brdList);

    Api<BRDCountDataResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD list retrieved successfully",
            Optional.of(countDataResponse),
            Optional.empty());

    ResponseEntity<Api<BRDCountDataResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdList(page, size)).thenReturn(Mono.just(responseEntity));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<BRDCountDataResponse>>> result = brdController.getBrdList(page, size);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD list retrieved successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(1, response.getBody().getData().get().getTotalCount());
              assertEquals(1, response.getBody().getData().get().getBrdList().size());
              assertEquals(
                  "BRD123", response.getBody().getData().get().getBrdList().get(0).getBrdId());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdList(page, size);
  }

  @Test
  @DisplayName("Should handle empty BRD list")
  void getBrdList_WithEmptyList_ShouldReturnEmptyList() {
    // Arrange
    int page = 0;
    int size = 10;

    BRDCountDataResponse emptyResponse = new BRDCountDataResponse(0, Collections.emptyList());

    Api<BRDCountDataResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL, "No BRDs found", Optional.of(emptyResponse), Optional.empty());

    ResponseEntity<Api<BRDCountDataResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdList(page, size)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDCountDataResponse>>> result = brdController.getBrdList(page, size);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("No BRDs found", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(0, response.getBody().getData().get().getTotalCount());
              assertTrue(response.getBody().getData().get().getBrdList().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdList(page, size);
  }

  @Test
  @DisplayName("Should filter BRDs based on security permissions")
  void getBrdList_WithSecurityFiltering_ShouldFilterBrdList() {
    // Arrange
    int page = 0;
    int size = 10;

    // Create two BRD responses, one that will pass security check and one that won't
    BRDListResponse accessibleBrd = new BRDListResponse();
    accessibleBrd.setBrdId("BRD123");
    accessibleBrd.setBrdFormId("123");
    accessibleBrd.setStatus("Draft");

    BRDListResponse restrictedBrd = new BRDListResponse();
    restrictedBrd.setBrdId("BRD456");
    restrictedBrd.setBrdFormId("456");
    restrictedBrd.setStatus("Confidential");

    List<BRDListResponse> brdList = Arrays.asList(accessibleBrd, restrictedBrd);
    BRDCountDataResponse countDataResponse = new BRDCountDataResponse(2, brdList);

    Api<BRDCountDataResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD list retrieved successfully",
            Optional.of(countDataResponse),
            Optional.empty());

    ResponseEntity<Api<BRDCountDataResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdList(page, size)).thenReturn(Mono.just(responseEntity));

    // Set up security service to allow access to "Draft" status but deny "Confidential"
    when(securityService.withSecurityCheck("Draft")).thenReturn(Mono.empty());

    when(securityService.withSecurityCheck("Confidential"))
        .thenReturn(Mono.error(new AccessDeniedException("Access denied")));

    // Act
    Mono<ResponseEntity<Api<BRDCountDataResponse>>> result = brdController.getBrdList(page, size);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD list retrieved successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(
                  2,
                  response
                      .getBody()
                      .getData()
                      .get()
                      .getTotalCount()); // Total count remains the same
              assertEquals(
                  1,
                  response
                      .getBody()
                      .getData()
                      .get()
                      .getBrdList()
                      .size()); // But filtered list has only one item
              assertEquals(
                  "BRD123", response.getBody().getData().get().getBrdList().get(0).getBrdId());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdList(page, size);
    verify(securityService, times(1)).withSecurityCheck("Draft");
    verify(securityService, times(1)).withSecurityCheck("Confidential");
  }

  @Test
  @DisplayName("Should handle BadRequestException from service")
  void getBrdList_WithBadRequest_ShouldPropagateError() {
    // Arrange
    int page = -1; // Invalid page number
    int size = 10;

    when(brdService.getBrdList(page, size))
        .thenReturn(
            Mono.error(
                new BadRequestException(
                    "Page number must be non-negative and size must be greater than 0.")));

    // Act
    Mono<ResponseEntity<Api<BRDCountDataResponse>>> result = brdController.getBrdList(page, size);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable
                        .getMessage()
                        .equals(
                            "Page number must be non-negative and size must be greater than 0."))
        .verify();

    verify(brdService, times(1)).getBrdList(page, size);
  }

  @Test
  @DisplayName("Should handle NotFoundException from service")
  void getBrdList_WithNotFound_ShouldPropagateError() {
    // Arrange
    int page = 100; // Page that doesn't exist
    int size = 10;

    when(brdService.getBrdList(page, size))
        .thenReturn(
            Mono.error(
                new NotFoundException(
                    "No BRD sections found for the given pagination parameters.")));

    // Act
    Mono<ResponseEntity<Api<BRDCountDataResponse>>> result = brdController.getBrdList(page, size);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable
                        .getMessage()
                        .equals("No BRD sections found for the given pagination parameters."))
        .verify();

    verify(brdService, times(1)).getBrdList(page, size);
  }

  @Test
  @DisplayName("Should get BRD status history successfully")
  void getBrdStatusHistory_Successfully() {
    // Arrange
    String brdFormId = "123";

    StatusHistoryResponse history1 = new StatusHistoryResponse();
    history1.setStatus("Draft");
    history1.setChangedBy("John Doe");

    StatusHistoryResponse history2 = new StatusHistoryResponse();
    history2.setStatus("In Progress");
    history2.setChangedBy("Jane Smith");

    List<StatusHistoryResponse> historyList = Arrays.asList(history1, history2);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(auditLogService.fetchStatusHistory(eq(brdFormId), anyList()))
        .thenReturn(Mono.just(historyList));

    // Act
    Mono<ResponseEntity<Api<List<StatusHistoryResponse>>>> result =
        brdController.getBrdStatusHistory(brdFormId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(2, response.getBody().getData().get().size());
              assertEquals("Draft", response.getBody().getData().get().get(0).getStatus());
              assertEquals("In Progress", response.getBody().getData().get().get(1).getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).withSecurityCheck(anyString());
    verify(auditLogService, times(1)).fetchStatusHistory(eq(brdFormId), anyList());
  }

  @Test
  @DisplayName("Should handle not found exception when getting BRD status history")
  void getBrdStatusHistory_NotFound() {
    // Arrange
    String brdFormId = "nonexistent";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(Mono.error(new NotFoundException("BRD not found with id: " + brdFormId)));

    // Act
    Mono<ResponseEntity<Api<List<StatusHistoryResponse>>>> result =
        brdController.getBrdStatusHistory(brdFormId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(response.getBody().getMessage().contains("BRD not found"));
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verifyNoInteractions(auditLogService);
  }

  @Test
  @DisplayName("Should handle access denied exception when getting BRD status history")
  void getBrdStatusHistory_AccessDenied() {
    // Arrange
    String brdFormId = "123";

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString()))
        .thenThrow(new AccessDeniedException("Access denied"));

    // Act
    Mono<ResponseEntity<Api<List<StatusHistoryResponse>>>> result =
        brdController.getBrdStatusHistory(brdFormId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("Access denied", response.getBody().getMessage());
              assertFalse(response.getBody().getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(securityService, times(1)).withSecurityCheck(anyString());
    verifyNoInteractions(auditLogService);
  }

  @Test
  @DisplayName("Should update aiPrefillRate field via partial update")
  void updateBrdPartially_WithAiPrefillRate_ShouldUpdateBrd() {
    // Arrange
    String brdFormId = "12345";
    Double newAiPrefillRate = 85.5;

    Map<String, Object> fields = new HashMap<>();
    fields.put("aiPrefillRate", newAiPrefillRate);

    BRDResponse validBrdResponseData = new BRDResponse();
    validBrdResponseData.setBrdFormId(brdFormId);
    validBrdResponseData.setBrdId("BRD-123");
    validBrdResponseData.setStatus("Draft");
    validBrdResponseData.setAiPrefillRate(newAiPrefillRate);

    Api<BRDResponse> apiResponse =
        new Api<>(
            "Successful",
            "BRD updated successfully",
            Optional.of(validBrdResponseData),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponseData),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(newAiPrefillRate, response.getBody().getData().get().getAiPrefillRate());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
    verify(securityService, times(1)).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should set aiPrefillRate to null when null value is provided")
  void updateBrdPartially_WithNullAiPrefillRate_ShouldUpdateBrdWithNull() {
    // Arrange
    String brdFormId = "12345";

    Map<String, Object> fields = new HashMap<>();
    fields.put("aiPrefillRate", null);

    BRDResponse validBrdResponseData = new BRDResponse();
    validBrdResponseData.setBrdFormId(brdFormId);
    validBrdResponseData.setBrdId("BRD-123");
    validBrdResponseData.setStatus("Draft");
    validBrdResponseData.setAiPrefillRate(null);

    Api<BRDResponse> apiResponse =
        new Api<>(
            "Successful",
            "BRD updated successfully",
            Optional.of(validBrdResponseData),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponseData),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertNull(response.getBody().getData().get().getAiPrefillRate());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
  }

  @Test
  @DisplayName("Should update aiPrefillRate along with other fields")
  void updateBrdPartially_WithAiPrefillRateAndOtherFields_ShouldUpdateAllFields() {
    // Arrange
    String brdFormId = "12345";
    Double newAiPrefillRate = 65.0;
    String newStatus = "In Progress";

    Map<String, Object> fields = new HashMap<>();
    fields.put("aiPrefillRate", newAiPrefillRate);
    fields.put("status", newStatus);

    BRDResponse validBrdResponseData = new BRDResponse();
    validBrdResponseData.setBrdFormId(brdFormId);
    validBrdResponseData.setBrdId("BRD-123");
    validBrdResponseData.setStatus(newStatus);
    validBrdResponseData.setAiPrefillRate(newAiPrefillRate);

    Api<BRDResponse> apiResponse =
        new Api<>(
            "Successful",
            "BRD updated successfully",
            Optional.of(validBrdResponseData),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD found",
                        Optional.of(validBrdResponseData),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class)))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially(brdFormId, Mono.just(fields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals("Successful", response.getBody().getStatus());
              assertEquals("BRD updated successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(newAiPrefillRate, response.getBody().getData().get().getAiPrefillRate());
              assertEquals(newStatus, response.getBody().getData().get().getStatus());
              return true;
            })
        .verifyComplete();

    verify(brdService, times(1)).getBrdById(brdFormId);
    verify(brdService, times(1))
        .updateBrdPartiallyWithOrderedOperations(eq(brdFormId), any(Map.class));
  }

  @Test
  @DisplayName("Should update ssdAvailable flag successfully")
  void updateBrdPartially_WithSsdAvailableFlag_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("ssdAvailable", true);

    // Create a response with updated ssdAvailable flag
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("123");
    updatedResponse.setBrdId("BRD123");
    updatedResponse.setStatus("Draft");
    updatedResponse.setProjectId("PRJ-123");
    updatedResponse.setSsdAvailable(true);

    // Create API response
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());

    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    // Mock service responses
    when(brdService.getBrdById("123"))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD Found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));

    when(securityService.withSecurityCheck("Draft")).thenReturn(Mono.empty());

    when(brdService.updateBrdPartiallyWithOrderedOperations("123", updateFields))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially("123", Mono.just(updateFields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isSsdAvailable());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should update contractAvailable flag successfully")
  void updateBrdPartially_WithContractAvailableFlag_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("contractAvailable", true);

    // Create a response with updated contractAvailable flag
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("123");
    updatedResponse.setBrdId("BRD123");
    updatedResponse.setStatus("Draft");
    updatedResponse.setProjectId("PRJ-123");
    updatedResponse.setContractAvailable(true);

    // Create API response
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());

    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    // Mock service responses
    when(brdService.getBrdById("123"))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD Found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));

    when(securityService.withSecurityCheck("Draft")).thenReturn(Mono.empty());

    when(brdService.updateBrdPartiallyWithOrderedOperations("123", updateFields))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially("123", Mono.just(updateFields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isContractAvailable());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should update both flags simultaneously")
  void updateBrdPartially_WithBothFlags_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("ssdAvailable", true);
    updateFields.put("contractAvailable", true);

    // Create a response with both flags updated
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("123");
    updatedResponse.setBrdId("BRD123");
    updatedResponse.setStatus("Draft");
    updatedResponse.setProjectId("PRJ-123");
    updatedResponse.setSsdAvailable(true);
    updatedResponse.setContractAvailable(true);

    // Create API response
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD updated successfully",
            Optional.of(updatedResponse),
            Optional.empty());

    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    // Mock service responses
    when(brdService.getBrdById("123"))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "BRD Found",
                        Optional.of(validBrdResponse),
                        Optional.empty()))));

    when(securityService.withSecurityCheck("Draft")).thenReturn(Mono.empty());

    when(brdService.updateBrdPartiallyWithOrderedOperations("123", updateFields))
        .thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially("123", Mono.just(updateFields));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isSsdAvailable());
              assertTrue(response.getBody().getData().get().isContractAvailable());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BRD not found when updating flags")
  void updateBrdPartially_WithFlagUpdatesAndNotFoundBrd_ShouldReturn404() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("ssdAvailable", true);

    // Mock service responses
    when(brdService.getBrdById("non-existent"))
        .thenReturn(Mono.error(new NotFoundException("BRD not found with id: non-existent")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdController.updateBrdPartially("non-existent", Mono.just(updateFields));

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with id: non-existent"))
        .verify();
  }

  @Test
  @DisplayName("Should get industry verticals successfully when user has PM role")
  void getIndustryVerticals_WithPMRole_ShouldReturnVerticals() {
    // Arrange
    List<String> verticals = Arrays.asList("Retail", "Healthcare", "Other");
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.getIndustryVerticals()).thenReturn(Mono.just(verticals));

    // Act & Assert
    StepVerifier.create(brdController.getIndustryVerticals())
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals("Success", apiResponse.getStatus());
              assertEquals("Industry verticals retrieved successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(verticals, apiResponse.getData().get());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(brdService).getIndustryVerticals();
  }

  @Test
  @DisplayName("Should return forbidden when user does not have PM role")
  void getIndustryVerticals_WithoutPMRole_ShouldReturnForbidden() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act & Assert
    StepVerifier.create(brdController.getIndustryVerticals())
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals("Error", apiResponse.getStatus());
              assertEquals("Access denied. Only PM role is allowed.", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(brdService, never()).getIndustryVerticals();
  }

  @Test
  @DisplayName("Should handle error when fetching industry verticals")
  void getIndustryVerticals_WithError_ShouldReturnInternalServerError() {
    // Arrange
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdService.getIndustryVerticals())
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(brdController.getIndustryVerticals())
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals("Error", apiResponse.getStatus());
              assertEquals("Failed to retrieve industry verticals", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(brdService).getIndustryVerticals();
  }

  @Test
  @DisplayName("Should get BRD with file upload timestamps successfully")
  void getBrdByID_WithFileUploadTimestamps_ShouldReturnBrd() {
    // Arrange
    String brdFormId = "12345";
    LocalDateTime achUploadTime = LocalDateTime.now().minusDays(1);
    LocalDateTime walletronUploadTime = LocalDateTime.now().minusHours(2);

    validBrdResponse.setAchUploadedOn(achUploadTime);
    validBrdResponse.setWalletronUploadedOn(walletronUploadTime);

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL, "BRD Found", Optional.of(validBrdResponse), Optional.empty());
    ResponseEntity<Api<BRDResponse>> responseEntity = ResponseEntity.ok(apiResponse);

    when(brdService.getBrdById(brdFormId)).thenReturn(Mono.just(responseEntity));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdController.getBrdByID(brdFormId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("BRD Found", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              BRDResponse brdResponse = response.getBody().getData().get();
              assertEquals(achUploadTime, brdResponse.getAchUploadedOn());
              assertEquals(walletronUploadTime, brdResponse.getWalletronUploadedOn());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdById(brdFormId);
    verify(securityService).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Get BRDs by PM username - Success")
  void getBrdsByPmUsername_Success() {
    // Given
    String pmUsername = "test.pm@example.com";
    List<BRDResponse> expectedBrds =
        Arrays.asList(
            BRDResponse.builder().brdFormId("brd1").creator(pmUsername).status("Draft").build(),
            BRDResponse.builder()
                .brdFormId("brd2")
                .creator(pmUsername)
                .status("In Progress")
                .build());

    when(brdService.getBrdsByPmUsername(pmUsername)).thenReturn(Mono.just(expectedBrds));

    // When
    Mono<ResponseEntity<Api<List<BRDResponse>>>> result =
        brdController.getBrdsByPmUsername(pmUsername);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<BRDResponse>> body = response.getBody();
              assertNotNull(body);
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertEquals("BRDs retrieved successfully", body.getMessage());
              assertTrue(body.getData().isPresent());
              assertEquals(expectedBrds, body.getData().get());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdsByPmUsername(pmUsername);
  }

  @Test
  @DisplayName("Get BRDs by PM username - Not Found")
  void getBrdsByPmUsername_NotFound() {
    // Given
    String pmUsername = "nonexistent.pm@example.com";
    when(brdService.getBrdsByPmUsername(pmUsername))
        .thenReturn(Mono.error(new NotFoundException("No BRDs found for PM: " + pmUsername)));

    // When
    Mono<ResponseEntity<Api<List<BRDResponse>>>> result =
        brdController.getBrdsByPmUsername(pmUsername);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<List<BRDResponse>> body = response.getBody();
              assertNotNull(body);
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertEquals("No BRDs found for PM: " + pmUsername, body.getMessage());
              assertTrue(body.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdsByPmUsername(pmUsername);
  }

  @Test
  @DisplayName("Get BRDs by PM username - Forbidden")
  void getBrdsByPmUsername_Forbidden() {
    // Given
    String pmUsername = "test.pm@example.com";
    when(brdService.getBrdsByPmUsername(pmUsername))
        .thenReturn(
            Mono.error(
                new AccessDeniedException(
                    "Access denied. Only Manager role can access this endpoint.")));

    // When
    Mono<ResponseEntity<Api<List<BRDResponse>>>> result =
        brdController.getBrdsByPmUsername(pmUsername);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<BRDResponse>> body = response.getBody();
              assertNotNull(body);
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertEquals(
                  "Access denied. Only Manager role can access this endpoint.", body.getMessage());
              assertTrue(body.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdsByPmUsername(pmUsername);
  }

  @Test
  @DisplayName("Get BRDs by PM username - Internal Server Error")
  void getBrdsByPmUsername_InternalServerError() {
    // Given
    String pmUsername = "test.pm@example.com";
    when(brdService.getBrdsByPmUsername(pmUsername))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    // When
    Mono<ResponseEntity<Api<List<BRDResponse>>>> result =
        brdController.getBrdsByPmUsername(pmUsername);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertNotNull(response);
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<List<BRDResponse>> body = response.getBody();
              assertNotNull(body);
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertEquals("An error occurred while retrieving BRDs", body.getMessage());
              assertTrue(body.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdService).getBrdsByPmUsername(pmUsername);
  }
}
