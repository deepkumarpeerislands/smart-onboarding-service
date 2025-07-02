package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.BRDSummaryResponse;
import com.aci.smart_onboarding.enums.ContextName;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface IAssistantService {
  Flux<String> askStreamingAssistant(String question, ContextName contextName, String documentName);

  Mono<String> askAssistant(String question, ContextName contextName, String documentName);

  Mono<JsonNode> prefillBRDProcessJson(
      JsonNode sections,
      List<String> documentNames,
      ContextName contextName,
      String additionalContext);

  /**
   * Backward compatibility method for prefilling BRD sections.
   *
   * @param sections The JSON sections to process
   * @param documentNames List of document names to search for relevant context
   * @return The processed JSON with prefilled sections
   */
  Mono<JsonNode> prefillBRDProcessJson(JsonNode sections, List<String> documentNames);

  /**
   * Generates a summary of a BRD document.
   *
   * @param brdId The ID of the BRD to summarize
   * @return A flux of BRDSummaryResponse objects containing the summary
   */
  Flux<BRDSummaryResponse> generateBRDSummary(String brdId);

  /**
   * Generates a Walletron summary for the given BRD ID
   *
   * @param brdId The ID of the BRD
   * @return A Mono containing the JsonNode with the Walletron summary
   */
  Mono<JsonNode> generateWalletronSummary(String brdId);
}
