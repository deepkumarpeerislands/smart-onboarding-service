package com.aci.smart_onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBARequest;
import com.aci.smart_onboarding.dto.AssignBAResponse;
import com.aci.smart_onboarding.dto.BAReassignmentRequest;
import com.aci.smart_onboarding.dto.UserDetailResponse;
import com.aci.smart_onboarding.exception.BAAssignmentException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BAAssignment;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BAAssignmentRepository;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.service.implementation.BAAssignmentService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BAAssignmentServiceTest {

  @Mock private BAAssignmentRepository baAssignmentRepository;

  @Mock private IBRDService brdService;

  @Mock private BRDRepository brdRepository;

  @Mock private IEmailService emailService;

  @Mock private IUserService userService;

  @Mock private UserRepository userRepository;

  @InjectMocks private BAAssignmentService baAssignmentService;

  private String brdId;
  private BRD mockBrd;
  private BAAssignment mockAssignment;
  private AssignBARequest validRequest;

  @BeforeEach
  void setUp() {
    brdId = "BRD-123";

    // Set up BRD
    mockBrd = new BRD();
    mockBrd.setBrdId(brdId);
    mockBrd.setBrdFormId("FORM-123");
    mockBrd.setBrdName("Test BRD");

    // Set up request
    validRequest = new AssignBARequest();
    validRequest.setBaEmail("test@example.com");
    validRequest.setStatus(BrdConstants.STATUS_INTERNAL_REVIEW);
    validRequest.setDescription("Test assignment");

    // Set up assignment
    mockAssignment = new BAAssignment();
    mockAssignment.setBrdId(brdId);
    mockAssignment.setBaEmail("test@example.com");
    mockAssignment.setDescription("Test assignment");
    mockAssignment.setAssignedAt(LocalDateTime.now());
    mockAssignment.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void assignBA_Success() {
    // Arrange
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(baAssignmentRepository.existsByBrdId(brdId)).thenReturn(Mono.just(false));
    when(baAssignmentRepository.save(any(BAAssignment.class)))
        .thenReturn(Mono.just(mockAssignment));
    when(brdService.updateBrdStatus(
            mockBrd.getBrdFormId(), validRequest.getStatus(), validRequest.getDescription()))
        .thenReturn(Mono.empty());
    when(emailService.sendBrdStatusChangeNotification(
            validRequest.getBaEmail(),
            mockBrd.getBrdId(),
            mockBrd.getBrdName(),
            mockBrd.getBrdFormId()))
        .thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(baAssignmentService.assignBA(brdId, validRequest))
        .expectNextMatches(
            response ->
                response.getStatus().equals(validRequest.getStatus())
                    && response.getBrdId().equals(brdId)
                    && response.getBaEmail().equals(validRequest.getBaEmail())
                    && response.getDescription().equals(validRequest.getDescription()))
        .verifyComplete();

    // Verify interactions in order
    verify(brdRepository, times(2)).findByBrdId(brdId);
    verify(baAssignmentRepository, times(1)).existsByBrdId(brdId);
    verify(baAssignmentRepository, times(1)).save(any(BAAssignment.class));
    verify(brdService, times(1))
        .updateBrdStatus(
            mockBrd.getBrdFormId(), validRequest.getStatus(), validRequest.getDescription());
    verify(emailService, times(1))
        .sendBrdStatusChangeNotification(
            validRequest.getBaEmail(),
            mockBrd.getBrdId(),
            mockBrd.getBrdName(),
            mockBrd.getBrdFormId());
    verifyNoMoreInteractions(brdRepository, baAssignmentRepository, brdService, emailService);
  }

  @Test
  void validateAndCreateAssignment_ShouldCreateNewAssignment_WhenNoBAExists() {
    // Arrange
    when(baAssignmentRepository.existsByBrdId(brdId)).thenReturn(Mono.just(false));

    // Access the private method using reflection
    java.lang.reflect.Method method = null;
    try {
      method =
          BAAssignmentService.class.getDeclaredMethod(
              "validateAndCreateAssignment", String.class, AssignBARequest.class);
      method.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    // Act & Assert
    Object result;
    try {
      result = method.invoke(baAssignmentService, brdId, validRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    StepVerifier.create((Mono<BAAssignment>) result)
        .expectNextMatches(
            assignment ->
                assignment.getBrdId().equals(brdId)
                    && assignment.getBaEmail().equals(validRequest.getBaEmail())
                    && assignment.getDescription().equals(validRequest.getDescription()))
        .verifyComplete();

    verify(baAssignmentRepository).existsByBrdId(brdId);
  }

  @Test
  void validateAndCreateAssignment_ShouldReturnExistingAssignment_WhenSameBAAlreadyAssigned() {
    // Arrange
    when(baAssignmentRepository.existsByBrdId(brdId)).thenReturn(Mono.just(true));
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockAssignment));

    // Access the private method using reflection
    java.lang.reflect.Method method = null;
    try {
      method =
          BAAssignmentService.class.getDeclaredMethod(
              "validateAndCreateAssignment", String.class, AssignBARequest.class);
      method.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    // Act & Assert
    Object result;
    try {
      result = method.invoke(baAssignmentService, brdId, validRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    StepVerifier.create((Mono<BAAssignment>) result).expectNext(mockAssignment).verifyComplete();

    verify(baAssignmentRepository).existsByBrdId(brdId);
    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  void validateAndCreateAssignment_ShouldThrowException_WhenDifferentBAAlreadyAssigned() {
    // Arrange
    String differentEmail = "different@example.com";
    AssignBARequest newRequest = new AssignBARequest();
    newRequest.setBaEmail(differentEmail);
    newRequest.setStatus(BrdConstants.STATUS_INTERNAL_REVIEW);
    newRequest.setDescription("New assignment");

    when(baAssignmentRepository.existsByBrdId(brdId)).thenReturn(Mono.just(true));
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockAssignment));

    // Access the private method using reflection
    java.lang.reflect.Method method = null;
    try {
      method =
          BAAssignmentService.class.getDeclaredMethod(
              "validateAndCreateAssignment", String.class, AssignBARequest.class);
      method.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    // Act & Assert
    Object result;
    try {
      result = method.invoke(baAssignmentService, brdId, newRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    StepVerifier.create((Mono<BAAssignment>) result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BAAssignmentException
                    && throwable.getMessage().contains("Cannot assign BA to BRD"))
        .verify();

    verify(baAssignmentRepository).existsByBrdId(brdId);
    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  void validateAndCreateAssignment_ShouldHandleErrorRetrieval() {
    // Arrange
    when(baAssignmentRepository.existsByBrdId(brdId)).thenReturn(Mono.just(true));
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    // Access the private method using reflection
    java.lang.reflect.Method method = null;
    try {
      method =
          BAAssignmentService.class.getDeclaredMethod(
              "validateAndCreateAssignment", String.class, AssignBARequest.class);
      method.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    // Act & Assert
    Object result;
    try {
      result = method.invoke(baAssignmentService, brdId, validRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    StepVerifier.create((Mono<BAAssignment>) result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BAAssignmentException
                    && throwable.getMessage().contains("Error retrieving BA assignment for BRD"))
        .verify();

    verify(baAssignmentRepository).existsByBrdId(brdId);
    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  void getAssignmentsByBaUsername_Success() {
    // Arrange
    String username = "test@example.com";
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

    when(baAssignmentRepository.findByBaEmail(username))
        .thenReturn(Flux.fromIterable(expectedAssignments));

    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(username))
        .expectNext(expectedAssignments)
        .verifyComplete();

    verify(baAssignmentRepository).findByBaEmail(username);
  }

  @Test
  void getAssignmentsByBaUsername_EmptyList() {
    // Arrange
    String username = "test@example.com";
    when(baAssignmentRepository.findByBaEmail(username)).thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(username))
        .expectNext(Collections.emptyList())
        .verifyComplete();

    verify(baAssignmentRepository).findByBaEmail(username);
  }

  @Test
  void getAssignmentsByBaUsername_InvalidEmail() {
    // Arrange
    String invalidEmail = "invalid-email";

    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(invalidEmail))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().contains("Invalid email format"))
        .verify();

    verifyNoInteractions(baAssignmentRepository);
  }

  @Test
  void getAssignmentsByBaUsername_EmptyUsername() {
    // Arrange
    String emptyUsername = "";

    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(emptyUsername))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Username cannot be empty"))
        .verify();

    verifyNoInteractions(baAssignmentRepository);
  }

  @Test
  void getAssignmentsByBaUsername_NullUsername() {
    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Username cannot be empty"))
        .verify();

    verifyNoInteractions(baAssignmentRepository);
  }

  @Test
  void getAssignmentsByBaUsername_RepositoryError() {
    // Arrange
    String username = "test@example.com";
    when(baAssignmentRepository.findByBaEmail(username))
        .thenReturn(Flux.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(baAssignmentService.getAssignmentsByBaUsername(username))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BAAssignmentException
                    && throwable.getMessage().contains("Failed to fetch assignments for BA"))
        .verify();

    verify(baAssignmentRepository).findByBaEmail(username);
  }

  @Test
  void reassignBAs_Success() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("ba1@example.com")
                .build(),
            BAReassignmentRequest.builder()
                .brdId("BRD-124")
                .newBaUsername("ba2@example.com")
                .build());

    BAAssignment assignment1 = new BAAssignment();
    assignment1.setBrdId("BRD-123");
    assignment1.setBaEmail("oldba1@example.com");

    BAAssignment assignment2 = new BAAssignment();
    assignment2.setBrdId("BRD-124");
    assignment2.setBaEmail("oldba2@example.com");

    // Update test user responses with correct BA role
    UserDetailResponse baResponse1 =
        UserDetailResponse.builder()
            .id("1")
            .username("ba1@example.com")
            .activeRole(SecurityConstants.ROLE_BA)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA))
            .build();

    UserDetailResponse baResponse2 =
        UserDetailResponse.builder()
            .id("2")
            .username("ba2@example.com")
            .activeRole(SecurityConstants.ROLE_BA)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA))
            .build();

    // Update API responses
    Api<UserDetailResponse> baApiResponse1 = new Api<>();
    baApiResponse1.setStatus(BrdConstants.SUCCESSFUL);
    baApiResponse1.setData(Optional.of(baResponse1));

    Api<UserDetailResponse> baApiResponse2 = new Api<>();
    baApiResponse2.setStatus(BrdConstants.SUCCESSFUL);
    baApiResponse2.setData(Optional.of(baResponse2));

    // Update service mocks
    when(userService.getUserByEmail("ba1@example.com"))
        .thenReturn(Mono.just(ResponseEntity.ok(baApiResponse1)));
    when(userService.getUserByEmail("ba2@example.com"))
        .thenReturn(Mono.just(ResponseEntity.ok(baApiResponse2)));
    when(baAssignmentRepository.findByBrdId("BRD-123")).thenReturn(Mono.just(assignment1));
    when(baAssignmentRepository.findByBrdId("BRD-124")).thenReturn(Mono.just(assignment2));
    when(baAssignmentRepository.save(any(BAAssignment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Act & Assert
    StepVerifier.create(baAssignmentService.reassignBAs(requests))
        .expectNextMatches(
            response ->
                BrdConstants.SUCCESSFUL.equals(response.getStatus())
                    && "All BAs reassigned successfully".equals(response.getMessage())
                    && response.getData().isEmpty()
                    && response.getErrors().isEmpty())
        .verifyComplete();

    verify(baAssignmentRepository, times(2)).save(any(BAAssignment.class));
  }

  @Test
  void reassignBAs_PartialFailure() {
    // Arrange
    List<BAReassignmentRequest> requests =
        Arrays.asList(
            BAReassignmentRequest.builder()
                .brdId("BRD-124")
                .newBaUsername("ba2@example.com")
                .build(),
            BAReassignmentRequest.builder()
                .brdId("BRD-123")
                .newBaUsername("ba1@example.com")
                .build());

    BAAssignment assignment = new BAAssignment();
    assignment.setBrdId("BRD-123");
    assignment.setBaEmail("oldba@example.com");

    // Update test user responses - make ba1 a BA and ba2 not a BA
    UserDetailResponse baResponse =
        UserDetailResponse.builder()
            .id("1")
            .username("ba1@example.com")
            .activeRole(SecurityConstants.ROLE_BA)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA))
            .build();

    UserDetailResponse nonBaResponse =
        UserDetailResponse.builder()
            .id("2")
            .username("ba2@example.com")
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
            .build();

    // Update API responses
    Api<UserDetailResponse> baApiResponse = new Api<>();
    baApiResponse.setStatus(BrdConstants.SUCCESSFUL);
    baApiResponse.setData(Optional.of(baResponse));

    Api<UserDetailResponse> nonBaApiResponse = new Api<>();
    nonBaApiResponse.setStatus(BrdConstants.SUCCESSFUL);
    nonBaApiResponse.setData(Optional.of(nonBaResponse));

    // Update service mocks
    when(userService.getUserByEmail("ba1@example.com"))
        .thenReturn(Mono.just(ResponseEntity.ok(baApiResponse)));
    when(userService.getUserByEmail("ba2@example.com"))
        .thenReturn(Mono.just(ResponseEntity.ok(nonBaApiResponse)));
    when(baAssignmentRepository.findByBrdId("BRD-123")).thenReturn(Mono.just(assignment));
    when(baAssignmentRepository.save(any(BAAssignment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Act & Assert
    StepVerifier.create(baAssignmentService.reassignBAs(requests))
        .consumeNextWith(
            response -> {
              assertThat(response.getStatus()).isEqualTo(BrdConstants.FAILURE);
              assertThat(response.getMessage()).isEqualTo("Some reassignments failed");
              assertThat(response.getData()).isEmpty();
              assertThat(response.getErrors()).isPresent();

              Map<String, String> errors = response.getErrors().get();
              assertThat(errors).hasSize(1);
              assertThat(errors.values())
                  .containsExactly(
                      "Failed to reassign BRD BRD-124: User ba2@example.com is not a BA");
            })
        .verifyComplete();

    verify(baAssignmentRepository, times(1)).save(any(BAAssignment.class));
  }

  @Test
  @DisplayName("Should assign BA to BRD when both users exist and have correct roles")
  void assignBA_WhenUsersExistWithCorrectRoles_ShouldAssignBA() {
    // Given
    String baEmail = "ba@example.com";
    String testBrdId = "brd123";

    BRD brd =
        BRD.builder()
            .brdId(testBrdId)
            .brdFormId("FORM-123")
            .brdName("Test BRD")
            .status(BrdConstants.DRAFT)
            .build();

    BAAssignment baAssignment =
        BAAssignment.builder()
            .brdId(testBrdId)
            .baEmail(baEmail)
            .description("Test assignment")
            .assignedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    AssignBARequest request =
        AssignBARequest.builder()
            .baEmail(baEmail)
            .status(BrdConstants.STATUS_INTERNAL_REVIEW)
            .description("Test assignment")
            .build();

    // Mock repository calls
    when(brdRepository.findByBrdId(testBrdId)).thenReturn(Mono.just(brd));
    when(baAssignmentRepository.existsByBrdId(testBrdId)).thenReturn(Mono.just(false));
    when(baAssignmentRepository.save(any(BAAssignment.class))).thenReturn(Mono.just(baAssignment));
    when(brdService.updateBrdStatus(
            brd.getBrdFormId(), BrdConstants.STATUS_INTERNAL_REVIEW, request.getDescription()))
        .thenReturn(Mono.empty());
    when(emailService.sendBrdStatusChangeNotification(
            baEmail, brd.getBrdId(), brd.getBrdName(), brd.getBrdFormId()))
        .thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(baAssignmentService.assignBA(testBrdId, request))
        .expectNextMatches(
            response ->
                response.getBrdId().equals(testBrdId)
                    && response.getBaEmail().equals(baEmail)
                    && response.getStatus().equals(BrdConstants.STATUS_INTERNAL_REVIEW)
                    && response.getDescription().equals("Test assignment"))
        .verifyComplete();

    // Verify interactions in order
    verify(brdRepository, times(2)).findByBrdId(testBrdId);
    verify(baAssignmentRepository, times(1)).existsByBrdId(testBrdId);
    verify(baAssignmentRepository, times(1)).save(any(BAAssignment.class));
    verify(brdService, times(1))
        .updateBrdStatus(
            brd.getBrdFormId(), BrdConstants.STATUS_INTERNAL_REVIEW, request.getDescription());
    verify(emailService, times(1))
        .sendBrdStatusChangeNotification(
            baEmail, brd.getBrdId(), brd.getBrdName(), brd.getBrdFormId());
    verifyNoMoreInteractions(brdRepository, baAssignmentRepository, brdService, emailService);
  }

  @Test
  @DisplayName("Should fail to assign BA when BRD does not exist")
  void assignBA_WhenBrdDoesNotExist_ShouldFail() {
    // Given
    String testBrdId = "nonexistent-brd";
    String baEmail = "ba@example.com";

    AssignBARequest request =
        AssignBARequest.builder()
            .baEmail(baEmail)
            .status(BrdConstants.STATUS_INTERNAL_REVIEW)
            .description("Test assignment")
            .build();

    when(brdRepository.findByBrdId(testBrdId)).thenReturn(Mono.empty());
    when(baAssignmentRepository.existsByBrdId(testBrdId)).thenReturn(Mono.just(false));

    // When & Then
    StepVerifier.create(baAssignmentService.assignBA(testBrdId, request))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("BRD not found with id: " + testBrdId))
        .verify();

    verify(brdRepository, times(1)).findByBrdId(testBrdId);
    verifyNoMoreInteractions(brdRepository, baAssignmentRepository, brdService, emailService);
  }

  @Test
  @DisplayName("Should fail to assign BA when BA is already assigned")
  void assignBA_WhenBAAlreadyAssigned_ShouldFail() {
    // Given
    String testBrdId = "brd123";
    String baEmail = "ba@example.com";

    BRD brd =
        BRD.builder().brdId(testBrdId).status(BrdConstants.DRAFT).creator("pm@example.com").build();

    AssignBARequest request =
        AssignBARequest.builder()
            .baEmail(baEmail)
            .status(BrdConstants.STATUS_INTERNAL_REVIEW)
            .description("Test assignment")
            .build();

    when(brdRepository.findByBrdId(testBrdId)).thenReturn(Mono.just(brd));
    when(baAssignmentRepository.existsByBrdId(testBrdId)).thenReturn(Mono.just(true));
    when(baAssignmentRepository.findByBrdId(testBrdId))
        .thenReturn(
            Mono.just(
                BAAssignment.builder().brdId(testBrdId).baEmail("existing@example.com").build()));

    // When
    Mono<AssignBAResponse> result = baAssignmentService.assignBA(testBrdId, request);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof BAAssignmentException
                    && throwable.getMessage().contains("Cannot assign BA to BRD"))
        .verify();

    verify(brdRepository).findByBrdId(testBrdId);
    verify(baAssignmentRepository).existsByBrdId(testBrdId);
    verify(baAssignmentRepository).findByBrdId(testBrdId);
    verifyNoInteractions(brdService);
    verifyNoInteractions(emailService);
  }

  @Test
  @DisplayName("Should return true when specific BA is assigned to BRD")
  void isBAAssignedToUser_WhenBAAssigned_ShouldReturnTrue() {
    // Arrange
    String baEmail = "ba@example.com";
    BAAssignment assignment = new BAAssignment();
    assignment.setBrdId(brdId);
    assignment.setBaEmail(baEmail);
    
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.just(assignment));

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(brdId, baEmail))
        .expectNext(true)
        .verifyComplete();

    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  @DisplayName("Should return false when specific BA is not assigned to BRD")
  void isBAAssignedToUser_WhenBANotAssigned_ShouldReturnFalse() {
    // Arrange
    String baEmail = "ba@example.com";
    String differentBA = "other@example.com";
    BAAssignment assignment = new BAAssignment();
    assignment.setBrdId(brdId);
    assignment.setBaEmail(differentBA);
    
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.just(assignment));

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(brdId, baEmail))
        .expectNext(false)
        .verifyComplete();

    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  @DisplayName("Should return false when no assignment exists for BRD")
  void isBAAssignedToUser_WhenNoAssignment_ShouldReturnFalse() {
    // Arrange
    String baEmail = "ba@example.com";
    when(baAssignmentRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(brdId, baEmail))
        .expectNext(false)
        .verifyComplete();

    verify(baAssignmentRepository).findByBrdId(brdId);
  }

  @Test
  @DisplayName("Should return error when BRD ID is empty")
  void isBAAssignedToUser_WhenBrdIdEmpty_ShouldReturnError() {
    // Arrange
    String emptyBrdId = "";
    String baEmail = "ba@example.com";

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(emptyBrdId, baEmail))
        .expectErrorMatches(e -> 
            e instanceof BadRequestException && 
            e.getMessage().equals("BRD ID cannot be empty"))
        .verify();

    verifyNoInteractions(baAssignmentRepository);
  }

  @Test
  @DisplayName("Should return error when BA email is empty")
  void isBAAssignedToUser_WhenBAEmailEmpty_ShouldReturnError() {
    // Arrange
    String emptyEmail = "";

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(brdId, emptyEmail))
        .expectErrorMatches(e -> 
            e instanceof BadRequestException && 
            e.getMessage().equals("BA email cannot be empty"))
        .verify();

    verifyNoInteractions(baAssignmentRepository);
  }

  @Test
  @DisplayName("Should handle repository errors gracefully")
  void isBAAssignedToUser_WhenRepositoryError_ShouldReturnError() {
    // Arrange
    String baEmail = "ba@example.com";
    when(baAssignmentRepository.findByBrdId(brdId))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act & Assert
    StepVerifier.create(baAssignmentService.isBAAssignedToUser(brdId, baEmail))
        .expectErrorMatches(e -> 
            e instanceof BAAssignmentException && 
            e.getMessage().contains("Failed to check BA assignment for BRD") &&
            e.getMessage().contains("Database error"))
        .verify();

    verify(baAssignmentRepository).findByBrdId(brdId);
  }
}
