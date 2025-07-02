package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.Site;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SiteRepository extends ReactiveMongoRepository<Site, String> {
  Flux<Site> findByBrdId(String brdId);

  Mono<Boolean> existsByBrdIdAndIdentifierCode(String brdId, String identifierCode);

  /**
   * Find a site by both brdId and siteId
   *
   * @param brdId The BRD ID to search for
   * @param siteId The site ID to search for
   * @return A Mono containing the site if found, or empty if not found
   */
  Mono<Site> findByBrdIdAndSiteId(String brdId, String siteId);
}
