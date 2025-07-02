package com.aci.smart_onboarding.service.implementation;

import com.aci.ai.factory.AIServiceFactory;
import com.aci.ai.services.IContextProvider;
import com.aci.smart_onboarding.service.IAIService;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AIService implements IAIService {

  private final AIServiceFactory aiServiceFactory;
  private final IContextProvider iContextProvider;

  public AIService(AIServiceFactory aiServiceFactory, IContextProvider iContextProvider) {
    this.aiServiceFactory = aiServiceFactory;
    this.iContextProvider = iContextProvider;
    this.iContextProvider.setContextName("chat");
  }

  @Override
  public Mono<List<Double>> getEmbeddings(String context) {
    return aiServiceFactory.getReactiveAIEmbeddings().generateEmbeddings(context);
  }

  @Override
  public Flux<String> generateAnswerAsStream(String question, String context, String contextName) {
    iContextProvider.setContextName(contextName);
    return aiServiceFactory.getReactiveAIService().generateAnswerAsStream(question, context);
  }

  @Override
  public Mono<String> generateAnswer(String question, String context, String contextName) {
    iContextProvider.setContextName(contextName);
    return aiServiceFactory.getReactiveAIService().generateAnswer(question, context);
  }

  @Override
  public Mono<List<List<Double>>> getEmbeddingsForBatch(List<String> contexts) {
    // Filter out null values and process each context individually
    return Flux.fromIterable(contexts)
        .filter(context -> context != null && !context.isEmpty())
        .flatMap(this::getEmbeddings)
        .collectList();
  }
}
