package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.Artifact;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ArtifactRepository extends ReactiveMongoRepository<Artifact, String> {

  Flux<Artifact> findByDocumentId(String documentId);

  Flux<Artifact> findByCollectionId(String collectionId);

  Flux<Artifact> findByDocumentName(String documentName);
}
