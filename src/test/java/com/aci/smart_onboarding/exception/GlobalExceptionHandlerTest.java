package com.aci.smart_onboarding.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.dto.Api;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

@DisplayName("GlobalExceptionHandlerTest")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Nested
  @DisplayName("Request Value Exception Tests")
  class RequestValueExceptionTests {

    @Test
    @DisplayName("Should handle MissingRequestValueException with reason")
    void handleMissingRequestValueException_WithReason() {
      // Arrange
      String reason = "user ID";
      MissingRequestValueException ex = mock(MissingRequestValueException.class);
      when(ex.getReason()).thenReturn(reason);

      // Act & Assert
      StepVerifier.create(exceptionHandler.handleMissingRequestValueException(ex))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                Api<Map<String, Object>> body = response.getBody();
                assertEquals(BrdConstants.FAILURE, body.getStatus());
                String expectedMessage = "Required user ID is missing";
                assertEquals(expectedMessage, body.getMessage());
                assertTrue(body.getData().isPresent());
                Map<String, Object> errorDetails = body.getData().get();
                assertEquals(HttpStatus.BAD_REQUEST.value(), errorDetails.get("status"));
                assertEquals(ErrorValidationMessage.BAD_REQUEST, errorDetails.get("error"));
                assertEquals(expectedMessage, errorDetails.get("message"));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle MissingRequestValueException without reason")
    void handleMissingRequestValueException_WithoutReason() {
      // Arrange
      MissingRequestValueException ex = mock(MissingRequestValueException.class);
      when(ex.getReason()).thenReturn(null);

      // Act & Assert
      StepVerifier.create(exceptionHandler.handleMissingRequestValueException(ex))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                Api<Map<String, Object>> body = response.getBody();
                assertEquals("Required request parameter is missing", body.getMessage());
                return true;
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Server Web Input Tests")
  class ServerWebInputTests {

    @Test
    @DisplayName("Should handle ServerWebInputException with reason")
    void handleServerWebInputException_WithReason() {
      // Arrange
      ServerWebInputException ex = mock(ServerWebInputException.class);
      when(ex.getReason()).thenReturn("Invalid input");

      // Act & Assert
      StepVerifier.create(exceptionHandler.handleServerWebInputException(ex))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                Api<Map<String, Object>> body = response.getBody();
                assertEquals("Invalid input", body.getMessage());
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle ServerWebInputException without reason")
    void handleServerWebInputException_WithoutReason() {
      // Arrange
      ServerWebInputException ex = mock(ServerWebInputException.class);
      when(ex.getReason()).thenReturn(null);

      // Act & Assert
      StepVerifier.create(exceptionHandler.handleServerWebInputException(ex))
          .expectNextMatches(
              response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                Api<Map<String, Object>> body = response.getBody();
                assertEquals("Invalid request parameter", body.getMessage());
                return true;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle NotFoundException")
  void handleNotFoundException() {
    // Arrange
    String errorMessage = "Resource not found";
    NotFoundException ex = new NotFoundException(errorMessage);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleNotFoundException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertEquals(errorMessage, body.getMessage());
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.NOT_FOUND.value(), errorDetails.get("status"));
              assertEquals("Not Found Exception", errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle AlreadyExistException")
  void handleAlreadyExistException() {
    // Arrange
    String errorMessage = "Resource already exists";
    AlreadyExistException ex = new AlreadyExistException(errorMessage);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleAlreadyExistException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertEquals(errorMessage, body.getMessage());
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.CONFLICT.value(), errorDetails.get("status"));
              assertEquals("Already exists", errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle ConstraintViolationException")
  void handleConstraintViolationException() {
    // Arrange
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
    when(violation.getPropertyPath().toString()).thenReturn("email");
    when(violation.getMessage()).thenReturn("must be valid");
    violations.add(violation);

    ConstraintViolationException ex =
        new ConstraintViolationException("Validation failed", violations);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleConstraintViolationException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertEquals("email: must be valid", body.getMessage());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle WebExchangeBindException")
  void handleWebExchangeBindException() {
    // Arrange
    WebExchangeBindException ex = mock(WebExchangeBindException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("test", "field", "must not be blank");
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleWebExchangeBindException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.BAD_REQUEST.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.BAD_REQUEST, errorDetails.get("error"));
              assertEquals("field: must not be blank", errorDetails.get("message"));
              assertNotNull(errorDetails.get("timestamp"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle MethodArgumentNotValidException")
  void handleMethodArgumentNotValidException() {
    // Arrange
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    List<FieldError> fieldErrors = new ArrayList<>();
    fieldErrors.add(new FieldError("object", "field", "error message"));

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleMethodArgumentNotValidException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertEquals("field: error message", body.getMessage());
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BadRequestException")
  void handleBadRequestException() {
    // Arrange
    String errorMessage = "Invalid request";
    BadRequestException ex = new BadRequestException(errorMessage);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleBadRequestException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertEquals(errorMessage, body.getMessage());
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.BAD_REQUEST.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.BAD_REQUEST, errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle InternalServerException")
  void handleInternalServerException() {
    // Arrange
    String errorMessage = "Internal error occurred";
    Map<String, String> additionalDetails = new HashMap<>();
    additionalDetails.put("detail", "Additional error info");
    InternalServerException ex = new InternalServerException(errorMessage, additionalDetails);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleInternalServerException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.INTERNAL_SERVER_ERROR, errorDetails.get("error"));
              assertEquals(
                  ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN,
                  errorDetails.get("message"));

              @SuppressWarnings("unchecked")
              Map<String, Object> nestedErrorDetails =
                  (Map<String, Object>) errorDetails.get("errorDetails");
              assertNotNull(nestedErrorDetails);
              assertEquals(errorMessage, nestedErrorDetails.get("errorMessage"));
              assertEquals("Additional error info", nestedErrorDetails.get("detail"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle generic Exception")
  void handleGenericException() {
    // Arrange
    RuntimeException ex = new RuntimeException("Unexpected error");

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleRuntimeException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.INTERNAL_SERVER_ERROR, errorDetails.get("error"));
              assertEquals(
                  ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN,
                  errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle AIServiceException")
  void handleAIServiceException() {
    // Arrange
    String errorMessage = "AI service error";
    AIServiceException ex = new AIServiceException(errorMessage);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleAIServiceException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorDetails.get("status"));
              assertEquals("AI Service Exception", errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle ExpiredJwtException")
  void handleExpiredJwtException() {
    // Arrange
    ExpiredJwtException ex = mock(ExpiredJwtException.class);

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleExpiredJwtException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.UNAUTHORIZED.value(), errorDetails.get("status"));
              assertEquals("Token Expired", errorDetails.get("error"));
              assertEquals(
                  "Authentication token has expired. Please login again.",
                  errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle AccessDeniedException")
  void handleAccessDeniedException() {
    // Arrange
    AccessDeniedException ex = new AccessDeniedException("Access denied");

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleAccessDeniedException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.FORBIDDEN.value(), errorDetails.get("status"));
              assertEquals("Access Denied", errorDetails.get("error"));
              assertEquals("Access Denied", errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BadCredentialsException")
  void handleBadCredentialsException() {
    // Arrange
    BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleBadCredentialsException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.UNAUTHORIZED.value(), errorDetails.get("status"));
              assertEquals("Unauthorized", errorDetails.get("error"));
              assertEquals("Invalid credentials", errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle AuthenticationException")
  void handleAuthenticationException() {
    // Arrange
    AuthenticationException ex = mock(AuthenticationException.class);
    when(ex.getMessage()).thenReturn("Authentication failed");

    // Act & Assert
    StepVerifier.create(exceptionHandler.handleAuthenticationException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.UNAUTHORIZED.value(), errorDetails.get("status"));
              assertEquals("Unauthorized", errorDetails.get("error"));
              assertEquals("Authentication failed", errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle IllegalStateException")
  void handleIllegalStateException() {
    String errorMessage = "Session expired";
    IllegalStateException ex = new IllegalStateException(errorMessage);

    StepVerifier.create(exceptionHandler.handleIllegalStateException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.FORBIDDEN.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.FORBIDDEN, errorDetails.get("error"));
              assertEquals("Session error: " + errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle IllegalArgumentException")
  void handleIllegalArgumentException() {
    String errorMessage = "Invalid argument";
    IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

    StepVerifier.create(exceptionHandler.handleIllegalArgumentException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.BAD_REQUEST.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.BAD_REQUEST, errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle AccountBlockedException")
  void handleAccountBlockedException() {
    String errorMessage = "Account is blocked";
    AccountBlockedException ex = new AccountBlockedException(errorMessage);

    StepVerifier.create(exceptionHandler.handleAccountBlockedException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.FORBIDDEN.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.FORBIDDEN, errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle RuntimeException")
  void handleRuntimeException() {
    RuntimeException ex = new RuntimeException("Some runtime error");

    StepVerifier.create(exceptionHandler.handleRuntimeException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.INTERNAL_SERVER_ERROR, errorDetails.get("error"));
              assertEquals(
                  ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN,
                  errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle NullPointerException")
  void handleNullPointerException() {
    NullPointerException ex = new NullPointerException();

    StepVerifier.create(exceptionHandler.handleNullPointerException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.INTERNAL_SERVER_ERROR, errorDetails.get("error"));
              assertEquals(
                  ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN,
                  errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle SecurityException")
  void handleSecurityException() {
    String errorMessage = "Security violation";
    SecurityException ex = new SecurityException(errorMessage);

    StepVerifier.create(exceptionHandler.handleSecurityException(ex))
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              Api<Map<String, Object>> body = response.getBody();
              assertNotNull(body);
              assertTrue(body.getData().isPresent());
              Map<String, Object> errorDetails = body.getData().get();
              assertEquals(HttpStatus.FORBIDDEN.value(), errorDetails.get("status"));
              assertEquals(ErrorValidationMessage.FORBIDDEN, errorDetails.get("error"));
              assertEquals(errorMessage, errorDetails.get("message"));
              return true;
            })
        .verifyComplete();
  }
}
