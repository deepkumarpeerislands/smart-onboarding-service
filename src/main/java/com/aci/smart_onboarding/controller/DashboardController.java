package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.dto.AdditionalFactorsResponse;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdAiPrefillAccuracyResponse;
import com.aci.smart_onboarding.dto.BrdAiPrefillRateResponse;
import com.aci.smart_onboarding.dto.BrdSnapshotMetricsResponse;
import com.aci.smart_onboarding.dto.BrdStatusCountResponse;
import com.aci.smart_onboarding.dto.BrdStatusTransitionTimeResponse;
import com.aci.smart_onboarding.dto.BrdTypeCountResponse;
import com.aci.smart_onboarding.dto.BrdUploadMetricsResponse;
import com.aci.smart_onboarding.dto.BrdVerticalCountResponse;
import com.aci.smart_onboarding.dto.CommentGroupStatsResponse;
import com.aci.smart_onboarding.dto.CommentResolutionTimeResponse;
import com.aci.smart_onboarding.dto.UnresolvedCommentGroupsCountResponse;
import com.aci.smart_onboarding.exception.IllegalParameterException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IDashboardService;
import com.aci.smart_onboarding.swagger.DashboardRequestAndReponse.Descriptions;
import com.aci.smart_onboarding.swagger.DashboardRequestAndReponse.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Controller for dashboard related endpoints. */
@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(value = "${api.default.path}/dashboard", name = "Dashboard Management")
@Tag(name = "Dashboard", description = "Dashboard API endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

  private final IDashboardService dashboardService;
  private final BRDSecurityService securityService;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Determines the effective scope based on user role. For Manager role, always use "team" scope
   * regardless of input. For PM role, use the requested scope or default to "me".
   *
   * @param requestedScope The scope requested in the API call
   * @param role The user's role
   * @return The effective scope to use
   */
  private String determineEffectiveScope(String requestedScope, String role) {
    // For managers, always use team scope
    if (DashboardConstants.MANAGER_ROLE.equals(role)) {
      log.debug(
          "User has MANAGER role, defaulting scope to 'team' regardless of requested scope: {}",
          requestedScope);
      return DashboardConstants.SCOPE_TEAM;
    }

    // For PMs, use requested scope or default to "me"
    return requestedScope != null ? requestedScope : DashboardConstants.SCOPE_ME;
  }

  @GetMapping(value = "/brds-by-status", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get BRD counts by status")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BRD counts"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM or Manager"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdStatusCountResponse>>> getBrdCounts(
      @Parameter(
              description =
                  "Scope filter - 'me' or 'team'. For MANAGER role, always defaults to 'team'")
          @RequestParam(name = "scope", required = false, defaultValue = "me")
          String scope) {

    // Validate the input scope
    if (scope != null && !DashboardConstants.VALID_SCOPES.contains(scope)) {
      String message =
          String.format(
              DashboardConstants.INVALID_PARAM,
              scope,
              String.join(", ", DashboardConstants.VALID_SCOPES));
      log.warn(message);
      return Mono.just(
          ResponseEntity.<Api<BrdStatusCountResponse>>status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      DashboardConstants.ERROR,
                      message,
                      Optional.empty(),
                      Optional.of(Map.of(DashboardConstants.ERROR, message)))));
    }

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!(DashboardConstants.PM_ROLE.equals(role)
                              || DashboardConstants.MANAGER_ROLE.equals(role)
                              || DashboardConstants.BA_ROLE.equals(role))) {
                            log.warn(DashboardConstants.PM_MANAGER_ROLE, username);
                            return Mono.just(
                                ResponseEntity.<Api<BrdStatusCountResponse>>status(
                                        HttpStatus.FORBIDDEN)
                                    .body(
                                        new Api<>(
                                            DashboardConstants.ERROR,
                                            DashboardConstants.PM_MANAGER_ONLY_MESSAGE,
                                            Optional.empty(),
                                            Optional.of(
                                                Map.of(
                                                    DashboardConstants.ERROR,
                                                    DashboardConstants.PM_MANAGER_ONLY_MESSAGE)))));
                          }

                          // Determine effective scope based on role
                          String effectiveScope = determineEffectiveScope(scope, role);
                          log.info(
                              "Processing BRD status count request for user {} with role {} and effective scope {}",
                              username,
                              role,
                              effectiveScope);

                          return dashboardService
                              .getOpenBrdsByStatus(effectiveScope, username)
                              .map(
                                  response ->
                                      ResponseEntity.ok(
                                          new Api<>(
                                              DashboardConstants.SUCCESS,
                                              DashboardConstants.BRD_STATUS_SUCCESS_MESSAGE,
                                              Optional.of(response),
                                              Optional.empty())))
                              .onErrorResume(
                                  IllegalParameterException.class,
                                  e ->
                                      Mono.just(
                                          ResponseEntity.<Api<BrdStatusCountResponse>>status(
                                                  HttpStatus.BAD_REQUEST)
                                              .body(
                                                  new Api<>(
                                                      DashboardConstants.ERROR,
                                                      e.getMessage(),
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              DashboardConstants.ERROR,
                                                              e.getMessage()))))))
                              .onErrorResume(
                                  e ->
                                      Mono.just(
                                          ResponseEntity.<Api<BrdStatusCountResponse>>status(
                                                  HttpStatus.INTERNAL_SERVER_ERROR)
                                              .body(
                                                  new Api<>(
                                                      DashboardConstants.ERROR,
                                                      e.getMessage() != null
                                                          ? e.getMessage()
                                                          : DashboardConstants.ERROR_MESSAGE,
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              DashboardConstants.ERROR,
                                                              e.getMessage() != null
                                                                  ? e.getMessage()
                                                                  : "Unknown error"))))));
                        }));
  }

  @GetMapping(value = "/brds-by-vertical", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get BRDs count grouped by industry vertical")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BRDs by vertical"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM or Manager"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> getBrdsByVertical(
      @Parameter(
              description =
                  "Scope filter - 'me' or 'team'. For MANAGER role, always defaults to 'team'")
          @RequestParam(name = "scope", required = false, defaultValue = "me")
          String scope,
      @Parameter(description = "BRD scope filter - 'open' or 'all'")
          @RequestParam(name = "brdScope", required = false, defaultValue = "open")
          String brdScope,
      @Parameter(
              description =
                  "Period filter - 'month', 'quarter', or 'year' (required when brdScope is 'all')")
          @RequestParam(name = "period", required = false)
          String period) {

    // Validate input scope separately
    if (scope != null && !DashboardConstants.VALID_SCOPES.contains(scope)) {
      String message =
          String.format(
              DashboardConstants.INVALID_PARAM,
              scope,
              String.join(", ", DashboardConstants.VALID_SCOPES));
      log.warn(message);
      return Mono.just(
          ResponseEntity.<Api<BrdVerticalCountResponse>>status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      DashboardConstants.ERROR,
                      message,
                      Optional.empty(),
                      Optional.of(Map.of(DashboardConstants.ERROR, message)))));
    }

    // Validate the input parameters except scope (which will be handled by role-based logic)
    validateInputParameters(brdScope, period);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Check if user has either PM or Manager role
                          if (!(DashboardConstants.PM_ROLE.equals(role)
                              || DashboardConstants.MANAGER_ROLE.equals(role)
                              || DashboardConstants.BA_ROLE.equals(role))) {
                            log.warn(DashboardConstants.PM_MANAGER_ROLE, username);
                            return handleUnauthorizedAccess(username);
                          }

                          // Determine effective scope based on role
                          String effectiveScope = determineEffectiveScope(scope, role);

                          // Log request details and process
                          log.info(
                              "Processing BRD verticals request for user {} with role {} and effective scope {}, brdScope {} and period {}",
                              username,
                              role,
                              effectiveScope,
                              brdScope,
                              period);

                          return dashboardService
                              .getBrdsByVertical(effectiveScope, brdScope, period, username)
                              .map(this::createSuccessResponse)
                              .onErrorResume(
                                  IllegalParameterException.class, this::handleValidationError)
                              .onErrorResume(this::handleGenericError);
                        }));
  }

  /**
   * Validates the input parameters for the getBrdsByVertical endpoint. Note: Scope validation is
   * handled separately through determineEffectiveScope.
   */
  private void validateInputParameters(String brdScope, String period) {
    // Scope validation is now handled separately
    // Only validate brdScope and period

    validateScopeParameter(brdScope, DashboardConstants.VALID_BRD_SCOPES);

    // Check if period is required but missing
    if (DashboardConstants.BRD_SCOPE_ALL.equals(brdScope) && (period == null || period.isEmpty())) {
      throw new IllegalParameterException(DashboardConstants.PERIOD_REQUIRED_MESSAGE);
    }

    // Validate period if provided
    if (period != null && !period.isEmpty()) {
      validateScopeParameter(period, DashboardConstants.VALID_PERIODS);
    }
  }

  /** Creates a success response with the BRD vertical count data. */
  private ResponseEntity<Api<BrdVerticalCountResponse>> createSuccessResponse(
      BrdVerticalCountResponse response) {
    return ResponseEntity.ok(
        new Api<>(
            "SUCCESS",
            DashboardConstants.BRD_VERTICAL_SUCCESS_MESSAGE,
            Optional.of(response),
            Optional.empty()));
  }

  /** Handles the case when a user is not authorized as PM or Manager. */
  private Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> handleUnauthorizedAccess(
      String username) {
    log.warn(
        "Access denied for user {}. This operation is allowed for PMs and Managers only.",
        username);
    return Mono.just(
        ResponseEntity.<Api<BrdVerticalCountResponse>>status(HttpStatus.FORBIDDEN)
            .body(
                new Api<>(
                    DashboardConstants.ERROR,
                    DashboardConstants.PM_MANAGER_ONLY_MESSAGE,
                    Optional.empty(),
                    Optional.of(
                        Map.of(
                            DashboardConstants.ERROR,
                            DashboardConstants.PM_MANAGER_ONLY_MESSAGE)))));
  }

  /** Handles validation errors. */
  private Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> handleValidationError(
      IllegalParameterException e) {
    return Mono.just(
        ResponseEntity.<Api<BrdVerticalCountResponse>>status(HttpStatus.BAD_REQUEST)
            .body(
                new Api<>(
                    DashboardConstants.ERROR,
                    e.getMessage(),
                    Optional.empty(),
                    Optional.of(Map.of("error", e.getMessage())))));
  }

  /** Handles generic errors. */
  private Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> handleGenericError(Throwable e) {
    String errorMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";
    return Mono.just(
        ResponseEntity.<Api<BrdVerticalCountResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    "ERROR",
                    errorMessage,
                    Optional.empty(),
                    Optional.of(Map.of(DashboardConstants.ERROR, errorMessage)))));
  }

  /**
   * Gets username from the security context.
   *
   * @return Mono containing the username
   */
  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName);
  }

  private void validateScopeParameter(String value, List<String> validValues) {
    if (!validValues.contains(value)) {
      String message =
          String.format(DashboardConstants.INVALID_PARAM, value, String.join(", ", validValues));
      log.warn(message);
      throw new IllegalParameterException(message);
    }
  }

  @GetMapping(value = "/additional-factors", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get Walletron and ACH factor statistics")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved factor statistics"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM or Manager"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<AdditionalFactorsResponse>>> getAdditionalFactors(
      @Parameter(
              description =
                  "Scope filter - 'me' or 'team'. For MANAGER role, always defaults to 'team'")
          @RequestParam(name = "scope", required = false, defaultValue = "me")
          String scope,
      @Parameter(description = "BRD scope filter - 'open' or 'all'")
          @RequestParam(name = "brdScope", required = false, defaultValue = "open")
          String brdScope,
      @Parameter(
              description =
                  "Period filter - 'month', 'quarter', or 'year' (required when brdScope is 'all')")
          @RequestParam(name = "period", required = false)
          String period) {

    // Validate input scope separately
    if (scope != null && !DashboardConstants.VALID_SCOPES.contains(scope)) {
      String message =
          String.format(
              DashboardConstants.INVALID_PARAM,
              scope,
              String.join(", ", DashboardConstants.VALID_SCOPES));
      log.warn(message);
      return Mono.just(
          ResponseEntity.<Api<AdditionalFactorsResponse>>status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      DashboardConstants.ERROR,
                      message,
                      Optional.empty(),
                      Optional.of(Map.of(DashboardConstants.ERROR, message)))));
    }

    // Validate the input parameters except scope (which will be handled by role-based logic)
    validateInputParameters(brdScope, period);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Check if user has either PM or Manager role
                          if (!(DashboardConstants.PM_ROLE.equals(role)
                              || DashboardConstants.MANAGER_ROLE.equals(role)
                              || DashboardConstants.BA_ROLE.equals(role))) {
                            log.warn(DashboardConstants.PM_MANAGER_ROLE, username);
                            return handleUnauthorizedAccessGeneric(username);
                          }

                          // Determine effective scope based on role
                          String effectiveScope = determineEffectiveScope(scope, role);

                          // Log request details and process
                          log.info(
                              "Processing additional factors request for user {} with role {} and effective scope {}, brdScope {} and period {}",
                              username,
                              role,
                              effectiveScope,
                              brdScope,
                              period);

                          return dashboardService
                              .getAdditionalFactors(effectiveScope, brdScope, period, username)
                              .map(this::createAdditionalFactorsSuccessResponse)
                              .onErrorResume(
                                  IllegalParameterException.class,
                                  this::handleValidationErrorGeneric)
                              .onErrorResume(this::handleGenericErrorGeneric);
                        }));
  }

  /** Creates a success response with the additional factors data. */
  private ResponseEntity<Api<AdditionalFactorsResponse>> createAdditionalFactorsSuccessResponse(
      AdditionalFactorsResponse response) {
    return ResponseEntity.ok(
        new Api<>(
            DashboardConstants.SUCCESS,
            DashboardConstants.ADDITIONAL_FACTORS_SUCCESS_MESSAGE,
            Optional.of(response),
            Optional.empty()));
  }

  /** Generic version of handleUnauthorizedAccess for any response type. */
  private <T> Mono<ResponseEntity<Api<T>>> handleUnauthorizedAccessGeneric(String username) {
    log.warn(
        "Access denied for user {}. This operation is allowed for PMs and Managers only.",
        username);
    return Mono.just(
        ResponseEntity.<Api<T>>status(HttpStatus.FORBIDDEN)
            .body(
                new Api<>(
                    DashboardConstants.ERROR,
                    DashboardConstants.PM_MANAGER_ONLY_MESSAGE,
                    Optional.empty(),
                    Optional.of(
                        Map.of(
                            DashboardConstants.ERROR,
                            DashboardConstants.PM_MANAGER_ONLY_MESSAGE)))));
  }

  /** Generic version of handleValidationError for any response type. */
  private <T> Mono<ResponseEntity<Api<T>>> handleValidationErrorGeneric(
      IllegalParameterException e) {
    return Mono.just(
        ResponseEntity.<Api<T>>status(HttpStatus.BAD_REQUEST)
            .body(
                new Api<>(
                    DashboardConstants.ERROR,
                    e.getMessage(),
                    Optional.empty(),
                    Optional.of(Map.of(DashboardConstants.ERROR, e.getMessage())))));
  }

  /** Generic version of handleGenericError for any response type. */
  private <T> Mono<ResponseEntity<Api<T>>> handleGenericErrorGeneric(Throwable e) {
    String errorMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";
    log.error("Error processing request: {}", errorMessage, e);
    return Mono.just(
        ResponseEntity.<Api<T>>status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    DashboardConstants.ERROR,
                    errorMessage,
                    Optional.empty(),
                    Optional.of(Map.of(DashboardConstants.ERROR, errorMessage)))));
  }

  @GetMapping(value = "/brds/snapshot/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get BRD snapshot metrics")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BRD snapshot metrics"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM or Manager"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdSnapshotMetricsResponse>>> getBrdSnapshotMetrics(
      @Parameter(
              description =
                  "Scope filter - 'me' or 'team'. For MANAGER role, always defaults to 'team'")
          @RequestParam(name = "scope", required = false, defaultValue = "me")
          String scope) {

    // Validate input scope
    if (scope != null && !DashboardConstants.VALID_SCOPES.contains(scope)) {
      String message =
          String.format(
              "Invalid parameter value: %s. Valid values are: %s",
              scope, String.join(", ", DashboardConstants.VALID_SCOPES));
      log.warn(message);
      return Mono.just(
          ResponseEntity.<Api<BrdSnapshotMetricsResponse>>status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      DashboardConstants.ERROR,
                      message,
                      Optional.empty(),
                      Optional.of(Map.of(DashboardConstants.ERROR, message)))));
    }

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Check if user has either PM or Manager role
                          if (!(DashboardConstants.PM_ROLE.equals(role)
                              || DashboardConstants.MANAGER_ROLE.equals(role)
                              || DashboardConstants.BA_ROLE.equals(role))) {
                            log.warn(
                                "User {} does not have required role (PM or Manager)", username);
                            return Mono.just(
                                ResponseEntity.<Api<BrdSnapshotMetricsResponse>>status(
                                        HttpStatus.FORBIDDEN)
                                    .body(
                                        new Api<>(
                                            DashboardConstants.ERROR,
                                            "Access denied. This operation is allowed for PMs and Managers only.",
                                            Optional.empty(),
                                            Optional.of(
                                                Map.of(
                                                    DashboardConstants.ERROR,
                                                    "Access denied. This operation is allowed for PMs and Managers only.")))));
                          }

                          // Determine effective scope based on role
                          String effectiveScope = determineEffectiveScope(scope, role);
                          log.info(
                              "Processing BRD snapshot metrics request for user {} with role {} and effective scope {}",
                              username,
                              role,
                              effectiveScope);

                          return dashboardService
                              .getBrdSnapshotMetrics(effectiveScope, username)
                              .map(
                                  response ->
                                      ResponseEntity.ok(
                                          new Api<>(
                                              DashboardConstants.SUCCESS,
                                              DashboardConstants.BRD_SNAPSHOT_SUCCESS_MESSAGE,
                                              Optional.of(response),
                                              Optional.empty())))
                              .onErrorResume(
                                  IllegalParameterException.class,
                                  e ->
                                      Mono.just(
                                          ResponseEntity.<Api<BrdSnapshotMetricsResponse>>status(
                                                  HttpStatus.BAD_REQUEST)
                                              .body(
                                                  new Api<>(
                                                      DashboardConstants.ERROR,
                                                      e.getMessage(),
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              DashboardConstants.ERROR,
                                                              e.getMessage()))))))
                              .onErrorResume(
                                  e ->
                                      Mono.just(
                                          ResponseEntity.<Api<BrdSnapshotMetricsResponse>>status(
                                                  HttpStatus.INTERNAL_SERVER_ERROR)
                                              .body(
                                                  new Api<>(
                                                      DashboardConstants.ERROR,
                                                      e.getMessage() != null
                                                          ? e.getMessage()
                                                          : DashboardConstants.ERROR_MESSAGE,
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              DashboardConstants.ERROR,
                                                              e.getMessage() != null
                                                                  ? e.getMessage()
                                                                  : "Unknown error"))))));
                        }));
  }

  @GetMapping(value = "/ai-prefill-accuracy", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get AI prefill accuracy metrics")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved AI prefill accuracy metrics"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a PM or Manager"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdAiPrefillAccuracyResponse>>> getAiPrefillAccuracy(
      @Parameter(
              description =
                  "Scope filter - 'me' or 'team'. For MANAGER role, always defaults to 'team'")
          @RequestParam(name = "scope", required = false, defaultValue = "me")
          String scope) {

    // Validate input scope
    if (scope != null && !DashboardConstants.VALID_SCOPES.contains(scope)) {
      String message =
          String.format(
              "Invalid parameter value: %s. Valid values are: %s",
              scope, String.join(", ", DashboardConstants.VALID_SCOPES));
      log.warn(message);
      return Mono.just(
          ResponseEntity.<Api<BrdAiPrefillAccuracyResponse>>status(HttpStatus.BAD_REQUEST)
              .body(
                  new Api<>(
                      DashboardConstants.ERROR,
                      message,
                      Optional.empty(),
                      Optional.of(Map.of(DashboardConstants.ERROR, message)))));
    }

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Check if user has either PM, BA, or Manager role
                          if (!(DashboardConstants.PM_ROLE.equals(role)
                              || DashboardConstants.BA_ROLE.equals(role)
                              || DashboardConstants.MANAGER_ROLE.equals(role))) {
                            log.warn(
                                "User {} does not have required role (PM, BA, or Manager)", username);
                            return handleUnauthorizedAccessGeneric(username);
                          }

                          // Determine effective scope based on role
                          String effectiveScope = determineEffectiveScope(scope, role);
                          log.info(
                              "Processing AI prefill accuracy metrics request for user {} with role {} and effective scope {}",
                              username,
                              role,
                              effectiveScope);

                          return dashboardService
                              .getAiPrefillAccuracy(effectiveScope, username)
                              .map(
                                  response ->
                                      ResponseEntity.ok(
                                          new Api<>(
                                              DashboardConstants.SUCCESS,
                                              DashboardConstants
                                                  .BRD_AI_PREFILL_ACCURACY_SUCCESS_MESSAGE,
                                              Optional.of(response),
                                              Optional.empty())))
                              .onErrorResume(
                                  IllegalParameterException.class,
                                  this::handleValidationErrorGeneric)
                              .onErrorResume(this::handleGenericErrorGeneric);
                        }));
  }

  /**
   * Get average status transition times for BRDs
   *
   * @param period The time period (month, quarter, year)
   * @return Api containing BrdStatusTransitionTimeResponse with average status transition times
   */
  @GetMapping("/average-status-transitions")
  @Operation(
      summary = "Get average BRD status transition times",
      description =
          "Returns the average time in days it takes for BRDs to transition between statuses for the specified period. This endpoint is only accessible to users with MANAGER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved average status transition times"),
        @ApiResponse(responseCode = "403", description = "User does not have manager role"),
        @ApiResponse(responseCode = "400", description = "Invalid period parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdStatusTransitionTimeResponse>>> getAverageStatusTransitionTime(
      @Parameter(
              description =
                  "Period for which to retrieve data (month, quarter, year). Defaults to 'quarter' if not provided.")
          @RequestParam(required = false)
          String period) {
    return validatePeriod(period)
        .flatMap(
            validatedPeriod ->
                validateRole(
                        List.of(DashboardConstants.MANAGER_ROLE), 
                        DashboardConstants.MANAGER_ONLY_MESSAGE)
                    .flatMap(
                        username ->
                            dashboardService
                                .getAverageStatusTransitionTime(validatedPeriod, username)
                                .map(
                                    response -> {
                                      Api<BrdStatusTransitionTimeResponse> apiResponse =
                                          new Api<>();
                                      apiResponse.setStatus(DashboardConstants.SUCCESS);
                                      apiResponse.setMessage(
                                          DashboardConstants
                                              .BRD_STATUS_TRANSITION_TIME_SUCCESS_MESSAGE);
                                      apiResponse.setData(Optional.of(response));
                                      return ResponseEntity.ok(apiResponse);
                                    })
                                .onErrorResume(
                                    error -> {
                                      Api<BrdStatusTransitionTimeResponse> errorResponse =
                                          handleErrorResponse(error);
                                      if (error instanceof IllegalParameterException) {
                                        return Mono.just(
                                            ResponseEntity.badRequest().body(errorResponse));
                                      } else if (error instanceof AccessDeniedException) {
                                        return Mono.just(
                                            ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(errorResponse));
                                      } else {
                                        return Mono.just(
                                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(errorResponse));
                                      }
                                    })));
  }

  /**
   * Validates that the period is valid
   *
   * @return The validated period (which might be the default if none was provided)
   */
  private Mono<String> validatePeriod(String period) {
    String validatedPeriod = period;

    // If period is null or empty, use the default value (quarter)
    if (validatedPeriod == null || validatedPeriod.isEmpty()) {
      validatedPeriod = DashboardConstants.QUARTER_PERIOD;
      log.info("No period specified, using default: {}", validatedPeriod);
    }

    // Store in final variable for lambda
    final String finalPeriod = validatedPeriod;

    if (!DashboardConstants.VALID_PERIODS.contains(finalPeriod.toLowerCase())) {
      return Mono.error(
          new IllegalParameterException("Invalid period. Valid values are: month, quarter, year"));
    }

    return Mono.just(finalPeriod);
  }

  /** Validates that the user has one of the required roles */
  private Mono<String> validateRole(List<String> requiredRoles, String errorMessage) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getName)
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          boolean hasRequiredRole = role != null && requiredRoles.contains(role);
                          if (hasRequiredRole) {
                            return Mono.just(username);
                          } else {
                            log.error("User does not have any of the required roles: {}", requiredRoles);
                            return Mono.error(new AccessDeniedException(errorMessage));
                          }
                        }));
  }

  @Operation(
      summary = "Get count of unresolved comment groups for BRDs not in 'Submitted' state",
      description = Descriptions.GET_UNRESOLVED_COMMENT_GROUPS_COUNT,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved unresolved comment groups count",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "ME Scope",
                          value = Response.UNRESOLVED_COMMENT_GROUPS_COUNT_SUCCESS,
                          description =
                              "Response when scope=ME - only includes comment groups created by current user"),
                      @ExampleObject(
                          name = "TEAM Scope",
                          value = Response.UNRESOLVED_COMMENT_GROUPS_COUNT_SUCCESS,
                          description =
                              "Response when scope=TEAM - includes all comment groups regardless of creator")
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid scope parameter",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @ExampleObject(
                            name = "Invalid Scope",
                            value = Response.INVALID_SCOPE_ERROR,
                            description = "Error when scope parameter is invalid"))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have PM role",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred during processing",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.SERVER_ERROR))),
        @ApiResponse(
            responseCode = "504",
            description = "Request timed out after 30 seconds",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.TIMEOUT_ERROR)))
      })
  @GetMapping("/unresolved-comment-groups-count")
  public Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>>
      getUnresolvedCommentGroupsCount(
          @Parameter(
                  description =
                      "Scope to apply - ME: only comment groups created by current user, TEAM: all comment groups. "
                          + "Filters based on the createdBy field of comment groups.",
                  example = "TEAM",
                  schema = @Schema(allowableValues = {"ME", "TEAM"}))
              @RequestParam(name = "scope", defaultValue = "ME")
              @Pattern(regexp = "(?i)^(ME|TEAM)$", message = "Scope must be either ME or TEAM")
              String scope) {
    log.info("Received request for unresolved comment groups count with scope: {}", scope);

    // Verify user has PM or BA role to access this endpoint
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!(DashboardConstants.PM_ROLE.equals(role)
                  || DashboardConstants.BA_ROLE.equals(role))) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "403",
                                "This endpoint is only accessible to users with PM and BA roles",
                                Optional.empty(),
                                Optional.empty())));
              }

              // Get current username if scope is ME
              if ("me".equalsIgnoreCase(scope)) {
                // Get current username to filter comment groups
                return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication().getName())
                    .flatMap(
                        username ->
                            dashboardService
                                .getUnresolvedCommentGroupsCount(scope.toUpperCase(), username)
                                .timeout(REQUEST_TIMEOUT)
                                .onErrorResume(this::handleTimeout));
              } else {
                // For team scope, don't filter by username
                return dashboardService
                    .getUnresolvedCommentGroupsCount(scope.toUpperCase(), null)
                    .timeout(REQUEST_TIMEOUT)
                    .onErrorResume(this::handleTimeout);
              }
            });
  }

  private Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> handleTimeout(
      Throwable ex) {
    if (ex instanceof java.util.concurrent.TimeoutException) {
      log.error("Request timed out after {} seconds", REQUEST_TIMEOUT.getSeconds(), ex);
      return Mono.just(
          ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
              .body(
                  new Api<>(
                      "504",
                      "Request timed out. Please try again later.",
                      Optional.empty(),
                      Optional.empty())));
    }
    log.error("Error processing unresolved comment groups request", ex);
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<>(
                    "500",
                    "An error occurred while retrieving comment group counts: " + ex.getMessage(),
                    Optional.empty(),
                    Optional.empty())));
  }

  /** Handle errors for ResponseEntity responses */
  private <T> Api<T> handleErrorResponse(Throwable error) {
    log.error("Error occurred: {}", error.getMessage(), error);

    Api<T> response = new Api<>();
    response.setData(Optional.empty());

    if (error instanceof IllegalParameterException) {
      response.setStatus("BAD_REQUEST");
      response.setMessage(error.getMessage());
    } else if (error instanceof AccessDeniedException) {
      response.setStatus("FORBIDDEN");
      response.setMessage(error.getMessage());
    } else {
      response.setStatus("ERROR");
      response.setMessage(error.getMessage());
    }

    return response;
  }

  /**
   * Get AI prefill rate statistics over time.
   *
   * @param period The time period to analyze (month, quarter, year)
   * @return Api containing BrdAiPrefillRateResponse with AI prefill rate statistics
   */
  @GetMapping("/ai-pre-fill-rate")
  @Operation(
      summary = "Get AI prefill rate statistics over time",
      description =
          "Returns the average AI prefill rate statistics over time for the specified period. This endpoint is only accessible to users with MANAGER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved AI prefill rate statistics"),
        @ApiResponse(responseCode = "403", description = "User does not have manager role"),
        @ApiResponse(responseCode = "400", description = "Invalid period parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<Api<BrdAiPrefillRateResponse>>> getAiPrefillRateOverTime(
      @Parameter(
              description =
                  "Period for which to retrieve data (month, quarter, year). Defaults to 'quarter' if not provided.")
          @RequestParam(required = false)
          String period) {
    return validatePeriod(period)
        .flatMap(
            validatedPeriod ->
                validateRole(
                        List.of(DashboardConstants.MANAGER_ROLE),
                        DashboardConstants.MANAGER_ONLY_MESSAGE)
                    .flatMap(
                        username ->
                            dashboardService
                                .getAiPrefillRateOverTime(validatedPeriod, username)
                                .map(
                                    response -> {
                                      Api<BrdAiPrefillRateResponse> apiResponse = new Api<>();
                                      apiResponse.setStatus(DashboardConstants.SUCCESS);
                                      apiResponse.setMessage(
                                          DashboardConstants.BRD_AI_PREFILL_RATE_SUCCESS_MESSAGE);
                                      apiResponse.setData(Optional.of(response));
                                      return ResponseEntity.ok(apiResponse);
                                    })
                                .onErrorResume(
                                    error -> {
                                      Api<BrdAiPrefillRateResponse> errorResponse =
                                          handleErrorResponse(error);
                                      if (error instanceof IllegalParameterException) {
                                        return Mono.just(
                                            ResponseEntity.badRequest().body(errorResponse));
                                      } else if (error instanceof AccessDeniedException) {
                                        return Mono.just(
                                            ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(errorResponse));
                                      } else {
                                        return Mono.just(
                                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(errorResponse));
                                      }
                                    })));
  }

  @Operation(
      summary = "Get counts of BRDs by type (NEW, UPDATE, TRIAGE) for the past 52 weeks",
      description = Descriptions.GET_BRD_COUNTS_BY_TYPE,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BRD counts by type",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.BRD_COUNTS_BY_TYPE_SUCCESS))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid scope parameter",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "Invalid Scope",
                            value = Response.INVALID_SCOPE_ERROR))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have PM role",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred during processing",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.SERVER_ERROR)))
      })
  @GetMapping("/brd-counts-by-type")
  public Mono<ResponseEntity<Api<BrdTypeCountResponse>>> getBrdCountsByType(
      @Parameter(
              description = "Scope to apply - ME: only current user's BRDs, TEAM: all BRDs",
              example = "TEAM",
              schema = @Schema(allowableValues = {"ME", "TEAM"}))
          @RequestParam(name = "scope", defaultValue = "ME")
          @Pattern(regexp = "(?i)^(ME|TEAM)$", message = "Scope must be either ME or TEAM")
          String scope) {

    log.info("Received request for BRD counts by type with scope: {}", scope);

    // Note: Temporarily treating all BRDs as NEW type regardless of actual type

    // Verify user has PM role to access this endpoint
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              String effectiveScope = determineEffectiveScope(scope, role);
              if (!(DashboardConstants.PM_ROLE.equals(role)
                  || DashboardConstants.MANAGER_ROLE.equals(role)
                  || DashboardConstants.BA_ROLE.equals(role))) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "403",
                                "This endpoint is only accessible to users with PM, Manager and BA role",
                                Optional.empty(),
                                Optional.empty())));
              }

              if ("me".equalsIgnoreCase(effectiveScope)) {
                // Get current username to filter BRDs
                return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication().getName())
                    .flatMap(
                        username ->
                            dashboardService.getBrdCountsByType(scope.toUpperCase(), username));
              } else {
                // For team scope, don't filter by username
                return dashboardService.getBrdCountsByType(effectiveScope.toUpperCase(), null);
              }
            });
  }

  @Operation(
      summary = "Get upload metrics for SSD and Contract files by BRD type",
      description = Descriptions.GET_BRD_UPLOAD_METRICS,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BRD upload metrics",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.BRD_UPLOAD_METRICS_SUCCESS))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter or scope parameter",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = {
                      @ExampleObject(
                          name = "Invalid Filter",
                          value = Response.INVALID_TIME_PERIOD_ERROR),
                      @ExampleObject(name = "Invalid Scope", value = Response.INVALID_SCOPE_ERROR)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have PM role",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred during processing",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.SERVER_ERROR)))
      })
  @GetMapping("/brd-upload-metrics")
  public Mono<ResponseEntity<Api<BrdUploadMetricsResponse>>> getBrdUploadMetrics(
      @Parameter(
              description =
                  "Filter to apply - OPEN: exclude submitted BRDs, ALL: include all BRDs and weekly metrics",
              example = "ALL",
              schema = @Schema(allowableValues = {"OPEN", "ALL"}))
          @RequestParam(name = "filter", defaultValue = "OPEN")
          @Pattern(regexp = "(?i)^(OPEN|ALL)$", message = "Filter must be either OPEN or ALL")
          String filter,
      @Parameter(
              description = "Scope to apply - ME: only current user's BRDs, TEAM: all BRDs",
              example = "TEAM",
              schema = @Schema(allowableValues = {"ME", "TEAM"}))
          @RequestParam(name = "scope", defaultValue = "ME")
          @Pattern(regexp = "(?i)^(ME|TEAM)$", message = "Scope must be either ME or TEAM")
          String scope) {

    // Note: Temporarily treating all BRDs as NEW type regardless of actual type

    // Verify user has PM role to access this endpoint
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              String effectiveScope = determineEffectiveScope(scope, role);
              if (!(DashboardConstants.PM_ROLE.equals(role)
                  || DashboardConstants.MANAGER_ROLE.equals(role)
                  || DashboardConstants.BA_ROLE.equals(role))) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "403",
                                "This endpoint is only accessible to users with PM, Manager and BA role",
                                Optional.empty(),
                                Optional.empty())));
              }

              if ("me".equalsIgnoreCase(effectiveScope)) {
                // Get current username to filter BRDs
                return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication().getName())
                    .flatMap(username -> dashboardService.getBrdUploadMetrics(filter, username));
              } else {
                // For team scope, don't filter by username
                return dashboardService.getBrdUploadMetrics(filter, null);
              }
            });
  }

  /**
   * Get average time to resolve comments for all time periods (month, quarter, and year). Returns
   * data for all three periods in a single response: - Month: single figure for current month -
   * Quarter: trend line across months in current quarter - Year: trend line across months in
   * current year
   *
   * @return Api containing CommentResolutionTimeResponse with data for all three periods
   */
  @GetMapping("/average-comment-resolution-time")
  @Operation(
      summary = "Get average time to resolve comments",
      description = Descriptions.GET_AVERAGE_COMMENT_RESOLUTION_TIME,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved average comment resolution time data",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.COMMENT_RESOLUTION_TIME_SUCCESS))),
        @ApiResponse(
            responseCode = "403",
            description = "User does not have manager role",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = Response.SERVER_ERROR)))
      })
  public Mono<ResponseEntity<Api<CommentResolutionTimeResponse>>>
      getAverageCommentResolutionTime() {
    return validateRole(
            List.of(DashboardConstants.MANAGER_ROLE),
            DashboardConstants.MANAGER_ONLY_MESSAGE)
        .flatMap(username -> dashboardService.getAverageCommentResolutionTime());
  }

  @Operation(
      summary = "Get comment group statistics",
      description = Descriptions.GET_COMMENT_GROUP_STATS,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved comment group statistics",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.COMMENT_GROUP_STATS_SUCCESS))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires Manager role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.FORBIDDEN_ERROR))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = @ExampleObject(value = Response.SERVER_ERROR)))
      })
  @GetMapping("/comment-stats")
  public Mono<ResponseEntity<Api<CommentGroupStatsResponse>>> getCommentGroupStats() {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!(DashboardConstants.MANAGER_ROLE.equals(role)
                  || DashboardConstants.BA_ROLE.equals(role))) {
                Api<CommentGroupStatsResponse> errorResponse =
                    new Api<>(
                        BrdConstants.FAILURE,
                            DashboardConstants.BA_MANAGER_ONLY_MESSAGE,
                        Optional.empty(),
                        Optional.of(
                            Map.of(
                                DashboardConstants.ERROR,
                                    DashboardConstants.BA_MANAGER_ONLY_MESSAGE)));
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
              }

              return dashboardService.getCommentGroupStats();
            });
  }
}
