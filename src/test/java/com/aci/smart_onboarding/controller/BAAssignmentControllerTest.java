package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBARequest;
import com.aci.smart_onboarding.dto.AssignBAResponse;
import com.aci.smart_onboarding.dto.BAReassignmentRequest;
import com.aci.smart_onboarding.dto.BAAssignmentStatusResponse;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.UpdateBAEmailRequest;
import com.aci.smart_onboarding.exception.BAAssignmentException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.model.BAAssignment;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBAAssignmentService;
import com.aci.smart_onboarding.service.IBRDService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BAAssignmentControllerTest {

  @Mock private IBAAssignmentService baAssignmentService;
  @Mock private BRDRepository brdRepository;
  @Mock private IBRDService brdService;
  @Mock private BRDSecurityService brdSecurityService;
  @InjectMocks private BAAssignmentController baAssignmentController;

  private WebTestClient webTestClient;
  private AssignBARequest validRequest;
  private AssignBAResponse successResponse;
  private BRD brd;
  private BRDResponse brdResponse;

  @BeforeEach
  void setUp() {
    BAAssignmentController controller =
        new BAAssignmentController(
            baAssignmentService, brdRepository, brdService, brdSecurityService);
    webTestClient = WebTestClient.bindToController(controller).build();

    // Setup valid request
    validRequest = new AssignBARequest();
    validRequest.setBaEmail("john.doe@example.com");
    validRequest.setStatus("IN_PROGRESS");
    validRequest.setDescription("Test assignment");

    // Setup success response
    successResponse = new AssignBAResponse();
    successResponse.setBrdId("BRD123");
    successResponse.setBaEmail("john.doe@example.com");
    successResponse.setStatus("IN_PROGRESS");
    successResponse.setDescription("BA assigned successfully");

    // Setup BRD
    brd = new BRD();
    brd.setBrdId("BRD123");
    brd.setBrdFormId("FORM123");

    // Setup BRDResponse
    brdResponse = new BRDResponse();
    brdResponse.setStatus("DRAFT");
  }

  @Test
  void assignBA_Success() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Success",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(baAssignmentService.assignBA(anyString(), any(AssignBARequest.class)))
        .thenReturn(Mono.just(successResponse));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.SUCCESSFUL, api.getStatus());
              assertEquals("BA assigned successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertEquals("BRD123", api.getData().get().getBrdId());
              assertEquals("john.doe@example.com", api.getData().get().getBaEmail());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_BRDNotFound() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.empty());

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD not found"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_AccessDenied() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Success",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(brdSecurityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.defer(() -> Mono.error(new AccessDeniedException("Access denied"))));
    when(baAssignmentService.assignBA(anyString(), any(AssignBARequest.class)))
        .thenReturn(Mono.just(successResponse));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("Access denied", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_BAAssignmentError() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Success",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(baAssignmentService.assignBA(anyString(), any(AssignBARequest.class)))
        .thenReturn(Mono.error(new BAAssignmentException("Invalid BA assignment")));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("Invalid BA assignment", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_UnexpectedError() {
    // Given
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Success",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(brdSecurityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(baAssignmentService.assignBA(anyString(), any(AssignBARequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("Unexpected error"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_InvalidRequest() {
    // Given
    AssignBARequest invalidRequest = new AssignBARequest();
    // Missing required fields baEmail and status
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(Mono.error(new BAAssignmentException("Invalid request")));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA("BRD123", invalidRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("Invalid request"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void assignBA_EmptyBRDId() {
    // Given
    String emptyBrdId = "";
    when(brdRepository.findByBrdId(emptyBrdId))
        .thenReturn(Mono.error(new BAAssignmentException("BRD ID cannot be empty")));

    // When
    Mono<ResponseEntity<Api<AssignBAResponse>>> result =
        baAssignmentController.assignBA(emptyBrdId, validRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<AssignBAResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("BRD ID cannot be empty"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should get BA emails successfully when user has PM role")
  void getBaEmails_WithPMRole_ShouldReturnEmails() {
    // Arrange
    List<String> expectedEmails = Arrays.asList("ba1@example.com", "ba2@example.com");

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(baAssignmentService.getAllBaEmails()).thenReturn(Mono.just(expectedEmails));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getBaEmails())
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BA emails retrieved successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(expectedEmails, apiResponse.getData().get());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(baAssignmentService).getAllBaEmails();
  }

  @Test
  @DisplayName("Should return forbidden when user does not have PM role")
  void getBaEmails_WithoutPMRole_ShouldReturnForbidden() {
    // Arrange
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getBaEmails())
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<String>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals(
                  "Only Project Managers (PM) can access BA emails", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should handle error when fetching BA emails")
  void getBaEmails_WithError_ShouldReturnInternalServerError() {
    // Arrange
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(baAssignmentService.getAllBaEmails())
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getBaEmails())
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
    verify(baAssignmentService).getAllBaEmails();
  }

  @Test
  @DisplayName("Should update BA email when record exists")
  void updateBaEmail_WhenRecordExists_ShouldUpdate() {
    // Arrange
    String brdId = "BRD123";
    UpdateBAEmailRequest request = new UpdateBAEmailRequest("ba@example.com");

    BAAssignment existingAssignment = new BAAssignment();
    existingAssignment.setBrdId(brdId);
    existingAssignment.setBaEmail("old@example.com");
    existingAssignment.setAssignedAt(LocalDateTime.now().minusDays(1));

    BAAssignment updatedAssignment = new BAAssignment();
    updatedAssignment.setBrdId(brdId);
    updatedAssignment.setBaEmail(request.getBaEmail());
    updatedAssignment.setAssignedAt(existingAssignment.getAssignedAt());
    updatedAssignment.setUpdatedAt(LocalDateTime.now());

    when(baAssignmentService.updateBaEmail(brdId, request.getBaEmail()))
        .thenReturn(Mono.just(updatedAssignment));

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BA email updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdId, apiResponse.getData().get().getBrdId());
              assertEquals(request.getBaEmail(), apiResponse.getData().get().getBaEmail());
              assertEquals(
                  existingAssignment.getAssignedAt(), apiResponse.getData().get().getAssignedAt());
              assertNotNull(apiResponse.getData().get().getUpdatedAt());
              return true;
            })
        .verifyComplete();

    verify(baAssignmentService).updateBaEmail(brdId, request.getBaEmail());
  }

  @Test
  @DisplayName("Should create new record when BA assignment doesn't exist")
  void updateBaEmail_WhenRecordDoesNotExist_ShouldCreate() {
    // Arrange
    String brdId = "BRD123";
    UpdateBAEmailRequest request = new UpdateBAEmailRequest("ba@example.com");

    BAAssignment newAssignment = new BAAssignment();
    newAssignment.setBrdId(brdId);
    newAssignment.setBaEmail(request.getBaEmail());
    newAssignment.setAssignedAt(LocalDateTime.now());
    newAssignment.setUpdatedAt(LocalDateTime.now());

    when(baAssignmentService.updateBaEmail(brdId, request.getBaEmail()))
        .thenReturn(Mono.just(newAssignment));

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BA email updated successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(brdId, apiResponse.getData().get().getBrdId());
              assertEquals(request.getBaEmail(), apiResponse.getData().get().getBaEmail());
              assertNotNull(apiResponse.getData().get().getAssignedAt());
              assertNotNull(apiResponse.getData().get().getUpdatedAt());
              return true;
            })
        .verifyComplete();

    verify(baAssignmentService).updateBaEmail(brdId, request.getBaEmail());
  }

  @Test
  @DisplayName("Should return 400 when BRD ID is empty")
  void updateBaEmail_WhenBrdIdEmpty_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "";
    UpdateBAEmailRequest request = new UpdateBAEmailRequest("ba@example.com");

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BRD ID cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when BA email is empty")
  void updateBaEmail_WhenBaEmailEmpty_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";
    UpdateBAEmailRequest request = UpdateBAEmailRequest.builder().baEmail("").build();

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BA email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when BA email is null")
  void updateBaEmail_WhenBaEmailNull_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";
    UpdateBAEmailRequest request = UpdateBAEmailRequest.builder().baEmail(null).build();

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BA email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when request is null")
  void updateBaEmail_WhenRequestNull_ShouldReturnBadRequest() {
    // Arrange
    String brdId = "BRD123";

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, null))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BA email cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when BRD ID is null")
  void updateBaEmail_WhenBrdIdNull_ShouldReturnBadRequest() {
    // Arrange
    UpdateBAEmailRequest request = UpdateBAEmailRequest.builder().baEmail("ba@example.com").build();

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(null, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("BRD ID cannot be empty", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should handle error when updating BA email")
  void updateBaEmail_WhenError_ShouldReturnInternalServerError() {
    // Arrange
    String brdId = "BRD123";
    UpdateBAEmailRequest request = new UpdateBAEmailRequest("ba@example.com");

    when(baAssignmentService.updateBaEmail(brdId, request.getBaEmail()))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(baAssignmentController.updateBaEmail(brdId, request))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<BAAssignment> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("An unexpected error occurred"));
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(baAssignmentService).updateBaEmail(brdId, request.getBaEmail());
  }

  @Test
  @DisplayName("Should get BA assignments successfully when user has MANAGER role")
  void getAssignmentsByBaUsername_WithManagerRole_ShouldReturnAssignments() {
    // Arrange
    String username = "ba@example.com";
    List<BAAssignment> expectedAssignments =
        Arrays.asList(
            BAAssignment.builder()
                .brdId("BRD-1")
                .baEmail(username)
                .description("Test assignment 1")
                .assignedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            BAAssignment.builder()
                .brdId("BRD-2")
                .baEmail(username)
                .description("Test assignment 2")
                .assignedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_MANAGER"));
    when(baAssignmentService.getAssignmentsByBaUsername(username))
        .thenReturn(Mono.just(expectedAssignments));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getAssignmentsByBaUsername(username))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<BAAssignment>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BA assignments retrieved successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertEquals(expectedAssignments, apiResponse.getData().get());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(baAssignmentService).getAssignmentsByBaUsername(username);
  }

  @Test
  @DisplayName("Should return forbidden when user does not have MANAGER role")
  void getAssignmentsByBaUsername_WithoutManagerRole_ShouldReturnForbidden() {
    // Arrange
    String username = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getAssignmentsByBaUsername(username))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<List<BAAssignment>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("Only Managers can view BA assignments", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should handle invalid email format")
  void getAssignmentsByBaUsername_WithInvalidEmail_ShouldReturnBadRequest() {
    // Arrange
    String invalidEmail = "invalid-email";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_MANAGER"));
    when(baAssignmentService.getAssignmentsByBaUsername(invalidEmail))
        .thenReturn(Mono.error(new BadRequestException("Invalid email format")));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getAssignmentsByBaUsername(invalidEmail))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<List<BAAssignment>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertEquals("Invalid email format", apiResponse.getMessage());
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(baAssignmentService).getAssignmentsByBaUsername(invalidEmail);
  }

  @Test
  @DisplayName("Should handle empty assignments list")
  void getAssignmentsByBaUsername_WithNoAssignments_ShouldReturnEmptyList() {
    // Arrange
    String username = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_MANAGER"));
    when(baAssignmentService.getAssignmentsByBaUsername(username))
        .thenReturn(Mono.just(Collections.emptyList()));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getAssignmentsByBaUsername(username))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<List<BAAssignment>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.SUCCESSFUL, apiResponse.getStatus());
              assertEquals("BA assignments retrieved successfully", apiResponse.getMessage());
              assertTrue(apiResponse.getData().isPresent());
              assertTrue(apiResponse.getData().get().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(baAssignmentService).getAssignmentsByBaUsername(username);
  }

  @Test
  @DisplayName("Should handle unexpected errors")
  void getAssignmentsByBaUsername_WithUnexpectedError_ShouldReturnInternalServerError() {
    // Arrange
    String username = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_MANAGER"));
    when(baAssignmentService.getAssignmentsByBaUsername(username))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    // Act & Assert
    StepVerifier.create(baAssignmentController.getAssignmentsByBaUsername(username))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<List<BAAssignment>> apiResponse = response.getBody();
              assertNotNull(apiResponse);
              assertEquals(BrdConstants.FAILURE, apiResponse.getStatus());
              assertTrue(apiResponse.getMessage().contains("An unexpected error occurred"));
              assertFalse(apiResponse.getData().isPresent());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(baAssignmentService).getAssignmentsByBaUsername(username);
  }

  @Test
  void shouldReassignBASuccessfullyWhenUserHasMANAGERRole() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("ba1@example.com")
                .build());

    when(brdSecurityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(baAssignmentService.reassignBAs(any()))
        .thenReturn(
            Mono.just(
                new Api<>(
                    BrdConstants.SUCCESSFUL,
                    "All BAs reassigned successfully",
                    Optional.empty(),
                    Optional.empty())));

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/brds/reassign-ba")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requests)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(BrdConstants.SUCCESSFUL)
        .jsonPath("$.message")
        .isEqualTo("All BAs reassigned successfully");

    verify(baAssignmentService).reassignBAs(requests);
  }

  @Test
  void shouldReturn403WhenUserIsNotAMANAGER() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("ba1@example.com")
                .build());

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/brds/reassign-ba")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requests)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(BrdConstants.FAILURE)
        .jsonPath("$.message")
        .isEqualTo("This endpoint is only accessible to users with MANAGER role");

    verify(baAssignmentService, never()).reassignBAs(any());
  }

  @Test
  void shouldReturn404WhenBRDIsNotFound() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("ba1@example.com")
                .build());

    when(brdSecurityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(baAssignmentService.reassignBAs(any()))
        .thenReturn(
            Mono.just(
                new Api<>(
                    BrdConstants.FAILURE,
                    "Some reassignments failed",
                    Optional.empty(),
                    Optional.of(
                        Map.of("error1", "Failed to reassign BRD BRD-123: BRD not found")))));

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/brds/reassign-ba")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requests)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(BrdConstants.FAILURE)
        .jsonPath("$.message")
        .isEqualTo("Some reassignments failed")
        .jsonPath("$.errors.error1")
        .isEqualTo("Failed to reassign BRD BRD-123: BRD not found");

    verify(baAssignmentService).reassignBAs(requests);
  }

  @Test
  void shouldReturn400WhenUserIsNotABA() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("user1@example.com")
                .build());

    when(brdSecurityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
    when(baAssignmentService.reassignBAs(any()))
        .thenReturn(
            Mono.just(
                new Api<>(
                    BrdConstants.FAILURE,
                    "Some reassignments failed",
                    Optional.empty(),
                    Optional.of(
                        Map.of(
                            "error1",
                            "Failed to reassign BRD BRD-123: User user1@example.com is not a BA")))));

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/brds/reassign-ba")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requests)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(BrdConstants.FAILURE)
        .jsonPath("$.message")
        .isEqualTo("Some reassignments failed")
        .jsonPath("$.errors.error1")
        .isEqualTo("Failed to reassign BRD BRD-123: User user1@example.com is not a BA");

    verify(baAssignmentService).reassignBAs(requests);
  }

  @Test
  @DisplayName("Should return true when current BA is assigned to BRD")
  void isBAAssigned_WhenCurrentBAIsAssigned_ShouldReturnTrue() {
    // Given
    String brdId = "BRD123";
    String currentUser = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.BA_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just(currentUser));
    when(baAssignmentService.isBAAssignedToUser(brdId, currentUser)).thenReturn(Mono.just(true));

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.SUCCESSFUL, api.getStatus());
              assertEquals("BA assignment status retrieved successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertTrue(api.getData().get().getIsAssigned());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(brdSecurityService).getCurrentUserEmail();
    verify(baAssignmentService).isBAAssignedToUser(brdId, currentUser);
  }

  @Test
  @DisplayName("Should return false when current BA is not assigned to BRD")
  void isBAAssigned_WhenCurrentBANotAssigned_ShouldReturnFalse() {
    // Given
    String brdId = "BRD123";
    String currentUser = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.BA_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just(currentUser));
    when(baAssignmentService.isBAAssignedToUser(brdId, currentUser)).thenReturn(Mono.just(false));

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.SUCCESSFUL, api.getStatus());
              assertEquals("BA assignment status retrieved successfully", api.getMessage());
              assertTrue(api.getData().isPresent());
              assertFalse(api.getData().get().getIsAssigned());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(brdSecurityService).getCurrentUserEmail();
    verify(baAssignmentService).isBAAssignedToUser(brdId, currentUser);
  }

  @Test
  @DisplayName("Should return forbidden when user does not have BA role")
  void isBAAssigned_WhenUserNotBA_ShouldReturnForbidden() {
    // Given
    String brdId = "BRD123";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("This endpoint is only accessible to users with BA role", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verifyNoInteractions(baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when BRD ID is empty")
  void isBAAssigned_WhenBrdIdEmpty_ShouldReturnBadRequest() {
    // Given
    String brdId = "";

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("BRD ID cannot be empty", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(brdSecurityService, baAssignmentService);
  }

  @Test
  @DisplayName("Should return 400 when BRD ID is null")
  void isBAAssigned_WhenBrdIdNull_ShouldReturnBadRequest() {
    // Given
    String brdId = null;

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertEquals("BRD ID cannot be empty", api.getMessage());
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verifyNoInteractions(brdSecurityService, baAssignmentService);
  }

  @Test
  @DisplayName("Should handle service errors gracefully")
  void isBAAssigned_WhenServiceError_ShouldReturnInternalServerError() {
    // Given
    String brdId = "BRD123";
    String currentUser = "ba@example.com";
    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.BA_ROLE));
    when(brdSecurityService.getCurrentUserEmail()).thenReturn(Mono.just(currentUser));
    when(baAssignmentService.isBAAssignedToUser(brdId, currentUser))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // When
    Mono<ResponseEntity<Api<BAAssignmentStatusResponse>>> result =
        baAssignmentController.isBAAssigned(brdId);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<BAAssignmentStatusResponse> api = response.getBody();
              assertNotNull(api);
              assertEquals(BrdConstants.FAILURE, api.getStatus());
              assertTrue(api.getMessage().contains("An unexpected error occurred: Database error"));
              assertTrue(api.getData().isEmpty());
              return true;
            })
        .verifyComplete();

    verify(brdSecurityService).getCurrentUserRole();
    verify(brdSecurityService).getCurrentUserEmail();
    verify(baAssignmentService).isBAAssignedToUser(brdId, currentUser);
  }
}
