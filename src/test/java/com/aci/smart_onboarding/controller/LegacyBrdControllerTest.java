package com.aci.smart_onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.service.implementation.LegacyBrdService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LegacyBrdControllerTest {

  @Mock private LegacyBrdService legacyBrdService;

  @InjectMocks private LegacyBrdController legacyBrdController;

  private String testFileUrl;
  private List<GuidanceData> testGuidanceData;
  private List<BrdRules> testBrdRules;
  private LegacyBrdRequest testLegacyBrdRequest;
  private LegacyPrefillRequest testLegacyPrefillRequest;

  @BeforeEach
  void setUp() {
    testFileUrl = "https://example.com/test.txt";

    // Setup test GuidanceData
    GuidanceData guidanceData = new GuidanceData();
    guidanceData.setRuleName("Test Rule");
    guidanceData.setMappingKey("Test Key");
    testGuidanceData = Arrays.asList(guidanceData);

    // Setup test BrdRules
    BrdRules brdRule = new BrdRules();
    brdRule.setRuleName("Test Rule");
    brdRule.setRuleId("123");
    testBrdRules = Arrays.asList(brdRule);

    // Setup test LegacyBrdRequest
    testLegacyBrdRequest = new LegacyBrdRequest();
    testLegacyBrdRequest.setBrdId("test-brd-id");
    testLegacyBrdRequest.setBrdRulesFileUrl(testFileUrl);

    // Setup test LegacyPrefillRequest
    testLegacyPrefillRequest = new LegacyPrefillRequest();
    testLegacyPrefillRequest.setBrdId("test-brd-id");
    testLegacyPrefillRequest.setDocumentName("test-document");
  }

  @Test
  void getStandardData_Success() {
    // Given
    when(legacyBrdService.getStandardData(anyString())).thenReturn(Mono.just(testGuidanceData));

    // When
    Mono<ResponseEntity<Api<List<GuidanceData>>>> result =
        legacyBrdController.getStandardData(testFileUrl);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<List<GuidanceData>> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().equals(testGuidanceData);
            })
        .verifyComplete();
  }

  @Test
  void getStandardData_ServiceError() {
    // Given
    when(legacyBrdService.getStandardData(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Api<List<GuidanceData>>>> result =
        legacyBrdController.getStandardData(testFileUrl);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void getUserRules_Success() {
    // Given
    when(legacyBrdService.getUserRules(anyString())).thenReturn(Mono.just(testBrdRules));

    // When
    Mono<ResponseEntity<Api<List<BrdRules>>>> result =
        legacyBrdController.getUserRules(testFileUrl);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<List<BrdRules>> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().equals(testBrdRules);
            })
        .verifyComplete();
  }

  @Test
  void getUserRules_ServiceError() {
    // Given
    when(legacyBrdService.getUserRules(anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Api<List<BrdRules>>>> result =
        legacyBrdController.getUserRules(testFileUrl);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void getRulesWithData_Success() {
    // Given
    Resource testResource = new ByteArrayResource("test data".getBytes());
    when(legacyBrdService.getRulesWithData(any(LegacyBrdRequest.class)))
        .thenReturn(Mono.just(testResource));

    // When
    Mono<ResponseEntity<Resource>> result =
        legacyBrdController.getRulesWithData(testLegacyBrdRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              HttpHeaders headers = response.getHeaders();
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody() != null
                  && headers.getContentType().equals(MediaType.APPLICATION_JSON)
                  && headers
                      .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                      .equals("form-data; name=\"attachment\"; filename=\"combined_rules.json\"");
            })
        .verifyComplete();
  }

  @Test
  void getRulesWithData_ServiceError() {
    // Given
    when(legacyBrdService.getRulesWithData(any(LegacyBrdRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Resource>> result =
        legacyBrdController.getRulesWithData(testLegacyBrdRequest);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void prefillLegacyBrd_Success() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.just(true));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().equals(true);
            })
        .verifyComplete();
  }

  @Test
  void prefillLegacyBrd_Failure() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.just(false));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isPresent()
                  && api.getData().get().equals(false)
                  && api.getMessage() != null
                  && !api.getMessage().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void prefillLegacyBrd_ServiceError() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isEmpty()
                  && api.getMessage() != null
                  && !api.getMessage().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void getStandardData_EmptyResponse() {
    // Given
    when(legacyBrdService.getStandardData(anyString()))
        .thenReturn(Mono.just(Collections.emptyList()));

    // When
    Mono<ResponseEntity<Api<List<GuidanceData>>>> result =
        legacyBrdController.getStandardData(testFileUrl);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<List<GuidanceData>> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void getStandardData_InvalidUrl() {
    // Given
    String invalidUrl = "invalid-url";
    when(legacyBrdService.getStandardData(invalidUrl))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid URL")));

    // When
    Mono<ResponseEntity<Api<List<GuidanceData>>>> result =
        legacyBrdController.getStandardData(invalidUrl);

    // Then
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void getUserRules_EmptyResponse() {
    // Given
    when(legacyBrdService.getUserRules(anyString())).thenReturn(Mono.just(Collections.emptyList()));

    // When
    Mono<ResponseEntity<Api<List<BrdRules>>>> result =
        legacyBrdController.getUserRules(testFileUrl);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<List<BrdRules>> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void getUserRules_InvalidUrl() {
    // Given
    String invalidUrl = "invalid-url";
    when(legacyBrdService.getUserRules(invalidUrl))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid URL")));

    // When
    Mono<ResponseEntity<Api<List<BrdRules>>>> result = legacyBrdController.getUserRules(invalidUrl);

    // Then
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void getRulesWithData_EmptyRequest() {
    // Given
    LegacyBrdRequest emptyRequest = new LegacyBrdRequest();
    when(legacyBrdService.getRulesWithData(emptyRequest))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid request")));

    // When
    Mono<ResponseEntity<Resource>> result = legacyBrdController.getRulesWithData(emptyRequest);

    // Then
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void getRulesWithData_InvalidRequest() {
    // Given
    LegacyBrdRequest invalidRequest = new LegacyBrdRequest();
    invalidRequest.setBrdId(null);
    invalidRequest.setBrdRulesFileUrl("invalid-url");
    when(legacyBrdService.getRulesWithData(invalidRequest))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid request parameters")));

    // When
    Mono<ResponseEntity<Resource>> result = legacyBrdController.getRulesWithData(invalidRequest);

    // Then
    StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void prefillLegacyBrd_EmptyRequest() {
    // Given
    LegacyPrefillRequest emptyRequest = new LegacyPrefillRequest();
    when(legacyBrdService.prefillLegacyBRD(emptyRequest))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid request")));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result = legacyBrdController.prefillLegacyBrd(emptyRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isEmpty()
                  && api.getMessage().equals("Failed to prefill BRD")
                  && api.getErrors().isPresent()
                  && api.getErrors().get().containsKey("error")
                  && api.getErrors().get().get("error").equals("Invalid request");
            })
        .verifyComplete();
  }

  @Test
  void prefillLegacyBrd_InvalidRequest() {
    // Given
    LegacyPrefillRequest invalidRequest = new LegacyPrefillRequest();
    invalidRequest.setBrdId(null);
    invalidRequest.setDocumentName("");
    when(legacyBrdService.prefillLegacyBRD(invalidRequest))
        .thenReturn(Mono.error(new IllegalArgumentException("Invalid request parameters")));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(invalidRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isEmpty()
                  && api.getMessage().equals("Failed to prefill BRD")
                  && api.getErrors().isPresent()
                  && api.getErrors().get().containsKey("error")
                  && api.getErrors().get().get("error").equals("Invalid request parameters");
            })
        .verifyComplete();
  }

  @Test
  void prefillLegacyBrd_TimeoutError() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Request timeout")));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isEmpty()
                  && api.getMessage().equals("Failed to prefill BRD")
                  && api.getErrors().isPresent()
                  && api.getErrors().get().containsKey("error")
                  && api.getErrors().get().get("error").equals("Request timeout");
            })
        .verifyComplete();
  }

  @Test
  void getRulesWithData_TimeoutError() {
    // Given
    when(legacyBrdService.getRulesWithData(any(LegacyBrdRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Request timeout")));

    // When
    Mono<ResponseEntity<Resource>> result =
        legacyBrdController.getRulesWithData(testLegacyBrdRequest);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void getRulesWithData_SuccessWithHeaders() {
    // Given
    Resource testResource = new ByteArrayResource("test data".getBytes());
    when(legacyBrdService.getRulesWithData(any(LegacyBrdRequest.class)))
        .thenReturn(Mono.just(testResource));

    // When
    Mono<ResponseEntity<Resource>> result =
        legacyBrdController.getRulesWithData(testLegacyBrdRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              HttpHeaders headers = response.getHeaders();
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody() != null
                  && response.getBody().equals(testResource)
                  && headers.getContentType().equals(MediaType.APPLICATION_JSON)
                  && headers
                      .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                      .equals("form-data; name=\"attachment\"; filename=\"combined_rules.json\"");
            })
        .verifyComplete();
  }

  @Test
  void getRulesWithData_EmptyResource() {
    // Given
    Resource emptyResource = new ByteArrayResource(new byte[0]);
    when(legacyBrdService.getRulesWithData(any(LegacyBrdRequest.class)))
        .thenReturn(Mono.just(emptyResource));

    // When
    Mono<ResponseEntity<Resource>> result =
        legacyBrdController.getRulesWithData(testLegacyBrdRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              HttpHeaders headers = response.getHeaders();
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody() != null
                  && response.getBody().equals(emptyResource)
                  && headers.getContentType().equals(MediaType.APPLICATION_JSON)
                  && headers
                      .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                      .equals("form-data; name=\"attachment\"; filename=\"combined_rules.json\"");
            })
        .verifyComplete();
  }

  @Test
  void getRulesWithData_ErrorHandling() {
    // Given
    LegacyBrdRequest invalidRequest = new LegacyBrdRequest();
    invalidRequest.setBrdId(null);
    invalidRequest.setBrdRulesFileUrl("invalid-url");
    when(legacyBrdService.getRulesWithData(invalidRequest))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<Resource>> result = legacyBrdController.getRulesWithData(invalidRequest);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void prefillLegacyBrd_SuccessTrue() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.just(true));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("SUCCESS")
                  && api.getData().isPresent()
                  && api.getData().get().equals(true)
                  && api.getMessage().equals("BRD prefill operation completed successfully")
                  && api.getErrors().isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void prefillLegacyBrd_SuccessFalse() {
    // Given
    when(legacyBrdService.prefillLegacyBRD(any(LegacyPrefillRequest.class)))
        .thenReturn(Mono.just(false));

    // When
    Mono<ResponseEntity<Api<Boolean>>> result =
        legacyBrdController.prefillLegacyBrd(testLegacyPrefillRequest);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Boolean> api = response.getBody();
              return response.getStatusCode().is2xxSuccessful()
                  && api != null
                  && api.getStatus().equals("ERROR")
                  && api.getData().isPresent()
                  && api.getData().get().equals(false)
                  && api.getMessage()
                      .equals("BRD prefill operation failed - BRD not found or conversion issues")
                  && api.getErrors().isPresent()
                  && api.getErrors().get().containsKey("error")
                  && api.getErrors()
                      .get()
                      .get("error")
                      .equals(
                          "Unable to locate or process BRD with ID: "
                              + testLegacyPrefillRequest.getBrdId());
            })
        .verifyComplete();
  }
}
