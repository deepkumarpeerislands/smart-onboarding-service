package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.dto.BAEmailsResponse;
import com.aci.smart_onboarding.dto.BAInfo;
import com.aci.smart_onboarding.dto.BillerEmailsResponse;
import com.aci.smart_onboarding.dto.BillerInfo;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.config.AzureADConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for interacting with Azure Active Directory to fetch biller information.
 * Handles authentication, user retrieval, and filtering based on BRD names.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureADService {

  private final AzureADConfig azureADConfig;
  private final BRDValidationService brdValidationService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_RETRIES = 3;

  /**
   * Retrieves all biller emails from the users collection where roles contains "BILLER".
   *
   * @return A Mono emitting a BillerEmailsResponse containing all biller information
   * @throws RuntimeException if there's an error communicating with the database
   */
  public Mono<BillerEmailsResponse> getBillerEmailsByBrdName() {
    return userRepository
        .findByRolesRegex("^biller$")
        .map(
            user ->
                BillerInfo.builder()
                    .email(user.getEmail())
                    .displayName(user.getFirstName() + " " + user.getLastName())
                    .build())
        .collectList()
        .map(billers -> BillerEmailsResponse.builder().billerEmails(billers).build());
  }

  /**
   * Retrieves all BA emails from the users collection where roles contains "BA".
   *
   * @return A Mono emitting a BAEmailsResponse containing all BA information
   * @throws RuntimeException if there's an error communicating with the database
   */
  public Mono<BAEmailsResponse> getBAEmailsByBrdName() {
    return userRepository
        .findByRolesRegex("^ba$")
        .map(
            user ->
                BAInfo.builder()
                    .email(user.getEmail())
                    .displayName(user.getFirstName() + " " + user.getLastName())
                    .build())
        .collectList()
        .map(bas -> BAEmailsResponse.builder().baEmails(bas).build());
  }
}
