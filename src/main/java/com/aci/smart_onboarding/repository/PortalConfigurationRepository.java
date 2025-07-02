package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.PortalConfiguration;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PortalConfigurationRepository
    extends ReactiveMongoRepository<PortalConfiguration, String> {
  Flux<PortalConfiguration> findByBrdId(String brdId);

  Flux<PortalConfiguration> findByType(PortalTypes type);

  Mono<PortalConfiguration> findByIdAndType(String id, PortalTypes type);

  Mono<PortalConfiguration> findByBrdIdAndType(String brdId, PortalTypes type);
}
