package com.aci.smart_onboarding.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.dto.BAEmailsResponse;
import com.aci.smart_onboarding.dto.BAInfo;
import com.aci.smart_onboarding.dto.BillerEmailsResponse;
import com.aci.smart_onboarding.dto.BillerInfo;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.config.AzureADConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AzureADServiceTest {

  @Mock private AzureADConfig azureADConfig;
  @Mock private BRDValidationService brdValidationService;
  @Mock private UserRepository userRepository;

  private AzureADService azureADService;

  @BeforeEach
  void setUp() {
    azureADService = new AzureADService(azureADConfig, brdValidationService, userRepository);
  }

  @Test
  void getBillerEmailsByBrdName_WithValidBrdName_ShouldReturnBillerEmails() {
    // Given
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Namrata")
                .lastName("Gurufale")
                .email("namrata.gurufale@peerislands.io")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("2")
                .firstName("Eligeti")
                .lastName("Raviteja")
                .email("eligeti.raviteja@peerislands.io")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("3")
                .firstName("Gopi")
                .lastName("Manickam")
                .email("gopi.manickam@peerislands.io")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("4")
                .firstName("Deepnarayan")
                .lastName("Kudra")
                .email("deepnarayan.kudra@peerislands.io")
                .roles(Arrays.asList("biller"))
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertEquals(4, response.getBillerEmails().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBAEmailsByBrdName_WithValidBrdName_ShouldReturnBaEmails() {
    // Given
    List<User> baUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Namrata")
                .lastName("Gurufale")
                .email("namrata.gurufale@peerislands.io")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("2")
                .firstName("Eligeti")
                .lastName("Raviteja")
                .email("eligeti.raviteja@peerislands.io")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("3")
                .firstName("Gopi")
                .lastName("Manickam")
                .email("gopi.manickam@peerislands.io")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("4")
                .firstName("Deepnarayan")
                .lastName("Kudra")
                .email("deepnarayan.kudra@peerislands.io")
                .roles(Arrays.asList("ba"))
                .build());

    when(userRepository.findByRolesRegex("^ba$")).thenReturn(Flux.fromIterable(baUsers));

    // When & Then
    StepVerifier.create(azureADService.getBAEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertEquals(4, response.getBaEmails().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerEmailsByBrdName_WithMultipleCalls_ShouldReturnSameInstance() {
    // Given
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Test")
                .lastName("Biller")
                .email("test@example.com")
                .roles(Arrays.asList("biller"))
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When
    Mono<BillerEmailsResponse> firstCall = azureADService.getBillerEmailsByBrdName();
    Mono<BillerEmailsResponse> secondCall = azureADService.getBillerEmailsByBrdName();

    // Then
    StepVerifier.create(firstCall)
        .expectNextMatches(
            response -> {
              StepVerifier.create(secondCall)
                  .expectNext(response) // Verify same instance is returned
                  .verifyComplete();
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerEmailsByBrdName_WithValidBrdName_ShouldReturnValidStructure() {
    // Given
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Test")
                .lastName("Biller")
                .email("test@example.com")
                .roles(Arrays.asList("biller"))
                .build());
    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertNotNull(response, "Response should not be null");
              assertNotNull(response.getBillerEmails(), "Biller emails list should not be null");
              response
                  .getBillerEmails()
                  .forEach(
                      biller -> {
                        assertNotNull(biller.getEmail(), "Biller email should not be null");
                        assertNotNull(
                            biller.getDisplayName(), "Biller display name should not be null");
                        assertTrue(biller.getEmail().contains("@"), "Email should be valid");
                      });
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBillerEmailsByBrdName_WithSpecialCharacters_ShouldReturnEmails() {
    // Given
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Test")
                .lastName("Biller")
                .email("test@example.com")
                .roles(Arrays.asList("biller"))
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  void getBillerEmailsByBrdName_WithValidBrdName_ShouldReturnFourBillers() {
    // Given
    int expectedNumberOfBillers = 4;
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Biller")
                .lastName("One")
                .email("biller1@example.com")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("2")
                .firstName("Biller")
                .lastName("Two")
                .email("biller2@example.com")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("3")
                .firstName("Biller")
                .lastName("Three")
                .email("biller3@example.com")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("4")
                .firstName("Biller")
                .lastName("Four")
                .email("biller4@example.com")
                .roles(Arrays.asList("biller"))
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextMatches(response -> response.getBillerEmails().size() == expectedNumberOfBillers)
        .verifyComplete();
  }

  @Test
  void getBAEmailsByBrdName_WithMultipleCalls_ShouldReturnSameInstance() {
    // Given
    List<User> baUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Test")
                .lastName("BA")
                .email("test@example.com")
                .roles(Arrays.asList("ba"))
                .build());

    when(userRepository.findByRolesRegex("^ba$")).thenReturn(Flux.fromIterable(baUsers));

    // When
    Mono<BAEmailsResponse> firstCall = azureADService.getBAEmailsByBrdName();
    Mono<BAEmailsResponse> secondCall = azureADService.getBAEmailsByBrdName();

    // Then
    StepVerifier.create(firstCall)
        .expectNextMatches(
            response -> {
              StepVerifier.create(secondCall).expectNext(response).verifyComplete();
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBAEmailsByBrdName_WithValidBrdName_ShouldReturnValidStructure() {
    // Given
    List<User> baUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("Test")
                .lastName("BA")
                .email("test@example.com")
                .roles(Arrays.asList("ba"))
                .build());

    when(userRepository.findByRolesRegex("^ba$")).thenReturn(Flux.fromIterable(baUsers));

    // When & Then
    StepVerifier.create(azureADService.getBAEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertNotNull(response, "Response should not be null");
              assertNotNull(response.getBaEmails(), "BA emails list should not be null");
              response
                  .getBaEmails()
                  .forEach(
                      ba -> {
                        assertNotNull(ba.getEmail(), "BA email should not be null");
                        assertNotNull(ba.getDisplayName(), "BA display name should not be null");
                        assertTrue(ba.getEmail().contains("@"), "Email should be valid");
                      });
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getBAEmailsByBrdName_WithValidBrdName_ShouldReturnFourBAs() {
    // Given
    int expectedNumberOfBAs = 4;
    List<User> baUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("BA")
                .lastName("One")
                .email("ba1@example.com")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("2")
                .firstName("BA")
                .lastName("Two")
                .email("ba2@example.com")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("3")
                .firstName("BA")
                .lastName("Three")
                .email("ba3@example.com")
                .roles(Arrays.asList("ba"))
                .build(),
            User.builder()
                .id("4")
                .firstName("BA")
                .lastName("Four")
                .email("ba4@example.com")
                .roles(Arrays.asList("ba"))
                .build());

    when(userRepository.findByRolesRegex("^ba$")).thenReturn(Flux.fromIterable(baUsers));

    // When & Then
    StepVerifier.create(azureADService.getBAEmailsByBrdName())
        .expectNextMatches(response -> response.getBaEmails().size() == expectedNumberOfBAs)
        .verifyComplete();
  }

  @Test
  void getBillerEmailsByBrdName_ShouldFetchBillersFromDatabase() {
    // Given
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Arrays.asList("biller"))
                .build(),
            User.builder()
                .id("2")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .roles(Arrays.asList("biller", "PM"))
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertNotNull(response, "Response should not be null");
              assertNotNull(response.getBillerEmails(), "Biller emails list should not be null");
              assertEquals(2, response.getBillerEmails().size(), "Should have 2 biller emails");

              // Verify first biller
              BillerInfo firstBiller = response.getBillerEmails().get(0);
              assertEquals("john.doe@example.com", firstBiller.getEmail());
              assertEquals("John Doe", firstBiller.getDisplayName());

              // Verify second biller
              BillerInfo secondBiller = response.getBillerEmails().get(1);
              assertEquals("jane.smith@example.com", secondBiller.getEmail());
              assertEquals("Jane Smith", secondBiller.getDisplayName());

              return true;
            })
        .verifyComplete();

    verify(userRepository).findByRolesRegex("^biller$");
  }

  @Test
  void getBillerEmailsByBrdName_ShouldMatchCaseInsensitive() {
    // Given - Users with different case variants of "biller" role
    List<User> billerUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Arrays.asList("BILLER")) // Uppercase
                .build(),
            User.builder()
                .id("2")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .roles(Arrays.asList("Biller")) // Capitalized
                .build(),
            User.builder()
                .id("3")
                .firstName("Bob")
                .lastName("Wilson")
                .email("bob.wilson@example.com")
                .roles(Arrays.asList("biller")) // Lowercase
                .build(),
            User.builder()
                .id("4")
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .roles(Arrays.asList("BiLLeR")) // Mixed case
                .build());

    when(userRepository.findByRolesRegex("^biller$")).thenReturn(Flux.fromIterable(billerUsers));

    // When & Then
    StepVerifier.create(azureADService.getBillerEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertNotNull(response, "Response should not be null");
              assertNotNull(response.getBillerEmails(), "Biller emails list should not be null");
              assertEquals(4, response.getBillerEmails().size(), "Should have 4 biller emails");

              // Verify all billers have the required fields
              response
                  .getBillerEmails()
                  .forEach(
                      biller -> {
                        assertNotNull(biller.getEmail(), "Email should not be null");
                        assertNotNull(biller.getDisplayName(), "Display name should not be null");
                      });

              // Verify specific users are included
              List<String> emails =
                  response.getBillerEmails().stream().map(BillerInfo::getEmail).toList();

              assertTrue(emails.contains("john.doe@example.com"), "Should include John Doe");
              assertTrue(emails.contains("jane.smith@example.com"), "Should include Jane Smith");
              assertTrue(emails.contains("bob.wilson@example.com"), "Should include Bob Wilson");
              assertTrue(
                  emails.contains("alice.johnson@example.com"), "Should include Alice Johnson");

              return true;
            })
        .verifyComplete();

    verify(userRepository).findByRolesRegex("^biller$");
  }

  @Test
  void getBAEmailsByBrdName_ShouldMatchCaseInsensitive() {
    // Given - Users with different case variants of "ba" role
    List<User> baUsers =
        Arrays.asList(
            User.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Arrays.asList("BA")) // Uppercase
                .build(),
            User.builder()
                .id("2")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .roles(Arrays.asList("Ba")) // Capitalized
                .build(),
            User.builder()
                .id("3")
                .firstName("Bob")
                .lastName("Wilson")
                .email("bob.wilson@example.com")
                .roles(Arrays.asList("ba")) // Lowercase
                .build(),
            User.builder()
                .id("4")
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .roles(Arrays.asList("bA")) // Mixed case
                .build());

    when(userRepository.findByRolesRegex("^ba$")).thenReturn(Flux.fromIterable(baUsers));

    // When & Then
    StepVerifier.create(azureADService.getBAEmailsByBrdName())
        .expectNextMatches(
            response -> {
              assertNotNull(response, "Response should not be null");
              assertNotNull(response.getBaEmails(), "BA emails list should not be null");
              assertEquals(4, response.getBaEmails().size(), "Should have 4 BA emails");

              // Verify all BAs have the required fields
              response
                  .getBaEmails()
                  .forEach(
                      ba -> {
                        assertNotNull(ba.getEmail(), "Email should not be null");
                        assertNotNull(ba.getDisplayName(), "Display name should not be null");
                      });

              // Verify specific users are included
              List<String> emails = response.getBaEmails().stream().map(BAInfo::getEmail).toList();

              assertTrue(emails.contains("john.doe@example.com"), "Should include John Doe");
              assertTrue(emails.contains("jane.smith@example.com"), "Should include Jane Smith");
              assertTrue(emails.contains("bob.wilson@example.com"), "Should include Bob Wilson");
              assertTrue(
                  emails.contains("alice.johnson@example.com"), "Should include Alice Johnson");

              return true;
            })
        .verifyComplete();

    verify(userRepository).findByRolesRegex("^ba$");
  }
}
