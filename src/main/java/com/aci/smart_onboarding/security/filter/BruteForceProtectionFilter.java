package com.aci.smart_onboarding.security.filter;

import com.aci.smart_onboarding.constants.ApiPaths;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BruteForceProtectionFilter implements WebFilter {

  private final LoginAuditService loginAuditService;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!isLoginRequest(exchange)) {
      return chain.filter(exchange);
    }

    return exchange.getSession().flatMap(session -> handleLoginAttempt(session, chain, exchange));
  }

  private boolean isLoginRequest(ServerWebExchange exchange) {
    return exchange.getRequest().getPath().value().equals(ApiPaths.AUTH_LOGIN);
  }

  private Mono<Void> handleLoginAttempt(
      WebSession session, WebFilterChain chain, ServerWebExchange exchange) {
    Long blockedUntil = session.getAttribute(SecurityConstants.BLOCKED_UNTIL_KEY);

    if (blockedUntil != null) {
      if (System.currentTimeMillis() >= blockedUntil) {
        // Block period has expired, reset counters
        session.getAttributes().remove(SecurityConstants.BLOCKED_UNTIL_KEY);
        session.getAttributes().remove(SecurityConstants.ATTEMPTS_KEY);
        return chain.filter(exchange);
      } else if (isCurrentlyBlocked(blockedUntil)) {
        return handleBlockedRequest(exchange, blockedUntil);
      }
    }

    return chain.filter(exchange);
  }

  private boolean isCurrentlyBlocked(Long blockedUntil) {
    return blockedUntil != null && blockedUntil > System.currentTimeMillis();
  }

  private Mono<Void> handleBlockedRequest(ServerWebExchange exchange, Long blockedUntil) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    long remainingSeconds =
        Duration.between(Instant.now(), Instant.ofEpochMilli(blockedUntil)).getSeconds();

    String message =
        String.format(
            "Too many failed attempts. Please try again after %d minutes and %d seconds.",
            remainingSeconds / 60, remainingSeconds % 60);

    Api<Void> errorResponse = new Api<>("failure", message, Optional.empty(), Optional.empty());

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
    } catch (Exception e) {
      log.error("Error writing blocked response", e);
      return response.setComplete();
    }
  }
}
