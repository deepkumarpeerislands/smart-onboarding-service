package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.enums.UserStatus;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Primary
public class CustomAuthenticationManager implements ReactiveAuthenticationManager {
  private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationManager.class);
  private static final String ROLE_PREFIX = "ROLE_";
  private static final Duration RETRY_BACKOFF = Duration.ofMillis(100);
  private static final int MAX_RETRIES = 3;
  private static final String PROVIDED = "provided";
  private static final String INVALID_CREDENTIALS = "Invalid credentials";
  
  // Cache for password validation results (username -> validation result)
  private final Map<String, Boolean> passwordValidationCache = new ConcurrentHashMap<>();
  private static final int PASSWORD_CACHE_SIZE = 500;
  private static final Duration PASSWORD_CACHE_TTL = Duration.ofMinutes(5);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public CustomAuthenticationManager(
      UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return validateCredentials(authentication)
        .flatMap(
            auth ->
                authenticateUser(
                    auth.getName(),
                    Optional.ofNullable(auth.getCredentials()).map(Object::toString).orElse("")))
        .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF).filter(this::isTransientException));
  }

  private Mono<Authentication> validateCredentials(Authentication authentication) {
    String email = authentication.getName();
    Object credentials = authentication.getCredentials();

    if (email == null || credentials == null) {
      log.error(
          "Email or credentials are null - Email: {}, Credentials: {}",
          email,
          credentials != null ? PROVIDED : "null");
      return Mono.error(new BadCredentialsException("Email and password cannot be empty"));
    }

    String password = credentials.toString();

    log.debug("Validating credentials - Email: {}, Password length: {}", email, password.length());

    if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
      log.error(
          "Empty credentials - Email: {}, Password: {}",
          StringUtils.isBlank(email) ? "empty" : PROVIDED,
          StringUtils.isBlank(password) ? "empty" : PROVIDED);
      return Mono.error(new BadCredentialsException("Email and password cannot be empty"));
    }

    return Mono.just(authentication);
  }

  private Mono<Authentication> authenticateUser(String email, String password) {
    log.debug("Attempting authentication for user: {}", email);

    return userRepository
        .findByEmail(email)
        .doOnNext(
            user ->
                log.debug(
                    "Found user: email={}, firstName={}, role={}, roles={}",
                    user.getEmail(),
                    user.getFirstName(),
                    user.getActiveRole(),
                    user.getRoles()))
        .switchIfEmpty(
            Mono.error(
                () -> {
                  log.error("User not found in database: {}", email);
                  return new BadCredentialsException(INVALID_CREDENTIALS);
                }))
        .flatMap(
            user -> {
              if (user.getStatus() == UserStatus.INACTIVE) {
                log.warn("Login attempt for inactive user: {}", user.getEmail());
                return Mono.error(
                    new BadCredentialsException(
                        "Your account is inactive. Please contact support."));
              }

              // Ensure roles list is initialized
              if (user.getRoles() == null) {
                user.setRoles(new ArrayList<>());
              }
              // Add legacy role if not in roles list
              if (user.getActiveRole() != null && !user.getRoles().contains(user.getActiveRole())) {
                user.getRoles().add(user.getActiveRole());
              }

              return validatePasswordAndCreateToken(user, password);
            });
  }

  private Mono<Authentication> validatePasswordAndCreateToken(User user, String rawPassword) {
    String email = user.getEmail();
    
    // Check password validation cache first
    String cacheKey = email + ":" + rawPassword.hashCode();
    Boolean cachedResult = passwordValidationCache.get(cacheKey);
    if (cachedResult != null) {
      if (Boolean.FALSE.equals(cachedResult)) {
        log.debug("Password validation failed (cached) for user: {}", email);
        return Mono.error(new BadCredentialsException(INVALID_CREDENTIALS));
      }
      // If cached result is true, proceed to create token
    } else {
      // Get stored password and ensure it's not null
      char[] storedPasswordChars = user.getPassword();
      if (storedPasswordChars == null || storedPasswordChars.length == 0) {
        log.error("Stored password is null or empty for user: {}", email);
        return Mono.error(new BadCredentialsException(INVALID_CREDENTIALS));
      }

      String storedPassword = String.valueOf(storedPasswordChars);

      // Validate BCrypt format
      if (!storedPassword.matches("\\$2[ayb]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
        log.error("Stored password is not in valid BCrypt format for user: {}", email);
        return Mono.error(new BadCredentialsException(INVALID_CREDENTIALS));
      }

      // Check if the raw password matches the stored password
      boolean isValid = passwordEncoder.matches(rawPassword, storedPassword);
      
      // Cache the result if cache is not full
      if (passwordValidationCache.size() < PASSWORD_CACHE_SIZE) {
        passwordValidationCache.put(cacheKey, isValid);
      }
      
      if (!isValid) {
        log.error("Password validation failed for user: {}", email);
        return Mono.error(new BadCredentialsException(INVALID_CREDENTIALS));
      }
    }

    Authentication token = createAuthenticationToken(user, rawPassword);
    log.debug("Authentication successful - Created token with roles: {}", token.getAuthorities());
    return Mono.just(token);
  }

  private Authentication createAuthenticationToken(User user, String password) {
    // Get all roles from both legacy role field and new roles list
    List<String> userRoles = new ArrayList<>();
    if (user.getActiveRole() != null) {
      userRoles.add(user.getActiveRole());
    }
    if (user.getRoles() != null) {
      userRoles.addAll(user.getRoles());
    }

    // Remove duplicates and ensure proper format more efficiently
    List<SimpleGrantedAuthority> authorities = new ArrayList<>(userRoles.size());
    for (String role : userRoles) {
      String formattedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
      if (!authorities.contains(new SimpleGrantedAuthority(formattedRole))) {
        authorities.add(new SimpleGrantedAuthority(formattedRole));
      }
    }

    log.debug("Creating auth token - Email: {}, Roles: {}", user.getEmail(), authorities);

    return new UsernamePasswordAuthenticationToken(user.getEmail(), password, authorities);
  }

  private boolean isTransientException(Throwable throwable) {
    if (throwable == null) {
      return false;
    }

    // Check for common transient exceptions in authentication and Redis operations
    return throwable instanceof io.lettuce.core.RedisConnectionException
        || throwable instanceof io.lettuce.core.RedisCommandTimeoutException
        || throwable instanceof org.springframework.dao.TransientDataAccessException
        || throwable instanceof org.springframework.dao.DataAccessResourceFailureException
        || throwable instanceof java.util.concurrent.TimeoutException
        || throwable instanceof java.io.IOException
        || (throwable.getCause() != null && isTransientException(throwable.getCause()));
  }
}
