package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.*;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.UserDetailResponse;
import com.aci.smart_onboarding.dto.UserListResponse;
import com.aci.smart_onboarding.dto.UserProjection;
import com.aci.smart_onboarding.dto.UserRequest;
import com.aci.smart_onboarding.dto.UserResponse;
import com.aci.smart_onboarding.enums.UserStatus;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.BAAssignmentRepository;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IEmailService;
import com.aci.smart_onboarding.service.IUserService;
import com.mongodb.DuplicateKeyException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final BRDRepository brdRepository;
  private final BAAssignmentRepository baAssignmentRepository;
  private final IEmailService emailService;
  private final IAuditLogService auditLogService;

  private static final String EMAIL_NOT_FOUND = "User not found with email: ";
  private static final String PM_ROLE = SecurityConstants.PM_ROLE;
  private static final String USERS_RETRIEVED = "Users retrieved successfully";
  private static final String USERS_SEARCHED = "Users searched successfully";
  private static final String USER_CREATED = "User created successfully";
  private static final String USER_DETAILS_RETRIEVED = "User details retrieved successfully";
  private static final String USER_NOT_FOUND_WITH_ID = "User not found with id: ";
  private static final String ENTITY_TYPE_USER = "USER";
  private static final String ACTION_ADD_ROLE = "ADD_ROLE";
  private static final String ACTION_REMOVE_ROLE = "REMOVE_ROLE";

  /**
   * Validates if a user has the specified role by checking both activeRole and roles list.
   * Handles null values safely and supports both prefixed and non-prefixed role formats.
   *
   * @param user The user to validate
   * @param role The role to check for (can be with or without ROLE_ prefix)
   * @return true if the user has the specified role, false otherwise
   */
  private boolean hasRole(User user, String role) {
    if (user == null || role == null) {
      return false;
    }

    String userRole = user.getActiveRole();
    List<String> userRoles = user.getRoles();

    // Remove ROLE_ prefix for comparison if present
    String normalizedRole = role.replace(SecurityConstants.ROLE_PREFIX, "");
    String normalizedUserRole = userRole != null ? userRole.replace(SecurityConstants.ROLE_PREFIX, "") : null;

    // Check activeRole
    boolean hasInActiveRole = normalizedUserRole != null && normalizedUserRole.equals(normalizedRole);

    // Check roles list
    boolean hasInRolesList = userRoles != null && userRoles.stream()
        .anyMatch(r -> r != null && r.replace(SecurityConstants.ROLE_PREFIX, "").equals(normalizedRole));

    return hasInActiveRole || hasInRolesList;
  }

  /**
   * Validates if a user has PM role by checking both activeRole and roles list.
   *
   * @param user The user to validate
   * @return true if the user has PM role, false otherwise
   */
  private boolean isUserPM(User user) {
    return hasRole(user, UserConstants.PM_ROLE);
  }

  /**
   * Validates if a user has BA role by checking both activeRole and roles list.
   *
   * @param user The user to validate
   * @return true if the user has BA role, false otherwise
   */
  private boolean isUserBA(User user) {
    return hasRole(user, UserConstants.BA_ROLE);
  }

  /**
   * Checks if a UserInfo has PM role by checking both activeRole and roles list.
   *
   * @param userInfo The UserInfo to validate
   * @return true if the user has PM role, false otherwise
   */
  private boolean hasPMRole(UserListResponse.UserInfo userInfo) {
    if (userInfo == null) {
      return false;
    }

    String userRole = userInfo.getActiveRole();
    List<String> userRoles = userInfo.getRoles();

    // Check activeRole
    boolean hasInActiveRole = userRole != null && 
                             UserConstants.PM_ROLE.equals(userRole.replace(SecurityConstants.ROLE_PREFIX, ""));

    // Check roles list
    boolean hasInRolesList = userRoles != null && userRoles.stream()
        .anyMatch(r -> r != null && UserConstants.PM_ROLE.equals(r.replace(SecurityConstants.ROLE_PREFIX, "")));

    return hasInActiveRole || hasInRolesList;
  }

  /**
   * Checks if a UserInfo has BA role by checking both activeRole and roles list.
   *
   * @param userInfo The UserInfo to validate
   * @return true if the user has BA role, false otherwise
   */
  private boolean hasBARole(UserListResponse.UserInfo userInfo) {
    if (userInfo == null) {
      return false;
    }

    String userRole = userInfo.getActiveRole();
    List<String> userRoles = userInfo.getRoles();

    // Check activeRole
    boolean hasInActiveRole = userRole != null && 
                             UserConstants.BA_ROLE.equals(userRole.replace(SecurityConstants.ROLE_PREFIX, ""));

    // Check roles list
    boolean hasInRolesList = userRoles != null && userRoles.stream()
        .anyMatch(r -> r != null && UserConstants.BA_ROLE.equals(r.replace(SecurityConstants.ROLE_PREFIX, "")));

    return hasInActiveRole || hasInRolesList;
  }

  @Override
  public Mono<ResponseEntity<Api<UserResponse>>> createUser(UserRequest userRequest) {
    User user = mapToEntity(userRequest);
    String defaultPassword = userRequest.getFirstName() + "_" + userRequest.getActiveRole();

    return userRepository
        .save(user)
        .map(this::mapToResponse)
        .map(
            userResponse -> {
              // Create the API response
              Api<UserResponse> apiResponse = new Api<>();
              apiResponse.setStatus(UserConstants.SUCCESS);
              apiResponse.setMessage(USER_CREATED);
              apiResponse.setData(Optional.of(userResponse));
              apiResponse.setErrors(Optional.empty());

              ResponseEntity<Api<UserResponse>> responseEntity =
                  ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);

              // Send emails asynchronously in background after response is prepared
              String userEmail = userResponse.getEmail();
              String userName = userResponse.getEmail(); // Using email as username

              log.info("User created successfully, scheduling welcome emails to: {}", userEmail);

              // Send both emails asynchronously without blocking the response
              sendWelcomeEmailsAsync(userEmail, userName, defaultPassword);

              return responseEntity;
            })
        .onErrorMap(this::handleErrors);
  }

  /** Sends welcome emails asynchronously in the background */
  private void sendWelcomeEmailsAsync(String userEmail, String userName, String defaultPassword) {
    // Send both emails asynchronously in parallel using reactive non-blocking approach
    Mono.when(
            emailService
                .sendUserWelcomeEmailWithResetLink(userEmail, userName)
                .doOnSuccess(
                    v ->
                        log.info(
                            "Welcome email with reset link sent successfully to: {}", userEmail))
                .doOnError(
                    e ->
                        log.error(
                            "Failed to send welcome email to {}: {}", userEmail, e.getMessage()))
                .onErrorResume(e -> Mono.empty()),
            emailService
                .sendUserCredentialsEmail(userEmail, defaultPassword)
                .doOnSuccess(v -> log.info("Credentials email sent successfully to: {}", userEmail))
                .doOnError(
                    e ->
                        log.error(
                            "Failed to send credentials email to {}: {}",
                            userEmail,
                            e.getMessage()))
                .onErrorResume(e -> Mono.empty()))
        .subscribeOn(Schedulers.parallel())
        .subscribe(); // Fire and forget
  }

  @Override
  public Mono<ResponseEntity<Api<UserDetailResponse>>> getUserByEmail(String email) {
    // Normalize email to lowercase for consistent lookup
    String normalizedEmail = normalizeEmail(email);
    return userRepository
        .findByEmail(normalizedEmail)
        .switchIfEmpty(Mono.error(new NotFoundException(EMAIL_NOT_FOUND + email)))
        .map(this::mapToDetailResponse)
        .map(
            response -> {
              Api<UserDetailResponse> apiResponse = new Api<>();
              apiResponse.setStatus(UserConstants.SUCCESS);
              apiResponse.setMessage(USER_DETAILS_RETRIEVED);
              apiResponse.setData(Optional.of(response));
              apiResponse.setErrors(Optional.empty());
              return ResponseEntity.ok(apiResponse);
            })
        .onErrorResume(ex -> Mono.error(handleErrors(ex)));
  }

  @Override
  public Flux<UserDetailResponse> getUsersByRole(String role) {
    log.debug("Fetching users with role: {}", role);
    return userRepository
        .findByRole(role)
        .mergeWith(userRepository.findByRoles(role))
        .distinct(User::getId)
        .map(this::mapToDetailResponse)
        .doOnComplete(() -> log.debug("Completed fetching users with role: {}", role));
  }

  @Override
  public Mono<Api<List<PMReassignmentResponse>>> reassignProjectManagers(
      List<PMReassignmentRequest> requests, String modifiedBy, String userRole) {
    log.info("Processing batch PM reassignment for {} BRDs by user: {}", requests.size(), modifiedBy);

    return Flux.fromIterable(requests)
        .concatMap(request -> processSingleReassignment(request, modifiedBy, userRole))
        .collectList()
        .map(this::buildBatchResponse);
  }

  private Mono<PMReassignmentResponse> processSingleReassignment(
      PMReassignmentRequest request, String modifiedBy, String userRole) {
    return findUserAndBRD(request.getNewPmUsername(), request.getBrdId())
        .flatMap(tuple -> validateAndProcessReassignment(tuple.getT1(), tuple.getT2(), request, modifiedBy, userRole))
        .onErrorResume(e -> handleReassignmentError(e, request));
  }

  private Mono<Tuple2<User, BRD>> findUserAndBRD(String pmUsername, String brdId) {
    return Mono.zip(
        userRepository.findByEmail(normalizeEmail(pmUsername)).defaultIfEmpty(User.builder().build()),
        brdRepository.findByBrdId(brdId).defaultIfEmpty(BRD.builder().build()));
  }

  private Mono<PMReassignmentResponse> validateAndProcessReassignment(
      User user, BRD brd, PMReassignmentRequest request, String modifiedBy, String userRole) {
    
    PMReassignmentResponse validationError = validateReassignmentRequest(user, brd, request);
    if (validationError != null) {
      return Mono.just(validationError);
    }

    return performReassignment(brd, request, modifiedBy, userRole);
  }

  private PMReassignmentResponse validateReassignmentRequest(User user, BRD brd, PMReassignmentRequest request) {
    if (user.getEmail() == null) {
      return createFailureResponse(request.getBrdId(), request.getNewPmUsername(), "PM not found");
    }
    
    if (brd.getBrdId() == null) {
      return createFailureResponse(request.getBrdId(), request.getNewPmUsername(), "BRD not found");
    }
    
    if (!isUserPM(user)) {
      log.warn("User {} is not a PM. Active role: {}, Roles: {}", 
          request.getNewPmUsername(), user.getActiveRole(), user.getRoles());
      return createFailureResponse(request.getBrdId(), request.getNewPmUsername(), DashboardConstants.PM_ONLY_ROLE);
    }
    
    return null;
  }

  private PMReassignmentResponse createFailureResponse(String brdId, String newPmUsername, String reason) {
    return PMReassignmentResponse.builder()
        .brdId(brdId)
        .status(ErrorValidationMessage.FAILURE)
        .newPmUsername(newPmUsername)
        .reason(reason)
        .build();
  }

  private Mono<PMReassignmentResponse> performReassignment(
      BRD brd, PMReassignmentRequest request, String modifiedBy, String userRole) {
    
    String oldPmUsername = brd.getCreator();
    brd.setCreator(request.getNewPmUsername());
    
    return brdRepository.save(brd)
        .flatMap(savedBrd -> createAuditLogAndResponse(savedBrd, oldPmUsername, request.getNewPmUsername(), modifiedBy, userRole))
        .onErrorResume(e -> handleSaveError(e, request));
  }

  private Mono<PMReassignmentResponse> createAuditLogAndResponse(
      BRD savedBrd, String oldPmUsername, String newPmUsername, String modifiedBy, String userRole) {
    
    AuditLogRequest auditRequest = buildAuditLogRequest(savedBrd, oldPmUsername, newPmUsername, modifiedBy, userRole);
    
    return auditLogService.logCreation(auditRequest)
        .then(Mono.just(createSuccessResponse(savedBrd.getBrdId(), oldPmUsername, newPmUsername)));
  }

  private AuditLogRequest buildAuditLogRequest(
      BRD brd, String oldPmUsername, String newPmUsername, String modifiedBy, String userRole) {
    
    Map<String, Object> oldValues = new HashMap<>();
    oldValues.put(BrdConstants.CREATOR, oldPmUsername);
    
    Map<String, Object> newValues = new HashMap<>();
    newValues.put(BrdConstants.CREATOR, newPmUsername);
    
    return AuditLogRequest.builder()
        .entityType("BRD")
        .entityId(brd.getBrdId())
        .action(UserConstants.ACTION_PM_REASSIGNMENT)
        .userId(modifiedBy)
        .userName(modifiedBy)
        .userRole(userRole)
        .eventTimestamp(LocalDateTime.now())
        .oldValues(oldValues)
        .newValues(newValues)
        .comment("Project Manager reassigned from " + oldPmUsername + " to " + newPmUsername)
        .build();
  }

  private PMReassignmentResponse createSuccessResponse(String brdId, String oldPmUsername, String newPmUsername) {
    return PMReassignmentResponse.builder()
        .brdId(brdId)
        .oldPmUsername(oldPmUsername)
        .newPmUsername(newPmUsername)
        .status("SUCCESS")
        .build();
  }

  private Mono<PMReassignmentResponse> handleSaveError(Throwable e, PMReassignmentRequest request) {
    String errorMessage = e.getMessage();
    log.error("Error saving BRD {}: {}", request.getBrdId(), errorMessage);
    return Mono.just(createFailureResponse(request.getBrdId(), request.getNewPmUsername(), "Database error: " + errorMessage));
  }

  private Mono<PMReassignmentResponse> handleReassignmentError(Throwable e, PMReassignmentRequest request) {
    String errorMessage = e.getMessage();
    log.error("Error processing reassignment for BRD {}: {}", request.getBrdId(), errorMessage);
    return Mono.just(createFailureResponse(request.getBrdId(), request.getNewPmUsername(), "Processing error: " + errorMessage));
  }

  private Api<List<PMReassignmentResponse>> buildBatchResponse(List<PMReassignmentResponse> results) {
    List<PMReassignmentResponse> failedReassignments = getFailedReassignments(results);
    List<PMReassignmentResponse> successfulReassignments = getSuccessfulReassignments(results);

    if (failedReassignments.isEmpty()) {
      return new Api<>(
          DashboardConstants.SUCCESS,
          "All PMs reassigned successfully",
          Optional.of(successfulReassignments),
          Optional.empty());
    } else {
      return new Api<>(
          ErrorValidationMessage.FAILURE,
          "Some PM reassignments failed",
          Optional.of(failedReassignments),
          Optional.empty());
    }
  }

  private List<PMReassignmentResponse> getFailedReassignments(List<PMReassignmentResponse> results) {
    return results.stream()
        .filter(response -> ErrorValidationMessage.FAILURE.equals(response.getStatus()))
        .toList();
  }

  private List<PMReassignmentResponse> getSuccessfulReassignments(List<PMReassignmentResponse> results) {
    return results.stream()
        .filter(response -> DashboardConstants.SUCCESS.equals(response.getStatus()))
        .toList();
  }

  @Override
  public Mono<Api<Void>> reassignProjectManager(String brdId, String newPmUsername, String modifiedBy, String userRole) {
    log.info("Reassigning PM for BRD: {} to {} by user: {}", brdId, newPmUsername, modifiedBy);

    return brdRepository
        .findByBrdId(brdId)
        .flatMap(brd -> validateAndUpdatePM(brd, newPmUsername, modifiedBy, userRole))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn("BRD not found with ID: {}", brdId);
                  return Mono.just(
                      new Api<Void>(
                          ErrorValidationMessage.FAILURE,
                          "BRD not found",
                          Optional.empty(),
                          Optional.empty()));
                }))
        .onErrorResume(ex -> {
          // If the error is a PM validation error, return a failure response instead of error
          if (ex instanceof IllegalArgumentException && ex.getMessage().equals(DashboardConstants.PM_ONLY_ROLE)) {
            return Mono.just(new Api<>(
                ErrorValidationMessage.FAILURE,
                DashboardConstants.PM_ONLY_ROLE,
                Optional.empty(),
                Optional.empty()
            ));
          }
          return Mono.error(handleErrors(ex));
        });
  }

  private Mono<Api<Void>> validateAndUpdatePM(BRD brd, String newPmUsername, String modifiedBy, String userRole) {
    return validatePMUser(newPmUsername)
        .flatMap(user -> updateBRDCreator(brd, newPmUsername, modifiedBy, userRole));
  }

  private Mono<User> validatePMUser(String pmUsername) {
    return userRepository
        .findByEmail(normalizeEmail(pmUsername))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn("PM not found with username: {}", pmUsername);
                  return Mono.error(new IllegalArgumentException("PM not found"));
                }))
        .flatMap(
            user -> {
              if (!hasRole(user, UserConstants.PM_ROLE)) {
                log.warn("User {} is not a PM. Active role: {}, Roles: {}", pmUsername, user.getActiveRole(), user.getRoles());
                return Mono.error(new IllegalArgumentException(DashboardConstants.PM_ONLY_ROLE));
              }
              return Mono.just(user);
            });
  }

  private Mono<Api<Void>> updateBRDCreator(BRD brd, String newPmUsername, String modifiedBy, String userRole) {
    String oldPmUsername = brd.getCreator();
    brd.setCreator(newPmUsername);
    
    return brdRepository
        .save(brd)
        .flatMap(savedBrd -> {
          log.info(
              "Successfully updated BRD {} creator from {} to {}", 
              savedBrd.getBrdId(), 
              oldPmUsername, 
              newPmUsername);
          
          // Create audit log
          Map<String, Object> oldValues = new HashMap<>();
          oldValues.put(BrdConstants.CREATOR, oldPmUsername);
          
          Map<String, Object> newValues = new HashMap<>();
          newValues.put(BrdConstants.CREATOR, newPmUsername);
          
          AuditLogRequest auditRequest = AuditLogRequest.builder()
              .entityType("BRD")
              .entityId(brd.getBrdId())
              .action(UserConstants.ACTION_PM_REASSIGNMENT)
              .userId(modifiedBy)
              .userName(modifiedBy)
              .userRole(userRole)
              .eventTimestamp(LocalDateTime.now())
              .oldValues(oldValues)
              .newValues(newValues)
              .comment("Project Manager reassigned from " + oldPmUsername + " to " + newPmUsername)
              .build();
          
          return auditLogService
              .logCreation(auditRequest)
              .then(Mono.just(new Api<>(
                  "success",
                  "Project Manager reassigned successfully",
                  Optional.empty(),
                  Optional.empty())));
        });
  }

  @Override
  public Mono<ResponseEntity<Api<UserListResponse>>> getPMAndBAUsers() {
    log.debug("Fetching PM and BA users");

    return userRepository
        .findByRoleInOrRolesInProjected(Arrays.asList(UserConstants.PM_ROLE, UserConstants.BA_ROLE))
        .distinct(UserProjection::getId) // Remove duplicates based on user ID
        .map(this::mapToUserInfo)
        .collectList()
        .map(
            allUsers -> {
              List<UserListResponse.UserInfo> pmUsers =
                  allUsers.stream()
                      .filter(this::hasPMRole)
                      .toList();

              List<UserListResponse.UserInfo> baUsers =
                  allUsers.stream()
                      .filter(this::hasBARole)
                      .toList();

              UserListResponse response =
                  UserListResponse.builder().pmUsers(pmUsers).baUsers(baUsers).build();

              Api<UserListResponse> apiResponse = new Api<>();
              apiResponse.setStatus(DashboardConstants.SUCCESS);
              apiResponse.setMessage(USERS_RETRIEVED);
              apiResponse.setData(Optional.of(response));
              apiResponse.setErrors(Optional.empty());

              return ResponseEntity.ok(apiResponse);
            })
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<UserListResponse>>> searchPMAndBAUsers(String searchTerm) {

    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      log.debug("Search term is empty, returning all PM and BA users");
      return getPMAndBAUsers();
    }

    String normalizedSearchTerm = searchTerm.trim();
    log.debug("Searching PM and BA users with term: '{}'", normalizedSearchTerm);

    // Try optimized text search first
    return searchWithTextIndex(normalizedSearchTerm)
        .switchIfEmpty(searchWithRegexFallback(normalizedSearchTerm))
        .collectList()
        .map(this::buildUserListResponse)
        .doOnSuccess(
            response -> {
              UserListResponse data = response.getBody().getData().orElse(null);
              if (data != null) {
                int pmCount = data.getPmUsers() != null ? data.getPmUsers().size() : 0;
                int baCount = data.getBaUsers() != null ? data.getBaUsers().size() : 0;
                log.debug("Search completed. Found {} PM users and {} BA users", pmCount, baCount);
              }
            })
        .onErrorMap(this::handleErrors);
  }

  /** Optimized text search using MongoDB text index */
  private Flux<UserProjection> searchWithTextIndex(String searchTerm) {
    return userRepository
        .searchByRoleAndTextProjected(
            Arrays.asList(UserConstants.PM_ROLE, UserConstants.BA_ROLE), searchTerm)
        .onErrorResume(
            throwable -> {
              log.debug(
                  "Text search failed, will fallback to regex search: {}", throwable.getMessage());
              return Flux.empty();
            });
  }

  /** Fallback regex search with performance optimizations */
  private Flux<UserProjection> searchWithRegexFallback(String searchTerm) {
    log.debug("Using regex fallback search for term: '{}'", searchTerm);

    // Split search term for full name search
    String[] searchParts = searchTerm.split("\\s+");
    String firstName;
    String lastName;

    if (searchParts.length >= 2) {
      // Full name search: "john smith" -> firstName: "john", lastName: "smith"
      firstName = searchParts[0];
      lastName = searchParts[1];
      log.debug("Full name search: firstName='{}', lastName='{}'", firstName, lastName);
    } else {
      // Single term search: use same term for both fields
      firstName = searchTerm;
      lastName = searchTerm;
      log.debug("Single term search: '{}'", searchTerm);
    }

    return userRepository
        .searchByRoleAndNameOrEmailProjectedOptimized(
            Arrays.asList(UserConstants.PM_ROLE, UserConstants.BA_ROLE),
            searchTerm,
            firstName,
            lastName)
        .doOnNext(
            user ->
                log.debug(
                    "Found user: {} {} ({})",
                    user.getFirstName(),
                    user.getLastName(),
                    user.getActiveRole()))
        .doOnComplete(() -> log.debug("Regex search completed"));
  }

  /** Build the response from user list */
  private ResponseEntity<Api<UserListResponse>> buildUserListResponse(
      List<UserProjection> allUsers) {
    List<UserListResponse.UserInfo> pmUsers =
        allUsers.stream()
            .filter(user -> UserConstants.PM_ROLE.equals(user.getActiveRole()))
            .map(this::mapToUserInfo)
            .toList();

    List<UserListResponse.UserInfo> baUsers =
        allUsers.stream()
            .filter(user -> UserConstants.BA_ROLE.equals(user.getActiveRole()))
            .map(this::mapToUserInfo)
            .toList();

    UserListResponse response =
        UserListResponse.builder().pmUsers(pmUsers).baUsers(baUsers).build();

    Api<UserListResponse> apiResponse = new Api<>();
    apiResponse.setStatus(UserConstants.SUCCESS);
    apiResponse.setMessage(USERS_SEARCHED);
    apiResponse.setData(Optional.of(response));
    apiResponse.setErrors(Optional.empty());

    return ResponseEntity.ok(apiResponse);
  }

  private String normalizeEmail(String email) {
    return email != null ? email.toLowerCase() : null;
  }

  private User mapToEntity(UserRequest request) {
    String defaultPassword = request.getFirstName() + "_" + request.getActiveRole();
    String encodedPassword = passwordEncoder.encode(defaultPassword);
    String normalizedEmail = normalizeEmail(request.getEmail());

    // Initialize roles list with both the legacy role and new roles
    List<String> roles = new ArrayList<>();
    if (request.getActiveRole() != null) {
      String role =
          request
              .getActiveRole()
              .replace(SecurityConstants.ROLE_PREFIX, ""); // Remove prefix for storage
      roles.add(role);
    }
    if (request.getRoles() != null) {
      roles.addAll(
          request.getRoles().stream()
              .map(r -> r.replace(SecurityConstants.ROLE_PREFIX, "")) // Remove prefix for storage
              .toList());
    }
    // Remove duplicates
    roles = roles.stream().distinct().collect(Collectors.toList());

    return User.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(normalizedEmail)
        .activeRole(
            request
                .getActiveRole()
                .replace(SecurityConstants.ROLE_PREFIX, "")) // Remove prefix for storage
        .roles(roles) // Use the combined roles list without prefix
        .password(encodedPassword.toCharArray())
        .createdAt(LocalDateTime.now())
        .passwordChangeRequired(true)
        .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
        .build();
  }

  private UserResponse mapToResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .activeRole(user.getActiveRole())
        .roles(user.getRoles())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
        .build();
  }

  private UserDetailResponse mapToDetailResponse(User user) {
    return UserDetailResponse.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .username(user.getEmail())
        .activeRole(user.getActiveRole())
        .roles(user.getRoles())
        .dateCreated(user.getCreatedAt())
        .dateLastModified(user.getUpdatedAt())
        .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
        .build();
  }

  private UserListResponse.UserInfo mapToUserInfo(UserProjection projection) {
    return UserListResponse.UserInfo.builder()
        .id(projection.getId())
        .fullName(projection.getFirstName() + " " + projection.getLastName())
        .email(projection.getEmail())
        .activeRole(projection.getActiveRole())
        .roles(projection.getRoles())
        .status(projection.getStatus() != null ? projection.getStatus() : UserStatus.ACTIVE)
        .createdAt(projection.getCreatedAt())
        .build();
  }

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException) {
      return ex;
    } else if (ex instanceof DuplicateKeyException
        || ex instanceof org.springframework.dao.DuplicateKeyException
        || (ex instanceof Exception
            && ex.getMessage().contains("duplicate key error")
            && ex.getMessage().contains("email"))) {
      return new AlreadyExistException("User already exists with the given email address");
    } else if (ex instanceof Exception && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  @Override
  public Mono<User> findById(String id) {
    return userRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND_WITH_ID + id)));
  }

  @Override
  public Mono<Void> deleteById(String id) {
    return userRepository.deleteById(id);
  }

  /** Creates a failure API response */
  private Api<String> createFailureResponse(String message) {
    return new Api<>(ErrorValidationMessage.FAILURE, message, Optional.empty(), Optional.empty());
  }

  /** Creates a success API response */
  private Api<String> createSuccessResponse(String message) {
    return new Api<>(DashboardConstants.SUCCESS, message, Optional.empty(), Optional.empty());
  }

  /** Validates if a user has the expected role */
  private Mono<User> validateUserRole(User user, String expectedRole, String roleErrorMessage) {
    if (!hasRole(user, expectedRole)) {
      log.warn("User {} is not a {}. Active role: {}, Roles: {}", user.getId(), expectedRole, user.getActiveRole(), user.getRoles());
      return Mono.error(new IllegalArgumentException(roleErrorMessage));
    }
    return Mono.just(user);
  }

  /** Handles common error cases for user deletion */
  private Mono<Api<String>> handleDeletionError(Throwable e) {
    if (e instanceof NotFoundException) {
      return Mono.just(createFailureResponse("User not found"));
    }
    if (e instanceof IllegalArgumentException) {
      return Mono.just(createFailureResponse(e.getMessage()));
    }
    return Mono.just(
        createFailureResponse("An error occurred while deleting the user: " + e.getMessage()));
  }

  @Override
  public Mono<Api<String>> deletePmUser(String userId) {
    log.info("Attempting to delete PM user with ID: {}", userId);

    return findById(userId)
        .flatMap(
            user ->
                validateUserRole(user, SecurityConstants.ROLE_PM, DashboardConstants.PM_ONLY_ROLE)
                    .flatMap(
                        validatedUser -> {
                          String userEmail = validatedUser.getEmail();

                          // First call to check for active BRDs
                          return brdRepository
                              .findByCreator(userEmail)
                              .filter(
                                  brd -> {
                                    String status = brd.getStatus();
                                    return status != null
                                        && !status.equalsIgnoreCase(BrdConstants.SUBMIT);
                                  })
                              .hasElements()
                              .flatMap(
                                  hasActive ->
                                      Boolean.TRUE.equals(hasActive)
                                          ? Mono.just(
                                              createFailureResponse(
                                                  "Active BRD(s) associated with this user. Please reassign them before removing user."))
                                          : brdRepository
                                              .findByCreator(userEmail)
                                              .filter(
                                                  brd -> {
                                                    String status = brd.getStatus();
                                                    return status != null
                                                        && status.equalsIgnoreCase(
                                                            BrdConstants.SUBMIT);
                                                  })
                                              .flatMap(
                                                  brd -> {
                                                    brd.setUserPMRemoved(true);
                                                    return brdRepository.save(brd);
                                                  })
                                              .collectList()
                                              .flatMap(
                                                  savedBrds -> {
                                                    if (savedBrds.isEmpty()) {
                                                      return deleteById(userId)
                                                          .thenReturn(
                                                              createSuccessResponse(
                                                                  "PM user removed successfully."));
                                                    }
                                                    return Flux.fromIterable(savedBrds)
                                                        .then(deleteById(userId))
                                                        .thenReturn(
                                                            createSuccessResponse(
                                                                "PM user removed successfully."));
                                                  }));
                        }))
        .onErrorResume(this::handleDeletionError);
  }

  @Override
  public Mono<Api<String>> deleteBaUser(String userId) {
    log.info("Attempting to delete BA user with ID: {}", userId);

    return findById(userId)
        .flatMap(
            user ->
                validateUserRole(user, UserConstants.BA_ROLE, "User is not a BA")
                    .flatMap(
                        validatedUser -> {
                          String userEmail = validatedUser.getEmail();
                          log.info("Found BA user with email: {}", userEmail);

                          return baAssignmentRepository
                              .findByBaEmail(userEmail)
                              .doOnNext(
                                  assignment ->
                                      log.info(
                                          "Found BA assignment for BRD: {}", assignment.getBrdId()))
                              .flatMap(
                                  assignment ->
                                      brdRepository
                                          .findByBrdId(assignment.getBrdId())
                                          .doOnNext(
                                              brd ->
                                                  log.info(
                                                      "Found BRD {} with status: {}",
                                                      brd.getBrdId(),
                                                      brd.getStatus())))
                              .filter(
                                  brd -> {
                                    String status = brd.getStatus();
                                    boolean isActive =
                                        status != null
                                            && !status.equalsIgnoreCase(BrdConstants.SUBMIT);
                                    log.info(
                                        "BRD {} active status check: {}", brd.getBrdId(), isActive);
                                    return isActive;
                                  })
                              .hasElements()
                              .doOnNext(hasActive -> log.info("Has active BRDs: {}", hasActive))
                              .flatMap(
                                  hasActive ->
                                      Boolean.TRUE.equals(hasActive)
                                          ? Mono.just(
                                              createFailureResponse(
                                                  "Active BRD assignment(s) associated with this user. Please reassign them before removing user."))
                                          : baAssignmentRepository
                                              .findByBaEmail(userEmail)
                                              .doOnNext(
                                                  assignment ->
                                                      log.info(
                                                          "Processing submitted BRD: {}",
                                                          assignment.getBrdId()))
                                              .flatMap(
                                                  assignment ->
                                                      brdRepository.findByBrdId(
                                                          assignment.getBrdId()))
                                              .filter(
                                                  brd -> {
                                                    String status = brd.getStatus();
                                                    boolean isSubmitted =
                                                        status != null
                                                            && status.equalsIgnoreCase(
                                                                BrdConstants.SUBMIT);
                                                    log.info(
                                                        "BRD {} submitted status check: {}",
                                                        brd.getBrdId(),
                                                        isSubmitted);
                                                    return isSubmitted;
                                                  })
                                              .flatMap(
                                                  brd -> {
                                                    brd.setUserBARemoved(true);
                                                    log.info(
                                                        "Setting userBARemoved flag for BRD: {}",
                                                        brd.getBrdId());
                                                    return brdRepository.save(brd);
                                                  })
                                              .then(deleteById(userId))
                                              .thenReturn(
                                                  createSuccessResponse(
                                                      "BA user removed successfully.")));
                        }))
        .onErrorResume(this::handleDeletionError);
  }

  @Override
  public Mono<User> findByEmailAndRole(String email, String role) {
    // Remove ROLE_ prefix for database lookup
    String dbRole = role.replace(SecurityConstants.ROLE_PREFIX, "");

    return userRepository
        .findByEmailAndRole(normalizeEmail(email), dbRole)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    "User not found with email and Role: " + email + " and role: " + role)))
        .map(
            user -> {
              // Ensure roles list is initialized
              if (user.getRoles() == null) {
                user.setRoles(new ArrayList<>());
              }

              // Add both the legacy role and any additional roles
              if (user.getActiveRole() != null && !user.getRoles().contains(user.getActiveRole())) {
                user.getRoles().add(user.getActiveRole());
              }

              // Add MANAGER role if it's not already there (based on your specific case)
              if (isUserPM(user) && !user.getRoles().contains(SecurityConstants.ROLE_MANAGER)) {
                user.getRoles().add(SecurityConstants.ROLE_MANAGER);
              }

              // Remove duplicates
              user.setRoles(user.getRoles().stream().distinct().toList());

              return user;
            });
  }

  @Override
  public Mono<Api<Map<String, Boolean>>> checkPasswordChangeRequired(
      String email, List<String> roles) {
    return userRepository
        .findByEmail(normalizeEmail(email))
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(ErrorValidationMessage.USER_NOT_FOUND_WITH_EMAIL + email)))
        .map(
            user -> {
              // Check if any of the provided roles match either the legacy role field or the new
              // roles list
              boolean requiresChange = false;
              for (String role : roles) {
                if (hasRole(user, role)) {
                  requiresChange = requiresChange || user.isPasswordChangeRequired();
                }
              }

              Api<Map<String, Boolean>> response = new Api<>();
              response.setStatus(DashboardConstants.SUCCESS);
              response.setMessage("Password change requirement checked successfully");
              response.setData(
                  Optional.of(Collections.singletonMap("requirePasswordChange", requiresChange)));
              response.setErrors(Optional.empty());
              return response;
            });
  }

  @Override
  public Mono<Api<Void>> changePassword(String email, String role, ChangePasswordRequest request) {
    log.info("Changing password for user with email: {} and role: {}", email, role);

    return userRepository
        .findByEmail(normalizeEmail(email))
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(ErrorValidationMessage.USER_NOT_FOUND_WITH_EMAIL + email)))
        .flatMap(
            user -> {
              // Verify that the user has the specified role
              if (!hasRole(user, role)) {
                log.warn(
                    "User does not have the specified role. Email: {}, Required role: {}",
                    email,
                    role);
                return Mono.error(new BadRequestException("User does not have the specified role"));
              }

              // Verify current password
              String currentStoredPassword = String.valueOf(user.getPassword());
              if (!passwordEncoder.matches(request.getCurrentPassword(), currentStoredPassword)) {
                log.warn("Invalid current password provided for user: {}", email);
                return Mono.error(new BadRequestException("Current password is incorrect"));
              }

              // Encode new password and update user
              String newEncodedPassword = passwordEncoder.encode(request.getNewPassword());
              user.setPassword(newEncodedPassword.toCharArray());
              user.setPasswordChangeRequired(false);
              user.setUpdatedAt(LocalDateTime.now());

              return userRepository
                  .save(user)
                  .map(
                      savedUser -> {
                        log.info("Successfully changed password for user: {}", email);
                        Api<Void> response = new Api<>();
                        response.setStatus("success");
                        response.setMessage("Password changed successfully");
                        response.setData(Optional.empty());
                        response.setErrors(Optional.empty());
                        return response;
                      });
            })
        .onErrorResume(
            e -> {
              log.error("Error in password change process: {}", e.getMessage());
              Api<Void> response = new Api<>();
              response.setStatus(ErrorValidationMessage.FAILURE);
              response.setMessage(e.getMessage());
              response.setErrors(
                  Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())));
              return Mono.just(response);
            });
  }

  @Override
  public Mono<Api<UserDetailResponse>> changeUserStatus(String userId, UserStatus newStatus) {
    return userRepository
        .findById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND_WITH_ID + userId)))
        .flatMap(
            user ->
                !isUserPM(user) && !isUserBA(user)
                    ? Mono.error(
                        new BadRequestException("Only PM or BA user status can be changed."))
                    : Mono.defer(
                        () -> {
                          user.setStatus(newStatus);
                          user.setUpdatedAt(java.time.LocalDateTime.now());
                          return userRepository
                              .save(user)
                              .map(this::mapToDetailResponse)
                              .map(
                                  updated ->
                                      new Api<>(
                                          UserConstants.SUCCESS,
                                          "User status updated successfully",
                                          java.util.Optional.of(updated),
                                          java.util.Optional.empty()));
                        }));
  }

  @Override
  public Mono<Api<UserDetailResponse>> updateUser(
      String userId, UpdateUserRequest request, String adminId) {
    log.info("Updating user details for userId: {} by admin: {}", userId, adminId);

    return userRepository
        .findById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND_WITH_ID + userId)))
        .flatMap(
            existingUser ->
                userRepository
                    .findByEmail(normalizeEmail(request.getEmail()))
                    .filter(user -> !user.getId().equals(userId))
                    .flatMap(user -> Mono.error(new AlreadyExistException("Email already in use")))
                    .then(Mono.just(existingUser))
                    .flatMap(
                        user -> {
                          // Update user details
                          user.setFirstName(request.getFirstName());
                          user.setLastName(request.getLastName());
                          user.setEmail(normalizeEmail(request.getEmail()));

                          // Update roles
                          user.setActiveRole(request.getActiveRole());
                          user.setRoles(request.getRoles());
                          user.setUpdatedAt(LocalDateTime.now());

                          // Log the changes
                          log.info(
                              "User details updated by admin: {}. Changes: firstName={}, lastName={}, email={}, activeRole={}, roles={}",
                              adminId,
                              request.getFirstName(),
                              request.getLastName(),
                              request.getEmail(),
                              request.getActiveRole(),
                              request.getRoles());

                          return userRepository
                              .save(user)
                              .map(this::mapToDetailResponse)
                              .map(
                                  updated ->
                                      new Api<>(
                                          UserConstants.SUCCESS,
                                          "User details updated successfully",
                                          Optional.of(updated),
                                          Optional.empty()));
                        }))
        .onErrorResume(ex -> Mono.error(handleErrors(ex)));
  }

  @Override
  public Mono<Api<String>> removeRole(String userId, String role, String modifiedBy) {
    log.info("Attempting to remove role {} from user {} by {}", role, userId, modifiedBy);

    return userRepository
        .findById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND_WITH_ID + userId)))
        .flatMap(
            user -> {
              // Check if user has the role
              if (!user.getRoles().contains(role) && !role.equals(user.getActiveRole())) {
                log.warn("User {} does not have role {}", userId, role);
                return Mono.error(new BadRequestException("User does not have the specified role"));
              }

              // Check if this is the last role
              if (user.getRoles().size() <= 1 && user.getActiveRole() != null) {
                log.warn("Cannot remove last role from user {}", userId);
                return Mono.error(
                    new BadRequestException("Cannot remove the last role from a user"));
              }

              // Store old values for audit
              Map<String, Object> oldValues = new HashMap<>();
              oldValues.put(SecurityConstants.ROLES, new ArrayList<>(user.getRoles()));
              oldValues.put("role", user.getActiveRole());

              // Remove role from roles list
              user.getRoles().remove(role);

              // Update legacy role field if needed
              if (role.equals(user.getActiveRole())) {
                user.setActiveRole(user.getRoles().isEmpty() ? null : user.getRoles().get(0));
              }

              // Update modified info
              user.setUpdatedAt(LocalDateTime.now());

              // Prepare new values for audit
              Map<String, Object> newValues = new HashMap<>();
              newValues.put(SecurityConstants.ROLES, new ArrayList<>(user.getRoles()));
              newValues.put("role", user.getActiveRole());

              return userRepository
                  .save(user)
                  .flatMap(
                      savedUser -> {
                        // Create audit log
                        AuditLogRequest auditRequest =
                            AuditLogRequest.builder()
                                .entityType(ENTITY_TYPE_USER)
                                .entityId(userId)
                                .action(ACTION_REMOVE_ROLE)
                                .userId(modifiedBy)
                                .userName(modifiedBy)
                                .userRole(
                                    SecurityConstants
                                        .ROLE_MANAGER) // Only managers can modify roles
                                .eventTimestamp(LocalDateTime.now())
                                .oldValues(oldValues)
                                .newValues(newValues)
                                .comment("Removed role: " + role)
                                .build();

                        return auditLogService
                            .logCreation(auditRequest)
                            .then(
                                Mono.just(
                                    createSuccessResponse(
                                        "Role " + role + " removed from user " + userId)));
                      })
                  .doOnSuccess(
                      v -> log.info("Successfully removed role {} from user {}", role, userId))
                  .onErrorResume(
                      e -> {
                        log.error(
                            "Error removing role {} from user {}: {}",
                            role,
                            userId,
                            e.getMessage());
                        return Mono.just(createFailureResponse(e.getMessage()));
                      });
            })
        .onErrorResume(
            e -> {
              log.error("Error removing role {} from user {}: {}", role, userId, e.getMessage());
              return Mono.just(createFailureResponse(e.getMessage()));
            });
  }

  @Override
  public Mono<Api<String>> addRoleToUser(String userId, String role, String modifiedBy) {
    log.info("Attempting to add role {} to user {} by {}", role, userId, modifiedBy);

    return userRepository
        .findById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND_WITH_ID + userId)))
        .flatMap(
            user -> {
              // Check if user already has the role
              if (user.getRoles().contains(role) || role.equals(user.getActiveRole())) {
                log.warn("User {} already has role {}", userId, role);
                return Mono.error(new BadRequestException("User already has the specified role"));
              }

              // Store old values for audit
              Map<String, Object> oldValues = new HashMap<>();
              oldValues.put(
                  "roles",
                  user.getRoles() != null ? new ArrayList<>(user.getRoles()) : new ArrayList<>());
              oldValues.put("role", user.getActiveRole());

              // Add role to roles list
              if (user.getRoles() == null) {
                user.setRoles(new ArrayList<>());
              }
              user.getRoles().add(role);

              // If legacy role field is empty, set it to this role
              if (user.getActiveRole() == null) {
                user.setActiveRole(role);
              }

              // Update modified info
              user.setUpdatedAt(LocalDateTime.now());

              // Prepare new values for audit
              Map<String, Object> newValues = new HashMap<>();
              newValues.put("roles", new ArrayList<>(user.getRoles()));
              newValues.put("role", user.getActiveRole());

              return userRepository
                  .save(user)
                  .flatMap(
                      savedUser -> {
                        // Create audit log
                        AuditLogRequest auditRequest =
                            AuditLogRequest.builder()
                                .entityType(ENTITY_TYPE_USER)
                                .entityId(userId)
                                .action(ACTION_ADD_ROLE)
                                .userId(modifiedBy)
                                .userName(modifiedBy)
                                .userRole(
                                    SecurityConstants
                                        .ROLE_MANAGER) // Only managers can modify roles
                                .eventTimestamp(LocalDateTime.now())
                                .oldValues(oldValues)
                                .newValues(newValues)
                                .comment("Added role: " + role)
                                .build();

                        return auditLogService
                            .logCreation(auditRequest)
                            .then(
                                Mono.just(
                                    createSuccessResponse(
                                        "Role " + role + " added to user " + userId)));
                      })
                  .doOnSuccess(v -> log.info("Successfully added role {} to user {}", role, userId))
                  .onErrorResume(
                      e -> {
                        log.error(
                            "Error adding role {} to user {}: {}", role, userId, e.getMessage());
                        return Mono.just(createFailureResponse(e.getMessage()));
                      });
            })
        .onErrorResume(
            e -> {
              log.error("Error adding role {} to user {}: {}", role, userId, e.getMessage());
              return Mono.just(createFailureResponse(e.getMessage()));
            });
  }
}
