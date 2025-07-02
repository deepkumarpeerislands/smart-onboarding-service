package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.DashboardConstants;
import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUserService;
import com.aci.smart_onboarding.swagger.UserRequestAndResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "${api.default.path}/user", name = "User Management")
@Tag(name = "User Management", description = "APIs for user management")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private static final String ROLE_PREFIX = "ROLE_";

  private final IUserService userService;
  private final BRDSecurityService securityService;
  private final BRDRepository brdRepository;

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Create a new user",
      description =
          "Creates a new user with the provided details. A default password will be generated in the format 'firstName_role'. Only accessible to users with MANAGER role. Requires authentication with a valid JWT token in the Authorization header.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = UserRequest.class),
                      examples =
                          @ExampleObject(
                              name = "User creation request",
                              value = UserRequestAndResponse.USER_REQUEST_EXAMPLE,
                              description =
                                  "Example request to create a new user. A default password will be generated as 'firstName_role'"))),
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "201",
      description = "User created successfully with default password",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value = UserRequestAndResponse.USER_CREATION_SUCCESS_RESPONSE,
                      description = "User created successfully with generated default password")))
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Validation error",
                      value = UserRequestAndResponse.VALIDATION_ERROR_RESPONSE,
                      description = "Invalid request data")))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Unauthorized error",
                      value =
                          "{\n  \"status\": \"failure\",\n  \"message\": \"No authentication token provided\",\n  \"data\": null,\n  \"errors\": null\n}",
                      description = "Authentication token is missing or invalid")))
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - User is not a Manager",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Access denied",
                      value = UserRequestAndResponse.ACCESS_DENIED_RESPONSE,
                      description = "User does not have MANAGER role")))
  @ApiResponse(
      responseCode = "409",
      description = "Email already exists",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Email conflict",
                      value = UserRequestAndResponse.EMAIL_ALREADY_EXISTS_ERROR,
                      description = "A user with the provided email already exists")))
  public Mono<ResponseEntity<Api<UserResponse>>> createUser(
      @Valid @RequestBody UserRequest userRequest) {
    log.info("Creating new user with email: {}", userRequest.getEmail());

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow managers to create users
                          if (!UserConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to create a user but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(UserConstants.MANAGER_ONLY_MESSAGE));
                          }

                          log.info(
                              "Authorized user {} with role {} is creating a new user",
                              username,
                              role);
                          return userService.createUser(userRequest);
                        }));
  }

  @GetMapping(value = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Get user details by email",
      description = UserRequestAndResponse.USER_DETAILS_ENDPOINT_DESCRIPTION,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "User details retrieved successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value = UserRequestAndResponse.USER_DETAIL_RESPONSE,
                      description = "Detailed user information retrieved successfully")))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Unauthorized error",
                      value =
                          "{\n  \"status\": \"failure\",\n  \"message\": \"No authentication token provided\",\n  \"data\": null,\n  \"errors\": null\n}",
                      description = "Authentication token is missing or invalid")))
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - User is not a Manager",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Access denied",
                      value = UserRequestAndResponse.ACCESS_DENIED_RESPONSE,
                      description = "User does not have MANAGER role")))
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Not found error",
                      value = UserRequestAndResponse.USER_NOT_FOUND_RESPONSE,
                      description = "User with the provided email not found")))
  public Mono<ResponseEntity<Api<UserDetailResponse>>> getUserByEmail(@PathVariable String email) {
    log.info("Retrieving user details with email: {}", email);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow managers to view user details
                          if (!UserConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to retrieve user details but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(UserConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .getUserByEmail(email)
                              .doOnSuccess(
                                  resp ->
                                      log.info(
                                          "Successfully retrieved user details with email: {}",
                                          email));
                        }));
  }

  @GetMapping(value = "/pm-ba-users", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Get all PM and BA users",
      description = UserRequestAndResponse.PM_BA_USERS_ENDPOINT_DESCRIPTION,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Users retrieved successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples = {
                @ExampleObject(
                    name = "Success response with users",
                    value = UserRequestAndResponse.PM_BA_USERS_SUCCESS_RESPONSE,
                    description = "Successfully retrieved PM and BA users"),
                @ExampleObject(
                    name = "Success response - No users",
                    value = UserRequestAndResponse.PM_BA_USERS_EMPTY_RESPONSE,
                    description = "Successfully retrieved but no users found")
              }))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Unauthorized error",
                      value = UserRequestAndResponse.PM_BA_USERS_UNAUTHORIZED_RESPONSE,
                      description = "Authentication token is missing or invalid")))
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - User is not a Manager",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Access denied",
                      value = UserRequestAndResponse.ACCESS_DENIED_RESPONSE,
                      description = "User does not have MANAGER role")))
  @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Server error",
                      value = UserRequestAndResponse.PM_BA_USERS_SERVER_ERROR_RESPONSE,
                      description = "Internal server error occurred")))
  public Mono<ResponseEntity<Api<UserListResponse>>> getPMAndBAUsers() {
    log.info("Retrieving all PM and BA users");

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        userRole -> {
                          if (!UserConstants.MANAGER_ROLE.equals(userRole)) {
                            return Mono.error(
                                new AccessDeniedException(UserConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .getPMAndBAUsers()
                              .doOnSuccess(
                                  resp -> log.info("Successfully retrieved PM and BA users"));
                        }));
  }

  @GetMapping(value = "/pm-ba-users/search", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Search PM and BA users",
      description = UserRequestAndResponse.SEARCH_PM_BA_USERS_ENDPOINT_DESCRIPTION,
      parameters = {
        @Parameter(
            name = "searchTerm",
            description = UserRequestAndResponse.SEARCH_PARAM_DESC,
            required = false,
            schema = @Schema(type = "string"),
            example = "john smith")
      },
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Users searched successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples = {
                @ExampleObject(
                    name = "Search results found",
                    value = UserRequestAndResponse.SEARCH_PM_BA_USERS_SUCCESS_RESPONSE,
                    description = "Successfully found matching PM and BA users"),
                @ExampleObject(
                    name = "No search results",
                    value = UserRequestAndResponse.SEARCH_PM_BA_USERS_NO_RESULTS_RESPONSE,
                    description = "No users found matching the search criteria")
              }))
  @ApiResponse(
      responseCode = "400",
      description = "Bad request - Invalid search parameter",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Bad request",
                      value = UserRequestAndResponse.SEARCH_PM_BA_USERS_BAD_REQUEST_RESPONSE,
                      description = "Invalid search parameter provided")))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Unauthorized error",
                      value = UserRequestAndResponse.PM_BA_USERS_UNAUTHORIZED_RESPONSE,
                      description = "Authentication token is missing or invalid")))
  @ApiResponse(
      responseCode = "403",
      description = "Access denied - User is not a Manager",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Access denied",
                      value = UserRequestAndResponse.ACCESS_DENIED_RESPONSE,
                      description = "User does not have MANAGER role")))
  @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Server error",
                      value = UserRequestAndResponse.PM_BA_USERS_SERVER_ERROR_RESPONSE,
                      description = "Internal server error occurred")))
  public Mono<ResponseEntity<Api<UserListResponse>>> searchPMAndBAUsers(
      @RequestParam(value = "searchTerm", required = false) String searchTerm) {

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        userRole -> {
                          if (!UserConstants.MANAGER_ROLE.equals(userRole)) {
                            return Mono.error(
                                new AccessDeniedException(UserConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .searchPMAndBAUsers(searchTerm)
                              .doOnSuccess(
                                  resp -> log.info("Successfully searched PM and BA users"));
                        }));
  }

  /**
   * Gets username from the security context.
   *
   * @return Mono containing the username
   */
  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> context.getAuthentication().getName());
  }

  @GetMapping(value = "/role/{role}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Get users by role",
      description =
          "Retrieves all users with the specified role. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Users retrieved successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserDetailResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "No users found with the specified role")
  public Mono<ResponseEntity<List<UserDetailResponse>>> getUsersByRole(@PathVariable String role) {
    log.info("Retrieving users with role: {}", role);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        currentRole -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(currentRole)) {
                            log.warn(
                                "User {} with role {} attempted to retrieve users but is not authorized",
                                username,
                                currentRole);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .getUsersByRole(role)
                              .collectList()
                              .flatMap(
                                  users -> {
                                    if (users.isEmpty()) {
                                      log.info("No users found with role: {}", role);
                                      return Mono.just(ResponseEntity.notFound().build());
                                    }
                                    log.info("Found {} users with role: {}", users.size(), role);
                                    return Mono.just(ResponseEntity.ok(users));
                                  });
                        }));
  }

  @PutMapping(value = "/reassign-pm")
  @Operation(
      summary = "Reassign Project Manager for a BRD",
      description =
          "Updates the creator field in the BRD collection with a new Project Manager. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Project Manager reassigned successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value =
                          """
                  {
                      "status": "success",
                      "message": "Project Manager reassigned successfully",
                      "data": null,
                      "errors": null
                  }
                  """,
                      description = "PM reassigned successfully")))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request - BRD ID or PM username is invalid",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Bad request",
                      value =
                          """
                  {
                      "status": "failure",
                      "message": "Invalid request",
                      "data": null,
                      "errors": {
                          "error": "User is not a PM"
                      }
                  }
                  """,
                      description = "Invalid request parameters")))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "BRD not found")
  public Mono<ResponseEntity<Api<Void>>> reassignProjectManager(
      @Valid @RequestBody PMReassignmentRequest request) {
    log.info(
        "Reassigning Project Manager for BRD: {} to {}",
        request.getBrdId(),
        request.getNewPmUsername());

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(DashboardConstants.MANAGER_ONLY_MESSAGE, username, role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }
                          return userService
                              .reassignProjectManager(
                                  request.getBrdId(), request.getNewPmUsername(), username, role)
                              .map(
                                  apiResponse -> {
                                    if (DashboardConstants.SUCCESS.equals(
                                        apiResponse.getStatus())) {
                                      return ResponseEntity.ok(apiResponse);
                                    } else if ("BRD not found".equals(apiResponse.getMessage())) {
                                      return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                          .body(apiResponse);
                                    } else {
                                      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                          .body(apiResponse);
                                    }
                                  });
                        }));
  }

  @PutMapping(value = "/reassign-pms")
  @Operation(
      summary = "Reassign Project Managers for multiple BRDs",
      description =
          "Updates the creator field in the BRD collection with new Project Managers for multiple BRDs. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Project Managers reassigned successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value =
                          """
                  {
                      "status": "success",
                      "message": "All PMs reassigned successfully",
                      "data": [
                          {
                              "brdId": "brd123",
                              "oldPmUsername": "oldpm@example.com",
                              "newPmUsername": "newpm@example.com",
                              "status": "SUCCESS"
                          }
                      ],
                      "errors": null
                  }
                  """,
                      description = "All PMs reassigned successfully")))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request - Some reassignments failed",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Partial failure",
                      value =
                          """
                  {
                      "status": "failure",
                      "message": "Some PM reassignments failed",
                      "data": [
                          {
                              "brdId": "brd123",
                              "newPmUsername": "invalid@example.com",
                              "status": "FAILED",
                              "reason": "User is not a PM"
                          }
                      ],
                      "errors": null
                  }
                  """,
                      description = "Some reassignments failed")))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  public Mono<ResponseEntity<Api<List<PMReassignmentResponse>>>> reassignProjectManagers(
      @Valid @RequestBody List<PMReassignmentRequest> requests) {
    log.info("Processing batch PM reassignment for {} BRDs", requests.size());

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(DashboardConstants.PM_MANAGER_ROLE, username, role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }
                          return userService
                              .reassignProjectManagers(requests, username, role)
                              .map(
                                  response -> {
                                    if (DashboardConstants.SUCCESS.equals(response.getStatus())) {
                                      return ResponseEntity.ok(response);
                                    } else {
                                      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                          .body(response);
                                    }
                                  });
                        }));
  }

  @DeleteMapping(value = "/pm/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Delete a PM user",
      description =
          "Deletes a PM user if they have no active BRDs. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "PM user removed successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class),
              examples =
                  @ExampleObject(
                      name = "Success response",
                      value =
                          """
                  {
                      "status": "success",
                      "message": "PM user removed successfully.",
                      "data": null,
                      "errors": null
                  }
                  """,
                      description = "PM user was successfully removed")))
  @ApiResponse(responseCode = "400", description = "User is not a PM")
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "409", description = "PM has active BRDs")
  public Mono<ResponseEntity<Api<String>>> deletePmUser(@PathVariable String userId) {
    log.info("Request received to delete PM user with ID: {}", userId);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        currentRole -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(currentRole)) {
                            log.warn(
                                "User {} with role {} attempted to delete a PM but is not authorized",
                                username,
                                currentRole);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .deletePmUser(userId)
                              .map(
                                  response -> {
                                    if (DashboardConstants.SUCCESS.equals(response.getStatus())) {
                                      return ResponseEntity.ok(response);
                                    } else if (response.getMessage().contains("not found")) {
                                      return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                          .body(response);
                                    } else if (response.getMessage().contains("Active BRD")) {
                                      return ResponseEntity.status(HttpStatus.CONFLICT)
                                          .body(response);
                                    } else {
                                      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                          .body(response);
                                    }
                                  });
                        }));
  }

  @DeleteMapping("/ba/{userId}")
  @Operation(
      summary = "Delete a BA user",
      description =
          "Deletes a BA user if they have no active BRD assignments. Only accessible by users with MANAGER role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "BA user deleted successfully",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "User is not a BA",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User does not have MANAGER role",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "409",
            description = "BA has active assignments",
            content = @Content(schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = Api.class)))
      })
  public Mono<ResponseEntity<Api<String>>> deleteBaUser(
      @Parameter(description = "ID of the BA user to delete", required = true) @PathVariable
          String userId) {

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        currentRole -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(currentRole)) {
                            log.warn(
                                "User {} with role {} attempted to delete BA user but is not authorized",
                                username,
                                currentRole);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .deleteBaUser(userId)
                              .map(
                                  response -> {
                                    if (ErrorValidationMessage.FAILURE.equals(
                                        response.getStatus())) {
                                      if (response.getMessage().contains("User not found")) {
                                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                            .body(response);
                                      } else if (response
                                          .getMessage()
                                          .contains("User is not a BA")) {
                                        return ResponseEntity.badRequest().body(response);
                                      } else if (response
                                          .getMessage()
                                          .contains("Active BRD assignment")) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                            .body(response);
                                      } else {
                                        return ResponseEntity.internalServerError().body(response);
                                      }
                                    }
                                    return ResponseEntity.ok(response);
                                  });
                        }));
  }

  @GetMapping("/require-password-change")
  @Operation(
      summary = "Check if user needs to change password",
      description =
          "Returns whether the authenticated user needs to change their password across any of their roles")
  @ApiResponse(
      responseCode = "200",
      description = "Successfully checked password change requirement",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples =
                  @ExampleObject(
                      value =
                          """
                  {
                      "status": "success",
                      "message": "Password change requirement checked successfully",
                      "data": {
                          "requirePasswordChange": true
                      },
                      "errors": null
                  }
                  """)))
  public Mono<ResponseEntity<Api<Map<String, Boolean>>>> requirePasswordChange() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap(
            auth -> {
              String email = auth.getName();
              List<String> roles =
                  auth.getAuthorities().stream()
                      .map(GrantedAuthority::getAuthority)
                      .map(role -> role.replace(ROLE_PREFIX, ""))
                      .toList();

              return userService.checkPasswordChangeRequired(email, roles).map(ResponseEntity::ok);
            });
  }

  @PostMapping(value = "/change-password", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Change user password",
      description =
          "Changes the user's password and updates the password change requirement flag. "
              + "The new password must be at least 8 characters long and contain at least one number and one special character (@#$%^&+=!).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password changed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or validation error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  public Mono<Api<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request,
      @AuthenticationPrincipal Authentication auth) {

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      Api<Void> response = new Api<>();
      response.setStatus(ErrorValidationMessage.FAILURE);
      response.setMessage("New password and confirm password do not match");
      response.setErrors(Optional.of(Map.of("error", "Passwords must match")));
      return Mono.just(response);
    }

    String email = auth.getName();
    String role =
        auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElseThrow(() -> new AccessDeniedException("User has no role"))
            .replace(ROLE_PREFIX, "");

    return userService.changePassword(email, role, request);
  }

  @PostMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Change user status (activate/inactivate)",
      description = "Allows MANAGER to activate/inactivate PM or BA users.")
  public Mono<ResponseEntity<Api<UserDetailResponse>>> changeUserStatus(
      @RequestBody @Valid ChangeUserStatusRequest request) {
    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          String normalizedRole =
                              role.startsWith(ROLE_PREFIX)
                                  ? role.substring(ROLE_PREFIX.length())
                                  : role;
                          if (!"MANAGER".equals(normalizedRole)) {
                            log.warn(
                                "User {} with role {} attempted to change user status but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException("Only MANAGER can change user status."));
                          }
                          return userService
                              .changeUserStatus(request.getUserId(), request.getNewStatus())
                              .map(ResponseEntity::ok);
                        }));
  }

  @PutMapping(value = "/{userId}/edit", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Edit user details",
      description = "Edits the details of a user. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "User details updated successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request or validation error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "409", description = "Email already in use")
  public Mono<ResponseEntity<Api<UserDetailResponse>>> updateUser(
      @PathVariable String userId,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal Authentication auth) {

    log.info("Request received to edit user details for userId: {}", userId);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to edit user details but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }
                          return userService
                              .updateUser(userId, request, username)
                              .map(ResponseEntity::ok);
                        }));
  }

  @DeleteMapping(value = "/{userId}/role", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Remove a role from a user",
      description =
          "Removes a specified role from a user's roles list. Only accessible to users with MANAGER role. Cannot remove the last role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Role removed successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request or validation error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "User not found")
  public Mono<ResponseEntity<Api<String>>> removeRole(
      @PathVariable String userId,
      @Valid @RequestBody RemoveRoleRequest request,
      @AuthenticationPrincipal Authentication auth) {

    log.info("Request received to remove role {} from user {}", request.getRole(), userId);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to remove role but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .removeRole(userId, request.getRole(), username)
                              .map(ResponseEntity::ok);
                        }));
  }

  @PutMapping(value = "/{userId}/role", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Add a role to a user",
      description =
          "Adds a specified role to a user's roles list. Only accessible to users with MANAGER role.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Role added successfully",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request or validation error",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Api.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Missing or invalid authentication token")
  @ApiResponse(responseCode = "403", description = "Access denied - User is not a Manager")
  @ApiResponse(responseCode = "404", description = "User not found")
  public Mono<ResponseEntity<Api<String>>> addRole(
      @PathVariable String userId,
      @Valid @RequestBody AddRoleRequest request,
      @AuthenticationPrincipal Authentication auth) {

    log.info("Request received to add role {} to user {}", request.getRole(), userId);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to add role but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(DashboardConstants.MANAGER_ONLY_MESSAGE));
                          }

                          return userService
                              .addRoleToUser(userId, request.getRole(), username)
                              .map(ResponseEntity::ok);
                        }));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Mono<Api<Void>> handleValidationErrors(WebExchangeBindException ex) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();

    Api<Void> response = new Api<>();
    response.setStatus(ErrorValidationMessage.FAILURE);
    response.setMessage(String.join(", ", errors));
    response.setErrors(Optional.of(Map.of("error", "Validation failed")));
    return Mono.just(response);
  }
}
