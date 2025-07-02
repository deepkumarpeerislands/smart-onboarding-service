package com.aci.smart_onboarding.service;

import reactor.core.publisher.Mono;

/** Service interface for generating sequence numbers for MongoDB document IDs */
public interface ISequenceGeneratorService {

  /**
   * Get next sequence number for the given sequence name
   *
   * @param sequenceName name of the sequence to get next value for
   * @return Mono with the next sequence value
   */
  Mono<Long> generateSequence(String sequenceName);

  /**
   * Generates a comment ID for a specific BRD field by combining the sequence with the context
   *
   * @param brdId BRD identifier
   * @param sectionName Section name
   * @param fieldPath Field path
   * @return Mono with the generated ID
   */
  Mono<String> generateCommentId(String brdId, String sectionName, String fieldPath);

  /**
   * Generates a reply comment ID for a specific parent comment
   *
   * @param parentCommentId Parent comment ID
   * @return Mono with the generated reply ID
   */
  Mono<String> generateReplyId(String parentCommentId);
}
