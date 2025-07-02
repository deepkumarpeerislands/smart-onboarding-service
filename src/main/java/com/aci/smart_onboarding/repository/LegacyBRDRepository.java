package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.LegacyBRD;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LegacyBRDRepository extends ReactiveMongoRepository<LegacyBRD, String> {
  // Find a legacy BRD by its BRDId field
  Mono<LegacyBRD> findByBrdId(String brdId);

  // You can add custom query methods here if needed
}
