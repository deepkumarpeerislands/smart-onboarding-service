package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.BillerAssignment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * Repository interface for managing biller assignments. Provides methods to interact with the
 * biller assignment collection in MongoDB.
 */
@Repository
public interface BillerAssignmentRepository
    extends ReactiveMongoRepository<BillerAssignment, String> {

  /**
   * Checks if a biller is already assigned to a BRD.
   *
   * @param brdId The ID of the BRD
   * @param billerEmail The email of the biller
   * @return A Mono emitting true if the biller is assigned to the BRD, false otherwise
   */
  Mono<Boolean> existsByBrdIdAndBillerEmail(String brdId, String billerEmail);

  /**
   * Checks if a BRD already has a biller assigned.
   *
   * @param brdId The ID of the BRD
   * @return A Mono emitting true if the BRD has a biller assigned, false otherwise
   */
  Mono<Boolean> existsByBrdId(String brdId);

  /**
   * Finds the biller assignment for a specific BRD.
   *
   * @param brdId The ID of the BRD
   * @return A Mono emitting the biller assignment if found
   */
  Mono<BillerAssignment> findByBrdId(String brdId);

  /**
   * Finds all biller assignments for a specific biller email.
   *
   * @param billerEmail The email of the biller
   * @return A Flux emitting all biller assignments for the given email
   */
  Flux<BillerAssignment> findByBillerEmail(String billerEmail);
}
