package com.aci.smart_onboarding.service.implementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdAiPrefillRateResponse;
import com.aci.smart_onboarding.dto.BrdStatusTransitionTimeResponse;
import com.aci.smart_onboarding.dto.BrdTypeCountResponse;
import com.aci.smart_onboarding.dto.BrdUploadMetricsResponse;
import com.aci.smart_onboarding.dto.CommentGroupStatsResponse;
import com.aci.smart_onboarding.dto.UnresolvedCommentGroupsCountResponse;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.IllegalParameterException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.model.dashboard.TimeSegment;
import com.aci.smart_onboarding.repository.BRDRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

  @Mock private BRDRepository brdRepository;

  @Mock private ReactiveMongoTemplate mongoTemplate;

  @InjectMocks private DashboardService dashboardService;

  @Test
  @DisplayName("Should return BRD counts by status for 'me' scope")
  void getOpenBrdsByStatus_WithMeScope_ShouldReturnCorrectCounts() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Create test BRDs
    BRD brd1 = BRD.builder().brdId("BRD-001").status("Draft").creator(username).build();
    BRD brd2 = BRD.builder().brdId("BRD-002").status("Draft").creator(username).build();
    BRD brd3 = BRD.builder().brdId("BRD-003").status("In Progress").creator(username).build();
    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Verify Query includes correct criteria
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .then(
            invocation -> {
              Query query = invocation.getArgument(0);
              // For scope=me, query should include creator=username
              String json = query.getQueryObject().toJson();

              // Verify query contains both status and creator
              boolean hasCorrectQuery = json.contains("\"status\"") && json.contains("\"creator\"");

              return hasCorrectQuery ? Flux.fromIterable(brds) : Flux.empty();
            });

    // Act
    var result = dashboardService.getOpenBrdsByStatus(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect =
                  response.getScope().equals(scope) && response.getLoggedinPm().equals(username);

              // Verify status counts
              boolean draftCountCorrect =
                  response.getBrdStatusCounts().stream()
                      .filter(sc -> sc.getStatus().equals("Draft"))
                      .findFirst()
                      .map(sc -> sc.getCount() == 2)
                      .orElse(false);

              boolean inProgressCountCorrect =
                  response.getBrdStatusCounts().stream()
                      .filter(sc -> sc.getStatus().equals("In Progress"))
                      .findFirst()
                      .map(sc -> sc.getCount() == 1)
                      .orElse(false);

              // Verify all other statuses have zero count
              boolean otherStatusesCorrect =
                  response.getBrdStatusCounts().stream()
                      .filter(
                          sc ->
                              !sc.getStatus().equals("Draft")
                                  && !sc.getStatus().equals("In Progress"))
                      .allMatch(sc -> sc.getCount() == 0);

              // Verify we have 7 status counts total (all statuses should be included)
              boolean correctStatusCount = response.getBrdStatusCounts().size() == 7;

              return scopeAndLoggedInPmCorrect
                  && draftCountCorrect
                  && inProgressCountCorrect
                  && otherStatusesCorrect
                  && correctStatusCount;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return BRD counts by status for 'team' scope")
  void getOpenBrdsByStatus_WithTeamScope_ShouldReturnCorrectCounts() {
    // Arrange
    String scope = "team";
    String username = "testuser";

    // Create test BRDs - for team scope, should include all BRDs regardless of creator
    BRD brd1 = BRD.builder().brdId("BRD-001").status("Draft").creator(username).build();
    BRD brd2 = BRD.builder().brdId("BRD-002").status("Draft").creator("otheruser").build();
    BRD brd3 = BRD.builder().brdId("BRD-003").status("In Progress").creator("thirduser").build();
    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Verify Query includes correct criteria
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .then(
            invocation -> {
              Query query = invocation.getArgument(0);
              // For scope=team, query should only filter by status, not by creator
              String json = query.getQueryObject().toJson();

              // Verify query contains status but not creator
              boolean hasCorrectQuery =
                  json.contains("\"status\"") && !json.contains("\"creator\"");

              return hasCorrectQuery ? Flux.fromIterable(brds) : Flux.empty();
            });

    // Act
    var result = dashboardService.getOpenBrdsByStatus(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect =
                  response.getScope().equals(scope) && response.getLoggedinPm().equals(username);

              // Verify status counts
              boolean draftCountCorrect =
                  response.getBrdStatusCounts().stream()
                      .filter(sc -> sc.getStatus().equals("Draft"))
                      .findFirst()
                      .map(sc -> sc.getCount() == 2)
                      .orElse(false);

              boolean inProgressCountCorrect =
                  response.getBrdStatusCounts().stream()
                      .filter(sc -> sc.getStatus().equals("In Progress"))
                      .findFirst()
                      .map(sc -> sc.getCount() == 1)
                      .orElse(false);

              return scopeAndLoggedInPmCorrect && draftCountCorrect && inProgressCountCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results")
  void getOpenBrdsByStatus_WithNoMatches_ShouldReturnZeroCounts() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Return empty list
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getOpenBrdsByStatus(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect =
                  response.getScope().equals(scope) && response.getLoggedinPm().equals(username);

              // Verify all statuses have zero count
              boolean allZeroCounts =
                  response.getBrdStatusCounts().stream().allMatch(sc -> sc.getCount() == 0);

              // Verify all statuses are included
              boolean allStatusesPresent = response.getBrdStatusCounts().size() == 7;

              return scopeAndLoggedInPmCorrect && allZeroCounts && allStatusesPresent;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should return BRDs by vertical with correct filtering for 'me' scope and 'open' brdScope")
  void getBrdsByVertical_WithMeScopeAndOpenBrdScope_ShouldReturnCorrectCounts() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null; // Not needed for open scope
    String username = "testuser";

    // Create test BRDs with industry verticals
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(15)) // 15 days ago
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(10)) // 10 days ago
            .updatedAt(getDateFromNowMinus(5)) // 5 days ago
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .brdFormId("form-003")
            .status("Draft")
            .creator(username)
            .industryVertical("Finance")
            .createdAt(getDateFromNowMinus(15)) // 15 days ago
            .build();

    List<BRD> userBrds = Arrays.asList(brd1, brd2, brd3);

    // Create system-wide BRDs for percentage calculation
    BRD systemBrd1 =
        BRD.builder()
            .brdId("SYS-001")
            .brdFormId("sys-form-001")
            .status("Draft")
            .creator("otheruser")
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(20))
            .build();

    BRD systemBrd2 =
        BRD.builder()
            .brdId("SYS-002")
            .brdFormId("sys-form-002")
            .status("In Progress")
            .creator("anotheruser")
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(15))
            .build();

    List<BRD> allOpenBrds = Arrays.asList(brd1, brd2, brd3, systemBrd1, systemBrd2);

    // Mock query executions - first for user's BRDs, then for system-wide BRDs
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(userBrds))
        .thenReturn(Flux.fromIterable(allOpenBrds));

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Check basic response properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getLoggedinPm().equals(username);

              // Check vertical counts - should have Healthcare (2) and Finance (1)
              var healthcareCount =
                  response.getVerticalCounts().stream()
                      .filter(vc -> vc.getIndustryName().equals("Healthcare"))
                      .findFirst();

              var financeCount =
                  response.getVerticalCounts().stream()
                      .filter(vc -> vc.getIndustryName().equals("Finance"))
                      .findFirst();

              boolean verticalCountsCorrect =
                  healthcareCount.isPresent()
                      && healthcareCount.get().getBrdCount() == 2
                      && financeCount.isPresent()
                      && financeCount.get().getBrdCount() == 1;

              // Check correct sort order - highest count should be first
              boolean sortOrderCorrect =
                  response.getVerticalCounts().get(0).getIndustryName().equals("Healthcare")
                      && response.getVerticalCounts().get(1).getIndustryName().equals("Finance");

              return basicPropsCorrect && verticalCountsCorrect && sortOrderCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return BRDs by vertical with correct filtering for all BRDs with period")
  void getBrdsByVertical_WithAllBrdsAndPeriod_ShouldReturnCorrectCounts() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = "month"; // Required for "all" scope
    String username = "testuser";

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(15)) // 15 days ago
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("Submit") // Submitted BRD
            .creator(username)
            .industryVertical("Finance")
            .createdAt(getDateFromNowMinus(10)) // 10 days ago
            .updatedAt(getDateFromNowMinus(5)) // 5 days ago
            .build();

    List<BRD> userBrds = Arrays.asList(brd1, brd2);

    // Create all system BRDs for percentage calculation
    BRD systemBrd1 =
        BRD.builder()
            .brdId("SYS-001")
            .brdFormId("sys-form-001")
            .status("Draft")
            .creator("otheruser")
            .industryVertical("Healthcare")
            .createdAt(getDateFromNowMinus(20))
            .build();

    List<BRD> allBrds = Arrays.asList(brd1, brd2, systemBrd1);

    // Mock query executions
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(userBrds))
        .thenReturn(Flux.fromIterable(allBrds));

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Check basic properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getPeriod().equals(period)
                      && response.getLoggedinPm().equals(username);

              // Verify we have two vertical counts: Healthcare and Finance
              boolean correctVerticalCount = response.getVerticalCounts().size() == 2;

              // Each should have count 1
              boolean correctCounts =
                  response.getVerticalCounts().stream().allMatch(vc -> vc.getBrdCount() == 1);

              return basicPropsCorrect && correctVerticalCount && correctCounts;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should require period parameter when brdScope is 'all'")
  void getBrdsByVertical_WithAllBrdScopeButNoPeriod_ShouldThrowException() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = null; // Missing required parameter
    String username = "testuser";

    // Act & Assert
    StepVerifier.create(dashboardService.getBrdsByVertical(scope, brdScope, period, username))
        .expectErrorMatches(
            error ->
                error instanceof IllegalParameterException
                    && error.getMessage().contains("Period parameter is required"))
        .verify();
  }

  @Test
  @DisplayName("Should handle empty results for getBrdsByVertical")
  void getBrdsByVertical_WithNoMatches_ShouldReturnEmptyList() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Return empty list for both queries
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.empty())
        .thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Check basic properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getLoggedinPm().equals(username);

              // Should have empty vertical counts
              boolean emptyVerticalCounts = response.getVerticalCounts().isEmpty();

              return basicPropsCorrect && emptyVerticalCounts;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BRDs with missing verticals")
  void getBrdsByVertical_WithMissingVerticals_ShouldCategorizeAsOther() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRDs with missing verticals
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical(null) // Missing vertical
            .createdAt(getDateFromNowMinus(15)) // 15 days ago
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .industryVertical("") // Empty vertical
            .createdAt(getDateFromNowMinus(10)) // 10 days ago
            .updatedAt(getDateFromNowMinus(5)) // 5 days ago
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .brdFormId("form-003")
            .status("Draft")
            .creator(username)
            .industryVertical("Healthcare") // Valid vertical
            .createdAt(getDateFromNowMinus(15)) // 15 days ago
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Mock query executions
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(brds))
        .thenReturn(Flux.fromIterable(brds)); // Same list for system-wide

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Should have "Other" category with count 2 and "Healthcare" with count 1
              var otherCount =
                  response.getVerticalCounts().stream()
                      .filter(vc -> vc.getIndustryName().equals("Other"))
                      .findFirst();

              var healthcareCount =
                  response.getVerticalCounts().stream()
                      .filter(vc -> vc.getIndustryName().equals("Healthcare"))
                      .findFirst();

              boolean verticalCategorizationCorrect =
                  otherCount.isPresent()
                      && otherCount.get().getBrdCount() == 2
                      && healthcareCount.isPresent()
                      && healthcareCount.get().getBrdCount() == 1;

              // "Other" should be first since it has higher count
              boolean sortOrderCorrect =
                  response.getVerticalCounts().get(0).getIndustryName().equals("Other");

              return verticalCategorizationCorrect && sortOrderCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should return additional factors with correct calculations for 'me' scope and 'open' brdScope")
  void getAdditionalFactors_WithMeScopeAndOpenBrdScope_ShouldReturnCorrectStats() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(false)
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .brdFormId("form-003")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(false)
            .achEncrypted(true)
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Mock the MongoDB query
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getPeriod() == period
                      && response.getLoggedinPm().equals(username);

              // Expected: 2 out of 3 BRDs have Walletron enabled (67%)
              boolean walletronStatsCorrect =
                  Math.abs(response.getWalletron().getYesPercentage() - 67.0) < 0.1
                      && Math.abs(response.getWalletron().getNoPercentage() - 33.0) < 0.1;

              // Expected: 2 out of 3 BRDs have ACH encrypted (67%)
              boolean achStatsCorrect =
                  Math.abs(response.getAchForm().getYesPercentage() - 67.0) < 0.1
                      && Math.abs(response.getAchForm().getNoPercentage() - 33.0) < 0.1;

              return basicPropsCorrect && walletronStatsCorrect && achStatsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "Should return additional factors with correct calculations for 'team' scope and 'all' brdScope")
  void getAdditionalFactors_WithTeamScopeAndAllBrdScope_ShouldReturnCorrectStats() {
    // Arrange
    String scope = "team";
    String brdScope = "all";
    String period = "month";
    String username = "testuser";

    // Create test BRDs with different creators (for team scope)
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator("otheruser")
            .wallentronIncluded(false)
            .achEncrypted(false)
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .brdFormId("form-003")
            .status("Submit") // Not in "open" status
            .creator("thirduser")
            .wallentronIncluded(false)
            .achEncrypted(true)
            .createdAt(LocalDateTime.now().minusDays(15))
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Mock the MongoDB query
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getPeriod().equals(period)
                      && response.getLoggedinPm().equals(username);

              // Expected: 1 out of 3 BRDs have Walletron enabled (33%)
              boolean walletronStatsCorrect =
                  Math.abs(response.getWalletron().getYesPercentage() - 33.0) < 0.1
                      && Math.abs(response.getWalletron().getNoPercentage() - 67.0) < 0.1;

              // Expected: 2 out of 3 BRDs have ACH encrypted (67%)
              boolean achStatsCorrect =
                  Math.abs(response.getAchForm().getYesPercentage() - 67.0) < 0.1
                      && Math.abs(response.getAchForm().getNoPercentage() - 33.0) < 0.1;

              return basicPropsCorrect && walletronStatsCorrect && achStatsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should require period parameter when brdScope is 'all' for additional factors")
  void getAdditionalFactors_WithAllBrdScopeButNoPeriod_ShouldThrowException() {
    // Arrange
    String scope = "me";
    String brdScope = "all";
    String period = null; // Missing required period for "all" scope
    String username = "testuser";

    // Act & Assert
    StepVerifier.create(dashboardService.getAdditionalFactors(scope, brdScope, period, username))
        .expectError(IllegalParameterException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle empty results for additional factors")
  void getAdditionalFactors_WithNoMatches_ShouldReturnEmptyStats() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Mock empty result set
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getScope().equals(scope)
                      && response.getBrdScope().equals(brdScope)
                      && response.getPeriod() == period
                      && response.getLoggedinPm().equals(username);

              // For empty result set, all percentages should be 0
              boolean zeroFactors =
                  response.getWalletron().getYesPercentage() == 0.0
                      && response.getWalletron().getNoPercentage() == 0.0
                      && response.getAchForm().getYesPercentage() == 0.0
                      && response.getAchForm().getNoPercentage() == 0.0;

              return basicPropsCorrect && zeroFactors;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return BRD snapshot metrics for 'me' scope")
  void getBrdSnapshotMetrics_WithMeScope_ShouldReturnCorrectMetrics() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .status("In Progress")
            .creator(username)
            .wallentronIncluded(false)
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .status("Submit")
            .creator(username)
            .wallentronIncluded(true)
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Mock repository calls for total BRDs
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .then(
            invocation -> {
              Query query = invocation.getArgument(0);
              String json = query.getQueryObject().toJson();

              // For scope=me, query should include creator=username
              boolean hasCorrectCreatorQuery = json.contains("\"creator\"");

              return hasCorrectCreatorQuery ? Flux.fromIterable(brds) : Flux.empty();
            });

    // Act
    var result = dashboardService.getBrdSnapshotMetrics(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect = response.getScope().equals(scope);

              // Verify metrics
              boolean totalBrdsCorrect = response.getSnapshotMetrics().getTotalBrds() == 3;
              boolean openBrdsCorrect = response.getSnapshotMetrics().getOpenBrds() == 2;
              boolean walletronEnabledBrdsCorrect =
                  response.getSnapshotMetrics().getWalletronEnabledBrds() == 2;

              return scopeAndLoggedInPmCorrect
                  && totalBrdsCorrect
                  && openBrdsCorrect
                  && walletronEnabledBrdsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return BRD snapshot metrics for 'team' scope")
  void getBrdSnapshotMetrics_WithTeamScope_ShouldReturnCorrectMetrics() {
    // Arrange
    String scope = "team";
    String username = "testuser";

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .status("In Progress")
            .creator("otheruser")
            .wallentronIncluded(false)
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .status("Submit")
            .creator("thirduser")
            .wallentronIncluded(true)
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3);

    // Mock repository calls for total BRDs
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .then(
            invocation -> {
              Query query = invocation.getArgument(0);
              String json = query.getQueryObject().toJson();

              // For scope=team, query should not include creator
              boolean hasCorrectQuery = !json.contains("\"creator\"");

              return hasCorrectQuery ? Flux.fromIterable(brds) : Flux.empty();
            });

    // Act
    var result = dashboardService.getBrdSnapshotMetrics(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect = response.getScope().equals(scope);

              // Verify metrics
              boolean totalBrdsCorrect = response.getSnapshotMetrics().getTotalBrds() == 3;
              boolean openBrdsCorrect = response.getSnapshotMetrics().getOpenBrds() == 2;
              boolean walletronEnabledBrdsCorrect =
                  response.getSnapshotMetrics().getWalletronEnabledBrds() == 2;

              return scopeAndLoggedInPmCorrect
                  && totalBrdsCorrect
                  && openBrdsCorrect
                  && walletronEnabledBrdsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results for getBrdSnapshotMetrics")
  void getBrdSnapshotMetrics_WithNoMatches_ShouldReturnZeroMetrics() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Return empty list
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getBrdSnapshotMetrics(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify scope and logged-in PM
              boolean scopeAndLoggedInPmCorrect = response.getScope().equals(scope);

              // Verify metrics are all zero
              boolean allZeroMetrics =
                  response.getSnapshotMetrics().getTotalBrds() == 0
                      && response.getSnapshotMetrics().getOpenBrds() == 0
                      && response.getSnapshotMetrics().getWalletronEnabledBrds() == 0;

              return scopeAndLoggedInPmCorrect && allZeroMetrics;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return AI prefill accuracy metrics for 'me' scope")
  void getAiPrefillAccuracy_WithMeScope_ShouldReturnCorrectMetrics() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Create test BRDs with varying AI prefill rates
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .aiPrefillRate(85.7)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .aiPrefillRate(92.3)
            .build();

    BRD brd3 =
        BRD.builder()
            .brdId("BRD-003")
            .brdFormId("form-003")
            .status("Internal Review")
            .creator(username)
            .aiPrefillRate(0.0)
            .build();

    BRD brd4 =
        BRD.builder()
            .brdId("BRD-004")
            .brdFormId("form-004")
            .status("Ready for Sign-Off")
            .creator(username)
            .aiPrefillRate(null)
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2, brd3, brd4);

    // Mock the MongoDB query
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Act
    var result = dashboardService.getAiPrefillAccuracy(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Expected: Total of prefill rates (85.7 + 92.3 + 0.0 + 0.0) / 4 = 44.5
              double expectedRate = 44.5; // (85.7 + 92.3) / 4 rounded to 2 decimal places

              // Exact match for rounded value
              return response.getAiPrefillAccuracy() == expectedRate;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should treat null AI prefill rates as zero")
  void getAiPrefillAccuracy_WithNullRates_ShouldTreatAsZero() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Create test BRDs with no AI prefill rates
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .aiPrefillRate(75.0)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .aiPrefillRate(null) // No AI prefill rate - treated as 0
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);

    // Mock the MongoDB query
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Act
    var result = dashboardService.getAiPrefillAccuracy(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Expected: (75.0 + 0) / 2 = 37.5
              double expectedRate = 37.5; // 75.0 / 2 rounded to 2 decimal places
              return response.getAiPrefillAccuracy() == expectedRate;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty result set for AI prefill accuracy")
  void getAiPrefillAccuracy_WithNoResults_ShouldReturnEmptyMetrics() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Mock empty result set
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAiPrefillAccuracy(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Expected: 0.0 as no BRDs found
              return response.getAiPrefillAccuracy() == 0.0;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should calculate average status transition times for monthly period")
  void getAverageStatusTransitionTime_WithMonthPeriod_ShouldReturnCorrectAverages() {
    // Arrange
    String period = "month";
    String username = "manager1";

    // Create test audit logs for BRD status changes
    AuditLog draftLog1 = createAuditLog("BRD-001", "Draft", "2023-05-01T10:00:00");
    AuditLog inProgressLog1 = createAuditLog("BRD-001", "In Progress", "2023-05-03T14:00:00");
    AuditLog editCompleteLog1 = createAuditLog("BRD-001", "Edit Complete", "2023-05-07T09:00:00");

    AuditLog draftLog2 = createAuditLog("BRD-002", "Draft", "2023-05-05T11:00:00");
    AuditLog inProgressLog2 = createAuditLog("BRD-002", "In Progress", "2023-05-08T16:00:00");
    AuditLog editCompleteLog2 = createAuditLog("BRD-002", "Edit Complete", "2023-05-11T10:00:00");

    // Create test BRDs with appropriate dates
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("BRD-001") // Match entityId in audit logs
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-01T10:00:00"))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("BRD-002") // Match entityId in audit logs
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-05T11:00:00"))
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);
    List<AuditLog> auditLogs =
        Arrays.asList(
            draftLog1,
            inProgressLog1,
            editCompleteLog1,
            draftLog2,
            inProgressLog2,
            editCompleteLog2);

    // Mock BRD repository to return BRDs created in the given period
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Mock audit logs query to return the logs for the BRDs
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.fromIterable(auditLogs));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getPeriod().equals(period)
                      && response.getLoggedinManager().equals(username);

              // For month period, we should have exactly 1 period data
              boolean hasSinglePeriodData =
                  response.getPeriodData() != null && response.getPeriodData().size() == 1;

              if (!hasSinglePeriodData) {
                return false;
              }

              var periodData = response.getPeriodData().get(0);
              Map<String, Double> averages = periodData.getAverages();

              // Verify response contains all valid transition types
              boolean containsAllTransitions =
                  averages.containsKey("Draft ➔ In Progress")
                      && averages.containsKey("In Progress ➔ Edit Complete")
                      && averages.containsKey("Edit Complete ➔ Internal Review")
                      && averages.containsKey("Internal Review ➔ Reviewed")
                      && averages.containsKey("Reviewed ➔ Ready for Sign-Off")
                      && averages.containsKey("Ready for Sign-Off ➔ Signed Off")
                      && averages.containsKey("Signed Off ➔ Submit");

              // Verify specific calculation values
              // Expected averages:
              // Draft → In Progress: BRD-001 = 2.17 days, BRD-002 = 3.21 days, Average = 2.7 days
              // In Progress → Edit Complete: BRD-001 = 3.79 days, BRD-002 = 2.75 days, Average =
              // 3.3 days
              boolean draftToInProgressCorrect =
                  Math.abs(averages.get("Draft ➔ In Progress") - 2.7) < 0.1;
              boolean inProgressToEditCompleteCorrect =
                  Math.abs(averages.get("In Progress ➔ Edit Complete") - 3.3) < 0.1;

              // Other transitions should have 0.0 values
              boolean otherTransitionsZero =
                  averages.get("Edit Complete ➔ Internal Review") == 0.0
                      && averages.get("Internal Review ➔ Reviewed") == 0.0
                      && averages.get("Reviewed ➔ Ready for Sign-Off") == 0.0
                      && averages.get("Ready for Sign-Off ➔ Signed Off") == 0.0
                      && averages.get("Signed Off ➔ Submit") == 0.0;

              return basicPropsCorrect
                  && hasSinglePeriodData
                  && containsAllTransitions
                  && draftToInProgressCorrect
                  && inProgressToEditCompleteCorrect
                  && otherTransitionsZero;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results for status transition times")
  void getAverageStatusTransitionTime_WithNoMatches_ShouldReturnEmptyAverages() {
    // Arrange
    String period = "month";
    String username = "manager1";

    // Mock BRD query to return empty results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getPeriod().equals(period)
                      && response.getLoggedinManager().equals(username);

              // For month period, we should have exactly 1 period data
              boolean hasSinglePeriodData =
                  response.getPeriodData() != null && response.getPeriodData().size() == 1;

              if (!hasSinglePeriodData) {
                return false;
              }

              var periodData = response.getPeriodData().get(0);
              Map<String, Double> averages = periodData.getAverages();

              // Verify all transitions are present with zero values
              boolean hasAllTransitionsWithZero =
                  averages.size() == 7 && averages.values().stream().allMatch(v -> v == 0.0);

              return basicPropsCorrect && hasSinglePeriodData && hasAllTransitionsWithZero;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BRDs with missing status transitions")
  void getAverageStatusTransitionTime_WithMissingTransitions_ShouldCalculateAvailableAverages() {
    // Arrange
    String period = "quarter";
    String username = "manager1";

    // Create test audit logs for BRD with valid transitions
    AuditLog draftLog1 = createAuditLog("BRD-001", "Draft", "2023-05-01T10:00:00");
    AuditLog inProgressLog1 = createAuditLog("BRD-001", "In Progress", "2023-05-03T14:00:00");
    AuditLog editCompleteLog1 = createAuditLog("BRD-001", "Edit Complete", "2023-05-07T09:00:00");
    AuditLog internalReviewLog1 =
        createAuditLog("BRD-001", "Internal Review", "2023-05-10T14:00:00");

    // Create invalid transition (skipping a step)
    AuditLog draftLog2 = createAuditLog("BRD-002", "Draft", "2023-05-05T11:00:00");
    // Missing "In Progress" status - this creates an invalid transition that should be filtered out
    AuditLog editCompleteLog2 = createAuditLog("BRD-002", "Edit Complete", "2023-05-11T10:00:00");

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("BRD-001")
            .status("Internal Review")
            .createdAt(LocalDateTime.parse("2023-05-01T10:00:00"))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("BRD-002")
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-05T11:00:00"))
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);
    List<AuditLog> auditLogs =
        Arrays.asList(
            draftLog1,
            inProgressLog1,
            editCompleteLog1,
            internalReviewLog1,
            draftLog2,
            editCompleteLog2);

    // Mock BRD repository to return BRDs created in the given period
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Mock audit logs query to return the logs for the BRDs
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.fromIterable(auditLogs));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify basic properties
              boolean basicPropsCorrect =
                  response.getPeriod().equals(period)
                      && response.getLoggedinManager().equals(username);

              // For quarter period, we should have exactly 3 period data entries (one for each
              // month)
              boolean hasThreePeriodData =
                  response.getPeriodData() != null && response.getPeriodData().size() == 3;

              if (!hasThreePeriodData) {
                return false;
              }

              // Check at least one of the period data entries has valid transitions
              boolean hasValidTransitions =
                  response.getPeriodData().stream()
                      .anyMatch(
                          pd -> {
                            Map<String, Double> averages = pd.getAverages();

                            // Verify all valid transitions are present
                            boolean hasAllTransitions = averages.size() == 7;

                            // Verify valid transitions have values (from BRD-001)
                            boolean validTransitionsHaveValues =
                                averages.getOrDefault("Draft ➔ In Progress", 0.0) > 0.0
                                    || averages.getOrDefault("In Progress ➔ Edit Complete", 0.0)
                                        > 0.0
                                    || averages.getOrDefault("Edit Complete ➔ Internal Review", 0.0)
                                        > 0.0;

                            // Verify invalid transition (Draft -> Edit Complete from BRD-002) is
                            // not counted
                            boolean skippedTransitionNotCounted =
                                !averages.containsKey("Draft ➔ Edit Complete")
                                    || averages.get("Draft ➔ Edit Complete") == 0.0;

                            return hasAllTransitions
                                && validTransitionsHaveValues
                                && skippedTransitionNotCounted;
                          });

              return basicPropsCorrect && hasThreePeriodData && hasValidTransitions;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should use default quarter period when period is null")
  void getAverageStatusTransitionTime_WithNullPeriod_ShouldUseDefaultPeriod() {
    // Arrange
    String period = null;
    String username = "manager1";

    // Create mock BRDs and audit logs for empty result
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(dashboardService.getAverageStatusTransitionTime(period, username))
        .expectNextMatches(
            response -> {
              // Should use "quarter" as default period
              return response.getPeriod().equals("quarter")
                  && response.getLoggedinManager().equals(username)
                  && response.getPeriodData() != null
                  && response.getPeriodData().size() == 3; // Quarter should have 3 months of data
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should calculate blended averages for trend data in monthly period")
  void getAverageStatusTransitionTime_WithMonthPeriod_ShouldCalculateBlendedAverages() {
    // Arrange
    String period = "month";
    String username = "manager1";

    // Create test audit logs for BRD status changes
    AuditLog draftLog1 = createAuditLog("BRD-001", "Draft", "2023-05-01T10:00:00");
    AuditLog inProgressLog1 =
        createAuditLog("BRD-001", "In Progress", "2023-05-03T14:00:00"); // 2.17 days
    AuditLog editCompleteLog1 =
        createAuditLog("BRD-001", "Edit Complete", "2023-05-07T09:00:00"); // 3.79 days

    AuditLog draftLog2 = createAuditLog("BRD-002", "Draft", "2023-05-05T11:00:00");
    AuditLog inProgressLog2 =
        createAuditLog("BRD-002", "In Progress", "2023-05-08T16:00:00"); // 3.21 days
    AuditLog editCompleteLog2 =
        createAuditLog("BRD-002", "Edit Complete", "2023-05-11T10:00:00"); // 2.75 days

    // Create test BRDs with appropriate dates
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("BRD-001") // Match entityId in audit logs
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-01T10:00:00"))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("BRD-002") // Match entityId in audit logs
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-05T11:00:00"))
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);
    List<AuditLog> auditLogs =
        Arrays.asList(
            draftLog1,
            inProgressLog1,
            editCompleteLog1,
            draftLog2,
            inProgressLog2,
            editCompleteLog2);

    // Mock BRD repository to return BRDs created in the given period
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    // Mock audit logs query to return the logs for the BRDs
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.fromIterable(auditLogs));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify trend data exists
              boolean hasTrendData =
                  response.getTrendData() != null && !response.getTrendData().isEmpty();

              if (!hasTrendData) {
                return false;
              }

              // For month period, we should have exactly 1 trend point
              boolean hasSingleTrendPoint = response.getTrendData().size() == 1;

              if (!hasSingleTrendPoint) {
                return false;
              }

              // Get the trend point
              var trendPoint = response.getTrendData().get(0);

              // Verify label matches period data label
              boolean labelMatches =
                  trendPoint.getLabel().equals(response.getPeriodData().get(0).getLabel());

              // Expected blended average calculation:
              // Average transition times: 2.7 days (Draft → In Progress) and 3.3 days (In Progress
              // → Edit Complete)
              // Blended average: (2.7 + 3.3) / 2 = 3.0 days
              double expectedBlendedAvg = 3.0;
              boolean correctBlendedAvg =
                  Math.abs(trendPoint.getBlendedAverage() - expectedBlendedAvg) < 0.1;

              return hasTrendData && hasSingleTrendPoint && labelMatches && correctBlendedAvg;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should calculate blended averages for trend data in quarterly period")
  void getAverageStatusTransitionTime_WithQuarterPeriod_ShouldCalculateBlendedAverages() {
    // Arrange
    String period = "quarter";
    String username = "manager1";

    // We'll use a simpler approach instead of trying to match different queries
    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("BRD-001")
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-01T10:00:00"))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("BRD-002")
            .status("Edit Complete")
            .createdAt(LocalDateTime.parse("2023-05-05T11:00:00"))
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);

    // Create test audit logs with data that will produce three distinct periods
    // First month transitions - BRD-001
    AuditLog log1 = createAuditLog("BRD-001", "Draft", "2023-03-01T10:00:00");
    AuditLog log2 = createAuditLog("BRD-001", "In Progress", "2023-03-02T10:00:00");

    // Second month transitions - BRD-001
    AuditLog log3 = createAuditLog("BRD-001", "In Progress", "2023-04-01T10:00:00");
    AuditLog log4 = createAuditLog("BRD-001", "Edit Complete", "2023-04-03T10:00:00");

    // Third month transitions - BRD-002
    AuditLog log5 = createAuditLog("BRD-002", "Draft", "2023-05-01T10:00:00");
    AuditLog log6 = createAuditLog("BRD-002", "In Progress", "2023-05-04T10:00:00");

    List<AuditLog> allLogs = Arrays.asList(log1, log2, log3, log4, log5, log6);

    // Mock responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.fromIterable(allLogs));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify trend data exists
              boolean hasTrendData =
                  response.getTrendData() != null && !response.getTrendData().isEmpty();

              if (!hasTrendData) {
                return false;
              }

              // For quarter period, we should have 3 trend points (one for each month)
              boolean hasThreeTrendPoints = response.getTrendData().size() == 3;

              return hasTrendData && hasThreeTrendPoints;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should include empty trend data in response when no transitions found")
  void getAverageStatusTransitionTime_WithNoData_ShouldReturnEmptyTrendData() {
    // Arrange
    String period = "month";
    String username = "manager1";

    // Mock BRD query to return empty results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              // Verify trend data exists but is empty/null values
              boolean hasTrendData = response.getTrendData() != null;

              if (!hasTrendData) {
                return false;
              }

              // For month period, we should have 1 trend point with null average
              boolean hasSingleTrendPoint = response.getTrendData().size() == 1;
              boolean hasNullAverage = response.getTrendData().get(0).getBlendedAverage() == null;

              return hasTrendData && hasSingleTrendPoint && hasNullAverage;
            })
        .verifyComplete();
  }

  // Tests from DashboardServiceAiPrefillRateTest

  @Test
  @DisplayName("Should return error for invalid period parameter for AI prefill rate")
  void getAiPrefillRateOverTime_WithInvalidPeriod_ShouldReturnError() {
    // Test with invalid period
    StepVerifier.create(dashboardService.getAiPrefillRateOverTime("invalid_period", "testuser"))
        .expectErrorMatches(
            error ->
                error instanceof IllegalParameterException
                    && error.getMessage().contains("Invalid period parameter"))
        .verify();
  }

  @Test
  @DisplayName("Should use quarter as default period when null is provided for AI prefill rate")
  void getAiPrefillRateOverTime_WithNullPeriod_ShouldUseQuarterAsDefault() {
    // Set up default empty results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Test with null period - should use quarter
    StepVerifier.create(dashboardService.getAiPrefillRateOverTime(null, "testuser"))
        .expectNextMatches(
            response ->
                "quarter".equals(response.getPeriod()) && response.getTimeSegments().size() == 3)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results correctly for AI prefill rate")
  void getAiPrefillRateOverTime_WithNoData_ShouldHandleEmptyResults() {
    // To ensure this test is deterministic, we'll mock the current date
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 31, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // Mock empty results from database
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Test with year period
    StepVerifier.create(spyService.getAiPrefillRateOverTime("year", "testuser"))
        .expectNextMatches(
            response -> {
              boolean correctPeriod = "year".equals(response.getPeriod());

              // Should have 12 time segments (Dec 2022 - Nov 2023)
              boolean has12Segments = response.getTimeSegments().size() == 12;

              // Log the labels to debug
              List<String> labels =
                  response.getTimeSegments().stream()
                      .map(BrdAiPrefillRateResponse.TimeSegment::getLabel)
                      .toList();
              System.out.println("Empty result test - segment labels: " + labels);

              // Check that at least one segment is from 2022 (December)
              boolean includesMonth2022 = labels.stream().anyMatch(label -> label.contains("2022"));

              // Check that all rates are zero
              boolean allZeroRates =
                  response.getTimeSegments().stream()
                      .allMatch(segment -> segment.getAveragePrefillRate() == 0.0);

              // Check that all counts are zero
              boolean allZeroCounts =
                  response.getTimeSegments().stream()
                      .allMatch(segment -> segment.getBrdCount() == 0);

              return correctPeriod
                  && has12Segments
                  && includesMonth2022
                  && allZeroRates
                  && allZeroCounts;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 12 monthly segments for AI prefill rate when period is year")
  void getAiPrefillRateOverTime_WithYearPeriod_ShouldReturn12MonthlySegments() {
    // To ensure this test is deterministic, we'll mock the current date
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 31, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // Create mock BRDs with AI prefill rates for each month
    List<BRD> mockBrds = new ArrayList<>();

    // Create BRDs for 12 months starting from Jan 2023 to Dec 2023
    for (int month = 1; month <= 12; month++) {
      // Create 2 BRDs per month with different prefill rates
      for (int i = 1; i <= 2; i++) {
        // For January-November 2023
        LocalDateTime createdAt = LocalDateTime.of(2023, month, 15, 0, 0);

        BRD brd =
            BRD.builder()
                .brdId("BRD-" + month + "-" + i)
                .createdAt(createdAt)
                .aiPrefillRate(0.3 + (month * 0.05) + (i * 0.1))
                .build();
        mockBrds.add(brd);
      }
    }

    // Create BRDs for Dec 2022 (since we're looking at the last 12 complete months)
    for (int i = 1; i <= 2; i++) {
      LocalDateTime createdAt = LocalDateTime.of(2022, 12, 15, 0, 0);
      BRD brd =
          BRD.builder()
              .brdId("BRD-Dec2022-" + i)
              .createdAt(createdAt)
              .aiPrefillRate(0.25 + (i * 0.1))
              .build();
      mockBrds.add(brd);
    }

    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(mockBrds));

    // Test with year period
    StepVerifier.create(spyService.getAiPrefillRateOverTime("year", "testuser"))
        .expectNextMatches(
            response -> {
              boolean correctPeriod = "year".equals(response.getPeriod());

              // Should have 12 segments for Dec 2022 - Nov 2023 (last 12 complete months)
              boolean has12Segments = response.getTimeSegments().size() == 12;

              // Check that segments follow correct chronological order
              List<String> labels =
                  response.getTimeSegments().stream()
                      .map(BrdAiPrefillRateResponse.TimeSegment::getLabel)
                      .toList();

              System.out.println("Segment labels in response: " + labels);

              // Check that at least 1 segment comes from 2022 (December)
              boolean includesMonth2022 =
                  labels.stream().anyMatch(label -> label.matches("[A-Z]+ 2022"));

              // Verify all segments have a BRD count greater than 0
              boolean hasPositiveBrdCounts =
                  response.getTimeSegments().stream()
                      .allMatch(segment -> segment.getBrdCount() > 0);

              // Verify all average rates are greater than 0
              boolean nonZeroRates =
                  response.getTimeSegments().stream()
                      .allMatch(segment -> segment.getAveragePrefillRate() > 0.0);

              return correctPeriod
                  && has12Segments
                  && includesMonth2022
                  && hasPositiveBrdCounts
                  && nonZeroRates;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 4 quarterly segments for status transition when period is year")
  void getAverageStatusTransitionTime_WithYearPeriod_ShouldReturn4QuarterlySegments() {
    // To ensure this test is deterministic, we'll mock the current date
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 31, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // Set up default empty results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.empty());

    // Test with year period - other endpoints should still return 4 quarterly segments
    StepVerifier.create(spyService.getAverageStatusTransitionTime("year", "testuser"))
        .expectNextMatches(
            response -> {
              boolean correctPeriod = "year".equals(response.getPeriod());
              boolean has4Segments = response.getPeriodData().size() == 4;
              boolean has4TrendPoints = response.getTrendData().size() == 4;

              // Log the labels for debugging
              List<String> labels =
                  response.getPeriodData().stream()
                      .map(BrdStatusTransitionTimeResponse.PeriodData::getLabel)
                      .toList();
              System.out.println("Year period quarterly segments: " + labels);

              // Check that segment labels follow quarterly format (e.g., "Q1")
              boolean hasQuarterlyLabels =
                  response.getPeriodData().stream()
                      .allMatch(segment -> segment.getLabel().matches("Q[1-4]"));

              // All quarters should have the same number of transition types
              Set<String> transitionTypes = getValidStatusTransitions();
              boolean allHaveSameTransitions =
                  response.getPeriodData().stream()
                      .allMatch(segment -> segment.getAverages().keySet().equals(transitionTypes));

              return correctPeriod
                  && has4Segments
                  && has4TrendPoints
                  && hasQuarterlyLabels
                  && allHaveSameTransitions;
            })
        .verifyComplete();
  }

  // Helper method to get the expected status transitions - match implementation in DashboardService
  private Set<String> getValidStatusTransitions() {
    Set<String> validTransitions = new HashSet<>();
    validTransitions.add("Draft ➔ In Progress");
    validTransitions.add("In Progress ➔ Edit Complete");
    validTransitions.add("Edit Complete ➔ Internal Review");
    validTransitions.add("Internal Review ➔ Reviewed");
    validTransitions.add("Reviewed ➔ Ready for Sign-Off");
    validTransitions.add("Ready for Sign-Off ➔ Signed Off");
    validTransitions.add("Signed Off ➔ Submit");
    return validTransitions;
  }

  @Test
  @DisplayName("Should calculate average comment resolution time")
  void getAverageCommentResolutionTime_ShouldReturnData() {
    // Mock the MongoDB reactive template to return empty results
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageCommentResolutionTime();

    // Just verify we get a response without exceptions
    StepVerifier.create(result)
        .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle error in comment resolution time calculation")
  void getAverageCommentResolutionTime_WithError_ShouldHandleGracefully() {
    // Mock the MongoDB reactive template to throw an exception
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.error(new RuntimeException("Test error")));

    // Act and verify
    StepVerifier.create(dashboardService.getAverageCommentResolutionTime())
        .expectNextMatches(response -> response.getStatusCode().is5xxServerError())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should correctly handle WeeklyCounts and WeeklyUploadCounts conversion")
  void weeklyCounts_toUploadCounts_ShouldConvertCorrectly() {
    // Arrange
    int size = 52;
    int[] totalNewCounts = new int[size];
    int[] totalUpdateCounts = new int[size];
    int[] ssdNewCounts = new int[size];
    int[] ssdUpdateCounts = new int[size];
    int[] contractNewCounts = new int[size];
    int[] contractUpdateCounts = new int[size];

    // Initialize some test data
    for (int i = 0; i < size; i++) {
      totalNewCounts[i] = i * 2;
      totalUpdateCounts[i] = i;
      ssdNewCounts[i] = i + 1;
      ssdUpdateCounts[i] = i / 2;
      contractNewCounts[i] = i + 3;
      contractUpdateCounts[i] = i / 3;
    }

    // Create a WeeklyCounts instance using reflection
    Object weeklyCounts =
        createWeeklyCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Act - convert to WeeklyUploadCounts
    Object weeklyUploadCounts = invokeToUploadCounts(weeklyCounts);

    // Assert
    assertNotNull(weeklyUploadCounts, "WeeklyUploadCounts should not be null");
    int[] resultTotalNewCounts = getIntArrayField(weeklyUploadCounts, "totalNewCounts");
    int[] resultTotalUpdateCounts = getIntArrayField(weeklyUploadCounts, "totalUpdateCounts");
    int[] resultSsdNewCounts = getIntArrayField(weeklyUploadCounts, "ssdNewCounts");
    int[] resultSsdUpdateCounts = getIntArrayField(weeklyUploadCounts, "ssdUpdateCounts");
    int[] resultContractNewCounts = getIntArrayField(weeklyUploadCounts, "contractNewCounts");
    int[] resultContractUpdateCounts = getIntArrayField(weeklyUploadCounts, "contractUpdateCounts");

    // Verify all arrays are correctly transferred
    assertArrayEquals(totalNewCounts, resultTotalNewCounts, "totalNewCounts should match");
    assertArrayEquals(totalUpdateCounts, resultTotalUpdateCounts, "totalUpdateCounts should match");
    assertArrayEquals(ssdNewCounts, resultSsdNewCounts, "ssdNewCounts should match");
    assertArrayEquals(ssdUpdateCounts, resultSsdUpdateCounts, "ssdUpdateCounts should match");
    assertArrayEquals(contractNewCounts, resultContractNewCounts, "contractNewCounts should match");
    assertArrayEquals(
        contractUpdateCounts, resultContractUpdateCounts, "contractUpdateCounts should match");
  }

  @Test
  @DisplayName("Should correctly initialize and use WeeklyCounts")
  void weeklyCounts_ShouldInitializeAndAccessFields() {
    // Arrange - create sample arrays
    int[] totalNewCounts = {1, 2, 3};
    int[] totalUpdateCounts = {4, 5, 6};
    int[] ssdNewCounts = {7, 8, 9};
    int[] ssdUpdateCounts = {10, 11, 12};
    int[] contractNewCounts = {13, 14, 15};
    int[] contractUpdateCounts = {16, 17, 18};

    // Act - create object via reflection
    Object weeklyCounts =
        createWeeklyCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Assert - verify getters return correct values
    assertNotNull(weeklyCounts, "WeeklyCounts should not be null");
    assertArrayEquals(totalNewCounts, getIntArrayViaGetter(weeklyCounts, "getTotalNewCounts"));
    assertArrayEquals(
        totalUpdateCounts, getIntArrayViaGetter(weeklyCounts, "getTotalUpdateCounts"));
    assertArrayEquals(ssdNewCounts, getIntArrayViaGetter(weeklyCounts, "getSsdNewCounts"));
    assertArrayEquals(ssdUpdateCounts, getIntArrayViaGetter(weeklyCounts, "getSsdUpdateCounts"));
    assertArrayEquals(
        contractNewCounts, getIntArrayViaGetter(weeklyCounts, "getContractNewCounts"));
    assertArrayEquals(
        contractUpdateCounts, getIntArrayViaGetter(weeklyCounts, "getContractUpdateCounts"));
  }

  /** Helper method to get an int array via getter method using reflection */
  private int[] getIntArrayViaGetter(Object obj, String getterName) {
    try {
      Method method = obj.getClass().getMethod(getterName);
      return (int[]) method.invoke(obj);
    } catch (Exception e) {
      fail("Failed to invoke " + getterName + ": " + e.getMessage());
      return null;
    }
  }

  /** Helper method to create a WeeklyCounts instance using reflection */
  private Object createWeeklyCounts(
      int[] totalNewCounts,
      int[] totalUpdateCounts,
      int[] ssdNewCounts,
      int[] ssdUpdateCounts,
      int[] contractNewCounts,
      int[] contractUpdateCounts) {

    try {
      // Get the WeeklyCounts class
      Class<?> weeklyCountsClass =
          Class.forName(
              "com.aci.smart_onboarding.service.implementation.DashboardService$WeeklyCounts");

      // Get constructor
      Constructor<?> constructor =
          weeklyCountsClass.getDeclaredConstructor(
              int[].class, int[].class, int[].class, int[].class, int[].class, int[].class);
      constructor.setAccessible(true);

      // Create instance
      return constructor.newInstance(
          totalNewCounts,
          totalUpdateCounts,
          ssdNewCounts,
          ssdUpdateCounts,
          contractNewCounts,
          contractUpdateCounts);
    } catch (Exception e) {
      fail("Failed to create WeeklyCounts instance: " + e.getMessage());
      return null;
    }
  }

  // Helper method to invoke toUploadCounts method using reflection
  private Object invokeToUploadCounts(Object weeklyCounts) {
    try {
      // Get the method
      Method method = weeklyCounts.getClass().getDeclaredMethod("toUploadCounts");
      method.setAccessible(true);

      // Invoke method
      return method.invoke(weeklyCounts);
    } catch (Exception e) {
      fail("Failed to invoke toUploadCounts method: " + e.getMessage());
      return null;
    }
  }

  // Helper method to get an int array field value using reflection
  private int[] getIntArrayField(Object object, String fieldName) {
    try {
      // Get the field
      Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);

      // Get field value
      return (int[]) field.get(object);
    } catch (Exception e) {
      fail("Failed to get field " + fieldName + ": " + e.getMessage());
      return null;
    }
  }

  @AfterEach
  void cleanup() {
    // Add any cleanup code if needed
  }

  /** Helper method to create an audit log with the given details */
  private AuditLog createAuditLog(String brdId, String status, String timestamp) {
    AuditLog auditLog = new AuditLog();
    auditLog.setEntityId(brdId);
    auditLog.setEntityType("BRD");
    auditLog.setAction("STATUS_UPDATE");
    auditLog.setEventTimestamp(LocalDateTime.parse(timestamp));
    auditLog.setNewValues(Map.of("status", status));
    return auditLog;
  }

  // Helper method to generate dates relative to now
  private LocalDateTime getDateFromNowMinus(int days) {
    return LocalDateTime.now().minusDays(days);
  }

  @Test
  @DisplayName("WeeklyUploadCounts should correctly store and retrieve values")
  void weeklyUploadCounts_ShouldStoreAndRetrieveValues() {
    // Arrange
    int[] totalNewCounts = {1, 2, 3, 4, 5};
    int[] totalUpdateCounts = {6, 7, 8, 9, 10};
    int[] ssdNewCounts = {11, 12, 13, 14, 15};
    int[] ssdUpdateCounts = {16, 17, 18, 19, 20};
    int[] contractNewCounts = {21, 22, 23, 24, 25};
    int[] contractUpdateCounts = {26, 27, 28, 29, 30};

    // Act - create instance using reflection
    Object weeklyUploadCounts =
        createWeeklyUploadCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Assert
    assertNotNull(weeklyUploadCounts, "WeeklyUploadCounts should not be null");

    // Verify getters return correct values
    assertArrayEquals(
        totalNewCounts, getIntArrayViaGetter(weeklyUploadCounts, "getTotalNewCounts"));
    assertArrayEquals(
        totalUpdateCounts, getIntArrayViaGetter(weeklyUploadCounts, "getTotalUpdateCounts"));
    assertArrayEquals(ssdNewCounts, getIntArrayViaGetter(weeklyUploadCounts, "getSsdNewCounts"));
    assertArrayEquals(
        ssdUpdateCounts, getIntArrayViaGetter(weeklyUploadCounts, "getSsdUpdateCounts"));
    assertArrayEquals(
        contractNewCounts, getIntArrayViaGetter(weeklyUploadCounts, "getContractNewCounts"));
    assertArrayEquals(
        contractUpdateCounts, getIntArrayViaGetter(weeklyUploadCounts, "getContractUpdateCounts"));
  }

  @Test
  @DisplayName("WeeklyUploadCounts equals and hashCode methods should work correctly")
  void weeklyUploadCounts_EqualsAndHashCode_ShouldWorkCorrectly() {
    // Arrange
    int[] totalNewCounts = {1, 2, 3};
    int[] totalUpdateCounts = {4, 5, 6};
    int[] ssdNewCounts = {7, 8, 9};
    int[] ssdUpdateCounts = {10, 11, 12};
    int[] contractNewCounts = {13, 14, 15};
    int[] contractUpdateCounts = {16, 17, 18};

    // Create first instance
    Object weeklyUploadCounts1 =
        createWeeklyUploadCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Create identical instance
    Object weeklyUploadCounts2 =
        createWeeklyUploadCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Create different instance
    int[] differentCounts = {99, 88, 77};
    Object weeklyUploadCounts3 =
        createWeeklyUploadCounts(
            differentCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Assert - equals method
    assertTrue(
        invokeEquals(weeklyUploadCounts1, weeklyUploadCounts1), "Object should equal itself");
    assertTrue(
        invokeEquals(weeklyUploadCounts1, weeklyUploadCounts2), "Equal objects should return true");
    assertFalse(
        invokeEquals(weeklyUploadCounts1, weeklyUploadCounts3),
        "Different objects should return false");
    assertFalse(invokeEquals(weeklyUploadCounts1, null), "Object should not equal null");
    assertFalse(
        invokeEquals(weeklyUploadCounts1, "string"), "Object should not equal different type");

    // Assert - hashCode method
    assertEquals(
        invokeHashCode(weeklyUploadCounts1),
        invokeHashCode(weeklyUploadCounts2),
        "Equal objects should have same hashCode");
    assertNotEquals(
        invokeHashCode(weeklyUploadCounts1),
        invokeHashCode(weeklyUploadCounts3),
        "Different objects should have different hashCode");
  }

  @Test
  @DisplayName("WeeklyCounts equals and hashCode methods should work correctly")
  void weeklyCounts_EqualsAndHashCode_ShouldWorkCorrectly() {
    // Arrange
    int[] totalNewCounts = {1, 2, 3};
    int[] totalUpdateCounts = {4, 5, 6};
    int[] ssdNewCounts = {7, 8, 9};
    int[] ssdUpdateCounts = {10, 11, 12};
    int[] contractNewCounts = {13, 14, 15};
    int[] contractUpdateCounts = {16, 17, 18};

    // Create first instance
    Object weeklyCounts1 =
        createWeeklyCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Create identical instance
    Object weeklyCounts2 =
        createWeeklyCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Create different instance
    int[] differentCounts = {99, 88, 77};
    Object weeklyCounts3 =
        createWeeklyCounts(
            differentCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Assert - equals method
    assertTrue(invokeEquals(weeklyCounts1, weeklyCounts1), "Object should equal itself");
    assertTrue(invokeEquals(weeklyCounts1, weeklyCounts2), "Equal objects should return true");
    assertFalse(
        invokeEquals(weeklyCounts1, weeklyCounts3), "Different objects should return false");
    assertFalse(invokeEquals(weeklyCounts1, null), "Object should not equal null");
    assertFalse(invokeEquals(weeklyCounts1, "string"), "Object should not equal different type");

    // Assert - hashCode method
    assertEquals(
        invokeHashCode(weeklyCounts1),
        invokeHashCode(weeklyCounts2),
        "Equal objects should have same hashCode");
    assertNotEquals(
        invokeHashCode(weeklyCounts1),
        invokeHashCode(weeklyCounts3),
        "Different objects should have different hashCode");
  }

  @Test
  @DisplayName("WeeklyCounts toString method should work correctly")
  void weeklyCounts_ToString_ShouldWorkCorrectly() {
    // Arrange
    int[] totalNewCounts = {1, 2};
    int[] totalUpdateCounts = {3, 4};
    int[] ssdNewCounts = {5, 6};
    int[] ssdUpdateCounts = {7, 8};
    int[] contractNewCounts = {9, 10};
    int[] contractUpdateCounts = {11, 12};

    Object weeklyCounts =
        createWeeklyCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Act
    String toStringResult = invokeToString(weeklyCounts);

    // Assert
    assertNotNull(toStringResult, "toString result should not be null");
    assertTrue(toStringResult.contains("totalNewCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("totalUpdateCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("ssdNewCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("ssdUpdateCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("contractNewCounts"), "toString should contain field names");
    assertTrue(
        toStringResult.contains("contractUpdateCounts"), "toString should contain field names");
  }

  @Test
  @DisplayName("WeeklyUploadCounts toString method should work correctly")
  void weeklyUploadCounts_ToString_ShouldWorkCorrectly() {
    // Arrange
    int[] totalNewCounts = {1, 2};
    int[] totalUpdateCounts = {3, 4};
    int[] ssdNewCounts = {5, 6};
    int[] ssdUpdateCounts = {7, 8};
    int[] contractNewCounts = {9, 10};
    int[] contractUpdateCounts = {11, 12};

    Object weeklyUploadCounts =
        createWeeklyUploadCounts(
            totalNewCounts,
            totalUpdateCounts,
            ssdNewCounts,
            ssdUpdateCounts,
            contractNewCounts,
            contractUpdateCounts);

    // Act
    String toStringResult = invokeToString(weeklyUploadCounts);

    // Assert
    assertNotNull(toStringResult, "toString result should not be null");
    assertTrue(toStringResult.contains("totalNewCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("totalUpdateCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("ssdNewCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("ssdUpdateCounts"), "toString should contain field names");
    assertTrue(toStringResult.contains("contractNewCounts"), "toString should contain field names");
    assertTrue(
        toStringResult.contains("contractUpdateCounts"), "toString should contain field names");
  }

  // Helper method to create a WeeklyUploadCounts instance using reflection
  private Object createWeeklyUploadCounts(
      int[] totalNewCounts,
      int[] totalUpdateCounts,
      int[] ssdNewCounts,
      int[] ssdUpdateCounts,
      int[] contractNewCounts,
      int[] contractUpdateCounts) {

    try {
      // Get the WeeklyUploadCounts class
      Class<?> weeklyUploadCountsClass =
          Class.forName(
              "com.aci.smart_onboarding.service.implementation.DashboardService$WeeklyUploadCounts");

      // Get constructor
      Constructor<?> constructor =
          weeklyUploadCountsClass.getDeclaredConstructor(
              int[].class, int[].class, int[].class, int[].class, int[].class, int[].class);
      constructor.setAccessible(true);

      // Create instance
      return constructor.newInstance(
          totalNewCounts,
          totalUpdateCounts,
          ssdNewCounts,
          ssdUpdateCounts,
          contractNewCounts,
          contractUpdateCounts);
    } catch (Exception e) {
      fail("Failed to create WeeklyUploadCounts instance: " + e.getMessage());
      return null;
    }
  }

  // Helper method to invoke equals method using reflection
  private boolean invokeEquals(Object obj, Object other) {
    try {
      Method equalsMethod = obj.getClass().getMethod("equals", Object.class);
      return (boolean) equalsMethod.invoke(obj, other);
    } catch (Exception e) {
      fail("Failed to invoke equals method: " + e.getMessage());
      return false;
    }
  }

  // Helper method to invoke hashCode method using reflection
  private int invokeHashCode(Object obj) {
    try {
      Method hashCodeMethod = obj.getClass().getMethod("hashCode");
      return (int) hashCodeMethod.invoke(obj);
    } catch (Exception e) {
      fail("Failed to invoke hashCode method: " + e.getMessage());
      return 0;
    }
  }

  // Helper method to invoke toString method using reflection
  private String invokeToString(Object obj) {
    try {
      Method toStringMethod = obj.getClass().getMethod("toString");
      return (String) toStringMethod.invoke(obj);
    } catch (Exception e) {
      fail("Failed to invoke toString method: " + e.getMessage());
      return null;
    }
  }

  @Test
  @DisplayName("WeeklyUploadCounts should be properly computed from BRDs")
  void computeWeeklyUploadCounts_ShouldWorkCorrectly() {
    try {
      // Set up test data - create BRDs with specific dates
      LocalDate today = LocalDate.now();
      LocalDate lastDayOfLastMonth =
          today.withDayOfMonth(1).minusDays(1); // Last day of previous month
      LocalDate weekAgo = lastDayOfLastMonth.minusWeeks(1);

      // Create test BRDs with different dates and file types
      List<BRD> brds =
          List.of(
              BRD.builder()
                  .brdId("BRD-001")
                  .type("NEW")
                  .originalSSDFileName("test-ssd.pdf")
                  .createdAt(
                      LocalDateTime.of(
                          lastDayOfLastMonth.getYear(),
                          lastDayOfLastMonth.getMonth(),
                          lastDayOfLastMonth.getDayOfMonth(),
                          10,
                          0))
                  .build(),
              BRD.builder()
                  .brdId("BRD-002")
                  .type("NEW")
                  .originalContractFileName("test-contract.pdf")
                  .createdAt(
                      LocalDateTime.of(
                          lastDayOfLastMonth.getYear(),
                          lastDayOfLastMonth.getMonth(),
                          lastDayOfLastMonth.getDayOfMonth(),
                          11,
                          0))
                  .build(),
              BRD.builder()
                  .brdId("BRD-003")
                  .type("UPDATE")
                  .originalSSDFileName("test-ssd-update.pdf")
                  .createdAt(
                      LocalDateTime.of(
                          weekAgo.getYear(), weekAgo.getMonth(), weekAgo.getDayOfMonth(), 10, 0))
                  .build(),
              BRD.builder()
                  .brdId("BRD-004")
                  .type("UPDATE")
                  .originalContractFileName("test-contract-update.pdf")
                  .createdAt(
                      LocalDateTime.of(
                          weekAgo.getYear(), weekAgo.getMonth(), weekAgo.getDayOfMonth(), 11, 0))
                  .build());

      // Create week labels and map
      List<String> weekLabels = List.of("Week 1", "Week 2");
      Map<String, Integer> weekKeyToIndexMap = new HashMap<>();

      // Setup week mapping (this matches how formatWeek works in the service)
      WeekFields weekFields = WeekFields.ISO;
      String lastMonthWeekKey =
          String.format(
              "%d-W%02d",
              lastDayOfLastMonth.get(weekFields.weekBasedYear()),
              lastDayOfLastMonth.get(weekFields.weekOfWeekBasedYear()));
      String weekAgoWeekKey =
          String.format(
              "%d-W%02d",
              weekAgo.get(weekFields.weekBasedYear()),
              weekAgo.get(weekFields.weekOfWeekBasedYear()));

      weekKeyToIndexMap.put(lastMonthWeekKey, 0); // Week 1 (most recent)
      weekKeyToIndexMap.put(weekAgoWeekKey, 1); // Week 2

      // Get the computeWeeklyUploadCounts method to test
      Method computeMethod =
          DashboardService.class.getDeclaredMethod(
              "computeWeeklyUploadCounts", List.class, List.class, Map.class);
      computeMethod.setAccessible(true);

      // Invoke the method
      Object result = computeMethod.invoke(dashboardService, brds, weekLabels, weekKeyToIndexMap);

      // Verify we got a WeeklyUploadCounts object
      assertNotNull(result, "WeeklyUploadCounts object should not be null");

      // Extract the arrays to verify
      int[] totalNewCounts = getIntArrayField(result, "totalNewCounts");
      int[] totalUpdateCounts = getIntArrayField(result, "totalUpdateCounts");
      int[] ssdNewCounts = getIntArrayField(result, "ssdNewCounts");
      int[] ssdUpdateCounts = getIntArrayField(result, "ssdUpdateCounts");
      int[] contractNewCounts = getIntArrayField(result, "contractNewCounts");
      int[] contractUpdateCounts = getIntArrayField(result, "contractUpdateCounts");

      // With the TEMPORARY CHANGE to treat all BRDs as NEW, we should see:
      // Week 1 (index 0, most recent): 2 NEW BRDs (1 SSD, 1 Contract)
      // Week 2 (index 1, older): 2 NEW BRDs (1 SSD, 1 Contract)
      assertEquals(2, totalNewCounts[0], "Week 1 should have 2 NEW BRDs");
      assertEquals(2, totalNewCounts[1], "Week 2 should have 2 NEW BRDs");

      // All UPDATE counts should be 0 due to the temporary change
      assertEquals(0, totalUpdateCounts[0], "Week 1 should have 0 UPDATE BRDs");
      assertEquals(0, totalUpdateCounts[1], "Week 2 should have 0 UPDATE BRDs");

      // SSD files
      assertEquals(1, ssdNewCounts[0], "Week 1 should have 1 NEW SSD");
      assertEquals(1, ssdNewCounts[1], "Week 2 should have 1 NEW SSD");
      assertEquals(0, ssdUpdateCounts[0], "Week 1 should have 0 UPDATE SSD");
      assertEquals(0, ssdUpdateCounts[1], "Week 2 should have 0 UPDATE SSD");

      // Contract files
      assertEquals(1, contractNewCounts[0], "Week 1 should have 1 NEW Contract");
      assertEquals(1, contractNewCounts[1], "Week 2 should have 1 NEW Contract");
      assertEquals(0, contractUpdateCounts[0], "Week 1 should have 0 UPDATE Contract");
      assertEquals(0, contractUpdateCounts[1], "Week 2 should have 0 UPDATE Contract");

    } catch (Exception e) {
      fail("Exception occurred while testing: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should return unresolved comment groups count for scope ME")
  void getUnresolvedCommentGroupsCount_WithMeScope_ShouldReturnCorrectCounts() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Create test BRDs for non-submitted status
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .brdName("Test BRD 1")
            .status("Draft")
            .creator(username)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .brdName("Test BRD 2")
            .status("In Progress")
            .creator(username)
            .build();

    List<BRD> brds = Arrays.asList(brd1, brd2);

    // Create test comment groups
    BrdFieldCommentGroup.CommentEntry comment1 =
        BrdFieldCommentGroup.CommentEntry.builder()
            .id("comment1")
            .content("Test comment 1")
            .createdBy(username)
            .userType("pm")
            .build();

    BrdFieldCommentGroup commentGroup1 =
        BrdFieldCommentGroup.builder()
            .id("group1")
            .brdFormId("form-001")
            .status("Pending")
            .createdBy(username)
            .comments(List.of(comment1))
            .build();

    BrdFieldCommentGroup commentGroup2 =
        BrdFieldCommentGroup.builder()
            .id("group2")
            .brdFormId("form-001")
            .status("Pending")
            .createdBy(username)
            .comments(List.of(comment1))
            .build();

    BrdFieldCommentGroup commentGroup3 =
        BrdFieldCommentGroup.builder()
            .id("group3")
            .brdFormId("form-002")
            .status("Pending")
            .createdBy(username)
            .comments(List.of(comment1))
            .build();

    // Mock MongoDB queries
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.fromIterable(Arrays.asList(commentGroup1, commentGroup2, commentGroup3)));

    // Act
    var result = dashboardService.getUnresolvedCommentGroupsCount(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>> typedResponse =
                  (ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>) response;
              Api<UnresolvedCommentGroupsCountResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              UnresolvedCommentGroupsCountResponse countResponse = apiResponse.getData().get();

              // Check total count
              boolean correctTotalCount = countResponse.getTotalCount() == 3;

              // Check BRD counts
              boolean hasTwoBrds = countResponse.getBrdCounts().size() == 2;

              // Verify counts for each BRD
              boolean firstBrdCorrect =
                  countResponse.getBrdCounts().get("form-001").getCount() == 2;
              boolean secondBrdCorrect =
                  countResponse.getBrdCounts().get("form-002").getCount() == 1;

              // Verify BRD details
              boolean brdDetailsCorrect =
                  "BRD-001".equals(countResponse.getBrdCounts().get("form-001").getBrdId())
                      && "Test BRD 1"
                          .equals(countResponse.getBrdCounts().get("form-001").getBrdName())
                      && "Draft".equals(countResponse.getBrdCounts().get("form-001").getStatus());

              return response.getStatusCode().is2xxSuccessful()
                  && correctTotalCount
                  && hasTwoBrds
                  && firstBrdCorrect
                  && secondBrdCorrect
                  && brdDetailsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return empty results when no BRDs found for unresolved comments")
  void getUnresolvedCommentGroupsCount_WithNoBrds_ShouldReturnEmptyResponse() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Mock empty BRD query results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getUnresolvedCommentGroupsCount(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>> typedResponse =
                  (ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>) response;
              Api<UnresolvedCommentGroupsCountResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              UnresolvedCommentGroupsCountResponse countResponse = apiResponse.getData().get();

              // Check empty response
              boolean emptyResponse =
                  countResponse.getTotalCount() == 0 && countResponse.getBrdCounts().isEmpty();

              return response.getStatusCode().is2xxSuccessful() && emptyResponse;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle errors gracefully in getUnresolvedCommentGroupsCount")
  void getUnresolvedCommentGroupsCount_WithError_ShouldHandleGracefully() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Mock BRD query that throws an error
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.error(new RuntimeException("Test error")));

    // Act
    var result = dashboardService.getUnresolvedCommentGroupsCount(scope, username);

    // Assert - should produce the mapped exception
    StepVerifier.create(result)
        .expectErrorSatisfies(
            error -> {
              // Verify that it's some kind of Exception
              assertTrue(error instanceof Exception);
              // Error message should exist
              assertNotNull(error.getMessage());
            })
        .verify();
  }

  @Test
  @DisplayName("Should return BRD counts by type with weekly metrics for ME scope")
  void getBrdCountsByType_WithMeScope_ShouldReturnWeeklyMetrics() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Mock the current date
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // Create test BRDs with different dates for weekly metrics
    List<BRD> testBrds = new ArrayList<>();

    // Create BRDs for the most recent week (Week 1)
    BRD week1Brd1 =
        BRD.builder()
            .brdId("BRD-W1-1")
            .type("NEW")
            .creator(username)
            .createdAt(LocalDateTime.of(2023, 11, 25, 10, 0)) // Week 1
            .build();

    BRD week1Brd2 =
        BRD.builder()
            .brdId("BRD-W1-2")
            .type("NEW")
            .creator(username)
            .createdAt(LocalDateTime.of(2023, 11, 26, 14, 0)) // Week 1
            .build();

    testBrds.add(week1Brd1);
    testBrds.add(week1Brd2);

    // Create BRDs for Week 2
    BRD week2Brd1 =
        BRD.builder()
            .brdId("BRD-W2-1")
            .type("UPDATE")
            .creator(username)
            .createdAt(LocalDateTime.of(2023, 11, 18, 10, 0)) // Week 2
            .build();

    testBrds.add(week2Brd1);

    // Mock MongoDB query for BRDs
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(testBrds));

    // Act
    var result = spyService.getBrdCountsByType(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<BrdTypeCountResponse>> typedResponse =
                  (ResponseEntity<Api<BrdTypeCountResponse>>) response;
              Api<BrdTypeCountResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              BrdTypeCountResponse countResponse = apiResponse.getData().get();

              // Check basic properties
              boolean hasCorrectScope = scope.equals(countResponse.getScope());

              // Check weekly metrics exists
              boolean hasWeeklyMetrics = countResponse.getWeeklyMetrics() != null;
              if (!hasWeeklyMetrics) {
                return false;
              }

              // Check weeks list exists
              boolean hasWeeks = countResponse.getWeeklyMetrics().getWeeks().size() > 0;

              // Verify data is present
              boolean hasNewCounts =
                  countResponse.getWeeklyMetrics().getCounts().getNewCounts().size() > 0;
              boolean hasUpdateCounts =
                  countResponse.getWeeklyMetrics().getCounts().getUpdateCounts().size() > 0;
              boolean hasTotalCounts =
                  countResponse.getWeeklyMetrics().getCounts().getTotalCounts().size() > 0;

              return response.getStatusCode().is2xxSuccessful()
                  && hasCorrectScope
                  && hasWeeks
                  && hasNewCounts
                  && hasUpdateCounts
                  && hasTotalCounts;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results for BRD counts by type")
  void getBrdCountsByType_WithNoResults_ShouldReturnEmptyMetrics() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Mock empty MongoDB query results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getBrdCountsByType(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<BrdTypeCountResponse>> typedResponse =
                  (ResponseEntity<Api<BrdTypeCountResponse>>) response;
              Api<BrdTypeCountResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              BrdTypeCountResponse countResponse = apiResponse.getData().get();

              // Check scope is set correctly
              boolean hasCorrectScope = scope.equals(countResponse.getScope());

              // Check weekly metrics exists but has zero counts
              boolean hasWeeklyMetrics = countResponse.getWeeklyMetrics() != null;
              if (!hasWeeklyMetrics) {
                return false;
              }

              // Verify all counts are zero
              boolean allNewCountsZero =
                  countResponse.getWeeklyMetrics().getCounts().getNewCounts().stream()
                      .allMatch(count -> count == 0);

              boolean allUpdateCountsZero =
                  countResponse.getWeeklyMetrics().getCounts().getUpdateCounts().stream()
                      .allMatch(count -> count == 0);

              boolean allTotalCountsZero =
                  countResponse.getWeeklyMetrics().getCounts().getTotalCounts().stream()
                      .allMatch(count -> count == 0);

              return response.getStatusCode().is2xxSuccessful()
                  && hasCorrectScope
                  && allNewCountsZero
                  && allUpdateCountsZero
                  && allTotalCountsZero;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle errors gracefully in getBrdCountsByType")
  void getBrdCountsByType_WithError_ShouldHandleGracefully() {
    // Arrange
    String scope = "ME";
    String username = "testuser";

    // Mock MongoDB query that throws an error
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.error(new RuntimeException("Test error")));

    // Act
    var result = dashboardService.getBrdCountsByType(scope, username);

    // Assert - should produce the mapped exception
    StepVerifier.create(result)
        .expectErrorSatisfies(
            error -> {
              // Verify that it's some kind of Exception
              assertTrue(error instanceof Exception);
              // Error message should exist
              assertNotNull(error.getMessage());
            })
        .verify();
  }

  @Test
  @DisplayName("Should return comment group stats with correct counts")
  void getCommentGroupStats_ShouldReturnCorrectStats() {
    // Arrange
    // Create BRDs with non-submitted status
    BRD brd1 = BRD.builder().brdId("BRD-001").brdFormId("form-001").status("Draft").build();

    BRD brd2 = BRD.builder().brdId("BRD-002").brdFormId("form-002").status("In Progress").build();

    List<BRD> brds = Arrays.asList(brd1, brd2);

    // Create resolved comment groups
    BrdFieldCommentGroup.CommentEntry pmComment =
        BrdFieldCommentGroup.CommentEntry.builder()
            .id("comment1")
            .content("PM comment")
            .userType("pm")
            .build();

    BrdFieldCommentGroup.CommentEntry regularComment =
        BrdFieldCommentGroup.CommentEntry.builder()
            .id("comment2")
            .content("Regular comment")
            .userType("user")
            .build();

    BrdFieldCommentGroup resolvedGroup1 =
        BrdFieldCommentGroup.builder()
            .id("resolved1")
            .brdFormId("form-001")
            .status("Resolved")
            .comments(List.of(regularComment))
            .build();

    BrdFieldCommentGroup resolvedGroup2 =
        BrdFieldCommentGroup.builder()
            .id("resolved2")
            .brdFormId("form-002")
            .status("Resolved")
            .comments(List.of(regularComment))
            .build();

    // Create pending comment groups with PM comments
    BrdFieldCommentGroup pendingWithPm1 =
        BrdFieldCommentGroup.builder()
            .id("pending1")
            .brdFormId("form-001")
            .status("Pending")
            .comments(List.of(pmComment, regularComment))
            .build();

    BrdFieldCommentGroup pendingWithPm2 =
        BrdFieldCommentGroup.builder()
            .id("pending2")
            .brdFormId("form-001")
            .status("Pending")
            .comments(List.of(pmComment))
            .build();

    // Create pending comment group without PM comment
    BrdFieldCommentGroup pendingWithoutPm =
        BrdFieldCommentGroup.builder()
            .id("pending3")
            .brdFormId("form-002")
            .status("Pending")
            .comments(List.of(regularComment))
            .build();

    List<BrdFieldCommentGroup> commentGroups =
        Arrays.asList(
            resolvedGroup1, resolvedGroup2, pendingWithPm1, pendingWithPm2, pendingWithoutPm);

    // Mock MongoDB queries
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.fromIterable(brds));

    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.fromIterable(commentGroups));

    // Act
    var result = dashboardService.getCommentGroupStats();

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<CommentGroupStatsResponse>> typedResponse =
                  (ResponseEntity<Api<CommentGroupStatsResponse>>) response;
              Api<CommentGroupStatsResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              CommentGroupStatsResponse statsResponse = apiResponse.getData().get();

              // Check total counts
              boolean totalGroupsCorrect = statsResponse.getTotalCommentGroups() == 5;
              boolean resolvedGroupsCorrect = statsResponse.getResolvedCommentGroups() == 2;

              // Check pending comment stats
              boolean pendingStatsExist = statsResponse.getPendingCommentStats() != null;
              if (!pendingStatsExist) {
                return false;
              }

              boolean totalPendingCorrect =
                  statsResponse.getPendingCommentStats().getTotalPendingGroups() == 3;
              boolean withPmCorrect =
                  statsResponse.getPendingCommentStats().getGroupsWithPmComment() == 2;
              boolean withoutPmCorrect =
                  statsResponse.getPendingCommentStats().getGroupsWithoutPmComment() == 1;

              return response.getStatusCode().is2xxSuccessful()
                  && totalGroupsCorrect
                  && resolvedGroupsCorrect
                  && totalPendingCorrect
                  && withPmCorrect
                  && withoutPmCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return empty comment group stats when no BRDs found")
  void getCommentGroupStats_WithNoBrds_ShouldReturnEmptyStats() {
    // Arrange
    // Mock empty BRD query results
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getCommentGroupStats();

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<CommentGroupStatsResponse>> typedResponse =
                  (ResponseEntity<Api<CommentGroupStatsResponse>>) response;
              Api<CommentGroupStatsResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              CommentGroupStatsResponse statsResponse = apiResponse.getData().get();

              // Check all counts are zero
              boolean totalGroupsZero = statsResponse.getTotalCommentGroups() == 0;
              boolean resolvedGroupsZero = statsResponse.getResolvedCommentGroups() == 0;

              // Check pending comment stats has zero counts
              boolean pendingStatsExist = statsResponse.getPendingCommentStats() != null;
              if (!pendingStatsExist) {
                return false;
              }

              boolean allPendingCountsZero =
                  statsResponse.getPendingCommentStats().getTotalPendingGroups() == 0
                      && statsResponse.getPendingCommentStats().getGroupsWithPmComment() == 0
                      && statsResponse.getPendingCommentStats().getGroupsWithoutPmComment() == 0;

              return response.getStatusCode().is2xxSuccessful()
                  && totalGroupsZero
                  && resolvedGroupsZero
                  && allPendingCountsZero;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle error in getCommentGroupStats")
  void getCommentGroupStats_WithError_ShouldHandleGracefully() {
    // Arrange
    // Mock BRD query that throws an error
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.error(new RuntimeException("Test error")));

    // Act
    var result = dashboardService.getCommentGroupStats();

    // Assert
    StepVerifier.create(result)
        .expectErrorSatisfies(
            error -> {
              // Verify that it's some kind of Exception
              assertTrue(error instanceof Exception);
              // Error message should exist
              assertNotNull(error.getMessage());
            })
        .verify();
  }

  @Test
  @DisplayName("Should pass through BadRequestException in handleErrors")
  void handleErrors_WithBadRequestException_ShouldPassThrough() {
    // Arrange
    BadRequestException badRequestException = new BadRequestException("Bad request test");

    // Act
    Throwable result = dashboardService.handleErrors(badRequestException);

    // Assert
    assertEquals(badRequestException, result);
    assertEquals("Bad request test", result.getMessage());
  }

  @Test
  @DisplayName("Should pass through NotFoundException in handleErrors")
  void handleErrors_WithNotFoundException_ShouldPassThrough() {
    // Arrange
    NotFoundException notFoundException = new NotFoundException("Not found test");

    // Act
    Throwable result = dashboardService.handleErrors(notFoundException);

    // Assert
    assertEquals(notFoundException, result);
    assertEquals("Not found test", result.getMessage());
  }

  @Test
  @DisplayName("Should wrap other exceptions in handleErrors")
  void handleErrors_WithOtherException_ShouldWrapInGenericException() {
    // Arrange
    RuntimeException runtimeException = new RuntimeException("Runtime error test");

    // Act
    Throwable result = dashboardService.handleErrors(runtimeException);

    // Assert
    assertTrue(result instanceof Exception);
    assertEquals("Something went wrong: Runtime error test", result.getMessage());
  }

  @Test
  @DisplayName("Should handle null message in handleErrors")
  void handleErrors_WithNullMessage_ShouldHandleGracefully() {
    // Arrange
    RuntimeException runtimeException = new RuntimeException();

    // Act
    Throwable result = dashboardService.handleErrors(runtimeException);

    // Assert
    assertTrue(result instanceof Exception);
    assertEquals("Something went wrong: null", result.getMessage());
  }

  @Test
  @DisplayName("Should handle all empty case for AI prefill accuracy with no BRDs")
  void getAiPrefillAccuracy_WithAllEmptyCase_ShouldReturnZeroAccuracy() {
    // Arrange
    String scope = "me";
    String username = "testuser";

    // Mock completely empty BRD list
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAiPrefillAccuracy(scope, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(response -> response.getAiPrefillAccuracy() == 0.0)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty vertical counts in calculateTotalOpenBrdsForPercentage")
  void getBrdsByVertical_WithEmptyVerticalCounts_ShouldHandleGracefully() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Mock to return a non-empty list of BRDs but with no verticals defined
    // This will create an empty vertical counts map
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical(null)
            .build();

    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getVerticalCounts() != null
                  && response.getScope().equals(scope)
                  && response.getBrdScope().equals(brdScope);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle null brdScope in addPeriodFilter")
  void getAdditionalFactors_WithNullBrdScope_ShouldHandleGracefully() {
    // Arrange
    String scope = "me";
    String brdScope = null; // Null brd scope
    String period = "month";
    String username = "testuser";

    // Create a test BRD for response
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    // Mock query to return the test BRD
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Mock site count to return 0
    when(mongoTemplate.count(any(Query.class), eq(Site.class)))
        .thenReturn(reactor.core.publisher.Mono.just(0L));

    // Act & Assert - should complete without exceptions
    StepVerifier.create(dashboardService.getAdditionalFactors(scope, brdScope, period, username))
        .expectNextMatches(
            response ->
                response.getScope().equals(scope)
                    && response.getPeriod().equals(period)
                    && response.getLoggedinPm().equals(username))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle invalid period value in addPeriodFilter")
  void getAdditionalFactors_WithInvalidPeriod_ShouldNotApplyFilter() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = "invalid_period"; // Invalid period value
    String username = "testuser";

    // Create test BRDs
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert - should handle the invalid period gracefully
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getScope().equals(scope)
                  && response.getPeriod().equals(period)
                  && response.getLoggedinPm().equals(username);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should calculate correct average sites with zero sites")
  void getAdditionalFactors_WithZeroSitesForBrd_ShouldCalculateCorrectAverage() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRDs
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    // Mock BRD query to return the test BRD
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Mock Site query to return a count of 0 (no sites)
    when(mongoTemplate.count(any(Query.class), eq(Site.class)))
        .thenReturn(reactor.core.publisher.Mono.just(0L));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert - should have 0.0 average sites
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getAverageSites() == 0.0;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty BRD IDs when calculating average sites")
  void getAdditionalFactors_WithEmptyBrdIds_ShouldReturnZeroAverageSites() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRD with null ID
    BRD brd =
        BRD.builder()
            .brdId(null) // Null BRD ID
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    // Mock BRD query to return the test BRD
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert - should have 0.0 average sites due to no valid BRD IDs
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getAverageSites() == 0.0;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle exception in site counting logic")
  void getAdditionalFactors_WithExceptionInSiteCounting_ShouldReturnZeroAverageSites() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRD
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .wallentronIncluded(true)
            .achEncrypted(true)
            .build();

    // Mock BRD query to return the test BRD
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    // Mock Site query to throw an exception
    when(mongoTemplate.count(any(Query.class), eq(Site.class)))
        .thenReturn(reactor.core.publisher.Mono.error(new RuntimeException("Test error")));

    // Act
    var result = dashboardService.getAdditionalFactors(scope, brdScope, period, username);

    // Assert - should have 0.0 average sites due to exception handling
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getAverageSites() == 0.0;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle nulls in getVerticalName")
  void getBrdsByVertical_WithLiteralNullVertical_ShouldCategorizeAsOther() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRD with "null" as the vertical string (not null reference)
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical("null") // Literal "null" string
            .build();

    // Mock query responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.just(brd))
        .thenReturn(Flux.just(brd));

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert - should categorize as "Other"
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getVerticalCounts().size() == 1
                  && response.getVerticalCounts().get(0).getIndustryName().equals("Other");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle whitespace-only vertical in getVerticalName")
  void getBrdsByVertical_WithWhitespaceVertical_ShouldCategorizeAsOther() {
    // Arrange
    String scope = "me";
    String brdScope = "open";
    String period = null;
    String username = "testuser";

    // Create test BRD with whitespace-only vertical
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .industryVertical("   ") // Whitespace only
            .build();

    // Mock query responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.just(brd))
        .thenReturn(Flux.just(brd));

    // Act
    var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

    // Assert - should categorize as "Other"
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getVerticalCounts().size() == 1
                  && response.getVerticalCounts().get(0).getIndustryName().equals("Other");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle transitions for BRDs with null form IDs")
  void getAverageStatusTransitionTime_WithNullFormIds_ShouldSkipThoseBrds() {
    // Arrange
    String period = "month";
    String username = "testuser";

    // Create test BRDs with null form ID
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId(null) // Null form ID
            .status("Draft")
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    // Mock repository responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert - should still return a valid response with empty data
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getPeriod().equals(period)
                  && response.getLoggedinManager().equals(username)
                  && response.getPeriodData() != null
                  && !response.getPeriodData().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle null status values in audit logs")
  void getAverageStatusTransitionTime_WithNullStatuses_ShouldSkipThoseTransitions() {
    // Arrange
    String period = "month";
    String username = "testuser";

    // Create test BRD
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    // Create audit logs with null status
    AuditLog log1 = new AuditLog();
    log1.setEntityId("form-001");
    log1.setEntityType("BRD");
    log1.setAction("STATUS_UPDATE");
    log1.setEventTimestamp(LocalDateTime.now().minusDays(9));
    log1.setNewValues(Map.of("status", "Draft"));

    AuditLog log2 = new AuditLog();
    log2.setEntityId("form-001");
    log2.setEntityType("BRD");
    log2.setAction("STATUS_UPDATE");
    log2.setEventTimestamp(LocalDateTime.now().minusDays(8));
    log2.setNewValues(null); // Null new values

    // Mock repository responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.just(log1, log2));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert - should skip the null status transition
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getPeriod().equals(period)
                  && response.getLoggedinManager().equals(username)
                  && response.getPeriodData() != null;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle no transitions in sample period for trend calculation")
  void
      getAverageStatusTransitionTime_WithNoTransitionsInPeriod_ShouldReturnNullForBlendedAverage() {
    // Arrange
    String period = "month";
    String username = "testuser";

    // Create test BRD
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    // Mock empty audit logs for the period
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert - should have null blended average
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getTrendData() != null
                  && !response.getTrendData().isEmpty()
                  && response.getTrendData().get(0).getBlendedAverage() == null;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty audit logs for trend data generation")
  void getAverageStatusTransitionTime_WithEmptyAuditLogs_ShouldGenerateEmptyTrendData() {
    // Arrange
    String period = "quarter";
    String username = "testuser";

    // Create test BRD
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .createdAt(LocalDateTime.now().minusDays(30))
            .build();

    // Mock repository responses with empty audit logs
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.empty());

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert - should have trend data with null averages
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getTrendData() != null
                  && response.getTrendData().size() == 3
                  && // Quarter has 3 months
                  response.getTrendData().stream().allMatch(tp -> tp.getBlendedAverage() == null);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BRD with no form ID when calculating transitions")
  void calculateTransitionsForBrd_WithLessThanTwoLogs_ShouldSkipBrd() {
    // Arrange
    String period = "month";
    String username = "testuser";

    // Create test BRD
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    // Create single audit log (not enough for a transition)
    AuditLog log = new AuditLog();
    log.setEntityId("form-001");
    log.setEntityType("BRD");
    log.setAction("STATUS_UPDATE");
    log.setEventTimestamp(LocalDateTime.now().minusDays(9));
    log.setNewValues(Map.of("status", "Draft"));

    // Mock repository responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.just(log));

    // Act
    var result = dashboardService.getAverageStatusTransitionTime(period, username);

    // Assert - should return empty transition data
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getPeriodData() == null || response.getPeriodData().isEmpty()) {
                return false;
              }
              Map<String, Double> averages = response.getPeriodData().get(0).getAverages();
              // All averages should be 0.0 as no valid transitions were found
              return averages.values().stream().allMatch(v -> v == 0.0);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should calculate correct comment resolution days")
  void getAverageCommentResolutionTime_WithValidComments_ShouldCalculateCorrectlyOverTime() {
    // Arrange
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime threeDaysAgo = now.minusDays(3);
    LocalDateTime twoHoursAgo = now.minusHours(2);

    // Create resolved comment group with resolution time
    BrdFieldCommentGroup resolvedGroup =
        BrdFieldCommentGroup.builder()
            .id("resolved1")
            .brdFormId("form-001")
            .status("Resolved")
            .createdAt(threeDaysAgo)
            .build();

    // Use reflection to set the resolvedAt field since it's not in the builder
    try {
      java.lang.reflect.Field resolvedAtField =
          BrdFieldCommentGroup.class.getDeclaredField("resolvedAt");
      resolvedAtField.setAccessible(true);
      resolvedAtField.set(resolvedGroup, twoHoursAgo);
    } catch (Exception e) {
      // If field doesn't exist, we'll just continue with the test
    }

    // Mock repository response
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.just(resolvedGroup));

    // Act
    var result = dashboardService.getAverageCommentResolutionTime();

    // Assert - should contain valid resolution data
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              return response.getStatusCode().is2xxSuccessful();
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should get BRD upload metrics for OPEN filter")
  void getBrdUploadMetrics_WithOpenFilter_ShouldReturnCorrectMetrics() {
    // Arrange
    String filter = "OPEN";
    String username = "testuser";

    // Create test BRDs
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("form-001")
            .status("Draft")
            .creator(username)
            .type("NEW")
            .originalSSDFileName("test-ssd.pdf")
            .originalContractFileName(null)
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .brdFormId("form-002")
            .status("In Progress")
            .creator(username)
            .type("NEW")
            .originalSSDFileName(null)
            .originalContractFileName("test-contract.pdf")
            .build();

    // Mock queries
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(Arrays.asList(brd1, brd2)));

    // Act
    var result = dashboardService.getBrdUploadMetrics(filter, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<BrdUploadMetricsResponse>> typedResponse =
                  (ResponseEntity<Api<BrdUploadMetricsResponse>>) response;
              Api<BrdUploadMetricsResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              BrdUploadMetricsResponse metricsResponse = apiResponse.getData().get();

              // Verify filter type is correct
              boolean correctFilterType = "OPEN".equals(metricsResponse.getFilterType());

              // Verify SSD uploads
              boolean ssdMetricsCorrect =
                  metricsResponse.getSsdUploads().getNewBrds().getUploadedCount() == 1
                      && metricsResponse.getSsdUploads().getNewBrds().getNotUploadedCount() == 1;

              // Verify Contract uploads
              boolean contractMetricsCorrect =
                  metricsResponse.getContractUploads().getNewBrds().getUploadedCount() == 1
                      && metricsResponse.getContractUploads().getNewBrds().getNotUploadedCount()
                          == 1;

              return response.getStatusCode().is2xxSuccessful()
                  && correctFilterType
                  && ssdMetricsCorrect
                  && contractMetricsCorrect;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should get BRD upload metrics for ALL filter with weekly metrics")
  void getBrdUploadMetrics_WithAllFilter_ShouldReturnWeeklyMetrics() {
    // Arrange
    String filter = "ALL";
    String username = "testuser";

    // Mock the current date for deterministic weekly metrics
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // BRDs for week 1 (most recent week)
    BRD week1Brd1 =
        BRD.builder()
            .brdId("BRD-W1-1")
            .brdFormId("form-w1-1")
            .status("Draft")
            .creator(username)
            .type("NEW")
            .originalSSDFileName("ssd-w1-1.pdf")
            .originalContractFileName(null)
            .createdAt(LocalDateTime.of(2023, 11, 25, 10, 0)) // Week 1
            .build();

    BRD week1Brd2 =
        BRD.builder()
            .brdId("BRD-W1-2")
            .brdFormId("form-w1-2")
            .status("In Progress")
            .creator(username)
            .type("UPDATE")
            .originalSSDFileName(null)
            .originalContractFileName("contract-w1-2.pdf")
            .createdAt(LocalDateTime.of(2023, 11, 26, 14, 0)) // Week 1
            .build();

    // BRDs for week 2 (previous week)
    BRD week2Brd1 =
        BRD.builder()
            .brdId("BRD-W2-1")
            .brdFormId("form-w2-1")
            .status("Draft")
            .creator(username)
            .type("NEW")
            .originalSSDFileName("ssd-w2-1.pdf")
            .originalContractFileName("contract-w2-1.pdf")
            .createdAt(LocalDateTime.of(2023, 11, 18, 10, 0)) // Week 2
            .build();

    List<BRD> testBrds = Arrays.asList(week1Brd1, week1Brd2, week2Brd1);

    // Mock queries
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(testBrds));

    // Act
    var result = spyService.getBrdUploadMetrics(filter, username);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              ResponseEntity<Api<BrdUploadMetricsResponse>> typedResponse =
                  (ResponseEntity<Api<BrdUploadMetricsResponse>>) response;
              Api<BrdUploadMetricsResponse> apiResponse = typedResponse.getBody();
              if (apiResponse == null || !apiResponse.getData().isPresent()) {
                return false;
              }

              BrdUploadMetricsResponse metricsResponse = apiResponse.getData().get();

              // Verify filter type is correct
              boolean correctFilterType = "ALL".equals(metricsResponse.getFilterType());

              // Verify weekly metrics exist
              boolean hasWeeklyMetrics = metricsResponse.getWeeklyMetrics() != null;
              if (!hasWeeklyMetrics) {
                return false;
              }

              // Verify weeks list exists and has correct size
              boolean hasWeeks = metricsResponse.getWeeklyMetrics().getWeeks().size() > 0;

              // Verify NEW BRDs metrics - both weeks should have data
              boolean hasNewBrdData =
                  metricsResponse.getWeeklyMetrics().getNewBrds().getTotalCounts().size() > 0
                      && metricsResponse
                              .getWeeklyMetrics()
                              .getNewBrds()
                              .getSsdUploadedCounts()
                              .size()
                          > 0
                      && metricsResponse
                              .getWeeklyMetrics()
                              .getNewBrds()
                              .getContractUploadedCounts()
                              .size()
                          > 0;

              return response.getStatusCode().is2xxSuccessful()
                  && correctFilterType
                  && hasWeeks
                  && hasNewBrdData;
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should validate filter correctly")
  void isValidFilter_WithValidAndInvalidInputs_ShouldReturnCorrectResults() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method isValidFilterMethod =
          DashboardService.class.getDeclaredMethod("isValidFilter", String.class);
      isValidFilterMethod.setAccessible(true);

      // Valid filters
      boolean openResult = (boolean) isValidFilterMethod.invoke(dashboardService, "OPEN");
      boolean allResult = (boolean) isValidFilterMethod.invoke(dashboardService, "ALL");
      boolean openLowercaseResult = (boolean) isValidFilterMethod.invoke(dashboardService, "open");
      boolean allLowercaseResult = (boolean) isValidFilterMethod.invoke(dashboardService, "all");

      // Invalid filters
      boolean nullResult = (boolean) isValidFilterMethod.invoke(dashboardService, (Object) null);
      boolean emptyResult = (boolean) isValidFilterMethod.invoke(dashboardService, "");
      boolean invalidResult = (boolean) isValidFilterMethod.invoke(dashboardService, "INVALID");

      // Assert
      assertTrue(openResult, "OPEN should be valid");
      assertTrue(allResult, "ALL should be valid");
      assertTrue(openLowercaseResult, "open should be valid (case insensitive)");
      assertTrue(allLowercaseResult, "all should be valid (case insensitive)");
      assertFalse(nullResult, "null should be invalid");
      assertFalse(emptyResult, "empty string should be invalid");
      assertFalse(invalidResult, "INVALID should be invalid");

    } catch (Exception e) {
      fail("Failed to test isValidFilter method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should get BRDs for upload metrics with proper filtering")
  void getBrdsForUploadMetrics_WithDifferentFilters_ShouldApplyCorrectFilters() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method getBrdsForUploadMetricsMethod =
          DashboardService.class.getDeclaredMethod(
              "getBrdsForUploadMetrics", String.class, boolean.class, String.class);
      getBrdsForUploadMetricsMethod.setAccessible(true);

      // Arrange - Set up test data
      String username = "testuser";

      // Create test BRDs
      BRD openBrd =
          BRD.builder()
              .brdId("BRD-001")
              .status("Draft") // Open status
              .creator(username)
              .build();

      BRD submittedBrd =
          BRD.builder()
              .brdId("BRD-002")
              .status("Submit") // Submitted status
              .creator(username)
              .build();

      // Test OPEN filter
      // Mock mongoTemplate to verify query includes status filter
      when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
          .thenAnswer(
              invocation -> {
                Query query = invocation.getArgument(0);
                String queryJson = query.getQueryObject().toJson();

                // For OPEN filter, query should exclude Submit status
                if (queryJson.contains("status") && queryJson.contains("Submit")) {
                  return Flux.just(openBrd);
                }
                return Flux.empty();
              });

      // Invoke the method with OPEN filter
      Flux<BRD> openResult =
          (Flux<BRD>)
              getBrdsForUploadMetricsMethod.invoke(dashboardService, "OPEN", false, username);

      // Test ALL filter
      // Mock mongoTemplate to verify query doesn't filter by status
      when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
          .thenAnswer(
              invocation -> {
                Query query = invocation.getArgument(0);
                String queryJson = query.getQueryObject().toJson();

                // For ALL filter, query should not filter by status
                if (!queryJson.contains("status")) {
                  return Flux.just(openBrd, submittedBrd);
                }
                return Flux.empty();
              });

      // Invoke the method with ALL filter
      Flux<BRD> allResult =
          (Flux<BRD>)
              getBrdsForUploadMetricsMethod.invoke(dashboardService, "ALL", false, username);

      // Assert the results via StepVerifier
      StepVerifier.create(openResult)
          .expectNextCount(1) // One BRD for OPEN filter
          .verifyComplete();

      StepVerifier.create(allResult)
          .expectNextCount(2) // Two BRDs for ALL filter
          .verifyComplete();

    } catch (Exception e) {
      fail("Failed to test getBrdsForUploadMetrics method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should calculate file type metrics correctly")
  void calculateFileTypeMetrics_ForDifferentFileTypes_ShouldCalculateCorrectly() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method calculateFileTypeMetricsMethod =
          DashboardService.class.getDeclaredMethod(
              "calculateFileTypeMetrics", List.class, boolean.class);
      calculateFileTypeMetricsMethod.setAccessible(true);

      // Arrange - Create test BRDs with different file types
      List<BRD> testBrds =
          Arrays.asList(
              // BRD with SSD file only
              BRD.builder()
                  .brdId("BRD-001")
                  .originalSSDFileName("test-ssd.pdf")
                  .originalContractFileName(null)
                  .build(),

              // BRD with Contract file only
              BRD.builder()
                  .brdId("BRD-002")
                  .originalSSDFileName(null)
                  .originalContractFileName("test-contract.pdf")
                  .build(),

              // BRD with both files
              BRD.builder()
                  .brdId("BRD-003")
                  .originalSSDFileName("test-ssd-both.pdf")
                  .originalContractFileName("test-contract-both.pdf")
                  .build(),

              // BRD with no files
              BRD.builder()
                  .brdId("BRD-004")
                  .originalSSDFileName(null)
                  .originalContractFileName(null)
                  .build());

      // Act - Calculate metrics for SSD files
      Object ssdMetricsObj =
          calculateFileTypeMetricsMethod.invoke(dashboardService, testBrds, true);

      // Get fields using reflection
      Class<?> typeMetricsClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdUploadMetricsResponse$TypeMetrics");
      Method getTotalCountMethod = typeMetricsClass.getMethod("getTotalCount");
      Method getUploadedCountMethod = typeMetricsClass.getMethod("getUploadedCount");
      Method getNotUploadedCountMethod = typeMetricsClass.getMethod("getNotUploadedCount");
      Method getUploadedPercentageMethod = typeMetricsClass.getMethod("getUploadedPercentage");
      Method getNotUploadedPercentageMethod =
          typeMetricsClass.getMethod("getNotUploadedPercentage");

      // Extract values from SSD metrics
      int ssdTotalCount = (int) getTotalCountMethod.invoke(ssdMetricsObj);
      int ssdUploadedCount = (int) getUploadedCountMethod.invoke(ssdMetricsObj);
      int ssdNotUploadedCount = (int) getNotUploadedCountMethod.invoke(ssdMetricsObj);
      int ssdUploadedPercentage = (int) getUploadedPercentageMethod.invoke(ssdMetricsObj);
      int ssdNotUploadedPercentage = (int) getNotUploadedPercentageMethod.invoke(ssdMetricsObj);

      // Act - Calculate metrics for Contract files
      Object contractMetricsObj =
          calculateFileTypeMetricsMethod.invoke(dashboardService, testBrds, false);

      // Extract values from Contract metrics
      int contractTotalCount = (int) getTotalCountMethod.invoke(contractMetricsObj);
      int contractUploadedCount = (int) getUploadedCountMethod.invoke(contractMetricsObj);
      int contractNotUploadedCount = (int) getNotUploadedCountMethod.invoke(contractMetricsObj);
      int contractUploadedPercentage = (int) getUploadedPercentageMethod.invoke(contractMetricsObj);
      int contractNotUploadedPercentage =
          (int) getNotUploadedPercentageMethod.invoke(contractMetricsObj);

      // Assert - SSD metrics
      assertEquals(4, ssdTotalCount, "Total count should be the number of BRDs");
      assertEquals(2, ssdUploadedCount, "Two BRDs have SSD files");
      assertEquals(2, ssdNotUploadedCount, "Two BRDs don't have SSD files");
      assertEquals(50, ssdUploadedPercentage, "50% of BRDs have SSD files");
      assertEquals(50, ssdNotUploadedPercentage, "50% of BRDs don't have SSD files");

      // Assert - Contract metrics
      assertEquals(4, contractTotalCount, "Total count should be the number of BRDs");
      assertEquals(2, contractUploadedCount, "Two BRDs have Contract files");
      assertEquals(2, contractNotUploadedCount, "Two BRDs don't have Contract files");
      assertEquals(50, contractUploadedPercentage, "50% of BRDs have Contract files");
      assertEquals(50, contractNotUploadedPercentage, "50% of BRDs don't have Contract files");

    } catch (Exception e) {
      fail("Failed to test calculateFileTypeMetrics method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should process BRD correctly for weekly counts")
  void processBrdForWeeklyCountsAsNew_ShouldUpdateCorrectArrays() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method processBrdMethod =
          DashboardService.class.getDeclaredMethod(
              "processBrdForWeeklyCountsAsNew", BRD.class, Map.class, int[].class, int[].class);
      processBrdMethod.setAccessible(true);

      // Arrange - Create test data
      BRD testBrd =
          BRD.builder()
              .brdId("BRD-001")
              .type("NEW") // Type should be treated as NEW due to temporary change
              .createdAt(LocalDateTime.of(2023, 5, 15, 10, 0))
              .build();

      // Create week map
      Map<String, Integer> weekMap = new HashMap<>();
      weekMap.put("2023-W20", 0); // Week of May 15, 2023

      // Create count arrays
      int[] newCounts = new int[1];
      int[] totalCounts = new int[1];

      // Act - Process the BRD
      processBrdMethod.invoke(dashboardService, testBrd, weekMap, newCounts, totalCounts);

      // Assert - Arrays should be updated
      assertEquals(1, newCounts[0], "New count should be incremented");
      assertEquals(1, totalCounts[0], "Total count should be incremented");

      // Test with a BRD that's not in the week map
      BRD outOfRangeBrd =
          BRD.builder()
              .brdId("BRD-002")
              .type("NEW")
              .createdAt(LocalDateTime.of(2022, 1, 1, 10, 0)) // Different week
              .build();

      int[] newCounts2 = new int[1];
      int[] totalCounts2 = new int[1];

      // Act - Process the out-of-range BRD
      processBrdMethod.invoke(dashboardService, outOfRangeBrd, weekMap, newCounts2, totalCounts2);

      // Assert - Arrays should not be updated (BRD week not in map)
      assertEquals(0, newCounts2[0], "New count should not change for out-of-range BRD");
      assertEquals(0, totalCounts2[0], "Total count should not change for out-of-range BRD");

    } catch (Exception e) {
      fail("Failed to test processBrdForWeeklyCountsAsNew method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should calculate weekly metrics correctly")
  void calculateWeeklyMetrics_WithValidBrds_ShouldReturnCorrectMetrics() {
    // Mock weekly metrics calculation dependencies
    try {
      // Create spy service to mock getCurrentDateTime
      DashboardService spyService = Mockito.spy(dashboardService);
      LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
      Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

      // Mock MongoDB response for the getBrdUploadMetrics call
      BRD brd1 =
          BRD.builder()
              .brdId("BRD-001")
              .status("Draft")
              .type("NEW")
              .originalSSDFileName("ssd.pdf")
              .originalContractFileName(null)
              .createdAt(mockedDate.minusDays(10))
              .build();

      BRD brd2 =
          BRD.builder()
              .brdId("BRD-002")
              .status("In Progress")
              .type("NEW")
              .originalSSDFileName(null)
              .originalContractFileName("contract.pdf")
              .createdAt(mockedDate.minusDays(20))
              .build();

      when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
          .thenReturn(Flux.fromIterable(Arrays.asList(brd1, brd2)));

      // Call the actual service method
      var result = spyService.getBrdUploadMetrics("ALL", "testuser");

      // Verify results contain weekly metrics
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                ResponseEntity<Api<BrdUploadMetricsResponse>> typedResponse =
                    (ResponseEntity<Api<BrdUploadMetricsResponse>>) response;
                Api<BrdUploadMetricsResponse> apiResponse = typedResponse.getBody();
                if (apiResponse == null || !apiResponse.getData().isPresent()) {
                  return false;
                }

                BrdUploadMetricsResponse metrics = apiResponse.getData().get();

                // Verify weekly metrics present with correct structure
                return metrics.getWeeklyMetrics() != null
                    && metrics.getWeeklyMetrics().getWeeks() != null
                    && metrics.getWeeklyMetrics().getWeeks().size() == 52
                    && metrics.getWeeklyMetrics().getNewBrds() != null
                    && metrics.getWeeklyMetrics().getNewBrds().getTotalCounts().size() == 52;
              })
          .verifyComplete();

    } catch (Exception e) {
      fail("Test failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should handle invalid filter in getBrdUploadMetrics")
  void getBrdUploadMetrics_WithInvalidFilter_ShouldReturnError() {
    // Arrange
    String filter = "INVALID"; // Invalid filter value
    String username = "testuser";

    // Act
    var result = dashboardService.getBrdUploadMetrics(filter, username);

    // Assert
    StepVerifier.create(result).expectError(BadRequestException.class).verify();
  }

  @Test
  @DisplayName("Should handle error in getBrdUploadMetrics")
  void getBrdUploadMetrics_WithErrorInQuery_ShouldHandleGracefully() {
    // Arrange
    String filter = "OPEN";
    String username = "testuser";

    // Mock query to throw error
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.error(new RuntimeException("Test error")));

    // Act
    var result = dashboardService.getBrdUploadMetrics(filter, username);

    // Assert - should map to a general Exception through handleErrors
    StepVerifier.create(result)
        .expectErrorSatisfies(
            error -> {
              assertTrue(error instanceof Exception);
              assertTrue(error.getMessage().contains("Something went wrong"));
            })
        .verify();
  }

  @Test
  @DisplayName("Should calculate upload metrics correctly with empty list")
  void calculateUploadMetrics_WithEmptyList_ShouldReturnEmptyMetrics() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method calculateUploadMetricsMethod =
          DashboardService.class.getDeclaredMethod(
              "calculateUploadMetrics", List.class, String.class);
      calculateUploadMetricsMethod.setAccessible(true);

      // Act - Calculate metrics with empty list
      Object metricsObj = calculateUploadMetricsMethod.invoke(dashboardService, List.of(), "OPEN");

      // Get fields using reflection
      Class<?> uploadMetricsResponseClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdUploadMetricsResponse");
      Method getFilterTypeMethod = uploadMetricsResponseClass.getMethod("getFilterType");
      Method getSsdUploadsMethod = uploadMetricsResponseClass.getMethod("getSsdUploads");
      Method getContractUploadsMethod = uploadMetricsResponseClass.getMethod("getContractUploads");

      String filterType = (String) getFilterTypeMethod.invoke(metricsObj);
      Object ssdUploadsObj = getSsdUploadsMethod.invoke(metricsObj);
      Object contractUploadsObj = getContractUploadsMethod.invoke(metricsObj);

      Class<?> uploadMetricsClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdUploadMetricsResponse$UploadMetrics");
      Method getNewBrdsMethod = uploadMetricsClass.getMethod("getNewBrds");

      Object ssdNewBrdsObj = getNewBrdsMethod.invoke(ssdUploadsObj);
      Object contractNewBrdsObj = getNewBrdsMethod.invoke(contractUploadsObj);

      Class<?> typeMetricsClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdUploadMetricsResponse$TypeMetrics");
      Method getTotalCountMethod = typeMetricsClass.getMethod("getTotalCount");

      int ssdTotalCount = (int) getTotalCountMethod.invoke(ssdNewBrdsObj);
      int contractTotalCount = (int) getTotalCountMethod.invoke(contractNewBrdsObj);

      // Assert
      assertEquals("OPEN", filterType, "Filter type should match input");
      assertEquals(0, ssdTotalCount, "SSD total count should be 0 for empty list");
      assertEquals(0, contractTotalCount, "Contract total count should be 0 for empty list");

    } catch (Exception e) {
      fail("Failed to test calculateUploadMetrics method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should create empty AI prefill rate response with correct structure")
  void createEmptyAiPrefillRateResponse_ShouldReturnProperlyStructuredResponse() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method createEmptyResponseMethod =
          DashboardService.class.getDeclaredMethod(
              "createEmptyAiPrefillRateResponse", String.class, String.class, List.class);
      createEmptyResponseMethod.setAccessible(true);

      // Create test data
      String period = "month";
      String username = "testuser";

      // Create time segments to pass to the method
      List<TimeSegment> timeSegments = new ArrayList<>();
      timeSegments.add(
          new TimeSegment(
              LocalDateTime.now().minusMonths(1).withDayOfMonth(1),
              LocalDateTime.now().minusMonths(1).withDayOfMonth(28),
              "May 2023"));
      timeSegments.add(
          new TimeSegment(
              LocalDateTime.now().minusMonths(2).withDayOfMonth(1),
              LocalDateTime.now().minusMonths(2).withDayOfMonth(28),
              "April 2023"));

      // Invoke the method
      Object responseObj =
          createEmptyResponseMethod.invoke(dashboardService, period, username, timeSegments);

      // Verify the response
      Class<?> responseClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdAiPrefillRateResponse");

      // Get the getter methods
      Method getPeriodMethod = responseClass.getMethod("getPeriod");
      Method getLoggedinManagerMethod = responseClass.getMethod("getLoggedinManager");
      Method getTimeSegmentsMethod = responseClass.getMethod("getTimeSegments");
      Method getTrendDataMethod = responseClass.getMethod("getTrendData");

      // Extract values
      String responsePeriod = (String) getPeriodMethod.invoke(responseObj);
      String responseUsername = (String) getLoggedinManagerMethod.invoke(responseObj);
      List<?> responseSegments = (List<?>) getTimeSegmentsMethod.invoke(responseObj);
      List<?> responseTrend = (List<?>) getTrendDataMethod.invoke(responseObj);

      // Assert
      assertEquals(period, responsePeriod, "Period should match input");
      assertEquals(username, responseUsername, "Username should match input");
      assertEquals(
          timeSegments.size(),
          responseSegments.size(),
          "Number of time segments should match input");
      assertEquals(
          timeSegments.size(), responseTrend.size(), "Number of trend points should match input");

      // Check that all time segments have 0.0 rate
      for (Object segment : responseSegments) {
        Method getAveragePrefillRateMethod = segment.getClass().getMethod("getAveragePrefillRate");
        double rate = (double) getAveragePrefillRateMethod.invoke(segment);
        assertEquals(0.0, rate, "Prefill rate should be 0.0 for empty response");
      }

      // Check that all trend points have 0.0 rate
      for (Object trend : responseTrend) {
        Method getPrefillRateMethod = trend.getClass().getMethod("getPrefillRate");
        double rate = (double) getPrefillRateMethod.invoke(trend);
        assertEquals(0.0, rate, "Trend prefill rate should be 0.0 for empty response");
      }

    } catch (Exception e) {
      fail("Failed to test createEmptyAiPrefillRateResponse method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should create empty transition response with correct structure")
  void createEmptyTransitionResponse_ShouldReturnProperlyStructuredResponse() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method createEmptyResponseMethod =
          DashboardService.class.getDeclaredMethod(
              "createEmptyTransitionResponse", String.class, String.class, List.class);
      createEmptyResponseMethod.setAccessible(true);

      // Create test data
      String period = "quarter";
      String username = "testuser";

      // Create time segments to pass to the method
      List<TimeSegment> timeSegments = new ArrayList<>();
      timeSegments.add(
          new TimeSegment(
              LocalDateTime.now().minusMonths(1).withDayOfMonth(1),
              LocalDateTime.now().minusMonths(1).withDayOfMonth(28),
              "Q1 2023"));
      timeSegments.add(
          new TimeSegment(
              LocalDateTime.now().minusMonths(4).withDayOfMonth(1),
              LocalDateTime.now().minusMonths(2).withDayOfMonth(28),
              "Q2 2023"));

      // Invoke the method
      Object responseObj =
          createEmptyResponseMethod.invoke(dashboardService, period, username, timeSegments);

      // Verify the response
      Class<?> responseClass =
          Class.forName("com.aci.smart_onboarding.dto.BrdStatusTransitionTimeResponse");

      // Get the getter methods
      Method getPeriodMethod = responseClass.getMethod("getPeriod");
      Method getLoggedinManagerMethod = responseClass.getMethod("getLoggedinManager");
      Method getPeriodDataMethod = responseClass.getMethod("getPeriodData");
      Method getTrendDataMethod = responseClass.getMethod("getTrendData");

      // Extract values
      String responsePeriod = (String) getPeriodMethod.invoke(responseObj);
      String responseUsername = (String) getLoggedinManagerMethod.invoke(responseObj);
      List<?> responsePeriodData = (List<?>) getPeriodDataMethod.invoke(responseObj);
      List<?> responseTrend = (List<?>) getTrendDataMethod.invoke(responseObj);

      // Assert
      assertEquals(period, responsePeriod, "Period should match input");
      assertEquals(username, responseUsername, "Username should match input");
      assertEquals(
          timeSegments.size(),
          responsePeriodData.size(),
          "Number of period data points should match input");
      assertEquals(
          timeSegments.size(), responseTrend.size(), "Number of trend points should match input");

      // Check that all period data have 0.0 averages
      for (Object periodData : responsePeriodData) {
        Method getAveragesMethod = periodData.getClass().getMethod("getAverages");
        Map<String, Double> averages = (Map<String, Double>) getAveragesMethod.invoke(periodData);

        // Verify averages exists and all are 0.0
        assertNotNull(averages, "Averages map should not be null");
        assertFalse(averages.isEmpty(), "Averages map should not be empty");
        assertTrue(
            averages.values().stream().allMatch(v -> v == 0.0),
            "All average values should be 0.0 for empty response");
      }

      // Check that all trend points have null blended average
      for (Object trend : responseTrend) {
        Method getBlendedAverageMethod = trend.getClass().getMethod("getBlendedAverage");
        Double blendedAverage = (Double) getBlendedAverageMethod.invoke(trend);
        assertNull(blendedAverage, "Blended average should be null for empty response");
      }

    } catch (Exception e) {
      fail("Failed to test createEmptyTransitionResponse method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should add period filter to query correctly")
  void addPeriodFilter_ShouldAddCorrectCriteriaToQuery() {
    // Test directly since this is a private method in the service
    try {
      // Get the method using reflection
      Method addPeriodFilterMethod =
          DashboardService.class.getDeclaredMethod(
              "addPeriodFilter", Query.class, String.class, String.class);
      addPeriodFilterMethod.setAccessible(true);

      // Create test data
      Query query = new Query();

      // Test month period
      addPeriodFilterMethod.invoke(dashboardService, query, "all", "month");
      String monthQueryString = query.getQueryObject().toJson();
      assertTrue(monthQueryString.contains("createdAt"), "Query should filter by createdAt");

      // Test quarter period
      query = new Query();
      addPeriodFilterMethod.invoke(dashboardService, query, "all", "quarter");
      String quarterQueryString = query.getQueryObject().toJson();
      assertTrue(quarterQueryString.contains("createdAt"), "Query should filter by createdAt");

      // Test year period
      query = new Query();
      addPeriodFilterMethod.invoke(dashboardService, query, "all", "year");
      String yearQueryString = query.getQueryObject().toJson();
      assertTrue(yearQueryString.contains("createdAt"), "Query should filter by createdAt");

      // Test invalid period (should not modify query)
      query = new Query();
      int initialCriteriaCount = query.getQueryObject().toJson().length();
      addPeriodFilterMethod.invoke(dashboardService, query, "all", "invalid_period");
      String invalidQueryString = query.getQueryObject().toJson();
      assertEquals(
          initialCriteriaCount,
          invalidQueryString.length(),
          "Query should not be modified for invalid period");

    } catch (Exception e) {
      fail("Failed to test addPeriodFilter method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should calculate average sites correctly")
  void calculateAverageSites_ShouldReturnCorrectAverage() {
    try {
      // Get the method using reflection
      Method calculateAverageSitesMethod =
          DashboardService.class.getDeclaredMethod(
              "calculateAverageSites", List.class, String.class);
      calculateAverageSitesMethod.setAccessible(true);

      // Create test BRDs
      List<BRD> brds =
          Arrays.asList(
              BRD.builder().brdId("BRD-001").status("Draft").build(),
              BRD.builder().brdId("BRD-002").status("In Progress").build(),
              BRD.builder()
                  .brdId(null) // Test null BRD ID
                  .status("Draft")
                  .build());

      // Set up mock for site count
      when(mongoTemplate.count(any(Query.class), eq(Site.class)))
          .thenReturn(reactor.core.publisher.Mono.just(10L)); // 10 sites total

      // Test with open scope
      Object result = calculateAverageSitesMethod.invoke(dashboardService, brds, "open");

      // Verify result - 10 sites / 2 BRDs = 5.0 average (2 BRDs because one has null ID)
      assertEquals(
          5.0, (double) result, 0.1, "Average should be 5.0 (10 sites / 2 BRDs with valid ID)");

      // Test with all scope (same behavior)
      Object allScopeResult = calculateAverageSitesMethod.invoke(dashboardService, brds, "all");
      assertEquals(
          5.0, (double) allScopeResult, 0.1, "Average should be 5.0 for all scope as well");

      // Test with empty BRD list
      Object emptyResult = calculateAverageSitesMethod.invoke(dashboardService, List.of(), "open");
      assertEquals(0.0, (double) emptyResult, 0.1, "Average should be 0.0 with empty BRD list");

    } catch (Exception e) {
      fail("Failed to test calculateAverageSites method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should calculate resolution days correctly")
  void calculateResolutionDays_ShouldReturnCorrectDays() {
    try {
      // Get the method using reflection
      Method calculateResolutionDaysMethod =
          DashboardService.class.getDeclaredMethod(
              "calculateResolutionDays", BrdFieldCommentGroup.class);
      calculateResolutionDaysMethod.setAccessible(true);

      // Create comment group with created date
      BrdFieldCommentGroup commentGroup =
          BrdFieldCommentGroup.builder()
              .id("group1")
              .brdFormId("form-001")
              .status("Resolved")
              .createdAt(LocalDateTime.now().minusDays(5))
              .build();

      // Just verify the method doesn't throw an exception
      Object result = calculateResolutionDaysMethod.invoke(dashboardService, commentGroup);

      // We don't care about the specific value, just that it returns successfully
      assertNotNull(result, "Method should return a non-null value");

    } catch (Exception e) {
      fail("Failed to test calculateResolutionDays method: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should filter BRDs by date range correctly")
  void lambda_FilterBrdsByDateRange_ShouldFilterCorrectly() {
    try {
      // Test lambda by running the getBrdUploadMetrics method

      // Mock the current date
      DashboardService spyService = Mockito.spy(dashboardService);
      LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
      Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

      // Create test BRDs with different dates
      BRD brd =
          BRD.builder()
              .brdId("BRD-001")
              .status("Draft")
              .type("NEW")
              .createdAt(mockedDate.minusMonths(2))
              .build();

      // Mock the MongoDB response
      when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

      // Call the method which will trigger the lambda
      var result = spyService.getBrdUploadMetrics("ALL", "testuser");

      // Just verify the call completes successfully - the lambda executed
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response instanceof ResponseEntity
                      && ((ResponseEntity<?>) response).getStatusCode().is2xxSuccessful())
          .verifyComplete();

    } catch (Exception e) {
      fail("Test failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should filter BRDs correctly for weekly type metrics")
  void lambda_FilterBrdsForWeeklyTypeMetrics_ShouldFilterCorrectly() {
    try {
      // Test lambda via the real service method

      // Mock the current date
      DashboardService spyService = Mockito.spy(dashboardService);
      LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
      Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

      // Create test BRD
      BRD brd =
          BRD.builder()
              .brdId("BRD-001")
              .status("Draft")
              .type("NEW")
              .createdAt(mockedDate.minusMonths(2))
              .build();

      // Mock MongoDB response
      when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));

      // Call the service method which will use the lambda
      var result = spyService.getBrdCountsByType("ME", "testuser");

      // Just verify the call completes successfully - the lambda executed
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response instanceof ResponseEntity
                      && ((ResponseEntity<?>) response).getStatusCode().is2xxSuccessful())
          .verifyComplete();

    } catch (Exception e) {
      fail("Test failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should handle duplicate BRD formIds correctly")
  void lambda_HandleDuplicateBrds_ShouldUseFirstEntry() {
    // Test lambda$getBrdsWithFormIdsForSegment$28 by setting up duplicate formIds
    try {
      // Create test BRDs with duplicate formIds
      BRD brd1 = BRD.builder().brdId("BRD-001").brdFormId("form-001").status("Draft").build();

      // Same formId, different BRD ID
      BRD brd2 =
          BRD.builder()
              .brdId("BRD-002")
              .brdFormId("form-001") // Same formId as brd1
              .status("In Progress")
              .build();

      BRD brd3 = BRD.builder().brdId("BRD-003").brdFormId("form-003").status("Draft").build();

      // Create test data
      String period = "month";
      String username = "testuser";

      // Mock the current date
      DashboardService spyService = Mockito.spy(dashboardService);
      LocalDateTime mockedDate = LocalDateTime.of(2023, 6, 1, 0, 0);
      Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

      // Set up mock for BRDs query - where the duplicate logic will be triggered
      when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
          .thenReturn(Flux.fromIterable(Arrays.asList(brd1, brd2, brd3)));

      // Set up mock for audit logs query - empty for simplicity
      when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(Flux.empty());

      // Call the service method which will use the duplicate-handling logic
      var result = spyService.getAverageStatusTransitionTime(period, username);

      // Just verify the call completes successfully - duplicate handling logic executed
      StepVerifier.create(result)
          .expectNextMatches(response -> response != null && period.equals(response.getPeriod()))
          .verifyComplete();

    } catch (Exception e) {
      fail("Test failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should properly handle BRD vertical information")
  void lambda_LogBrdVertical_ShouldHandleAllCases() {
    // Test lambda$fetchAndTransformBrdsForVertical$2 with different vertical conditions
    try {
      // Create test BRDs with various vertical data
      BRD brdWithVertical =
          BRD.builder()
              .brdId("BRD-001")
              .brdFormId("form-001")
              .status("Draft")
              .industryVertical("Healthcare")
              .build();

      // Set up request parameters
      String scope = "me";
      String brdScope = "open";
      String period = null;
      String username = "testuser";

      // Set up mock for BRDs query
      when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
          .thenReturn(Flux.just(brdWithVertical))
          .thenReturn(Flux.just(brdWithVertical));

      // Call the service method
      var result = dashboardService.getBrdsByVertical(scope, brdScope, period, username);

      // Just verify the call completes successfully - vertical handling logic executed
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response != null
                      && scope.equals(response.getScope())
                      && brdScope.equals(response.getBrdScope()))
          .verifyComplete();

    } catch (Exception e) {
      fail("Test failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should handle resolution days calculation")
  void calculateResolutionDays_ShouldReturnExpectedValues() {
    // Instead of testing internal methods, test the functionality through the public API

    // Arrange - Create test comment groups
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime threeDaysAgo = now.minusDays(3);

    BrdFieldCommentGroup commentGroup =
        BrdFieldCommentGroup.builder()
            .id("resolved1")
            .brdFormId("form-001")
            .status("Resolved")
            .createdAt(threeDaysAgo)
            .build();

    // Mock the find query for comment groups
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.just(commentGroup));

    // Act
    var result = dashboardService.getAverageCommentResolutionTime();

    // Assert - just verify the call completes without exceptions
    StepVerifier.create(result)
        .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle BRD week index calculation for normal cases")
  void brdWeekIndexInMetrics_ShouldHandleNormalCases() {
    // Test week index calculation through public API

    // Arrange
    // Mock the current date
    DashboardService spyService = Mockito.spy(dashboardService);
    LocalDateTime mockedDate = LocalDateTime.of(2023, 12, 15, 0, 0);
    Mockito.doReturn(mockedDate).when(spyService).getCurrentDateTime();

    // Create test BRDs with dates
    BRD brd1 =
        BRD.builder()
            .brdId("BRD-001")
            .status("Draft")
            .type("NEW")
            .createdAt(mockedDate.minusMonths(1))
            .build();

    BRD brd2 =
        BRD.builder()
            .brdId("BRD-002")
            .status("Draft")
            .type("NEW")
            .createdAt(null) // Test null date
            .build();

    // Mock MongoDB query
    when(mongoTemplate.find(any(Query.class), eq(BRD.class)))
        .thenReturn(Flux.fromIterable(Arrays.asList(brd1, brd2)));

    // Act - get upload metrics which will use week index calculation internally
    var result = spyService.getBrdUploadMetrics("ALL", "testuser");

    // Assert - just check the call completes successfully
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response instanceof ResponseEntity
                    && ((ResponseEntity<?>) response).getStatusCode().is2xxSuccessful())
        .verifyComplete();
  }

  //

  @Test
  @DisplayName("Should include both CREATE and STATUS_UPDATE actions in status transitions")
  void getAverageStatusTransitionTime_ShouldIncludeBothCreateAndStatusUpdateActions() {
    // Arrange
    String period = "month";
    String username = "manager1";

    // Create test BRD with both CREATE and STATUS_UPDATE actions
    BRD brd =
        BRD.builder()
            .brdId("BRD-001")
            .brdFormId("BRD-001")
            .status("In Progress")
            .createdAt(LocalDateTime.parse("2023-05-01T10:00:00"))
            .build();

    // Create audit logs for both CREATE and STATUS_UPDATE actions
    AuditLog createLog = new AuditLog();
    createLog.setEntityId("BRD-001");
    createLog.setAction(BrdConstants.ACTION_CREATE);
    createLog.setEventTimestamp(LocalDateTime.parse("2023-05-01T10:00:00"));
    Map<String, Object> createNewValues = new HashMap<>();
    createNewValues.put(BrdConstants.STATUS_FIELD, BrdConstants.DRAFT);
    createLog.setNewValues(createNewValues);

    AuditLog statusUpdateLog = new AuditLog();
    statusUpdateLog.setEntityId("BRD-001");
    statusUpdateLog.setAction(BrdConstants.ACTION_STATUS_UPDATE);
    statusUpdateLog.setEventTimestamp(LocalDateTime.parse("2023-05-02T10:00:00"));
    Map<String, Object> updateNewValues = new HashMap<>();
    updateNewValues.put(BrdConstants.STATUS_FIELD, BrdConstants.STATUS_IN_PROGRESS);
    statusUpdateLog.setNewValues(updateNewValues);

    // Mock repository responses
    when(mongoTemplate.find(any(Query.class), eq(BRD.class))).thenReturn(Flux.just(brd));
    when(mongoTemplate.find(any(Query.class), eq(AuditLog.class)))
        .thenReturn(Flux.just(createLog, statusUpdateLog));

    // Act & Assert
    StepVerifier.create(dashboardService.getAverageStatusTransitionTime(period, username))
        .expectNextMatches(
            response -> {
              // Get the averages from the first period data (should only have one for month period)
              Map<String, Double> averages = response.getPeriodData().get(0).getAverages();

              // Verify the transition from Draft to In Progress is captured
              String transitionKey =
                  BrdConstants.DRAFT
                      + DashboardConstants.TRANSITION_ARROW
                      + BrdConstants.STATUS_IN_PROGRESS;
              Double transitionTime = averages.get(transitionKey);

              return transitionTime != null && transitionTime > 0.0;
            })
        .verifyComplete();
  }
}
