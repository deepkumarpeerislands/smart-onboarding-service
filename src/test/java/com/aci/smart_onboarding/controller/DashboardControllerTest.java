package com.aci.smart_onboarding.controller;

import static com.aci.smart_onboarding.constants.DashboardConstants.BRD_AI_PREFILL_RATE_SUCCESS_MESSAGE;
import static com.aci.smart_onboarding.constants.DashboardConstants.ERROR;
import static com.aci.smart_onboarding.constants.DashboardConstants.MANAGER_ONLY_MESSAGE;
import static com.aci.smart_onboarding.constants.DashboardConstants.MANAGER_ROLE;
import static com.aci.smart_onboarding.constants.DashboardConstants.MONTH_PERIOD;
import static com.aci.smart_onboarding.constants.DashboardConstants.QUARTER_PERIOD;
import static com.aci.smart_onboarding.constants.DashboardConstants.SUCCESS;
import static com.aci.smart_onboarding.constants.DashboardConstants.YEAR_PERIOD;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.AdditionalFactorsResponse;
import com.aci.smart_onboarding.dto.AdditionalFactorsResponse.FactorStats;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdAiPrefillAccuracyResponse;
import com.aci.smart_onboarding.dto.BrdAiPrefillRateResponse;
import com.aci.smart_onboarding.dto.BrdSnapshotMetricsResponse;
import com.aci.smart_onboarding.dto.BrdStatusCountResponse;
import com.aci.smart_onboarding.dto.BrdStatusCountResponse.StatusCount;
import com.aci.smart_onboarding.dto.BrdStatusTransitionTimeResponse;
import com.aci.smart_onboarding.dto.BrdTypeCountResponse;
import com.aci.smart_onboarding.dto.BrdUploadMetricsResponse;
import com.aci.smart_onboarding.dto.BrdVerticalCountResponse;
import com.aci.smart_onboarding.dto.BrdVerticalCountResponse.VerticalCount;
import com.aci.smart_onboarding.dto.CommentGroupStatsResponse;
import com.aci.smart_onboarding.dto.CommentResolutionTimeResponse;
import com.aci.smart_onboarding.dto.UnresolvedCommentGroupsCountResponse;
import com.aci.smart_onboarding.exception.IllegalParameterException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IDashboardService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardControllerTest {

  private static final String TEST_USERNAME = "testuser";

  @Mock private IDashboardService dashboardService;

  @Mock private BRDSecurityService securityService;

  @InjectMocks private DashboardController dashboardController;

  @Test
  @DisplayName("Should return BRD counts by status for 'me' scope when user is PM")
  void getOpenBrdsByStatus_WithMeScope_ShouldReturnUserBrds() {
    // Arrange
    String scope = "me";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock the role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response
      List<StatusCount> statusCounts =
          Arrays.asList(
              new StatusCount("Draft", 2),
              new StatusCount("In Progress", 1),
              new StatusCount("Edit Complete", 0),
              new StatusCount("Internal Review", 0),
              new StatusCount("Reviewed", 0),
              new StatusCount("Ready for Signoff", 0),
              new StatusCount("Signed Off", 0));

      BrdStatusCountResponse expectedResponse =
          BrdStatusCountResponse.builder()
              .scope(scope)
              .loggedinPm(TEST_USERNAME)
              .brdStatusCounts(statusCounts)
              .build();

      // Mock service method
      when(dashboardService.getOpenBrdsByStatus(scope, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdStatusCountResponse>>> result =
          dashboardController.getBrdCounts(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdStatusCountResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getLoggedinPm().equals(TEST_USERNAME)
                    && data.getBrdStatusCounts().size() == 7;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not a PM or Manager")
  void getOpenBrdsByStatus_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdStatusCountResponse>>> result =
          dashboardController.getBrdCounts(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response
                          .getBody()
                          .getMessage()
                          .contains(
                              "Access denied. This operation is allowed for PMs and Managers only."))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle exceptions gracefully")
  void getOpenBrdsByStatus_WhenExceptionOccurs_ShouldReturnErrorResponse() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but a service error
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
      when(dashboardService.getOpenBrdsByStatus(scope, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Test error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getBrdCounts(scope))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches = response.getBody().getMessage().equals("Test error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return bad request for invalid scope")
  void getOpenBrdsByStatus_WithInvalidScope_ShouldReturnBadRequest() {
    // Arrange
    String invalidScope = "invalid";

    // Act - No need to set up authentication/security mocks since we validate scope before those
    // checks
    Mono<ResponseEntity<Api<BrdStatusCountResponse>>> result =
        dashboardController.getBrdCounts(invalidScope);

    // Assert - Should return a BAD_REQUEST response with appropriate error message
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              boolean statusIsBadRequest = response.getStatusCode() == HttpStatus.BAD_REQUEST;
              boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
              boolean errorMessageMentionsInvalidParam =
                  response.getBody().getMessage().contains("Invalid parameter value");
              boolean errorMessageContainsInvalidValue =
                  response.getBody().getMessage().contains(invalidScope);

              return statusIsBadRequest
                  && messageContainsError
                  && errorMessageMentionsInvalidParam
                  && errorMessageContainsInvalidValue;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return BRDs by vertical when parameters are valid")
  void getBrdsByVertical_WithValidParams_ShouldReturnBrds() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response
      List<VerticalCount> verticalCounts =
          Arrays.asList(
              new VerticalCount("Healthcare", 5, 50.0),
              new VerticalCount("Finance", 3, 30.0),
              new VerticalCount("Retail", 2, 20.0));

      BrdVerticalCountResponse expectedResponse =
          BrdVerticalCountResponse.builder()
              .scope(scope)
              .brdScope(brdScope)
              .period(period)
              .loggedinPm(TEST_USERNAME)
              .verticalCounts(verticalCounts)
              .build();

      // Mock service
      when(dashboardService.getBrdsByVertical(scope, brdScope, period, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> result =
          dashboardController.getBrdsByVertical(scope, brdScope, period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdVerticalCountResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getBrdScope().equals(brdScope)
                    && data.getPeriod().equals(period)
                    && data.getLoggedinPm().equals(TEST_USERNAME)
                    && data.getVerticalCounts().size() == 3;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not PM or Manager for brds-by-vertical")
  void getBrdsByVertical_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> result =
          dashboardController.getBrdsByVertical(scope, brdScope, period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response
                          .getBody()
                          .getMessage()
                          .contains(
                              "Access denied. This operation is allowed for PMs and Managers only."))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle service errors gracefully for brds-by-vertical")
  void getBrdsByVertical_WhenServiceErrors_ShouldReturnErrorResponse() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate successful role check but error from service
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
      when(dashboardService.getBrdsByVertical(scope, brdScope, period, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Service error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getBrdsByVertical(scope, brdScope, period))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches =
                    response.getBody().getMessage().equals("Service error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle parameter validation errors for brds-by-vertical")
  void getBrdsByVertical_WithInvalidParams_ShouldReturnBadRequest() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = null; // Missing required period

    // Act & Assert
    // The controller throws IllegalParameterException for missing required parameters
    IllegalParameterException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalParameterException.class,
            () -> dashboardController.getBrdsByVertical(scope, brdScope, period));

    // Verify exception message contains expected information
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains("Period parameter is required"),
        "Exception message should mention that period parameter is required");
  }

  @Test
  @DisplayName("Should return bad request for invalid scope parameter in brds-by-vertical")
  void getBrdsByVertical_WithInvalidScope_ShouldReturnBadRequest() {
    // Arrange
    String invalidScope = "invalid";
    String brdScope = "all";
    String period = "year";

    // Act - No need to set up authentication/security mocks since we validate scope before those
    // checks
    Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> result =
        dashboardController.getBrdsByVertical(invalidScope, brdScope, period);

    // Assert - Should return a BAD_REQUEST response with appropriate error message
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              boolean statusIsBadRequest = response.getStatusCode() == HttpStatus.BAD_REQUEST;
              boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
              boolean errorMessageMentionsInvalidParam =
                  response.getBody().getMessage().contains("Invalid parameter value");
              boolean errorMessageContainsInvalidValue =
                  response.getBody().getMessage().contains(invalidScope);

              return statusIsBadRequest
                  && messageContainsError
                  && errorMessageMentionsInvalidParam
                  && errorMessageContainsInvalidValue;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return bad request for invalid brdScope parameter")
  void getBrdsByVertical_WithInvalidBrdScope_ShouldReturnBadRequest() {
    // Arrange
    String scope = "me";
    String invalidBrdScope = "invalid";
    String period = "year";

    // Act & Assert
    // The controller throws IllegalParameterException for invalid parameters
    IllegalParameterException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalParameterException.class,
            () -> dashboardController.getBrdsByVertical(scope, invalidBrdScope, period));

    // Verify exception message contains expected information
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains("Invalid parameter value"),
        "Exception message should mention invalid parameter");
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains(invalidBrdScope),
        "Exception message should contain the invalid value");
  }

  @Test
  @DisplayName("Should return additional factors when parameters are valid")
  void getAdditionalFactors_WithValidParams_ShouldReturnFactors() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response
      FactorStats walletronStats =
          FactorStats.builder()
              .yesCount(6)
              .noCount(4)
              .yesPercentage(60.0)
              .noPercentage(40.0)
              .build();

      FactorStats achStats =
          FactorStats.builder()
              .yesCount(7)
              .noCount(3)
              .yesPercentage(75.0)
              .noPercentage(25.0)
              .build();

      AdditionalFactorsResponse expectedResponse =
          AdditionalFactorsResponse.builder()
              .scope(scope)
              .brdScope(brdScope)
              .period(period)
              .loggedinPm(TEST_USERNAME)
              .walletron(walletronStats)
              .achForm(achStats)
              .averageSites(2.5)
              .build();

      // Mock service
      when(dashboardService.getAdditionalFactors(scope, brdScope, period, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<AdditionalFactorsResponse>>> result =
          dashboardController.getAdditionalFactors(scope, brdScope, period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                AdditionalFactorsResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getBrdScope().equals(brdScope)
                    && data.getPeriod().equals(period)
                    && data.getLoggedinPm().equals(TEST_USERNAME)
                    && Math.abs(data.getAverageSites() - 2.5) < 0.01
                    && data.getWalletron().getYesPercentage() == 60.0
                    && data.getAchForm().getYesPercentage() == 75.0;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not PM or Manager for additional factors")
  void getAdditionalFactors_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<AdditionalFactorsResponse>>> result =
          dashboardController.getAdditionalFactors(scope, brdScope, period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response
                          .getBody()
                          .getMessage()
                          .contains(
                              "Access denied. This operation is allowed for PMs and Managers only."))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle service errors gracefully for additional factors")
  void getAdditionalFactors_WhenServiceErrors_ShouldReturnErrorResponse() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate successful role check but error from service
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
      when(dashboardService.getAdditionalFactors(scope, brdScope, period, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Service error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getAdditionalFactors(scope, brdScope, period))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches =
                    response.getBody().getMessage().equals("Service error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle parameter validation errors for additional factors")
  void getAdditionalFactors_WithMissingPeriod_ShouldReturnBadRequest() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = null; // Missing required period

    // Act & Assert
    // The controller throws IllegalParameterException for missing required parameters
    IllegalParameterException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalParameterException.class,
            () -> dashboardController.getAdditionalFactors(scope, brdScope, period));

    // Verify exception message contains expected information
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains("Period parameter is required"),
        "Exception message should mention that period parameter is required");
  }

  @Test
  @DisplayName("Should return bad request for invalid scope parameter in additional factors")
  void getAdditionalFactors_WithInvalidScope_ShouldReturnBadRequest() {
    // Arrange
    String invalidScope = "invalid";
    String brdScope = "all";
    String period = "year";

    // Act - No need to set up authentication/security mocks since we validate scope before those
    // checks
    Mono<ResponseEntity<Api<AdditionalFactorsResponse>>> result =
        dashboardController.getAdditionalFactors(invalidScope, brdScope, period);

    // Assert - Should return a BAD_REQUEST response with appropriate error message
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              boolean statusIsBadRequest = response.getStatusCode() == HttpStatus.BAD_REQUEST;
              boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
              boolean errorMessageMentionsInvalidParam =
                  response.getBody().getMessage().contains("Invalid parameter value");
              boolean errorMessageContainsInvalidValue =
                  response.getBody().getMessage().contains(invalidScope);

              return statusIsBadRequest
                  && messageContainsError
                  && errorMessageMentionsInvalidParam
                  && errorMessageContainsInvalidValue;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return bad request for invalid brdScope parameter in additional factors")
  void getAdditionalFactors_WithInvalidBrdScope_ShouldReturnBadRequest() {
    // Arrange
    String scope = "me";
    String invalidBrdScope = "invalid";
    String period = "year";

    // Act & Assert
    // The controller throws IllegalParameterException for invalid parameters
    IllegalParameterException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalParameterException.class,
            () -> dashboardController.getAdditionalFactors(scope, invalidBrdScope, period));

    // Verify exception message contains expected information
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains("Invalid parameter value"),
        "Exception message should mention invalid parameter");
    org.junit.jupiter.api.Assertions.assertTrue(
        exception.getMessage().contains(invalidBrdScope),
        "Exception message should contain the invalid value");
  }

  @Test
  @DisplayName("Should return BRD snapshot metrics for 'me' scope when user is PM")
  void getBrdSnapshotMetrics_WithMeScope_ShouldReturnMetrics() {
    // Arrange
    String scope = "me";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock the role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response
      BrdSnapshotMetricsResponse.SnapshotMetrics metrics =
          BrdSnapshotMetricsResponse.SnapshotMetrics.builder()
              .totalBrds(10)
              .openBrds(5)
              .walletronEnabledBrds(3)
              .build();

      BrdSnapshotMetricsResponse expectedResponse =
          BrdSnapshotMetricsResponse.builder().scope(scope).snapshotMetrics(metrics).build();

      // Mock service method
      when(dashboardService.getBrdSnapshotMetrics(scope, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdSnapshotMetricsResponse>>> result =
          dashboardController.getBrdSnapshotMetrics(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdSnapshotMetricsResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getSnapshotMetrics().getTotalBrds() == 10
                    && data.getSnapshotMetrics().getOpenBrds() == 5
                    && data.getSnapshotMetrics().getWalletronEnabledBrds() == 3;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not a PM or Manager for snapshot metrics")
  void getBrdSnapshotMetrics_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdSnapshotMetricsResponse>>> result =
          dashboardController.getBrdSnapshotMetrics(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response
                          .getBody()
                          .getMessage()
                          .contains(
                              "Access denied. This operation is allowed for PMs and Managers only."))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle service exceptions gracefully for snapshot metrics")
  void getBrdSnapshotMetrics_WhenExceptionOccurs_ShouldReturnErrorResponse() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but a service error
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
      when(dashboardService.getBrdSnapshotMetrics(scope, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Test error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getBrdSnapshotMetrics(scope))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches = response.getBody().getMessage().equals("Test error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return bad request for invalid scope parameter in snapshot metrics")
  void getBrdSnapshotMetrics_WithInvalidScope_ShouldReturnBadRequest() {
    // Arrange
    String invalidScope = "invalid";

    // Act - No need to set up authentication/security mocks since we validate scope before those
    // checks
    Mono<ResponseEntity<Api<BrdSnapshotMetricsResponse>>> result =
        dashboardController.getBrdSnapshotMetrics(invalidScope);

    // Assert - Should return a BAD_REQUEST response with appropriate error message
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              boolean statusIsBadRequest = response.getStatusCode() == HttpStatus.BAD_REQUEST;
              boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
              boolean errorMessageMentionsInvalidParam =
                  response.getBody().getMessage().contains("Invalid parameter value");
              boolean errorMessageContainsInvalidValue =
                  response.getBody().getMessage().contains(invalidScope);

              return statusIsBadRequest
                  && messageContainsError
                  && errorMessageMentionsInvalidParam
                  && errorMessageContainsInvalidValue;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return AI prefill accuracy metrics for 'me' scope when user is PM")
  void getAiPrefillAccuracy_WithMeScope_ShouldReturnMetrics() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response
      BrdAiPrefillAccuracyResponse expectedResponse =
          BrdAiPrefillAccuracyResponse.builder()
              .aiPrefillAccuracy(9.49) // Example: 75.9 / 8 = 9.4875 rounded to 9.49
              .build();

      // Mock service response
      when(dashboardService.getAiPrefillAccuracy(scope, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillAccuracyResponse>>> result =
          dashboardController.getAiPrefillAccuracy(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdAiPrefillAccuracyResponse data = response.getBody().getData().get();

                // Exact match for rounded value
                return response.getStatusCode() == HttpStatus.OK
                    && data.getAiPrefillAccuracy() == 9.49;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not a PM, BA, or Manager for AI prefill accuracy")
  void getAiPrefillAccuracy_WhenUserIsNotPMBAOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillAccuracyResponse>>> result =
          dashboardController.getAiPrefillAccuracy(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response
                          .getBody()
                          .getMessage()
                          .contains(
                              "Access denied. This operation is allowed for PMs and Managers only."))
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle service exceptions gracefully for AI prefill accuracy")
  void getAiPrefillAccuracy_WhenExceptionOccurs_ShouldReturnErrorResponse() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but a service error
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
      when(dashboardService.getAiPrefillAccuracy(scope, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Test error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getAiPrefillAccuracy(scope))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches = response.getBody().getMessage().equals("Test error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return bad request for invalid scope parameter in AI prefill accuracy")
  void getAiPrefillAccuracy_WithInvalidScope_ShouldReturnBadRequest() {
    // Arrange
    String invalidScope = "invalid";

    // Act - No need to set up authentication/security mocks since we validate scope before those
    // checks
    Mono<ResponseEntity<Api<BrdAiPrefillAccuracyResponse>>> result =
        dashboardController.getAiPrefillAccuracy(invalidScope);

    // Assert - Should return a BAD_REQUEST response with appropriate error message
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              boolean statusIsBadRequest = response.getStatusCode() == HttpStatus.BAD_REQUEST;
              boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
              boolean errorMessageMentionsInvalidParam =
                  response.getBody().getMessage().contains("Invalid parameter value");
              boolean errorMessageContainsInvalidValue =
                  response.getBody().getMessage().contains(invalidScope);

              return statusIsBadRequest
                  && messageContainsError
                  && errorMessageMentionsInvalidParam
                  && errorMessageContainsInvalidValue;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should return average status transition times when parameters are valid and user is MANAGER")
  void getAverageStatusTransitionTime_WithValidParams_ShouldReturnTransitionTimes() {
    // Arrange
    String period = "month";

    // Create expected response with periodData
    Map<String, Double> averages =
        Map.of(
            "Draft ➔ In Progress", 2.5,
            "In Progress ➔ Edit Complete", 3.2);

    // Create a PeriodData object
    BrdStatusTransitionTimeResponse.PeriodData periodData =
        new BrdStatusTransitionTimeResponse.PeriodData("JUNE 2023", averages);

    // Create the response with a list containing one PeriodData
    BrdStatusTransitionTimeResponse expectedResponse =
        BrdStatusTransitionTimeResponse.builder()
            .period(period)
            .loggedinManager(TEST_USERNAME)
            .periodData(Collections.singletonList(periodData))
            .build();

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check for manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Mock service response
      when(dashboardService.getAverageStatusTransitionTime(period, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdStatusTransitionTimeResponse>>> result =
          dashboardController.getAverageStatusTransitionTime(period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                // Check for successful HTTP status
                boolean statusOk = response.getStatusCode() == HttpStatus.OK;

                // Check that response contains expected data
                boolean hasExpectedData =
                    response.getBody().getData().isPresent()
                        && response.getBody().getData().get().getPeriod().equals(period)
                        && response
                            .getBody()
                            .getData()
                            .get()
                            .getLoggedinManager()
                            .equals(TEST_USERNAME)
                        && response.getBody().getData().get().getPeriodData() != null
                        && response.getBody().getData().get().getPeriodData().size() == 1
                        && response
                                .getBody()
                                .getData()
                                .get()
                                .getPeriodData()
                                .get(0)
                                .getAverages()
                                .size()
                            == 2;

                return statusOk && hasExpectedData;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden when user is not a MANAGER for transition times")
  void getAverageStatusTransitionTime_WhenUserIsNotManager_ShouldReturnForbidden() {
    // Arrange
    String period = "month";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Act & Assert
      StepVerifier.create(dashboardController.getAverageStatusTransitionTime(period))
          .expectError(AccessDeniedException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should handle service exceptions gracefully for transition times")
  void getAverageStatusTransitionTime_WhenExceptionOccurs_ShouldReturnErrorResponse() {
    // Arrange
    String period = "month";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but a service error
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_MANAGER"));
      when(dashboardService.getAverageStatusTransitionTime(period, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Test error")));

      // Act & Assert
      StepVerifier.create(dashboardController.getAverageStatusTransitionTime(period))
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = response.getBody().getStatus().equals(ERROR);
                boolean errorMessageMatches = response.getBody().getMessage().equals("Test error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return bad request for invalid period parameter in transition times")
  void getAverageStatusTransitionTime_WithInvalidPeriod_ShouldReturnBadRequest() {
    // Arrange
    String invalidPeriod = "invalid";

    // Act & Assert - no security context needed for validating period parameter
    StepVerifier.create(dashboardController.getAverageStatusTransitionTime(invalidPeriod))
        .expectError(IllegalParameterException.class)
        .verify();
  }

  @Test
  @DisplayName("Should return AI prefill rate over time when user is MANAGER")
  void getAiPrefillRateOverTime_WithValidParams_ShouldReturnRates() {
    // Arrange
    String period = QUARTER_PERIOD;

    // Create mock response
    BrdAiPrefillRateResponse mockResponse = createAiPrefillRateMockResponse(period);

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check for manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Mock service response
      when(dashboardService.getAiPrefillRateOverTime(period, TEST_USERNAME))
          .thenReturn(Mono.just(mockResponse));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillRateResponse>>> result =
          dashboardController.getAiPrefillRateOverTime(period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                // Check for successful HTTP status
                boolean statusOk = response.getStatusCode() == HttpStatus.OK;

                // Check status and message
                boolean hasSuccessStatus = SUCCESS.equals(response.getBody().getStatus());
                boolean hasCorrectMessage =
                    BRD_AI_PREFILL_RATE_SUCCESS_MESSAGE.equals(response.getBody().getMessage());

                // Check that response contains expected data
                boolean hasExpectedData =
                    response.getBody().getData().isPresent()
                        && response.getBody().getData().get().getPeriod().equals(period)
                        && response
                            .getBody()
                            .getData()
                            .get()
                            .getLoggedinManager()
                            .equals(TEST_USERNAME)
                        && response.getBody().getData().get().getTimeSegments() != null
                        && response.getBody().getData().get().getTimeSegments().size() == 3
                        && response.getBody().getData().get().getTrendData() != null;

                return statusOk && hasSuccessStatus && hasCorrectMessage && hasExpectedData;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return 403 when user is not a MANAGER for AI prefill rate")
  void getAiPrefillRateOverTime_WhenUserIsNotManager_ShouldReturnForbidden() {
    // Arrange
    String period = QUARTER_PERIOD;

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // User has PM role, not MANAGER
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Act & Assert - should return AccessDeniedException
      StepVerifier.create(dashboardController.getAiPrefillRateOverTime(period))
          .expectError(AccessDeniedException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should return 400 when invalid period parameter for AI prefill rate")
  void getAiPrefillRateOverTime_WithInvalidPeriod_ShouldReturnBadRequest() {
    // Arrange
    String invalidPeriod = "invalid";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check for manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Act & Assert - should throw IllegalParameterException
      StepVerifier.create(dashboardController.getAiPrefillRateOverTime(invalidPeriod))
          .expectError(IllegalParameterException.class)
          .verify();
    }
  }

  @Test
  @DisplayName("Should return 500 when unexpected error occurs in AI prefill rate")
  void getAiPrefillRateOverTime_WhenExceptionOccurs_ShouldReturnErrorResponse() {
    // Arrange
    String period = QUARTER_PERIOD;

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but a service error
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));
      when(dashboardService.getAiPrefillRateOverTime(period, TEST_USERNAME))
          .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillRateResponse>>> result =
          dashboardController.getAiPrefillRateOverTime(period);

      // Assert - Should return internal server error with appropriate message
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsError =
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                boolean messageContainsError = ERROR.equals(response.getBody().getStatus());
                boolean errorMessageMatches =
                    response.getBody().getMessage().equals("Unexpected error");
                return statusIsError && messageContainsError && errorMessageMatches;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return 403 when access denied for AI prefill rate")
  void getAiPrefillRateOverTime_WhenAccessDenied_ShouldReturnForbidden() {
    // Arrange
    String period = QUARTER_PERIOD;

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Simulate a successful role check but service returns AccessDeniedException
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));
      when(dashboardService.getAiPrefillRateOverTime(period, TEST_USERNAME))
          .thenReturn(Mono.error(new AccessDeniedException(MANAGER_ONLY_MESSAGE)));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillRateResponse>>> result =
          dashboardController.getAiPrefillRateOverTime(period);

      // Assert - Should return forbidden status
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && "FORBIDDEN".equals(response.getBody().getStatus()))
          .verifyComplete();
    }
  }

  /** Creates a mock AI prefill rate response for testing */
  private BrdAiPrefillRateResponse createAiPrefillRateMockResponse(String period) {
    List<BrdAiPrefillRateResponse.TimeSegment> segments = new ArrayList<>();
    List<BrdAiPrefillRateResponse.TrendPoint> trendPoints = new ArrayList<>();

    int segmentCount;
    int currentYear = LocalDateTime.now().getYear();

    switch (period) {
      case MONTH_PERIOD:
        segmentCount = 1;
        break;
      case YEAR_PERIOD:
        segmentCount = 12; // 12 months for the current year
        break;
      case QUARTER_PERIOD:
      default:
        segmentCount = 3; // Default to quarter
    }

    // Create mock segments
    for (int i = 0; i < segmentCount; i++) {
      // For year period, use current year months (JAN through DEC)
      String label;
      if (YEAR_PERIOD.equals(period)) {
        int month = i + 1; // 1-based month (1=JAN, 12=DEC)
        java.time.Month monthEnum = java.time.Month.of(month);
        label = monthEnum.toString() + " " + currentYear;
      } else {
        label = "Segment " + (i + 1);
      }

      double rate = 0.75 + (i * 0.05); // Rates: 0.75, 0.80, 0.85, etc.

      BrdAiPrefillRateResponse.BrdTypeRates typeRates =
          BrdAiPrefillRateResponse.BrdTypeRates.builder().newBrdRate(rate).build();

      segments.add(
          BrdAiPrefillRateResponse.TimeSegment.builder()
              .label(label)
              .averagePrefillRate(rate)
              .brdCount(10 + i)
              .brdTypeRates(typeRates)
              .build());

      trendPoints.add(
          BrdAiPrefillRateResponse.TrendPoint.builder().label(label).prefillRate(rate).build());
    }

    return BrdAiPrefillRateResponse.builder()
        .period(period)
        .loggedinManager(TEST_USERNAME)
        .timeSegments(segments)
        .trendData(trendPoints)
        .build();
  }

  @Test
  @DisplayName("Should return unresolved comment groups count for ME scope when user is PM")
  void getUnresolvedCommentGroupsCount_WithMeScope_ShouldReturnValidResponse() {
    // Arrange
    String scope = "ME";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response with proper properties
      UnresolvedCommentGroupsCountResponse expectedResponse =
          UnresolvedCommentGroupsCountResponse.builder().totalCount(5).build();

      ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getUnresolvedCommentGroupsCount(scope, TEST_USERNAME))
          .thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> result =
          dashboardController.getUnresolvedCommentGroupsCount(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean correctCount = response.getBody().getData().get().getTotalCount() == 5;
                return statusIsOk && hasData && correctCount;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return unresolved comment groups count for TEAM scope when user is PM")
  void getUnresolvedCommentGroupsCount_WithTeamScope_ShouldReturnValidResponse() {
    // Arrange
    String scope = "TEAM";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response with proper properties
      UnresolvedCommentGroupsCountResponse expectedResponse =
          UnresolvedCommentGroupsCountResponse.builder().totalCount(15).build();

      ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getUnresolvedCommentGroupsCount(scope, null))
          .thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> result =
          dashboardController.getUnresolvedCommentGroupsCount(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean correctCount = response.getBody().getData().get().getTotalCount() == 15;
                return statusIsOk && hasData && correctCount;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden for unresolved comment groups when user is not PM or BA")
  void getUnresolvedCommentGroupsCount_WhenUserIsNotPMOrBA_ShouldReturnForbidden() {
    // Arrange
    String scope = "ME";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - not a PM
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> result =
          dashboardController.getUnresolvedCommentGroupsCount(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsForbidden = response.getStatusCode() == HttpStatus.FORBIDDEN;
                boolean hasErrorMessage =
                    response
                        .getBody()
                        .getMessage()
                        .contains("This endpoint is only accessible to users with PM and BA roles");
                return statusIsForbidden && hasErrorMessage;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle timeout for unresolved comment groups count request")
  void getUnresolvedCommentGroupsCount_WhenTimeout_ShouldReturnGatewayTimeout() {
    // Arrange
    String scope = "ME";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Mock service method with timeout
      when(dashboardService.getUnresolvedCommentGroupsCount(scope, TEST_USERNAME))
          .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Request timed out")));

      // Act
      Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> result =
          dashboardController.getUnresolvedCommentGroupsCount(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsTimeout = response.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT;
                boolean hasTimeoutMessage = response.getBody().getMessage().contains("timed out");
                return statusIsTimeout && hasTimeoutMessage;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return BRD counts by type for ME scope when user is PM")
  void getBrdCountsByType_WithMeScope_ShouldReturnValidResponse() {
    // Arrange
    String scope = "ME";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response with proper structure
      BrdTypeCountResponse.WeeklyTypeCounts counts =
          BrdTypeCountResponse.WeeklyTypeCounts.builder()
              .newCounts(List.of(5, 3, 1))
              .updateCounts(List.of(3, 2, 1))
              .triageCounts(List.of(1, 1, 0))
              .totalCounts(List.of(9, 6, 2))
              .build();

      BrdTypeCountResponse.WeeklyMetrics weeklyMetrics =
          BrdTypeCountResponse.WeeklyMetrics.builder()
              .weeks(List.of("Week 1", "Week 2", "Week 3"))
              .counts(counts)
              .build();

      BrdTypeCountResponse expectedResponse =
          BrdTypeCountResponse.builder().scope(scope).weeklyMetrics(weeklyMetrics).build();

      ResponseEntity<Api<BrdTypeCountResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getBrdCountsByType(scope, TEST_USERNAME))
          .thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<BrdTypeCountResponse>>> result =
          dashboardController.getBrdCountsByType(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean hasWeeklyMetrics =
                    response.getBody().getData().get().getWeeklyMetrics() != null;
                boolean hasCorrectScope =
                    scope.equals(response.getBody().getData().get().getScope());
                return statusIsOk && hasData && hasWeeklyMetrics && hasCorrectScope;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return BRD counts by type for TEAM scope when user is Manager")
  void getBrdCountsByType_WithTeamScope_ForManager_ShouldReturnValidResponse() {
    // Arrange
    String scope = "ME"; // Will be overridden to TEAM for Manager role

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - Manager role
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Create test response with proper structure
      BrdTypeCountResponse.WeeklyTypeCounts counts =
          BrdTypeCountResponse.WeeklyTypeCounts.builder()
              .newCounts(List.of(10, 8, 5))
              .updateCounts(List.of(8, 6, 4))
              .triageCounts(List.of(5, 3, 2))
              .totalCounts(List.of(23, 17, 11))
              .build();

      BrdTypeCountResponse.WeeklyMetrics weeklyMetrics =
          BrdTypeCountResponse.WeeklyMetrics.builder()
              .weeks(List.of("Week 1", "Week 2", "Week 3"))
              .counts(counts)
              .build();

      BrdTypeCountResponse expectedResponse =
          BrdTypeCountResponse.builder()
              .scope("TEAM") // Manager role always uses TEAM scope
              .weeklyMetrics(weeklyMetrics)
              .build();

      ResponseEntity<Api<BrdTypeCountResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getBrdCountsByType("TEAM", null)).thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<BrdTypeCountResponse>>> result =
          dashboardController.getBrdCountsByType(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean hasWeeklyMetrics =
                    response.getBody().getData().get().getWeeklyMetrics() != null;
                boolean isTeamScope = "TEAM".equals(response.getBody().getData().get().getScope());
                return statusIsOk && hasData && hasWeeklyMetrics && isTeamScope;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden for BRD counts by type when user is not PM or Manager")
  void getBrdCountsByType_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String scope = "ME";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - not a PM or Manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdTypeCountResponse>>> result =
          dashboardController.getBrdCountsByType(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsForbidden = response.getStatusCode() == HttpStatus.FORBIDDEN;
                boolean hasErrorMessage =
                    response
                        .getBody()
                        .getMessage()
                        .contains("This endpoint is only accessible to users with PM, Manager and BA role");
                return statusIsForbidden && hasErrorMessage;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return BRD upload metrics with valid params")
  void getBrdUploadMetrics_WithValidParams_ShouldReturnValidResponse() {
    // Arrange
    String filter = "ALL";
    String scope = "TEAM";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Create test response with proper structure
      BrdUploadMetricsResponse.TypeMetrics newBrdsMetrics =
          BrdUploadMetricsResponse.TypeMetrics.builder()
              .totalCount(10)
              .uploadedCount(8)
              .uploadedPercentage(80)
              .notUploadedCount(2)
              .notUploadedPercentage(20)
              .build();

      BrdUploadMetricsResponse.TypeMetrics updateBrdsMetrics =
          BrdUploadMetricsResponse.TypeMetrics.builder()
              .totalCount(10)
              .uploadedCount(7)
              .uploadedPercentage(70)
              .notUploadedCount(3)
              .notUploadedPercentage(30)
              .build();

      BrdUploadMetricsResponse.UploadMetrics ssdUploads =
          BrdUploadMetricsResponse.UploadMetrics.builder()
              .newBrds(newBrdsMetrics)
              .updateBrds(updateBrdsMetrics)
              .build();

      BrdUploadMetricsResponse.UploadMetrics contractUploads =
          BrdUploadMetricsResponse.UploadMetrics.builder()
              .newBrds(newBrdsMetrics)
              .updateBrds(updateBrdsMetrics)
              .build();

      BrdUploadMetricsResponse expectedResponse =
          BrdUploadMetricsResponse.builder()
              .filterType(filter)
              .scope(scope)
              .ssdUploads(ssdUploads)
              .contractUploads(contractUploads)
              .build();

      ResponseEntity<Api<BrdUploadMetricsResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getBrdUploadMetrics(filter, null)).thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<BrdUploadMetricsResponse>>> result =
          dashboardController.getBrdUploadMetrics(filter, scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean hasSsdUploads = response.getBody().getData().get().getSsdUploads() != null;
                boolean hasContractUploads =
                    response.getBody().getData().get().getContractUploads() != null;
                return statusIsOk && hasData && hasSsdUploads && hasContractUploads;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return forbidden for BRD upload metrics when user is not PM or Manager")
  void getBrdUploadMetrics_WhenUserIsNotPMOrManager_ShouldReturnForbidden() {
    // Arrange
    String filter = "OPEN";
    String scope = "TEAM";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - not a PM or Manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<BrdUploadMetricsResponse>>> result =
          dashboardController.getBrdUploadMetrics(filter, scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsForbidden = response.getStatusCode() == HttpStatus.FORBIDDEN;
                boolean hasErrorMessage =
                    response
                        .getBody()
                        .getMessage()
                        .contains(
                            "This endpoint is only accessible to users with PM, Manager and BA role");
                return statusIsForbidden && hasErrorMessage;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return comment resolution time metrics for manager")
  void getAverageCommentResolutionTime_WhenUserIsManager_ShouldReturnValidResponse() {
    // Arrange

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Create test response with proper structure
      CommentResolutionTimeResponse.MonthPeriod monthPeriod =
          CommentResolutionTimeResponse.MonthPeriod.builder()
              .month("June 2023")
              .averageResolutionDays(2.5)
              .resolvedCommentsCount(15)
              .build();

      CommentResolutionTimeResponse.MonthlyDataPoint monthlyDataPoint =
          CommentResolutionTimeResponse.MonthlyDataPoint.builder()
              .month("April 2023")
              .averageResolutionDays(3.1)
              .resolvedCommentsCount(12)
              .build();

      List<CommentResolutionTimeResponse.MonthlyDataPoint> quarterMonthlyData =
          List.of(monthlyDataPoint);
      List<CommentResolutionTimeResponse.MonthlyDataPoint> yearMonthlyData =
          List.of(monthlyDataPoint);

      CommentResolutionTimeResponse.PeriodTrend quarterTrend =
          CommentResolutionTimeResponse.PeriodTrend.builder()
              .monthlyData(quarterMonthlyData)
              .build();

      CommentResolutionTimeResponse.PeriodTrend yearTrend =
          CommentResolutionTimeResponse.PeriodTrend.builder().monthlyData(yearMonthlyData).build();

      CommentResolutionTimeResponse expectedResponse =
          CommentResolutionTimeResponse.builder()
              .monthPeriod(monthPeriod)
              .quarterPeriod(quarterTrend)
              .yearPeriod(yearTrend)
              .build();

      ResponseEntity<Api<CommentResolutionTimeResponse>> apiResponse =
          ResponseEntity.ok(
              new Api<>("200", "Success", Optional.of(expectedResponse), Optional.empty()));

      // Mock service method
      when(dashboardService.getAverageCommentResolutionTime()).thenReturn(Mono.just(apiResponse));

      // Act
      Mono<ResponseEntity<Api<CommentResolutionTimeResponse>>> result =
          dashboardController.getAverageCommentResolutionTime();

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                boolean hasMonthPeriod =
                    response.getBody().getData().get().getMonthPeriod() != null;
                boolean hasQuarterPeriod =
                    response.getBody().getData().get().getQuarterPeriod() != null;
                boolean hasYearPeriod = response.getBody().getData().get().getYearPeriod() != null;
                return statusIsOk && hasData && hasMonthPeriod && hasQuarterPeriod && hasYearPeriod;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return error when comment resolution time requested by non-manager")
  void getAverageCommentResolutionTime_WhenUserIsNotManager_ShouldReturnError() {
    // Arrange

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - PM instead of Manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      // Act
      Mono<ResponseEntity<Api<CommentResolutionTimeResponse>>> result =
          dashboardController.getAverageCommentResolutionTime();

      // Assert
      StepVerifier.create(result).expectError(AccessDeniedException.class).verify();
    }
  }

  @Test
  @DisplayName("Should return comment group stats when user is Manager")
  void getCommentGroupStats_WhenUserIsManager_ShouldReturnStats() {
    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check for Manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Create test response
      CommentGroupStatsResponse.PendingCommentStats pendingStats =
          CommentGroupStatsResponse.PendingCommentStats.builder()
              .totalPendingGroups(4)
              .groupsWithPmComment(3)
              .groupsWithoutPmComment(1)
              .build();

      CommentGroupStatsResponse expectedResponse =
          CommentGroupStatsResponse.builder()
              .totalCommentGroups(10)
              .resolvedCommentGroups(6)
              .pendingCommentStats(pendingStats)
              .build();

      // Mock service method
      when(dashboardService.getCommentGroupStats())
          .thenReturn(
              Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          SUCCESS,
                          "Comment group statistics retrieved successfully",
                          Optional.of(expectedResponse),
                          Optional.empty()))));

      // Act
      Mono<ResponseEntity<Api<CommentGroupStatsResponse>>> result =
          dashboardController.getCommentGroupStats();

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                boolean statusIsOk = response.getStatusCode() == HttpStatus.OK;
                boolean hasData = response.getBody().getData().isPresent();
                CommentGroupStatsResponse data = response.getBody().getData().get();
                boolean hasCorrectStats =
                    data.getTotalCommentGroups() == 10
                        && data.getResolvedCommentGroups() == 6
                        && data.getPendingCommentStats().getTotalPendingGroups() == 4
                        && data.getPendingCommentStats().getGroupsWithPmComment() == 3
                        && data.getPendingCommentStats().getGroupsWithoutPmComment() == 1;
                return statusIsOk && hasData && hasCorrectStats;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should handle service error for comment group stats")
  void getCommentGroupStats_WhenServiceError_ShouldReturnError() {
    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check for Manager
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just(MANAGER_ROLE));

      // Mock service error
      when(dashboardService.getCommentGroupStats())
          .thenReturn(Mono.error(new RuntimeException("Service error")));

      // Act
      Mono<ResponseEntity<Api<CommentGroupStatsResponse>>> result =
          dashboardController.getCommentGroupStats();

      // Assert
      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Test
  @DisplayName("Should return BRD counts by status for 'me' scope when user is BA")
  void getOpenBrdsByStatus_WithMeScope_WhenUserIsBA_ShouldReturnUserBrds() {
    // Arrange
    String scope = "me";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock the role check - BA role should be allowed
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

      // Create test response
      List<StatusCount> statusCounts =
          Arrays.asList(
              new StatusCount("Draft", 2),
              new StatusCount("In Progress", 1),
              new StatusCount("Edit Complete", 0),
              new StatusCount("Internal Review", 0),
              new StatusCount("Reviewed", 0),
              new StatusCount("Ready for Signoff", 0),
              new StatusCount("Signed Off", 0));

      BrdStatusCountResponse expectedResponse =
          BrdStatusCountResponse.builder()
              .scope(scope)
              .loggedinPm(TEST_USERNAME)
              .brdStatusCounts(statusCounts)
              .build();

      // Mock service method
      when(dashboardService.getOpenBrdsByStatus(scope, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdStatusCountResponse>>> result =
          dashboardController.getBrdCounts(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdStatusCountResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getLoggedinPm().equals(TEST_USERNAME)
                    && data.getBrdStatusCounts().size() == 7;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return AI prefill accuracy metrics when user is BA")
  void getAiPrefillAccuracy_WhenUserIsBA_ShouldReturnMetrics() {
    // Arrange
    String scope = "me";

    // Set up security context mock
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - BA role should be allowed
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

      // Create test response
      BrdAiPrefillAccuracyResponse expectedResponse =
          BrdAiPrefillAccuracyResponse.builder()
              .aiPrefillAccuracy(9.49)
              .build();

      // Mock service response
      when(dashboardService.getAiPrefillAccuracy(scope, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdAiPrefillAccuracyResponse>>> result =
          dashboardController.getAiPrefillAccuracy(scope);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdAiPrefillAccuracyResponse data = response.getBody().getData().get();
                return response.getStatusCode() == HttpStatus.OK
                    && data.getAiPrefillAccuracy() == 9.49;
              })
          .verifyComplete();
    }
  }

  @Test
  @DisplayName("Should return BRDs by vertical when user is BA")
  void getBrdsByVertical_WhenUserIsBA_ShouldReturnBrds() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "year";

    // Mock security context
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(TEST_USERNAME);

    try (MockedStatic<ReactiveSecurityContextHolder> securityContextHolder =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      securityContextHolder
          .when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));

      // Mock role check - BA role should be allowed
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

      // Create test response
      List<VerticalCount> verticalCounts =
          Arrays.asList(
              new VerticalCount("Healthcare", 5, 50.0),
              new VerticalCount("Finance", 3, 30.0),
              new VerticalCount("Retail", 2, 20.0));

      BrdVerticalCountResponse expectedResponse =
          BrdVerticalCountResponse.builder()
              .scope(scope)
              .brdScope(brdScope)
              .period(period)
              .verticalCounts(verticalCounts)
              .build();

      // Mock service method
      when(dashboardService.getBrdsByVertical(scope, brdScope, period, TEST_USERNAME))
          .thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<BrdVerticalCountResponse>>> result =
          dashboardController.getBrdsByVertical(scope, brdScope, period);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                BrdVerticalCountResponse data = response.getBody().getData().get();

                return response.getStatusCode() == HttpStatus.OK
                    && data.getScope().equals(scope)
                    && data.getBrdScope().equals(brdScope)
                    && data.getPeriod().equals(period)
                    && data.getVerticalCounts().size() == 3;
              })
          .verifyComplete();
    }
  }
}
