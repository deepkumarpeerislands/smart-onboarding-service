package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBillerRequest;
import com.aci.smart_onboarding.dto.AssignBillerResponse;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.UpdateBillerEmailRequest;
import com.aci.smart_onboarding.exception.BillerAssignmentException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BillerAssignment;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBillerAssignmentService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillerAssignmentControllerTest {

  @Mock private IBillerAssignmentService billerAssignmentService;

  @Mock private BRDRepository brdRepository;

  @Mock private IBRDService brdService;

  @Mock private BRDSecurityService brdSecurityService;

  @Mock private DtoModelMapper dtoModelMapper;

  @InjectMocks private BillerAssignmentController billerAssignmentController;

  private AssignBillerRequest validRequest;
  private AssignBillerResponse successResponse;
  private BRD brd;
  private BRDResponse brdResponse;

  @BeforeEach
  void setUp() {
    // Setup valid request
    validRequest = new AssignBillerRequest();
    validRequest.setBillerEmail("biller@example.com");
    validRequest.setStatus("IN_PROGRESS");
    validRequest.setDescription("Test assignment");

    // Setup success response
    successResponse = new AssignBillerResponse();
    successResponse.setBrdId("BRD123");
    successResponse.setBillerEmail("biller@example.com");
    successResponse.setStatus("IN_PROGRESS");
    successResponse.setDescription("Biller assigned successfully");

    // Setup BRD
    brd = new BRD();
    brd.setBrdId("BRD123");
    brd.setBrdFormId("FORM123");

    // Setup BRDResponse
    brdResponse = new BRDResponse();
    brdResponse.setStatus("DRAFT");

    // Setup mapper mock
    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
  }

  @Test
  void assignBiller_Success() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.assignBiller(anyString(), any(AssignBillerRequest.class)))
        .thenReturn(Mono.just(successResponse));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Biller assigned successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals("BRD123", api.getData().get().getBrdId());
              assertEquals("biller@example.com", api.getData().get().getBillerEmail());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_BRDNotFound() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.empty());

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD not found"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_AccessDenied() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
    when(brdSecurityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.error(new AccessDeniedException("Access denied")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(
                  api.getMessage().contains(ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_BillerAssignmentError() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.assignBiller(anyString(), any(AssignBillerRequest.class)))
        .thenReturn(Mono.error(new BillerAssignmentException("Invalid biller assignment")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("Invalid biller assignment", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_UnexpectedError() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.assignBiller(anyString(), any(AssignBillerRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("An unexpected error occurred: Unexpected error", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_InvalidRequest() {
    // Given
    AssignBillerRequest invalidRequest = new AssignBillerRequest();
    // Missing required fields billerEmail and status
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.assignBiller(anyString(), any(AssignBillerRequest.class)))
        .thenReturn(Mono.error(new BillerAssignmentException("Invalid request")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", invalidRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("Invalid request", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_EmptyBRDId() {
    // Given
    String emptyBrdId = "";
    when(brdRepository.findByBrdId(emptyBrdId))
        .thenReturn(Mono.error(new BillerAssignmentException("BRD ID cannot be empty")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller(emptyBrdId, validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD ID cannot be empty"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBiller_BRDStatusNotFound() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.assignBiller(anyString(), any(AssignBillerRequest.class)))
        .thenReturn(Mono.error(new BillerAssignmentException("BRD status not found")));

    // When
    Mono<ResponseEntity<Api<AssignBillerResponse>>> result =
        billerAssignmentController.assignBiller("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBillerResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD status not found"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerDetails_Success() {
    // Given
    Map<String, String> billerDetails =
        Map.of(
            "brdId", "BRD123",
            "billerEmail", "biller@example.com");

    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.getBillerDetails(anyString()))
        .thenReturn(Mono.just(billerDetails));

    // When
    Mono<ResponseEntity<Api<Map<String, String>>>> result =
        billerAssignmentController.getBillerDetails("BRD123");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<Map<String, String>> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.SUCCESSFUL, api.getStatus());
              assertEquals("Biller details retrieved successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals("BRD123", api.getData().get().get("brdId"));
              assertEquals("biller@example.com", api.getData().get().get("billerEmail"));
              return true;
            })
        .verifyComplete();

    verify(billerAssignmentService).getBillerDetails("BRD123");
  }

  @Test
  void getBillerDetails_NotPmRole() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // When
    Mono<ResponseEntity<Api<Map<String, String>>>> result =
        billerAssignmentController.getBillerDetails("BRD123");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<Map<String, String>> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals(
                  "Only Project Managers (PM) can access biller details", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void getBillerDetails_BRDNotFound() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.empty());

    // When
    Mono<ResponseEntity<Api<Map<String, String>>>> result =
        billerAssignmentController.getBillerDetails("BRD123");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<Map<String, String>> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD not found"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerDetails_SecurityCheckFailed() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(dtoModelMapper.mapToBrdResponse(any(BRD.class))).thenReturn(brdResponse);
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdSecurityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.error(new AccessDeniedException("Security check failed")));

    // When
    Mono<ResponseEntity<Api<Map<String, String>>>> result =
        billerAssignmentController.getBillerDetails("BRD123");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, String>> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(
                  api.getMessage().contains(ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerDetails_BillerNotAssigned() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(billerAssignmentService.getBillerDetails(anyString()))
        .thenReturn(Mono.error(new NotFoundException("Biller not assigned to BRD")));

    // When
    Mono<ResponseEntity<Api<Map<String, String>>>> result =
        billerAssignmentController.getBillerDetails("BRD123");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<Map<String, String>> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("Biller not assigned to BRD", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerEmails_WithPMRole_ShouldReturnEmails() {
    // Arrange
    List<String> expectedEmails = Arrays.asList("biller1@example.com", "biller2@example.com");

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(billerAssignmentService.getAllBillerEmails()).thenReturn(Mono.just(expectedEmails));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.getBillerEmails())
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("Biller emails retrieved successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(expectedEmails, apiResponse.getData().get());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(billerAssignmentService).getAllBillerEmails();
  }

  @Test
  void getBillerEmails_WithoutPMRole_ShouldReturnForbidden() {
    // Arrange
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.getBillerEmails())
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals(
                  "Only Project Managers (PM) can access biller emails", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void getBillerEmails_WithError_ShouldReturnInternalServerError() {
    // Arrange
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(billerAssignmentService.getAllBillerEmails())
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.getBillerEmails())
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("An unexpected error occurred"));
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(billerAssignmentService).getAllBillerEmails();
  }

  @Test
  @DisplayName("Should update Biller email when record exists")
  void updateBillerEmail_WhenRecordExists_ShouldUpdate() {
    // Arrange
    String brdId = "BRD123";
    UpdateBillerEmailRequest request = new UpdateBillerEmailRequest("biller@example.com");

    BillerAssignment existingAssignment = new BillerAssignment();
    existingAssignment.setBrdId(brdId);
    existingAssignment.setBillerEmail("old@example.com");
    existingAssignment.setAssignedAt(LocalDateTime.now().minusDays(1));

    BillerAssignment updatedAssignment = new BillerAssignment();
    updatedAssignment.setBrdId(brdId);
    updatedAssignment.setBillerEmail(request.getBillerEmail());
    updatedAssignment.setAssignedAt(existingAssignment.getAssignedAt());
    updatedAssignment.setUpdatedAt(LocalDateTime.now());

    when(billerAssignmentService.updateBillerEmail(brdId, request.getBillerEmail()))
        .thenReturn(Mono.just(updatedAssignment));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("Biller email updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdId, apiResponse.getData().get().getBrdId());
              assertEquals(request.getBillerEmail(), apiResponse.getData().get().getBillerEmail());
              assertEquals(
                  existingAssignment.getAssignedAt(), apiResponse.getData().get().getAssignedAt());
              assertNotNull(apiResponse.getData().get().getUpdatedAt());
              return true;
            })
        .verifyComplete();

    verify(billerAssignmentService).updateBillerEmail(brdId, request.getBillerEmail());
  }

  @Test
  @DisplayName("Should create new record when Biller assignment doesn't exist")
  void updateBillerEmail_WhenRecordDoesNotExist_ShouldCreate() {
    // Arrange
    String brdId = "BRD123";
    UpdateBillerEmailRequest request = new UpdateBillerEmailRequest("biller@example.com");

    BillerAssignment newAssignment = new BillerAssignment();
    newAssignment.setBrdId(brdId);
    newAssignment.setBillerEmail(request.getBillerEmail());
    newAssignment.setAssignedAt(LocalDateTime.now());
    newAssignment.setUpdatedAt(LocalDateTime.now());

    when(billerAssignmentService.updateBillerEmail(brdId, request.getBillerEmail()))
        .thenReturn(Mono.just(newAssignment));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("Biller email updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdId, apiResponse.getData().get().getBrdId());
              assertEquals(request.getBillerEmail(), apiResponse.getData().get().getBillerEmail());
              assertNotNull(apiResponse.getData().get().getAssignedAt());
              assertNotNull(apiResponse.getData().get().getUpdatedAt());
              return true;
            })
        .verifyComplete();

    verify(billerAssignmentService).updateBillerEmail(brdId, request.getBillerEmail());
  }

  @Test
  void updateBillerEmail_WhenBrdIdEmpty_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "";
    UpdateBillerEmailRequest request = new UpdateBillerEmailRequest("biller@example.com");

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BRD ID cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void updateBillerEmail_WhenBillerEmailEmpty_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";
    UpdateBillerEmailRequest request = UpdateBillerEmailRequest.builder().billerEmail("").build();

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("Biller email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void updateBillerEmail_WhenBillerEmailNull_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";
    UpdateBillerEmailRequest request = UpdateBillerEmailRequest.builder().billerEmail(null).build();

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("Biller email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void updateBillerEmail_WhenRequestNull_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, null))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("Biller email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(billerAssignmentService);
  }

  @Test
  void updateBillerEmail_WhenError_ShouldReturnInternalServerError() {
    // Arrange
    String brdId = "BRD123";
    UpdateBillerEmailRequest request = new UpdateBillerEmailRequest("biller@example.com");

    when(billerAssignmentService.updateBillerEmail(brdId, request.getBillerEmail()))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(billerAssignmentController.updateBillerEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<BillerAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("An unexpected error occurred"));
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(billerAssignmentService).updateBillerEmail(brdId, request.getBillerEmail());
  }
}
