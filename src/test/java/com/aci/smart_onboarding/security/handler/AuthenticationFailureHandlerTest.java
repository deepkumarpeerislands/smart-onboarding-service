package com.aci.smart_onboarding.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.security.config.SecurityConstants;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.aci.smart_onboarding.util.WebUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AuthenticationFailureHandlerTest {

  @Mock private SecurityConstants securityConstants;
  @Mock private LoginAuditService loginAuditService;
  @Mock private ObjectMapper objectMapper;
  @Mock private WebFilterExchange webFilterExchange;
  @Mock private MockServerWebExchange exchange;
  @Mock private MockServerHttpResponse response;
  @Mock private MockServerHttpRequest request;
  @Mock private WebSession session;
  @Mock private DataBuffer dataBuffer;
  @Mock private org.springframework.core.io.buffer.DataBufferFactory bufferFactory;
  @Mock private HttpHeaders httpHeaders;

  @InjectMocks private AuthenticationFailureHandler authenticationFailureHandler;

  private static final String USERNAME = "testuser";
  private static final String CLIENT_IP = "192.168.1.1";
  private static final String ERROR_MESSAGE = "Invalid credentials";
  private static final int MAX_ATTEMPTS = 3;
  private static final long BLOCK_DURATION_SECONDS = 300L;

  private MockedStatic<WebUtils> webUtilsMock;

  @BeforeEach
  void setUp() {
    // Setup lenient mocks for commonly used methods
    lenient().when(webFilterExchange.getExchange()).thenReturn(exchange);
    lenient().when(exchange.getResponse()).thenReturn(response);
    lenient().when(exchange.getRequest()).thenReturn(request);
    lenient().when(response.bufferFactory()).thenReturn(bufferFactory);
    lenient().when(bufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
    lenient().when(response.writeWith(any(Mono.class))).thenReturn(Mono.empty());
    lenient().when(securityConstants.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
    lenient().when(securityConstants.getBlockDurationSeconds()).thenReturn(BLOCK_DURATION_SECONDS);
    lenient().when(response.getHeaders()).thenReturn(httpHeaders);

    webUtilsMock = mockStatic(WebUtils.class);
    webUtilsMock.when(() -> WebUtils.getClientIpAddress(any())).thenReturn("192.168.1.1");
  }

  @AfterEach
  void tearDown() {
    if (webUtilsMock != null) {
      webUtilsMock.close();
    }
  }

  @Test
  void onAuthenticationFailure_ShouldHandleFirstFailedAttempt() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(USERNAME);
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    verify(response.getHeaders()).setContentType(MediaType.APPLICATION_JSON);
    verify(loginAuditService).logLoginAttempt(USERNAME, CLIENT_IP, false, ERROR_MESSAGE);
    assertEquals(1, sessionAttributes.get(SecurityConstants.ATTEMPTS_KEY));
  }

  @Test
  void onAuthenticationFailure_ShouldHandleSubsequentFailedAttempts() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put(SecurityConstants.ATTEMPTS_KEY, 2);
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(USERNAME);
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(2);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertEquals(3, sessionAttributes.get(SecurityConstants.ATTEMPTS_KEY));
  }

  @Test
  void onAuthenticationFailure_ShouldBlockAccount_WhenMaxAttemptsReached() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put(SecurityConstants.ATTEMPTS_KEY, MAX_ATTEMPTS - 1);
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(USERNAME);
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(MAX_ATTEMPTS - 1);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertEquals(MAX_ATTEMPTS, sessionAttributes.get(SecurityConstants.ATTEMPTS_KEY));
    assertNotNull(sessionAttributes.get(SecurityConstants.BLOCKED_UNTIL_KEY));
    
    Long blockedUntil = (Long) sessionAttributes.get(SecurityConstants.BLOCKED_UNTIL_KEY);
    long expectedBlockTime = System.currentTimeMillis() + (BLOCK_DURATION_SECONDS * 1000L);
    assertTrue(blockedUntil > System.currentTimeMillis());
    assertTrue(blockedUntil <= expectedBlockTime);
  }

  @ParameterizedTest
  @MethodSource("usernameResolutionScenarios")
  void onAuthenticationFailure_ShouldResolveUsernameCorrectly(
      String exchangeUsername, 
      String sessionUsername, 
      boolean sessionExists, 
      String expectedUsername) throws Exception {
    
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(exchangeUsername);
    
    if (sessionExists) {
      lenient().when(exchange.getSession()).thenReturn(Mono.just(session));
      lenient().when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
      lenient().when(session.getAttribute(SecurityConstants.LAST_USERNAME_KEY)).thenReturn(sessionUsername);
      lenient().when(session.getAttributes()).thenReturn(sessionAttributes);
    } else {
      lenient().when(exchange.getSession()).thenReturn(Mono.empty());
    }
    
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(expectedUsername, CLIENT_IP, false, ERROR_MESSAGE);
  }

  private static Stream<Arguments> usernameResolutionScenarios() {
    return Stream.of(
        // exchangeUsername, sessionUsername, sessionExists, expectedUsername
        Arguments.of(USERNAME, null, true, USERNAME), // Username from exchange attribute
        Arguments.of(null, USERNAME, true, USERNAME), // Username from session when exchange is null
        Arguments.of(null, null, false, "unknown_user"), // Unknown user when no session
        Arguments.of("", USERNAME, true, USERNAME), // Empty exchange username, fallback to session
        Arguments.of("   ", USERNAME, true, USERNAME) // Whitespace exchange username, fallback to session
    );
  }

  @Test
  void onAuthenticationFailure_ShouldHandleEmptyUsernameFromExchange() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn("");
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
    when(session.getAttribute(SecurityConstants.LAST_USERNAME_KEY)).thenReturn(USERNAME);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(USERNAME, CLIENT_IP, false, ERROR_MESSAGE);
  }

  @Test
  void onAuthenticationFailure_ShouldHandleWhitespaceUsernameFromExchange() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", ERROR_MESSAGE, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Invalid credentials\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn("   ");
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
    when(session.getAttribute(SecurityConstants.LAST_USERNAME_KEY)).thenReturn(USERNAME);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(USERNAME, CLIENT_IP, false, ERROR_MESSAGE);
  }

  @Test
  void onAuthenticationFailure_ShouldHandleObjectMapperException() throws JsonProcessingException {
    AuthenticationException exception = new BadCredentialsException(ERROR_MESSAGE);
    when(exchange.getAttribute("username")).thenReturn("testuser");
    doThrow(new RuntimeException("Serialization error"))
        .when(objectMapper).writeValueAsBytes(any(Api.class));

    StepVerifier.create(authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void onAuthenticationFailure_ShouldHandleDifferentAuthenticationExceptionTypes() throws Exception {
    // Arrange
    AuthenticationException exception = new org.springframework.security.authentication.AccountExpiredException("Account expired");
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", "Account expired", Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":\"Account expired\"}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(USERNAME);
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(USERNAME, CLIENT_IP, false, "Account expired");
  }

  @Test
  void onAuthenticationFailure_ShouldHandleNullExceptionMessage() throws Exception {
    // Arrange
    AuthenticationException exception = new BadCredentialsException(null);
    Map<String, Object> sessionAttributes = new HashMap<>();
    Api<Void> expectedResponse = new Api<>("failure", null, Optional.empty(), Optional.empty());
    byte[] responseBytes = "{\"status\":\"failure\",\"message\":null}".getBytes();

    when(exchange.getAttribute("username")).thenReturn(USERNAME);
    when(exchange.getSession()).thenReturn(Mono.just(session));
    when(session.getAttribute(SecurityConstants.ATTEMPTS_KEY)).thenReturn(null);
    when(session.getAttributes()).thenReturn(sessionAttributes);
    when(objectMapper.writeValueAsBytes(expectedResponse)).thenReturn(responseBytes);

    // Act
    Mono<Void> result = authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(loginAuditService).logLoginAttempt(USERNAME, CLIENT_IP, false, null);
  }
} 