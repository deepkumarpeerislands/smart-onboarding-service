package com.aci.smart_onboarding.security.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SecurityExceptionHandlerTest {

  private SecurityExceptionHandler securityExceptionHandler;

  @BeforeEach
  void setUp() {
    securityExceptionHandler = new SecurityExceptionHandler();
  }

  @Test
  void handleAccessDeniedException_WithErrorMessage_ShouldReturnForbiddenResponse() {
    // Given
    String errorMessage = "Access denied";
    AccessDeniedException exception = new AccessDeniedException(errorMessage);

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAccessDeniedException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(errorMessage, response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleAccessDeniedException_WithEmptyMessage_ShouldReturnForbiddenResponse() {
    // Given
    AccessDeniedException exception = new AccessDeniedException("");

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAccessDeniedException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleAuthenticationException_WithErrorMessage_ShouldReturnUnauthorizedResponse() {
    // Given
    String errorMessage = "Authentication failed";
    AuthenticationException exception = new AuthenticationException(errorMessage) {};

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAuthenticationException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Authentication failed. Please provide valid credentials.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleAuthenticationException_WithEmptyMessage_ShouldReturnUnauthorizedResponse() {
    // Given
    AuthenticationException exception = new AuthenticationException("") {};

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAuthenticationException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Authentication failed. Please provide valid credentials.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleAuthenticationException_WithBadCredentials_ShouldReturnUnauthorizedResponse() {
    // Given
    String errorMessage = "Invalid credentials";
    BadCredentialsException exception = new BadCredentialsException(errorMessage);

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAuthenticationException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Authentication failed. Please provide valid credentials.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void
      handleAuthenticationException_WithInsufficientAuthentication_ShouldReturnUnauthorizedResponse() {
    // Given
    String errorMessage = "Insufficient authentication";
    InsufficientAuthenticationException exception =
        new InsufficientAuthenticationException(errorMessage);

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleAuthenticationException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Authentication failed. Please provide valid credentials.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleGenericException_WithErrorMessage_ShouldReturnInternalServerError() {
    // Given
    String errorMessage = "Something went wrong";
    Exception exception = new Exception(errorMessage);

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleGenericException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Internal Server Error: " + errorMessage, response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleGenericException_WithNullMessage_ShouldReturnInternalServerError() {
    // Given
    Exception exception = new Exception();

    // When
    Mono<ResponseEntity<Api<Object>>> responseMono =
        securityExceptionHandler.handleGenericException(exception);

    // Then
    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals("Internal Server Error: null", response.getBody().getMessage());
            })
        .verifyComplete();
  }
}
