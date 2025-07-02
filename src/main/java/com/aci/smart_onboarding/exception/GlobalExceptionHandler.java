package com.aci.smart_onboarding.exception;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.dto.Api;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the application. Provides centralized exception handling across
 * all @RequestMapping methods. Translates exceptions into standardized API responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final String EXCEPTION_FIELD = "message";

  /**
   * Creates a standardized error details map with common fields.
   *
   * @param status HTTP status code
   * @param error Error type description
   * @param message Detailed error message
   * @return Map containing error details
   */
  private Map<String, Object> createErrorDetails(HttpStatus status, String error, String message) {
    Map<String, Object> errorDetails = new HashMap<>();
    errorDetails.put("timestamp", LocalDateTime.now());
    errorDetails.put("status", status.value());
    errorDetails.put("error", error);
    errorDetails.put(EXCEPTION_FIELD, message);
    return errorDetails;
  }

  /**
   * Creates a standardized API response for errors.
   *
   * @param status HTTP status code
   * @param error Error type description
   * @param message Detailed error message
   * @return ResponseEntity containing the API response
   */
  private Mono<ResponseEntity<Api<Map<String, Object>>>> createErrorResponse(
      HttpStatus status, String error, String message) {
    Map<String, Object> errorDetails = createErrorDetails(status, error, message);
    return Mono.just(
        ResponseEntity.status(status)
            .body(
                new Api<>(
                    BrdConstants.FAILURE, message, Optional.of(errorDetails), Optional.empty())));
  }

  @ExceptionHandler(MissingRequestValueException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleMissingRequestValueException(
      MissingRequestValueException ex) {
    String paramName = ex.getReason() != null ? ex.getReason() : "request parameter";
    String message = String.format("Required %s is missing", paramName);
    log.debug("Handling MissingRequestValueException: {}", message);
    return createErrorResponse(HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, message);
  }

  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleServerWebInputException(
      ServerWebInputException ex) {
    String message = ex.getReason() != null ? ex.getReason() : "Invalid request parameter";
    log.debug("Handling ServerWebInputException: {}", message);
    return createErrorResponse(HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, message);
  }

  @ExceptionHandler(NotFoundException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleNotFoundException(
      NotFoundException ex) {
    log.debug("Handling NotFoundException: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.NOT_FOUND, "Not Found Exception", ex.getMessage());
  }

  @ExceptionHandler(AlreadyExistException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleAlreadyExistException(
      AlreadyExistException ex) {
    log.debug("Handling AlreadyExistException: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.CONFLICT, "Already exists", ex.getMessage());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleConstraintViolationException(
      ConstraintViolationException ex) {
    String errorMessage =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .findFirst()
            .orElse(ErrorValidationMessage.VALIDATION_ERROR_MESSAGE);
    log.debug("Handling ConstraintViolationException: {}", errorMessage);
    return createErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, errorMessage);
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleWebExchangeBindException(
      WebExchangeBindException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation error");
    log.debug("Handling WebExchangeBindException: {}", message);
    return createErrorResponse(HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    String errorMessage =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .findFirst()
            .orElse("Validation error occurred");
    log.debug("Handling MethodArgumentNotValidException: {}", errorMessage);
    return createErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, errorMessage);
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleBadRequestException(
      BadRequestException ex) {
    log.debug("Handling BadRequestException: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InternalServerException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleInternalServerException(
      InternalServerException ex) {
    log.error("Handling InternalServerException: {}", ex.getMessage());
    Map<String, Object> errorDetails =
        createErrorDetails(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorValidationMessage.INTERNAL_SERVER_ERROR,
            ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN);

    // Add additional error details if available
    Map<String, Object> additionalDetails = new HashMap<>();
    additionalDetails.put("errorMessage", ex.getMessage());
    if (ex.getErrorDetails() != null) {
      additionalDetails.putAll(ex.getErrorDetails());
    }
    errorDetails.put("errorDetails", additionalDetails);

    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    BrdConstants.FAILURE,
                    ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN,
                    Optional.of(errorDetails),
                    Optional.empty())));
  }

  @ExceptionHandler(AIServiceException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleAIServiceException(
      AIServiceException ex) {
    log.error("Handling AIServiceException: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "AI Service Exception", ex.getMessage());
  }

  @ExceptionHandler(ExpiredJwtException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleExpiredJwtException(
      ExpiredJwtException ex) {
    log.error("JWT token expired: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Token Expired",
        "Authentication token has expired. Please login again.");
  }

  @ExceptionHandler(BadCredentialsException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleBadCredentialsException(
      BadCredentialsException ex) {
    log.error("Bad credentials: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials");
  }

  @ExceptionHandler(AuthenticationException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleAuthenticationException(
      AuthenticationException ex) {
    log.error("Authentication failed: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication failed");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleAccessDeniedException(
      AccessDeniedException ex) {
    log.error("Access denied: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.FORBIDDEN, "Access Denied", "Access Denied");
  }

  @ExceptionHandler(AccountBlockedException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleAccountBlockedException(
      AccountBlockedException ex) {
    log.error("Account blocked: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.FORBIDDEN, ErrorValidationMessage.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleSecurityException(
      SecurityException ex) {
    log.error("Security exception: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.FORBIDDEN, ErrorValidationMessage.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleIllegalStateException(
      IllegalStateException ex) {
    log.error("Illegal state: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.FORBIDDEN,
        ErrorValidationMessage.FORBIDDEN,
        "Session error: " + ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    log.error("Illegal argument: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(JsonFileValidationException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleJsonFileValidationException(
      JsonFileValidationException ex) {
    log.warn("JSON file validation failed: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.BAD_REQUEST, "JSON File Validation Error", ex.getMessage());
  }

  @ExceptionHandler(NullPointerException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleNullPointerException(
      NullPointerException ex) {
    log.error("Null pointer exception", ex);
    return createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorValidationMessage.INTERNAL_SERVER_ERROR,
        ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN);
  }

  @ExceptionHandler(RuntimeException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleRuntimeException(
      RuntimeException ex) {
    log.error("Runtime exception", ex);
    return createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorValidationMessage.INTERNAL_SERVER_ERROR,
        ErrorValidationMessage.UNEXPECTED_ERROR_MESSAGE_TRY_AGAIN);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public Mono<ResponseEntity<Api<Map<String, Object>>>> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex) {

    String errorMessage =
        ex.getAllValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(
                error -> {
                  String defaultMessage = error.getDefaultMessage();
                  if (defaultMessage != null && !defaultMessage.isEmpty()) {
                    return defaultMessage;
                  }
                  return "Invalid parameter value";
                })
            .findFirst()
            .orElse("Validation error occurred");

    return createErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorValidationMessage.BAD_REQUEST, errorMessage);
  }
}
