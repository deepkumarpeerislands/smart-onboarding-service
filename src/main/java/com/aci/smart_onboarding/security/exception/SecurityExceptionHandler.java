package com.aci.smart_onboarding.security.exception;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class SecurityExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  public Mono<ResponseEntity<Api<Object>>> handleAccessDeniedException(AccessDeniedException ex) {
    Api<Object> response =
        new Api<>(BrdConstants.FAILURE, ex.getMessage(), Optional.empty(), Optional.empty());
    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
  }

  @ExceptionHandler(AuthenticationException.class)
  public Mono<ResponseEntity<Api<Object>>> handleAuthenticationException(
      AuthenticationException ex) {
    Api<Object> response =
        new Api<>(
            BrdConstants.FAILURE,
            "Authentication failed. Please provide valid credentials.",
            Optional.empty(),
            Optional.empty());
    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Api<Object>>> handleGenericException(Exception ex) {
    Api<Object> response =
        new Api<>(
            BrdConstants.FAILURE,
            "Internal Server Error: " + ex.getMessage(),
            Optional.empty(),
            Optional.empty());
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
  }
}
