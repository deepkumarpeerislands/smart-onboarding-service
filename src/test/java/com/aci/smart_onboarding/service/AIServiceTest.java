package com.aci.smart_onboarding.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.ai.factory.AIServiceFactory;
import com.aci.ai.services.IContextProvider;
import com.aci.ai.services.RxAIEmbeddings;
import com.aci.ai.services.RxAIService;
import com.aci.smart_onboarding.service.implementation.AIService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

  @Mock private AIServiceFactory aiServiceFactory;

  @Mock private IContextProvider contextProvider;

  @Mock private RxAIService reactiveAIService;

  @Mock private RxAIEmbeddings reactiveAIEmbeddings;

  @InjectMocks private AIService aiService;

  @Test
  void getEmbeddings_WithValidContext_ShouldReturnEmbeddingsList() {
    // Given
    List<Double> expectedEmbeddings = Arrays.asList(0.1, 0.2, 0.3);
    when(aiServiceFactory.getReactiveAIEmbeddings()).thenReturn(reactiveAIEmbeddings);
    when(reactiveAIEmbeddings.generateEmbeddings(anyString()))
        .thenReturn(Mono.just(expectedEmbeddings));

    // When
    Mono<List<Double>> result = aiService.getEmbeddings("test context");

    // Then
    StepVerifier.create(result).expectNext(expectedEmbeddings).verifyComplete();
  }

  @Test
  void generateAnswerAsStream_WithValidInput_ShouldReturnAnswerStream() {
    // Given
    String question = "test question";
    String context = "test context";
    String contextName = "test";

    when(aiServiceFactory.getReactiveAIService()).thenReturn(reactiveAIService);
    when(reactiveAIService.generateAnswerAsStream(question, context))
        .thenReturn(Flux.just("Answer", "stream", "response"));

    // When
    Flux<String> result = aiService.generateAnswerAsStream(question, context, contextName);

    // Then
    StepVerifier.create(result)
        .expectNext("Answer")
        .expectNext("stream")
        .expectNext("response")
        .verifyComplete();

    verify(contextProvider).setContextName(contextName);
  }

  @Test
  void generateAnswer_WithValidInput_ShouldReturnAnswer() {
    // Given
    String question = "test question";
    String context = "test context";
    String contextName = "test";
    String expectedAnswer = "test answer";

    when(aiServiceFactory.getReactiveAIService()).thenReturn(reactiveAIService);
    when(reactiveAIService.generateAnswer(question, context)).thenReturn(Mono.just(expectedAnswer));

    // When
    Mono<String> result = aiService.generateAnswer(question, context, contextName);

    // Then
    StepVerifier.create(result).expectNext(expectedAnswer).verifyComplete();

    verify(contextProvider).setContextName(contextName);
    verify(aiServiceFactory).getReactiveAIService();
    verify(reactiveAIService).generateAnswer(question, context);
  }
}
