package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.UserStatus;
import com.aci.smart_onboarding.model.User;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IUserService {

  /**
   * Creates a new user with the provided details
   *
   * @param userRequest The user creation request containing user details
   * @return A Mono containing ResponseEntity with Api wrapper containing the created user details
   */
  Mono<ResponseEntity<Api<UserResponse>>> createUser(UserRequest userRequest);

  /**
   * Retrieves user details by email
   *
   * @param email The email of the user to retrieve
   * @return A Mono containing ResponseEntity with Api wrapper containing the user details
   */
  Mono<ResponseEntity<Api<UserDetailResponse>>> getUserByEmail(String email);

  Flux<UserDetailResponse> getUsersByRole(String role);

  /**
   * Finds a user by their ID
   *
   * @param id The user's ID
   * @return A Mono containing the user if found
   */
  Mono<User> findById(String id);

  /**
   * Deletes a user by their ID
   *
   * @param id The user's ID
   * @return A Mono that completes when the user is deleted
   */
  Mono<Void> deleteById(String id);

  /**
   * Deletes a PM user if they have no active BRDs
   *
   * @param userId The ID of the PM user to delete
   * @return Api response indicating success or failure
   */
  Mono<Api<String>> deletePmUser(String userId);

  /**
   * Reassigns a BRD to a new Project Manager
   *
   * @param brdId The ID of the BRD to update
   * @param newPmUsername The username of the new Project Manager
   * @param modifiedBy The username of the user making the modification
   * @param userRole The role of the user making the modification
   * @return Api response
   */
  Mono<Api<Void>> reassignProjectManager(String brdId, String newPmUsername, String modifiedBy, String userRole);

  /**
   * Batch reassigns multiple BRDs to their respective new Project Managers
   *
   * @param requests List of reassignment requests containing BRD IDs and new PM usernames
   * @param modifiedBy The username of the user making the modification
   * @param userRole The role of the user making the modification
   * @return Api response with list of PMReassignmentResponse objects containing success/failure
   *     details
   */
  Mono<Api<List<PMReassignmentResponse>>> reassignProjectManagers(
      List<PMReassignmentRequest> requests, String modifiedBy, String userRole);

  Throwable handleErrors(Throwable ex);

  /**
   * Retrieves all PM and BA users
   *
   * @return A Mono containing ResponseEntity with Api wrapper containing the list of PM and BA
   *     users
   */
  Mono<ResponseEntity<Api<UserListResponse>>> getPMAndBAUsers();

  /**
   * Searches PM and BA users by name or email
   *
   * @param searchTerm The search term to filter users by name or email
   * @return A Mono containing ResponseEntity with Api wrapper containing filtered PM and BA users
   */
  Mono<ResponseEntity<Api<UserListResponse>>> searchPMAndBAUsers(String searchTerm);

  /**
   * Deletes a BA user if they have no active BRD assignments
   *
   * @param userId The ID of the BA user to delete
   * @return Api response indicating success or failure
   */
  Mono<Api<String>> deleteBaUser(String userId);

  /**
   * Finds a user by their email and role
   *
   * @param email The user's email
   * @param role The user's role
   * @return A Mono containing the user if found
   */
  Mono<User> findByEmailAndRole(String email, String role);

  /**
   * Checks if a user needs to change their password for any of their roles
   *
   * @param email The user's email
   * @param roles List of user's roles
   * @return A Mono containing the API response with the password change requirement status
   */
  Mono<Api<Map<String, Boolean>>> checkPasswordChangeRequired(String email, List<String> roles);

  /**
   * Changes a user's password and updates the password change requirement flag
   *
   * @param email The user's email
   * @param role The user's role
   * @param request The password change request containing current and new passwords
   * @return A Mono containing the API response
   */
  Mono<Api<Void>> changePassword(String email, String role, ChangePasswordRequest request);

  Mono<Api<UserDetailResponse>> changeUserStatus(String userId, UserStatus newStatus);

  /**
   * Updates user details
   *
   * @param userId ID of the user to update
   * @param request Update request containing new user details
   * @param adminId ID of the admin performing the update
   * @return Mono containing the updated user details
   */
  Mono<Api<UserDetailResponse>> updateUser(
      String userId, UpdateUserRequest request, String adminId);

  /**
   * Removes a role from a user
   *
   * @param userId The ID of the user
   * @param role The role to remove
   * @param modifiedBy The username of the user making the modification
   * @return Api response indicating success or failure
   */
  Mono<Api<String>> removeRole(String userId, String role, String modifiedBy);

  /**
   * Adds a role to a user
   *
   * @param userId The ID of the user
   * @param role The role to add
   * @param modifiedBy The username of the user making the modification
   * @return Api response indicating success or failure
   */
  Mono<Api<String>> addRoleToUser(String userId, String role, String modifiedBy);
}
