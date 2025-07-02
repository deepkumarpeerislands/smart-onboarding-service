package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.BRD;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface BRDRepository extends ReactiveMongoRepository<BRD, String> {
  Mono<Boolean> existsByBrdId(String brdId);

  Mono<Boolean> existsByBrdName(String brdName);

  Flux<BRD> findAllBy(Pageable pageable);

  Mono<BRD> findByBrdId(@NotBlank(message = "BRD ID cannot be blank") String brdId);

  Mono<BRD> findByBrdName(@NotBlank(message = "BRD name cannot be blank") String brdName);

  /**
   * Finds all BRDs created by a specific user
   *
   * @param creator The user ID of the creator
   * @return A Flux of BRDs created by the user
   */
  Flux<BRD> findByCreator(String creator);

  /**
   * Finds all BRDs by a list of BRD IDs
   *
   * @param brdIds List of BRD IDs to search for
   * @return A Flux of BRDs matching the provided IDs
   */
  Flux<BRD> findAllByBrdIdIn(List<String> brdIds);
}
