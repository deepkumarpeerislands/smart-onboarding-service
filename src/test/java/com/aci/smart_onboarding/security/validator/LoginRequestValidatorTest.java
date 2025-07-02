package com.aci.smart_onboarding.security.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aci.smart_onboarding.dto.LoginRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.BadCredentialsException;

class LoginRequestValidatorTest {
  private LoginRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LoginRequestValidator();
  }

  @ParameterizedTest
  @MethodSource("validLoginRequests")
  void validate_WithValidCredentials_ShouldNotThrowException(String username, String password) {
    LoginRequest request = new LoginRequest(username, password);
    assertDoesNotThrow(() -> validator.validate(request));
  }

  private static Stream<Arguments> validLoginRequests() {
    return Stream.of(
        Arguments.of("validuser", "validPass123"),
        Arguments.of("user@example.com", "validPass123"),
        Arguments.of("valid.user@example-domain_com", "validPass123"));
  }

  @ParameterizedTest
  @MethodSource("invalidUsernames")
  void validate_WithInvalidUsername_ShouldThrowBadCredentialsException(String username) {
    LoginRequest request = new LoginRequest(username, "validPass123");
    assertThrows(BadCredentialsException.class, () -> validator.validate(request));
  }

  private static Stream<Arguments> invalidUsernames() {
    return Stream.of(
        Arguments.of((Object) null), // null username
        Arguments.of(""), // empty
        Arguments.of("a"), // too short
        Arguments.of("ab"), // too short
        Arguments.of("user name"), // contains space
        Arguments.of("user<script>"), // contains script tags
        Arguments.of("user;drop table"), // contains SQL injection attempt
        Arguments.of("user#$*!"), // invalid special characters
        Arguments.of("a".repeat(101)) // 101 chars
        );
  }

  @ParameterizedTest
  @MethodSource("invalidPasswords")
  void validate_WithInvalidPassword_ShouldThrowBadCredentialsException(String password) {
    LoginRequest request = new LoginRequest("validuser", password);
    assertThrows(BadCredentialsException.class, () -> validator.validate(request));
  }

  private static Stream<Arguments> invalidPasswords() {
    return Stream.of(
        Arguments.of((Object) null), // null password
        Arguments.of(""), // empty
        Arguments.of("pass'"), // contains dangerous char
        Arguments.of("pass<script>"), // contains script tags
        Arguments.of("pass;drop table"), // contains SQL injection attempt
        Arguments.of("a".repeat(129)) // 129 chars
        );
  }
}
