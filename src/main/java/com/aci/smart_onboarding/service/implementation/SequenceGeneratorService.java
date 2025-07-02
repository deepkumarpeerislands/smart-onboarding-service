package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.model.DatabaseSequence;
import com.aci.smart_onboarding.service.ISequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service implementation to generate sequence numbers for MongoDB document IDs */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService implements ISequenceGeneratorService {

  private final ReactiveMongoTemplate mongoTemplate;

  /**
   * Get next sequence number for the given sequence name
   *
   * @param sequenceName name of the sequence to get next value for
   * @return Mono with the next sequence value
   */
  @Override
  public Mono<Long> generateSequence(String sequenceName) {
    return mongoTemplate
        .findAndModify(
            Query.query(Criteria.where("_id").is(sequenceName)),
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            DatabaseSequence.class)
        .map(DatabaseSequence::getSeq);
  }

  /**
   * Generates a comment ID for a specific BRD field by combining the sequence with the context
   *
   * @param brdId BRD identifier
   * @param sectionName Section name
   * @param fieldPath Field path
   * @return Mono with the generated ID
   */
  @Override
  public Mono<String> generateCommentId(String brdId, String sectionName, String fieldPath) {
    String sequenceKey =
        String.format(
            "comment_%s_%s_%s",
            sanitizeKey(brdId), sanitizeKey(sectionName), sanitizeKey(fieldPath));

    return generateSequence(sequenceKey)
        .map(sequence -> String.format("%s:%s:%s:%06d", brdId, sectionName, fieldPath, sequence));
  }

  /**
   * Generates a reply comment ID for a specific parent comment
   *
   * @param parentCommentId Parent comment ID
   * @return Mono with the generated reply ID
   */
  @Override
  public Mono<String> generateReplyId(String parentCommentId) {
    String sequenceKey = String.format("reply_%s", sanitizeKey(parentCommentId));

    return generateSequence(sequenceKey)
        .map(sequence -> String.format("%s:reply:%06d", parentCommentId, sequence));
  }

  /**
   * Sanitizes keys for MongoDB by replacing dots and other special characters
   *
   * @param key The key to sanitize
   * @return Sanitized key
   */
  private String sanitizeKey(String key) {
    if (key == null) return "";
    return key.replaceAll("[.\\s\\$]", "_");
  }
}
