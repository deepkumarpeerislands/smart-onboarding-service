package com.aci.smart_onboarding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ChangePasswordRequest;
import com.aci.smart_onboarding.dto.ChangeUserStatusRequest;
import com.aci.smart_onboarding.dto.PMReassignmentRequest;
import com.aci.smart_onboarding.dto.PMReassignmentResponse;
import com.aci.smart_onboarding.dto.UpdateUserRequest;
import com.aci.smart_onboarding.dto.UserDetailResponse;
import com.aci.smart_onboarding.dto.UserListResponse;
import com.aci.smart_onboarding.dto.UserRequest;
import com.aci.smart_onboarding.dto.UserResponse;
import com.aci.smart_onboarding.enums.UserStatus;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private IUserService userService;

  @Mock private BRDSecurityService securityService;
  @Mock private BRDRepository brdRepository;
  @Mock private UserRepository userRepository;
  @InjectMocks private UserController userController;

  private UserRequest userRequest;
  private UserResponse userResponse;
  private UserDetailResponse userDetailResponse;
  private UserListResponse userListResponse;
  private Api<UserListResponse> apiResponse;
  private LocalDateTime now;
  private final String testEmail = "john.doe@example.com";
  private final String testUsername = "testuser";

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    // Setup test data
    userRequest =
        UserRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email(testEmail)
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
            .build();

    userResponse =
        UserResponse.builder()
            .id("1")
            .firstName("John")
            .lastName("Doe")
            .email(testEmail)
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
            .createdAt(now)
            .build();

    userDetailResponse =
        UserDetailResponse.builder()
            .id("1")
            .firstName("John")
            .lastName("Doe")
            .username(testEmail)
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
            .dateCreated(now)
            .dateLastModified(null)
            .build();

    // Setup search test data
    UserListResponse.UserInfo pmUserInfo =
        UserListResponse.UserInfo.builder()
            .id("1")
            .fullName("John Doe")
            .email("john.doe@example.com")
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
            .createdAt(now)
            .build();

    UserListResponse.UserInfo baUserInfo =
        UserListResponse.UserInfo.builder()
            .id("2")
            .fullName("Jane Smith")
            .email("jane.smith@example.com")
            .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA)
            .roles(
                Collections.singletonList(
                    SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA))
            .createdAt(now)
            .build();

    userListResponse =
        UserListResponse.builder()
            .pmUsers(Collections.singletonList(pmUserInfo))
            .baUsers(Collections.singletonList(baUserInfo))
            .build();

    apiResponse = new Api<>();
    apiResponse.setStatus(UserConstants.SUCCESS);
    apiResponse.setMessage(UserConstants.USERS_RETRIEVED);
    apiResponse.setData(Optional.of(userListResponse));
    apiResponse.setErrors(Optional.empty());
  }

  private SecurityContext setupMockSecurityContext() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);

    // Explicitly make getName() return the test username
    lenient().when(authentication.getName()).thenReturn(testUsername);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

    return securityContext;
  }

  @Test
  @DisplayName("Create user with MANAGER role should return created user")
  void createUser_WithManagerRole_ShouldReturnCreatedUser() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock service calls
      Api<UserResponse> userCreationResponse = new Api<>();
      userCreationResponse.setStatus(UserConstants.SUCCESS);
      userCreationResponse.setMessage("User created successfully");
      userCreationResponse.setData(Optional.of(userResponse));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.createUser(any(UserRequest.class)))
          .thenReturn(
              Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(userCreationResponse)));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.CREATED
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().equals(userResponse))
          .verifyComplete();

      verify(userService).createUser(any(UserRequest.class));
    }
  }

  @Test
  @DisplayName("Create user without MANAGER role should throw AccessDeniedException")
  void createUser_WithoutManagerRole_ShouldThrowAccessDeniedException() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

      // Then
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
    }
  }

  @Test
  @DisplayName("Create user with security context error should propagate error")
  void createUser_WithSecurityContextError_ShouldPropagateError() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Use lenient to avoid unnecessary stubbing errors
      lenient()
          .when(securityService.getCurrentUserRole())
          .thenReturn(Mono.error(new RuntimeException("Security context error")));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

      // Then
      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Test
  @DisplayName("Create user should return success response")
  void createUser_ShouldReturnSuccessResponse() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));

      UserRequest userRequestTest =
          UserRequest.builder()
              .firstName("John")
              .lastName("Doe")
              .email("john.doe@example.com")
              .activeRole(SecurityConstants.ROLE_PM)
              .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
              .build();

      UserResponse userResponseTest =
          UserResponse.builder()
              .firstName("John")
              .lastName("Doe")
              .email("john.doe@example.com")
              .activeRole(SecurityConstants.ROLE_PM)
              .roles(Collections.singletonList(SecurityConstants.ROLE_PM))
              .build();

      Api<UserResponse> apiResponseTest =
          new Api<>(
              UserConstants.SUCCESS,
              "User created successfully",
              Optional.of(userResponseTest),
              Optional.empty());

      when(userService.createUser(any(UserRequest.class)))
          .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(apiResponseTest)));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequestTest);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                Api<UserResponse> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
                assertThat(body.getMessage()).isEqualTo("User created successfully");
                assertThat(body.getData()).isPresent();
                UserResponse data = body.getData().get();
                assertThat(data.getEmail()).isEqualTo("john.doe@example.com");
                assertThat(data.getActiveRole()).isEqualTo(SecurityConstants.ROLE_PM);
                assertThat(data.getRoles()).contains(SecurityConstants.ROLE_PM);
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Create user with invalid request should return bad request")
  void createUser_WithInvalidRequest_ShouldReturnBadRequest() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
      when(userService.createUser(any(UserRequest.class)))
          .thenReturn(Mono.error(new BadRequestException("Invalid request")));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof BadRequestException
                      && throwable.getMessage().equals("Invalid request"))
          .verify();
    }
  }

  @Test
  @DisplayName("Create user with existing email should return conflict")
  void createUser_WithExistingEmail_ShouldReturnConflict() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
      when(userService.createUser(any(UserRequest.class)))
          .thenReturn(Mono.error(new AlreadyExistException("User already exists")));

      // When
      Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof AlreadyExistException
                      && throwable.getMessage().equals("User already exists"))
          .verify();
    }
  }

  @Test
  @DisplayName("Get user by email with MANAGER role should return user details")
  void getUserByEmail_WithManagerRole_ShouldReturnUserDetails() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      Api<UserDetailResponse> userDetailApiResponse = new Api<>();
      userDetailApiResponse.setStatus(UserConstants.SUCCESS);
      userDetailApiResponse.setMessage("User retrieved successfully");
      userDetailApiResponse.setData(Optional.of(userDetailResponse));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
      when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.just(ResponseEntity.ok(userDetailApiResponse)));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().equals(userDetailResponse))
          .verifyComplete();

      verify(userService).getUserByEmail(testEmail);
    }
  }

  @Test
  @DisplayName("Get user by email without MANAGER role should throw AccessDeniedException")
  void getUserByEmail_WithoutManagerRole_ShouldThrowAccessDeniedException() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
    }
  }

  @Test
  @DisplayName("Get user by email with security context error should propagate error")
  void getUserByEmail_WithSecurityContextError_ShouldPropagateError() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Use lenient to avoid unnecessary stubbing errors
      lenient()
          .when(securityService.getCurrentUserRole())
          .thenReturn(Mono.error(new RuntimeException("Security context error")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Test
  @DisplayName("Get user by email with service error should propagate error")
  void getUserByEmail_WithServiceError_ShouldPropagateError() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Use lenient to avoid unnecessary stubbing errors
      lenient()
          .when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      lenient()
          .when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.error(new RuntimeException("Service error")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Test
  @DisplayName("Get user by email should return user details")
  void getUserByEmail_ShouldReturnUserDetails() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      Api<UserDetailResponse> userDetailApiResponse = new Api<>();
      userDetailApiResponse.setStatus(UserConstants.SUCCESS);
      userDetailApiResponse.setMessage("User retrieved successfully");
      userDetailApiResponse.setData(Optional.of(userDetailResponse));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.just(ResponseEntity.ok(userDetailApiResponse)));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Api<UserDetailResponse> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
                assertThat(body.getData()).isPresent();
                assertThat(body.getData().get().getUsername()).isEqualTo(testEmail);
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Get user by email with non-existent email should return not found")
  void getUserByEmail_WithNonExistentEmail_ShouldReturnNotFound() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.error(new NotFoundException("User not found")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof NotFoundException
                      && throwable.getMessage().equals("User not found"))
          .verify();
    }
  }

  @Test
  @DisplayName("Get user by email with invalid email should return bad request")
  void getUserByEmail_WithInvalidEmail_ShouldReturnBadRequest() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.error(new BadRequestException("Invalid email")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail("invalid@email");

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof BadRequestException
                      && throwable.getMessage().equals("Invalid email"))
          .verify();
    }
  }

  @Test
  @DisplayName("Get user by email with server error should return internal server error")
  void getUserByEmail_WithServerError_ShouldReturnInternalServerError() {
    // Given
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with proper authentication
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUserByEmail(anyString()))
          .thenReturn(Mono.error(new RuntimeException("Server error")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.getUserByEmail(testEmail);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable.getMessage().equals("Server error"))
          .verify();
    }
  }

  @Nested
  @DisplayName("Search PM and BA Users Tests")
  class SearchPMAndBAUsersTests {

    @Test
    @DisplayName("Search with valid term and MANAGER role should return filtered users")
    void searchPMAndBAUsers_WithValidTermAndManagerRole_ShouldReturnFilteredUsers() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.searchPMAndBAUsers("John"))
            .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result =
            userController.searchPMAndBAUsers("John");

        // Then
        StepVerifier.create(result)
            .expectNextMatches(
                response ->
                    response.getStatusCode() == HttpStatus.OK
                        && response.getBody().getData().isPresent()
                        && response.getBody().getData().get().equals(userListResponse))
            .verifyComplete();

        verify(userService).searchPMAndBAUsers("John");
      }
    }

    @Test
    @DisplayName("Search with empty term should return all users")
    void searchPMAndBAUsers_WithEmptyTerm_ShouldReturnAllUsers() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.searchPMAndBAUsers(null))
            .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result =
            userController.searchPMAndBAUsers(null);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(
                response ->
                    response.getStatusCode() == HttpStatus.OK
                        && response.getBody().getData().isPresent())
            .verifyComplete();
      }
    }

    @Test
    @DisplayName("Search without MANAGER role should throw AccessDeniedException")
    void searchPMAndBAUsers_WithoutManagerRole_ShouldThrowAccessDeniedException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result =
            userController.searchPMAndBAUsers("John");

        // Then
        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("Search with service error should propagate error")
    void searchPMAndBAUsers_WithServiceError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.searchPMAndBAUsers(anyString()))
            .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result =
            userController.searchPMAndBAUsers("John");

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }
  }

  @Nested
  @DisplayName("Get PM and BA Users Tests")
  class GetPMAndBAUsersTests {

    @Test
    @DisplayName("Get all PM and BA users with MANAGER role should return users")
    void getPMAndBAUsers_WithManagerRole_ShouldReturnUsers() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.getPMAndBAUsers()).thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result = userController.getPMAndBAUsers();

        // Then
        StepVerifier.create(result)
            .expectNextMatches(
                response ->
                    response.getStatusCode() == HttpStatus.OK
                        && response.getBody().getData().isPresent()
                        && response.getBody().getData().get().equals(userListResponse))
            .verifyComplete();

        verify(userService).getPMAndBAUsers();
      }
    }

    @Test
    @DisplayName("Get all PM and BA users without MANAGER role should throw AccessDeniedException")
    void getPMAndBAUsers_WithoutManagerRole_ShouldThrowAccessDeniedException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result = userController.getPMAndBAUsers();

        // Then
        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("Get all PM and BA users with service error should propagate error")
    void getPMAndBAUsers_WithServiceError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.getPMAndBAUsers())
            .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result = userController.getPMAndBAUsers();

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }
  }

  @Nested
  @DisplayName("Authorization Tests")
  class AuthorizationTests {
    @Mock private SecurityContext mockContext;
    @Mock private Authentication mockAuthentication;

    @Test
    @DisplayName("Create user with MANAGER role should succeed")
    void createUser_WithManagerRole_ShouldSucceed() {
      // Given
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Mock security context with proper authentication
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        Api<UserResponse> userCreationResponse = new Api<>();
        userCreationResponse.setStatus(UserConstants.SUCCESS);
        userCreationResponse.setMessage("User created successfully");
        userCreationResponse.setData(Optional.of(userResponse));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.createUser(any(UserRequest.class)))
            .thenReturn(
                Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(userCreationResponse)));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(
                response -> {
                  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                  Api<UserResponse> body = response.getBody();
                  assertThat(body).isNotNull();
                  assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
                  assertThat(body.getData()).isPresent();
                  assertThat(body.getData().get().getEmail()).isEqualTo(testEmail);
                  return true;
                })
            .verifyComplete();

        verify(userService).createUser(userRequest);
      }
    }

    @Test
    @DisplayName("Create user without MANAGER role should throw AccessDeniedException")
    void createUser_WithoutManagerRole_ShouldThrowAccessDeniedException() {
      // Given
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Mock security context with proper authentication
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then
        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("Get user by email with MANAGER role should succeed")
    void getUserByEmail_WithManagerRole_ShouldSucceed() {
      // Given
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Mock security context with proper authentication
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        Api<UserDetailResponse> userDetailApiResponse = new Api<>();
        userDetailApiResponse.setStatus(UserConstants.SUCCESS);
        userDetailApiResponse.setMessage("User retrieved successfully");
        userDetailApiResponse.setData(Optional.of(userDetailResponse));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.getUserByEmail(anyString()))
            .thenReturn(Mono.just(ResponseEntity.ok(userDetailApiResponse)));

        // When
        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.getUserByEmail(testEmail);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(
                response -> {
                  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                  Api<UserDetailResponse> body = response.getBody();
                  assertThat(body).isNotNull();
                  assertThat(body.getStatus()).isEqualTo(UserConstants.SUCCESS);
                  assertThat(body.getData()).isPresent();
                  assertThat(body.getData().get().getUsername()).isEqualTo(testEmail);
                  return true;
                })
            .verifyComplete();

        verify(userService).getUserByEmail(testEmail);
      }
    }

    @Test
    @DisplayName("Get user by email without MANAGER role should throw AccessDeniedException")
    void getUserByEmail_WithoutManagerRole_ShouldThrowAccessDeniedException() {
      // Given
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Mock security context with proper authentication
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

        // When
        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.getUserByEmail(testEmail);

        // Then
        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("Security context with null authentication should handle gracefully")
    void securityContext_WithNullAuthentication_ShouldHandleGracefully() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then
        StepVerifier.create(result).expectError(NullPointerException.class).verify();
      }
    }

    @Test
    @DisplayName("Security context error should propagate in createUser")
    void createUser_WithSecurityContextError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.error(new RuntimeException("Security context error")));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }

    @Test
    @DisplayName("Security context error should propagate in getUserByEmail")
    void getUserByEmail_WithSecurityContextError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.error(new RuntimeException("Security context error")));

        // When
        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.getUserByEmail(testEmail);

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }

    @Test
    @DisplayName("Security context error should propagate in getPMAndBAUsers")
    void getPMAndBAUsers_WithSecurityContextError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.error(new RuntimeException("Security context error")));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result = userController.getPMAndBAUsers();

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }

    @Test
    @DisplayName("security context error should propagate in searchPMAndBAUsers")
    void searchPMAndBAUsers_WithSecurityContextError_ShouldPropagateError() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.error(new RuntimeException("Security context error")));

        // When
        Mono<ResponseEntity<Api<UserListResponse>>> result =
            userController.searchPMAndBAUsers("test");

        // Then
        StepVerifier.create(result).expectError(RuntimeException.class).verify();
      }
    }

    @Test
    @DisplayName("getUsernameFromContext should extract username correctly")
    void getUsernameFromContext_ShouldExtractUsernameCorrectly() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.createUser(any(UserRequest.class)))
            .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(new Api<>())));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then - This test ensures getUsernameFromContext is called and works correctly
        StepVerifier.create(result)
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED)
            .verifyComplete();
      }
    }

    @Test
    @DisplayName("Authorization flow should log warning for unauthorized access")
    void authorizationFlow_ShouldLogWarningForUnauthorizedAccess() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then - This ensures the authorization check and logging is covered
        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("Authorization flow should log success for authorized access")
    void authorizationFlow_ShouldLogSuccessForAuthorizedAccess() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        // Given
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));

        Api<UserResponse> userCreationResponse = new Api<>();
        userCreationResponse.setStatus(UserConstants.SUCCESS);
        userCreationResponse.setData(Optional.of(userResponse));

        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
        when(userService.createUser(any(UserRequest.class)))
            .thenReturn(
                Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(userCreationResponse)));

        // When
        Mono<ResponseEntity<Api<UserResponse>>> result = userController.createUser(userRequest);

        // Then - This ensures the success logging is covered
        StepVerifier.create(result)
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED)
            .verifyComplete();
      }
    }
  }

  @Test
  @DisplayName("Get users by role with MANAGER role should return users list")
  void getUsersByRole_WithManagerRole_ShouldReturnUsersList() {
    // Given
    String role = "PM";
    List<UserDetailResponse> expectedUsers =
        Arrays.asList(
            UserDetailResponse.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .username("john.doe@example.com")
                .activeRole(role)
                .dateCreated(now)
                .build(),
            UserDetailResponse.builder()
                .id("2")
                .firstName("Jane")
                .lastName("Smith")
                .username("jane.smith@example.com")
                .activeRole(role)
                .dateCreated(now)
                .build());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      // Mock service calls
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUsersByRole(role)).thenReturn(Flux.fromIterable(expectedUsers));

      // When
      Mono<ResponseEntity<List<UserDetailResponse>>> result = userController.getUsersByRole(role);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                List<UserDetailResponse> actualUsers = response.getBody();
                return response.getStatusCode() == HttpStatus.OK
                    && actualUsers != null
                    && actualUsers.size() == 2
                    && actualUsers.containsAll(expectedUsers);
              })
          .verifyComplete();

      verify(userService).getUsersByRole(role);
    }
  }

  @Test
  @DisplayName("Get users by role without MANAGER role should throw AccessDeniedException")
  void getUsersByRole_WithoutManagerRole_ShouldThrowAccessDeniedException() {
    // Given
    String role = "PM";

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

      // When
      Mono<ResponseEntity<List<UserDetailResponse>>> result = userController.getUsersByRole(role);

      // Then
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
    }
  }

  @Test
  @DisplayName("Get users by role with no users found should return 404")
  void getUsersByRole_WithNoUsersFound_ShouldReturn404() {
    // Given
    String role = "NONEXISTENT_ROLE";

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUsersByRole(role)).thenReturn(Flux.empty());

      // When
      Mono<ResponseEntity<List<UserDetailResponse>>> result = userController.getUsersByRole(role);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
          .verifyComplete();

      verify(userService).getUsersByRole(role);
    }
  }

  @Test
  @DisplayName("Get users by role with security context error should propagate error")
  void getUsersByRole_WithSecurityContextError_ShouldPropagateError() {
    // Given
    String role = "PM";
    RuntimeException securityError = new RuntimeException("Security context error");

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context with error
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.error(securityError));

      // When
      Mono<ResponseEntity<List<UserDetailResponse>>> result = userController.getUsersByRole(role);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable.getMessage().equals("Security context error"))
          .verify();
    }
  }

  @Test
  @DisplayName("Get users by role with service error should propagate error")
  void getUsersByRole_WithServiceError_ShouldPropagateError() {
    // Given
    String role = "PM";
    RuntimeException serviceError = new RuntimeException("Service error");

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.getUsersByRole(role)).thenReturn(Flux.error(serviceError));

      // When
      Mono<ResponseEntity<List<UserDetailResponse>>> result = userController.getUsersByRole(role);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable.getMessage().equals("Service error"))
          .verify();

      verify(userService).getUsersByRole(role);
    }
  }

  @Test
  @DisplayName("Reassign PM should succeed when user has MANAGER role")
  void reassignProjectManager_WhenUserHasManagerRole_ShouldSucceed() {
    // Given
    PMReassignmentRequest request = new PMReassignmentRequest();
    request.setBrdId("brd123");
    request.setNewPmUsername("newpm@example.com");

    Api<Void> successResponse =
        new Api<>(
            DashboardConstants.SUCCESS,
            "Project Manager reassigned successfully",
            Optional.empty(),
            Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER))
          .thenReturn(Mono.just(successResponse));

      // When
      Mono<ResponseEntity<Api<Void>>> result = userController.reassignProjectManager(request);

      // Then
      StepVerifier.create(result).expectNext(ResponseEntity.ok(successResponse)).verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    }
  }

  @Test
  @DisplayName("Reassign PM should return 403 when user is not a MANAGER")
  void reassignProjectManager_WhenUserNotManager_ShouldReturnForbidden() {
    // Given
    PMReassignmentRequest request = new PMReassignmentRequest();
    request.setBrdId("brd123");
    request.setNewPmUsername("newpm@example.com");

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

      // When
      Mono<ResponseEntity<Api<Void>>> result = userController.reassignProjectManager(request);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof AccessDeniedException
                      && DashboardConstants.MANAGER_ONLY_MESSAGE.equals(throwable.getMessage()))
          .verify();

      verify(securityService).getCurrentUserRole();
      verify(userService, never()).reassignProjectManager(anyString(), anyString(), anyString(), anyString());
    }
  }

  @Test
  @DisplayName("Reassign PM should return 404 when BRD not found")
  void reassignProjectManager_WhenBRDNotFound_ShouldReturnNotFound() {
    // Given
    PMReassignmentRequest request = new PMReassignmentRequest();
    request.setBrdId("nonexistent");
    request.setNewPmUsername("newpm@example.com");

    Api<Void> notFoundResponse =
        new Api<>("failure", "BRD not found", Optional.empty(), Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER))
          .thenReturn(Mono.just(notFoundResponse));

      // When
      Mono<ResponseEntity<Api<Void>>> result = userController.reassignProjectManager(request);

      // Then
      StepVerifier.create(result)
          .expectNext(ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    }
  }

  @Test
  @DisplayName("Reassign PM should return 400 when PM username is invalid")
  void reassignProjectManager_WhenPMUsernameInvalid_ShouldReturnBadRequest() {
    // Given
    PMReassignmentRequest request = new PMReassignmentRequest();
    request.setBrdId("brd123");
    request.setNewPmUsername("invalid@example.com");

    Api<Void> badRequestResponse =
        new Api<>(
            "failure",
            "Invalid request",
            Optional.empty(),
            Optional.of(Map.of("error", "User is not a PM")));

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER))
          .thenReturn(Mono.just(badRequestResponse));

      // When
      Mono<ResponseEntity<Api<Void>>> result = userController.reassignProjectManager(request);

      // Then
      StepVerifier.create(result)
          .expectNext(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequestResponse))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).reassignProjectManager(request.getBrdId(), request.getNewPmUsername(), testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    }
  }

  @Test
  @DisplayName("Batch reassign PMs should succeed when user has MANAGER role")
  void reassignProjectManagers_WhenUserHasManagerRole_ShouldSucceed() {
    // Given
    List<PMReassignmentRequest> requests =
        Arrays.asList(
            PMReassignmentRequest.builder()
                .brdId("brd123")
                .newPmUsername("pm1@example.com")
                .build(),
            PMReassignmentRequest.builder()
                .brdId("brd124")
                .newPmUsername("pm2@example.com")
                .build());

    List<PMReassignmentResponse> successResponses =
        Arrays.asList(
            PMReassignmentResponse.builder()
                .brdId("brd123")
                .oldPmUsername("oldpm1@example.com")
                .newPmUsername("pm1@example.com")
                .status("SUCCESS")
                .build(),
            PMReassignmentResponse.builder()
                .brdId("brd124")
                .oldPmUsername("oldpm2@example.com")
                .newPmUsername("pm2@example.com")
                .status("SUCCESS")
                .build());

    Api<List<PMReassignmentResponse>> successResponse =
        new Api<>(
            DashboardConstants.SUCCESS,
            "All PMs reassigned successfully",
            Optional.of(successResponses),
            Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.reassignProjectManagers(requests, testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER)).thenReturn(Mono.just(successResponse));

      // When
      Mono<ResponseEntity<Api<List<PMReassignmentResponse>>>> result =
          userController.reassignProjectManagers(requests);

      // Then
      StepVerifier.create(result).expectNext(ResponseEntity.ok(successResponse)).verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).reassignProjectManagers(requests, testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    }
  }

  @Test
  @DisplayName("Batch reassign PMs should return 403 when user is not a MANAGER")
  void reassignProjectManagers_WhenUserNotManager_ShouldReturnForbidden() {
    // Given
    List<PMReassignmentRequest> requests =
        Arrays.asList(
            PMReassignmentRequest.builder()
                .brdId("brd123")
                .newPmUsername("pm1@example.com")
                .build());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

      // When
      Mono<ResponseEntity<Api<List<PMReassignmentResponse>>>> result =
          userController.reassignProjectManagers(requests);

      // Then
      StepVerifier.create(result)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof AccessDeniedException
                      && DashboardConstants.MANAGER_ONLY_MESSAGE.equals(throwable.getMessage()))
          .verify();

      verify(securityService).getCurrentUserRole();
      verify(userService, never()).reassignProjectManagers(any(), anyString(), anyString());
    }
  }

  @Test
  @DisplayName("Batch reassign PMs should return 400 when some reassignments fail")
  void reassignProjectManagers_WhenSomeReassignmentsFail_ShouldReturnBadRequest() {
    // Given
    List<PMReassignmentRequest> requests =
        Arrays.asList(
            PMReassignmentRequest.builder()
                .brdId("brd123")
                .newPmUsername("pm1@example.com")
                .build(),
            PMReassignmentRequest.builder()
                .brdId("brd124")
                .newPmUsername("invalid@example.com")
                .build());

    List<PMReassignmentResponse> failedResponses =
        Arrays.asList(
            PMReassignmentResponse.builder()
                .brdId("brd124")
                .newPmUsername("invalid@example.com")
                .status("FAILED")
                .reason("User is not a PM")
                .build());

    Api<List<PMReassignmentResponse>> failureResponse =
        new Api<>(
            "failure",
            "Some PM reassignments failed",
            Optional.of(failedResponses),
            Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.reassignProjectManagers(requests, testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER)).thenReturn(Mono.just(failureResponse));

      // When
      Mono<ResponseEntity<Api<List<PMReassignmentResponse>>>> result =
          userController.reassignProjectManagers(requests);

      // Then
      StepVerifier.create(result)
          .expectNext(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failureResponse))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).reassignProjectManagers(requests, testUsername, SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    }
  }

  @Test
  @DisplayName("Delete PM user with active BRDs should return 409 Conflict")
  void deletePmUser_WithActiveBrds_ShouldReturnConflict() {
    // Given
    String userId = "test-user-id";

    Api<String> conflictResponse =
        new Api<>(
            "failure",
            "Active BRD(s) associated with this user. Please reassign them before removing user.",
            Optional.empty(),
            Optional.empty());

    // Mock security context
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      // Mock service calls
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.deletePmUser(userId)).thenReturn(Mono.just(conflictResponse));

      // When
      Mono<ResponseEntity<Api<String>>> result = userController.deletePmUser(userId);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusMatches = response.getStatusCode() == HttpStatus.CONFLICT;
                boolean messageMatches =
                    response.getBody() != null
                        && response
                            .getBody()
                            .getMessage()
                            .equals(
                                "Active BRD(s) associated with this user. Please reassign them before removing user.");
                return statusMatches && messageMatches;
              })
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).deletePmUser(userId);
    }
  }

  @Test
  @DisplayName("Delete BA user with active assignments should return 409 Conflict")
  void deleteBaUser_WithActiveAssignments_ShouldReturnConflict() {
    // Given
    String userId = "test-ba-id";

    Api<String> conflictResponse =
        new Api<>(
            "failure",
            "Active BRD assignment(s) associated with this user. Please reassign them before removing user.",
            Optional.empty(),
            Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      // Mock service calls
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.deleteBaUser(userId)).thenReturn(Mono.just(conflictResponse));

      // When
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusMatches = response.getStatusCode() == HttpStatus.CONFLICT;
                boolean messageMatches =
                    response.getBody() != null
                        && response
                            .getBody()
                            .getMessage()
                            .equals(
                                "Active BRD assignment(s) associated with this user. Please reassign them before removing user.");
                return statusMatches && messageMatches;
              })
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).deleteBaUser(userId);
    }
  }

  @Test
  @DisplayName("Delete BA user without MANAGER role should return 403 Forbidden")
  void deleteBaUser_WithoutManagerRole_ShouldReturnForbidden() {
    // Given
    String userId = "test-ba-id";

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      // Mock service calls
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));

      // When
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);

      // Then
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();

      verify(securityService).getCurrentUserRole();
      verify(userService, never()).deleteBaUser(userId);
    }
  }

  @Test
  @DisplayName("Delete BA user successfully should return 200 OK")
  void deleteBaUser_Successfully_ShouldReturnOk() {
    // Given
    String userId = "test-ba-id";

    Api<String> successResponse =
        new Api<>("success", "BA user removed successfully.", Optional.empty(), Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      // Mock service calls
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deleteBaUser(userId)).thenReturn(Mono.just(successResponse));

      // When
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusMatches = response.getStatusCode() == HttpStatus.OK;
                boolean messageMatches =
                    response.getBody() != null
                        && response.getBody().getMessage().equals("BA user removed successfully.");
                return statusMatches && messageMatches;
              })
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(userService).deleteBaUser(userId);
    }
  }

  @ParameterizedTest
  @CsvSource({"true, true", "false, false"})
  @DisplayName("requirePasswordChange_WhenUserFoundOrNotFound_ReturnsExpected")
  void requirePasswordChange_WhenUserFoundOrNotFound_ReturnsExpected(
      boolean requirePasswordChange, boolean expected) {
    // Given
    String email = "test@example.com";

    Api<Map<String, Boolean>> expectedResponse = new Api<>();
    expectedResponse.setStatus(DashboardConstants.SUCCESS);
    expectedResponse.setMessage("Password change requirement checked successfully");
    expectedResponse.setData(
        Optional.of(Collections.singletonMap("requirePasswordChange", requirePasswordChange)));
    expectedResponse.setErrors(Optional.empty());

    SecurityContext mockSecurityContext = mock(SecurityContext.class);
    Authentication mockAuth = mock(Authentication.class);

    when(mockAuth.getName()).thenReturn(email);
    Collection<? extends GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PM"));
    when(mockAuth.getAuthorities()).thenReturn((Collection) authorities);
    when(mockSecurityContext.getAuthentication()).thenReturn(mockAuth);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockSecurityContext));

      lenient()
          .when(userService.checkPasswordChangeRequired(eq(email), anyList()))
          .thenReturn(Mono.just(expectedResponse));

      // When
      Mono<ResponseEntity<Api<Map<String, Boolean>>>> result =
          userController.requirePasswordChange();

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && response.getBody().getStatus().equals(DashboardConstants.SUCCESS)
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().get("requirePasswordChange")
                          == expected)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("changeUserStatus_shouldAllowManager")
  void changeUserStatus_shouldAllowManager() {
    // Arrange
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder().userId("user123").newStatus(UserStatus.INACTIVE).build();

    UserDetailResponse detailResponse =
        UserDetailResponse.builder().id("user123").status(UserStatus.INACTIVE).build();

    Api<UserDetailResponse> api = new Api<>();
    api.setStatus("success");
    api.setData(java.util.Optional.of(detailResponse));

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("MANAGER"));
      when(userService.changeUserStatus(any(), any())).thenReturn(Mono.just(api));

      // Act & Assert
      StepVerifier.create(userController.changeUserStatus(request))
          .expectNextMatches(
              response ->
                  response.getStatusCode().is2xxSuccessful()
                      && response.getBody().getStatus().equals("success")
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().getStatus() == UserStatus.INACTIVE)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("changeUserStatus_shouldDenyNonManager")
  void changeUserStatus_shouldDenyNonManager() {
    // Arrange
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder().userId("user123").newStatus(UserStatus.INACTIVE).build();

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("PM"));

      // Act & Assert
      StepVerifier.create(userController.changeUserStatus(request))
          .expectError(AccessDeniedException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("changePassword_WhenPasswordsDoNotMatch_ShouldReturnError")
  void changePassword_WhenPasswordsDoNotMatch_ShouldReturnError() {
    // Given
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setNewPassword("newPass123");
    request.setConfirmPassword("differentPass123");

    Authentication auth = mock(Authentication.class);
    lenient().when(auth.getName()).thenReturn(testEmail);
    Collection<? extends GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PM"));
    lenient().when(auth.getAuthorities()).thenReturn((Collection) authorities);

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // When
      Mono<Api<Void>> result = userController.changePassword(request, auth);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatus().equals("failure")
                      && response
                          .getMessage()
                          .equals("New password and confirm password do not match"))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("changePassword_WhenUserHasNoRole_ShouldThrowAccessDeniedException")
  void changePassword_WhenUserHasNoRole_ShouldThrowAccessDeniedException() {
    // Given
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setNewPassword("newPass123");
    request.setConfirmPassword("newPass123");

    Authentication auth = mock(Authentication.class);
    lenient().when(auth.getName()).thenReturn(testEmail);
    Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
    lenient().when(auth.getAuthorities()).thenReturn((Collection) authorities);

    // When
    org.junit.jupiter.api.function.Executable action =
        () -> userController.changePassword(request, auth).block();

    // Then
    Assertions.assertThrows(AccessDeniedException.class, action);
  }

  @Test
  @DisplayName("changePassword_WhenServiceReturnsSuccess_ShouldReturnSuccess")
  void changePassword_WhenServiceReturnsSuccess_ShouldReturnSuccess() {
    // Given
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setNewPassword("newPass123");
    request.setConfirmPassword("newPass123");

    Authentication auth = mock(Authentication.class);
    lenient().when(auth.getName()).thenReturn(testEmail);
    Collection<? extends GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PM"));
    lenient().when(auth.getAuthorities()).thenReturn((Collection) authorities);

    Api<Void> successResponse = new Api<>();
    successResponse.setStatus("success");
    successResponse.setMessage("Password changed successfully");

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(userService.changePassword(testEmail, "PM", request))
          .thenReturn(Mono.just(successResponse));

      // When
      Mono<Api<Void>> result = userController.changePassword(request, auth);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatus().equals("success")
                      && response.getMessage().equals("Password changed successfully"))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("changeUserStatus_WhenServiceReturnsError_ShouldPropagateError")
  void changeUserStatus_WhenServiceReturnsError_ShouldPropagateError() {
    // Given
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder().userId("user123").newStatus(UserStatus.INACTIVE).build();

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("MANAGER"));
      when(userService.changeUserStatus(any(), any()))
          .thenReturn(Mono.error(new RuntimeException("Service error")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.changeUserStatus(request);

      // Then
      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Test
  @DisplayName("changeUserStatus_WhenUserNotFound_ShouldReturnNotFound")
  void changeUserStatus_WhenUserNotFound_ShouldReturnNotFound() {
    // Given
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder()
            .userId("nonexistent")
            .newStatus(UserStatus.INACTIVE)
            .build();

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("MANAGER"));
      when(userService.changeUserStatus(any(), any()))
          .thenReturn(Mono.error(new NotFoundException("User not found")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.changeUserStatus(request);

      // Then
      StepVerifier.create(result).expectError(NotFoundException.class).verify();
    }
  }

  @Test
  @DisplayName("changeUserStatus_WhenInvalidStatus_ShouldReturnBadRequest")
  void changeUserStatus_WhenInvalidStatus_ShouldReturnBadRequest() {
    // Given
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder()
            .userId("user123")
            .newStatus(null) // Invalid status
            .build();

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("MANAGER"));
      when(userService.changeUserStatus(any(), any()))
          .thenReturn(Mono.error(new BadRequestException("Invalid status")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.changeUserStatus(request);

      // Then
      StepVerifier.create(result).expectError(BadRequestException.class).verify();
    }
  }

  @Test
  @DisplayName("changeUserStatus_WhenUserIsManager_ShouldReturnForbidden")
  void changeUserStatus_WhenUserIsManager_ShouldReturnForbidden() {
    // Given
    ChangeUserStatusRequest request =
        ChangeUserStatusRequest.builder()
            .userId("manager123")
            .newStatus(UserStatus.INACTIVE)
            .build();

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("MANAGER"));
      when(userService.changeUserStatus(any(), any()))
          .thenReturn(Mono.error(new AccessDeniedException("Cannot change status of a MANAGER")));

      // When
      Mono<ResponseEntity<Api<UserDetailResponse>>> result =
          userController.changeUserStatus(request);

      // Then
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
    }
  }

  @Test
  @DisplayName("handleValidationErrors_ShouldReturnBadRequestApi")
  void handleValidationErrors_ShouldReturnBadRequestApi() {
    // Given
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("userRequest", "email", "must not be blank");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
    WebExchangeBindException ex = new WebExchangeBindException(null, bindingResult);

    // When
    Mono<Api<Void>> result = userController.handleValidationErrors(ex);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            api ->
                api.getStatus().equals("failure")
                    && api.getMessage().contains("email: must not be blank")
                    && api.getErrors().isPresent())
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteBaUser_ShouldReturnNotFound")
  void deleteBaUser_ShouldReturnNotFound() {
    String userId = "ba-not-found";
    Api<String> notFoundResponse =
        new Api<>("failure", "User not found", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deleteBaUser(userId)).thenReturn(Mono.just(notFoundResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("deleteBaUser_ShouldReturnBadRequest_WhenNotBA")
  void deleteBaUser_ShouldReturnBadRequest_WhenNotBA() {
    String userId = "not-ba";
    Api<String> badRequestResponse =
        new Api<>("failure", "User is not a BA", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deleteBaUser(userId)).thenReturn(Mono.just(badRequestResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("deleteBaUser_ShouldReturnInternalServerError")
  void deleteBaUser_ShouldReturnInternalServerError() {
    String userId = "ba-error";
    Api<String> errorResponse =
        new Api<>("failure", "Some other error", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deleteBaUser(userId)).thenReturn(Mono.just(errorResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deleteBaUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(
              response -> response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("deletePmUser_ShouldReturnNotFound")
  void deletePmUser_ShouldReturnNotFound() {
    String userId = "pm-not-found";
    Api<String> notFoundResponse =
        new Api<>("failure", "User not found", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deletePmUser(userId)).thenReturn(Mono.just(notFoundResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deletePmUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("deletePmUser_ShouldReturnBadRequest_WhenNotPM")
  void deletePmUser_ShouldReturnBadRequest_WhenNotPM() {
    String userId = "not-pm";
    Api<String> badRequestResponse =
        new Api<>("failure", "User is not a PM", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deletePmUser(userId)).thenReturn(Mono.just(badRequestResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deletePmUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("deletePmUser_ShouldReturnInternalServerError")
  void deletePmUser_ShouldReturnInternalServerError() {
    String userId = "pm-error";
    Api<String> errorResponse =
        new Api<>("failure", "Some other error", Optional.empty(), Optional.empty());
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));
      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.MANAGER_ROLE));
      when(userService.deletePmUser(userId)).thenReturn(Mono.just(errorResponse));
      Mono<ResponseEntity<Api<String>>> result = userController.deletePmUser(userId);
      StepVerifier.create(result)
          .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("requirePasswordChange_ShouldHandleNoRoles")
  void requirePasswordChange_ShouldHandleNoRoles() {
    SecurityContext mockSecurityContext = mock(SecurityContext.class);
    Authentication mockAuth = mock(Authentication.class);
    when(mockAuth.getName()).thenReturn("test@example.com");
    when(mockAuth.getAuthorities()).thenReturn(Collections.emptyList());
    when(mockSecurityContext.getAuthentication()).thenReturn(mockAuth);
    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockSecurityContext));
      // Mock userService to return Mono.empty() for no roles
      when(userService.checkPasswordChangeRequired(anyString(), anyList()))
          .thenReturn(Mono.empty());
      // When
      Mono<ResponseEntity<Api<Map<String, Boolean>>>> result =
          userController.requirePasswordChange();
      // Then
      StepVerifier.create(result).verifyComplete();
    }
  }

  @Nested
  @DisplayName("Update User Endpoint Tests")
  class UpdateUserTests {
    private final String userId = "123";
    private final String adminUsername = "admin@example.com";
    private UpdateUserRequest updateRequest;
    private UserDetailResponse updatedUserDetail;
    private Api<UserDetailResponse> apiResponse;
    private Authentication mockAuth;

    @BeforeEach
    void setup() {
      updateRequest =
          UpdateUserRequest.builder()
              .firstName("Updated")
              .lastName("User")
              .email("updated@example.com")
              .activeRole(UserConstants.PM_ROLE)
              .build();
      updatedUserDetail =
          UserDetailResponse.builder()
              .id(userId)
              .firstName("Updated")
              .lastName("User")
              .username("updated@example.com")
              .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
              .roles(
                  Collections.singletonList(
                      SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
              .dateCreated(now)
              .dateLastModified(null)
              .build();
      apiResponse = new Api<>();
      apiResponse.setStatus(UserConstants.SUCCESS);
      apiResponse.setMessage("User details updated successfully");
      apiResponse.setData(Optional.of(updatedUserDetail));
      apiResponse.setErrors(Optional.empty());
      mockAuth = mock(Authentication.class);
      lenient().when(mockAuth.getName()).thenReturn(adminUsername);
    }

    @Test
    @DisplayName("updateUser with MANAGER role should return updated user")
    void updateUser_WithManagerRole_ShouldReturnUpdatedUser() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));
        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class), anyString()))
            .thenReturn(Mono.just(apiResponse));

        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.updateUser(userId, updateRequest, mockAuth);

        StepVerifier.create(result)
            .expectNextMatches(
                response ->
                    response.getStatusCode() == HttpStatus.OK
                        && response.getBody().getStatus().equals(UserConstants.SUCCESS)
                        && response.getBody().getData().isPresent()
                        && response.getBody().getData().get().getFirstName().equals("Updated"))
            .verifyComplete();
      }
    }

    @Test
    @DisplayName("updateUser with non-MANAGER role should throw AccessDeniedException")
    void updateUser_WithNonManagerRole_ShouldThrowAccessDeniedException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));
        when(securityService.getCurrentUserRole()).thenReturn(Mono.just(UserConstants.PM_ROLE));

        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.updateUser(userId, updateRequest, mockAuth);

        StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
      }
    }

    @Test
    @DisplayName("updateUser when user not found should propagate NotFoundException")
    void updateUser_WhenUserNotFound_ShouldPropagateNotFoundException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));
        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class), anyString()))
            .thenReturn(Mono.error(new NotFoundException("User not found")));

        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.updateUser(userId, updateRequest, mockAuth);

        StepVerifier.create(result)
            .expectErrorMatches(
                throwable ->
                    throwable instanceof NotFoundException
                        && throwable.getMessage().contains("User not found"))
            .verify();
      }
    }

    @Test
    @DisplayName("updateUser when email already in use should propagate AlreadyExistException")
    void updateUser_WhenEmailAlreadyInUse_ShouldPropagateAlreadyExistException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));
        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class), anyString()))
            .thenReturn(
                Mono.error(
                    new com.aci.smart_onboarding.exception.AlreadyExistException(
                        "Email already in use")));

        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.updateUser(userId, updateRequest, mockAuth);

        StepVerifier.create(result)
            .expectErrorMatches(
                throwable ->
                    throwable instanceof com.aci.smart_onboarding.exception.AlreadyExistException
                        && throwable.getMessage().contains("Email already in use"))
            .verify();
      }
    }

    @Test
    @DisplayName("updateUser when service throws generic error should propagate Exception")
    void updateUser_WhenServiceThrowsGenericError_ShouldPropagateException() {
      try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
          mockStatic(ReactiveSecurityContextHolder.class)) {
        SecurityContext securityContext = setupMockSecurityContext();
        mockedHolder
            .when(ReactiveSecurityContextHolder::getContext)
            .thenReturn(Mono.just(securityContext));
        when(securityService.getCurrentUserRole())
            .thenReturn(Mono.just(UserConstants.MANAGER_ROLE));
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class), anyString()))
            .thenReturn(Mono.error(new RuntimeException("DB error")));

        Mono<ResponseEntity<Api<UserDetailResponse>>> result =
            userController.updateUser(userId, updateRequest, mockAuth);

        StepVerifier.create(result)
            .expectErrorMatches(
                throwable ->
                    throwable instanceof RuntimeException
                        && throwable.getMessage().contains("DB error"))
            .verify();
      }
    }
  }

  @Test
  @DisplayName("Search users should return filtered users")
  void searchUsers_ShouldReturnFilteredUsers() {
    // Given
    String searchTerm = "john";

    UserListResponse userListResponseTest =
        UserListResponse.builder()
            .pmUsers(
                Arrays.asList(
                    UserListResponse.UserInfo.builder()
                        .id("1")
                        .fullName("John Doe")
                        .email("john.doe@example.com")
                        .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
                        .roles(
                            Collections.singletonList(
                                SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
                        .build(),
                    UserListResponse.UserInfo.builder()
                        .id("2")
                        .fullName("Johnny Smith")
                        .email("johnny.smith@example.com")
                        .activeRole(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM)
                        .roles(
                            Collections.singletonList(
                                SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
                        .build()))
            .baUsers(Collections.emptyList())
            .build();

    Api<UserListResponse> apiResponseTest = new Api<>();
    apiResponseTest.setStatus(UserConstants.SUCCESS);
    apiResponseTest.setMessage(UserConstants.USERS_RETRIEVED);
    apiResponseTest.setData(Optional.of(userListResponseTest));
    apiResponseTest.setErrors(Optional.empty());

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      // Mock security context
      SecurityContext mockContext = setupMockSecurityContext();
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(mockContext));

      when(securityService.getCurrentUserRole())
          .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER));
      when(userService.searchPMAndBAUsers(searchTerm))
          .thenReturn(Mono.just(ResponseEntity.ok(apiResponseTest)));

      // When
      Mono<ResponseEntity<Api<UserListResponse>>> result =
          userController.searchPMAndBAUsers(searchTerm);

      // Then
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && response.getBody().getStatus().equals(UserConstants.SUCCESS)
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().getPmUsers().size() == 2
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getPmUsers()
                          .get(0)
                          .getFullName()
                          .equals("John Doe")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getPmUsers()
                          .get(1)
                          .getFullName()
                          .equals("Johnny Smith"))
          .verifyComplete();
    }
  }
}
