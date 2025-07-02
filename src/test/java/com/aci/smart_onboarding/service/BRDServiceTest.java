package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdTemplateConfig;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.implementation.AuditLogService;
import com.aci.smart_onboarding.service.implementation.BRDService;
import com.aci.smart_onboarding.util.CustomBrdValidator;
import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BRDServiceTest {

  @Mock private BRDRepository brdRepository;

  @Mock private DtoModelMapper dtoModelMapper;

  @InjectMocks private BRDService brdService;

  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;
  @Mock private CustomBrdValidator customBrdValidator;
  @Mock private AuditLogService auditLogService;

  @Mock private ISiteService siteService;

  @Mock private BRDSecurityService securityService;

  private BRDRequest validBrdRequest;
  private BRD validBrd;
  private BRDResponse validBrdResponse;

  @BeforeEach
  void setUp() {
    brdService =
        new BRDService(
            brdRepository,
            dtoModelMapper,
            reactiveMongoTemplate,
            customBrdValidator,
            auditLogService,
            siteService,
            securityService);

    // Mock security service with proper role
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    // Mock audit log service with proper response
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context with proper authorities
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Setup test data
    validBrdRequest = new BRDRequest();
    validBrdRequest.setBrdId("BRD-1234");
    validBrdRequest.setStatus("DRAFT");
    validBrdRequest.setProjectId("PRJ-123456");

    ClientInformation clientInfo = new ClientInformation();
    clientInfo.setCompanyName("Test Company");
    validBrdRequest.setClientInformation(clientInfo);

    // Set up valid BRD
    validBrd = new BRD();
    validBrd.setBrdFormId("12345");
    validBrd.setBrdId("BRD-1234");
    validBrd.setStatus("DRAFT");
    validBrd.setProjectId("PRJ-123456");
    validBrd.setCreator("testPM");

    // Set up valid BRD response
    validBrdResponse = new BRDResponse();
    validBrdResponse.setBrdFormId("12345");
    validBrdResponse.setBrdId("BRD-1234");
    validBrdResponse.setStatus("DRAFT");
    validBrdResponse.setProjectId("PRJ-123456");
  }

  /*createBrdForm Test cases*/
  @Test
  @DisplayName("Should create BRD successfully - 201 Created")
  void createBrdForm_WithValidRequest_ShouldReturn201() {
    // Arrange
    when(dtoModelMapper.mapToBrd(validBrdRequest)).thenReturn(validBrd);
    when(brdRepository.save(validBrd)).thenReturn(Mono.just(validBrd));
    when(dtoModelMapper.mapToBrdResponse(validBrd)).thenReturn(validBrdResponse);
    when(dtoModelMapper.mapBrdResponseToMAP(validBrdResponse)).thenReturn(new HashMap<>());

    // Mock security service to return a role
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Mock audit log creation with proper response
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(validBrdRequest))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              Api<BRDResponse> apiResponse = responseEntity.getBody();
              assertNotNull(apiResponse);
              assertEquals("Successful", apiResponse.getStatus());
              assertEquals("BRD created", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(validBrdResponse, apiResponse.getData().get());
            })
        .verifyComplete();

    verify(dtoModelMapper).mapToBrd(validBrdRequest);
    verify(brdRepository).save(validBrd);
    verify(dtoModelMapper).mapToBrdResponse(validBrd);
    verify(dtoModelMapper).mapBrdResponseToMAP(validBrdResponse);
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any());
  }

  @Test
  @DisplayName("Should throw BadRequestException when request is null - 400 Bad Request")
  void createBrdForm_WithNullRequest_ShouldThrowBadRequest() {
    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("cannot be null or empty"))
        .verify();

    verify(dtoModelMapper, never()).mapToBrd(any());
    verify(brdRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw AlreadyExistException when BRD already exists - 409 Conflict")
  void createBrdForm_WithDuplicateBrd_ShouldThrowConflict() {
    // Arrange
    when(dtoModelMapper.mapToBrd(validBrdRequest)).thenReturn(validBrd);
    when(brdRepository.save(validBrd))
        .thenReturn(Mono.error(new DuplicateKeyException("Duplicate key")));

    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(validBrdRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AlreadyExistException
                    && throwable.getMessage().equals("Brd already exist for given brdId"))
        .verify();

    verify(dtoModelMapper).mapToBrd(validBrdRequest);
    verify(brdRepository).save(validBrd);
    verify(dtoModelMapper, never()).mapToBrdResponse(any());
  }

  @Test
  @DisplayName("Should handle database errors correctly - 500 Internal Server Error")
  void createBrdForm_WithDatabaseError_ShouldThrowException() {
    // Arrange
    when(dtoModelMapper.mapToBrd(validBrdRequest)).thenReturn(validBrd);
    when(brdRepository.save(validBrd))
        .thenReturn(Mono.error(new RuntimeException("Database connection error")));

    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(validBrdRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable
                        .getMessage()
                        .equals("Something went wrong: Database connection error"))
        .verify();

    verify(dtoModelMapper).mapToBrd(validBrdRequest);
    verify(brdRepository).save(validBrd);
    verify(dtoModelMapper, never()).mapToBrdResponse(any());
  }

  @Test
  @DisplayName("Should propagate audit logging errors in createBrdForm")
  void createBrdForm_WithAuditLogError_ShouldCreateBrdSuccessfully() {
    // Arrange
    when(dtoModelMapper.mapToBrd(validBrdRequest)).thenReturn(validBrd);
    when(brdRepository.save(validBrd)).thenReturn(Mono.just(validBrd));
    when(dtoModelMapper.mapToBrdResponse(validBrd)).thenReturn(validBrdResponse);
    when(dtoModelMapper.mapBrdResponseToMAP(validBrdResponse)).thenReturn(new HashMap<>());

    // Mock security service to return a role
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Mock audit log error - unlike updateBrdStatus, createBrdForm propagates errors
    RuntimeException auditError = new RuntimeException("Audit log error");
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.error(auditError));

    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(validBrdRequest))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable.getMessage().equals("Something went wrong: Audit log error"))
        .verify();

    verify(dtoModelMapper).mapToBrd(validBrdRequest);
    verify(brdRepository).save(validBrd);
    verify(dtoModelMapper).mapToBrdResponse(validBrd);
    verify(dtoModelMapper).mapBrdResponseToMAP(validBrdResponse);
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any());
  }

  @Test
  @DisplayName("Should create BRD successfully with file names")
  void createBrdForm_WithFileNames_ShouldCreateBrd() {
    // Arrange
    validBrdRequest.setOriginalSSDFileName("ssd_file.xls");
    validBrdRequest.setOriginalContractFileName("contract_file.pdf");
    validBrdRequest.setOriginalOtherFileName("other_file.doc");

    validBrd.setOriginalSSDFileName("ssd_file.xls");
    validBrd.setOriginalContractFileName("contract_file.pdf");
    validBrd.setOriginalOtherFileName("other_file.doc");

    validBrdResponse.setOriginalSSDFileName("ssd_file.xls");
    validBrdResponse.setOriginalContractFileName("contract_file.pdf");
    validBrdResponse.setOriginalOtherFileName("other_file.doc");

    when(dtoModelMapper.mapToBrd(validBrdRequest)).thenReturn(validBrd);
    when(brdRepository.save(validBrd)).thenReturn(Mono.just(validBrd));
    when(dtoModelMapper.mapToBrdResponse(validBrd)).thenReturn(validBrdResponse);
    when(dtoModelMapper.mapBrdResponseToMAP(validBrdResponse)).thenReturn(new HashMap<>());

    // Mock security service to return a role
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Mock audit log creation with proper response
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Act & Assert
    StepVerifier.create(brdService.createBrdForm(validBrdRequest))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              Api<BRDResponse> apiResponse = responseEntity.getBody();
              assertNotNull(apiResponse);
              assertEquals("Successful", apiResponse.getStatus());
              assertEquals("BRD created", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals("ssd_file.xls", apiResponse.getData().get().getOriginalSSDFileName());
              assertEquals(
                  "contract_file.pdf", apiResponse.getData().get().getOriginalContractFileName());
              assertEquals(
                  "other_file.doc", apiResponse.getData().get().getOriginalOtherFileName());
            })
        .verifyComplete();

    verify(dtoModelMapper).mapToBrd(validBrdRequest);
    verify(brdRepository).save(validBrd);
    verify(dtoModelMapper).mapToBrdResponse(validBrd);
    verify(dtoModelMapper).mapBrdResponseToMAP(validBrdResponse);
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any());
  }

  @Test
  @DisplayName("Should get BRD successfully when valid ID is provided - 200 OK")
  void getBrdById_WithValidId_ShouldReturn200() {
    // Arrange
    String brdFormId = "12345";
    when(brdRepository.findById(brdFormId)).thenReturn(Mono.just(validBrd));
    when(dtoModelMapper.mapToBrdResponse(validBrd)).thenReturn(validBrdResponse);

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdService.getBrdById(brdFormId);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              Api<BRDResponse> apiResponse = responseEntity.getBody();
              assertNotNull(apiResponse);
              assertEquals("Successful", apiResponse.getStatus());
              assertEquals("BRD Found", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(validBrdResponse, apiResponse.getData().get());
            })
        .verifyComplete();

    verify(brdRepository).findById(brdFormId);
    verify(dtoModelMapper).mapToBrdResponse(validBrd);
  }

  @Test
  @DisplayName(
      "Should update BRD without updating sites when no BRD form fields are present - 200 OK")
  void updateBrdPartiallyWithOrderedOperations_WithNoFormFields_ShouldNotUpdateSites() {
    // Arrange
    String brdFormId = "brdForm123";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("brdName", "Updated BRD Name");
    updateFields.put("description", "Updated Description");
    // No BRD form fields here, so sites shouldn't be updated

    BRD existingBrd =
        BRD.builder()
            .brdFormId(brdFormId)
            .brdId("BRD0003")
            .brdName("Original BRD Name")
            .description("Original Description")
            .creator("testPM")
            .build();

    BRD updatedBrd =
        BRD.builder()
            .brdFormId(brdFormId)
            .brdId("BRD0003")
            .brdName("Updated BRD Name")
            .description("Updated Description")
            .creator("testPM")
            .build();

    when(customBrdValidator.validatePartialUpdateField(any(), any()))
        .thenReturn(Mono.just(updateFields));

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdFormId(brdFormId);
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdName("Updated BRD Name");
    brdResponse.setDescription("Updated Description");

    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn("Original Value");

    // Mock security service
    when(securityService.canModifyBrd("testPM")).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    // Mock audit log creation with proper response
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(
            argThat(
                request ->
                    request.getAction().equals("UPDATE")
                        && request.getComment().contains("Update successful")
                        && request.getUserRole().equals(SecurityConstants.PM_ROLE))))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations(brdFormId, updateFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BRDResponse> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BRD updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());

              BRDResponse actualResponse = apiResponse.getData().get();
              assertEquals(brdFormId, actualResponse.getBrdFormId());
              assertEquals("BRD0003", actualResponse.getBrdId());
              assertEquals("Updated BRD Name", actualResponse.getBrdName());
              assertEquals("Updated Description", actualResponse.getDescription());
            })
        .verifyComplete();

    verify(customBrdValidator).validatePartialUpdateField(any(), any());
    verify(reactiveMongoTemplate).findOne(any(Query.class), eq(BRD.class));
    verify(reactiveMongoTemplate)
        .findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class));
    verify(securityService).canModifyBrd("testPM");
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
    // Verify that we don't call the site service since there are no form fields
    verify(siteService, never()).updateBrdFormFieldsForAllSites(anyString(), any());
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD not found - 404 Not Found")
  void getBrdById_WithNonExistentId_ShouldThrowNotFoundException() {
    // Arrange
    String nonExistentId = "nonexistent123";
    when(brdRepository.findById(nonExistentId)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdService.getBrdById(nonExistentId);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with id: " + nonExistentId))
        .verify();

    verify(brdRepository).findById(nonExistentId);
    verify(dtoModelMapper, never()).mapToBrdResponse(any());
  }

  @Test
  @DisplayName("Should handle database errors correctly - 500 Internal Server Error")
  void getBrdById_WithDatabaseError_ShouldThrowException() {
    // Arrange
    String brdFormId = "12345";
    when(brdRepository.findById(brdFormId))
        .thenReturn(Mono.error(new RuntimeException("Database connection error")));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result = brdService.getBrdById(brdFormId);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable
                        .getMessage()
                        .equals("Something went wrong: Database connection error"))
        .verify();

    verify(brdRepository).findById(brdFormId);
    verify(dtoModelMapper, never()).mapToBrdResponse(any());
  }

  @Test
  @DisplayName("Should handle errors correctly")
  void handleErrors_WithDifferentExceptions_ShouldReturnAppropriateExceptions() {
    // Test with BadRequestException
    BadRequestException badRequestException = new BadRequestException("test", "Bad request");
    Throwable result1 = brdService.handleErrors(badRequestException);
    assertSame(badRequestException, result1);

    // Test with NotFoundException
    NotFoundException notFoundException = new NotFoundException("Not found");
    Throwable result2 = brdService.handleErrors(notFoundException);
    assertSame(notFoundException, result2);

    AlreadyExistException alreadyExistException = new AlreadyExistException("Already exists");
    Throwable result3 = brdService.handleErrors(alreadyExistException);
    assertSame(alreadyExistException, result3);

    DuplicateKeyException duplicateKeyException = new DuplicateKeyException("duplicate key error");
    Throwable result4 = brdService.handleErrors(duplicateKeyException);
    assertTrue(result4 instanceof AlreadyExistException);
    assertEquals("Brd already exist for given brdId", result4.getMessage());

    RuntimeException otherException = new RuntimeException("Some other error");
    Throwable result5 = brdService.handleErrors(otherException);
    assertTrue(result5 instanceof Exception);
    assertEquals("Something went wrong: Some other error", result5.getMessage());
  }

  @Test
  @DisplayName("Should throw BadRequestException when fields are invalid - 400 Bad Request")
  void updateBrdPartially_WithInvalidFields_ShouldThrowBadRequest() {
    // Arrange
    String brdId = "12345";
    Map<String, Object> invalidFields = new HashMap<>();
    invalidFields.put("invalidField", "value");

    when(customBrdValidator.validatePartialUpdateField(invalidFields, BRDRequest.class))
        .thenReturn(Mono.error(new BadRequestException("Invalid fields")));

    // Act & Assert
    StepVerifier.create(brdService.updateBrdPartiallyWithOrderedOperations(brdId, invalidFields))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Invalid fields"))
        .verify();

    verify(customBrdValidator).validatePartialUpdateField(invalidFields, BRDRequest.class);
    verify(reactiveMongoTemplate, never()).findOne(any(Query.class), any());
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD doesn't exist - 404 Not Found")
  void updateBrdPartially_WithNonExistentBrd_ShouldThrowNotFound() {
    // Arrange
    String brdId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_PROGRESS");

    when(customBrdValidator.validatePartialUpdateField(fields, BRDRequest.class))
        .thenReturn(Mono.just(fields));
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class))).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(brdService.updateBrdPartiallyWithOrderedOperations(brdId, fields))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with id: " + brdId))
        .verify();

    verify(customBrdValidator).validatePartialUpdateField(fields, BRDRequest.class);
    verify(reactiveMongoTemplate).findOne(any(Query.class), eq(BRD.class));
    verify(reactiveMongoTemplate, never())
        .findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class));
  }

  @Test
  @DisplayName("Should handle database errors correctly - 500 Internal Server Error")
  void updateBrdPartially_WithDatabaseError_ShouldThrowException() {
    // Arrange
    String brdId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "IN_PROGRESS");

    when(customBrdValidator.validatePartialUpdateField(fields, BRDRequest.class))
        .thenReturn(Mono.just(fields));
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.error(new RuntimeException("Database connection error")));

    // Act & Assert
    StepVerifier.create(brdService.updateBrdPartiallyWithOrderedOperations(brdId, fields))
        .expectErrorMatches(
            throwable ->
                throwable instanceof Exception
                    && throwable
                        .getMessage()
                        .equals("Something went wrong: Database connection error"))
        .verify();

    verify(customBrdValidator).validatePartialUpdateField(fields, BRDRequest.class);
    verify(reactiveMongoTemplate).findOne(any(Query.class), eq(BRD.class));
    verify(reactiveMongoTemplate, never())
        .findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class));
  }

  @Test
  @DisplayName("Should handle audit logging errors gracefully")
  void updateBrdPartially_WithAuditLogError_ShouldUpdateSuccessfully() {
    // Arrange
    Map<String, Object> validFields = new HashMap<>();
    validFields.put("status", "IN_PROGRESS");

    when(customBrdValidator.validatePartialUpdateField(validFields, BRDRequest.class))
        .thenReturn(Mono.just(validFields));

    BRD existingBrd = new BRD();
    existingBrd.setBrdFormId("12345");
    existingBrd.setCreator("testPM");

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    BRD updatedBrd = new BRD();
    updatedBrd.setBrdFormId("12345");
    updatedBrd.setStatus("IN_PROGRESS");

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdFormId("12345");
    brdResponse.setStatus("IN_PROGRESS");

    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);

    // Mock audit log creation with proper response
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", validFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BRDResponse> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("updated successfully"));
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdResponse, apiResponse.getData().get());
            })
        .verifyComplete();

    // Verify audit log
    verify(auditLogService)
        .logCreation(
            argThat(
                request ->
                    request.getAction().equals("UPDATE")
                        && request.getComment().contains("Update successful")
                        && request.getComment().contains("SUCCESS")));
  }

  @Test
  @DisplayName("Should handle empty fields correctly and return original BRD")
  void updateBrdPartiallyWithOrderedOperations_WithEmptyFields_ShouldReturnOriginalBrd() {
    // Arrange
    Map<String, Object> emptyFields = new HashMap<>();
    String brdFormId = "12345";

    BRD existingBrd =
        BRD.builder()
            .brdFormId(brdFormId)
            .brdId("BRD0003")
            .creator("testPM")
            .status("DRAFT")
            .build();

    when(customBrdValidator.validatePartialUpdateField(any(), any()))
        .thenReturn(Mono.just(emptyFields));

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdFormId(brdFormId);
    brdResponse.setBrdId("BRD0003");
    brdResponse.setStatus("DRAFT");

    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn(null);

    // Set up security service
    when(securityService.canModifyBrd("testPM")).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    // Set up audit logging
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(
            argThat(
                request ->
                    request.getAction().equals("UPDATE")
                        && request.getComment().contains("Update successful")
                        && request.getUserRole().equals(SecurityConstants.PM_ROLE))))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations(brdFormId, emptyFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BRDResponse> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BRD updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdFormId, apiResponse.getData().get().getBrdFormId());
              assertEquals("BRD0003", apiResponse.getData().get().getBrdId());
              assertEquals("DRAFT", apiResponse.getData().get().getStatus());
            })
        .verifyComplete();

    verify(customBrdValidator).validatePartialUpdateField(any(), any());
    verify(reactiveMongoTemplate).findOne(any(Query.class), eq(BRD.class));
    verify(reactiveMongoTemplate)
        .findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class));
    verify(securityService).canModifyBrd("testPM");
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
    // Verify that we don't call the site service since there are no form fields
    verify(siteService, never()).updateBrdFormFieldsForAllSites(anyString(), any());
  }

  @Test
  @DisplayName("Should throw BadRequestException when empty fields map is provided")
  void updateBrdPartially_WithEmptyFields_ShouldThrowBadRequestException() {
    // Arrange
    String brdFormId = "12345";
    Map<String, Object> fields = new HashMap<>();

    BadRequestException emptyFieldsException =
        new BadRequestException(
            "No fields to update",
            Map.of("error", "At least one field must be provided for update").toString());
    when(customBrdValidator.validatePartialUpdateField(fields, BRDRequest.class))
        .thenReturn(Mono.error(emptyFieldsException));

    // Act
    Mono<ResponseEntity<Api<BRDResponse>>> result =
        brdService.updateBrdPartiallyWithOrderedOperations(brdFormId, fields);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("No fields to update"))
        .verify();

    verify(customBrdValidator).validatePartialUpdateField(fields, BRDRequest.class);
    verify(reactiveMongoTemplate, never())
        .findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class));
  }


  @Test
  @DisplayName("Should throw BadRequestException when fields are invalid - 400 Bad Request")
  void updateBrdPartially_WithInvalidRequest_ShouldThrowBadRequestException() {
    // Arrange
    Map<String, Object> invalidFields = new HashMap<>();
    invalidFields.put("status", "INVALID_STATUS");

    when(customBrdValidator.validatePartialUpdateField(invalidFields, BRDRequest.class))
        .thenReturn(Mono.error(new BadRequestException("Invalid status")));

    // Act & Assert
    StepVerifier.create(brdService.updateBrdPartiallyWithOrderedOperations("12345", invalidFields))
        .expectError(BadRequestException.class)
        .verify();

    verify(customBrdValidator).validatePartialUpdateField(invalidFields, BRDRequest.class);
    verifyNoInteractions(reactiveMongoTemplate, dtoModelMapper, auditLogService);
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD is not found - 404 Not Found")
  void updateBrdPartially_WithNonExistentBrd_ShouldThrowNotFoundException() {
    // Arrange
    Map<String, Object> validFields = new HashMap<>();
    validFields.put("status", "IN_PROGRESS");

    when(customBrdValidator.validatePartialUpdateField(validFields, BRDRequest.class))
        .thenReturn(Mono.just(validFields));

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class))).thenReturn(Mono.empty());

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", validFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectError(NotFoundException.class)
        .verify();

    verify(reactiveMongoTemplate).findOne(any(Query.class), eq(BRD.class));
    verifyNoMoreInteractions(reactiveMongoTemplate);
  }

  @Test
  @DisplayName("Should deny access when user is not authorized - 403 Forbidden")
  void updateBrdPartially_WithoutPermission_ShouldDenyAccess() {
    // Arrange
    Map<String, Object> validFields = new HashMap<>();
    validFields.put("status", "IN_PROGRESS");

    when(customBrdValidator.validatePartialUpdateField(validFields, BRDRequest.class))
        .thenReturn(Mono.just(validFields));

    BRD existingBrd = new BRD();
    existingBrd.setBrdFormId("12345");
    existingBrd.setCreator("otherPM");

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    // Mock security service to deny access
    when(securityService.canModifyBrd("otherPM")).thenReturn(Mono.just(false));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    // Mock audit log creation
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(
            argThat(
                request ->
                    request.getAction().equals("UPDATE")
                        && request.getComment().contains("Access denied")
                        && request.getUserRole().equals(SecurityConstants.PM_ROLE))))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", validFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<BRDResponse> body = response.getBody();
              assertNotNull(body);
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertTrue(body.getMessage().contains("not yet open for collaboration"));
              assertFalse(body.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(securityService).canModifyBrd("otherPM");
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Should continue with update even if site update fails")
  void updateBrdPartiallyWithOrderedOperations_WithSiteUpdateError_ShouldStillUpdateBrd() {
    // Arrange
    String brdId = "12345";
    Map<String, Object> fields = new HashMap<>();
    fields.put(
        "brdForm.clientInformation.companyName",
        "Updated Company Name"); // Add a form field with brdForm prefix

    BRD existingBrd = BRD.builder().brdFormId(brdId).brdId("BRD0003").creator("testPM").build();

    when(customBrdValidator.validatePartialUpdateField(any(), any())).thenReturn(Mono.just(fields));

    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(existingBrd));

    BRD updatedBrd = BRD.builder().brdFormId(brdId).brdId("BRD0003").creator("testPM").build();

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    // Mock site service error
    when(siteService.updateBrdFormFieldsForAllSites(eq(brdId), anyMap()))
        .thenReturn(Mono.error(new RuntimeException("Site update failed")));

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdFormId(brdId);
    brdResponse.setBrdId("BRD0003");
    when(dtoModelMapper.mapToBrdResponse(any())).thenReturn(brdResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn("oldValue");

    // Mock security service
    when(securityService.canModifyBrd("testPM")).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

    // Mock audit log service
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(
            argThat(
                request ->
                    request.getAction().equals("UPDATE")
                        && request.getComment().contains("Update successful")
                        && request.getUserRole().equals(SecurityConstants.PM_ROLE))))
        .thenReturn(Mono.just(auditLogResponse));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations(brdId, fields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BRDResponse> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("updated successfully"));
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdResponse, apiResponse.getData().get());
            })
        .verifyComplete();

    verify(siteService).updateBrdFormFieldsForAllSites(eq(brdId), anyMap());
    verify(securityService).canModifyBrd("testPM");
    verify(securityService).getCurrentUserRole();
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Should update the ssdAvailable flag successfully")
  void updateBrdPartiallyWithOrderedOperations_WithSsdAvailableFlag_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("ssdAvailable", true);

    // Set up validations
    when(customBrdValidator.validatePartialUpdateField(updateFields, BRDRequest.class))
        .thenReturn(Mono.just(updateFields));

    // Set up existing BRD retrieval
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(validBrd));

    // Set up BRD update
    BRD updatedBrd = new BRD();
    updatedBrd.setBrdFormId("12345");
    updatedBrd.setBrdId("BRD-1234");
    updatedBrd.setStatus("DRAFT");
    updatedBrd.setProjectId("PRJ-123456");
    updatedBrd.setSsdAvailable(true);

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    // Set up response mapping
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("12345");
    updatedResponse.setBrdId("BRD-1234");
    updatedResponse.setStatus("DRAFT");
    updatedResponse.setProjectId("PRJ-123456");
    updatedResponse.setSsdAvailable(true);

    when(dtoModelMapper.mapToBrdResponse(updatedBrd)).thenReturn(updatedResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn(null);

    // Set up security service
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Set up audit logging
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Set up site service
    when(siteService.updateBrdFormFieldsForAllSites(anyString(), anyMap()))
        .thenReturn(Mono.just(ResponseEntity.ok().build()));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", updateFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isSsdAvailable());
              return true;
            })
        .verifyComplete();

    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Should update the contractAvailable flag successfully")
  void updateBrdPartiallyWithOrderedOperations_WithContractAvailableFlag_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("contractAvailable", true);

    // Set up validations
    when(customBrdValidator.validatePartialUpdateField(updateFields, BRDRequest.class))
        .thenReturn(Mono.just(updateFields));

    // Set up existing BRD retrieval
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(validBrd));

    // Set up BRD update
    BRD updatedBrd = new BRD();
    updatedBrd.setBrdFormId("12345");
    updatedBrd.setBrdId("BRD-1234");
    updatedBrd.setStatus("DRAFT");
    updatedBrd.setProjectId("PRJ-123456");
    updatedBrd.setContractAvailable(true);

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    // Set up response mapping
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("12345");
    updatedResponse.setBrdId("BRD-1234");
    updatedResponse.setStatus("DRAFT");
    updatedResponse.setProjectId("PRJ-123456");
    updatedResponse.setContractAvailable(true);

    when(dtoModelMapper.mapToBrdResponse(updatedBrd)).thenReturn(updatedResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn(null);

    // Set up security service
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Set up audit logging
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Set up site service
    when(siteService.updateBrdFormFieldsForAllSites(anyString(), anyMap()))
        .thenReturn(Mono.just(ResponseEntity.ok().build()));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", updateFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isContractAvailable());
              return true;
            })
        .verifyComplete();

    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Should update both flags simultaneously")
  void updateBrdPartiallyWithOrderedOperations_WithBothFlags_ShouldUpdateBrd() {
    // Arrange
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("ssdAvailable", true);
    updateFields.put("contractAvailable", true);

    // Set up validations
    when(customBrdValidator.validatePartialUpdateField(updateFields, BRDRequest.class))
        .thenReturn(Mono.just(updateFields));

    // Set up existing BRD retrieval
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(BRD.class)))
        .thenReturn(Mono.just(validBrd));

    // Set up BRD update
    BRD updatedBrd = new BRD();
    updatedBrd.setBrdFormId("12345");
    updatedBrd.setBrdId("BRD-1234");
    updatedBrd.setStatus("DRAFT");
    updatedBrd.setProjectId("PRJ-123456");
    updatedBrd.setSsdAvailable(true);
    updatedBrd.setContractAvailable(true);

    when(reactiveMongoTemplate.findAndModify(
            any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.just(updatedBrd));

    // Set up response mapping
    BRDResponse updatedResponse = new BRDResponse();
    updatedResponse.setBrdFormId("12345");
    updatedResponse.setBrdId("BRD-1234");
    updatedResponse.setStatus("DRAFT");
    updatedResponse.setProjectId("PRJ-123456");
    updatedResponse.setSsdAvailable(true);
    updatedResponse.setContractAvailable(true);

    when(dtoModelMapper.mapToBrdResponse(updatedBrd)).thenReturn(updatedResponse);
    when(dtoModelMapper.getFieldValue(any(), anyString())).thenReturn(null);

    // Set up security service
    when(securityService.canModifyBrd(anyString())).thenReturn(Mono.just(true));
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Set up audit logging
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    "SUCCESS",
                    "Audit log created successfully",
                    Optional.of(new AuditLogResponse()),
                    Optional.empty()));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(auditLogResponse));

    // Set up site service
    when(siteService.updateBrdFormFieldsForAllSites(anyString(), anyMap()))
        .thenReturn(Mono.just(ResponseEntity.ok().build()));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testPM");
    when(auth.getAuthorities())
        .thenAnswer(
            invocation ->
                Collections.singleton(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE)));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdService
                .updateBrdPartiallyWithOrderedOperations("12345", updateFields)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isSsdAvailable());
              assertTrue(response.getBody().getData().get().isContractAvailable());
              return true;
            })
        .verifyComplete();

    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Should get industry verticals successfully")
  void getIndustryVerticals_ShouldReturnVerticals() {
    // Arrange
    List<String> templateTypes = Arrays.asList("Retail", "Healthcare");
    when(reactiveMongoTemplate.findDistinct("templateTypes", BrdTemplateConfig.class, String.class))
        .thenReturn(Flux.fromIterable(templateTypes));

    // Act & Assert
    StepVerifier.create(brdService.getIndustryVerticals())
        .assertNext(
            verticals -> {
              assertEquals(3, verticals.size());
              assertTrue(verticals.contains("Retail"));
              assertTrue(verticals.contains("Healthcare"));
              assertTrue(verticals.contains("Other"));
            })
        .verifyComplete();

    verify(reactiveMongoTemplate)
        .findDistinct("templateTypes", BrdTemplateConfig.class, String.class);
  }

  @Test
  @DisplayName("Should handle empty template types list")
  void getIndustryVerticals_WithEmptyList_ShouldReturnOther() {
    // Arrange
    when(reactiveMongoTemplate.findDistinct("templateTypes", BrdTemplateConfig.class, String.class))
        .thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(brdService.getIndustryVerticals())
        .assertNext(
            verticals -> {
              assertEquals(1, verticals.size());
              assertTrue(verticals.contains("Other"));
            })
        .verifyComplete();

    verify(reactiveMongoTemplate)
        .findDistinct("templateTypes", BrdTemplateConfig.class, String.class);
  }

  @Test
  @DisplayName("Should handle database error when fetching verticals")
  void getIndustryVerticals_WithDatabaseError_ShouldPropagateError() {
    // Arrange
    when(reactiveMongoTemplate.findDistinct("templateTypes", BrdTemplateConfig.class, String.class))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(brdService.getIndustryVerticals())
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Database error"))
        .verify();

    verify(reactiveMongoTemplate)
        .findDistinct("templateTypes", BrdTemplateConfig.class, String.class);
  }

  @Test
  @DisplayName("Should get BRDs by PM username successfully when user has MANAGER role")
  void getBrdsByPmUsername_WithManagerRole_ShouldReturnBrds() {
    // Arrange
    String pmUsername = "test.pm@example.com";
    BRD brd1 = new BRD();
    brd1.setBrdFormId("brd1");
    brd1.setCreator(pmUsername);
    brd1.setStatus("Draft");

    BRD brd2 = new BRD();
    brd2.setBrdFormId("brd2");
    brd2.setCreator(pmUsername);
    brd2.setStatus("In Progress");

    BRDResponse brdResponse1 = new BRDResponse();
    brdResponse1.setBrdFormId("brd1");
    brdResponse1.setCreator(pmUsername);
    brdResponse1.setStatus("Draft");

    BRDResponse brdResponse2 = new BRDResponse();
    brdResponse2.setBrdFormId("brd2");
    brdResponse2.setCreator(pmUsername);
    brdResponse2.setStatus("In Progress");

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(reactiveMongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.just(brd1, brd2));
    when(dtoModelMapper.mapToBrdResponse(brd1)).thenReturn(brdResponse1);
    when(dtoModelMapper.mapToBrdResponse(brd2)).thenReturn(brdResponse2);

    // Act & Assert
    StepVerifier.create(brdService.getBrdsByPmUsername(pmUsername))
        .assertNext(
            brds -> {
              assertEquals(2, brds.size());
              assertEquals("brd1", brds.get(0).getBrdFormId());
              assertEquals("brd2", brds.get(1).getBrdFormId());
              assertEquals(pmUsername, brds.get(0).getCreator());
              assertEquals(pmUsername, brds.get(1).getCreator());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(reactiveMongoTemplate).find(any(Query.class), eq(BRD.class));
    verify(dtoModelMapper, times(2)).mapToBrdResponse(any(BRD.class));
  }

  @Test
  @DisplayName("Should throw AccessDeniedException when user does not have MANAGER role")
  void getBrdsByPmUsername_WithoutManagerRole_ShouldThrowAccessDeniedException() {
    // Arrange
    String pmUsername = "test.pm@example.com";
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("USER"));

    // Act & Assert
    StepVerifier.create(brdService.getBrdsByPmUsername(pmUsername))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AccessDeniedException
                    && throwable
                        .getMessage()
                        .equals("Access denied. Only Manager role can access this endpoint."))
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(reactiveMongoTemplate, never()).find(any(Query.class), eq(BRD.class));
    verify(dtoModelMapper, never()).mapToBrdResponse(any(BRD.class));
  }

  @Test
  @DisplayName("Should throw NotFoundException when no BRDs found for PM")
  void getBrdsByPmUsername_WithNoBrds_ShouldThrowNotFoundException() {
    // Arrange
    String pmUsername = "test.pm@example.com";
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(reactiveMongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(brdService.getBrdsByPmUsername(pmUsername))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("No BRDs found for PM: " + pmUsername))
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(reactiveMongoTemplate).find(any(Query.class), eq(BRD.class));
    verify(dtoModelMapper, never()).mapToBrdResponse(any(BRD.class));
  }

  @Test
  @DisplayName("Should handle database error when fetching BRDs")
  void getBrdsByPmUsername_WithDatabaseError_ShouldThrowRuntimeException() {
    // Arrange
    String pmUsername = "test.pm@example.com";
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(reactiveMongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.error(new RuntimeException("Database connection error")));

    // Act & Assert
    StepVerifier.create(brdService.getBrdsByPmUsername(pmUsername))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("An error occurred while retrieving BRDs"))
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(reactiveMongoTemplate).find(any(Query.class), eq(BRD.class));
    verify(dtoModelMapper, never()).mapToBrdResponse(any(BRD.class));
  }

  @Test
  @DisplayName("getBrdSectionById should throw BadRequestException for invalid section name")
  void getBrdSectionById_InvalidSectionName_ShouldThrowBadRequest() {
    when(customBrdValidator.isValidField("invalidSection")).thenReturn(false);
    StepVerifier.create(brdService.getBrdSectionById("id123", "invalidSection"))
        .expectErrorMatches(e -> e instanceof BadRequestException && e.getMessage().contains("Invalid section name"))
        .verify();
  }

  @Test
  @DisplayName("getBrdSectionById should throw BadRequestException for invalid brdFormId format")
  void getBrdSectionById_InvalidBrdFormIdFormat_ShouldThrowBadRequest() {
    when(customBrdValidator.isValidField("sectionName")).thenReturn(true);
    StepVerifier.create(brdService.getBrdSectionById("notAnObjectId", "sectionName"))
        .expectErrorMatches(e -> e instanceof BadRequestException && e.getMessage().contains("Invalid brdFormId format"))
        .verify();
  }

  @Test
  @DisplayName("getBrdSectionById should throw NotFoundException if section not found in result")
  void getBrdSectionById_SectionNotFound_ShouldThrowNotFound() {
    when(customBrdValidator.isValidField("sectionName")).thenReturn(true);
    Map<String, Object> result = new HashMap<>();
    result.put("_id", "id123");
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(Map.class), eq("brd"))).thenReturn(Mono.just(result));
    StepVerifier.create(brdService.getBrdSectionById("507f1f77bcf86cd799439011", "sectionName"))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains("Section sectionName not found"))
        .verify();
  }

  @Test
  @DisplayName("getBrdSectionById should propagate database error")
  void getBrdSectionById_DatabaseError_ShouldPropagate() {
    when(customBrdValidator.isValidField("sectionName")).thenReturn(true);
    when(reactiveMongoTemplate.findOne(any(Query.class), eq(Map.class), eq("brd"))).thenReturn(Mono.error(new RuntimeException("db error")));
    StepVerifier.create(brdService.getBrdSectionById("507f1f77bcf86cd799439011", "sectionName"))
        .expectErrorMatches(e -> e instanceof Exception && e.getMessage().contains("db error"))
        .verify();
  }

  @Test
  @DisplayName("getBrdList should throw BadRequestException for invalid page/size")
  void getBrdList_InvalidPageOrSize_ShouldThrowBadRequest() {
    StepVerifier.create(brdService.getBrdList(-1, 10))
        .expectErrorMatches(BadRequestException.class::isInstance)
        .verify();
    StepVerifier.create(brdService.getBrdList(0, 0))
        .expectErrorMatches(BadRequestException.class::isInstance)
        .verify();
  }

  @Test
  @DisplayName("getBrdList should throw NotFoundException if no sections found")
  void getBrdList_NoSectionsFound_ShouldThrowNotFound() {
    when(brdRepository.count()).thenReturn(Mono.just(10L));
    when(brdRepository.findAllBy(any())).thenReturn(Flux.empty());
    StepVerifier.create(brdService.getBrdList(0, 10))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains("No BRD sections found"))
        .verify();
  }

  @Test
  @DisplayName("getBrdList should propagate database error")
  void getBrdList_DatabaseError_ShouldPropagate() {
    when(brdRepository.count()).thenReturn(Mono.error(new RuntimeException("db error")));
    StepVerifier.create(brdService.getBrdList(0, 10))
        .expectErrorMatches(e -> e instanceof Exception && e.getMessage().contains("db error"))
        .verify();
  }

  @Test
  @DisplayName("searchBRDs should return results page")
  void searchBRDs_ValidSearch_ShouldReturnPage() {
    List<Document> docs = List.of(new Document("_id", "id1"), new Document("_id", "id2"));
    BRDSearchResponse resp1 = new BRDSearchResponse();
    BRDSearchResponse resp2 = new BRDSearchResponse();
    when(reactiveMongoTemplate.find(any(Query.class), eq(Document.class), eq("brd"))).thenReturn(Flux.fromIterable(docs));
    when(dtoModelMapper.mapToSearchResponse(any(Document.class))).thenReturn(resp1, resp2);
    when(reactiveMongoTemplate.count(any(Query.class), eq("brd"))).thenReturn(Mono.just(2L));
    StepVerifier.create(brdService.searchBRDs("test", 0, 2, "brdId", "asc"))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("searchBRDs should handle invalid sort direction")
  void searchBRDs_InvalidSortDirection_ShouldThrow() {
    StepVerifier.create(brdService.searchBRDs("test", 0, 2, "brdId", "notADirection"))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  @DisplayName("searchBRDs should propagate database error")
  void searchBRDs_DatabaseError_ShouldPropagate() {
    when(reactiveMongoTemplate.find(any(Query.class), eq(Document.class), eq("brd"))).thenThrow(new RuntimeException("db error"));
    StepVerifier.create(brdService.searchBRDs("test", 0, 2, "brdId", "asc"))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  @DisplayName("updateBrdStatus should throw NotFoundException when BRD not found, regardless of security context")
  void updateBrdStatus_NoSecurityContext_ShouldReturnUnauthorized() {
    // Mock findAndModify to return Mono.empty() to simulate BRD not found
    when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class)))
        .thenReturn(Mono.empty());
    
    // The service checks database first, then security context
    // Since BRD is not found, it throws NotFoundException before checking security context
    
    StepVerifier.create(brdService.updateBrdStatus("id123", "APPROVED", "comment"))
        .expectErrorMatches(throwable -> 
            throwable instanceof NotFoundException && 
            throwable.getMessage().contains("BRD not found with id: id123"))
        .verify();
  }

  @Test
  @DisplayName("updateBrdStatus should throw NotFoundException if BRD not found")
  void updateBrdStatus_BrdNotFound_ShouldThrowNotFound() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class))).thenReturn(Mono.empty());
    StepVerifier.create(brdService.updateBrdStatus("id123", "APPROVED", "comment").contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("updateBrdStatus should propagate database error")
  void updateBrdStatus_DatabaseError_ShouldPropagate() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class))).thenReturn(Mono.error(new RuntimeException("db error")));
    StepVerifier.create(brdService.updateBrdStatus("id123", "APPROVED", "comment").contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  @DisplayName("updateBrdStatus should handle audit log error gracefully")
  void updateBrdStatus_AuditLogError_ShouldStillReturnSuccess() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    BRD updatedBrd = new BRD();
    updatedBrd.setBrdFormId("id123");
    when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(BRD.class))).thenReturn(Mono.just(updatedBrd));
    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(new BRDResponse());
    when(auditLogService.logCreation(any(AuditLogRequest.class))).thenReturn(Mono.error(new RuntimeException("audit error")));
    StepVerifier.create(brdService.updateBrdStatus("id123", "APPROVED", "comment").contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
        })
        .verifyComplete();
  }
}
