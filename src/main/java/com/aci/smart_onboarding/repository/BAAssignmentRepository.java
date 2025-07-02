package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.BAAssignment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BAAssignmentRepository extends ReactiveMongoRepository<BAAssignment, String> {
  Mono<BAAssignment> findByBrdId(String brdId);

  Mono<Boolean> existsByBrdId(String brdId);

  /**
   * Finds all BA assignments for a given BA email.
   *
   * @param baEmail The email of the BA
   * @return A Flux emitting all assignments for the BA
   */
  Flux<BAAssignment> findByBaEmail(String baEmail);
}
