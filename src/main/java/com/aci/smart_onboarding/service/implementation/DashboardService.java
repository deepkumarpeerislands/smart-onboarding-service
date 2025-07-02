package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.dto.BrdStatusCountResponse.StatusCount;
import com.aci.smart_onboarding.dto.BrdVerticalCountResponse.VerticalCount;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.IllegalParameterException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.model.dashboard.FactorCounts;
import com.aci.smart_onboarding.model.dashboard.TimeSegment;
import com.aci.smart_onboarding.model.dashboard.TransitionResult;
import com.aci.smart_onboarding.service.IDashboardService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

  private final ReactiveMongoTemplate mongoTemplate;
  private static final List<String> ORDERED_BRD_STATUSES =
      Arrays.asList(
          "Draft",
          "In Progress",
          "Edit Complete",
          "Internal Review",
          "Reviewed",
          "Ready for Signoff",
          DashboardConstants.SIGNED_OFF_STATUS);

  @Override
  public Mono<BrdStatusCountResponse> getOpenBrdsByStatus(String scope, String username) {
    log.info("Getting open BRDs by status for scope: {} and user: {}", scope, username);

    Query query = buildQueryForScope(scope, username);
    log.debug("Query: {}", query.getQueryObject().toJson());

    return fetchAndTransformBrdsForStatus(query, scope, username);
  }

  private Mono<BrdStatusCountResponse> fetchAndTransformBrdsForStatus(
      Query query, String scope, String username) {
    return mongoTemplate
        .find(query, BRD.class)
        .doOnNext(this::logBrdDetails)
        .collectMultimap(BRD::getStatus)
        .doOnSuccess(map -> log.debug("Found BRDs by status: {}", map.keySet()))
        .map(this::transformToStatusCounts)
        .map(counts -> buildStatusResponse(scope, username, counts));
  }

  private void logBrdDetails(BRD brd) {
    log.debug(
        "Found BRD: id={}, status={}, creator={}",
        brd.getBrdId(),
        brd.getStatus(),
        brd.getCreator());
  }

  @Override
  public Mono<BrdVerticalCountResponse> getBrdsByVertical(
      String scope, String brdScope, String period, String username) {
    log.info(
        "Getting BRDs by vertical: scope={}, brdScope={}, period={}, username={}",
        scope,
        brdScope,
        period,
        username);

    try {
      validatePeriodParameter(brdScope, period);

      Query query = buildQueryForVerticals(scope, brdScope, period, username);
      log.info("Built query for verticals: {}", query);

      return fetchAndTransformBrdsForVertical(query, scope, brdScope, period, username);
    } catch (IllegalParameterException e) {
      log.error("Invalid parameters for getBrdsByVertical: {}", e.getMessage());
      return Mono.error(e);
    } catch (Exception e) {
      log.error("Error in getBrdsByVertical", e);
      return Mono.error(e);
    }
  }

  private void validatePeriodParameter(String brdScope, String period) {
    if (DashboardConstants.BRD_SCOPE_ALL.equals(brdScope) && period == null) {
      log.warn("Period parameter is required for brdScope=all but was null");
      throw new IllegalParameterException(DashboardConstants.PERIOD_REQUIRED_MESSAGE);
    }
  }

  private Mono<BrdVerticalCountResponse> fetchAndTransformBrdsForVertical(
      Query query, String scope, String brdScope, String period, String username) {

    log.info("Executing query to fetch BRDs by vertical: {}", query.getQueryObject().toJson());

    return mongoTemplate
        .find(query, BRD.class)
        .doOnNext(
            brd -> {
              if (DashboardConstants.QUARTER_PERIOD.equals(period)) {
                log.debug(
                    "Found BRD matching quarterly filter: formId={}, id={}, creator={}, createdAt={}, vertical={}",
                    brd.getBrdFormId(),
                    brd.getBrdId(),
                    brd.getCreator(),
                    brd.getCreatedAt(),
                    brd.getIndustryVertical());
              } else {
                log.debug(
                    "Found BRD: formId={}, id={}, creator={}, vertical={}",
                    brd.getBrdFormId(),
                    brd.getBrdId(),
                    brd.getCreator(),
                    brd.getIndustryVertical());
              }
            })
        .collectList()
        .map(
            brds -> {
              log.info("Found {} BRDs for query", brds.size());

              if (brds.isEmpty()) {
                log.warn(
                    "No BRDs found for query! Check filter criteria: scope={}, brdScope={}, period={}, username={}",
                    scope,
                    brdScope,
                    period,
                    username);
              }

              // Deduplicate BRDs by brdFormId to ensure accurate counting
              Map<String, BRD> uniqueBrds = new LinkedHashMap<>();
              brds.forEach(
                  brd -> {
                    String formId = brd.getBrdFormId();
                    if (formId != null && !formId.isEmpty()) {
                      uniqueBrds.putIfAbsent(formId, brd);
                    }
                  });

              List<BRD> dedupBrds = new ArrayList<>(uniqueBrds.values());
              int uniqueCount = dedupBrds.size();

              log.info("Deduplicated to {} unique BRDs by formId", uniqueCount);

              // Count BRDs by vertical
              Map<String, Integer> verticalCounts = new HashMap<>();
              for (BRD brd : dedupBrds) {
                String vertical = getVerticalName(brd);
                verticalCounts.put(vertical, verticalCounts.getOrDefault(vertical, 0) + 1);
              }

              log.info("Vertical counts: {}", verticalCounts);

              if (DashboardConstants.BRD_SCOPE_OPEN.equals(brdScope)) {
                // For open scope: Count all BRDs in open states for the industry
                log.info("Calculating percentages for OPEN scope");
                return calculateTotalOpenBrdsForPercentage(
                    verticalCounts, scope, brdScope, period, username);
              } else {
                // For all scope: Use the total count we already have
                log.info("Calculating percentages for ALL scope");
                return calculateAllScopeResponse(verticalCounts, scope, brdScope, period, username);
              }
            })
        .flatMap(response -> response);
  }

  private Mono<BrdVerticalCountResponse> calculateAllScopeResponse(
      Map<String, Integer> verticalCounts,
      String scope,
      String brdScope,
      String period,
      String username) {

    // Calculate total BRDs across all verticals
    int totalBrds = verticalCounts.values().stream().mapToInt(Integer::intValue).sum();
    log.info("Total BRDs across all verticals: {}", totalBrds);

    // Create vertical count objects with percentages based on total BRDs
    List<VerticalCount> verticalCountList =
        verticalCounts.entrySet().stream()
            .map(
                entry -> {
                  String vertical = entry.getKey();
                  int count = entry.getValue();

                  double percentage = calculatePercentage(count, totalBrds);

                  log.info(
                      "ALL scope - Calculating percentage for {}: {}/{} = {}%",
                      vertical, count, totalBrds, percentage);

                  return new VerticalCount(vertical, count, percentage);
                })
            .sorted(Comparator.comparing(VerticalCount::getBrdCount).reversed())
            .toList();

    log.info("Final vertical count list (ALL scope): {}", verticalCountList);

    return Mono.just(buildVerticalResponse(verticalCountList, scope, brdScope, period, username));
  }

  private Mono<BrdVerticalCountResponse> calculateTotalOpenBrdsForPercentage(
      Map<String, Integer> verticalCounts,
      String scope,
      String brdScope,
      String period,
      String username) {

    // Calculate total BRDs across all verticals
    int totalBrds = verticalCounts.values().stream().mapToInt(Integer::intValue).sum();
    log.info("Total BRDs across all verticals: {}", totalBrds);

    // Create vertical count objects with percentages based on total BRDs
    List<VerticalCount> verticalCountList =
        verticalCounts.entrySet().stream()
            .map(
                entry -> {
                  String vertical = entry.getKey();
                  int count = entry.getValue();

                  double percentage = calculatePercentage(count, totalBrds);

                  log.info(
                      "OPEN scope - Calculating percentage for {}: {}/{} = {}%",
                      vertical, count, totalBrds, percentage);

                  return new VerticalCount(vertical, count, percentage);
                })
            .sorted(Comparator.comparing(VerticalCount::getBrdCount).reversed())
            .toList();

    log.info("Final vertical count list (OPEN scope): {}", verticalCountList);

    return Mono.just(buildVerticalResponse(verticalCountList, scope, brdScope, period, username));
  }

  private double calculatePercentage(int count, int total) {
    if (total == 0) return 0.0;
    // Round to whole number percentages
    return Math.round((count * 100.0) / total);
  }

  private BrdVerticalCountResponse buildVerticalResponse(
      List<VerticalCount> verticalCounts,
      String scope,
      String brdScope,
      String period,
      String loggedinPm) {
    return BrdVerticalCountResponse.builder()
        .scope(scope)
        .brdScope(brdScope)
        .period(period)
        .loggedinPm(loggedinPm)
        .verticalCounts(verticalCounts)
        .build();
  }

  private String getVerticalName(BRD brd) {
    // Extract the vertical value, handling null case safely
    String vertical = brd.getIndustryVertical();

    // Log detailed information about the BRD for debugging
    log.info(
        "Processing vertical for BRD - formId={}, id={}, creator={}, vertical='{}'",
        brd.getBrdFormId(),
        brd.getBrdId(),
        brd.getCreator(),
        vertical);

    // Handle ALL cases of missing/invalid vertical values consistently:
    // - null
    // - empty string
    // - whitespace only
    // - literal "null" string
    if (vertical == null || vertical.trim().isEmpty() || "null".equalsIgnoreCase(vertical.trim())) {
      log.warn(
          "BRD has missing/invalid vertical - formId={}, id={}, creator={}, raw_value='{}', categorizing as 'Other'",
          brd.getBrdFormId(),
          brd.getBrdId(),
          brd.getCreator(),
          vertical);
      return DashboardConstants.OTHER_VERTICAL;
    }

    // Just normalize by trimming whitespace
    String normalizedVertical = vertical.trim();

    log.info(
        "Using normalized vertical - formId={}, id={}, creator={}, vertical='{}'",
        brd.getBrdFormId(),
        brd.getBrdId(),
        brd.getCreator(),
        normalizedVertical);
    return normalizedVertical;
  }

  private Query buildQueryForVerticals(
      String scope, String brdScope, String period, String username) {
    log.info(
        "Building query for verticals: scope={}, brdScope={}, period={}, username={}",
        scope,
        brdScope,
        period,
        username);

    Query query = new Query();

    // Ensure BRDs have a form ID
    query.addCriteria(Criteria.where(DashboardConstants.BRD_FORM_ID).exists(true));

    // Add scope filter (me or team)
    addScopeFilter(query, scope, username);

    // Add BRD scope filter
    if (DashboardConstants.BRD_SCOPE_OPEN.equals(brdScope)) {
      // For open BRDs, exclude submitted status
      log.info("Adding filter for open BRDs: status != {}", DashboardConstants.SUBMITTED_STATUS);
      query.addCriteria(
          Criteria.where(DashboardConstants.BRD_STATUS).ne(DashboardConstants.SUBMITTED_STATUS));
    }

    // Always apply period filter if period is specified
    if (period != null && !period.isEmpty()) {
      addPeriodFilter(query, brdScope, period);
    }

    log.info("Final query for verticals: {}", query.getQueryObject().toJson());
    return query;
  }

  /**
   * Adds scope-based filtering to the query based on the scope parameter and username. - For "me"
   * scope: filters by creator matching the username - For "team" scope: no additional filtering
   * (returns all BRDs)
   */
  private void addScopeFilter(Query query, String scope, String username) {
    if (DashboardConstants.SCOPE_ME.equals(scope)) {
      log.debug("Applying 'me' scope filter: creator = {}", username);
      query.addCriteria(Criteria.where(DashboardConstants.CREATOR).is(username));
    } else {
      // For "team" scope, no creator filter is applied - all BRDs are returned
      log.debug("Using 'team' scope - no creator filter applied");
    }
  }

  /**
   * Adds a time period filter to the query based on the brdScope and period. Only includes
   * completed periods (previous month, previous quarters, etc.)
   */
  private void addPeriodFilter(Query query, String brdScope, String period) {
    log.info("Adding period filter: brdScope={}, period={}", brdScope, period);

    if (period != null && !period.isEmpty()) {
      LocalDateTime now = getCurrentDateTime();
      LocalDateTime startDate;
      LocalDateTime endDate;

      // Calculate start date based on period
      switch (period.toLowerCase()) {
        case DashboardConstants.MONTH_PERIOD:
          // For month: get the first day of the previous month
          YearMonth lastMonth = YearMonth.from(now).minusMonths(1);
          startDate = LocalDateTime.of(lastMonth.getYear(), lastMonth.getMonth(), 1, 0, 0, 0);
          // End date is the last day of the previous month
          endDate = startDate.plusMonths(1).minusNanos(1);
          log.info("Month filter: from {} to {}", startDate, endDate);
          break;
        case DashboardConstants.QUARTER_PERIOD:
          // For quarter: start from 3 months ago, using only completed months
          startDate = now.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
          // End date is the end of previous month
          endDate = YearMonth.from(now).minusMonths(1).atEndOfMonth().atTime(23, 59, 59);
          log.info("Quarter filter: from {} to {}", startDate, endDate);
          break;
        case DashboardConstants.YEAR_PERIOD:
          // For year: start from 12 months ago (last 4 completed quarters)
          startDate = now.minusMonths(12).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
          // End date is the end of previous month
          endDate = YearMonth.from(now).minusMonths(1).atEndOfMonth().atTime(23, 59, 59);
          log.info("Year filter: from {} to {}", startDate, endDate);
          break;
        default:
          log.warn("Invalid period value: {}", period);
          return;
      }

      // Convert LocalDateTime to date type appropriate for MongoDB
      Date startDateAsDate = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
      Date endDateAsDate = Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant());

      log.info("Filtering data between: {} and {}", startDateAsDate, endDateAsDate);

      // Directly use "createdAt" as the date field for BRD queries
      // This is simpler and more explicit than determining the field dynamically
      String dateField = DashboardConstants.CREATED_AT;

      // Add date range criteria (greater than or equal to start date, less than or equal to end
      // date)
      query.addCriteria(Criteria.where(dateField).gte(startDateAsDate).lte(endDateAsDate));
      log.debug("After adding period filter, query is: {}", query.getQueryObject().toJson());
    } else {
      log.info("No period filter applied because period is null or empty");
    }
  }

  private Query buildQueryForScope(String scope, String username) {
    Query query = new Query();

    query.addCriteria(
        Criteria.where(DashboardConstants.BRD_STATUS).in(DashboardConstants.OPEN_BRD_STATUSES));

    if (DashboardConstants.SCOPE_ME.equals(scope)) {
      query.addCriteria(Criteria.where(DashboardConstants.CREATOR).is(username));
    }

    return query;
  }

  private Map<String, Integer> transformToStatusCounts(Map<String, Collection<BRD>> brdsByStatus) {
    return brdsByStatus.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
  }

  private BrdStatusCountResponse buildStatusResponse(
      String scope, String username, Map<String, Integer> statusCounts) {
    List<StatusCount> brdStatusCounts =
        DashboardConstants.OPEN_BRD_STATUSES.stream()
            .map(status -> new StatusCount(status, statusCounts.getOrDefault(status, 0)))
            .toList();

    return BrdStatusCountResponse.builder()
        .scope(scope)
        .loggedinPm(username)
        .brdStatusCounts(brdStatusCounts)
        .build();
  }

  /**
   * Gets statistics for Walletron and ACH form factors.
   *
   * @param scope Scope of BRDs to include (me or team)
   * @param brdScope Type of BRDs to include (open or all)
   * @param period Time period for filtering (month, quarter, year) - only applicable when
   *     brdScope=all
   * @param username Username of the logged-in user
   * @return Response containing statistics for Walletron and ACH form flags
   */
  @Override
  public Mono<AdditionalFactorsResponse> getAdditionalFactors(
      String scope, String brdScope, String period, String username) {
    log.debug(
        "Entering method: getAdditionalFactors with parameters: scope={}, brdScope={}, period={}, username={}",
        scope,
        brdScope,
        period,
        username);

    // Validate input parameters
    if (DashboardConstants.BRD_SCOPE_ALL.equals(brdScope) && (period == null || period.isEmpty())) {
      return Mono.error(new IllegalParameterException(DashboardConstants.PERIOD_REQUIRED_MESSAGE));
    }

    // Build the query with all necessary filters
    Query query = buildQueryForFactorStats(scope, brdScope, period, username);

    return fetchBrdsAndCalculateFactorStats(query, scope, brdScope, period, username);
  }

  /** Fetches BRDs based on query and calculates factor statistics. */
  private Mono<AdditionalFactorsResponse> fetchBrdsAndCalculateFactorStats(
      Query query, String scope, String brdScope, String period, String username) {

    log.info(
        "Executing query to fetch BRDs for additional factor statistics: {}",
        query.getQueryObject().toJson());

    return mongoTemplate
        .find(query, BRD.class)
        .collectList()
        .map(
            brds -> {
              log.info("Found {} BRDs for additional factor statistics query", brds.size());

              if (brds.isEmpty()) {
                return createEmptyFactorsResponse(scope, brdScope, period, username);
              }

              // Get unique BRDs by formId
              List<BRD> uniqueBrds = deduplicateBrdsByFormId(brds);

              // Calculate factor statistics
              return calculateFactorStatistics(uniqueBrds, scope, brdScope, period, username);
            });
  }

  /** Creates an empty response when no BRDs are found. */
  private AdditionalFactorsResponse createEmptyFactorsResponse(
      String scope, String brdScope, String period, String username) {
    AdditionalFactorsResponse.FactorStats emptyStats =
        AdditionalFactorsResponse.FactorStats.builder()
            .yesCount(0)
            .noCount(0)
            .yesPercentage(0.0)
            .noPercentage(0.0)
            .build();

    return AdditionalFactorsResponse.builder()
        .scope(scope)
        .brdScope(brdScope)
        .period(period)
        .loggedinPm(username)
        .walletron(emptyStats)
        .achForm(emptyStats)
        .averageSites(0.0)
        .build();
  }

  /** Deduplicates BRDs by their form ID to ensure accurate counting. */
  private List<BRD> deduplicateBrdsByFormId(List<BRD> brds) {
    Map<String, BRD> uniqueBrds = new LinkedHashMap<>();
    brds.forEach(
        brd -> {
          String formId = brd.getBrdFormId();
          if (formId != null && !formId.isEmpty()) {
            uniqueBrds.putIfAbsent(formId, brd);
          }
        });

    List<BRD> dedupBrds = new ArrayList<>(uniqueBrds.values());
    log.info("Deduplicated to {} unique BRDs by formId", dedupBrds.size());

    return dedupBrds;
  }

  /** Calculates statistics for Walletron and ACH factors from the list of BRDs. */
  private AdditionalFactorsResponse calculateFactorStatistics(
      List<BRD> brds, String scope, String brdScope, String period, String username) {

    int totalCount = brds.size();

    // Calculate statistics for each factor
    FactorCounts walletronCounts = countWalletronFactor(brds);
    FactorCounts achCounts = countAchFactor(brds);

    // Calculate average sites per BRD - pass the brdScope parameter
    double averageSites = calculateAverageSites(brds, brdScope);

    // Calculate percentages
    AdditionalFactorsResponse.FactorStats walletronStats =
        createFactorStats(walletronCounts.getYesCount(), walletronCounts.getNoCount(), totalCount);

    AdditionalFactorsResponse.FactorStats achStats =
        createFactorStats(achCounts.getYesCount(), achCounts.getNoCount(), totalCount);

    // Build and return response
    return AdditionalFactorsResponse.builder()
        .scope(scope)
        .brdScope(brdScope)
        .period(period)
        .loggedinPm(username)
        .walletron(walletronStats)
        .achForm(achStats)
        .averageSites(averageSites)
        .build();
  }

  /**
   * Calculates the average number of sites per BRD. This method counts the actual sites for each
   * BRD and calculates the true average.
   *
   * @param brds List of unique BRDs
   * @param brdScope The scope of BRDs (open or all)
   * @return Average number of sites, rounded to 1 decimal place
   */
  private double calculateAverageSites(List<BRD> brds, String brdScope) {
    if (brds.isEmpty()) {
      return 0.0;
    }

    // Get all BRD IDs
    List<String> brdIds =
        brds.stream().map(BRD::getBrdId).filter(id -> id != null && !id.isEmpty()).toList();

    if (brdIds.isEmpty()) {
      log.warn("No valid BRD IDs found to count sites");
      return 0.0;
    }

    // Debug - list all BRD IDs for which we'll count sites
    log.info("BRD IDs for site counting: {}", brdIds);
    log.info("Counting sites for {} BRDs with scope: {}", brdIds.size(), brdScope);

    try {
      // Count sites for each BRD individually for detailed debugging
      for (String brdId : brdIds) {
        Query singleBrdQuery = new Query(Criteria.where(DashboardConstants.BRD_ID).is(brdId));
        long brdSiteCount = mongoTemplate.count(singleBrdQuery, Site.class).block();
        log.debug("BRD ID: {} has {} sites", brdId, brdSiteCount);
      }

      // Build query to count sites per BRD, ensuring we only count sites for the given BRD IDs
      Query countQuery = new Query(Criteria.where(DashboardConstants.BRD_ID).in(brdIds));

      // Use the mongoTemplate to count sites
      long totalSiteCount = mongoTemplate.count(countQuery, Site.class).block();

      if (totalSiteCount <= 0) {
        log.info("No sites found for the specified BRDs");
        return 0.0;
      }

      // Calculate average
      double average = (double) totalSiteCount / brdIds.size();

      // Round to 1 decimal place
      double roundedAverage = Math.round(average * 10.0) / 10.0;

      log.info(
          "Calculated ACTUAL average sites per BRD: {} sites across {} BRDs = {} (rounded to {})",
          totalSiteCount,
          brdIds.size(),
          average,
          roundedAverage);

      return roundedAverage;
    } catch (Exception e) {
      log.error("Error calculating average sites per BRD: {}", e.getMessage(), e);
      // Return 0 in case of error
      return 0.0;
    }
  }

  /** Counts Walletron factor values. */
  private FactorCounts countWalletronFactor(List<BRD> brds) {
    int yesCount = 0;
    int noCount = 0;

    for (BRD brd : brds) {
      if (brd.isWallentronIncluded()) {
        yesCount++;
      } else {
        noCount++;
      }
    }

    return new FactorCounts(yesCount, noCount);
  }

  /** Counts ACH factor values. */
  private FactorCounts countAchFactor(List<BRD> brds) {
    int yesCount = 0;
    int noCount = 0;

    for (BRD brd : brds) {
      if (brd.isAchEncrypted()) {
        yesCount++;
      } else {
        noCount++;
      }
    }

    return new FactorCounts(yesCount, noCount);
  }

  /** Creates a FactorStats object with the given counts and calculated percentages. */
  private AdditionalFactorsResponse.FactorStats createFactorStats(
      int yesCount, int noCount, int totalCount) {
    double yesPercentage = calculatePercentage(yesCount, totalCount);
    double noPercentage = calculatePercentage(noCount, totalCount);

    // Log calculations
    log.info(
        "Factor stats: Yes: {}/{} = {}%, No: {}/{} = {}%",
        yesCount, totalCount, yesPercentage, noCount, totalCount, noPercentage);

    return AdditionalFactorsResponse.FactorStats.builder()
        .yesCount(yesCount)
        .noCount(noCount)
        .yesPercentage(yesPercentage)
        .noPercentage(noPercentage)
        .build();
  }

  /**
   * Builds a query for fetching BRDs for additional factor statistics. This query applies filters
   * for scope, BRD scope, and period.
   */
  private Query buildQueryForFactorStats(
      String scope, String brdScope, String period, String username) {
    log.info(
        "Building query for additional factors: scope={}, brdScope={}, period={}, username={}",
        scope,
        brdScope,
        period,
        username);

    Query query = new Query();

    // Ensure BRDs have a form ID
    query.addCriteria(Criteria.where(DashboardConstants.BRD_FORM_ID).exists(true));

    // Add scope filter (me or team)
    addScopeFilter(query, scope, username);

    // Add BRD scope filter
    if (DashboardConstants.BRD_SCOPE_OPEN.equals(brdScope)) {
      // For open BRDs, exclude submitted status
      log.info("Adding filter for open BRDs: status != {}", DashboardConstants.SUBMITTED_STATUS);
      query.addCriteria(
          Criteria.where(DashboardConstants.BRD_STATUS).ne(DashboardConstants.SUBMITTED_STATUS));
    } else {
      log.info("Not filtering by status since brdScope={}", brdScope);
    }

    // Always apply period filter if period is specified
    if (period != null && !period.isEmpty()) {
      addPeriodFilter(query, brdScope, period);
    } else {
      log.info("No period filter applied because period is null or empty");
    }

    log.info("Final query for additional factors: {}", query.getQueryObject().toJson());
    return query;
  }

  @Override
  public Mono<BrdSnapshotMetricsResponse> getBrdSnapshotMetrics(String scope, String username) {
    log.info("Getting BRD snapshot metrics for scope: {} and user: {}", scope, username);

    // Create a query for all BRDs
    Query totalBrdsQuery = buildScopedQuery(scope, username);

    return mongoTemplate
        .find(totalBrdsQuery, BRD.class)
        .collectList()
        .map(
            brds -> {
              log.info("Found {} BRDs for snapshot metrics", brds.size());

              int totalBrds = brds.size();
              int openBrds = countOpenBrds(brds);
              int walletronEnabledBrds = countWalletronEnabledBrds(brds);

              log.info(
                  "Snapshot metrics - totalBrds: {}, openBrds: {}, walletronEnabledBrds: {}",
                  totalBrds,
                  openBrds,
                  walletronEnabledBrds);

              return createSnapshotMetricsResponse(
                  scope, totalBrds, openBrds, walletronEnabledBrds);
            });
  }

  /** Creates a query that filters by scope (me or team). */
  private Query buildScopedQuery(String scope, String username) {
    Query query = new Query();

    if (DashboardConstants.SCOPE_ME.equals(scope)) {
      query.addCriteria(Criteria.where(DashboardConstants.CREATOR).is(username));
    }

    return query;
  }

  /** Counts the number of open BRDs (non-submitted status). */
  private int countOpenBrds(List<BRD> brds) {
    return (int)
        brds.stream()
            .filter(brd -> !DashboardConstants.SUBMITTED_STATUS.equals(brd.getStatus()))
            .count();
  }

  /** Counts the number of BRDs with Walletron enabled. */
  private int countWalletronEnabledBrds(List<BRD> brds) {
    return (int) brds.stream().filter(BRD::isWallentronIncluded).count();
  }

  /** Creates the snapshot metrics response object. */
  private BrdSnapshotMetricsResponse createSnapshotMetricsResponse(
      String scope, int totalBrds, int openBrds, int walletronEnabledBrds) {

    BrdSnapshotMetricsResponse.SnapshotMetrics metrics =
        BrdSnapshotMetricsResponse.SnapshotMetrics.builder()
            .totalBrds(totalBrds)
            .openBrds(openBrds)
            .walletronEnabledBrds(walletronEnabledBrds)
            .build();

    return BrdSnapshotMetricsResponse.builder().scope(scope).snapshotMetrics(metrics).build();
  }

  @Override
  public Mono<BrdAiPrefillAccuracyResponse> getAiPrefillAccuracy(String scope, String username) {
    log.info("Getting AI prefill accuracy metrics for scope: {} and user: {}", scope, username);

    Query query = buildScopedQuery(scope, username);

    return mongoTemplate
        .find(query, BRD.class)
        .collectList()
        .map(
            brds -> {
              log.info("Found {} BRDs for AI prefill accuracy analysis", brds.size());

              if (brds.isEmpty()) {
                log.info("No BRDs found for analysis");
                return createEmptyAiPrefillResponse();
              }

              // Calculate AI prefill accuracy as average rate across all BRDs
              double accuracy = calculateTotalAiPrefillRate(brds);

              log.info("AI prefill metrics - Accuracy: {}", accuracy);

              return BrdAiPrefillAccuracyResponse.builder().aiPrefillAccuracy(accuracy).build();
            });
  }

  private double calculateTotalAiPrefillRate(List<BRD> brds) {
    if (brds.isEmpty()) {
      return 0.0;
    }

    double totalPrefillRate =
        brds.stream()
            .filter(brd -> brd.getAiPrefillRate() != null)
            .mapToDouble(BRD::getAiPrefillRate)
            .sum();

    int totalBrds = brds.size();

    // Calculate rate per BRD
    double ratePerBrd = totalPrefillRate / totalBrds;

    // Round to 2 decimal places
    double roundedRate = Math.round(ratePerBrd * 100.0) / 100.0;

    log.info(
        "Total AI prefill rate: {} across {} BRDs = {} (rounded to {})",
        totalPrefillRate,
        totalBrds,
        ratePerBrd,
        roundedRate);

    return roundedRate;
  }

  private BrdAiPrefillAccuracyResponse createEmptyAiPrefillResponse() {
    return BrdAiPrefillAccuracyResponse.builder().aiPrefillAccuracy(0.0).build();
  }

  @Override
  public Mono<BrdStatusTransitionTimeResponse> getAverageStatusTransitionTime(
      String period, String username) {
    // If period is null or empty, use the default value (quarter)
    String validatedPeriod = period;
    if (validatedPeriod == null || validatedPeriod.isEmpty()) {
      validatedPeriod = DashboardConstants.QUARTER_PERIOD;
      log.info("No period specified in service layer, using default: {}", validatedPeriod);
    }

    // Validate period value
    if (!Arrays.asList(
            DashboardConstants.MONTH_PERIOD,
            DashboardConstants.QUARTER_PERIOD,
            DashboardConstants.YEAR_PERIOD)
        .contains(validatedPeriod.toLowerCase())) {
      return Mono.error(
          new IllegalParameterException(
              "Invalid period parameter. Valid values are: month, quarter, year"));
    }

    return searchForTransitions(validatedPeriod, username);
  }

  /** Search for transitions based on the given period */
  private Mono<BrdStatusTransitionTimeResponse> searchForTransitions(
      String period, String username) {
    // Get the appropriate time segments based on the period
    List<TimeSegment> timeSegments = getTimeSegmentsForPeriod(period);

    // Log the segments order
    log.info("Using {} time segments for period {}", timeSegments.size(), period);
    for (int i = 0; i < timeSegments.size(); i++) {
      TimeSegment segment = timeSegments.get(i);
      log.info(
          "Processing segment {}: {} (from {} to {})",
          i + 1,
          segment.getLabel(),
          segment.getStartDate().toLocalDate(),
          segment.getEndDate().toLocalDate());
    }

    // Process each time segment and collect results, maintaining the original order
    return Flux.fromIterable(timeSegments)
        .index() // Add index to keep track of original order
        .flatMap(
            tuple -> {
              TimeSegment segment = tuple.getT2();
              Long index = tuple.getT1();

              return getBrdsAndCalculateTransitionsForSegment(segment)
                  .map(periodData -> new AbstractMap.SimpleEntry<>(index, periodData));
            })
        .collectList()
        .map(
            indexedResults -> {
              if (indexedResults.isEmpty()) {
                return createEmptyTransitionResponse(period, username, timeSegments);
              }

              // Sort by original index to restore original order
              indexedResults.sort(Comparator.comparing(Map.Entry::getKey));

              // Extract just the period data
              List<BrdStatusTransitionTimeResponse.PeriodData> periodDataList =
                  indexedResults.stream().map(Map.Entry::getValue).toList();

              // Calculate the blended averages for trend line
              List<BrdStatusTransitionTimeResponse.TrendPoint> trendPoints =
                  calculateBlendedAverages(periodDataList);

              log.info("Generated period data for {} time segments", periodDataList.size());
              periodDataList.forEach(
                  data ->
                      log.info(
                          "Period: {} with {} transitions",
                          data.getLabel(),
                          data.getAverages().size()));

              return BrdStatusTransitionTimeResponse.builder()
                  .period(period)
                  .loggedinManager(username)
                  .periodData(periodDataList)
                  .trendData(trendPoints)
                  .build();
            });
  }

  /**
   * Calculates blended averages for trend line by averaging all transition times for each period
   */
  private List<BrdStatusTransitionTimeResponse.TrendPoint> calculateBlendedAverages(
      List<BrdStatusTransitionTimeResponse.PeriodData> periodDataList) {

    return periodDataList.stream()
        .map(
            periodData -> {
              Map<String, Double> transitionAverages = periodData.getAverages();

              // Calculate blended average by averaging all transition times
              double blendedAverage = 0.0;
              int nonZeroCount = 0;

              for (Double average : transitionAverages.values()) {
                if (average > 0) {
                  blendedAverage += average;
                  nonZeroCount++;
                }
              }

              // Calculate final average (handle case of no data)
              if (nonZeroCount > 0) {
                blendedAverage = blendedAverage / nonZeroCount;
                // Round to 1 decimal place
                blendedAverage = Math.round(blendedAverage * 10.0) / 10.0;
              }

              return BrdStatusTransitionTimeResponse.TrendPoint.builder()
                  .label(periodData.getLabel())
                  .blendedAverage(blendedAverage > 0 ? blendedAverage : null)
                  .build();
            })
        .toList();
  }

  /** Creates time segments based on selected period */
  private List<TimeSegment> getTimeSegmentsForPeriod(String period) {
    // Default behavior for other endpoints - not AI prefill rate
    return getTimeSegmentsForPeriod(period, false);
  }

  /** Creates time segments for month period (last completed month only) */
  private List<TimeSegment> createMonthPeriodSegments() {
    List<TimeSegment> segments = new ArrayList<>();

    // For month: use the last completed month (previous month)
    YearMonth lastMonth = YearMonth.from(getCurrentDateTime()).minusMonths(1);
    String monthLabel = lastMonth.getMonth().toString() + " " + lastMonth.getYear();

    // Calculate start date as the first day of the last month
    LocalDateTime startOfMonth =
        LocalDateTime.of(lastMonth.getYear(), lastMonth.getMonth(), 1, 0, 0, 0);

    // End date is the last day of the last month
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

    segments.add(new TimeSegment(startOfMonth, endOfMonth, monthLabel));

    return segments;
  }

  /**
   * Creates time segments for quarter period (last 3 completed months) Returns segments in
   * chronological order (oldest first)
   */
  private List<TimeSegment> createQuarterPeriodSegments() {
    List<TimeSegment> segments = new ArrayList<>();

    // Get the current date
    LocalDateTime currentDate = getCurrentDateTime();

    // Use the previous month as the last complete month
    YearMonth lastCompleteMonth = YearMonth.from(currentDate).minusMonths(1);

    log.info(
        "Creating quarterly segments with last 3 completed months, ending with: {}",
        lastCompleteMonth);

    // Start with the oldest month and move forward
    // This ensures chronological order without having to reverse later
    YearMonth oldestMonth = lastCompleteMonth.minusMonths(2);

    // For quarter period: 3 segments (last 3 completed months)
    for (int i = 0; i < 3; i++) {
      YearMonth monthYM = oldestMonth.plusMonths(i);
      String label = monthYM.getMonth().toString() + " " + monthYM.getYear();

      // Calculate start date as the first day of the month
      LocalDateTime startOfMonth = monthYM.atDay(1).atStartOfDay();

      // End date is the last day of the month
      LocalDateTime endOfMonth = monthYM.atEndOfMonth().atTime(23, 59, 59);

      // Add segment
      segments.add(new TimeSegment(startOfMonth, endOfMonth, label));

      log.info(
          "Added quarterly segment {}: {} from {} to {}",
          i + 1,
          label,
          startOfMonth.toLocalDate(),
          endOfMonth.toLocalDate());
    }

    // Verify chronological order
    log.info("Quarterly segments in chronological order (oldest to newest):");
    for (int i = 0; i < segments.size(); i++) {
      TimeSegment segment = segments.get(i);
      log.info(
          "Segment {}: {} ({})", i + 1, segment.getLabel(), segment.getStartDate().toLocalDate());
    }

    return segments;
  }

  /**
   * Creates time segments for year period (4 quarters covering exactly one year backward from
   * current month) The quarters should span a complete 12-month cycle starting from the current
   * month backward Results are sorted in chronological order (oldest first).
   *
   * <p>For example, if current month is May 2025, quarters should be: Q1: May 2024-Jul 2024 Q2: Aug
   * 2024-Oct 2024 Q3: Nov 2024-Jan 2025 Q4: Feb 2025-Apr 2025
   */
  private List<TimeSegment> createYearPeriodSegments() {
    List<TimeSegment> segments = new ArrayList<>();

    // Get current date
    LocalDateTime currentDate = getCurrentDateTime();
    int currentMonth = currentDate.getMonthValue();
    int currentYear = currentDate.getYear();

    log.info(
        "Current date: {} (Month: {}, Year: {})",
        currentDate.toLocalDate(),
        currentMonth,
        currentYear);

    // Start with the oldest quarter (12 months ago) and move forward
    // This ensures chronological order without having to sort later
    for (int quarterIndex = 0; quarterIndex < 4; quarterIndex++) {
      // Calculate start month for this quarter starting with oldest (12 months ago)
      // and moving forward in increments of 3 months
      int monthsAgo = 12 - (quarterIndex * 3);

      // Calculate start year/month for this quarter
      YearMonth startYM = YearMonth.of(currentYear, currentMonth).minusMonths(monthsAgo);

      // Calculate end year/month (2 months after start)
      YearMonth endYM = startYM.plusMonths(2);

      // Create datetime objects
      LocalDateTime startDate = startYM.atDay(1).atStartOfDay();
      LocalDateTime endDate = endYM.atEndOfMonth().atTime(23, 59, 59);

      // Create a simple label for the quarter
      String quarterNumber = "Q" + (quarterIndex + 1);

      // Create detailed month part for logging
      String monthPart;
      if (startYM.getYear() == endYM.getYear()) {
        // Same year: "May-Jul 2024"
        monthPart = startYM.getMonth() + "-" + endYM.getMonth() + " " + startYM.getYear();
      } else {
        // Different years: "Nov 2024-Jan 2025"
        monthPart =
            startYM.getMonth()
                + " "
                + startYM.getYear()
                + "-"
                + endYM.getMonth()
                + " "
                + endYM.getYear();
      }

      // Add segment to list (in chronological order)
      segments.add(new TimeSegment(startDate, endDate, quarterNumber));

      // Add detailed logging
      log.info(
          "Quarter {}: {} from {} to {} ({})",
          quarterNumber,
          monthPart,
          startDate.toLocalDate(),
          endDate.toLocalDate(),
          startYM.getYear() == endYM.getYear()
              ? startYM.getYear()
              : startYM.getYear() + "/" + endYM.getYear());
    }

    // Verify chronological order
    log.info("Quarters in chronological order (oldest to newest):");
    for (int i = 0; i < segments.size(); i++) {
      TimeSegment segment = segments.get(i);
      log.info(
          "Quarter {}: {} to {}",
          segment.getLabel(),
          segment.getStartDate().toLocalDate(),
          segment.getEndDate().toLocalDate());
    }

    return segments;
  }

  /** Get BRDs for a specific time segment and calculate transition times */
  private Mono<BrdStatusTransitionTimeResponse.PeriodData> getBrdsAndCalculateTransitionsForSegment(
      TimeSegment segment) {
    return getBrdsWithFormIdsForSegment(segment)
        .flatMap(
            formIdMap -> {
              if (formIdMap.isEmpty()) {
                // Create an empty segment result
                Map<String, Double> emptyAverages = new HashMap<>();
                getValidStatusTransitions()
                    .forEach(transition -> emptyAverages.put(transition, 0.0));

                return Mono.just(
                    BrdStatusTransitionTimeResponse.PeriodData.builder()
                        .label(segment.getLabel())
                        .averages(emptyAverages)
                        .build());
              } else {
                return findStatusTransitionsForSegment(formIdMap, segment)
                    .map(result -> buildPeriodData(result, segment))
                    .defaultIfEmpty(createEmptyPeriodData(segment));
              }
            })
        .defaultIfEmpty(createEmptyPeriodData(segment));
  }

  /** Create empty PeriodData with 0.0 values for all transitions */
  private BrdStatusTransitionTimeResponse.PeriodData createEmptyPeriodData(TimeSegment segment) {
    Map<String, Double> emptyAverages = new HashMap<>();
    getValidStatusTransitions().forEach(transition -> emptyAverages.put(transition, 0.0));

    return BrdStatusTransitionTimeResponse.PeriodData.builder()
        .label(segment.getLabel())
        .averages(emptyAverages)
        .build();
  }

  /** Creates an empty transition response with all valid transitions set to 0.0 */
  private BrdStatusTransitionTimeResponse createEmptyTransitionResponse(
      String period, String username, List<TimeSegment> timeSegments) {

    List<BrdStatusTransitionTimeResponse.PeriodData> emptyPeriodData =
        timeSegments.stream().map(this::createEmptyPeriodData).toList();

    // Create empty trend points
    List<BrdStatusTransitionTimeResponse.TrendPoint> emptyTrendData =
        timeSegments.stream()
            .map(
                segment ->
                    BrdStatusTransitionTimeResponse.TrendPoint.builder()
                        .label(segment.getLabel())
                        .blendedAverage(null)
                        .build())
            .toList();

    return BrdStatusTransitionTimeResponse.builder()
        .period(period)
        .loggedinManager(username)
        .periodData(emptyPeriodData)
        .trendData(emptyTrendData)
        .build();
  }

  /** Gets BRDs created within the specified time segment and extracts their form IDs */
  private Mono<Map<String, String>> getBrdsWithFormIdsForSegment(TimeSegment segment) {
    // Create query to get BRDs with form IDs
    Query query =
        new Query()
            .addCriteria(
                Criteria.where(DashboardConstants.BRD_FORM_ID)
                    .exists(true)
                    .ne(null)
                    .ne(DashboardConstants.EMPTY_STRING));

    // Add date range filter for the segment
    Date startDate = Date.from(segment.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
    Date endDate = Date.from(segment.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
    query.addCriteria(Criteria.where(DashboardConstants.CREATED_AT).gte(startDate).lte(endDate));

    // Only include fields we need
    query.fields().include(DashboardConstants.BRD_ID).include(DashboardConstants.BRD_FORM_ID);

    log.info("Querying BRDs with form IDs for period segment: {}", segment.getLabel());

    return mongoTemplate
        .find(query, BRD.class)
        .collectList()
        .map(
            brds -> {
              log.info(
                  "Found {} BRDs with form IDs for time segment: {}",
                  brds.size(),
                  segment.getLabel());

              // Create mapping from form ID to BRD ID
              return brds.stream()
                  .filter(brd -> brd.getBrdFormId() != null && !brd.getBrdFormId().isEmpty())
                  .collect(
                      Collectors.toMap(
                          BRD::getBrdFormId,
                          BRD::getBrdId,
                          (existing, replacement) -> existing // Keep first in case of duplicates
                          ));
            });
  }

  /** Finds status transitions for the given form IDs within a specific time segment */
  private Mono<TransitionResult> findStatusTransitionsForSegment(
      Map<String, String> formIdToBrdIdMap, TimeSegment segment) {
    int formIdCount = formIdToBrdIdMap.size();
    log.info(
        "Finding status transitions for {} BRD form IDs in time segment: {}",
        formIdCount,
        segment.getLabel());

    // Build optimized query for status update and create audit logs
    Query auditQuery =
        new Query()
            .addCriteria(
                Criteria.where(DashboardConstants.ENTITY_TYPE)
                    .is(DashboardConstants.ENTITY_TYPE_BRD))
            .addCriteria(Criteria.where(DashboardConstants.ENTITY_ID).in(formIdToBrdIdMap.keySet()))
            .addCriteria(
                Criteria.where(DashboardConstants.AUDIT_ACTION)
                    .in(
                        Arrays.asList(
                            DashboardConstants.STATUS_UPDATE_ACTION, BrdConstants.ACTION_CREATE)));

    // Add date range filter for the segment
    Date startDate = Date.from(segment.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
    Date endDate = Date.from(segment.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
    auditQuery.addCriteria(
        Criteria.where(DashboardConstants.EVENT_TIMESTAMP).gte(startDate).lte(endDate));

    // Project only fields we need
    auditQuery
        .fields()
        .include(DashboardConstants.ENTITY_ID)
        .include(DashboardConstants.EVENT_TIMESTAMP)
        .include(DashboardConstants.AUDIT_ACTION)
        .include("newValues.status");

    // Sort by timestamp for proper sequence
    auditQuery.with(Sort.by(Sort.Direction.ASC, DashboardConstants.EVENT_TIMESTAMP));

    return mongoTemplate
        .find(auditQuery, AuditLog.class)
        .collectList()
        .flatMap(
            auditLogs -> {
              int logCount = auditLogs.size();
              log.info(
                  "Found {} audit logs for status transitions in time segment: {}",
                  logCount,
                  segment.getLabel());

              if (logCount == 0) {
                return Mono.empty();
              }

              // Group logs by BRD
              Map<String, List<AuditLog>> logsByBrd =
                  auditLogs.stream().collect(Collectors.groupingBy(AuditLog::getEntityId));

              // Calculate transitions
              Map<String, Double> averages =
                  calculateAverageTransitionTimes(logsByBrd, formIdToBrdIdMap);
              return Mono.just(new TransitionResult(auditLogs, formIdToBrdIdMap, averages));
            });
  }

  /** Builds a PeriodData object from a TransitionResult */
  private BrdStatusTransitionTimeResponse.PeriodData buildPeriodData(
      TransitionResult result, TimeSegment segment) {
    // Get all valid transitions
    Set<String> validTransitions = getValidStatusTransitions();

    // Get calculated averages
    Map<String, Double> calculatedAverages = result.getAverages();

    // Create a new map with all valid transitions
    Map<String, Double> completeAverages = new HashMap<>();

    // Add all valid transitions with their values, or 0.0 if no data
    for (String transition : validTransitions) {
      completeAverages.put(transition, calculatedAverages.getOrDefault(transition, 0.0));
    }

    // Return PeriodData with label and complete transition map
    return BrdStatusTransitionTimeResponse.PeriodData.builder()
        .label(segment.getLabel())
        .averages(completeAverages)
        .build();
  }

  /**
   * Defines the valid status transitions for BRDs
   *
   * @return Set of valid transition keys in the format "FromStatus ➔ ToStatus"
   */
  private Set<String> getValidStatusTransitions() {
    Set<String> validTransitions = new HashSet<>();

    // Use status values from constants to ensure consistency
    List<String> statuses = DashboardConstants.ORDERED_BRD_STATUSES;
    if (statuses.size() >= 2) {
      for (int i = 0; i < statuses.size() - 1; i++) {
        validTransitions.add(
            statuses.get(i) + DashboardConstants.TRANSITION_ARROW + statuses.get(i + 1));
      }

      // Add the final transition to Submit if we have a Signed Off status
      if (statuses.contains(DashboardConstants.SIGNED_OFF_STATUS)) {
        validTransitions.add(
            DashboardConstants.SIGNED_OFF_STATUS + DashboardConstants.TRANSITION_ARROW + "Submit");
      }
    }

    return validTransitions;
  }

  /** Calculates transitions for a specific BRD */
  private void calculateTransitionsForBrd(
      String formId,
      List<AuditLog> brdLogs,
      Map<String, String> formIdToBrdIdMap,
      Map<String, List<Double>> transitionDays) {

    // Skip if fewer than 2 logs (need at least 2 for a transition)
    if (brdLogs.size() < 2) {
      return;
    }

    String brdId = formIdToBrdIdMap.getOrDefault(formId, "Unknown");

    // Sort logs by timestamp
    brdLogs.sort(Comparator.comparing(AuditLog::getEventTimestamp));

    // Extract status sequence for debugging
    List<String> statusSequence =
        brdLogs.stream().map(this::extractStatus).filter(Objects::nonNull).toList();

    if (statusSequence.size() < 2) {
      return;
    }

    log.debug("Processing status sequence for BRD {}: {}", brdId, statusSequence);

    // Get valid transitions
    Set<String> validTransitions = getValidStatusTransitions();

    // Process adjacent transitions
    for (int i = 0; i < brdLogs.size() - 1; i++) {
      processTransitionPair(
          brdLogs.get(i), brdLogs.get(i + 1), brdId, validTransitions, transitionDays);
    }
  }

  /** Processes a pair of consecutive audit logs to find valid transitions */
  private void processTransitionPair(
      AuditLog currentLog,
      AuditLog nextLog,
      String brdId,
      Set<String> validTransitions,
      Map<String, List<Double>> transitionDays) {

    String currentStatus = extractStatus(currentLog);
    String nextStatus = extractStatus(nextLog);

    // Only process if both statuses are valid and different
    if (isValidStatusPair(currentStatus, nextStatus)) {
      String transitionKey = currentStatus + DashboardConstants.TRANSITION_ARROW + nextStatus;

      // Only process valid transitions
      if (validTransitions.contains(transitionKey)) {
        // Calculate transition time in days
        double days =
            calculateDaysBetween(currentLog.getEventTimestamp(), nextLog.getEventTimestamp());

        // Add to transition days map
        transitionDays.computeIfAbsent(transitionKey, k -> new ArrayList<>()).add(days);

        log.debug("Added valid transition: {} (BRD: {}, days: {})", transitionKey, brdId, days);
      } else {
        log.debug("Skipping invalid transition: {} for BRD: {}", transitionKey, brdId);
      }
    }
  }

  /** Checks if a pair of statuses is valid for transition calculation */
  private boolean isValidStatusPair(String currentStatus, String nextStatus) {
    return currentStatus != null && nextStatus != null && !currentStatus.equals(nextStatus);
  }

  /** Calculates average transition times from all collected transitions */
  private Map<String, Double> calculateAverageTransitionTimes(
      Map<String, List<AuditLog>> logsByBrd, Map<String, String> formIdToBrdIdMap) {

    // Map to accumulate transition times by transition type
    Map<String, List<Double>> transitionDays = new HashMap<>();
    int brdCount = logsByBrd.size();

    log.info("Processing transition logs for {} BRDs", brdCount);

    // Process each BRD's logs to calculate transition times
    logsByBrd.forEach(
        (formId, logs) ->
            calculateTransitionsForBrd(formId, logs, formIdToBrdIdMap, transitionDays));

    // Calculate averages for each transition and return
    return calculateAveragesFromTransitions(transitionDays);
  }

  /** Calculates days between two dates */
  private double calculateDaysBetween(LocalDateTime start, LocalDateTime end) {
    return Optional.ofNullable(start)
        .flatMap(
            s ->
                Optional.ofNullable(end)
                    .map(e -> Duration.between(s, e).toMinutes() / (60.0 * 24.0)))
        .orElse(0.0);
  }

  /** Extracts status from audit log */
  private String extractStatus(AuditLog auditLog) {
    if (auditLog == null || auditLog.getNewValues() == null) {
      return null;
    }

    Object statusObj = auditLog.getNewValues().get(DashboardConstants.AUDIT_STATUS);
    return statusObj != null ? statusObj.toString().trim() : null;
  }

  /** Calculates average transition times from all collected transitions */
  private Map<String, Double> calculateAveragesFromTransitions(
      Map<String, List<Double>> transitionDays) {
    if (transitionDays.isEmpty()) {
      return Collections.emptyMap();
    }

    return transitionDays.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  // Calculate average and round to 1 decimal place
                  double avg =
                      entry.getValue().stream()
                          .mapToDouble(Double::doubleValue)
                          .average()
                          .orElse(0.0);

                  double roundedAvg = Math.round(avg * 10.0) / 10.0;
                  log.debug(
                      "Transition '{}': {} days avg ({} samples)",
                      entry.getKey(),
                      roundedAvg,
                      entry.getValue().size());

                  return roundedAvg;
                }));
  }

  @Override
  public Mono<BrdAiPrefillRateResponse> getAiPrefillRateOverTime(String period, String username) {
    // If period is null or empty, use the default value (quarter)
    String validatedPeriod = period;
    if (validatedPeriod == null || validatedPeriod.isEmpty()) {
      validatedPeriod = DashboardConstants.QUARTER_PERIOD;
      log.info("No period specified, using default: {}", validatedPeriod);
    }

    // Validate period value
    if (!DashboardConstants.VALID_PERIODS.contains(validatedPeriod)) {
      return Mono.error(
          new IllegalParameterException(
              "Invalid period parameter. Valid values are: month, quarter, year"));
    }

    return calculateAiPrefillRateStatistics(validatedPeriod, username);
  }

  /** Calculates AI prefill rate statistics for the given time period. */
  private Mono<BrdAiPrefillRateResponse> calculateAiPrefillRateStatistics(
      String period, String username) {
    // Get the appropriate time segments based on the period
    List<TimeSegment> timeSegments = getTimeSegmentsForPeriod(period, true);

    // Log the segments order
    log.info("Using {} time segments for AI prefill rate (period={})", timeSegments.size(), period);
    for (int i = 0; i < timeSegments.size(); i++) {
      TimeSegment segment = timeSegments.get(i);
      log.info(
          "Time segment {}: {} (from {} to {})",
          i + 1,
          segment.getLabel(),
          segment.getStartDate().toLocalDate(),
          segment.getEndDate().toLocalDate());
    }

    // Process each time segment and collect results, maintaining the original order
    return Flux.fromIterable(timeSegments)
        .index() // Add index to keep track of original order
        .flatMap(
            tuple -> {
              TimeSegment segment = tuple.getT2();
              Long index = tuple.getT1();

              return calculateAiPrefillRateForSegment(segment)
                  .map(timeSegment -> new AbstractMap.SimpleEntry<>(index, timeSegment));
            })
        .collectList()
        .map(
            indexedResults -> {
              if (indexedResults.isEmpty()) {
                return createEmptyAiPrefillRateResponse(period, username, timeSegments);
              }

              // Sort by original index to preserve chronological order
              indexedResults.sort(Comparator.comparing(Map.Entry::getKey));

              // Extract just the time segments
              List<BrdAiPrefillRateResponse.TimeSegment> orderedSegments =
                  indexedResults.stream().map(Map.Entry::getValue).toList();

              // Create trend points using the ordered segments
              List<BrdAiPrefillRateResponse.TrendPoint> trendPoints =
                  createTrendPoints(orderedSegments);

              // Log the final order for verification
              log.info("Final order of time segments for AI prefill rate:");
              for (int i = 0; i < orderedSegments.size(); i++) {
                log.info("Segment {}: {}", i + 1, orderedSegments.get(i).getLabel());
              }

              return BrdAiPrefillRateResponse.builder()
                  .period(period)
                  .loggedinManager(username)
                  .timeSegments(orderedSegments)
                  .trendData(trendPoints)
                  .build();
            });
  }

  /** Calculates AI prefill rate for a specific time segment. */
  private Mono<BrdAiPrefillRateResponse.TimeSegment> calculateAiPrefillRateForSegment(
      TimeSegment segment) {
    Query query = new Query();

    // Filter for BRDs with AI prefill rate (not null)
    query.addCriteria(Criteria.where(DashboardConstants.AI_PREFILL_RATE).exists(true).ne(null));

    // Add date range filter for the segment
    Date startDate = Date.from(segment.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
    Date endDate = Date.from(segment.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
    query.addCriteria(Criteria.where(DashboardConstants.CREATED_AT).gte(startDate).lte(endDate));

    // Include only fields we need to optimize query
    query
        .fields()
        .include(DashboardConstants.BRD_FORM_ID)
        .include(DashboardConstants.AI_PREFILL_RATE)
        .include(DashboardConstants.CREATED_AT);

    log.info("Querying BRDs with AI prefill rate for period segment: {}", segment.getLabel());

    return mongoTemplate
        .find(query, BRD.class)
        .collectList()
        .map(
            brds -> {
              int brdCount = brds.size();
              log.info(
                  "Found {} BRDs with AI prefill rate for time segment: {}",
                  brdCount,
                  segment.getLabel());

              if (brds.isEmpty()) {
                return createEmptyTimeSegment(segment.getLabel());
              }

              // Calculate average AI prefill rate
              double totalPrefillRate = brds.stream().mapToDouble(BRD::getAiPrefillRate).sum();

              // Calculate average and round to 2 decimal places
              double averagePrefillRate = Math.round((totalPrefillRate / brdCount) * 100.0) / 100.0;

              log.info(
                  "Calculated average AI prefill rate for {}: {} (across {} BRDs)",
                  segment.getLabel(),
                  averagePrefillRate,
                  brdCount);

              // For future extension, prepare the brdTypeRates object with only newBrdRate
              // populated
              BrdAiPrefillRateResponse.BrdTypeRates brdTypeRates =
                  BrdAiPrefillRateResponse.BrdTypeRates.builder()
                      .newBrdRate(averagePrefillRate)
                      .build();

              return BrdAiPrefillRateResponse.TimeSegment.builder()
                  .label(segment.getLabel())
                  .averagePrefillRate(averagePrefillRate)
                  .brdCount(brdCount)
                  .brdTypeRates(brdTypeRates)
                  .build();
            })
        .defaultIfEmpty(createEmptyTimeSegment(segment.getLabel()));
  }

  /** Creates trend points from time segment data for visualization. */
  private List<BrdAiPrefillRateResponse.TrendPoint> createTrendPoints(
      List<BrdAiPrefillRateResponse.TimeSegment> timeSegments) {

    return timeSegments.stream()
        .map(
            segment ->
                BrdAiPrefillRateResponse.TrendPoint.builder()
                    .label(segment.getLabel())
                    .prefillRate(segment.getAveragePrefillRate())
                    .build())
        .toList();
  }

  /** Creates an empty time segment for periods with no data. */
  private BrdAiPrefillRateResponse.TimeSegment createEmptyTimeSegment(String label) {
    return BrdAiPrefillRateResponse.TimeSegment.builder()
        .label(label)
        .averagePrefillRate(0.0)
        .brdCount(0)
        .brdTypeRates(BrdAiPrefillRateResponse.BrdTypeRates.builder().newBrdRate(0.0).build())
        .build();
  }

  /**
   * Creates an empty response when no data is found. Preserves the chronological order of the time
   * segments.
   */
  private BrdAiPrefillRateResponse createEmptyAiPrefillRateResponse(
      String period, String username, List<TimeSegment> timeSegments) {

    // Create empty segments in the same order as the input time segments
    List<BrdAiPrefillRateResponse.TimeSegment> emptySegments = new ArrayList<>();
    List<BrdAiPrefillRateResponse.TrendPoint> emptyTrendPoints = new ArrayList<>();

    // Populate both lists in the same order
    for (TimeSegment segment : timeSegments) {
      // Create empty time segment
      BrdAiPrefillRateResponse.TimeSegment emptySegment =
          createEmptyTimeSegment(segment.getLabel());
      emptySegments.add(emptySegment);

      // Create empty trend point
      BrdAiPrefillRateResponse.TrendPoint emptyTrendPoint =
          BrdAiPrefillRateResponse.TrendPoint.builder()
              .label(segment.getLabel())
              .prefillRate(0.0)
              .build();
      emptyTrendPoints.add(emptyTrendPoint);

      // Log for verification
      log.debug("Added empty segment for {}", segment.getLabel());
    }

    // Log the order of segments
    log.info(
        "Created empty AI prefill rate response with {} ordered segments", emptySegments.size());

    return BrdAiPrefillRateResponse.builder()
        .period(period)
        .loggedinManager(username)
        .timeSegments(emptySegments)
        .trendData(emptyTrendPoints)
        .build();
  }

  /**
   * Creates time segments based on selected period
   *
   * @param isAiPrefillRate Whether this is being called for the AI prefill rate endpoint
   */
  private List<TimeSegment> getTimeSegmentsForPeriod(String period, boolean isAiPrefillRate) {
    List<TimeSegment> segments;

    switch (period) {
      case DashboardConstants.MONTH_PERIOD:
        segments = createMonthPeriodSegments();
        break;

      case DashboardConstants.QUARTER_PERIOD:
        segments = createQuarterPeriodSegments();
        break;

      case DashboardConstants.YEAR_PERIOD:
        // Special case for AI prefill rate endpoint when period = year
        if (isAiPrefillRate) {
          segments = createMonthlySegmentsForYear();
        } else {
          segments = createYearPeriodSegments();
        }
        break;

      default:
        log.warn("Unexpected period value: '{}'. Falling back to quarter period.", period);
        segments = createQuarterPeriodSegments();
        break;
    }

    return segments;
  }

  /**
   * Creates 12 monthly time segments for a year period, specifically for the AI prefill rate
   * endpoint. Includes the last 12 completed months, which may span across previous years. Results
   * are sorted in chronological order (oldest first).
   */
  private List<TimeSegment> createMonthlySegmentsForYear() {
    List<TimeSegment> segments = new ArrayList<>();

    // Get the current date and time
    LocalDateTime currentDate = getCurrentDateTime();

    // Use the previous month as the last complete month
    YearMonth lastCompleteMonth = YearMonth.from(currentDate).minusMonths(1);

    log.info(
        "Creating 12 monthly segments for year period, starting from last complete month: {}",
        lastCompleteMonth);

    // Start with the oldest month (12 months ago) and move forward
    // This ensures chronological order without having to reverse later
    YearMonth oldestMonth = lastCompleteMonth.minusMonths(11);

    for (int i = 0; i < 12; i++) {
      // Calculate the month (going forward from oldest month)
      YearMonth monthYM = oldestMonth.plusMonths(i);

      // Create label for this month
      String label = monthYM.getMonth().toString() + " " + monthYM.getYear();

      // Create start and end date-times for this month
      LocalDateTime startOfMonth = monthYM.atDay(1).atStartOfDay();
      LocalDateTime endOfMonth = monthYM.atEndOfMonth().atTime(23, 59, 59);

      // Add segment
      segments.add(new TimeSegment(startOfMonth, endOfMonth, label));

      log.info(
          "Added monthly segment {}: {} from {} to {}",
          i + 1,
          label,
          startOfMonth.toLocalDate(),
          endOfMonth.toLocalDate());
    }

    // Verify chronological order
    log.info("Monthly segments in chronological order (oldest to newest):");
    for (int i = 0; i < segments.size(); i++) {
      TimeSegment segment = segments.get(i);
      log.info(
          "Segment {}: {} ({})", i + 1, segment.getLabel(), segment.getStartDate().toLocalDate());
    }

    return segments;
  }

  /**
   * Gets the current date time. This method is extracted to allow mocking in tests.
   *
   * @return Current LocalDateTime
   */
  protected LocalDateTime getCurrentDateTime() {
    return LocalDateTime.now();
  }

  @Override
  public Mono<ResponseEntity<Api<UnresolvedCommentGroupsCountResponse>>>
      getUnresolvedCommentGroupsCount(String scope, String username) {
    return getNonSubmittedBrds()
        .flatMap(brds -> getCommentGroupsForBrds(brds, scope, username))
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        DashboardConstants.SUCCESSFUL,
                        DashboardConstants.UNRESOLVED_COMMENT_GROUPS_SUCCESS_MESSAGE,
                        Optional.of(response),
                        Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  private Mono<List<BRD>> getNonSubmittedBrds() {
    Query brdQuery =
        new Query(Criteria.where(DashboardConstants.BRD_STATUS).in(ORDERED_BRD_STATUSES));
    brdQuery
        .fields()
        .include("brdFormId")
        .include("brdId")
        .include("brdName")
        .include(DashboardConstants.BRD_STATUS);

    return mongoTemplate.find(brdQuery, BRD.class).collectList();
  }

  private Mono<UnresolvedCommentGroupsCountResponse> getCommentGroupsForBrds(
      List<BRD> brds, String scope, String username) {
    if (brds.isEmpty()) {
      return Mono.just(createEmptyUnresolvedCommentsResponse());
    }

    List<String> brdFormIds = brds.stream().map(BRD::getBrdFormId).toList();
    Map<String, BRD> brdMap = createBrdMap(brds);

    return findUnresolvedCommentGroups(brdFormIds, scope, username)
        .collectMultimap(BrdFieldCommentGroup::getBrdFormId)
        .map(commentGroups -> buildCommentGroupResponse(commentGroups, brdMap));
  }

  private Map<String, BRD> createBrdMap(List<BRD> brds) {
    return brds.stream().collect(Collectors.toMap(BRD::getBrdFormId, brd -> brd));
  }

  private Flux<BrdFieldCommentGroup> findUnresolvedCommentGroups(
      List<String> brdFormIds, String scope, String username) {
    Query commentQuery =
        new Query(
            Criteria.where(DashboardConstants.BRD_FORM_ID)
                .in(brdFormIds)
                .and(DashboardConstants.BRD_STATUS)
                .is(BrdConstants.COMMENT_STATUS_PENDING));

    // If scope is ME, filter by createdBy field matching the current username
    if ("ME".equals(scope) && username != null) {
      commentQuery.addCriteria(Criteria.where(DashboardConstants.CREATED_BY).is(username));
      log.info("Filtering unresolved comment groups for user: {}", username);
    }

    commentQuery
        .fields()
        .include(DashboardConstants.BRD_FORM_ID)
        .include(DashboardConstants.BRD_STATUS);

    return mongoTemplate.find(commentQuery, BrdFieldCommentGroup.class);
  }

  private UnresolvedCommentGroupsCountResponse buildCommentGroupResponse(
      Map<String, Collection<BrdFieldCommentGroup>> commentGroupsByBrd, Map<String, BRD> brdMap) {

    int totalCount = 0;
    Map<String, UnresolvedCommentGroupsCountResponse.BrdCommentGroupCount> brdCounts =
        new HashMap<>();

    for (Map.Entry<String, Collection<BrdFieldCommentGroup>> entry :
        commentGroupsByBrd.entrySet()) {
      String brdFormId = entry.getKey();
      Collection<BrdFieldCommentGroup> commentGroups = entry.getValue();
      BRD brd = brdMap.get(brdFormId);

      if (brd != null) {
        int count = commentGroups.size();
        totalCount += count;
        brdCounts.put(brdFormId, createBrdCommentCount(brd, count));
      }
    }

    return UnresolvedCommentGroupsCountResponse.builder()
        .totalCount(totalCount)
        .brdCounts(brdCounts)
        .build();
  }

  private UnresolvedCommentGroupsCountResponse createEmptyUnresolvedCommentsResponse() {
    return UnresolvedCommentGroupsCountResponse.builder().totalCount(0).brdCounts(Map.of()).build();
  }

  private UnresolvedCommentGroupsCountResponse.BrdCommentGroupCount createBrdCommentCount(
      BRD brd, int count) {
    return UnresolvedCommentGroupsCountResponse.BrdCommentGroupCount.builder()
        .brdId(brd.getBrdId())
        .brdName(brd.getBrdName())
        .status(brd.getStatus())
        .count(count)
        .build();
  }

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException || ex instanceof NotFoundException) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  @Override
  public Mono<ResponseEntity<Api<BrdTypeCountResponse>>> getBrdCountsByType(
      String scope, String username) {
    log.info("Getting BRD counts by type with scope: {} and username: {}", scope, username);

    // Get BRDs for weekly metrics calculation
    return getBrdsForWeeklyMetrics(scope, username)
        .collectList()
        .map(
            brds -> {
              // Build response with only weekly metrics
              BrdTypeCountResponse response =
                  BrdTypeCountResponse.builder()
                      .scope(scope)
                      .weeklyMetrics(calculateWeeklyTypeMetrics(brds))
                      .build();

              return ResponseEntity.ok(
                  new Api<>(
                      DashboardConstants.SUCCESSFUL,
                      DashboardConstants.BRD_COUNTS_BY_TYPE_SUCCESS_MESSAGE,
                      Optional.of(response),
                      Optional.empty()));
            })
        .onErrorMap(this::handleErrors);
  }

  /** Get BRDs for weekly metrics calculation */
  private Flux<BRD> getBrdsForWeeklyMetrics(String scope, String username) {
    // Define the time range - past 52 weeks ending with last month
    LocalDate now = LocalDate.now();
    LocalDate endDate = now.withDayOfMonth(1).minusDays(1); // Last day of previous month
    LocalDate startDate = endDate.minusWeeks(51); // Go back 51 more weeks for a total of 52 weeks

    Query query =
        new Query(
            Criteria.where(DashboardConstants.CREATED_AT)
                .gte(startDate.atStartOfDay())
                .lte(endDate.atTime(23, 59, 59)));

    // Apply scope filter if "ME"
    if (username != null && "ME".equals(scope)) {
      query.addCriteria(Criteria.where(DashboardConstants.CREATOR).is(username));
    }

    // Only select fields we need
    query.fields().include(DashboardConstants.BRD_TYPE).include(DashboardConstants.CREATED_AT);

    return mongoTemplate.find(query, BRD.class);
  }

  /** Calculate weekly metrics for BRD types */
  private BrdTypeCountResponse.WeeklyMetrics calculateWeeklyTypeMetrics(List<BRD> brds) {
    // Define the time range - past 52 weeks ending with last month
    LocalDate now = LocalDate.now();
    LocalDate endDate = now.withDayOfMonth(1).minusDays(1); // Last day of previous month
    LocalDate startDate = endDate.minusWeeks(51); // Go back 51 more weeks for a total of 52 weeks

    // Generate list of week identifiers in reverse order (most recent first)
    List<String> weeks = generateWeeksList(startDate, endDate);
    Collections.reverse(weeks); // Reverse so week 1 is the most recent (end of previous month)

    // Replace ISO format with simple week numbers
    List<String> weekLabels = new ArrayList<>();
    for (int i = 0; i < weeks.size(); i++) {
      weekLabels.add(DashboardConstants.WEEK_PREFIX + (i + 1)); // Week 1, Week 2, etc.
    }

    // Create map from ISO week to index for faster lookup
    Map<String, Integer> weekKeyToIndexMap = new HashMap<>();
    for (int i = 0; i < weeks.size(); i++) {
      weekKeyToIndexMap.put(weeks.get(i), i);
    }

    // Filter BRDs to those within our date range
    List<BRD> brdInRange =
        brds.stream()
            .filter(brd -> brd.getCreatedAt() != null)
            .filter(
                brd -> {
                  LocalDate brdDate = brd.getCreatedAt().toLocalDate();
                  return !brdDate.isBefore(startDate) && !brdDate.isAfter(endDate);
                })
            .toList();

    // Initialize counters - one value per week for the past 52 weeks
    int[] newCounts = new int[weekLabels.size()];
    int[] updateCounts = new int[weekLabels.size()];
    int[] triageCounts = new int[weekLabels.size()];
    int[] totalCounts = new int[weekLabels.size()];

    // Process each BRD - TEMPORARY CHANGE: Treat all BRDs as NEW type
    for (BRD brd : brdInRange) {
      processBrdForWeeklyCountsAsNew(brd, weekKeyToIndexMap, newCounts, totalCounts);
    }

    // Build WeeklyMetrics response
    return BrdTypeCountResponse.WeeklyMetrics.builder()
        .weeks(weekLabels)
        .counts(
            BrdTypeCountResponse.WeeklyTypeCounts.builder()
                .newCounts(Arrays.stream(newCounts).boxed().toList())
                .updateCounts(Arrays.stream(updateCounts).boxed().toList())
                .triageCounts(Arrays.stream(triageCounts).boxed().toList())
                .totalCounts(Arrays.stream(totalCounts).boxed().toList())
                .build())
        .build();
  }

  /**
   * Process a single BRD and update the weekly counts arrays TEMPORARY IMPLEMENTATION: Treat all
   * BRDs as NEW type
   */
  private void processBrdForWeeklyCountsAsNew(
      BRD brd, Map<String, Integer> weekKeyToIndexMap, int[] newCounts, int[] totalCounts) {

    // Get the ISO week key for this BRD
    String weekKey = formatWeek(brd.getCreatedAt().toLocalDate());

    // Find the week index using the map
    Integer weekIndex = weekKeyToIndexMap.get(weekKey);
    if (weekIndex == null) {
      return; // Skip if week not found
    }

    // TEMPORARY CHANGE: Count all BRDs as NEW type
    newCounts[weekIndex]++;

    // TEMPORARY CHANGE: updateCounts remains zero (array is initialized with zeros)
    // No update to updateCounts array

    // Increment total count for this week
    totalCounts[weekIndex]++;
  }

  private List<String> generateWeeksList(LocalDate startDate, LocalDate endDate) {
    List<String> weeks = new ArrayList<>();

    // Adjust start to beginning of week (Monday)
    LocalDate weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // Adjust end to end of week (Sunday)
    LocalDate weekEnd = endDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

    LocalDate current = weekStart;
    while (!current.isAfter(weekEnd)) {
      weeks.add(formatWeek(current));
      current = current.plusWeeks(1);
    }

    return weeks;
  }

  private String formatWeek(LocalDate date) {
    // Use ISO week format (consistent with WeekFields)
    WeekFields weekFields = WeekFields.ISO;
    int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
    int year = date.get(weekFields.weekBasedYear()); // Important: use week-based year!
    return String.format("%d-W%02d", year, weekNumber);
  }

  @Override
  public Mono<ResponseEntity<Api<BrdUploadMetricsResponse>>> getBrdUploadMetrics(
      String filter, String username) {
    log.info("Getting BRD upload metrics with filter: {} and username: {}", filter, username);

    if (!isValidFilter(filter)) {
      return Mono.error(new BadRequestException("Invalid filter. Must be one of: OPEN, ALL"));
    }

    // Determine scope based on username
    String scopeValue = username != null ? "ME" : "TEAM";
    log.debug("Determined scope value: {}", scopeValue);

    boolean includeWeeklyMetrics = "ALL".equalsIgnoreCase(filter);
    return getBrdsForUploadMetrics(filter, includeWeeklyMetrics, username)
        .collectList()
        .map(
            brds -> {
              BrdUploadMetricsResponse response = calculateUploadMetrics(brds, filter);

              // Set the scope in the response
              response.setScope(scopeValue);

              // Add weekly metrics only if filter is ALL
              if (includeWeeklyMetrics) {
                response.setWeeklyMetrics(calculateWeeklyMetrics(brds));
              }

              return ResponseEntity.ok(
                  new Api<>(
                      DashboardConstants.SUCCESSFUL,
                      DashboardConstants.BRD_UPLOAD_METRICS_SUCCESS_MESSAGE,
                      Optional.of(response),
                      Optional.empty()));
            })
        .onErrorMap(this::handleErrors);
  }

  private boolean isValidFilter(String filter) {
    return filter != null && (filter.equalsIgnoreCase("OPEN") || filter.equalsIgnoreCase("ALL"));
  }

  private Flux<BRD> getBrdsForUploadMetrics(
      String filter, boolean includeWeeklyMetrics, String username) {
    Query query = new Query();

    // Apply filter if OPEN (exclude Submitted status)
    if ("OPEN".equalsIgnoreCase(filter)) {
      List<String> excludedStatuses = Arrays.asList(DashboardConstants.SUBMITTED_STATUS);
      query.addCriteria(Criteria.where(DashboardConstants.BRD_STATUS).nin(excludedStatuses));
    }

    // Only select fields we need for metrics calculation
    query
        .fields()
        .include(DashboardConstants.BRD_ID)
        .include(DashboardConstants.BRD_TYPE)
        .include(DashboardConstants.ORIGINAL_SSD_FILENAME)
        .include(DashboardConstants.ORIGINAL_CONTRACT_FILENAME);

    // Include createdAt if we need weekly metrics
    if (includeWeeklyMetrics) {
      query.fields().include(DashboardConstants.CREATED_AT);
    }

    if (username != null) {
      query.addCriteria(Criteria.where(DashboardConstants.CREATOR).is(username));
    }

    return mongoTemplate.find(query, BRD.class);
  }

  private BrdUploadMetricsResponse calculateUploadMetrics(List<BRD> brds, String filter) {
    // TEMPORARY CHANGE: Treat all BRDs as NEW type
    List<BRD> newBrds = brds;
    List<BRD> updateBrds = List.of(); // Empty list for UPDATE type

    // Calculate metrics
    BrdUploadMetricsResponse.TypeMetrics ssdNewMetrics = calculateFileTypeMetrics(newBrds, true);
    BrdUploadMetricsResponse.TypeMetrics ssdUpdateMetrics =
        calculateFileTypeMetrics(updateBrds, true);
    BrdUploadMetricsResponse.TypeMetrics contractNewMetrics =
        calculateFileTypeMetrics(newBrds, false);
    BrdUploadMetricsResponse.TypeMetrics contractUpdateMetrics =
        calculateFileTypeMetrics(updateBrds, false);

    // Build the response
    return BrdUploadMetricsResponse.builder()
        .filterType(filter)
        .ssdUploads(
            BrdUploadMetricsResponse.UploadMetrics.builder()
                .newBrds(ssdNewMetrics)
                .updateBrds(ssdUpdateMetrics)
                .build())
        .contractUploads(
            BrdUploadMetricsResponse.UploadMetrics.builder()
                .newBrds(contractNewMetrics)
                .updateBrds(contractUpdateMetrics)
                .build())
        .build();
  }

  /** Calculate metrics for a specific file type (SSD or Contract) */
  private BrdUploadMetricsResponse.TypeMetrics calculateFileTypeMetrics(
      List<BRD> brds, boolean isSsd) {
    int totalCount = brds.size();

    // Count BRDs with the specified file type uploaded
    long uploadedCount = brds.stream().filter(brd -> hasFileUploaded(brd, isSsd)).count();

    // Calculate percentages
    int uploadedPercentage = totalCount > 0 ? (int) ((uploadedCount * 100) / totalCount) : 0;
    int notUploadedCount = totalCount - (int) uploadedCount;
    int notUploadedPercentage = totalCount > 0 ? (100 - uploadedPercentage) : 0;

    // Build TypeMetrics
    return BrdUploadMetricsResponse.TypeMetrics.builder()
        .totalCount(totalCount)
        .uploadedCount((int) uploadedCount)
        .uploadedPercentage(uploadedPercentage)
        .notUploadedCount(notUploadedCount)
        .notUploadedPercentage(notUploadedPercentage)
        .build();
  }

  /** Check if a BRD has the specified file type uploaded */
  private boolean hasFileUploaded(BRD brd, boolean isSsd) {
    String fileName = isSsd ? brd.getOriginalSSDFileName() : brd.getOriginalContractFileName();
    return fileName != null && !fileName.trim().isEmpty();
  }

  private BrdUploadMetricsResponse.WeeklyMetrics calculateWeeklyMetrics(List<BRD> brds) {
    // Define the time range - past 52 weeks ending with last month
    LocalDate now = LocalDate.now();
    LocalDate endDate = now.withDayOfMonth(1).minusDays(1); // Last day of previous month
    LocalDate startDate = endDate.minusWeeks(51); // Go back 51 more weeks for a total of 52 weeks

    log.info(
        "Calculating weekly metrics from {} to {}",
        startDate.format(DateTimeFormatter.ISO_DATE),
        endDate.format(DateTimeFormatter.ISO_DATE));

    // Generate list of week identifiers in reverse order (most recent first)
    List<String> weeks = generateWeeksList(startDate, endDate);
    Collections.reverse(weeks); // Reverse so week 1 is the most recent (end of previous month)

    List<String> weekLabels = new ArrayList<>();
    for (int i = 0; i < weeks.size(); i++) {
      weekLabels.add(DashboardConstants.WEEK_PREFIX + (i + 1)); // Week 1, Week 2, etc.
    }

    Map<String, Integer> weekKeyToIndexMap = new HashMap<>();
    for (int i = 0; i < weeks.size(); i++) {
      weekKeyToIndexMap.put(weeks.get(i), i);
    }

    // Filter BRDs to those within our date range
    List<BRD> brdInRange =
        brds.stream()
            .filter(brd -> brd.getCreatedAt() != null)
            .filter(
                brd -> {
                  LocalDate brdDate = brd.getCreatedAt().toLocalDate();
                  return !brdDate.isBefore(startDate) && !brdDate.isAfter(endDate);
                })
            .toList();

    // Process BRDs and compute counts
    WeeklyUploadCounts uploadCounts =
        computeWeeklyUploadCounts(brdInRange, weekLabels, weekKeyToIndexMap);

    // Convert to Lists for the response
    return BrdUploadMetricsResponse.WeeklyMetrics.builder()
        .weeks(weekLabels)
        .newBrds(
            BrdUploadMetricsResponse.WeeklyTypeMetrics.builder()
                .totalCounts(Arrays.stream(uploadCounts.getTotalNewCounts()).boxed().toList())
                .ssdUploadedCounts(Arrays.stream(uploadCounts.getSsdNewCounts()).boxed().toList())
                .contractUploadedCounts(
                    Arrays.stream(uploadCounts.getContractNewCounts()).boxed().toList())
                .build())
        .updateBrds(
            BrdUploadMetricsResponse.WeeklyTypeMetrics.builder()
                .totalCounts(Arrays.stream(uploadCounts.getTotalUpdateCounts()).boxed().toList())
                .ssdUploadedCounts(
                    Arrays.stream(uploadCounts.getSsdUpdateCounts()).boxed().toList())
                .contractUploadedCounts(
                    Arrays.stream(uploadCounts.getContractUpdateCounts()).boxed().toList())
                .build())
        .build();
  }

  /** Helper class to hold the weekly upload counts */
  @lombok.Data
  private static class WeeklyUploadCounts {
    private final int[] totalNewCounts;
    private final int[] totalUpdateCounts;
    private final int[] ssdNewCounts;
    private final int[] ssdUpdateCounts;
    private final int[] contractNewCounts;
    private final int[] contractUpdateCounts;
  }

  /** Working container for weekly counts while processing */
  @lombok.Data
  private static class WeeklyCounts {
    private final int[] totalNewCounts;
    private final int[] totalUpdateCounts;
    private final int[] ssdNewCounts;
    private final int[] ssdUpdateCounts;
    private final int[] contractNewCounts;
    private final int[] contractUpdateCounts;

    // Convert to the immutable result object
    public WeeklyUploadCounts toUploadCounts() {
      return new WeeklyUploadCounts(
          totalNewCounts,
          totalUpdateCounts,
          ssdNewCounts,
          ssdUpdateCounts,
          contractNewCounts,
          contractUpdateCounts);
    }
  }

  /** Compute weekly upload counts from the filtered BRDs */
  private WeeklyUploadCounts computeWeeklyUploadCounts(
      List<BRD> brdInRange, List<String> weekLabels, Map<String, Integer> weekKeyToIndexMap) {

    // Initialize counters - one value per week for the past 52 weeks
    WeeklyCounts counts =
        new WeeklyCounts(
            new int[weekLabels.size()],
            new int[weekLabels.size()], // This will always remain zeros
            new int[weekLabels.size()],
            new int[weekLabels.size()], // This will always remain zeros
            new int[weekLabels.size()],
            new int[weekLabels.size()]); // This will always remain zeros

    // Process each BRD
    for (BRD brd : brdInRange) {
      // Get week index, skip if not valid
      Integer weekIndex = getWeekIndexForBrd(brd, weekKeyToIndexMap);
      if (weekIndex == null) {
        continue;
      }

      // Process BRD counts - increment NEW and total counts
      counts.getTotalNewCounts()[weekIndex]++;

      // Check for file uploads
      boolean hasSsd = hasFileUploaded(brd, true);
      boolean hasContract = hasFileUploaded(brd, false);

      // Increment file upload counts (for NEW type only)
      if (hasSsd) {
        counts.getSsdNewCounts()[weekIndex]++;
      }

      if (hasContract) {
        counts.getContractNewCounts()[weekIndex]++;
      }
    }

    return counts.toUploadCounts();
  }

  /** Get the week index for a BRD, or null if invalid */
  private Integer getWeekIndexForBrd(BRD brd, Map<String, Integer> weekKeyToIndexMap) {
    if (brd.getCreatedAt() == null) {
      log.debug("BRD is missing createdAt timestamp");
      return null;
    }

    String weekKey = formatWeek(brd.getCreatedAt().toLocalDate());
    Integer weekIndex = weekKeyToIndexMap.get(weekKey);

    if (weekIndex == null) {
      log.debug("Week key not found in map: {}", weekKey);
    }

    return weekIndex;
  }

  @Override
  public Mono<ResponseEntity<Api<CommentResolutionTimeResponse>>>
      getAverageCommentResolutionTime() {
    log.info("Calculating average comment resolution time for all periods");

    // Get time segments for each period
    List<TimeSegment> monthSegments = getTimeSegmentsForPeriod(DashboardConstants.MONTH_PERIOD);
    List<TimeSegment> quarterSegments = getTimeSegmentsForPeriod(DashboardConstants.QUARTER_PERIOD);
    // Use monthly segments for year period instead of quarterly segments
    List<TimeSegment> yearSegments = createMonthlySegmentsForYear();

    // Calculate month period data (single value)
    Mono<CommentResolutionTimeResponse.MonthPeriod> monthPeriodMono =
        calculateMonthPeriod(monthSegments.get(0));

    // Calculate quarter period data (trend line)
    Mono<CommentResolutionTimeResponse.PeriodTrend> quarterTrendMono =
        calculatePeriodTrend(quarterSegments);

    // Calculate year period data (trend line)
    Mono<CommentResolutionTimeResponse.PeriodTrend> yearTrendMono =
        calculatePeriodTrend(yearSegments);

    // Combine all data into a single response
    return Mono.zip(monthPeriodMono, quarterTrendMono, yearTrendMono)
        .map(
            tuple ->
                CommentResolutionTimeResponse.builder()
                    .monthPeriod(tuple.getT1())
                    .quarterPeriod(tuple.getT2())
                    .yearPeriod(tuple.getT3())
                    .build())
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        DashboardConstants.SUCCESS,
                        DashboardConstants.COMMENT_RESOLUTION_TIME_SUCCESS_MESSAGE,
                        Optional.of(response),
                        Optional.empty())))
        .onErrorResume(
            error -> {
              log.error("Error calculating comment resolution time: {}", error.getMessage(), error);
              return Mono.just(
                  ResponseEntity.<Api<CommentResolutionTimeResponse>>status(
                          HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              DashboardConstants.ERROR,
                              error.getMessage() != null
                                  ? error.getMessage()
                                  : DashboardConstants.ERROR_MESSAGE,
                              Optional.empty(),
                              Optional.of(
                                  Map.of(
                                      DashboardConstants.ERROR,
                                      error.getMessage() != null
                                          ? error.getMessage()
                                          : "Unknown error")))));
            });
  }

  /** Calculates the month period data (single value) */
  private Mono<CommentResolutionTimeResponse.MonthPeriod> calculateMonthPeriod(
      TimeSegment monthSegment) {
    return findResolvedCommentGroups(monthSegment)
        .collectList()
        .map(
            commentGroups -> {
              if (commentGroups.isEmpty()) {
                log.info("No resolved comment groups found for month: {}", monthSegment.getLabel());
                return CommentResolutionTimeResponse.MonthPeriod.builder()
                    .month(monthSegment.getLabel())
                    .averageResolutionDays(0.0)
                    .resolvedCommentsCount(0)
                    .build();
              }

              double totalDays =
                  commentGroups.stream().mapToDouble(this::calculateResolutionDays).sum();

              double averageDays = totalDays / commentGroups.size();
              log.info(
                  "Average resolution time for {} comment groups in month {}: {} days",
                  commentGroups.size(),
                  monthSegment.getLabel(),
                  averageDays);

              return CommentResolutionTimeResponse.MonthPeriod.builder()
                  .month(monthSegment.getLabel())
                  .averageResolutionDays(averageDays)
                  .resolvedCommentsCount(commentGroups.size())
                  .build();
            });
  }

  /** Calculates period trend data (for quarter or year) */
  private Mono<CommentResolutionTimeResponse.PeriodTrend> calculatePeriodTrend(
      List<TimeSegment> segments) {
    return Flux.fromIterable(segments)
        .flatMap(this::calculatePeriodDataPoint)
        .collectList()
        .map(
            dataPoints ->
                CommentResolutionTimeResponse.PeriodTrend.builder()
                    .monthlyData(dataPoints)
                    .build());
  }

  /** Calculates a single data point for a trend */
  private Mono<CommentResolutionTimeResponse.MonthlyDataPoint> calculatePeriodDataPoint(
      TimeSegment segment) {
    return findResolvedCommentGroups(segment)
        .collectList()
        .map(
            commentGroups -> {
              if (commentGroups.isEmpty()) {
                return CommentResolutionTimeResponse.MonthlyDataPoint.builder()
                    .month(segment.getLabel())
                    .averageResolutionDays(0.0)
                    .resolvedCommentsCount(0)
                    .build();
              }

              double totalDays =
                  commentGroups.stream().mapToDouble(this::calculateResolutionDays).sum();

              double averageDays = totalDays / commentGroups.size();

              return CommentResolutionTimeResponse.MonthlyDataPoint.builder()
                  .month(segment.getLabel())
                  .averageResolutionDays(averageDays)
                  .resolvedCommentsCount(commentGroups.size())
                  .build();
            });
  }

  /**
   * Finds comment groups that were resolved within the specified time segment.
   *
   * @param timeSegment The time segment to search within
   * @return Flux of resolved BrdFieldCommentGroup
   */
  private Flux<BrdFieldCommentGroup> findResolvedCommentGroups(TimeSegment timeSegment) {
    Query query = new Query();

    // Only include comment groups with status = "Resolved"
    query.addCriteria(
        Criteria.where(DashboardConstants.BRD_STATUS)
            .is(DashboardConstants.COMMENT_STATUS_RESOLVED));

    // Filter by updatedAt within the time segment - use LocalDateTime directly
    // (since resolution happens when the status changes to "Resolved" and updatedAt is set)
    query.addCriteria(
        Criteria.where("updatedAt").gte(timeSegment.getStartDate()).lt(timeSegment.getEndDate()));

    log.debug("Searching for resolved comment groups with query criteria");

    return mongoTemplate
        .find(query, BrdFieldCommentGroup.class)
        .doOnNext(
            comment ->
                log.debug(
                    "Found resolved comment group: id={}, brdFormId={}, updatedAt={}, createdAt={}",
                    comment.getId(),
                    comment.getBrdFormId(),
                    comment.getUpdatedAt(),
                    comment.getCreatedAt()));
  }

  /**
   * Calculates the resolution time in days for a comment group.
   *
   * @param commentGroup The comment group to calculate for
   * @return The resolution time in days (with decimal precision)
   */
  private double calculateResolutionDays(BrdFieldCommentGroup commentGroup) {
    LocalDateTime createdAt = commentGroup.getCreatedAt();
    LocalDateTime resolvedAt = commentGroup.getUpdatedAt();

    if (createdAt == null || resolvedAt == null) {
      log.warn(
          "Comment group missing timestamp data: id={}, createdAt={}, updatedAt={}",
          commentGroup.getId(),
          createdAt,
          resolvedAt);
      return 0.0;
    }

    // Calculate duration between creation and resolution
    Duration duration = Duration.between(createdAt, resolvedAt);
    double days = duration.toMillis() / (1000.0 * 60 * 60 * 24);

    log.debug(
        "Comment group id={} resolution time: {} days (created: {}, resolved: {})",
        commentGroup.getId(),
        days,
        createdAt,
        resolvedAt);

    return days;
  }

  @Override
  public Mono<ResponseEntity<Api<CommentGroupStatsResponse>>> getCommentGroupStats() {
    return getNonSubmittedBrds()
        .flatMap(
            brds -> {
              if (brds.isEmpty()) {
                return createEmptyCommentGroupStatsResponse();
              }

              List<String> brdFormIds =
                  brds.stream()
                      .map(BRD::getBrdFormId)
                      .filter(id -> id != null && !id.isEmpty())
                      .toList();

              if (brdFormIds.isEmpty()) {
                return createEmptyCommentGroupStatsResponse();
              }

              Query query = Query.query(Criteria.where("brdFormId").in(brdFormIds));

              // Only select fields we need
              query.fields().include("status").include("comments.userType");

              return mongoTemplate
                  .find(query, BrdFieldCommentGroup.class)
                  .collectList()
                  .map(this::calculateCommentGroupStats)
                  .map(
                      stats ->
                          ResponseEntity.ok(
                              new Api<>(
                                  DashboardConstants.SUCCESSFUL,
                                  "Comment group statistics retrieved successfully",
                                  Optional.of(stats),
                                  Optional.empty())));
            });
  }

  private Mono<ResponseEntity<Api<CommentGroupStatsResponse>>>
      createEmptyCommentGroupStatsResponse() {
    CommentGroupStatsResponse emptyStats =
        CommentGroupStatsResponse.builder()
            .totalCommentGroups(0)
            .resolvedCommentGroups(0)
            .pendingCommentStats(
                CommentGroupStatsResponse.PendingCommentStats.builder()
                    .totalPendingGroups(0)
                    .groupsWithPmComment(0)
                    .groupsWithoutPmComment(0)
                    .build())
            .build();

    return Mono.just(
        ResponseEntity.ok(
            new Api<>(
                DashboardConstants.SUCCESSFUL,
                "Comment group statistics retrieved successfully",
                Optional.of(emptyStats),
                Optional.empty())));
  }

  private CommentGroupStatsResponse calculateCommentGroupStats(
      List<BrdFieldCommentGroup> commentGroups) {
    int totalGroups = commentGroups.size();

    // Use efficient stream operations with minimal object creation
    Map<String, List<BrdFieldCommentGroup>> groupsByStatus =
        commentGroups.stream()
            .collect(
                Collectors.groupingBy(
                    group -> group.getStatus() != null ? group.getStatus() : "",
                    HashMap::new,
                    Collectors.toList()));

    int resolvedGroups = groupsByStatus.getOrDefault("Resolved", Collections.emptyList()).size();

    List<BrdFieldCommentGroup> pendingGroups =
        groupsByStatus.getOrDefault("Pending", Collections.emptyList());

    int totalPendingGroups = pendingGroups.size();

    // Count PM comments in a single pass - ensure we're checking for "ROLE_PM" in userType field
    Map<Boolean, Long> pmCommentCounts =
        pendingGroups.stream()
            .collect(
                Collectors.partitioningBy(
                    group ->
                        group.getComments().stream()
                            .anyMatch(comment -> "pm".equals(comment.getUserType())),
                    Collectors.counting()));

    return CommentGroupStatsResponse.builder()
        .totalCommentGroups(totalGroups)
        .resolvedCommentGroups(resolvedGroups)
        .pendingCommentStats(
            CommentGroupStatsResponse.PendingCommentStats.builder()
                .totalPendingGroups(totalPendingGroups)
                .groupsWithPmComment(pmCommentCounts.getOrDefault(true, 0L).intValue())
                .groupsWithoutPmComment(pmCommentCounts.getOrDefault(false, 0L).intValue())
                .build())
        .build();
  }
}
