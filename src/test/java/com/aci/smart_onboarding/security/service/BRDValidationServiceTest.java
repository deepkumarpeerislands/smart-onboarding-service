package com.aci.smart_onboarding.security.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.SpelEvaluationException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

@SpringBootTest(classes = {BRDValidationService.class, BRDValidationServiceTest.TestConfig.class})
class BRDValidationServiceTest {

  @Configuration
  @EnableCaching
  static class TestConfig {
    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("brdValidation");
    }
  }

  @MockBean private BRDRepository brdRepository;

  @Autowired private BRDValidationService brdValidationService;

  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    // Clear all caches before each test
    cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    reset(brdRepository);
  }

  @Test
  void validateBrdName_WithExistingBrd_ShouldReturnTrue() {
    // Given
    String brdName = "test-brd";
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName.trim())).thenReturn(Mono.just(brd));

    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    verify(brdRepository).findByBrdName(brdName.trim());
  }

  @Test
  void validateBrdName_WithNonExistentBrd_ShouldReturnFalse() {
    // Given
    String brdName = "non-existent-brd";
    when(brdRepository.findByBrdName(brdName.trim())).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(false)
        .verifyComplete();

    verify(brdRepository).findByBrdName(brdName.trim());
  }

  @Test
  void validateBrdName_WithNullBrdName_ShouldThrowSpelException() {
    // When & Then
    StepVerifier.create(
            Mono.defer(() -> brdValidationService.validateBrdName(null))
                .onErrorResume(SpelEvaluationException.class, Mono::error))
        .expectError(SpelEvaluationException.class)
        .verify();

    verify(brdRepository, never()).findByBrdName(anyString());
  }

  @Test
  void validateBrdName_WithEmptyBrdName_ShouldThrowIllegalArgumentException() {
    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName(""))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().equals("BRD name cannot be null or empty"))
        .verify();

    verify(brdRepository, never()).findByBrdName(anyString());
  }

  @Test
  void validateBrdName_WithBlankBrdName_ShouldThrowIllegalArgumentException() {
    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName("   "))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().equals("BRD name cannot be null or empty"))
        .verify();

    verify(brdRepository, never()).findByBrdName(anyString());
  }

  @Test
  void validateBrdName_WithWhitespaceBrdName_ShouldTrimAndValidate() {
    // Given
    String brdName = "  test-brd  ";
    String trimmedBrdName = brdName.trim();
    BRD brd = new BRD();
    brd.setBrdName(trimmedBrdName);
    when(brdRepository.findByBrdName(trimmedBrdName)).thenReturn(Mono.just(brd));

    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    verify(brdRepository).findByBrdName(trimmedBrdName);
  }

  @Test
  void validateBrdName_WithCachedResult_ShouldNotHitRepository() {
    // Given
    String brdName = "test-brd";
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // When & Then - First call
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Second call should use cached result
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called only once
    verify(brdRepository, times(1)).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithNegativeResult_ShouldNotCache() {
    // Given
    String brdName = "non-existent-brd";
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.empty());

    // When & Then - First call
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(false)
        .verifyComplete();

    // Second call should not use cached result
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(false)
        .verifyComplete();

    // Verify repository was called twice
    verify(brdRepository, times(2)).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithCaseInsensitiveCache_ShouldReuseCache() {
    // Given
    String brdName = "TEST-BRD";
    String lowerCaseBrdName = brdName.toLowerCase();
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // When & Then - First call with uppercase
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Second call with lowercase should use cached result
    StepVerifier.create(brdValidationService.validateBrdName(lowerCaseBrdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called only once
    verify(brdRepository, times(1)).findByBrdName(brdName);
  }

  @Test
  void evictBrdCache_WithSpecificBrd_ShouldClearCache() {
    // Given
    String brdName = "test-brd";
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // First call - will be cached
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Evict cache
    brdValidationService.evictBrdCache(brdName);

    // Second call - should hit repository again
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called twice
    verify(brdRepository, times(2)).findByBrdName(brdName);
  }

  @Test
  void evictBrdCache_WithCaseInsensitiveName_ShouldClearCache() {
    // Given
    String brdName = "TEST-BRD";
    String lowerCaseBrdName = brdName.toLowerCase();
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // First call - will be cached
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Evict cache using lowercase name
    brdValidationService.evictBrdCache(lowerCaseBrdName);

    // Second call - should hit repository again
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called twice
    verify(brdRepository, times(2)).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithConcurrentAccess_ShouldHandleMultipleRequests() {
    // Given
    String brdName = "TestBRD";
    when(brdRepository.findByBrdName(anyString())).thenReturn(Mono.empty());

    // When
    StepVerifier.create(
            Mono.zip(
                brdValidationService.validateBrdName(brdName),
                brdValidationService.validateBrdName(brdName),
                brdValidationService.validateBrdName(brdName)))
        .expectNext(Tuples.of(false, false, false))
        .verifyComplete();

    // Then
    verify(brdRepository, times(3)).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithRepositoryError_ShouldPropagateError() {
    // Given
    String brdName = "test-brd";
    RuntimeException error = new RuntimeException("Database error");
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.error(error));

    // When & Then
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Database error"))
        .verify();

    verify(brdRepository).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithRepositoryError_ShouldNotCacheError() {
    // Given
    String brdName = "test-brd";
    RuntimeException error = new RuntimeException("Database error");
    BRD brd = new BRD();
    brd.setBrdName(brdName);

    // First call returns error
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.error(error));

    // When & Then - First call
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectError(RuntimeException.class)
        .verify();

    // Second call returns success
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // When & Then - Second call
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called twice
    verify(brdRepository, times(2)).findByBrdName(brdName);
  }

  @Test
  void validateBrdName_WithExpiredCache_ShouldRefreshFromRepository() {
    // Given
    String brdName = "test-brd";
    BRD brd = new BRD();
    brd.setBrdName(brdName);
    when(brdRepository.findByBrdName(brdName)).thenReturn(Mono.just(brd));

    // First call - will be cached
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Clear cache manually
    cacheManager.getCache("brdValidation").clear();

    // Second call - should hit repository again
    StepVerifier.create(brdValidationService.validateBrdName(brdName))
        .expectNext(true)
        .verifyComplete();

    // Verify repository was called twice
    verify(brdRepository, times(2)).findByBrdName(brdName);
  }
}
