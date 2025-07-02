package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.dto.UserProjection;
import com.aci.smart_onboarding.model.User;
import java.util.List;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

  Mono<User> findByEmail(String email);

  /**
   * Find user by email and role, checking both legacy role field and new roles list The role
   * parameter can be provided with or without the ROLE_ prefix
   *
   * @param email The user's email
   * @param role The role to search for (with or without ROLE_ prefix)
   * @return Mono of User if found
   */
  @Query(
      "{ "
          + "'email': ?0, "
          + "$or: ["
          + "  { 'activeRole': ?1 }, "
          + "  { 'activeRole': { $regex: '^ROLE_.*' } }, "
          + "  { 'activeRole': { $regex: '^(?!ROLE_).*' } }, "
          + "  { 'roles': ?1 }, "
          + "  { 'roles': { $regex: '^ROLE_.*' } }, "
          + "  { 'roles': { $regex: '^(?!ROLE_).*' } }"
          + "] }")
  Mono<User> findByEmailAndRole(String email, String role);

  /**
   * Find users by their roles with projection
   *
   * @param roles List of roles to filter by (without ROLE_ prefix)
   * @return Flux of UserProjection containing only required fields
   */
  @Query(
      value = "{ 'activeRole': { $in: ?0 } }",
      fields =
          "{ '_id': 1, 'firstName': 1, 'lastName': 1, 'email': 1, 'activeRole': 1, 'roles': 1, 'status': 1, 'createdAt': 1 }")
  Flux<UserProjection> findByRoleInProjected(List<String> roles);

  /**
   * Find users by their roles with projection, checking both legacy role field and new roles list
   *
   * @param roles List of roles to filter by (without ROLE_ prefix)
   * @return Flux of UserProjection containing only required fields
   */
  @Query(
      value = "{ $or: [ { 'activeRole': { $in: ?0 } }, { 'roles': { $elemMatch: { $in: ?0 } } } ] }",
      fields =
          "{ '_id': 1, 'firstName': 1, 'lastName': 1, 'email': 1, 'activeRole': 1, 'roles': 1, 'status': 1, 'createdAt': 1 }")
  Flux<UserProjection> findByRoleInOrRolesInProjected(List<String> roles);

  @Query(
      value =
          """
      {
          $or: [ { 'activeRole': { $in: ?0 } }, { 'roles': { $elemMatch: { $in: ?0 } } } ],
          $text: { $search: ?1 }
      }
      """,
      fields = "{ '_id': 1, 'firstName': 1, 'lastName': 1, 'email': 1, 'activeRole': 1, 'roles': 1 }")
  Flux<UserProjection> searchByRoleAndTextProjected(List<String> roles, String searchTerm);

  @Query(
      value =
          """
      {
          $or: [ { 'activeRole': { $in: ?0 } }, { 'roles': { $elemMatch: { $in: ?0 } } } ],
          $or: [
              { 'firstName': { $regex: ?1, $options: 'i' } },
              { 'lastName': { $regex: ?1, $options: 'i' } },
              { 'email': { $regex: ?1, $options: 'i' } },
              {
                  $and: [
                      { 'firstName': { $regex: ?2, $options: 'i' } },
                      { 'lastName': { $regex: ?3, $options: 'i' } }
                  ]
              }
          ]
      }
      """,
      fields = "{ '_id': 1, 'firstName': 1, 'lastName': 1, 'email': 1, 'activeRole': 1, 'roles': 1 }")
  Flux<UserProjection> searchByRoleAndNameOrEmailProjectedOptimized(
      List<String> roles, String searchTermLower, String firstNameLower, String lastNameLower);

  Mono<Boolean> existsByEmail(String email);

  @Query("{ 'activeRole': ?0 }")
  Flux<User> findByRole(String role);

  /**
   * Find users that have a specific role in their roles list
   *
   * @param role The role to search for (without ROLE_ prefix)
   * @return Flux of users that have the specified role
   */
  @Query("{ 'roles': { $in: [?0] } }")
  Flux<User> findByRoles(String role);

  @Query("{ 'roles': { $in: ?0 } }")
  Flux<User> findByRolesIn(List<String> roles);

  /**
   * Find users that have a specific role in their roles list (case-insensitive)
   *
   * @param rolePattern The regex pattern to search for (e.g., ^biller$)
   * @return Flux of users that have the specified role (case-insensitive)
   */
  @Query("{ 'roles': { $in: [ { $regex: ?0, $options: 'i' } ] } }")
  Flux<User> findByRolesRegex(String rolePattern);
}
