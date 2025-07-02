package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.model.UATConfigurator;
import java.util.List;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Repository for UAT Configurator operations. */
@Repository
public interface UATConfiguratorRepository
    extends ReactiveMongoRepository<UATConfigurator, String> {

  /**
   * Find all configurations by portal type.
   *
   * @param type the portal type
   * @return flux of matching configurations
   */
  Flux<UATConfigurator> findByType(PortalTypes type);

  /**
   * Find all configurations by configuration names.
   *
   * @param configurationNames list of configuration names to search for
   * @return flux of matching configurations
   */
  Flux<UATConfigurator> findByConfigurationNameIn(List<String> configurationNames);
}
