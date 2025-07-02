package com.aci.smart_onboarding.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration class to create MongoDB indexes on application startup.
 * This ensures optimal query performance for frequently accessed fields.
 * Indexes are only created if they don't already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig implements CommandLineRunner {

  private final ReactiveMongoTemplate reactiveMongoTemplate;

  // Define required indexes
  private static final String INDEX_EMAIL = "idx_users_email";
  private static final String INDEX_ACTIVE_ROLE = "idx_users_activeRole";
  private static final String INDEX_ROLES = "idx_users_roles";
  private static final String INDEX_EMAIL_ACTIVE_ROLE = "idx_users_email_activeRole";
  private static final String INDEX_ACTIVE_ROLE_STATUS = "idx_users_activeRole_status";

  @Override
  public void run(String... args) throws Exception {
    log.info("Checking and creating MongoDB indexes for optimal performance...");
    
    checkAndCreateIndexes()
        .doOnSuccess(v -> log.info("MongoDB index check completed successfully"))
        .doOnError(error -> log.error("Error during MongoDB index check: {}", error.getMessage()))
        .subscribe();
  }

  /**
   * Checks existing indexes and creates only missing ones
   */
  private Mono<Void> checkAndCreateIndexes() {
    return reactiveMongoTemplate
        .indexOps("users")
        .getIndexInfo()
        .collectList()
        .flatMap(existingIndexes -> {
          Set<String> existingIndexNames = existingIndexes.stream()
              .map(IndexInfo::getName)
              .collect(Collectors.toSet());
          
          log.debug("Existing indexes: {}", existingIndexNames);
          
          return createMissingIndexes(existingIndexNames);
        })
        .then();
  }

  /**
   * Creates only the indexes that don't already exist
   */
  private Mono<Void> createMissingIndexes(Set<String> existingIndexes) {
    return Mono.empty()
        .then(createIndexIfMissing(existingIndexes, INDEX_EMAIL, 
            new Index().on("email", Sort.Direction.ASC).unique().named(INDEX_EMAIL)))
        .then(createIndexIfMissing(existingIndexes, INDEX_ACTIVE_ROLE,
            new Index().on("activeRole", Sort.Direction.ASC).named(INDEX_ACTIVE_ROLE)))
        .then(createIndexIfMissing(existingIndexes, INDEX_ROLES,
            new Index().on("roles", Sort.Direction.ASC).named(INDEX_ROLES)))
        .then(createIndexIfMissing(existingIndexes, INDEX_EMAIL_ACTIVE_ROLE,
            new Index().on("email", Sort.Direction.ASC).on("activeRole", Sort.Direction.ASC).named(INDEX_EMAIL_ACTIVE_ROLE)))
        .then(createIndexIfMissing(existingIndexes, INDEX_ACTIVE_ROLE_STATUS,
            new Index().on("activeRole", Sort.Direction.ASC).on("status", Sort.Direction.ASC).named(INDEX_ACTIVE_ROLE_STATUS)))
        .then();
  }

  /**
   * Creates an index only if it doesn't already exist
   */
  private Mono<Void> createIndexIfMissing(Set<String> existingIndexes, String indexName, Index index) {
    if (existingIndexes.contains(indexName)) {
      log.debug("Index '{}' already exists, skipping creation", indexName);
      return Mono.empty();
    }
    
    log.info("Creating missing index: {}", indexName);
    return reactiveMongoTemplate
        .indexOps("users")
        .ensureIndex(index)
        .doOnSuccess(v -> log.info("Successfully created index: {}", indexName))
        .doOnError(error -> log.error("Failed to create index '{}': {}", indexName, error.getMessage()))
        .then();
  }
} 