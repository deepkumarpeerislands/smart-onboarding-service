package com.aci.smart_onboarding.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ForgotPasswordRequest;
import com.aci.smart_onboarding.dto.LoginRequest;
import com.aci.smart_onboarding.dto.ResetPasswordRequest;
import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.PasswordResetTokenRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.AzureADAuthenticationManager;
import com.aci.smart_onboarding.security.service.CustomAuthenticationManager;
import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.service.LoginAttemptService;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.security.validator.LoginRequestValidator;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IForgotPasswordService;
import com.aci.smart_onboarding.service.RedisSessionService;
import com.aci.smart_onboarding.service.IAuthService;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private JwtService jwtService;
  @Mock private CustomAuthenticationManager customAuthenticationManager;
  @Mock private AzureADAuthenticationManager azureADAuthenticationManager;
  @Mock private LoginAuditService loginAuditService;
  @Mock private LoginRequestValidator loginRequestValidator;
  @Mock private LoginAttemptService loginAttemptService;
  @Mock private IForgotPasswordService forgotPasswordService;
  private ServerWebExchange exchange;
  private ServerHttpRequest request;
  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RedisSessionService redisSessionService;
  @Mock private ReactiveAuthenticationManager authenticationManager;
  @Mock private IAuditLogService auditLogService;
  @Mock private IAuthService authService;

  @InjectMocks private AuthController authController;
  private LocalValidatorFactoryBean validator;

  private static final String TEST_USERNAME = "test@example.com";
  private static final String TEST_PASSWORD = "password";
  private static final String TEST_ROLE = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
  private static final String TEST_TOKEN = "test-token";
  private static final String TEST_JTI = "test-jti";
  private static final String TEST_IP = "127.0.0.1";
  private static final String TEST_FIRST_NAME = "John";
  private static final String TEST_LAST_NAME = "Doe";

  private static class TestWebSession implements WebSession {
    private final Map<String, Object> attributes = new HashMap<>();
    private boolean started = true;
    private final String id = "test-session-id";
    private final Instant creationTime = Instant.now();
    private final Duration maxIdleTime = Duration.ofMinutes(30);
    private Instant lastAccessTime = Instant.now();

    @Override
    public String getId() {
      return id;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return attributes;
    }

    @Override
    public void start() {
      started = true;
    }

    @Override
    public boolean isStarted() {
      return started;
    }

    @Override
    public Mono<Void> changeSessionId() {
      return Mono.empty();
    }

    @Override
    public Mono<Void> invalidate() {
      attributes.clear();
      started = false;
      return Mono.empty();
    }

    @Override
    public Mono<Void> save() {
      return Mono.empty();
    }

    @Override
    public boolean isExpired() {
      return false;
    }

    @Override
    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    @Override
    public void setMaxIdleTime(Duration maxIdleTime) {
      // No-op for test
    }

    @Override
    public Duration getMaxIdleTime() {
      return maxIdleTime;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    // Initialize AuthController with Azure AD support
    authController =
        new AuthController(
            jwtService,
            customAuthenticationManager,
            loginAuditService,
            loginRequestValidator,
            loginAttemptService,
            Optional.of(azureADAuthenticationManager),
            forgotPasswordService,
            passwordEncoder,
            tokenRepository,
            userRepository,
            redisSessionService,
            auditLogService,
            authService);
    validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    // Set up request and exchange
    request =
        MockServerHttpRequest.post("/login")
            .remoteAddress(new InetSocketAddress(InetAddress.getByName(TEST_IP), 8080))
            .build();
    exchange = MockServerWebExchange.from((MockServerHttpRequest) request);
    exchange.getAttributes().put("username", TEST_USERNAME);
    exchange.getAttributes().put("clientIp", TEST_IP);

    // Set up default mock behavior
    lenient()
        .when(loginAttemptService.checkBlockedStatus(anyString()))
        .thenReturn(Mono.just(false));
    lenient().when(loginAttemptService.loginFailed(anyString())).thenReturn(Mono.just(false));
    lenient().when(redisSessionService.generateJti()).thenReturn(TEST_JTI);
    lenient()
        .when(redisSessionService.createSession(anyString(), anyString(), anyString(), anyList()))
        .thenReturn(Mono.just(true));
    lenient()
        .when(jwtService.generateToken(anyString(), anyList(), anyString(), anyString()))
        .thenReturn(TEST_TOKEN);

    // Set up default user
    User defaultUser =
        User.builder()
            .email(TEST_USERNAME)
            .firstName(TEST_FIRST_NAME)
            .lastName(TEST_LAST_NAME)
            .activeRole(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, ""))
            .roles(Collections.singletonList(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, "")))
            .build();
    lenient()
        .when(userRepository.findByEmailAndRole(eq(TEST_USERNAME), anyString()))
        .thenReturn(Mono.just(defaultUser));
  }

  @Test
  @DisplayName("Should block after max attempts")
  void login_WithMaxAttemptsExceeded_ShouldThrowAccountBlockedException() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    lenient()
        .when(customAuthenticationManager.authenticate(any()))
        .thenReturn(Mono.error(new BadCredentialsException("Invalid credentials")));
    lenient().when(loginAttemptService.loginFailed(any())).thenReturn(Mono.just(true));
    lenient().when(loginAttemptService.checkBlockedStatus(any())).thenReturn(Mono.just(true));

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage())
                  .isEqualTo("Too many failed attempts. Account blocked for 15 minutes.");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService)
        .logLoginAttempt(
            TEST_USERNAME,
            TEST_IP,
            false,
            "Too many failed attempts. Account blocked for 15 minutes.");
    verify(loginAuditService)
        .logSuspiciousActivity(
            TEST_USERNAME, TEST_IP, "Too many failed attempts. Account blocked for 15 minutes.");
  }

  @Test
  @DisplayName("Should handle invalid input")
  void login_WithInvalidInput_ShouldThrowBadCredentialsException() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    BadCredentialsException validationError = new BadCredentialsException("Invalid credentials");
    lenient().doThrow(validationError).when(loginRequestValidator).validate(loginRequest);
    lenient().when(customAuthenticationManager.authenticate(any())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("Invalid credentials");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(TEST_USERNAME, TEST_IP, false, "Invalid credentials");
  }

  @Test
  @DisplayName("Should handle authentication failure")
  void login_WithInvalidCredentials_ShouldThrowBadCredentialsException() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    lenient()
        .when(customAuthenticationManager.authenticate(any()))
        .thenReturn(Mono.error(new BadCredentialsException("Invalid credentials")));
    lenient().when(loginAttemptService.loginFailed(any())).thenReturn(Mono.just(false));

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("Invalid credentials");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(TEST_USERNAME, TEST_IP, false, "Invalid credentials");
  }

  @Test
  @DisplayName("Should successfully authenticate valid credentials")
  void login_WithValidCredentials_ShouldReturnUserInfo() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            TEST_USERNAME, null, Collections.singletonList(new SimpleGrantedAuthority(TEST_ROLE)));

    lenient()
        .when(customAuthenticationManager.authenticate(any()))
        .thenReturn(Mono.just(authentication));

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getData()).isPresent();
              UserInfo userInfo = apiResponse.getData().get();
              return userInfo.getUsername().equals(TEST_USERNAME)
                  && userInfo.getActiveRole().equals(TEST_ROLE)
                  && userInfo.getRoles().contains(TEST_ROLE)
                  && userInfo.getToken().equals(TEST_TOKEN);
            })
        .verifyComplete();

    verify(loginAuditService)
        .logLoginAttempt(TEST_USERNAME, TEST_IP, true, "Authentication successful");
  }

  @Test
  @DisplayName("Should reject blocked user")
  void login_WithBlockedUser_ShouldThrowAccountBlockedException() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    lenient().when(loginAttemptService.checkBlockedStatus(anyString())).thenReturn(Mono.just(true));
    lenient().when(customAuthenticationManager.authenticate(any())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("Invalid credentials");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(TEST_USERNAME, TEST_IP, false, "Invalid credentials");
  }

  @Test
  @DisplayName("Should throw BadCredentialsException for null username or password")
  void login_WithNullOrEmptyUsernameOrPassword_ShouldThrowBadCredentialsException() {
    // Given
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername(null);
    loginRequest.setPassword(null);

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("Username and password are required");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService)
        .logLoginAttempt(null, TEST_IP, false, "Username and password are required");
    verifyNoInteractions(loginAttemptService);
    verifyNoInteractions(customAuthenticationManager);
  }

  @Test
  @DisplayName("Should wrap unhandled exception in RuntimeException")
  void login_WithUnhandledException_ShouldWrapInRuntimeException() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    RuntimeException unexpected = new RuntimeException("unexpected");
    lenient()
        .when(loginAttemptService.checkBlockedStatus(anyString()))
        .thenReturn(Mono.error(unexpected));
    lenient().when(customAuthenticationManager.authenticate(any())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("Invalid credentials");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(TEST_USERNAME, TEST_IP, false, "Invalid credentials");
  }

  @Test
  @DisplayName("Should use Azure AD authentication in prod profile")
  void login_WithProdProfile_ShouldUseAzureAD() {
    // Given
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            TEST_USERNAME, null, Collections.singletonList(new SimpleGrantedAuthority(TEST_ROLE)));

    User user =
        User.builder()
            .email(TEST_USERNAME)
            .firstName(TEST_FIRST_NAME)
            .lastName(TEST_LAST_NAME)
            .activeRole(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, ""))
            .roles(Collections.singletonList(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, "")))
            .build();

    // Set prod profile
    ReflectionTestUtils.setField(authController, "activeProfile", "prod");

    lenient()
        .when(azureADAuthenticationManager.authenticate(any()))
        .thenReturn(Mono.just(authentication));
    lenient()
        .when(userRepository.findByEmailAndRole(eq(TEST_USERNAME), anyString()))
        .thenReturn(Mono.just(user));
    lenient()
        .when(redisSessionService.createSession(anyString(), anyString(), anyString(), anyList()))
        .thenReturn(Mono.just(true));
    lenient()
        .when(jwtService.generateToken(anyString(), anyList(), anyString(), anyString()))
        .thenReturn(TEST_TOKEN);

    // When & Then
    StepVerifier.create(authController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getData()).isPresent();
              UserInfo userInfo = apiResponse.getData().get();
              return userInfo.getUsername().equals(TEST_USERNAME)
                  && userInfo.getActiveRole().equals(TEST_ROLE)
                  && userInfo.getRoles().contains(TEST_ROLE)
                  && userInfo.getToken().equals(TEST_TOKEN);
            })
        .verifyComplete();

    verify(loginAuditService)
        .logLoginAttempt(TEST_USERNAME, TEST_IP, true, "Authentication successful");
    verify(azureADAuthenticationManager).authenticate(any());
    verify(customAuthenticationManager, never()).authenticate(any());
  }

  @Test
  @DisplayName("Should return current user info from /me endpoint")
  void getCurrentUser_ShouldReturnUserInfo() {
    // Given
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            TEST_USERNAME, null, Collections.singletonList(new SimpleGrantedAuthority(TEST_ROLE)));

    try (MockedStatic<ReactiveSecurityContextHolder> mockedHolder =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
      securityContext.setAuthentication(auth);
      mockedHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // When & Then
      StepVerifier.create(authController.getCurrentUser())
          .expectNextMatches(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Api<UserInfo> apiResponse = response.getBody();
                assertThat(apiResponse).isNotNull();
                assertThat(apiResponse.getData()).isPresent();
                UserInfo userInfo = apiResponse.getData().get();
                return userInfo.getActiveRole().equals(TEST_ROLE)
                    && userInfo.getRoles().contains(TEST_ROLE);
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return user info with token from /user-info endpoint")
  void getUserInfo_WithValidToken_ShouldReturnUserInfo() {
    // Given
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            TEST_USERNAME, null, Collections.singletonList(new SimpleGrantedAuthority(TEST_ROLE)));

    User user =
        User.builder()
            .email(TEST_USERNAME)
            .firstName(TEST_FIRST_NAME)
            .lastName(TEST_LAST_NAME)
            .activeRole(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, ""))
            .roles(Collections.singletonList(TEST_ROLE.replace(SecurityConstants.ROLE_PREFIX, "")))
            .build();

    lenient()
        .when(userRepository.findByEmailAndRole(eq(TEST_USERNAME), anyString()))
        .thenReturn(Mono.just(user));
    lenient().when(redisSessionService.generateJti()).thenReturn(TEST_JTI);
    lenient()
        .when(jwtService.generateToken(anyString(), anyList(), anyString(), anyString()))
        .thenReturn(TEST_TOKEN);

    // When & Then
    StepVerifier.create(authController.getUserInfo(auth))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getData()).isPresent();
              UserInfo userInfo = apiResponse.getData().get();
              return userInfo.getUsername().equals(TEST_USERNAME)
                  && userInfo.getActiveRole().equals(TEST_ROLE)
                  && userInfo.getRoles().contains(TEST_ROLE);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should throw IllegalStateException if AzureADAuthenticationManager is missing in prod")
  void login_WithProdProfileAndNoAzureAD_ShouldThrowIllegalStateException() {
    // Create controller without Azure AD manager
    AuthController prodController =
        new AuthController(
            jwtService,
            customAuthenticationManager,
            loginAuditService,
            loginRequestValidator,
            loginAttemptService,
            Optional.empty(),
            forgotPasswordService,
            passwordEncoder,
            tokenRepository,
            userRepository,
            redisSessionService,
            auditLogService,
            authService);

    // Set prod profile
    ReflectionTestUtils.setField(prodController, "activeProfile", "prod");

    // Create login request
    LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

    // Test that IllegalStateException is thrown immediately
    StepVerifier.create(prodController.login(loginRequest, exchange))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage())
                  .isEqualTo("Azure AD authentication manager not configured in production");
              return true;
            })
        .verifyComplete();

    verify(loginAuditService)
        .logLoginAttempt(
            TEST_USERNAME,
            TEST_IP,
            false,
            "Azure AD authentication manager not configured in production");
  }

  @Test
  @DisplayName("Should process valid password reset request")
  void requestPasswordReset_WithValidEmail_ShouldReturnSuccess() {
    // Given
    String email = "test@example.com";
    ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest(email);
    when(forgotPasswordService.requestPasswordReset(email)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(authController.requestPasswordReset(forgotPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("SUCCESS");
              assertThat(apiResponse.getMessage())
                  .isEqualTo(
                      "If an account with that email exists, a password reset link has been sent.");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).requestPasswordReset(email);
  }

  @Test
  @DisplayName("Should handle service errors gracefully")
  void requestPasswordReset_WithServiceError_ShouldReturnSuccess() {
    // Given
    String email = "test@example.com";
    ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest(email);
    when(forgotPasswordService.requestPasswordReset(email))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When & Then
    StepVerifier.create(authController.requestPasswordReset(forgotPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("SUCCESS");
              assertThat(apiResponse.getMessage())
                  .isEqualTo(
                      "If an account with that email exists, a password reset link has been sent.");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).requestPasswordReset(email);
  }

  @Test
  @DisplayName("Should validate email format")
  void requestPasswordReset_WithInvalidEmail_ShouldFailValidation() {
    // Given
    String invalidEmail = "not-an-email";
    ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest(invalidEmail);

    // When & Then
    var violations = validator.validate(forgotPasswordRequest);

    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
    assertEquals("Please provide a valid email address", violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Should require non-blank email")
  void requestPasswordReset_WithBlankEmail_ShouldFailValidation() {
    // Given
    ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest("");

    // When & Then
    var violations = validator.validate(forgotPasswordRequest);

    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(violation -> violation.getMessage().equals("Email is required")));
  }

  @Test
  @DisplayName("Should successfully reset password with valid token")
  void resetPassword_WithValidToken_ShouldResetPassword() {
    // Given
    String token = "valid-token";
    String newPassword = "newPass123!";
    ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(newPassword, newPassword);

    Api<Void> successResponse =
        new Api<>(
            "SUCCESS",
            "Password reset successful. Please login with your new password.",
            Optional.empty(),
            Optional.empty());

    when(forgotPasswordService.resetPassword(token, resetPasswordRequest))
        .thenReturn(Mono.just(successResponse));

    // When & Then
    StepVerifier.create(authController.resetPassword(token, resetPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("SUCCESS");
              assertThat(apiResponse.getMessage())
                  .isEqualTo("Password reset successful. Please login with your new password.");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).resetPassword(token, resetPasswordRequest);
  }

  @Test
  @DisplayName("Should fail with invalid token")
  void resetPassword_WithInvalidToken_ShouldReturnError() {
    // Given
    String token = "invalid-token";
    String newPassword = "newPass123!";
    ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(newPassword, newPassword);

    Api<Void> errorResponse =
        new Api<>(
            "failure",
            "Invalid or expired token",
            Optional.empty(),
            Optional.empty());

    when(forgotPasswordService.resetPassword(token, resetPasswordRequest))
        .thenReturn(Mono.just(errorResponse));

    // When & Then
    StepVerifier.create(authController.resetPassword(token, resetPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage())
                  .isEqualTo("Invalid or expired token");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).resetPassword(token, resetPasswordRequest);
  }

  @Test
  @DisplayName("Should fail when passwords don't match")
  void resetPassword_WithMismatchedPasswords_ShouldReturnError() {
    // Given
    String token = "valid-token";
    ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("password1", "password2");

    Api<Void> errorResponse =
        new Api<>("failure", "New password and confirm password do not match", Optional.empty(), Optional.empty());

    when(forgotPasswordService.resetPassword(token, resetPasswordRequest))
        .thenReturn(Mono.just(errorResponse));

    // When & Then
    StepVerifier.create(authController.resetPassword(token, resetPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage()).isEqualTo("New password and confirm password do not match");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).resetPassword(token, resetPasswordRequest);
  }

  @Test
  @DisplayName("Should fail with expired token")
  void resetPassword_WithExpiredToken_ShouldReturn401() {
    // Given
    String token = "expired-token";
    String newPassword = "newPass123!";
    ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(newPassword, newPassword);

    Api<Void> errorResponse =
        new Api<>(
            "failure",
            "Invalid or expired token",
            Optional.empty(),
            Optional.empty());

    when(forgotPasswordService.resetPassword(token, resetPasswordRequest))
        .thenReturn(Mono.just(errorResponse));

    // When & Then
    StepVerifier.create(authController.resetPassword(token, resetPasswordRequest))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("failure");
              assertThat(apiResponse.getMessage())
                  .isEqualTo("Invalid or expired token");
              return true;
            })
        .verifyComplete();

    verify(forgotPasswordService).resetPassword(token, resetPasswordRequest);
  }

  @Test
  @DisplayName("Should successfully logout user and invalidate session")
  void logout_WithValidAuthentication_ShouldLogoutSuccessfully() {
    // Given
    String username = "test@example.com";
    String activeRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
    String jti = "test-jti";
    List<String> roles = Arrays.asList(activeRole);
    
    JwtAuthenticationToken authentication = 
        new JwtAuthenticationToken(username, activeRole, roles, jti);
    
    Api<Void> expectedResponse = new Api<>(
        UserConstants.SUCCESS,
        "Logout successful",
        Optional.empty(),
        Optional.empty());
    
    ResponseEntity<Api<Void>> expectedResponseEntity = ResponseEntity.ok(expectedResponse);

    when(authService.logout(authentication)).thenReturn(Mono.just(expectedResponseEntity));

    // When & Then
    StepVerifier.create(authController.logout(authentication))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<Void> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("SUCCESS");
              assertThat(apiResponse.getMessage()).isEqualTo("Logout successful");
              return true;
            })
        .verifyComplete();

    verify(authService).logout(authentication);
  }

}
