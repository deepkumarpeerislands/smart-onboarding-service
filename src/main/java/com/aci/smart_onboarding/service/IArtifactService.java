package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.model.Artifact;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface IArtifactService {
  List<String> getRelevantArtifactsDescription(String context);

  Mono<List<String>> findByDocumentId(String documentId);

  Flux<Artifact> findByCollectionId(String collectionId);

  Mono<List<String>> findByDocumentName(String documentName);

  /** Performs vector search across all artifacts */
  Flux<Artifact> performVectorSearch(List<Double> queryVector, int limit);

  /** Performs vector search with optional document name filtering */
  Flux<Artifact> performVectorSearch(List<Double> queryVector, int limit, String documentName);
}
