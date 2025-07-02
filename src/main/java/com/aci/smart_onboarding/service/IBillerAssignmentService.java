package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BillerAssignment;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;

/**
 * Service interface for managing biller assignments to BRDs. Defines the contract for assigning
 * billers to BRDs and managing their status.
 */
public interface IBillerAssignmentService {

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
  Mono<AssignBillerResponse> assignBiller(String brdId, AssignBillerRequest request);

  /**
   * Retrieves biller details for a specific BRD.
   *
   * @param brdId The ID of the BRD to get biller details for
   * @return A Mono emitting a Map containing the biller details (brdId and billerEmail)
   * @throws NotFoundException if the BRD or biller assignment doesn't exist
   */
  Mono<Map<String, String>> getBillerDetails(String brdId);

  /**
   * Retrieves all unique biller email addresses from the biller assignments.
   *
   * @return A Mono emitting a list of unique biller email addresses
   */
  Mono<List<String>> getAllBillerEmails();

  /**
   * Updates or creates a biller assignment record for the given BRD ID. If a record exists, only
   * updates the updatedAt field. If no record exists, creates a new record with all required
   * fields.
   *
   * @param brdId The ID of the BRD to update/create biller assignment for
   * @param billerEmail The email of the biller to assign
   * @return A Mono emitting the updated or created biller assignment
   */
  Mono<BillerAssignment> updateBillerEmail(String brdId, String billerEmail);

  /**
   * Check if a BRD is assigned to a specific biller
   *
   * @param brdId The BRD ID to check
   * @param billerEmail The biller's email
   * @return Mono<Boolean> true if assigned, false otherwise
   */
  Mono<Boolean> isBrdAssignedToBiller(String brdId, String billerEmail);

  /**
   * Check if the current user is authorized to access a BRD
   *
   * @param brdId The BRD ID to check
   * @return Mono<Api<AuthorizationResponse>> containing authorization result and message
   */
  Mono<Api<AuthorizationResponse>> checkBrdAuthorization(String brdId);

  /**
   * Retrieves all BRD IDs assigned to a specific biller email.
   *
   * @param billerEmail The biller email to search for
   * @return A Mono emitting a list of BRD IDs assigned to the biller
   */
  Mono<List<String>> getBrdsByBillerEmail(String billerEmail);

  /**
   * Retrieves all BRDs assigned to the current logged-in biller with proper security filtering.
   *
   * @return A Mono emitting BRDCountDataResponse containing filtered BRDs for the current biller
   */
  Mono<BRDCountDataResponse> getBrdsByCurrentBillerEmail();
}
