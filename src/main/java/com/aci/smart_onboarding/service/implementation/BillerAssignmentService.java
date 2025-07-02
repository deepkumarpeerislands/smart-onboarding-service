package com.aci.smart_onboarding.service.implementation;

import static com.aci.smart_onboarding.constants.SecurityConstants.BILLER_ROLE;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBillerRequest;
import com.aci.smart_onboarding.dto.AssignBillerResponse;
import com.aci.smart_onboarding.dto.AuthorizationResponse;
import com.aci.smart_onboarding.dto.BRDCountDataResponse;
import com.aci.smart_onboarding.dto.BRDListResponse;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.BillerAssignmentException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BillerAssignment;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.BillerAssignmentRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBillerAssignmentService;
import com.aci.smart_onboarding.service.IEmailService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service implementation for managing biller assignments to BRDs. Handles the assignment of billers
 * to BRDs and updates the BRD status accordingly.
 *
 * <p>This service ensures: 1. No duplicate biller assignments to the same BRD 2. Atomic operations
 * for both biller assignment and BRD status update 3. Proper error handling and logging 4.
 * Transactional integrity 5. Audit logging for all operations 6. Validation of all inputs and state
 * transitions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillerAssignmentService implements IBillerAssignmentService {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_RETRIES = 3;
  private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);
  private static final Duration MAX_BACKOFF = Duration.ofSeconds(1);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

  private final BillerAssignmentRepository billerAssignmentRepository;
  private final IBRDService brdService;
  private final BRDRepository brdRepository;
  private final IEmailService emailService;
  private final BRDSecurityService brdSecurityService;
  private final DtoModelMapper dtoModelMapper;

  /**
   * Assigns a biller to a BRD and updates the BRD status. This operation is transactional and will
   * ensure both the biller assignment and BRD status update succeed or fail together.
   *
   * @param brdId The ID of the BRD to assign the biller to
   * @param request The assignment request containing biller details and status
   * @return A Mono emitting the assignment response
   * @throws IllegalArgumentException if the biller is already assigned to the BRD
   * @throws DuplicateKeyException if there's a concurrent duplicate assignment
   * @throws NotFoundException if the BRD doesn't exist
   * @throws BadRequestException if the request is invalid
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public Mono<AssignBillerResponse> assignBiller(String brdId, AssignBillerRequest request) {
    log.info(
        "Starting biller assignment process - BRD: {}, Biller: {}",
        brdId,
        request.getBillerEmail());

    return validateRequest(request)
        .then(validateBrdExists(brdId))
        .then(validateAndCreateAssignment(brdId, request))
        .flatMap(assignment -> saveAssignmentAndUpdateBrd(assignment, request.getStatus()))
        .map(assignment -> createResponse(assignment, request.getStatus()))
        .timeout(TIMEOUT)
        .retryWhen(
            Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .filter(this::shouldRetry)
                .doBeforeRetry(
                    signal ->
                        log.warn(
                            "Retrying biller assignment after error: {}",
                            signal.failure().getMessage())))
        .onErrorMap(
            e -> {
              if (e.getMessage() != null
                  && (e.getMessage().contains("authorization grant is invalid")
                      || e.getMessage().contains("Authenticated user is not authorized"))) {
                return new BillerAssignmentException(
                    "Failed to send email notification. Please check SendGrid configuration: "
                        + e.getMessage());
              }
              return e;
            })
        .doOnSuccess(
            response ->
                log.info(
                    "Successfully assigned biller {} to BRD {}", request.getBillerEmail(), brdId))
        .doOnError(
            error -> {
              if (error instanceof BillerAssignmentException) {
                log.warn("Biller assignment error: {}", error.getMessage());
              } else {
                log.error(
                    "Failed to assign biller {} to BRD {}: {}",
                    request.getBillerEmail(),
                    brdId,
                    error.getMessage());
              }
            });
  }

  /** Validates the request parameters. */
  private Mono<Void> validateRequest(AssignBillerRequest request) {
    if (request == null) {
      return Mono.error(new BadRequestException("Request cannot be null"));
    }

    if (request.getBillerEmail() == null || request.getBillerEmail().trim().isEmpty()) {
      return Mono.error(new BadRequestException("Biller email cannot be empty"));
    }

    if (!EMAIL_PATTERN.matcher(request.getBillerEmail()).matches()) {
      return Mono.error(
          new BadRequestException(
              String.format(
                  "Invalid biller email format: %s. Please provide a valid email address.",
                  request.getBillerEmail())));
    }

    if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
      return Mono.error(new BadRequestException("Status cannot be empty"));
    }

    return Mono.empty();
  }

  /** Validates that the BRD exists. */
  private Mono<BRD> validateBrdExists(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new NotFoundException("BRD not found with id: " + brdId)));
  }

  /** Validates the assignment request and creates a new BillerAssignment entity. */
  private Mono<BillerAssignment> validateAndCreateAssignment(
      String brdId, AssignBillerRequest request) {
    return billerAssignmentRepository
        .existsByBrdId(brdId)
        .timeout(Duration.ofSeconds(5))
        .flatMap(
            brdHasBiller -> {
              if (Boolean.TRUE.equals(brdHasBiller)) {
                return billerAssignmentRepository
                    .findByBrdId(brdId)
                    .timeout(Duration.ofSeconds(5))
                    .switchIfEmpty(
                        Mono.error(
                            new BillerAssignmentException(
                                "Error retrieving biller assignment for BRD: " + brdId)))
                    .flatMap(
                        existingAssignment -> {
                          if (existingAssignment != null
                              && !existingAssignment
                                  .getBillerEmail()
                                  .equals(request.getBillerEmail())) {
                            log.warn(
                                "BRD {} already has biller {} assigned",
                                brdId,
                                existingAssignment.getBillerEmail());
                            return Mono.error(
                                new BillerAssignmentException(
                                    String.format(
                                        "Cannot assign biller to BRD %s. This BRD already has biller %s assigned. "
                                            + "Please unassign the current biller before assigning a new one.",
                                        brdId, existingAssignment.getBillerEmail())));
                          }
                          // If the same biller is already assigned, update the description and
                          // updatedAt
                          // while preserving the original assignedAt
                          existingAssignment.setDescription(request.getDescription());
                          existingAssignment.setUpdatedAt(LocalDateTime.now());
                          return Mono.just(existingAssignment);
                        });
              }
              // If no biller is assigned, create a new assignment
              return createNewAssignment(brdId, request);
            })
        .onErrorResume(
            e -> {
              if (e instanceof BillerAssignmentException) {
                return Mono.error(e);
              }
              log.error("Error during biller assignment validation: {}", e.getMessage());
              return Mono.error(
                  new BillerAssignmentException(
                      "Error validating biller assignment: " + e.getMessage()));
            });
  }

  private Mono<BillerAssignment> createNewAssignment(String brdId, AssignBillerRequest request) {
    return Mono.just(
        BillerAssignment.builder()
            .brdId(brdId)
            .billerEmail(request.getBillerEmail())
            .description(request.getDescription())
            .assignedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  /**
   * Saves the biller assignment and updates the BRD status atomically. Also sends appropriate email
   * notifications based on the status change.
   *
   * @param assignment The biller assignment to save
   * @param status The new status to set for the BRD
   * @return A Mono emitting the saved assignment
   * @throws NotFoundException if the BRD is not found
   */
  private Mono<BillerAssignment> saveAssignmentAndUpdateBrd(
      BillerAssignment assignment, String status) {
    return billerAssignmentRepository
        .save(assignment)
        .flatMap(
            savedAssignment ->
                brdRepository
                    .findByBrdId(assignment.getBrdId())
                    .flatMap(
                        brd -> {
                          Mono<Void> statusUpdate =
                              brdService
                                  .updateBrdStatus(
                                      brd.getBrdFormId(), status, assignment.getDescription())
                                  .then();

                          // Send appropriate email notifications based on status
                          Mono<Void> emailNotification;
                          if (BrdConstants.STATUS_IN_PROGRESS.equals(status)) {
                            emailNotification =
                                emailService.sendBrdStatusChangeNotification(
                                    assignment.getBillerEmail(),
                                    brd.getBrdId(),
                                    brd.getBrdName(),
                                    brd.getBrdFormId());
                          } else {
                            emailNotification =
                                emailService.sendBillerWelcomeEmail(
                                    assignment.getBillerEmail(),
                                    brd.getBrdId(),
                                    brd.getBrdName(),
                                    brd.getBrdFormId());
                          }

                          // Complete both status update and email notification
                          // If email fails, the error will be propagated
                          return statusUpdate.then(emailNotification).thenReturn(savedAssignment);
                        })
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(
                                "BRD not found with id: " + assignment.getBrdId()))));
  }

  /** Creates the response DTO from the saved assignment. */
  private AssignBillerResponse createResponse(BillerAssignment assignment, String status) {
    return AssignBillerResponse.builder()
        .brdId(assignment.getBrdId())
        .status(status)
        .billerEmail(assignment.getBillerEmail())
        .description(assignment.getDescription())
        .build();
  }

  /**
   * Determines if an error should trigger a retry attempt. Authentication errors and invalid API
   * keys should not be retried as they won't be resolved by retrying.
   */
  private boolean shouldRetry(Throwable throwable) {
    // Don't retry for authentication errors
    if (throwable.getMessage() != null
        && (throwable.getMessage().contains("authorization grant is invalid")
            || throwable.getMessage().contains("Authenticated user is not authorized"))) {
      return false;
    }

    // Don't retry for business logic errors
    return !(throwable instanceof IllegalArgumentException)
        && !(throwable instanceof DuplicateKeyException)
        && !(throwable instanceof NotFoundException)
        && !(throwable instanceof BadRequestException)
        && !(throwable instanceof BillerAssignmentException);
  }

  /**
   * Retrieves biller details for a specific BRD.
   *
   * @param brdId The ID of the BRD to get biller details for
   * @return A Mono emitting a Map containing the biller details (brdId and billerEmail)
   * @throws NotFoundException if the BRD or biller assignment doesn't exist
   */
  @Override
  public Mono<Map<String, String>> getBillerDetails(String brdId) {
    log.info("Fetching biller details for BRD: {}", brdId);

    return validateBrdExists(brdId)
        .then(
            billerAssignmentRepository
                .findByBrdId(brdId)
                .switchIfEmpty(
                    Mono.error(
                        new NotFoundException(
                            String.format("No biller assigned to BRD with id: %s", brdId))))
                .map(
                    billerAssignment -> {
                      Map<String, String> response = new HashMap<>();
                      response.put("brdId", brdId);
                      response.put("billerEmail", billerAssignment.getBillerEmail());
                      return response;
                    }))
        .timeout(TIMEOUT)
        .doOnSuccess(
            response -> log.info("Successfully retrieved biller details for BRD: {}", brdId))
        .doOnError(
            error -> {
              if (error instanceof NotFoundException) {
                log.warn("Biller details retrieval error: {}", error.getMessage());
              } else {
                log.error(
                    "Failed to retrieve biller details for BRD {}: {}", brdId, error.getMessage());
              }
            });
  }

  /**
   * Retrieves all unique biller email addresses from the biller assignments.
   *
   * @return A Mono emitting a list of unique biller email addresses
   */
  @Override
  public Mono<List<String>> getAllBillerEmails() {
    log.info("Fetching all Biller email addresses");
    return billerAssignmentRepository
        .findAll()
        .map(BillerAssignment::getBillerEmail)
        .distinct()
        .collectList()
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error fetching Biller emails: {}", e.getMessage());
              return new BillerAssignmentException(
                  "Failed to fetch Biller emails: " + e.getMessage());
            });
  }

  @Override
  public Mono<BillerAssignment> updateBillerEmail(String brdId, String billerEmail) {
    log.info("Updating biller email for BRD: {}", brdId);

    return billerAssignmentRepository
        .findByBrdId(brdId)
        .flatMap(
            existingAssignment -> {
              // Update only updatedAt for existing record
              existingAssignment.setBillerEmail(billerEmail);
              existingAssignment.setUpdatedAt(LocalDateTime.now());
              return billerAssignmentRepository.save(existingAssignment);
            })
        .switchIfEmpty(
            // Create new record if none exists
            Mono.defer(
                () -> {
                  BillerAssignment newAssignment = new BillerAssignment();
                  newAssignment.setBrdId(brdId);
                  newAssignment.setBillerEmail(billerEmail);
                  newAssignment.setAssignedAt(LocalDateTime.now());
                  newAssignment.setUpdatedAt(LocalDateTime.now());
                  return billerAssignmentRepository.save(newAssignment);
                }))
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error updating biller email: {}", e.getMessage());
              return new BillerAssignmentException(
                  "Failed to update biller email: " + e.getMessage());
            });
  }

  @Override
  public Mono<Boolean> isBrdAssignedToBiller(String brdId, String billerEmail) {
    log.info("Checking if BRD {} is assigned to biller {}", brdId, billerEmail);
    return billerAssignmentRepository
        .existsByBrdIdAndBillerEmail(brdId, billerEmail)
        .doOnNext(
            isAssigned ->
                log.info("BRD {} assigned to biller {}: {}", brdId, billerEmail, isAssigned))
        .doOnError(error -> log.error("Error checking BRD assignment: {}", error.getMessage()));
  }

  @Override
  public Mono<Api<AuthorizationResponse>> checkBrdAuthorization(String brdId) {
    log.info("Checking authorization for BRD: {}", brdId);

    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new NotFoundException(BrdConstants.BRD_NOT_FOUND + brdId)))
        .flatMap(this::checkUserRoleAndAccess)
        .onErrorResume(this::handleAuthorizationError);
  }

  private Mono<Api<AuthorizationResponse>> checkUserRoleAndAccess(BRD brd) {
    return brdSecurityService
        .getCurrentUserRole()
        .doOnNext(role -> log.info("Current user role: {}", role))
        .flatMap(role -> handleUserRole(role, brd));
  }

  private Mono<Api<AuthorizationResponse>> handleUserRole(String role, BRD brd) {
    if (!BILLER_ROLE.equals(role)) {
      log.warn("User with role {} attempted to access biller endpoint", role);
      return createUnauthorizedResponse("Only Billers can access BRDs through this endpoint");
    }

    log.info("User has biller role, checking email authorization");
    return checkBillerEmailAuthorization(brd);
  }

  private Mono<Api<AuthorizationResponse>> checkBillerEmailAuthorization(BRD brd) {
    return brdSecurityService
        .getCurrentUserEmail()
        .doOnNext(email -> log.info("Current user email: {}", email))
        .flatMap(userEmail -> checkBrdAssignment(brd, userEmail));
  }

  private Mono<Api<AuthorizationResponse>> checkBrdAssignment(BRD brd, String userEmail) {
    return isBrdAssignedToBiller(brd.getBrdId(), userEmail)
        .doOnNext(isAssigned -> log.info("BRD assignment check result: {}", isAssigned))
        .flatMap(isAssigned -> handleBrdAssignment(brd, userEmail, isAssigned));
  }

  private Mono<Api<AuthorizationResponse>> handleBrdAssignment(
      BRD brd, String userEmail, Boolean isAssigned) {
    if (Boolean.FALSE.equals(isAssigned)) {
      log.warn("BRD {} is not assigned to user {}", brd.getBrdId(), userEmail);
      return createUnauthorizedResponse("This BRD is not assigned to you");
    }

    log.info(
        "BRD {} is assigned to user {}, checking status authorization", brd.getBrdId(), userEmail);
    return checkStatusAuthorization(brd, userEmail);
  }

  private Mono<Api<AuthorizationResponse>> checkStatusAuthorization(BRD brd, String userEmail) {
    return brdSecurityService
        .withSecurityCheck(brd.getStatus())
        .then(createAuthorizedResponse())
        .onErrorResume(e -> handleStatusAuthorizationError(e, brd, userEmail));
  }

  private Mono<Api<AuthorizationResponse>> handleStatusAuthorizationError(
      Throwable error, BRD brd, String userEmail) {
    if (error instanceof AccessDeniedException) {
      log.warn(
          "User {} not authorized to view BRD {} in status {}",
          userEmail,
          brd.getBrdId(),
          brd.getStatus());
      return createUnauthorizedResponse(
          "You are not authorized to view this BRD in its current status");
    }
    return Mono.error(error);
  }

  private Mono<Api<AuthorizationResponse>> handleAuthorizationError(Throwable error) {
    if (error instanceof NotFoundException) {
      log.warn("BRD not found: {}", error.getMessage());
      return createUnauthorizedResponse(error.getMessage());
    }

    log.error("Error checking authorization: {}", error.getMessage());
    return createUnauthorizedResponse("An error occurred while checking authorization");
  }

  private Mono<Api<AuthorizationResponse>> createUnauthorizedResponse(String message) {
    AuthorizationResponse response =
        AuthorizationResponse.builder().authorized(false).message(message).build();

    return Mono.just(
        new Api<>(BrdConstants.FAILURE, message, Optional.of(response), Optional.empty()));
  }

  private Mono<Api<AuthorizationResponse>> createAuthorizedResponse() {
    AuthorizationResponse authResponse = new AuthorizationResponse();
    authResponse.setAuthorized(true);
    authResponse.setMessage("User is authorized to access this BRD");

    return Mono.just(
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "Authorization check successful",
            Optional.of(authResponse),
            Optional.empty()));
  }

  @Override
  public Mono<List<String>> getBrdsByBillerEmail(String billerEmail) {
    log.info("Getting BRDs for biller email: {}", billerEmail);
    
    if (billerEmail == null || billerEmail.trim().isEmpty()) {
      return Mono.error(new BadRequestException("Biller email cannot be empty"));
    }
    
    if (!EMAIL_PATTERN.matcher(billerEmail).matches()) {
      return Mono.error(
          new BadRequestException(
              String.format(
                  "Invalid biller email format: %s. Please provide a valid email address.",
                  billerEmail)));
    }
    
    return billerAssignmentRepository
        .findByBillerEmail(billerEmail)
        .collectList()
        .map(assignments -> 
            assignments.stream()
                .map(BillerAssignment::getBrdId)
                .toList())
        .doOnSuccess(brdIds -> 
            log.info("Found {} BRDs assigned to biller email: {}", brdIds.size(), billerEmail))
        .doOnError(error -> 
            log.error("Error getting BRDs for biller email {}: {}", billerEmail, error.getMessage()));
  }

  @Override
  public Mono<BRDCountDataResponse> getBrdsByCurrentBillerEmail() {
    log.info("Getting BRDs for current biller email");
    
    return brdSecurityService.getCurrentUserEmail()
        .flatMap(billerEmail -> {
          log.info("Getting BRDs for biller email: {}", billerEmail);
          return getBrdsByBillerEmail(billerEmail);
        })
        .flatMap(brdIds -> {
          if (brdIds.isEmpty()) {
            return Mono.just(new BRDCountDataResponse(0, Collections.emptyList()));
          }
          
          // Get BRD form IDs from BRD collection using the BRD IDs
          return brdRepository.findAllByBrdIdIn(brdIds)
              .collectList()
              .map(brds -> {
                if (brds.isEmpty()) {
                  return new BRDCountDataResponse(0, Collections.emptyList());
                }
                
                // Convert BRDs to BRDListResponse format
                List<BRDListResponse> brdListResponses = brds.stream()
                    .map(dtoModelMapper::mapToBrdListResponse)
                    .toList();
                
                return new BRDCountDataResponse(brdListResponses.size(), brdListResponses);
              });
        })
        .flatMap(brdCountDataResponse -> {
          // Apply security filtering like in getBrdList
          List<BRDListResponse> brdList = brdCountDataResponse.getBrdList();
          if (brdList.isEmpty()) {
            return Mono.just(brdCountDataResponse);
          }
          
          return Flux.fromIterable(brdList)
              .filterWhen(
                  brd ->
                      brdSecurityService
                          .withSecurityCheck(brd.getStatus())
                          .thenReturn(true)
                          .onErrorResume(e -> Mono.just(false)))
              .collectList()
              .map(filteredBrds -> new BRDCountDataResponse(filteredBrds.size(), filteredBrds));
        })
        .doOnSuccess(response -> 
            log.info("Retrieved {} BRDs for current biller", response.getBrdList().size()))
        .doOnError(error -> 
            log.error("Error getting BRDs for current biller: {}", error.getMessage()));
  }
}
