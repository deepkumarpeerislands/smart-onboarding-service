package com.aci.smart_onboarding.security.filter;

import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.RedisSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

  private final JwtService jwtService;
  private final RedisSessionService redisSessionService;
  private final ObjectMapper objectMapper;

  private static final List<String> PERMIT_ALL_PATHS =
      Arrays.asList(
          "/api/v1/auth/login",
          "/api/v1/auth/request-password-reset",
          "/api/v1/auth/reset-password",
          "/swagger-ui.html",
          "/swagger-ui/**",
          "/v3/api-docs/**",
          "/v3/api-docs",
          "/api-docs/**",
          "/api-docs",
          "/swagger-resources/**",
          "/swagger-resources",
          "/webjars/**",
          "/configuration/**",
          "/configuration/ui",
          "/configuration/security");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    log.debug("Processing request for path: {}", path);

    // Skip token validation for permitted paths
    if (PERMIT_ALL_PATHS.stream().anyMatch(pattern -> path.matches(pattern.replace("**", ".*")))) {
      log.debug("Path {} is in permitted list, skipping authentication", path);
      return chain.filter(exchange);
    }

    String token = extractToken(exchange);

    if (token == null) {
      log.debug("No token found in request");
      return handleAuthenticationError(exchange, "No authentication token provided");
    }

    log.debug("Validating token for path: {}", path);
    return jwtService
        .validateToken(token)
        .flatMap(
            authentication -> {
              JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
              log.debug(
                  "Token validated successfully for user: {} with roles: {} and active role: {}",
                  jwtAuth.getUsername(),
                  jwtAuth.getAuthorities(),
                  jwtAuth.getActiveRole());

              return redisSessionService
                  .validateSession(jwtAuth.getUsername(), jwtAuth.getJti())
                  .doOnNext(
                      isValid ->
                          log.debug(
                              "Session validation result for user {} with jti {}: {}",
                              jwtAuth.getUsername(),
                              jwtAuth.getJti(),
                              isValid))
                  .flatMap(
                      isValid -> {
                        if (Boolean.FALSE.equals(isValid)) {
                          log.warn(
                              "Invalid session for user: {} with jti: {}",
                              jwtAuth.getUsername(),
                              jwtAuth.getJti());
                          return handleAuthenticationError(exchange, "Invalid or expired session");
                        }

                        log.debug(
                            "Token and session validation successful for user: {}",
                            jwtAuth.getUsername());
                        return chain
                            .filter(exchange)
                            .contextWrite(
                                ReactiveSecurityContextHolder.withAuthentication(authentication));
                      });
            })
        .switchIfEmpty(handleAuthenticationError(exchange, "Invalid authentication token"))
        .onErrorResume(
            e -> {
              log.error("Error during token validation: {}", e.getMessage(), e);
              return handleAuthenticationError(exchange, "Authentication error: " + e.getMessage());
            });
  }

  private String extractToken(ServerWebExchange exchange) {
    String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "failure");
    errorResponse.put("message", message);
    errorResponse.put("data", null);
    errorResponse.put("errors", null);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange
          .getResponse()
          .writeWith(Mono.just(buffer))
          .doOnError(error -> log.error("Error writing error response", error))
          .onErrorResume(e -> Mono.empty());
    } catch (Exception e) {
      log.error("Error serializing error response", e);
      return Mono.empty();
    }
  }
}
