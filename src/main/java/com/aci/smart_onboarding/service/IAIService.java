package com.aci.smart_onboarding.service;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface IAIService {
  Mono<List<Double>> getEmbeddings(String context);

  Flux<String> generateAnswerAsStream(String question, String context, String contextName);

  Mono<String> generateAnswer(String question, String context, String contextName);

  Mono<List<List<Double>>> getEmbeddingsForBatch(List<String> contexts);
}
