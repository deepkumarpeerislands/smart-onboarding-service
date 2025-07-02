package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.model.DatabaseSequence;
import com.aci.smart_onboarding.service.implementation.SequenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorServiceTest {

  @Mock private ReactiveMongoTemplate mongoTemplate;

  @InjectMocks private SequenceGeneratorService sequenceGeneratorService;

  @Captor private ArgumentCaptor<Query> queryCaptor;

  @Captor private ArgumentCaptor<Update> updateCaptor;

  @Captor private ArgumentCaptor<FindAndModifyOptions> optionsCaptor;

  private DatabaseSequence mockSequence;

  @BeforeEach
  void setUp() {
    mockSequence = new DatabaseSequence();
    mockSequence.setId("test_sequence");
    mockSequence.setSeq(42L);
  }

  @Test
  @DisplayName("generateSequence should increment and return next sequence value")
  void generateSequence_ShouldIncrementAndReturnNextValue() {
    // Arrange
    String sequenceName = "test_sequence";
    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.just(mockSequence));

    // Act & Assert
    StepVerifier.create(sequenceGeneratorService.generateSequence(sequenceName))
        .expectNext(42L)
        .verifyComplete();

    // Verify the correct query was built
    verify(mongoTemplate)
        .findAndModify(
            queryCaptor.capture(),
            updateCaptor.capture(),
            optionsCaptor.capture(),
            eq(DatabaseSequence.class));

    // Check the query parameters
    Query capturedQuery = queryCaptor.getValue();
    assertTrue(capturedQuery.getQueryObject().get("_id").toString().contains(sequenceName));

    // Check the update operation
    Update capturedUpdate = updateCaptor.getValue();
    assertTrue(capturedUpdate.getUpdateObject().toString().contains("$inc"));
    assertTrue(capturedUpdate.getUpdateObject().toString().contains("seq"));

    // Check the options
    FindAndModifyOptions capturedOptions = optionsCaptor.getValue();
    assertTrue(capturedOptions.isReturnNew());
    assertTrue(capturedOptions.isUpsert());
  }

  @Test
  @DisplayName("generateCommentId should generate formatted comment ID")
  void generateCommentId_ShouldGenerateFormattedCommentId() {
    // Arrange
    String brdId = "brd-123";
    String sectionName = "section1";
    String fieldPath = "field.path";

    // Expected sanitized key format: comment_brd-123_section1_field_path
    String expectedSequenceKey = "comment_brd-123_section1_field_path";

    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.just(mockSequence));

    // Act & Assert
    StepVerifier.create(sequenceGeneratorService.generateCommentId(brdId, sectionName, fieldPath))
        .expectNext("brd-123:section1:field.path:000042")
        .verifyComplete();

    // Verify the sequence key was properly sanitized
    verify(mongoTemplate)
        .findAndModify(
            queryCaptor.capture(),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class));

    Query capturedQuery = queryCaptor.getValue();
    assertTrue(capturedQuery.getQueryObject().get("_id").toString().contains(expectedSequenceKey));
  }

  @Test
  @DisplayName("generateReplyId should generate formatted reply ID")
  void generateReplyId_ShouldGenerateFormattedReplyId() {
    // Arrange
    String parentCommentId = "parent-comment-123";

    // Expected sanitized key format: reply_parent-comment-123
    String expectedSequenceKey = "reply_parent-comment-123";

    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.just(mockSequence));

    // Act & Assert
    StepVerifier.create(sequenceGeneratorService.generateReplyId(parentCommentId))
        .expectNext("parent-comment-123:reply:000042")
        .verifyComplete();

    // Verify the sequence key was properly sanitized
    verify(mongoTemplate)
        .findAndModify(
            queryCaptor.capture(),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class));

    Query capturedQuery = queryCaptor.getValue();
    assertTrue(capturedQuery.getQueryObject().get("_id").toString().contains(expectedSequenceKey));
  }

  @ParameterizedTest
  @DisplayName("sanitizeKey method should properly handle special characters")
  @CsvSource({
    "test.field, test_field",
    "field$name, field_name",
    "section name, section_name",
    "null, ''",
    "'', ''"
  })
  void sanitizeKey_ShouldHandleSpecialCharacters(String input, String expectedOutput) {
    // Arrange
    if ("null".equals(input)) {
      input = null;
    }

    // We'll test the sanitizeKey method indirectly through generateCommentId
    String brdId = "test";
    String sectionName = "test";

    // Mock sequence generation to return a fixed value
    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.just(mockSequence));

    // Act & Assert - We're testing that the sanitized field path becomes part of the sequence key
    sequenceGeneratorService.generateCommentId(brdId, sectionName, input).block();

    verify(mongoTemplate)
        .findAndModify(
            queryCaptor.capture(),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class));

    Query capturedQuery = queryCaptor.getValue();
    String capturedId = capturedQuery.getQueryObject().get("_id").toString();

    // The expectedOutput should be part of the sequence key
    if (input != null) {
      assertTrue(
          capturedId.contains(expectedOutput),
          "Expected sanitized value '" + expectedOutput + "' not found in: " + capturedId);
    }
  }

  @Test
  @DisplayName("generateSequence should handle errors properly")
  void generateSequence_ShouldHandleErrors() {
    // Arrange
    String sequenceName = "error_sequence";
    RuntimeException testException = new RuntimeException("Test exception");

    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.error(testException));

    // Act & Assert
    StepVerifier.create(sequenceGeneratorService.generateSequence(sequenceName))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException && "Test exception".equals(error.getMessage()))
        .verify();
  }

  @Test
  @DisplayName("generateCommentId should handle sequence generation errors")
  void generateCommentId_ShouldHandleErrors() {
    // Arrange
    String brdId = "brd-123";
    String sectionName = "section1";
    String fieldPath = "field.path";

    RuntimeException testException = new RuntimeException("Sequence generation failed");

    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(DatabaseSequence.class)))
        .thenReturn(Mono.error(testException));

    // Act & Assert
    StepVerifier.create(sequenceGeneratorService.generateCommentId(brdId, sectionName, fieldPath))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && "Sequence generation failed".equals(error.getMessage()))
        .verify();
  }
}
