package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.model.Artifact;
import com.aci.smart_onboarding.repository.ArtifactRepository;
import com.aci.smart_onboarding.service.IArtifactService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ArtifactService implements IArtifactService {

  private final ArtifactRepository artifactRepository;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  public ArtifactService(
      ArtifactRepository artifactRepository, ReactiveMongoTemplate reactiveMongoTemplate) {
    this.artifactRepository = artifactRepository;
    this.reactiveMongoTemplate = reactiveMongoTemplate;
  }

  @Override
  public List<String> getRelevantArtifactsDescription(String context) {
    return List.of();
  }

  @Override
  public Mono<List<String>> findByDocumentId(String documentId) {
    return artifactRepository.findByDocumentId(documentId).map(Artifact::getText).collectList();
  }

  @Override
  public Flux<Artifact> findByCollectionId(String collectionId) {
    return artifactRepository.findByCollectionId(collectionId);
  }

  @Override
  public Mono<List<String>> findByDocumentName(String documentName) {
    return artifactRepository.findByDocumentName(documentName).map(Artifact::getText).collectList();
  }

  @Override
  public Flux<Artifact> performVectorSearch(List<Double> vector, int limit) {
    return performVectorSearch(vector, limit, null);
  }

  @Override
  public Flux<Artifact> performVectorSearch(List<Double> vector, int limit, String documentName) {
    Map<String, Object> vectorSearchQuery = new HashMap<>();
    vectorSearchQuery.put("index", "vector_index");
    vectorSearchQuery.put("path", "vector");
    vectorSearchQuery.put("queryVector", vector);
    vectorSearchQuery.put("numCandidates", 150);
    vectorSearchQuery.put("limit", limit);
    vectorSearchQuery.put("similarity", "euclidean"); // Add similarity metric

    if (documentName != null && !documentName.isEmpty()) {
      Map<String, Object> filter = new HashMap<>();
      filter.put("document_name", documentName); // Change to match index field name
      vectorSearchQuery.put("filter", new Document(filter));
    }

    Document vectorSearchStage = new Document("$vectorSearch", new Document(vectorSearchQuery));

    AggregationOperation aggregationOperation = context -> vectorSearchStage;

    TypedAggregation<Artifact> aggregation =
        Aggregation.newAggregation(Artifact.class, aggregationOperation)
            .withOptions(AggregationOptions.builder().allowDiskUse(true).build());

    return reactiveMongoTemplate.aggregate(aggregation, Artifact.class, Artifact.class);
  }
}
