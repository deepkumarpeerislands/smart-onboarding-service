package com.aci.smart_onboarding.security.filter;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class HttpMethodFilterTest {
  private final HttpMethodFilter filter = new HttpMethodFilter();

  @Test
  void filter_WithGetRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.GET);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithPostRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.POST);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithPutRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.PUT);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithDeleteRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.DELETE);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithPatchRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.PATCH);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithHeadRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.HEAD);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithOptionsRequest_ShouldAllowRequest() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.OPTIONS);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithNullRequest_ShouldCompleteSuccessfully() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest()).thenReturn(null);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithNullMethod_ShouldCompleteSuccessfully() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(null);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }

  @Test
  void filter_WithChainError_ShouldPropagateError() {
    // Given
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    WebFilterChain chain = mock(WebFilterChain.class);
    when(exchange.getRequest())
        .thenReturn(mock(org.springframework.http.server.reactive.ServerHttpRequest.class));
    when(exchange.getRequest().getMethod()).thenReturn(HttpMethod.GET);
    when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("Test error")));

    // When & Then
    StepVerifier.create(filter.filter(exchange, chain))
        .expectError(RuntimeException.class)
        .verify();
  }
}
