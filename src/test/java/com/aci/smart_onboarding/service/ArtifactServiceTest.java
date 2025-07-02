package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.model.Artifact;
import com.aci.smart_onboarding.repository.ArtifactRepository;
import com.aci.smart_onboarding.service.implementation.ArtifactService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

  @Mock private ArtifactRepository artifactRepository;

  @Mock private ReactiveMongoTemplate reactiveMongoTemplate;

  @InjectMocks private ArtifactService artifactService;

  private Artifact artifact1;
  private Artifact artifact2;
  private List<Double> testVector;
  private double[] testVectorArray;
  private String testDocumentId;
  private String testCollectionId;
  private String testDocumentName;

  @BeforeEach
  void setUp() {
    testDocumentId = "test-doc-id";
    testCollectionId = "test-collection-id";
    testDocumentName = "test-doc-name";
    testVector = Arrays.asList(0.1, 0.2, 0.3);
    testVectorArray = testVector.stream().mapToDouble(Double::doubleValue).toArray();

    artifact1 = new Artifact();
    artifact1.setId("1");
    artifact1.setText("Text 1");
    artifact1.setDocumentId(testDocumentId);
    artifact1.setCollectionId(testCollectionId);
    artifact1.setDocumentName(testDocumentName);
    artifact1.setVector(testVectorArray);

    artifact2 = new Artifact();
    artifact2.setId("2");
    artifact2.setText("Text 2");
    artifact2.setDocumentId(testDocumentId);
    artifact2.setCollectionId(testCollectionId);
    artifact2.setDocumentName(testDocumentName);
    artifact2.setVector(testVectorArray);
  }

  @Test
  void getRelevantArtifactsDescription_WithEmptyContext_ShouldReturnEmptyList() {
    // Given
    String context = "";

    // When
    List<String> result = artifactService.getRelevantArtifactsDescription(context);

    // Then
    assertNotNull(result);
    assertEquals(Collections.emptyList(), result);
  }

  @Test
  void getRelevantArtifactsDescription_WithNullContext_ShouldReturnEmptyList() {
    // Given
    String context = null;

    // When
    List<String> result = artifactService.getRelevantArtifactsDescription(context);

    // Then
    assertNotNull(result);
    assertEquals(Collections.emptyList(), result);
  }

  @Test
  void findByDocumentId_WithValidId_ShouldReturnTextList() {
    // Given
    when(artifactRepository.findByDocumentId(testDocumentId))
        .thenReturn(Flux.just(artifact1, artifact2));

    // When
    Mono<List<String>> result = artifactService.findByDocumentId(testDocumentId);

    // Then
    StepVerifier.create(result).expectNext(Arrays.asList("Text 1", "Text 2")).verifyComplete();

    verify(artifactRepository).findByDocumentId(testDocumentId);
  }

  @Test
  void findByDocumentId_WithEmptyId_ShouldReturnEmptyList() {
    // Given
    when(artifactRepository.findByDocumentId("")).thenReturn(Flux.empty());

    // When
    Mono<List<String>> result = artifactService.findByDocumentId("");

    // Then
    StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();
  }

  @Test
  void findByDocumentId_WithNullId_ShouldReturnEmptyList() {
    // Given
    when(artifactRepository.findByDocumentId(null)).thenReturn(Flux.empty());

    // When
    Mono<List<String>> result = artifactService.findByDocumentId(null);

    // Then
    StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();
  }

  @Test
  void findByCollectionId_WithValidId_ShouldReturnArtifacts() {
    // Given
    when(artifactRepository.findByCollectionId(testCollectionId))
        .thenReturn(Flux.just(artifact1, artifact2));

    // When
    Flux<Artifact> result = artifactService.findByCollectionId(testCollectionId);

    // Then
    StepVerifier.create(result).expectNext(artifact1).expectNext(artifact2).verifyComplete();

    verify(artifactRepository).findByCollectionId(testCollectionId);
  }

  @Test
  void findByCollectionId_WithEmptyId_ShouldReturnEmptyFlux() {
    // Given
    when(artifactRepository.findByCollectionId("")).thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.findByCollectionId("");

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void findByCollectionId_WithNullId_ShouldReturnEmptyFlux() {
    // Given
    when(artifactRepository.findByCollectionId(null)).thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.findByCollectionId(null);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void findByDocumentName_WithValidName_ShouldReturnTextList() {
    // Given
    when(artifactRepository.findByDocumentName(testDocumentName))
        .thenReturn(Flux.just(artifact1, artifact2));

    // When
    Mono<List<String>> result = artifactService.findByDocumentName(testDocumentName);

    // Then
    StepVerifier.create(result).expectNext(Arrays.asList("Text 1", "Text 2")).verifyComplete();

    verify(artifactRepository).findByDocumentName(testDocumentName);
  }

  @Test
  void findByDocumentName_WithEmptyName_ShouldReturnEmptyList() {
    // Given
    when(artifactRepository.findByDocumentName("")).thenReturn(Flux.empty());

    // When
    Mono<List<String>> result = artifactService.findByDocumentName("");

    // Then
    StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();
  }

  @Test
  void findByDocumentName_WithNullName_ShouldReturnEmptyList() {
    // Given
    when(artifactRepository.findByDocumentName(null)).thenReturn(Flux.empty());

    // When
    Mono<List<String>> result = artifactService.findByDocumentName(null);

    // Then
    StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();
  }

  @Test
  void performVectorSearch_WithValidVector_ShouldReturnMatchingArtifacts() {
    // Given
    int limit = 10;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.just(artifact1, artifact2));

    // When
    Flux<Artifact> result = artifactService.performVectorSearch(testVector, limit);

    // Then
    StepVerifier.create(result).expectNext(artifact1).expectNext(artifact2).verifyComplete();

    verify(reactiveMongoTemplate)
        .aggregate(any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class));
  }

  @Test
  void performVectorSearch_WithValidVectorAndDocumentName_ShouldReturnMatchingArtifacts() {
    // Given
    int limit = 10;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.just(artifact1, artifact2));

    // When
    Flux<Artifact> result =
        artifactService.performVectorSearch(testVector, limit, testDocumentName);

    // Then
    StepVerifier.create(result).expectNext(artifact1).expectNext(artifact2).verifyComplete();

    verify(reactiveMongoTemplate)
        .aggregate(any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class));
  }

  @Test
  void performVectorSearch_WithEmptyVector_ShouldReturnEmptyFlux() {
    // Given
    int limit = 10;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.performVectorSearch(Collections.emptyList(), limit);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void performVectorSearch_WithNullVector_ShouldReturnEmptyFlux() {
    // Given
    int limit = 10;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.performVectorSearch(null, limit);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void performVectorSearch_WithZeroLimit_ShouldReturnEmptyFlux() {
    // Given
    int limit = 0;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.performVectorSearch(testVector, limit);

    // Then
    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void performVectorSearch_WithNegativeLimit_ShouldReturnEmptyFlux() {
    // Given
    int limit = -1;
    when(reactiveMongoTemplate.aggregate(
            any(TypedAggregation.class), eq(Artifact.class), eq(Artifact.class)))
        .thenReturn(Flux.empty());

    // When
    Flux<Artifact> result = artifactService.performVectorSearch(testVector, limit);

    // Then
    StepVerifier.create(result).verifyComplete();
  }
}
