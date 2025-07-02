package com.aci.smart_onboarding.security.handler;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.security.config.SecurityConstants;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.aci.smart_onboarding.util.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFailureHandler implements ServerAuthenticationFailureHandler {
  private final SecurityConstants securityConstants;
  private final LoginAuditService loginAuditService;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> onAuthenticationFailure(
      WebFilterExchange webFilterExchange, AuthenticationException exception) {
    String clientIp = WebUtils.getClientIpAddress(webFilterExchange.getExchange());

    return getAttemptedUsername(webFilterExchange)
        .flatMap(
            username -> {
              // Set response status and headers
              var response = webFilterExchange.getExchange().getResponse();
              response.setStatusCode(HttpStatus.UNAUTHORIZED);
              response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

              // Log the failed attempt
              loginAuditService.logLoginAttempt(username, clientIp, false, exception.getMessage());

              // Create error response
              Api<Void> errorResponse =
                  new Api<>("failure", exception.getMessage(), Optional.empty(), Optional.empty());

              try {
                // Convert to bytes and write response
                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);

                // Update session with attempt count
                return webFilterExchange
                    .getExchange()
                    .getSession()
                    .flatMap(
                        session -> {
                          Integer attempts = session.getAttribute(SecurityConstants.ATTEMPTS_KEY);
                          attempts = (attempts == null) ? 1 : attempts + 1;
                          session.getAttributes().put(SecurityConstants.ATTEMPTS_KEY, attempts);

                          if (attempts >= securityConstants.getMaxAttempts()) {
                            session
                                .getAttributes()
                                .put(
                                    SecurityConstants.BLOCKED_UNTIL_KEY,
                                    System.currentTimeMillis()
                                        + (securityConstants.getBlockDurationSeconds() * 1000));
                          }

                          return response.writeWith(Mono.just(buffer));
                        });
              } catch (Exception e) {
                log.error("Error writing authentication failure response", e);
                return Mono.error(e);
              }
            });
  }

  private Mono<String> getAttemptedUsername(WebFilterExchange webFilterExchange) {
    var exchange = webFilterExchange.getExchange();

    // Try to get username from exchange attributes (set during authentication attempt)
    String username = exchange.getAttribute("username");
    if (username != null && !username.trim().isEmpty()) {
      return Mono.just(username);
    }

    // Try to get from session
    return exchange
        .getSession()
        .map(session -> session.getAttribute(SecurityConstants.LAST_USERNAME_KEY))
        .map(
            sessionUsername ->
                sessionUsername != null ? sessionUsername.toString() : "unknown_user")
        .defaultIfEmpty("unknown_user");
  }
}
