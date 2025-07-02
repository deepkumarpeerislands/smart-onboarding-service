package com.aci.smart_onboarding.security.config;

import com.aci.smart_onboarding.constants.ApiPaths;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.LoginRequest;
import com.aci.smart_onboarding.security.filter.BruteForceProtectionFilter;
import com.aci.smart_onboarding.security.filter.JwtAuthenticationFilter;
import com.aci.smart_onboarding.security.handler.AuthenticationFailureHandler;
import com.aci.smart_onboarding.security.handler.AuthenticationSuccessHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import reactor.core.publisher.Mono;

@Slf4j
@EnableWebFluxSecurity
public abstract class BaseSecurityConfig {
  protected final JwtAuthenticationFilter jwtAuthenticationFilter;
  protected final BruteForceProtectionFilter bruteForceProtectionFilter;
  protected final AuthenticationSuccessHandler authenticationSuccessHandler;
  protected final AuthenticationFailureHandler authenticationFailureHandler;
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected BaseSecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      BruteForceProtectionFilter bruteForceProtectionFilter,
      AuthenticationSuccessHandler authenticationSuccessHandler,
      AuthenticationFailureHandler authenticationFailureHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.bruteForceProtectionFilter = bruteForceProtectionFilter;
    this.authenticationSuccessHandler = authenticationSuccessHandler;
    this.authenticationFailureHandler = authenticationFailureHandler;
  }

  @Bean
  public ServerSecurityContextRepository securityContextRepository() {
    return new WebSessionServerSecurityContextRepository();
  }

  protected SecurityWebFilterChain configureSecurity(
      ServerHttpSecurity http, ReactiveAuthenticationManager authenticationManager) {

    // Create and configure the authentication filter
    AuthenticationWebFilter authenticationFilter =
        new AuthenticationWebFilter(authenticationManager);
    authenticationFilter.setRequiresAuthenticationMatcher(
        ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, ApiPaths.AUTH_LOGIN));
    authenticationFilter.setSecurityContextRepository(securityContextRepository());
    authenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
    authenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler);

    // Configure the server authentication converter
    authenticationFilter.setServerAuthenticationConverter(
        exchange ->
            exchange
                .getRequest()
                .getBody()
                .next()
                .flatMap(
                    dataBuffer -> {
                      try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        LoginRequest loginRequest =
                            objectMapper.readValue(bytes, LoginRequest.class);

                        // Store username in exchange attributes for failure handling
                        exchange.getAttributes().put("username", loginRequest.getUsername());

                        if (loginRequest.getUsername() == null
                            || loginRequest.getPassword() == null) {
                          return Mono.error(
                              new BadCredentialsException("Email and password cannot be empty"));
                        }

                        Authentication auth =
                            new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(), loginRequest.getPassword());
                        return Mono.just(auth);
                      } catch (Exception e) {
                        return Mono.error(new BadCredentialsException(e.getMessage()));
                      }
                    })
                .switchIfEmpty(
                    Mono.error(new BadCredentialsException("Request body cannot be empty"))));

    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(
            corsSpec ->
                corsSpec.configurationSource(
                    request -> {
                      CorsConfiguration config = new CorsConfiguration();
                      config.addAllowedOrigin("*");
                      config.addAllowedMethod("*");
                      config.addAllowedHeader("*");
                      return config;
                    }))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers(HttpMethod.POST, ApiPaths.AUTH_LOGIN)
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/auth/request-password-reset")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/auth/reset-password")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, ApiPaths.AUTH_ME)
                    .authenticated()
                    .pathMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    .pathMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/configuration/**",
                        "/swagger-config/**",
                        "/swagger-ui/index.html",
                        "/swagger-ui/swagger-ui.css",
                        "/swagger-ui/swagger-ui-bundle.js",
                        "/swagger-ui/swagger-ui-standalone-preset.js",
                        "/swagger-ui/swagger-initializer.js")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(
                    (exchange, ex) -> {
                      log.error("Authentication error: {}", ex.getMessage());
                      ServerHttpResponse response = exchange.getResponse();
                      response.setStatusCode(HttpStatus.UNAUTHORIZED);
                      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                      Api<?> errorResponse =
                          new Api<>(
                              "failure",
                              "Authentication failed: " + ex.getMessage(),
                              Optional.empty(),
                              Optional.of(Map.of("error", ex.getMessage())));
                      try {
                        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
                        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
                      } catch (JsonProcessingException e) {
                        log.error("Error writing error response: {}", e.getMessage());
                        return Mono.error(e);
                      }
                    }))
        .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .addFilterAt(bruteForceProtectionFilter, SecurityWebFiltersOrder.FIRST)
        .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .securityContextRepository(securityContextRepository())
        .build();
  }
}
