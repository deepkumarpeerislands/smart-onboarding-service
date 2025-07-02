package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.exception.AccountBlockedException;
import com.aci.smart_onboarding.security.config.SecurityConstants;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

  private final SecurityConstants securityConstants;
  private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
  private final Map<String, Long> blockCache = new ConcurrentHashMap<>();

  public Mono<Boolean> checkBlockedStatus(String username) {
    Long blockedUntil = blockCache.get(username);
    if (blockedUntil != null) {
      if (System.currentTimeMillis() < blockedUntil) {
        long remainingSeconds =
            Duration.between(Instant.now(), Instant.ofEpochMilli(blockedUntil)).getSeconds();

        String message =
            String.format(
                "Too many failed attempts. Please try again after %d minutes and %d seconds.",
                remainingSeconds / 60, remainingSeconds % 60);
        return Mono.error(new AccountBlockedException(message));
      } else {
        // Reset if block duration has expired
        blockCache.remove(username);
        attemptsCache.remove(username);
      }
    }
    return Mono.just(true);
  }

  public void loginSucceeded(String username) {
    log.debug("Login succeeded for user: {}, clearing attempts", username);
    attemptsCache.remove(username);
    blockCache.remove(username);
  }

  public Mono<Boolean> loginFailed(String username) {
    int attempts = attemptsCache.compute(username, (key, value) -> value == null ? 1 : value + 1);
    log.debug("Login failed for user: {}, attempts: {}", username, attempts);

    if (attempts >= securityConstants.getMaxAttempts()) {
      long blockedUntilTime =
          System.currentTimeMillis() + (securityConstants.getBlockDurationSeconds() * 1000);
      blockCache.put(username, blockedUntilTime);
      log.debug("User {} blocked until {}", username, Instant.ofEpochMilli(blockedUntilTime));

      String message =
          String.format(
              "Too many failed attempts. Account blocked for %d minutes.",
              securityConstants.getBlockDurationSeconds() / 60);
      return Mono.error(new AccountBlockedException(message));
    }

    return Mono.just(false);
  }
}
