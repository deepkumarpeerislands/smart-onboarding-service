package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/** Interface for dashboard operations */
public interface IDashboardService {

  /**
   * Gets count of open BRDs grouped by status
   *
   * @param scope Scope of the query (me or team)
   * @param username Username of the logged-in user
   * @return Response containing counts of BRDs by status
   */
  Mono<BrdStatusCountResponse> getOpenBrdsByStatus(String scope, String username);

  /**
   * Gets count of BRDs grouped by industry vertical
   *
   * @param scope Scope of BRDs to include (me or team)
   * @param brdScope Type of BRDs to include (open or all)
   * @param period Time period for filtering (month, quarter, year) - only applicable when
   *     brdScope=all
   * @param username Username of the logged-in user
   * @return Response containing counts and percentages of BRDs by industry vertical
   */
  Mono<BrdVerticalCountResponse> getBrdsByVertical(
      String scope, String brdScope, String period, String username);

  /**
   * Gets statistics for Walletron and ACH form factors
   *
   * @param scope Scope of BRDs to include (me or team)
   * @param brdScope Type of BRDs to include (open or all)
   * @param period Time period for filtering (month, quarter, year) - only applicable when
   *     brdScope=all
   * @param username Username of the logged-in user
   * @return Response containing statistics for Walletron and ACH form flags
   */
  Mono<AdditionalFactorsResponse> getAdditionalFactors(
      String scope, String brdScope, String period, String username);

  /**
   * Gets snapshot metrics for BRDs
   *
   * @param scope Scope of BRDs to include (me or team)
   * @param username Username of the logged-in user
   * @return Response containing total, open, and Walletron-enabled BRD counts
   */
  Mono<BrdSnapshotMetricsResponse> getBrdSnapshotMetrics(String scope, String username);

  /**
   * Gets AI prefill accuracy metrics for BRDs
   *
   * @param scope Scope of BRDs to include (me or team)
   * @param username Username of the logged-in user
   * @return Response containing AI prefill accuracy metrics
   */
  Mono<BrdAiPrefillAccuracyResponse> getAiPrefillAccuracy(String scope, String username);

  /**
   * Calculates average time in days for BRDs to transition between statuses
   *
   * @param period Time period for filtering BRDs (month, quarter, year)
   * @param username Username of the logged-in manager
   * @return Response containing average time in days for each status transition
   */
  Mono<BrdStatusTransitionTimeResponse> getAverageStatusTransitionTime(
      String period, String username);

  /**
   * Gets AI prefill rate statistics over time
   *
   * @param period Time period for filtering BRDs (month, quarter, year)
   * @param username Username of the logged-in manager
   * @return Response containing AI prefill rate statistics over time
   */
  Mono<BrdAiPrefillRateResponse> getAiPrefillRateOverTime(String period, String username);

  /**
   * Retrieves count of unresolved comment groups for BRDs not in 'Submitted' state
   *
   * @param scope The scope of BRDs to include (me/team)
   * @param username The username of the current user (only used when scope is "me")
   * @return A response containing total count and per-BRD counts of unresolved comment groups
   */
  Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>> getUnresolvedCommentGroupsCount(
      String scope, String username);

  /**
   * Retrieves counts of BRDs by type (NEW, UPDATE, TRIAGE) for all time periods (current month,
   * current quarter, current year)
   *
   * @param scope The scope of BRDs to include (me/team)
   * @param username The username of the current user (only used when scope is "me")
   * @return A response containing counts for each time period with time series data and weekly
   *     metrics
   */
  Mono<ResponseEntity<Api<BrdTypeCountResponse>>> getBrdCountsByType(String scope, String username);

  Mono<ResponseEntity<Api<BrdUploadMetricsResponse>>> getBrdUploadMetrics(
      String filter, String username);

  /**
   * Calculates average time to resolve comments (time between creation and resolution) for all
   * three periods: month, quarter, and year
   *
   * @return Response containing average resolution times for all three periods
   */
  Mono<ResponseEntity<Api<CommentResolutionTimeResponse>>> getAverageCommentResolutionTime();

  /**
   * Get statistics about comment groups in open BRDs
   *
   * @return Mono containing comment group statistics wrapped in ResponseEntity and Api
   */
  Mono<ResponseEntity<Api<CommentGroupStatsResponse>>> getCommentGroupStats();

  Throwable handleErrors(Throwable ex);
}
