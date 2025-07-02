package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBARequest;
import com.aci.smart_onboarding.dto.AssignBAResponse;
import com.aci.smart_onboarding.dto.BAReassignmentRequest;
import com.aci.smart_onboarding.exception.BAAssignmentException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BAAssignment;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BAAssignmentRepository;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.service.IBAAssignmentService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IEmailService;
import com.aci.smart_onboarding.service.IUserService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class BAAssignmentService implements IBAAssignmentService {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_RETRIES = 3;
  private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);
  private static final Duration MAX_BACKOFF = Duration.ofSeconds(1);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

  private final BAAssignmentRepository baAssignmentRepository;
  private final IBRDService brdService;
  private final BRDRepository brdRepository;
  private final IEmailService emailService;
  private final IUserService userService;

  public BAAssignmentService(
      BAAssignmentRepository baAssignmentRepository,
      IBRDService brdService,
      BRDRepository brdRepository,
      IEmailService emailService,
      IUserService userService) {
    this.baAssignmentRepository = baAssignmentRepository;
    this.brdService = brdService;
    this.brdRepository = brdRepository;
    this.emailService = emailService;
    this.userService = userService;
    log.info(
        "BAAssignmentServiceImpl initialized with EmailService: {}",
        emailService != null ? "present" : "null");
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public Mono<AssignBAResponse> assignBA(String brdId, AssignBARequest request) {
    log.info("Starting BA assignment process - BRD: {}, BA: {}", brdId, request.getBaEmail());

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
                            "Retrying BA assignment after error: {}",
                            signal.failure().getMessage())))
        .onErrorMap(
            e -> {
              if (e.getMessage() != null
                  && (e.getMessage().contains("authorization grant is invalid")
                      || e.getMessage().contains("Authenticated user is not authorized"))) {
                return new BAAssignmentException(
                    "Failed to send email notification. Please check SendGrid configuration: "
                        + e.getMessage());
              }
              return e;
            })
        .doOnSuccess(
            response ->
                log.info("Successfully assigned BA {} to BRD {}", request.getBaEmail(), brdId))
        .doOnError(
            error -> {
              if (error instanceof BAAssignmentException) {
                log.warn("BA assignment error: {}", error.getMessage());
              } else {
                log.error(
                    "Failed to assign BA {} to BRD {}: {}",
                    request.getBaEmail(),
                    brdId,
                    error.getMessage());
              }
            });
  }

  private Mono<Void> validateRequest(AssignBARequest request) {
    if (request == null) {
      return Mono.error(new BadRequestException("Request cannot be null"));
    }

    if (request.getBaEmail() == null || request.getBaEmail().trim().isEmpty()) {
      return Mono.error(new BadRequestException("BA email cannot be empty"));
    }

    if (!EMAIL_PATTERN.matcher(request.getBaEmail()).matches()) {
      return Mono.error(
          new BadRequestException(
              String.format(
                  "Invalid BA email format: %s. Please provide a valid email address.",
                  request.getBaEmail())));
    }

    if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
      return Mono.error(new BadRequestException("Status cannot be empty"));
    }

    return Mono.empty();
  }

  private Mono<BRD> validateBrdExists(String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(Mono.error(new NotFoundException("BRD not found with id: " + brdId)));
  }

  private Mono<BAAssignment> validateAndCreateAssignment(String brdId, AssignBARequest request) {
    return baAssignmentRepository
        .existsByBrdId(brdId)
        .timeout(Duration.ofSeconds(5))
        .flatMap(
            brdHasBA -> {
              if (Boolean.TRUE.equals(brdHasBA)) {
                return baAssignmentRepository
                    .findByBrdId(brdId)
                    .timeout(Duration.ofSeconds(5))
                    .switchIfEmpty(
                        Mono.error(
                            new BAAssignmentException(
                                "Error retrieving BA assignment for BRD: " + brdId)))
                    .flatMap(
                        existingAssignment -> {
                          if (existingAssignment != null
                              && !existingAssignment.getBaEmail().equals(request.getBaEmail())) {
                            log.warn(
                                "BRD {} already has BA {} assigned",
                                brdId,
                                existingAssignment.getBaEmail());
                            return Mono.error(
                                new BAAssignmentException(
                                    String.format(
                                        "Cannot assign BA to BRD %s. This BRD already has BA %s assigned. "
                                            + "Please unassign the current BA before assigning a new one.",
                                        brdId, existingAssignment.getBaEmail())));
                          }
                          // If the same BA is already assigned, just return the existing assignment
                          return Mono.just(existingAssignment);
                        });
              }
              // If no BA is assigned, create a new assignment
              return createNewAssignment(brdId, request);
            })
        .onErrorResume(
            e -> {
              if (e instanceof BAAssignmentException) {
                return Mono.error(e);
              }
              log.error("Error during BA assignment validation: {}", e.getMessage());
              return Mono.error(
                  new BAAssignmentException("Error validating BA assignment: " + e.getMessage()));
            });
  }

  private Mono<BAAssignment> createNewAssignment(String brdId, AssignBARequest request) {
    return Mono.just(
        BAAssignment.builder()
            .brdId(brdId)
            .baEmail(request.getBaEmail())
            .description(request.getDescription())
            .assignedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  private Mono<BAAssignment> saveAssignmentAndUpdateBrd(BAAssignment assignment, String status) {
    return baAssignmentRepository
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
                          Mono<Void> emailNotification = Mono.empty();
                          if (BrdConstants.STATUS_INTERNAL_REVIEW.equals(status)) {
                            emailNotification =
                                emailService.sendBrdStatusChangeNotification(
                                    assignment.getBaEmail(),
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

  private AssignBAResponse createResponse(BAAssignment assignment, String status) {
    return AssignBAResponse.builder()
        .brdId(assignment.getBrdId())
        .status(status)
        .baEmail(assignment.getBaEmail())
        .description(assignment.getDescription())
        .build();
  }

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
        && !(throwable instanceof BAAssignmentException);
  }

  @Override
  public Mono<List<String>> getAllBaEmails() {
    log.info("Fetching all BA email addresses");
    return baAssignmentRepository
        .findAll()
        .map(BAAssignment::getBaEmail)
        .distinct()
        .collectList()
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error fetching BA emails: {}", e.getMessage());
              return new BAAssignmentException("Failed to fetch BA emails: " + e.getMessage());
            });
  }

  @Override
  public Mono<BAAssignment> updateBaEmail(String brdId, String baEmail) {
    log.info("Updating BA email for BRD: {}", brdId);

    return baAssignmentRepository
        .findByBrdId(brdId)
        .flatMap(
            existingAssignment -> {
              // Update only updatedAt for existing record
              existingAssignment.setBaEmail(baEmail);
              existingAssignment.setUpdatedAt(LocalDateTime.now());
              return baAssignmentRepository.save(existingAssignment);
            })
        .switchIfEmpty(
            // Create new record if none exists
            Mono.defer(
                () -> {
                  BAAssignment newAssignment = new BAAssignment();
                  newAssignment.setBrdId(brdId);
                  newAssignment.setBaEmail(baEmail);
                  newAssignment.setAssignedAt(LocalDateTime.now());
                  newAssignment.setUpdatedAt(LocalDateTime.now());
                  return baAssignmentRepository.save(newAssignment);
                }))
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error updating BA email: {}", e.getMessage());
              return new BAAssignmentException("Failed to update BA email: " + e.getMessage());
            });
  }

  @Override
  public Mono<List<BAAssignment>> getAssignmentsByBaUsername(String username) {
    log.info("Fetching assignments for BA: {}", username);

    if (username == null || username.trim().isEmpty()) {
      return Mono.error(new BadRequestException("Username cannot be empty"));
    }

    if (!EMAIL_PATTERN.matcher(username).matches()) {
      return Mono.error(
          new BadRequestException(
              String.format(
                  "Invalid email format: %s. Please provide a valid email address.", username)));
    }

    return baAssignmentRepository
        .findByBaEmail(username)
        .collectList()
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error fetching assignments for BA {}: {}", username, e.getMessage());
              return new BAAssignmentException(
                  String.format(
                      "Failed to fetch assignments for BA %s: %s", username, e.getMessage()));
            });
  }

  @Override
  public Mono<Api<List<String>>> reassignBAs(List<BAReassignmentRequest> requests) {
    log.info("Processing batch BA reassignment for {} BRDs", requests.size());

    return Flux.fromIterable(requests)
        .concatMap(
            request ->
                userService
                    .getUserByEmail(request.getNewBaUsername())
                    .flatMap(
                        userResponse -> {
                          if (!SecurityConstants.ROLE_BA.equals(
                              Objects.requireNonNull(userResponse.getBody())
                                  .getData()
                                  .get()
                                  .getActiveRole())) {
                            log.warn("User {} is not a BA", request.getNewBaUsername());
                            return Mono.just(
                                String.format(
                                    "Failed to reassign BRD %s: User %s is not a BA",
                                    request.getBrdId(), request.getNewBaUsername()));
                          }

                          return baAssignmentRepository
                              .findByBrdId(request.getBrdId())
                              .flatMap(
                                  assignment -> {
                                    assignment.setBaEmail(request.getNewBaUsername());
                                    assignment.setUpdatedAt(LocalDateTime.now());
                                    return baAssignmentRepository.save(assignment).thenReturn("");
                                  })
                              .switchIfEmpty(
                                  Mono.just(
                                      String.format(
                                          "Failed to reassign BRD %s: BRD not found",
                                          request.getBrdId())));
                        })
                    .onErrorResume(
                        e -> {
                          log.error(
                              "Error reassigning BA for BRD {}: {}",
                              request.getBrdId(),
                              e.getMessage());
                          return Mono.just(
                              String.format(
                                  "Failed to reassign BRD %s: %s",
                                  request.getBrdId(), e.getMessage()));
                        }))
        .collectList()
        .map(
            results -> {
              List<String> errors = results.stream().filter(result -> !result.isEmpty()).toList();

              log.info("Found {} errors during batch reassignment", errors.size());
              if (!errors.isEmpty()) {
                Map<String, String> errorMap = new HashMap<>();
                for (int i = 0; i < errors.size(); i++) {
                  String error = errors.get(i);
                  String key = "error" + (i + 1);
                  log.info("Adding error with key {}: {}", key, error);
                  errorMap.put(key, error);
                }

                return new Api<>(
                    BrdConstants.FAILURE,
                    "Some reassignments failed",
                    Optional.empty(),
                    Optional.of(errorMap));
              }

              return new Api<>(
                  BrdConstants.SUCCESSFUL,
                  "All BAs reassigned successfully",
                  Optional.empty(),
                  Optional.empty());
            });
  }

  @Override
  public Mono<Boolean> isBAAssignedToUser(String brdId, String baEmail) {
    log.info("Checking if BA {} is assigned to BRD: {}", baEmail, brdId);
    
    if (brdId == null || brdId.trim().isEmpty()) {
      return Mono.error(new BadRequestException("BRD ID cannot be empty"));
    }
    
    if (baEmail == null || baEmail.trim().isEmpty()) {
      return Mono.error(new BadRequestException("BA email cannot be empty"));
    }
    
    return baAssignmentRepository
        .findByBrdId(brdId)
        .map(assignment -> baEmail.equals(assignment.getBaEmail()))
        .defaultIfEmpty(false)
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(
            e -> {
              log.error("Error checking BA assignment for BRD {} and BA {}: {}", brdId, baEmail, e.getMessage());
              return new BAAssignmentException(
                  String.format("Failed to check BA assignment for BRD %s and BA %s: %s", brdId, baEmail, e.getMessage()));
            });
  }
}
