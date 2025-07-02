package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.AssignBARequest;
import com.aci.smart_onboarding.dto.AssignBAResponse;
import com.aci.smart_onboarding.dto.BAReassignmentRequest;
import com.aci.smart_onboarding.model.BAAssignment;
import java.util.List;
import reactor.core.publisher.Mono;

public interface IBAAssignmentService {
  Mono<AssignBAResponse> assignBA(String brdId, AssignBARequest request);

  /**
   * Retrieves all unique BA email addresses from the ba_assignments table.
   *
   * @return A Mono emitting a list of unique BA email addresses
   */
  Mono<List<String>> getAllBaEmails();

  /**
   * Updates or creates a BA assignment record for the given BRD ID. If a record exists, only
   * updates the updatedAt field. If no record exists, creates a new record with all required
   * fields.
   *
   * @param brdId The ID of the BRD to update/create BA assignment for
   * @param baEmail The email of the BA to assign
   * @return A Mono emitting the updated or created BA assignment
   */
  Mono<BAAssignment> updateBaEmail(String brdId, String baEmail);

  /**
   * Retrieves all BA assignments for a given BA username (email).
   *
   * @param username The email of the BA to find assignments for
   * @return A Mono emitting a list of BA assignments
   */
  Mono<List<BAAssignment>> getAssignmentsByBaUsername(String username);

  /**
   * Batch reassigns multiple BRDs to their respective new BAs. Only updates the baEmail field and
   * updatedAt timestamp for each BRD.
   *
   * @param requests List of reassignment requests containing BRD IDs and new BA usernames
   * @return A Mono emitting the API response with success/failure details
   */
  Mono<Api<List<String>>> reassignBAs(List<BAReassignmentRequest> requests);


  /**
   * Checks if a specific BA user is assigned to a specific BRD.
   *
   * @param brdId The ID of the BRD to check
   * @param baEmail The email of the BA to check
   * @return A Mono emitting true if the specific BA is assigned, false otherwise
   */
  Mono<Boolean> isBAAssignedToUser(String brdId, String baEmail);
}
