package com.aci.smart_onboarding.security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BAEmailsResponse;
import com.aci.smart_onboarding.dto.BAInfo;
import com.aci.smart_onboarding.dto.BillerEmailsResponse;
import com.aci.smart_onboarding.dto.BillerInfo;
import com.aci.smart_onboarding.security.service.AzureADService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AzureADControllerTest {

  private WebTestClient webTestClient;

  @Mock private AzureADService azureADService;

  @BeforeEach
  void setUp() {
    AzureADController azureADController = new AzureADController(azureADService);
    webTestClient = WebTestClient.bindToController(azureADController).build();
  }

  @Test
  void getBillerEmails_ShouldReturnBillerList() {
    // Given
    BillerInfo biller1 =
        BillerInfo.builder().email("biller1@example.com").displayName("Biller 1").build();
    BillerInfo biller2 =
        BillerInfo.builder().email("biller2@example.com").displayName("Biller 2").build();
    BillerEmailsResponse response =
        BillerEmailsResponse.builder().billerEmails(Arrays.asList(biller1, biller2)).build();

    when(azureADService.getBillerEmailsByBrdName()).thenReturn(Mono.just(response));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/billers")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new ParameterizedTypeReference<Api<BillerEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BillerEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals("success", apiResponse.getStatus(), "Status should be success");
              assertEquals(
                  "Successfully retrieved biller emails",
                  apiResponse.getMessage(),
                  "Message should match");
              BillerEmailsResponse responseData = apiResponse.getData().orElse(null);
              assertNotNull(responseData, "Response data should not be null");
              assertEquals(2, responseData.getBillerEmails().size(), "Should have 2 biller emails");

              // Verify all fields of first biller
              BillerInfo firstBiller = responseData.getBillerEmails().getFirst();
              assertEquals(
                  biller1.getEmail(), firstBiller.getEmail(), "First biller email should match");
              assertEquals(
                  biller1.getDisplayName(),
                  firstBiller.getDisplayName(),
                  "First biller display name should match");

              // Verify all fields of second biller
              BillerInfo secondBiller = responseData.getBillerEmails().get(1);
              assertEquals(
                  biller2.getEmail(), secondBiller.getEmail(), "Second biller email should match");
              assertEquals(
                  biller2.getDisplayName(),
                  secondBiller.getDisplayName(),
                  "Second biller display name should match");
            });

    verify(azureADService).getBillerEmailsByBrdName();
  }

  @Test
  void getBillerEmails_WithEmptyBillerList_ShouldReturnSuccess() {
    // Given
    BillerEmailsResponse response =
        BillerEmailsResponse.builder().billerEmails(Collections.emptyList()).build();

    when(azureADService.getBillerEmailsByBrdName()).thenReturn(Mono.just(response));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/billers")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new ParameterizedTypeReference<Api<BillerEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BillerEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals("success", apiResponse.getStatus(), "Status should be success");
              assertEquals(
                  "Successfully retrieved biller emails",
                  apiResponse.getMessage(),
                  "Message should match");
              BillerEmailsResponse responseData = apiResponse.getData().orElse(null);
              assertNotNull(responseData, "Response data should not be null");
              assertEquals(
                  0, responseData.getBillerEmails().size(), "Should have no biller emails");
            });
  }

  @Test
  void getBillerEmails_WithServiceFailure_ShouldReturnServerError() {
    // Given
    String errorMessage = "Service unavailable";

    when(azureADService.getBillerEmailsByBrdName())
        .thenReturn(Mono.error(new RuntimeException(errorMessage)));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/billers")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody(new ParameterizedTypeReference<Api<BillerEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BillerEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals(
                  SecurityConstants.ERROR, apiResponse.getStatus(), "Status should be error");
              assertEquals(errorMessage, apiResponse.getMessage(), "Error message should match");
              assertEquals(Optional.empty(), apiResponse.getData(), "Data should be empty");
            });
  }

  @Test
  void getBAEmails_ShouldReturnBAList() {
    // Given
    BAInfo ba1 = BAInfo.builder().email("ba1@example.com").displayName("BA 1").build();
    BAInfo ba2 = BAInfo.builder().email("ba2@example.com").displayName("BA 2").build();
    BAEmailsResponse response =
        BAEmailsResponse.builder().baEmails(Arrays.asList(ba1, ba2)).build();

    when(azureADService.getBAEmailsByBrdName()).thenReturn(Mono.just(response));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/ba")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new ParameterizedTypeReference<Api<BAEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BAEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals("success", apiResponse.getStatus(), "Status should be success");
              assertEquals(
                  "Successfully retrieved BA emails",
                  apiResponse.getMessage(),
                  "Message should match");
              BAEmailsResponse responseData = apiResponse.getData().orElse(null);
              assertNotNull(responseData, "Response data should not be null");
              assertEquals(2, responseData.getBaEmails().size(), "Should have 2 BA emails");

              // Verify all fields of first BA
              BAInfo firstBA = responseData.getBaEmails().getFirst();
              assertEquals(ba1.getEmail(), firstBA.getEmail(), "First BA email should match");
              assertEquals(
                  ba1.getDisplayName(),
                  firstBA.getDisplayName(),
                  "First BA display name should match");

              // Verify all fields of second BA
              BAInfo secondBA = responseData.getBaEmails().get(1);
              assertEquals(ba2.getEmail(), secondBA.getEmail(), "Second BA email should match");
              assertEquals(
                  ba2.getDisplayName(),
                  secondBA.getDisplayName(),
                  "Second BA display name should match");
            });

    verify(azureADService).getBAEmailsByBrdName();
  }

  @Test
  void getBAEmails_WithEmptyBAList_ShouldReturnSuccess() {
    // Given
    BAEmailsResponse response =
        BAEmailsResponse.builder().baEmails(Collections.emptyList()).build();

    when(azureADService.getBAEmailsByBrdName()).thenReturn(Mono.just(response));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/ba")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new ParameterizedTypeReference<Api<BAEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BAEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals("success", apiResponse.getStatus(), "Status should be success");
              assertEquals(
                  "Successfully retrieved BA emails",
                  apiResponse.getMessage(),
                  "Message should match");
              BAEmailsResponse responseData = apiResponse.getData().orElse(null);
              assertNotNull(responseData, "Response data should not be null");
              assertEquals(0, responseData.getBaEmails().size(), "Should have no BA emails");
            });
  }

  @Test
  void getBAEmails_WithServiceFailure_ShouldReturnServerError() {
    // Given
    String errorMessage = "Service unavailable";

    when(azureADService.getBAEmailsByBrdName())
        .thenReturn(Mono.error(new RuntimeException(errorMessage)));

    // When & Then
    webTestClient
        .get()
        .uri("/api/v1/azure/ba")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody(new ParameterizedTypeReference<Api<BAEmailsResponse>>() {})
        .consumeWith(
            result -> {
              Api<BAEmailsResponse> apiResponse = result.getResponseBody();
              assertNotNull(apiResponse, "API response should not be null");
              assertEquals(
                  SecurityConstants.ERROR, apiResponse.getStatus(), "Status should be error");
              assertEquals(errorMessage, apiResponse.getMessage(), "Error message should match");
              assertEquals(Optional.empty(), apiResponse.getData(), "Data should be empty");
            });
  }
}
