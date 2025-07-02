package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BrdFieldCommentGroupRepository
    extends ReactiveMongoRepository<BrdFieldCommentGroup, String> {
  // Primary query methods for BRD source type
  Mono<BrdFieldCommentGroup> findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
      String brdFormId, String sourceType, String sectionName, String fieldPath);

  Flux<BrdFieldCommentGroup> findByBrdFormIdAndSourceType(String brdFormId, String sourceType);

  Flux<BrdFieldCommentGroup> findByBrdFormIdAndSourceTypeAndSectionName(
      String brdFormId, String sourceType, String sectionName);

  // Query methods for SITE source type (includes both brdFormId and siteId)
  Mono<BrdFieldCommentGroup> findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
      String brdFormId, String siteId, String sourceType, String sectionName, String fieldPath);

  Flux<BrdFieldCommentGroup> findByBrdFormIdAndSiteIdAndSourceType(
      String brdFormId, String siteId, String sourceType);

  Flux<BrdFieldCommentGroup> findByBrdFormIdAndSiteIdAndSourceTypeAndSectionName(
      String brdFormId, String siteId, String sourceType, String sectionName);

  // Helper methods
  Flux<BrdFieldCommentGroup> findBySiteId(String siteId);

  Flux<BrdFieldCommentGroup> findByBrdFormId(String brdFormId);
}
