package com.aci.smart_onboarding.security.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.aci.smart_onboarding.service.RedisSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticationSuccessHandlerTest {

  @Mock private JwtService jwtService;
  @Mock private UserRepository userRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private LoginAuditService loginAuditService;
  @Mock private RedisSessionService redisSessionService;
  @Mock private Authentication authentication;
  @Mock private ServerWebExchange exchange;
  @Mock private WebFilterExchange webFilterExchange;
  @Mock private WebSession session;

  private AuthenticationSuccessHandler authenticationSuccessHandler;
  private List<String> testRoles;
  private User testUser;
  private static final String TEST_USERNAME = "test@example.com";
  private static final String TEST_FIRST_NAME = "John";
  private static final String TEST_LAST_NAME = "Doe";
  private static final String TEST_ROLE = "ROLE_PM";
  private static final String TEST_TOKEN = "test-token";
  private static final String TEST_JTI = "test-jti";

  @BeforeEach
  void setUp() throws JsonProcessingException {
    MockitoAnnotations.openMocks(this);
    authenticationSuccessHandler =
        new AuthenticationSuccessHandler(
            jwtService, userRepository, objectMapper, loginAuditService, redisSessionService);

    testRoles = Arrays.asList(TEST_ROLE);
    testUser =
        User.builder()
            .email(TEST_USERNAME)
            .firstName(TEST_FIRST_NAME)
            .lastName(TEST_LAST_NAME)
            .activeRole(TEST_ROLE.replace("ROLE_", ""))
            .roles(Arrays.asList(TEST_ROLE.replace("ROLE_", "")))
            .build();

    // Setup mock response
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    HttpHeaders headers = new HttpHeaders();
    DataBufferFactory bufferFactory = mock(DataBufferFactory.class);
    DataBuffer dataBuffer = mock(DataBuffer.class);

    lenient().when(exchange.getResponse()).thenReturn(response);
    lenient().when(response.getHeaders()).thenReturn(headers);
    lenient().when(response.bufferFactory()).thenReturn(bufferFactory);
    lenient().when(bufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
    lenient().when(response.writeWith(any())).thenReturn(Mono.empty());
    lenient().when(webFilterExchange.getExchange()).thenReturn(exchange);

    // Setup session
    lenient().when(exchange.getSession()).thenReturn(Mono.just(session));
    lenient().when(session.invalidate()).thenReturn(Mono.empty());

    // Setup authentication
    lenient().when(authentication.getName()).thenReturn(TEST_USERNAME);
    doReturn(Collections.singleton(new SimpleGrantedAuthority(TEST_ROLE)))
        .when(authentication)
        .getAuthorities();
    lenient().when(redisSessionService.generateJti()).thenReturn(TEST_JTI);
    lenient()
        .when(
            jwtService.generateToken(eq(TEST_USERNAME), eq(testRoles), eq(TEST_ROLE), eq(TEST_JTI)))
        .thenReturn(TEST_TOKEN);
    lenient()
        .when(redisSessionService.createSession(TEST_USERNAME, TEST_JTI, TEST_ROLE, testRoles))
        .thenReturn(Mono.just(true));
    lenient().when(userRepository.findByEmail(TEST_USERNAME)).thenReturn(Mono.just(testUser));

    lenient()
        .when(
            objectMapper.writeValueAsBytes(
                argThat(
                    argument -> {
                      if (!(argument instanceof UserInfo userInfo)) return false;
                      return userInfo.getUsername().equals(TEST_USERNAME)
                          && userInfo.getFirstName().equals(TEST_FIRST_NAME)
                          && userInfo.getLastName().equals(TEST_LAST_NAME)
                          && userInfo.getActiveRole().equals(TEST_ROLE)
                          && userInfo.getRoles().equals(testRoles)
                          && userInfo.getToken().equals(TEST_TOKEN)
                          && userInfo.getEmail().equals(TEST_USERNAME);
                    })))
        .thenReturn(new byte[0]);
  }

  @Test
  void onAuthenticationSuccess_Success() {
    StepVerifier.create(
            authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();

    verify(jwtService).generateToken(TEST_USERNAME, testRoles, TEST_ROLE, TEST_JTI);
    verify(redisSessionService).createSession(TEST_USERNAME, TEST_JTI, TEST_ROLE, testRoles);
    verify(loginAuditService)
        .logLoginAttempt(eq(TEST_USERNAME), anyString(), eq(true), eq("Authentication successful"));
  }

  @Test
  void onAuthenticationSuccess_WithInvalidUser_ShouldFail() {
    when(authentication.getName()).thenReturn("invalid_user");
    when(userRepository.findByEmail("invalid_user")).thenReturn(Mono.empty());

    StepVerifier.create(
            authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .expectErrorMatches(
            e ->
                e instanceof NotFoundException
                    && e.getMessage().equals("User not found with email: invalid_user"))
        .verify();

    verify(jwtService, never()).generateToken(anyString(), anyList(), anyString(), anyString());
    verify(redisSessionService, never())
        .createSession(anyString(), anyString(), anyString(), anyList());
    verify(loginAuditService)
        .logLoginAttempt(
            eq("invalid_user"),
            anyString(),
            eq(false),
            eq("User not found with email: invalid_user"));
  }
}
