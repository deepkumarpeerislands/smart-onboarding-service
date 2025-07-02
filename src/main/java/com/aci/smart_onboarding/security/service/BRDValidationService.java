package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.repository.BRDRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for validating BRD (Business Requirements Document) names. Provides caching
 * to improve performance for frequently accessed BRD names.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BRDValidationService {

  private final BRDRepository brdRepository;

  /**
   * Validates if a BRD with the given name exists in the system. Results are cached to improve
   * performance for repeated queries.
   *
   * @param brdName The name of the BRD to validate
   * @return A Mono emitting true if the BRD exists, false otherwise
   * @throws IllegalArgumentException if the brdName is null or empty
   */
  @Cacheable(
      value = "brdValidation",
      key = "'BRD_' + #brdName.trim().toLowerCase()",
      unless = "#result == false")
  public Mono<Boolean> validateBrdName(String brdName) {
    if (brdName == null || brdName.trim().isEmpty()) {
      log.warn("Invalid BRD name provided: {}", brdName);
      return Mono.error(new IllegalArgumentException("BRD name cannot be null or empty"));
    }

    String normalizedBrdName = brdName.trim();
    return brdRepository
        .findByBrdName(normalizedBrdName)
        .map(brd -> true)
        .defaultIfEmpty(false)
        .doOnNext(
            exists -> log.debug("BRD validation result for {}: {}", normalizedBrdName, exists));
  }

  /**
   * Evicts the cache for a specific BRD name. This should be called when a BRD is updated or
   * deleted.
   *
   * @param brdName The name of the BRD to evict from cache
   */
  @CacheEvict(value = "brdValidation", key = "'BRD_' + #brdName.trim().toLowerCase()")
  public void evictBrdCache(String brdName) {
    log.debug("Evicting cache for BRD: {}", brdName);
  }
}
