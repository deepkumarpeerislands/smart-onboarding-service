package com.aci.smart_onboarding.security.handler;

import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.service.LoginAuditService;
import com.aci.smart_onboarding.service.RedisSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final LoginAuditService loginAuditService;
  private final RedisSessionService redisSessionService;

  @Override
  public Mono<Void> onAuthenticationSuccess(
      WebFilterExchange webFilterExchange, Authentication authentication) {
    String username = authentication.getName();

    // Get all roles from authentication
    List<String> authRoles =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    // Get active role (first role in the list)
    String activeRole =
        authRoles.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No roles found for user: " + username));

    // Safely get client IP with null checks and fallback
    String clientIp =
        Optional.ofNullable(webFilterExchange)
            .map(WebFilterExchange::getExchange)
            .map(ServerWebExchange::getRequest)
            .map(ServerHttpRequest::getRemoteAddress)
            .map(InetSocketAddress::getAddress)
            .map(InetAddress::getHostAddress)
            .orElse("unknown");

    return Objects.requireNonNull(webFilterExchange)
        .getExchange()
        .getSession()
        .flatMap(WebSession::invalidate)
        .then(userRepository.findByEmail(username))
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(String.format("User not found with email: %s", username))))
        .flatMap(
            user -> {
              String jti = redisSessionService.generateJti();

              return redisSessionService
                  .createSession(username, jti, activeRole, authRoles)
                  .then(
                      Mono.fromCallable(
                          () -> {
                            // Generate token with all roles and active role
                            String token =
                                jwtService.generateToken(username, authRoles, activeRole, jti);

                            UserInfo userInfo =
                                UserInfo.builder()
                                    .username(username)
                                    .firstName(user.getFirstName())
                                    .lastName(user.getLastName())
                                    .activeRole(activeRole)
                                    .roles(authRoles)
                                    .token(token)
                                    .email(username)
                                    .build();

                            var response = webFilterExchange.getExchange().getResponse();
                            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                            byte[] bytes = objectMapper.writeValueAsBytes(userInfo);
                            return response.bufferFactory().wrap(bytes);
                          }))
                  .flatMap(
                      buffer ->
                          webFilterExchange
                              .getExchange()
                              .getResponse()
                              .writeWith(Mono.just(buffer)))
                  .doOnSuccess(
                      v ->
                          loginAuditService.logLoginAttempt(
                              username, clientIp, true, "Authentication successful"))
                  .onErrorResume(
                      e -> {
                        log.error("Error in authentication success handler: {}", e.getMessage());
                        loginAuditService.logLoginAttempt(
                            username, clientIp, false, e.getMessage());
                        return Mono.error(e);
                      });
            })
        .onErrorResume(
            e -> {
              log.error("Error in authentication success handler: {}", e.getMessage());
              loginAuditService.logLoginAttempt(username, clientIp, false, e.getMessage());
              return Mono.error(e);
            });
  }
}
