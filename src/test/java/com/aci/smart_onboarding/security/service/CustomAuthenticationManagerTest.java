package com.aci.smart_onboarding.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CustomAuthenticationManagerTest {

  private CustomAuthenticationManager authenticationManager;

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    authenticationManager = new CustomAuthenticationManager(userRepository, passwordEncoder);
  }

  @Test
  void authenticate_WithValidCredentials_ShouldReturnAuthenticatedToken() {
    // Given
    String email = "test@example.com";
    String firstName = "John";
    String role = "PM";
    String password = firstName + "_" + role; // Correct format: FirstName_ROLE
    String encodedPassword = "$2a$10$BmcoWHjJQYmb5/wi7u27zOSpoYTDfwUqxQKlh33ykmxn3EOtnknoi";

    User user =
        User.builder()
            .email(email)
            .firstName(firstName)
            .password(encodedPassword.toCharArray())
            .activeRole(role)
            .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(email, password);

    when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
    when(passwordEncoder.encode("test")).thenReturn("$2a$10$testHash");
    when(passwordEncoder.matches("test", "$2a$10$testHash")).thenReturn(true);

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectNextMatches(
            authentication -> {
              assertThat(authentication.getName()).isEqualTo(email);
              assertThat(authentication.getCredentials()).isEqualTo(password);
              assertThat(authentication.getAuthorities())
                  .hasSize(1)
                  .extracting("authority")
                  .containsExactly("ROLE_" + role);
              return true;
            })
        .verifyComplete();
  }

  @Test
  void authenticate_WithPrefixedRole_ShouldReturnAuthenticatedToken() {
    // Given
    String email = "test@example.com";
    String firstName = "John";
    String roleWithPrefix = "ROLE_PM";
    String password =
        firstName + "_" + roleWithPrefix; // Password must match the full role including prefix
    String encodedPassword = "$2a$10$BmcoWHjJQYmb5/wi7u27zOSpoYTDfwUqxQKlh33ykmxn3EOtnknoi";

    User user =
        User.builder()
            .email(email)
            .firstName(firstName)
            .password(encodedPassword.toCharArray())
            .activeRole(roleWithPrefix) // Store with ROLE_ prefix
            .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(email, password);

    when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectNextMatches(
            authentication -> {
              assertThat(authentication.getName()).isEqualTo(email);
              assertThat(authentication.getCredentials()).isEqualTo(password);
              assertThat(authentication.getAuthorities())
                  .hasSize(1)
                  .extracting("authority")
                  .containsExactly(roleWithPrefix); // Should match the stored role with prefix
              return true;
            })
        .verifyComplete();
  }

  @Test
  void authenticate_WithNonExistentUser_ShouldThrowBadCredentialsException() {
    // Given
    String email = "nonexistent@example.com";
    String password = "John_PM";
    Authentication auth = new UsernamePasswordAuthenticationToken(email, password);

    when(userRepository.findByEmail(email)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectError(BadCredentialsException.class)
        .verify();
  }

  @Test
  void authenticate_WithIncorrectPasswordFormat_ShouldThrowBadCredentialsException() {
    // Given
    String email = "test@example.com";
    String firstName = "John";
    String role = "PM";
    String wrongPassword = "incorrect_password";
    String encodedPassword = "$2a$10$BmcoWHjJQYmb5/wi7u27zOSpoYTDfwUqxQKlh33ykmxn3EOtnknoi";

    User user =
        User.builder()
            .email(email)
            .firstName(firstName)
            .password(encodedPassword.toCharArray())
            .activeRole(role)
            .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(email, wrongPassword);

    when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectError(BadCredentialsException.class)
        .verify();
  }

  @Test
  void authenticate_WithEmptyEmail_ShouldThrowBadCredentialsException() {
    // Given
    Authentication auth = new UsernamePasswordAuthenticationToken("", "John_PM");

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectError(BadCredentialsException.class)
        .verify();
  }

  @Test
  void authenticate_WithEmptyPassword_ShouldThrowBadCredentialsException() {
    // Given
    Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "");

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectError(BadCredentialsException.class)
        .verify();
  }

  @Test
  void authenticate_WithTransientError_ShouldRetryAndSucceed() {
    // Given
    String email = "test@example.com";
    String firstName = "John";
    String role = "PM";
    String password = firstName + "_" + role; // Password format must match FirstName_ROLE
    String encodedPassword =
        "$2a$10$BmcoWHjJQYmb5/wi7u27zOSpoYTDfwUqxQKlh33ykmxn3EOtnknoi"; // Valid BCrypt format

    Authentication auth = new UsernamePasswordAuthenticationToken(email, password);

    // First attempt fails with transient exception, second succeeds
    when(userRepository.findByEmail(email))
        .thenReturn(Mono.error(new java.io.IOException("Network timeout")))
        .thenReturn(
            Mono.just(
                User.builder()
                    .email(email)
                    .firstName(firstName)
                    .password(encodedPassword.toCharArray())
                    .activeRole(role)
                    .build()));

    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectNextMatches(
            authentication ->
                authentication.getName().equals(email)
                    && authentication
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
                        .equals("ROLE_" + role))
        .verifyComplete();
  }

  @Test
  void authenticate_WithNonTransientError_ShouldFailImmediately() {
    // Given
    String email = "test@example.com";
    String password = "John_PM";
    Authentication auth = new UsernamePasswordAuthenticationToken(email, password);

    when(userRepository.findByEmail(email))
        .thenReturn(Mono.error(new BadCredentialsException("Invalid credentials")));

    // When & Then
    StepVerifier.create(authenticationManager.authenticate(auth))
        .expectError(BadCredentialsException.class)
        .verify();
  }
}
