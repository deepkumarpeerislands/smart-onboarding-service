package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.BrdTemplateConfig;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BRDTemplateRepository extends ReactiveMongoRepository<BrdTemplateConfig, String> {
  Mono<BrdTemplateConfig> findByTemplateTypes(String templateType);
}
