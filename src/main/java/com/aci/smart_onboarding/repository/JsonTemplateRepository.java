package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.JsonTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Repository interface for JsonTemplate operations. */
@Repository
public interface JsonTemplateRepository extends ReactiveMongoRepository<JsonTemplate, String> {

  /**
   * Finds a JSON template by template name.
   *
   * @param templateName The unique template name
   * @return Mono containing the JsonTemplate if found
   */
  Mono<JsonTemplate> findByTemplateName(String templateName);

  /**
   * Checks if a template exists by template name.
   *
   * @param templateName The template name to check
   * @return Mono containing true if exists, false otherwise
   */
  Mono<Boolean> existsByTemplateName(String templateName);

  /**
   * Deletes a JSON template by template name.
   *
   * @param templateName The template name to delete
   * @return Mono containing the number of deleted documents
   */
  Mono<Long> deleteByTemplateName(String templateName);
}
