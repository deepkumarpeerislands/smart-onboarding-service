package com.aci.smart_onboarding.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.ApiPaths;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BruteForceProtectionFilterTest {

  private LoginAuditService loginAuditService;
  private ObjectMapper objectMapper;
  private BruteForceProtectionFilter filter;
  private WebFilterChain chain;

  @BeforeEach
  void setUp() {
    loginAuditService = mock(LoginAuditService.class);
    objectMapper = new ObjectMapper();
    filter = new BruteForceProtectionFilter(loginAuditService, objectMapper);
    chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  void filter_shouldPassThroughForNonLoginRequest() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/other")
                .build());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    verify(chain).filter(exchange);
  }

  @Test
  void filter_shouldPassThroughForLoginRequestWithNoBlock() {
    MockServerWebExchange exchange = loginExchange();
    WebSession session = mock(WebSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(session.getAttributes()).thenReturn(attributes);
    when(session.getAttribute(SecurityConstants.BLOCKED_UNTIL_KEY))
        .then(invocation -> attributes.get(SecurityConstants.BLOCKED_UNTIL_KEY));
    MockServerWebExchange spyExchange = Mockito.spy(exchange);
    when(spyExchange.getSession()).thenReturn(Mono.just(session));

    StepVerifier.create(filter.filter(spyExchange, chain)).verifyComplete();

    verify(chain).filter(spyExchange);
  }

  @Test
  void filter_shouldResetBlockIfExpired() {
    MockServerWebExchange exchange = loginExchange();
    WebSession session = mock(WebSession.class);
    Map<String, Object> attributes = new HashMap<>();
    long expired = System.currentTimeMillis() - 1000;
    attributes.put(SecurityConstants.BLOCKED_UNTIL_KEY, expired);
    attributes.put(SecurityConstants.ATTEMPTS_KEY, 3);
    when(session.getAttributes()).thenReturn(attributes);
    when(session.getAttribute(SecurityConstants.BLOCKED_UNTIL_KEY))
        .then(invocation -> attributes.get(SecurityConstants.BLOCKED_UNTIL_KEY));
    MockServerWebExchange spyExchange = Mockito.spy(exchange);
    when(spyExchange.getSession()).thenReturn(Mono.just(session));

    StepVerifier.create(filter.filter(spyExchange, chain)).verifyComplete();

    verify(chain).filter(spyExchange);
  }

  @Test
  void filter_shouldReturn429IfBlocked() {
    MockServerWebExchange exchange = loginExchange();
    WebSession session = mock(WebSession.class);
    Map<String, Object> attributes = new HashMap<>();
    long blockedUntil = System.currentTimeMillis() + 60_000;
    attributes.put(SecurityConstants.BLOCKED_UNTIL_KEY, blockedUntil);
    when(session.getAttributes()).thenReturn(attributes);
    when(session.getAttribute(SecurityConstants.BLOCKED_UNTIL_KEY))
        .then(invocation -> attributes.get(SecurityConstants.BLOCKED_UNTIL_KEY));
    MockServerWebExchange spyExchange = Mockito.spy(exchange);
    when(spyExchange.getSession()).thenReturn(Mono.just(session));

    StepVerifier.create(filter.filter(spyExchange, chain)).verifyComplete();

    var response = spyExchange.getResponse();
    assert response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
    assert response.getHeaders().getContentType().equals(MediaType.APPLICATION_JSON);
    String body = response.getBodyAsString().block(Duration.ofSeconds(1));
    assert body.contains("Too many failed attempts");
  }

  @Test
  void handleBlockedRequest_shouldHandleException() throws Exception {
    // Simulate ObjectMapper throwing exception
    ObjectMapper failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsBytes(any())).thenThrow(new RuntimeException("fail"));
    BruteForceProtectionFilter filterWithFailingMapper =
        new BruteForceProtectionFilter(loginAuditService, failingMapper);

    MockServerWebExchange exchange = loginExchange();
    WebSession session = mock(WebSession.class);
    Map<String, Object> attributes = new HashMap<>();
    long blockedUntil = System.currentTimeMillis() + 60_000;
    attributes.put(SecurityConstants.BLOCKED_UNTIL_KEY, blockedUntil);
    when(session.getAttributes()).thenReturn(attributes);
    when(session.getAttribute(SecurityConstants.BLOCKED_UNTIL_KEY))
        .then(invocation -> attributes.get(SecurityConstants.BLOCKED_UNTIL_KEY));
    MockServerWebExchange spyExchange = Mockito.spy(exchange);
    when(spyExchange.getSession()).thenReturn(Mono.just(session));

    StepVerifier.create(filterWithFailingMapper.filter(spyExchange, chain)).verifyComplete();
  }

  private MockServerWebExchange loginExchange() {
    return MockServerWebExchange.from(
        org.springframework.mock.http.server.reactive.MockServerHttpRequest.post(
                ApiPaths.AUTH_LOGIN)
            .build());
  }
}
