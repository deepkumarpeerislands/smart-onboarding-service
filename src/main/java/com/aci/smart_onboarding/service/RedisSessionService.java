package com.aci.smart_onboarding.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSessionService {
  private final ReactiveRedisTemplate<String, String> redisTemplate;
  private static final Duration SESSION_TIMEOUT = Duration.ofHours(24);
  private static final String ROLE_SEPARATOR = ",";

  private String getSessionKey(String userId, String jti) {
    String key = String.format("session:%s:%s", userId, jti);
    log.debug("Generated session key: {}", key);
    return key;
  }

  public String generateJti() {
    log.debug("Entering method: RedisSessionService.generateJti");
    String jti = UUID.randomUUID().toString();
    log.debug("Generated JTI: {}", jti);
    log.debug("Exiting method: RedisSessionService.generateJti");
    return jti;
  }

  public Mono<Boolean> createSession(
      String userId, String jti, String activeRole, List<String> allRoles) {
    log.debug("Entering method: RedisSessionService.createSession");
    log.debug(
        "Creating session - User: {}, JTI: {}, Active Role: {}, All Roles: {}",
        userId,
        jti,
        activeRole,
        allRoles);

    String sessionKey = getSessionKey(userId, jti);

    // Store all roles as a comma-separated string, with active role first
    List<String> orderedRoles = new ArrayList<>();
    orderedRoles.add(activeRole); // Active role first
    allRoles.stream().filter(role -> !role.equals(activeRole)).forEach(orderedRoles::add);

    String rolesString = String.join(ROLE_SEPARATOR, orderedRoles);
    log.debug("Storing roles in Redis - Key: {}, Roles: {}", sessionKey, rolesString);

    return redisTemplate
        .opsForValue()
        .set(sessionKey, rolesString, SESSION_TIMEOUT)
        .doOnSuccess(
            success ->
                log.debug(
                    "Session created successfully - Key: {}, Success: {}", sessionKey, success))
        .doOnError(
            error -> {
              if (error instanceof RedisConnectionFailureException) {
                log.error("Redis connection error while creating session: {}", error.getMessage());
              } else {
                log.error("Error creating session: {}", error.getMessage());
              }
            })
        .onErrorReturn(false)
        .doFinally(signal -> log.debug("Exiting method: RedisSessionService.createSession"));
  }

  public Mono<List<String>> getSession(String userId, String jti) {
    log.debug("Entering method: RedisSessionService.getSession");
    String sessionKey = getSessionKey(userId, jti);
    log.debug("Retrieving session - Key: {}", sessionKey);

    return redisTemplate
        .opsForValue()
        .get(sessionKey)
        .doOnNext(
            rolesString ->
                log.debug(
                    "Retrieved roles string from Redis - Key: {}, Roles: {}",
                    sessionKey,
                    rolesString))
        .map(
            rolesString -> {
              List<String> roles = Arrays.asList(rolesString.split(ROLE_SEPARATOR));
              log.debug("Parsed roles from session - Key: {}, Roles: {}", sessionKey, roles);
              return roles;
            })
        .doOnError(
            error -> {
              if (error instanceof RedisConnectionFailureException) {
                log.error("Redis connection error while getting session: {}", error.getMessage());
              } else {
                log.error("Error getting session: {}", error.getMessage());
              }
            })
        .onErrorReturn(new ArrayList<>())
        .doFinally(signal -> log.debug("Exiting method: RedisSessionService.getSession"));
  }

  public Mono<String> getActiveRole(String userId, String jti) {
    log.debug("Getting active role for user: {} with JTI: {}", userId, jti);
    return getSession(userId, jti)
        .map(
            roles -> {
              String activeRole = roles.isEmpty() ? null : roles.get(0);
              log.debug("Retrieved active role: {}", activeRole);
              return activeRole;
            });
  }

  public Mono<Boolean> invalidateSession(String userId, String jti) {
    log.debug("Invalidating session - User: {}, JTI: {}", userId, jti);
    String sessionKey = getSessionKey(userId, jti);
    return redisTemplate
        .delete(sessionKey)
        .map(count -> count > 0)
        .doOnSuccess(
            deleted ->
                log.debug(
                    "Session invalidation result - Key: {}, Deleted: {}", sessionKey, deleted))
        .doOnError(
            error -> {
              if (error instanceof RedisConnectionFailureException) {
                log.error(
                    "Redis connection error while invalidating session: {}", error.getMessage());
              } else {
                log.error("Error invalidating session: {}", error.getMessage());
              }
            })
        .onErrorReturn(false);
  }

  public Mono<Boolean> validateSession(String userId, String jti) {
    log.debug("Entering method: RedisSessionService.validateSession");
    log.debug("Validating session - User: {}, JTI: {}", userId, jti);
    String sessionKey = getSessionKey(userId, jti);

    return redisTemplate
        .hasKey(sessionKey)
        .flatMap(
            exists -> {
              log.debug("Session key exists check - Key: {}, Exists: {}", sessionKey, exists);
              if (Boolean.FALSE.equals(exists)) {
                log.warn("Session key not found in Redis: {}", sessionKey);
                return Mono.just(false);
              }
              return getSession(userId, jti)
                  .map(
                      roles -> {
                        boolean valid = !roles.isEmpty();
                        log.debug(
                            "Session validation check - Key: {}, Roles: {}, Valid: {}",
                            sessionKey,
                            roles,
                            valid);
                        return valid;
                      });
            })
        .defaultIfEmpty(false)
        .doOnSuccess(
            valid -> log.debug("Session validation result - Key: {}, Valid: {}", sessionKey, valid))
        .doOnError(
            error -> {
              if (error instanceof RedisConnectionFailureException) {
                log.error(
                    "Redis connection error while validating session: {}", error.getMessage());
              } else {
                log.error("Error validating session: {}", error.getMessage());
              }
            })
        .onErrorReturn(false)
        .doFinally(signal -> log.debug("Exiting method: RedisSessionService.validateSession"));
  }
}
