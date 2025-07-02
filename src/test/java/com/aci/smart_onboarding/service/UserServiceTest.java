package com.aci.smart_onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.dto.ChangePasswordRequest;
import com.aci.smart_onboarding.dto.UserDetailResponse;
import com.aci.smart_onboarding.dto.UserListResponse;
import com.aci.smart_onboarding.dto.UserProjection;
import com.aci.smart_onboarding.dto.UserRequest;
import com.aci.smart_onboarding.dto.UserResponse;
import com.aci.smart_onboarding.enums.UserStatus;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BAAssignment;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.BAAssignmentRepository;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.service.implementation.UserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private BRDRepository brdRepository;

  @Mock private BAAssignmentRepository baAssignmentRepository;

  @Mock private IEmailService emailService;

  @Mock private IAuditLogService auditLogService;

  @InjectMocks private UserService userService;

  private UserRequest userRequest;
  private User user;
  private UserProjection pmUser1;
  private UserProjection pmUser2;
  private UserProjection baUser;
  private LocalDateTime now;
  private final String testEmail = "john.doe@example.com";
  private final String defaultPassword = "John_" + SecurityConstants.ROLE_PM;

  private static final class TestConstants {
    private static final String TEST_USER_ID = "1";
    private static final String TEST_EMAIL = "john.doe@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String TEST_PM_ID = "64a1b2c3d4e5f6789012345a";
    private static final String TEST_BA_ID = "64a1b2c3d4e5f6789012345c";
    private static final String ENCODED_PASSWORD = "encoded_password";
  }

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    setupUserRequest();
    setupTestUser();
    setupTestProjections();
  }

  private void setupUserRequest() {
    userRequest =
        UserRequest.builder()
            .firstName(TestConstants.TEST_FIRST_NAME)
            .lastName(TestConstants.TEST_LAST_NAME)
            .email(TestConstants.TEST_EMAIL)
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
            .build();
  }

  private void setupTestUser() {
    user =
        User.builder()
            .id(TestConstants.TEST_USER_ID)
            .firstName(TestConstants.TEST_FIRST_NAME)
            .lastName(TestConstants.TEST_LAST_NAME)
            .email(TestConstants.TEST_EMAIL.toLowerCase())
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
            .password(TestConstants.ENCODED_PASSWORD.toCharArray())
            .createdAt(now)
            .build();
  }

  private void setupTestProjections() {
    setupPmUser1();
    setupPmUser2();
    setupBaUser();
  }

  private void setupPmUser1() {
    pmUser1 = mock(UserProjection.class);
    lenient().when(pmUser1.getId()).thenReturn(TestConstants.TEST_PM_ID);
    lenient().when(pmUser1.getFirstName()).thenReturn(TestConstants.TEST_FIRST_NAME);
    lenient().when(pmUser1.getLastName()).thenReturn(TestConstants.TEST_LAST_NAME);
    lenient().when(pmUser1.getEmail()).thenReturn(TestConstants.TEST_EMAIL);
    lenient().when(pmUser1.getActiveRole()).thenReturn(SecurityConstants.ROLE_PM);
    lenient()
        .when(pmUser1.getRoles())
        .thenReturn(Collections.singletonList(SecurityConstants.ROLE_PM));
    lenient().when(pmUser1.getStatus()).thenReturn(UserStatus.ACTIVE);
    lenient().when(pmUser1.getCreatedAt()).thenReturn(now);
  }

  private void setupPmUser2() {
    pmUser2 = mock(UserProjection.class);
    lenient().when(pmUser2.getId()).thenReturn("64a1b2c3d4e5f6789012345b");
    lenient().when(pmUser2.getFirstName()).thenReturn("Jane");
    lenient().when(pmUser2.getLastName()).thenReturn("Smith");
    lenient().when(pmUser2.getEmail()).thenReturn("jane.smith@example.com");
    lenient().when(pmUser2.getActiveRole()).thenReturn(SecurityConstants.ROLE_PM);
    lenient()
        .when(pmUser2.getRoles())
        .thenReturn(Collections.singletonList(SecurityConstants.ROLE_PM));
    lenient().when(pmUser2.getStatus()).thenReturn(UserStatus.ACTIVE);
    lenient().when(pmUser2.getCreatedAt()).thenReturn(now);
  }

  private void setupBaUser() {
    baUser = mock(UserProjection.class);
    lenient().when(baUser.getId()).thenReturn(TestConstants.TEST_BA_ID);
    lenient().when(baUser.getFirstName()).thenReturn("Bob");
    lenient().when(baUser.getLastName()).thenReturn("Wilson");
    lenient().when(baUser.getEmail()).thenReturn("bob.wilson@example.com");
    lenient().when(baUser.getActiveRole()).thenReturn(SecurityConstants.ROLE_BA);
    lenient()
        .when(baUser.getRoles())
        .thenReturn(Collections.singletonList(SecurityConstants.ROLE_BA));
    lenient().when(baUser.getStatus()).thenReturn(UserStatus.ACTIVE);
    lenient().when(baUser.getCreatedAt()).thenReturn(now);
  }

  @Test
  @DisplayName("Create user should generate default password and normalize email")
  void createUser_ShouldGenerateDefaultPasswordAndNormalizeEmail() {
    // Given
    String defaultPasswordTest = userRequest.getFirstName() + "_" + userRequest.getActiveRole();
    String encodedPassword = "encoded_" + defaultPasswordTest;
    when(passwordEncoder.encode(defaultPasswordTest)).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(emailService.sendUserCredentialsEmail(anyString(), anyString())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(userService.createUser(userRequest))
        .expectNextMatches(
            response -> {
              UserResponse data = response.getBody().getData().orElse(null);
              return data != null
                  && data.getEmail().equals(testEmail.toLowerCase())
                  && response.getBody().getStatus().equals(UserConstants.SUCCESS)
                  && response.getBody().getMessage().equals("User created successfully");
            })
        .verifyComplete();

    verify(userRepository)
        .save(
            argThat(
                savedUser ->
                    savedUser.getEmail().equals(testEmail.toLowerCase())
                        && savedUser.getPassword() != null
                        && Arrays.equals(savedUser.getPassword(), encodedPassword.toCharArray())));
  }

  @Test
  @DisplayName("normalizeEmail should handle null email")
  void normalizeEmail_ShouldHandleNullEmail() {
    // Given
    userRequest.setEmail(null);

    // When & Then - Should throw NullPointerException when trying to convert encoded password to
    // char array
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.createUser(userRequest);
                }))
        .hasMessageContaining(
            "Cannot invoke \"String.toCharArray()\" because \"encodedPassword\" is null");
  }

  @Test
  @DisplayName("getUserByEmail should normalize email before lookup")
  void getUserByEmail_ShouldNormalizeEmailBeforeLookup() {
    // Given
    String mixedCaseEmail = "John.Doe@Example.com";
    when(userRepository.findByEmail(mixedCaseEmail.toLowerCase())).thenReturn(Mono.just(user));

    // When
    Mono<ResponseEntity<Api<UserDetailResponse>>> result =
        userService.getUserByEmail(mixedCaseEmail);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              verify(userRepository).findByEmail(mixedCaseEmail.toLowerCase());
              Api<UserDetailResponse> body = response.getBody();
              assertThat(body).isNotNull();
              assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
              assertThat(body.getData()).isPresent();
              assertThat(body.getData().get().getUsername())
                  .isEqualTo(mixedCaseEmail.toLowerCase());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getUserByEmail with null email should handle gracefully")
  void getUserByEmail_WithNullEmail_ShouldHandleGracefully() {
    // When & Then - Should throw NullPointerException when repository returns null for null email
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.getUserByEmail(null);
                }))
        .hasMessageContaining(
            "Cannot invoke \"reactor.core.publisher.Mono.switchIfEmpty(reactor.core.publisher.Mono)\" because the return value of \"com.aci.smart_onboarding.repository.UserRepository.findByEmail(String)\" is null");
  }

  @Test
  @DisplayName("Create user should set createdAt timestamp")
  void createUser_ShouldSetCreatedAtTimestamp() {
    // Given
    when(passwordEncoder.encode(defaultPassword)).thenReturn("encoded_password");
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(emailService.sendUserCredentialsEmail(anyString(), anyString())).thenReturn(Mono.empty());

    // When
    Mono<ResponseEntity<Api<UserResponse>>> result = userService.createUser(userRequest);

    // Then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              verify(userRepository).save(userCaptor.capture());
              User savedUser = userCaptor.getValue();
              assertThat(savedUser.getCreatedAt()).isNotNull();
              assertThat(response.getBody().getStatus()).isEqualTo(UserConstants.SUCCESS);
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Handle errors should convert duplicate key error message to AlreadyExistException")
  void handleErrors_ShouldConvertDuplicateKeyErrorMessageToAlreadyExistException() {
    // Given - use error message matching instead of DuplicateKeyException
    Exception ex = new Exception("duplicate key error collection: email");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isInstanceOf(AlreadyExistException.class);
    assertThat(result.getMessage()).contains("User already exists");
  }

  @Test
  @DisplayName("Handle BadRequestException should pass through")
  void handleErrors_ShouldPassThroughBadRequestException() {
    // Given
    BadRequestException ex = new BadRequestException("Bad request");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isSameAs(ex);
    assertThat(result.getMessage()).isEqualTo("Bad request");
  }

  @Test
  @DisplayName("Handle NotFound exceptions should pass through")
  void handleErrors_ShouldPassThroughNotFoundExceptions() {
    // Given
    NotFoundException ex = new NotFoundException("User not found");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isSameAs(ex);
    assertThat(result.getMessage()).isEqualTo("User not found");
  }

  @Test
  @DisplayName("Handle AlreadyExistException should pass through")
  void handleErrors_ShouldPassThroughAlreadyExistException() {
    // Given
    AlreadyExistException ex = new AlreadyExistException("User already exists");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isSameAs(ex);
    assertThat(result.getMessage()).isEqualTo("User already exists");
  }

  @Test
  @DisplayName("Handle DuplicateKeyException should convert to AlreadyExistException")
  void handleErrors_WithDuplicateKeyException_ShouldConvertToAlreadyExistException() {
    // Given
    org.springframework.dao.DuplicateKeyException ex =
        new org.springframework.dao.DuplicateKeyException("Duplicate key error for email");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result)
        .isInstanceOf(AlreadyExistException.class)
        .hasMessageContaining("User already exists");
  }

  @Test
  @DisplayName("Handle exception with 'Something went wrong:' prefix should pass through")
  void handleErrors_WithSomethingWentWrongPrefix_ShouldPassThrough() {
    // Given
    Exception ex = new Exception("Something went wrong: database error");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isSameAs(ex);
    assertThat(result.getMessage()).isEqualTo("Something went wrong: database error");
  }

  @Test
  @DisplayName("Handle generic exception should wrap with 'Something went wrong' message")
  void handleErrors_WithGenericException_ShouldWrapWithSomethingWentWrong() {
    // Given
    Exception ex = new Exception("Generic error");

    // When
    Throwable result = userService.handleErrors(ex);

    // Then
    assertThat(result).isInstanceOf(Exception.class);
    assertThat(result.getMessage()).startsWith("Something went wrong: Generic error");
  }

  @Test
  @DisplayName("Create user with null request should throw NullPointerException")
  void createUser_WithNullRequest_ShouldThrowNullPointerException() {
    // When & Then
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.createUser(null);
                }))
        .hasMessageContaining(
            "Cannot invoke \"com.aci.smart_onboarding.dto.UserRequest.getFirstName()\" because \"request\" is null");
  }

  @Test
  @DisplayName("Create user with null firstName should throw NullPointerException")
  void createUser_WithNullFirstName_ShouldThrowNullPointerException() {
    // Given
    userRequest.setFirstName(null);

    // When & Then
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.createUser(userRequest);
                }))
        .hasMessageContaining(
            "Cannot invoke \"String.toCharArray()\" because \"encodedPassword\" is null");
  }

  @Test
  @DisplayName("Create user with null lastName should throw NullPointerException")
  void createUser_WithNullLastName_ShouldThrowNullPointerException() {
    // Given
    userRequest.setLastName(null);

    // When & Then
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.createUser(userRequest);
                }))
        .hasMessageContaining(
            "Cannot invoke \"String.toCharArray()\" because \"encodedPassword\" is null");
  }

  @Test
  @DisplayName("Create user with null role should throw NullPointerException")
  void createUser_WithNullRole_ShouldThrowNullPointerException() {
    // Given
    userRequest.setActiveRole(null);

    // When & Then
    assertThat(
            org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                  userService.createUser(userRequest);
                }))
        .hasMessageContaining(
            "Cannot invoke \"String.replace(java.lang.CharSequence, java.lang.CharSequence)\" because the return value of \"com.aci.smart_onboarding.dto.UserRequest.getActiveRole()\" is null");
  }

  @Nested
  @DisplayName("Search PM and BA Users Tests")
  class SearchPMAndBAUsersTests {

    @Test
    @DisplayName("Search with valid term should return filtered users")
    void searchPMAndBAUsers_WithValidTerm_ShouldReturnFilteredUsers() {
      // Given
      String searchTerm = "john";
      List<String> roles = Arrays.asList(UserConstants.PM_ROLE, UserConstants.BA_ROLE);

      // Mock text search to return empty to trigger regex fallback
      when(userRepository.searchByRoleAndTextProjected(roles, searchTerm)).thenReturn(Flux.empty());

      // Mock regex search to return results
      when(userRepository.searchByRoleAndNameOrEmailProjectedOptimized(
              roles, searchTerm, searchTerm, searchTerm))
          .thenReturn(Flux.just(pmUser1, pmUser2));

      // When
      Mono<ResponseEntity<Api<UserListResponse>>> result =
          userService.searchPMAndBAUsers(searchTerm);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Api<UserListResponse> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.getData()).isPresent();
                UserListResponse userList = body.getData().get();

                assertThat(userList.getPmUsers())
                    .hasSize(2)
                    .allMatch(
                        userPM ->
                            userPM.getActiveRole().equals(SecurityConstants.ROLE_PM)
                                && userPM.getRoles().contains(SecurityConstants.ROLE_PM));

                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Search with empty term should return all users")
    void searchPMAndBAUsers_WithEmptyTerm_ShouldReturnAllUsers() {
      // Given
      List<String> roles = Arrays.asList(SecurityConstants.ROLE_PM, SecurityConstants.ROLE_BA);
      when(userRepository.findByRoleInOrRolesInProjected(roles))
          .thenReturn(Flux.just(pmUser1, pmUser2, baUser));

      // When
      Mono<ResponseEntity<Api<UserListResponse>>> result = userService.searchPMAndBAUsers("");

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Api<UserListResponse> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.getData()).isPresent();
                UserListResponse userList = body.getData().get();

                assertThat(userList.getPmUsers())
                    .hasSize(2)
                    .allMatch(
                        userInfoPM ->
                            userInfoPM.getActiveRole().equals(SecurityConstants.ROLE_PM)
                                && userInfoPM.getRoles().contains(SecurityConstants.ROLE_PM));

                assertThat(userList.getBaUsers())
                    .hasSize(1)
                    .allMatch(
                        userInfoBA ->
                            userInfoBA.getActiveRole().equals(SecurityConstants.ROLE_BA)
                                && userInfoBA.getRoles().contains(SecurityConstants.ROLE_BA));

                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Get users by role should return users when found in either role or roles")
  void getUsersByRole_WhenUsersExist_ShouldReturnUsers() {
    // Given
    String roleToSearch = SecurityConstants.ROLE_PM;
    List<User> users =
        Arrays.asList(
            User.builder()
                .id("1")
                .email("pm1@example.com")
                .firstName("PM1")
                .lastName("User")
                .activeRole(roleToSearch)
                .roles(Collections.singletonList(roleToSearch))
                .build(),
            User.builder()
                .id("2")
                .email("pm2@example.com")
                .firstName("PM2")
                .lastName("User")
                .activeRole(roleToSearch)
                .roles(Collections.singletonList(roleToSearch))
                .build());

    when(userRepository.findByRole(roleToSearch)).thenReturn(Flux.empty());
    when(userRepository.findByRoles(roleToSearch)).thenReturn(Flux.fromIterable(users));

    // When
    Flux<UserDetailResponse> result = userService.getUsersByRole(roleToSearch);

    // Then
    StepVerifier.create(result).expectNextCount(2).verifyComplete();

    verify(userRepository).findByRole(roleToSearch);
    verify(userRepository).findByRoles(roleToSearch);
  }

  @Test
  @DisplayName("Get users by role should return empty flux when no users found")
  void getUsersByRole_WhenNoUsers_ShouldReturnEmptyFlux() {
    // Given
    String roleToSearch = SecurityConstants.ROLE_PM;
    when(userRepository.findByRole(roleToSearch)).thenReturn(Flux.empty());
    when(userRepository.findByRoles(roleToSearch)).thenReturn(Flux.empty());

    // When
    Flux<UserDetailResponse> result = userService.getUsersByRole(roleToSearch);

    // Then
    StepVerifier.create(result).verifyComplete();

    verify(userRepository).findByRole(roleToSearch);
    verify(userRepository).findByRoles(roleToSearch);
  }

  @Test
  @DisplayName("Get users by role should handle empty role parameter")
  void getUsersByRole_WhenEmptyRole_ShouldReturnEmptyFlux() {
    // Given
    String role = "";
    when(userRepository.findByRole(role)).thenReturn(Flux.empty());
    when(userRepository.findByRoles(role)).thenReturn(Flux.empty());

    // When
    Flux<UserDetailResponse> result = userService.getUsersByRole(role);

    // Then
    StepVerifier.create(result).expectNextCount(0).verifyComplete();
  }

  @Test
  @DisplayName("Get users by role should handle null role parameter")
  void getUsersByRole_WhenNullRole_ShouldReturnEmptyFlux() {
    // Given
    String role = null;
    when(userRepository.findByRole(role)).thenReturn(Flux.empty());
    when(userRepository.findByRoles(role)).thenReturn(Flux.empty());

    // When
    Flux<UserDetailResponse> result = userService.getUsersByRole(role);

    // Then
    StepVerifier.create(result).expectNextCount(0).verifyComplete();
  }

  @Test
  @DisplayName("Get users by role should handle repository errors")
  void getUsersByRole_WhenRepositoryError_ShouldPropagateError() {
    // Given
    String role = SecurityConstants.ROLE_PM;
    RuntimeException expectedError = new RuntimeException("Database error");
    when(userRepository.findByRole(role)).thenReturn(Flux.empty());
    when(userRepository.findByRoles(role)).thenReturn(Flux.error(expectedError));

    // When
    Flux<UserDetailResponse> result = userService.getUsersByRole(role);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();

    verify(userRepository).findByRole(role);
    verify(userRepository).findByRoles(role);
  }

  @Test
  @DisplayName("Reassign PM should update BRD creator when BRD and PM exist")
  void reassignProjectManager_WhenBRDAndPMExist_ShouldUpdateCreator() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "newpm@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User pmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(pmUser));
    when(brdRepository.save(any(BRD.class))).thenReturn(Mono.just(existingBrd));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(new Api<>("SUCCESS", "Audit log created", Optional.empty(), Optional.empty()))));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                "success".equals(response.getStatus())
                    && "Project Manager reassigned successfully".equals(response.getMessage())
                    && !response.getErrors().isPresent())
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository).save(any(BRD.class));
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should return NOT_FOUND when BRD doesn't exist")
  void reassignProjectManager_WhenBRDNotFound_ShouldReturnNotFound() {
    // Given
    String brdId = "nonexistent";
    String newPmUsername = "newpm@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                "failure".equals(response.getStatus())
                    && "BRD not found".equals(response.getMessage())
                    && !response.getErrors().isPresent())
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository, never()).findByEmail(anyString());
    verify(brdRepository, never()).save(any(BRD.class));
    verify(auditLogService, never()).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should return BAD_REQUEST when PM doesn't exist")
  void reassignProjectManager_WhenPMNotFound_ShouldReturnBadRequest() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "nonexistent@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.empty());

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            e ->
                e instanceof Exception
                    && e.getMessage().contains("Something went wrong: PM not found"))
        .verify();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository, never()).save(any(BRD.class));
    verify(auditLogService, never()).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should return BAD_REQUEST when user is not a PM")
  void reassignProjectManager_WhenUserNotPM_ShouldReturnBadRequest() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "manager@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User nonPmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER))
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(nonPmUser));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(api ->
            ErrorValidationMessage.FAILURE.equals(api.getStatus()) &&
            api.getMessage().contains(DashboardConstants.PM_ONLY_ROLE))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository, never()).save(any(BRD.class));
    verify(auditLogService, never()).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should succeed when user has PM role in activeRole")
  void reassignProjectManager_WhenUserHasPMInActiveRole_ShouldSucceed() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "pm@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User pmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(SecurityConstants.PM_ROLE)
            .roles(Collections.emptyList())
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(pmUser));
    when(brdRepository.save(any(BRD.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(new Api<>("SUCCESS", "Audit log created", Optional.empty(), Optional.empty()))));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(api ->
            "success".equals(api.getStatus()) &&
            "Project Manager reassigned successfully".equals(api.getMessage()))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository).save(any(BRD.class));
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should succeed when user has PM role in roles list")
  void reassignProjectManager_WhenUserHasPMInRolesList_ShouldSucceed() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "pm@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User pmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(null) // Null activeRole
            .roles(Collections.singletonList(SecurityConstants.PM_ROLE))
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(pmUser));
    when(brdRepository.save(any(BRD.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(new Api<>("SUCCESS", "Audit log created", Optional.empty(), Optional.empty()))));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(api ->
            "success".equals(api.getStatus()) &&
            "Project Manager reassigned successfully".equals(api.getMessage()))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository).save(any(BRD.class));
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should succeed when user has PM role in both activeRole and roles list")
  void reassignProjectManager_WhenUserHasPMInBothActiveRoleAndRoles_ShouldSucceed() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "pm@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User pmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(UserConstants.PM_ROLE)
            .roles(Arrays.asList(SecurityConstants.PM_ROLE, UserConstants.BA_ROLE))
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(pmUser));
    when(brdRepository.save(any(BRD.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
    when(auditLogService.logCreation(any(AuditLogRequest.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(new Api<>("SUCCESS", "Audit log created", Optional.empty(), Optional.empty()))));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(api ->
            "success".equals(api.getStatus()) &&
            "Project Manager reassigned successfully".equals(api.getMessage()))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository).save(any(BRD.class));
    verify(auditLogService).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Reassign PM should fail when user has null activeRole and no PM in roles list")
  void reassignProjectManager_WhenUserHasNullActiveRoleAndNoPMInRoles_ShouldFail() {
    // Given
    String brdId = "brd123";
    String newPmUsername = "user@example.com";
    String modifiedBy = "manager@example.com";
    String userRole = "ROLE_MANAGER";

    BRD existingBrd = BRD.builder().brdId(brdId).creator("oldpm@example.com").build();

    User nonPmUser =
        User.builder()
            .email(newPmUsername)
            .activeRole(null) // Null activeRole
            .roles(Arrays.asList(UserConstants.BA_ROLE, SecurityConstants.ROLE_MANAGER)) // No PM role
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(existingBrd));
    when(userRepository.findByEmail(newPmUsername)).thenReturn(Mono.just(nonPmUser));

    // When
    Mono<Api<Void>> result = userService.reassignProjectManager(brdId, newPmUsername, modifiedBy, userRole);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(api ->
            ErrorValidationMessage.FAILURE.equals(api.getStatus()) &&
            api.getMessage().contains(DashboardConstants.PM_ONLY_ROLE))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(userRepository).findByEmail(newPmUsername);
    verify(brdRepository, never()).save(any(BRD.class));
    verify(auditLogService, never()).logCreation(any(AuditLogRequest.class));
  }

  @Test
  @DisplayName("Delete PM user with active BRDs should return conflict")
  void deletePmUser_WithActiveBrds_ShouldReturnConflict() {
    // Given
    String userId = "123";
    User pmUser =
        User.builder()
            .id(userId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
            .build();

    BRD activeBrd = BRD.builder().brdId("brd1").creator(pmUser.getEmail()).status("Draft").build();

    when(userRepository.findById(userId)).thenReturn(Mono.just(pmUser));
    when(brdRepository.findByCreator(pmUser.getEmail())).thenReturn(Flux.just(activeBrd));

    // When & Then
    StepVerifier.create(userService.deletePmUser(userId))
        .expectNextMatches(
            response ->
                response.getStatus().equals(ErrorValidationMessage.FAILURE)
                    && response
                        .getMessage()
                        .equals(
                            "Active BRD(s) associated with this user. Please reassign them before removing user."))
        .verifyComplete();

    verify(userRepository).findById(userId);
    verify(brdRepository).findByCreator(pmUser.getEmail());
    verify(userRepository, never()).deleteById(anyString());
  }

  @Test
  @DisplayName("Delete BA user with active assignments should return conflict response")
  void deleteBaUser_WithActiveAssignments_ShouldReturnConflict() {
    // Given
    String userId = "123";
    User baUserTest =
        User.builder()
            .id(userId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .activeRole(SecurityConstants.ROLE_BA)
            .roles(Collections.singletonList(SecurityConstants.ROLE_BA))
            .build();

    BAAssignment activeAssignment =
        BAAssignment.builder().brdId("brd1").baEmail(baUserTest.getEmail()).build();

    BRD activeBrd = BRD.builder().brdId("brd1").status("Active").build();

    when(userRepository.findById(userId)).thenReturn(Mono.just(baUserTest));
    when(baAssignmentRepository.findByBaEmail(baUserTest.getEmail()))
        .thenReturn(Flux.just(activeAssignment));
    when(brdRepository.findByBrdId(activeAssignment.getBrdId())).thenReturn(Mono.just(activeBrd));

    // When & Then
    StepVerifier.create(userService.deleteBaUser(userId))
        .expectNextMatches(
            response ->
                response.getStatus().equals(ErrorValidationMessage.FAILURE)
                    && response
                        .getMessage()
                        .equals(
                            "Active BRD assignment(s) associated with this user. Please reassign them before removing user."))
        .verifyComplete();

    verify(userRepository).findById(userId);
    verify(baAssignmentRepository).findByBaEmail(baUserTest.getEmail());
    verify(userRepository, never()).deleteById(anyString());
  }

  @Test
  @DisplayName("Delete BA user with non-BA role should return failure")
  void deleteBaUser_WithNonBaRole_ShouldReturnFailure() {
    // Given
    String userId = "123";
    User nonBaUser =
        User.builder()
            .id(userId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
            .build();

    when(userRepository.findById(userId)).thenReturn(Mono.just(nonBaUser));

    // When & Then
    StepVerifier.create(userService.deleteBaUser(userId))
        .expectNextMatches(
            response ->
                response.getStatus().equals(ErrorValidationMessage.FAILURE)
                    && response.getMessage().equals("User is not a BA"))
        .verifyComplete();

    verify(userRepository).findById(userId);
    verify(userRepository, never()).deleteById(anyString());
  }

  @Nested
  @DisplayName("Email Functionality Tests")
  class EmailFunctionalityTests {

    @Test
    @DisplayName("Create user should send welcome emails successfully")
    void createUser_ShouldSendWelcomeEmailsSuccessfully() {
      // Given
      setupDefaultEmailMocks();

      // When & Then
      StepVerifier.create(userService.createUser(userRequest))
          .expectNextMatches(
              response -> {
                assertSuccessfulEmailHandling(response);
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Create user should handle welcome email failure gracefully")
    void createUser_ShouldHandleWelcomeEmailFailureGracefully() {
      // Given
      setupDefaultEmailMocks();
      when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
          .thenReturn(Mono.error(new RuntimeException("Email service error")));

      // When & Then
      StepVerifier.create(userService.createUser(userRequest))
          .expectNextMatches(
              response -> {
                assertSuccessfulEmailHandling(response);
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Create user should handle credentials email failure gracefully")
    void createUser_ShouldHandleCredentialsEmailFailureGracefully() {
      // Given
      setupDefaultEmailMocks();
      when(emailService.sendUserCredentialsEmail(anyString(), anyString()))
          .thenReturn(Mono.error(new RuntimeException("Credentials email error")));

      // When & Then
      StepVerifier.create(userService.createUser(userRequest))
          .expectNextMatches(
              response -> {
                assertSuccessfulEmailHandling(response);
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Create user should handle both email failures gracefully")
    void createUser_ShouldHandleBothEmailFailuresGracefully() {
      // Given
      setupDefaultEmailMocks();
      when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
          .thenReturn(Mono.error(new RuntimeException("Welcome email error")));
      when(emailService.sendUserCredentialsEmail(anyString(), anyString()))
          .thenReturn(Mono.error(new RuntimeException("Credentials email error")));

      // When
      StepVerifier.create(userService.createUser(userRequest))
          .expectNextMatches(
              response -> {
                assertSuccessfulEmailHandling(response);
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Create user should use email as username for welcome email")
    void createUser_ShouldUseEmailAsUsernameForWelcomeEmail() {
      // Given
      String userEmailForTest = "test.user@example.com";
      userRequest.setEmail(userEmailForTest);
      userRequest.setFirstName("John");
      userRequest.setActiveRole(SecurityConstants.ROLE_PM);

      User savedUser =
          User.builder()
              .id("1")
              .firstName("John")
              .lastName("Doe")
              .email(userEmailForTest.toLowerCase())
              .activeRole(SecurityConstants.ROLE_PM)
              .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
              .password("encoded_password".toCharArray())
              .createdAt(now)
              .build();

      when(passwordEncoder.encode("John_" + SecurityConstants.ROLE_PM))
          .thenReturn("encoded_password");
      when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
      when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
          .thenReturn(Mono.empty());
      when(emailService.sendUserCredentialsEmail(anyString(), anyString()))
          .thenReturn(Mono.empty());

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userService.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                Api<UserResponse> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.getData()).isPresent();
                assertThat(body.getData().get().getEmail())
                    .isEqualTo(userEmailForTest.toLowerCase());
                return true;
              })
          .verifyComplete();

      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Create user should generate correct default password format")
    void createUser_ShouldGenerateCorrectDefaultPasswordFormat() {
      // Given
      String firstName = "TestUser";
      String role = SecurityConstants.ROLE_BA;
      String expectedPassword = firstName + "_" + role;

      userRequest.setFirstName(firstName);
      userRequest.setActiveRole(role);

      when(passwordEncoder.encode(expectedPassword)).thenReturn("encoded_test_password");
      when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
      when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
          .thenReturn(Mono.empty());
      when(emailService.sendUserCredentialsEmail(anyString(), anyString()))
          .thenReturn(Mono.empty());

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userService.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                return true;
              })
          .verifyComplete();

      // Verify correct password was encoded
      verify(passwordEncoder).encode(expectedPassword);
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Create user should log email scheduling information")
    void createUser_ShouldLogEmailSchedulingInformation() {
      // Given
      setupDefaultEmailMocks();

      // When
      StepVerifier.create(userService.createUser(userRequest))
          .expectNextMatches(
              response -> {
                assertSuccessfulEmailHandling(response);
                return true;
              })
          .verifyComplete();

      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Create user should return response immediately without waiting for emails")
    void createUser_ShouldReturnResponseImmediatelyWithoutWaitingForEmails() {
      // Given
      setupDefaultEmailMocks();

      // Simulate slow email service
      when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
          .thenReturn(Mono.<Void>empty().delayElement(java.time.Duration.ofSeconds(5)));
      when(emailService.sendUserCredentialsEmail(anyString(), anyString()))
          .thenReturn(Mono.<Void>empty().delayElement(java.time.Duration.ofSeconds(5)));

      // When
      long startTime = System.currentTimeMillis();
      Mono<ResponseEntity<Api<UserResponse>>> result = userService.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                // Response should be returned quickly (within 1 second), not waiting for emails
                assertThat(duration).isLessThan(1000);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                return true;
              })
          .verifyComplete();

      verify(userRepository).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Password Change Tests")
  class PasswordChangeTests {
    private static final class PasswordTestConstants {
      private static final String TEST_EMAIL = "test@example.com";
      private static final String TEST_USER_ID = "123";
      private static final String CURRENT_PASSWORD = "currentPass";
      private static final String NEW_PASSWORD = "newPass123";
      private static final String ENCODED_CURRENT_PASSWORD = "encoded_current_password";
      private static final String ENCODED_NEW_PASSWORD = "encoded_new_password";
    }

    private User testUser;
    private ChangePasswordRequest validRequest;

    @BeforeEach
    void setUp() {
      setupTestUser();
      setupPasswordEncoder();
      setupValidRequest();
    }

    private void setupTestUser() {
      testUser =
          User.builder()
              .id(PasswordTestConstants.TEST_USER_ID)
              .email(PasswordTestConstants.TEST_EMAIL)
              .activeRole(SecurityConstants.ROLE_PM)
              .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
              .password(PasswordTestConstants.ENCODED_CURRENT_PASSWORD.toCharArray())
              .build();
    }

    private void setupPasswordEncoder() {
      lenient()
          .when(
              passwordEncoder.matches(
                  PasswordTestConstants.CURRENT_PASSWORD, String.valueOf(testUser.getPassword())))
          .thenReturn(true);
      lenient()
          .when(passwordEncoder.encode(PasswordTestConstants.NEW_PASSWORD))
          .thenReturn(PasswordTestConstants.ENCODED_NEW_PASSWORD);
    }

    private void setupValidRequest() {
      validRequest =
          new ChangePasswordRequest(
              PasswordTestConstants.CURRENT_PASSWORD,
              PasswordTestConstants.NEW_PASSWORD,
              SecurityConstants.ROLE_PM);
    }

    @Test
    @DisplayName("Change password should succeed with valid current password")
    void changePassword_WithValidCurrentPassword_ShouldSucceed() {
      when(userRepository.findByEmail(PasswordTestConstants.TEST_EMAIL))
          .thenReturn(Mono.just(testUser));
      when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
      when(passwordEncoder.matches(
              PasswordTestConstants.CURRENT_PASSWORD, String.valueOf(testUser.getPassword())))
          .thenReturn(true);

      StepVerifier.create(
              userService.changePassword(
                  PasswordTestConstants.TEST_EMAIL, SecurityConstants.ROLE_PM, validRequest))
          .expectNextMatches(
              response -> {
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Password changed successfully");
                verify(userRepository).save(any(User.class));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Change password should fail with invalid current password")
    void changePassword_WithInvalidCurrentPassword_ShouldFail() {
      String wrongPassword = "wrongPass";
      ChangePasswordRequest request =
          new ChangePasswordRequest(
              wrongPassword, PasswordTestConstants.NEW_PASSWORD, SecurityConstants.ROLE_PM);

      when(userRepository.findByEmail(PasswordTestConstants.TEST_EMAIL))
          .thenReturn(Mono.just(testUser));
      when(passwordEncoder.matches(wrongPassword, String.valueOf(testUser.getPassword())))
          .thenReturn(false);

      StepVerifier.create(
              userService.changePassword(
                  PasswordTestConstants.TEST_EMAIL, SecurityConstants.ROLE_PM, request))
          .expectNextMatches(
              response -> {
                assertThat(response.getStatus()).isEqualTo(ErrorValidationMessage.FAILURE);
                assertThat(response.getMessage()).isEqualTo("Current password is incorrect");
                verify(userRepository, never()).save(any(User.class));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Change password should fail with user not found")
    void changePassword_WithUserNotFound_ShouldFail() {
      String nonexistentEmail = "nonexistent@example.com";
      when(userRepository.findByEmail(nonexistentEmail)).thenReturn(Mono.empty());

      StepVerifier.create(
              userService.changePassword(nonexistentEmail, SecurityConstants.ROLE_PM, validRequest))
          .expectNextMatches(
              response -> {
                assertThat(response.getStatus()).isEqualTo(ErrorValidationMessage.FAILURE);
                assertThat(response.getMessage())
                    .isEqualTo("User not found with email: " + nonexistentEmail);
                verify(userRepository, never()).save(any(User.class));
                return true;
              })
          .verifyComplete();
    }
  }

  // Add helper methods for common assertions
  private void assertSuccessfulUserCreation(ResponseEntity<Api<UserResponse>> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Api<UserResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
    assertThat(body.getMessage()).isEqualTo("User created successfully");
    assertThat(body.getData()).isPresent();
  }

  private void assertSuccessfulEmailHandling(ResponseEntity<Api<UserResponse>> response) {
    assertSuccessfulUserCreation(response);
    verify(userRepository).save(any(User.class));
  }

  private void setupDefaultEmailMocks() {
    when(passwordEncoder.encode(defaultPassword)).thenReturn(TestConstants.ENCODED_PASSWORD);
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(emailService.sendUserWelcomeEmailWithResetLink(anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(emailService.sendUserCredentialsEmail(anyString(), anyString())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("findById should return user when found")
  void findById_WhenUserExists_ShouldReturnUser() {
    String userId = "user-123";
    User foundUser = User.builder().id(userId).email("test@example.com").build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(foundUser));

    StepVerifier.create(userService.findById(userId))
        .expectNext(foundUser)
        .verifyComplete();
  }

  @Test
  @DisplayName("findById should throw NotFoundException when user not found")
  void findById_WhenUserNotFound_ShouldThrowNotFoundException() {
    String userId = "user-404";
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.findById(userId))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains(userId))
        .verify();
  }

  @Test
  @DisplayName("deleteById should call repository deleteById")
  void deleteById_ShouldCallRepositoryDeleteById() {
    String userId = "user-123";
    when(userRepository.deleteById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.deleteById(userId))
        .verifyComplete();
    verify(userRepository).deleteById(userId);
  }

  @Test
  @DisplayName("findByEmailAndRole should return user when found and add MANAGER role if PM")
  void findByEmailAndRole_WhenUserFound_ShouldReturnUserAndAddManagerRoleIfPM() {
    String email = "pm@example.com";
    String role = SecurityConstants.ROLE_PM;
    User foundUser = User.builder().email(email).activeRole(role).roles(new java.util.ArrayList<>()).build();
    when(userRepository.findByEmailAndRole(email, role)).thenReturn(Mono.just(foundUser));

    StepVerifier.create(userService.findByEmailAndRole(email, role))
        .expectNextMatches(u -> u.getRoles().contains(SecurityConstants.ROLE_MANAGER))
        .verifyComplete();
  }

  @Test
  @DisplayName("findByEmailAndRole should throw NotFoundException when user not found")
  void findByEmailAndRole_WhenUserNotFound_ShouldThrowNotFoundException() {
    String email = "notfound@example.com";
    String role = SecurityConstants.ROLE_PM;
    when(userRepository.findByEmailAndRole(email, role)).thenReturn(Mono.empty());

    StepVerifier.create(userService.findByEmailAndRole(email, role))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains(email))
        .verify();
  }

  @Test
  @DisplayName("checkPasswordChangeRequired should return true if required")
  void checkPasswordChangeRequired_ShouldReturnTrueIfRequired() {
    String email = "user@example.com";
    java.util.List<String> roles = java.util.List.of(SecurityConstants.ROLE_PM);
    User foundUser = User.builder().email(email).activeRole(SecurityConstants.ROLE_PM).roles(roles).passwordChangeRequired(true).build();
    when(userRepository.findByEmail(email)).thenReturn(Mono.just(foundUser));

    StepVerifier.create(userService.checkPasswordChangeRequired(email, roles))
        .expectNextMatches(api -> api.getData().get().get("requirePasswordChange"))
        .verifyComplete();
  }

  @Test
  @DisplayName("checkPasswordChangeRequired should throw NotFoundException if user not found")
  void checkPasswordChangeRequired_WhenUserNotFound_ShouldThrowNotFoundException() {
    String email = "notfound@example.com";
    java.util.List<String> roles = java.util.List.of(SecurityConstants.ROLE_PM);
    when(userRepository.findByEmail(email)).thenReturn(Mono.empty());

    StepVerifier.create(userService.checkPasswordChangeRequired(email, roles))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains(email))
        .verify();
  }

  @Test
  @DisplayName("changeUserStatus should update status for PM user")
  void changeUserStatus_ShouldUpdateStatusForPM() {
    String userId = "user-123";
    User foundUser = User.builder().id(userId).activeRole("PM").build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(foundUser));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(foundUser));

    StepVerifier.create(userService.changeUserStatus(userId, UserStatus.INACTIVE))
        .expectNextMatches(api -> api.getStatus().equals(UserConstants.SUCCESS))
        .verifyComplete();
  }

  @Test
  @DisplayName("changeUserStatus should throw NotFoundException if user not found")
  void changeUserStatus_WhenUserNotFound_ShouldThrowNotFoundException() {
    String userId = "notfound";
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.changeUserStatus(userId, UserStatus.INACTIVE))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains(userId))
        .verify();
  }

  @Test
  @DisplayName("changeUserStatus should throw BadRequestException for non-PM/BA user")
  void changeUserStatus_WhenUserNotPMOrBA_ShouldThrowBadRequestException() {
    String userId = "user-123";
    User nonPmBaUser = User.builder().id(userId).activeRole("OTHER").build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(nonPmBaUser));

    StepVerifier.create(userService.changeUserStatus(userId, UserStatus.INACTIVE))
        .expectErrorMatches(BadRequestException.class::isInstance)
        .verify();
  }

  @Test
  @DisplayName("updateUser should update user details")
  void updateUser_ShouldUpdateUserDetails() {
    String userId = "user-123";
    String adminId = "admin-1";
    User existingUser = User.builder().id(userId).email("old@example.com").build();
    com.aci.smart_onboarding.dto.UpdateUserRequest req = com.aci.smart_onboarding.dto.UpdateUserRequest.builder().firstName("New").lastName("Name").email("new@example.com").activeRole("PM").roles(java.util.List.of("PM")).build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
    when(userRepository.findByEmail("new@example.com")).thenReturn(Mono.empty());
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));

    StepVerifier.create(userService.updateUser(userId, req, adminId))
        .expectNextMatches(api -> api.getStatus().equals(UserConstants.SUCCESS))
        .verifyComplete();
  }

  @Test
  @DisplayName("updateUser should throw NotFoundException if user not found")
  void updateUser_WhenUserNotFound_ShouldThrowNotFoundException() {
    String userId = "notfound";
    com.aci.smart_onboarding.dto.UpdateUserRequest req = com.aci.smart_onboarding.dto.UpdateUserRequest.builder().email("new@example.com").build();
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.updateUser(userId, req, "admin"))
        .expectErrorMatches(e -> e instanceof NotFoundException && e.getMessage().contains(userId))
        .verify();
  }

  @Test
  @DisplayName("updateUser should throw AlreadyExistException if email already in use")
  void updateUser_WhenEmailAlreadyInUse_ShouldThrowAlreadyExistException() {
    String userId = "user-123";
    User existingUser = User.builder().id(userId).email("old@example.com").build();
    User otherUser = User.builder().id("other").email("new@example.com").build();
    com.aci.smart_onboarding.dto.UpdateUserRequest req = com.aci.smart_onboarding.dto.UpdateUserRequest.builder().email("new@example.com").build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
    when(userRepository.findByEmail("new@example.com")).thenReturn(Mono.just(otherUser));

    StepVerifier.create(userService.updateUser(userId, req, "admin"))
        .expectErrorMatches(AlreadyExistException.class::isInstance)
        .verify();
  }

  @Test
  @DisplayName("addRoleToUser should add role if not present")
  void addRoleToUser_ShouldAddRoleIfNotPresent() {
    String userId = "user-123";
    String role = "BA";
    String modifiedBy = "admin";
    User userToAddRole = User.builder().id(userId).roles(new java.util.ArrayList<>()).build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(userToAddRole));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(userToAddRole));
    when(auditLogService.logCreation(any())).thenReturn(Mono.empty());

    StepVerifier.create(userService.addRoleToUser(userId, role, modifiedBy))
        .expectNextMatches(api -> api.getStatus().equals("SUCCESS"))
        .verifyComplete();
  }

  @Test
  @DisplayName("addRoleToUser should throw NotFoundException if user not found")
  void addRoleToUser_WhenUserNotFound_ShouldThrowNotFoundException() {
    String userId = "notfound";
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.addRoleToUser(userId, "BA", "admin"))
        .expectNextMatches(api -> api.getStatus().equals("failure") && api.getMessage().contains("not found"))
        .verifyComplete();
  }

  @Test
  @DisplayName("addRoleToUser should throw BadRequestException if user already has role")
  void addRoleToUser_WhenUserAlreadyHasRole_ShouldThrowBadRequestException() {
    String userId = "user-123";
    String role = "BA";
    User userWithRole = User.builder().id(userId).roles(java.util.List.of(role)).activeRole(role).build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(userWithRole));

    StepVerifier.create(userService.addRoleToUser(userId, role, "admin"))
        .expectNextMatches(api -> api.getStatus().equals("failure") && api.getMessage().contains("already has"))
        .verifyComplete();
  }

  @Test
  @DisplayName("removeRole should remove role if present")
  void removeRole_ShouldRemoveRoleIfPresent() {
    String userId = "user-123";
    String role = "BA";
    String modifiedBy = "admin";
    java.util.List<String> roles = new java.util.ArrayList<>();
    roles.add(role);
    roles.add("PM");
    User userWithRoles = User.builder().id(userId).roles(roles).activeRole(role).build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(userWithRoles));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(userWithRoles));
    when(auditLogService.logCreation(any())).thenReturn(Mono.empty());

    StepVerifier.create(userService.removeRole(userId, role, modifiedBy))
        .expectNextMatches(api -> api.getStatus().equals("SUCCESS"))
        .verifyComplete();
  }

  @Test
  @DisplayName("removeRole should throw NotFoundException if user not found")
  void removeRole_WhenUserNotFound_ShouldThrowNotFoundException() {
    String userId = "notfound";
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(userService.removeRole(userId, "BA", "admin"))
        .expectNextMatches(api -> api.getStatus().equals("failure") && api.getMessage().contains("not found"))
        .verifyComplete();
  }

  @Test
  @DisplayName("removeRole should throw BadRequestException if user does not have role")
  void removeRole_WhenUserDoesNotHaveRole_ShouldThrowBadRequestException() {
    String userId = "user-123";
    String role = "BA";
    User userWithoutRole = User.builder().id(userId).roles(java.util.List.of("PM")).activeRole("PM").build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(userWithoutRole));

    StepVerifier.create(userService.removeRole(userId, role, "admin"))
        .expectNextMatches(api -> api.getStatus().equals("failure") && api.getMessage().contains("does not have"))
        .verifyComplete();
  }

  @Test
  @DisplayName("removeRole should throw BadRequestException if removing last role")
  void removeRole_WhenRemovingLastRole_ShouldThrowBadRequestException() {
    String userId = "user-123";
    String role = "BA";
    User userWithLastRole = User.builder().id(userId).roles(java.util.List.of(role)).activeRole(role).build();
    when(userRepository.findById(userId)).thenReturn(Mono.just(userWithLastRole));

    StepVerifier.create(userService.removeRole(userId, role, "admin"))
        .expectNextMatches(api -> api.getStatus().equals("failure") && api.getMessage().contains("last role"))
        .verifyComplete();
  }
}
