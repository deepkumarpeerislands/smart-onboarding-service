package com.aci.smart_onboarding.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.RedisSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  private static final String VALID_TOKEN = "valid.jwt.token";
  private static final String USERNAME = "test@example.com";
  private static final String ACTIVE_ROLE = "ROLE_USER";
  private static final String JTI = "testJti";

  @Mock private JwtService jwtService;

  @Mock private RedisSessionService redisSessionService;

  @Mock private WebFilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtService, redisSessionService, new ObjectMapper());
  }

  @Test
  void filter_WithValidToken_ShouldAuthenticate() {
    // Given
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build());

    Authentication expectedAuth =
        new JwtAuthenticationToken(USERNAME, ACTIVE_ROLE, List.of(ACTIVE_ROLE), JTI);
    when(jwtService.validateToken(VALID_TOKEN)).thenReturn(Mono.just(expectedAuth));
    when(redisSessionService.validateSession(USERNAME, JTI)).thenReturn(Mono.just(true));
    when(filterChain.filter(any())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

    verify(filterChain).filter(exchange);
  }

  @Test
  void filter_WithInvalidToken_ShouldNotAuthenticate() {
    // Given
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
                .build());

    when(jwtService.validateToken("invalid.token")).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

    verify(filterChain, never()).filter(exchange);
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void filter_WithNoToken_ShouldNotAuthenticate() {
    // Given
    ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

    // When & Then
    StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

    verify(filterChain, never()).filter(exchange);
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void filter_WithInvalidSession_ShouldNotAuthenticate() {
    // Given
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build());

    Authentication expectedAuth =
        new JwtAuthenticationToken(USERNAME, ACTIVE_ROLE, List.of(ACTIVE_ROLE), JTI);
    when(jwtService.validateToken(VALID_TOKEN)).thenReturn(Mono.just(expectedAuth));
    when(redisSessionService.validateSession(USERNAME, JTI)).thenReturn(Mono.just(false));

    // When & Then
    StepVerifier.create(filter.filter(exchange, filterChain)).verifyComplete();

    verify(filterChain, never()).filter(exchange);
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }
}
