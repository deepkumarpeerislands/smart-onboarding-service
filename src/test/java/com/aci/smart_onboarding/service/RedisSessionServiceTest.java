package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RedisSessionServiceTest {

  @Mock private ReactiveRedisTemplate<String, String> redisTemplate;
  @Mock private ReactiveValueOperations<String, String> valueOperations;

  @InjectMocks private RedisSessionService redisSessionService;

  private static final String USER_ID = "user123";
  private static final String JTI = "jti456";
  private static final String ACTIVE_ROLE = "ROLE_PM";
  private static final List<String> ALL_ROLES = Arrays.asList("ROLE_PM", "ROLE_BA", "ROLE_MANAGER");
  private static final String SESSION_KEY = "session:user123:jti456";
  private static final String ROLES_STRING = "ROLE_PM,ROLE_BA,ROLE_MANAGER";

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void generateJti_WhenCalled_ShouldReturnValidUUID() {
    String jti = redisSessionService.generateJti();

    assertNotNull(jti);
    assertFalse(jti.isEmpty());
    // Verify it's a valid UUID format
    assertTrue(jti.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
  }

  @Test
  void generateJti_WhenCalledMultipleTimes_ShouldReturnUniqueValues() {
    String jti1 = redisSessionService.generateJti();
    String jti2 = redisSessionService.generateJti();

    assertNotNull(jti1);
    assertNotNull(jti2);
    assertNotEquals(jti1, jti2);
  }

  @Test
  void createSession_WhenValidInput_ShouldSucceed() {
    when(valueOperations.set(eq(SESSION_KEY), eq(ROLES_STRING), any(Duration.class)))
        .thenReturn(Mono.just(true));

    StepVerifier.create(redisSessionService.createSession(USER_ID, JTI, ACTIVE_ROLE, ALL_ROLES))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void createSession_WhenRolesProvided_ShouldOrderRolesWithActiveRoleFirst() {
    List<String> roles = Arrays.asList("ROLE_BA", "ROLE_PM", "ROLE_MANAGER");
    String expectedRolesString = "ROLE_PM,ROLE_BA,ROLE_MANAGER";

    when(valueOperations.set(eq(SESSION_KEY), eq(expectedRolesString), any(Duration.class)))
        .thenReturn(Mono.just(true));

    StepVerifier.create(redisSessionService.createSession(USER_ID, JTI, ACTIVE_ROLE, roles))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void createSession_WhenRedisConnectionError_ShouldReturnFalse() {
    when(valueOperations.set(eq(SESSION_KEY), eq(ROLES_STRING), any(Duration.class)))
        .thenReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));

    StepVerifier.create(redisSessionService.createSession(USER_ID, JTI, ACTIVE_ROLE, ALL_ROLES))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void createSession_WhenGenericError_ShouldReturnFalse() {
    when(valueOperations.set(eq(SESSION_KEY), eq(ROLES_STRING), any(Duration.class)))
        .thenReturn(Mono.error(new RuntimeException("Generic error")));

    StepVerifier.create(redisSessionService.createSession(USER_ID, JTI, ACTIVE_ROLE, ALL_ROLES))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void getSession_WhenSessionExists_ShouldReturnRoles() {
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(ROLES_STRING));

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectNext(Arrays.asList("ROLE_PM", "ROLE_BA", "ROLE_MANAGER"))
        .verifyComplete();
  }

  @Test
  void getSession_WhenSessionDoesNotExist_ShouldReturnEmptyList() {
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.empty());

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectComplete()
        .verify();
  }

  @Test
  void getSession_WhenRedisConnectionError_ShouldReturnEmptyList() {
    when(valueOperations.get(SESSION_KEY))
        .thenReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectNext(List.of())
        .verifyComplete();
  }

  @Test
  void getSession_WhenGenericError_ShouldReturnEmptyList() {
    when(valueOperations.get(SESSION_KEY))
        .thenReturn(Mono.error(new RuntimeException("Generic error")));

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectNext(List.of())
        .verifyComplete();
  }

  @Test
  void getActiveRole_WhenSessionExists_ShouldReturnFirstRole() {
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(ROLES_STRING));

    StepVerifier.create(redisSessionService.getActiveRole(USER_ID, JTI))
        .expectNext("ROLE_PM")
        .verifyComplete();
  }

  @Test
  void getActiveRole_WhenSessionIsEmpty_ShouldReturnNull() {
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.empty());

    StepVerifier.create(redisSessionService.getActiveRole(USER_ID, JTI))
        .expectComplete()
        .verify();
  }

  @Test
  void getActiveRole_WhenSessionHasNoRoles_ShouldReturnEmptyString() {
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(""));

    StepVerifier.create(redisSessionService.getActiveRole(USER_ID, JTI))
        .expectNext("")
        .verifyComplete();
  }

  @Test
  void invalidateSession_WhenSessionExists_ShouldReturnTrue() {
    when(redisTemplate.delete(SESSION_KEY)).thenReturn(Mono.just(1L));

    StepVerifier.create(redisSessionService.invalidateSession(USER_ID, JTI))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void invalidateSession_WhenSessionDoesNotExist_ShouldReturnFalse() {
    when(redisTemplate.delete(SESSION_KEY)).thenReturn(Mono.just(0L));

    StepVerifier.create(redisSessionService.invalidateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void invalidateSession_WhenRedisConnectionError_ShouldReturnFalse() {
    when(redisTemplate.delete(SESSION_KEY))
        .thenReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));

    StepVerifier.create(redisSessionService.invalidateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void invalidateSession_WhenGenericError_ShouldReturnFalse() {
    when(redisTemplate.delete(SESSION_KEY))
        .thenReturn(Mono.error(new RuntimeException("Generic error")));

    StepVerifier.create(redisSessionService.invalidateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenSessionExistsAndHasRoles_ShouldReturnTrue() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.just(true));
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(ROLES_STRING));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenSessionDoesNotExist_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.just(false));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenSessionExistsButHasNoRoles_ShouldReturnTrue() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.just(true));
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(""));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenSessionExistsButIsEmpty_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.just(true));
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.empty());

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenRedisConnectionError_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY))
        .thenReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenGenericError_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY))
        .thenReturn(Mono.error(new RuntimeException("Generic error")));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenErrorInGetSession_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.just(true));
    when(valueOperations.get(SESSION_KEY))
        .thenReturn(Mono.error(new RuntimeException("Get session error")));

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void validateSession_WhenHasKeyReturnsEmpty_ShouldReturnFalse() {
    when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(Mono.empty());

    StepVerifier.create(redisSessionService.validateSession(USER_ID, JTI))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void createSession_WhenEmptyRolesList_ShouldHandleGracefully() {
    List<String> emptyRoles = List.of();
    when(valueOperations.set(eq(SESSION_KEY), eq(ACTIVE_ROLE), any(Duration.class)))
        .thenReturn(Mono.just(true));

    StepVerifier.create(redisSessionService.createSession(USER_ID, JTI, ACTIVE_ROLE, emptyRoles))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void getSession_WhenSingleRole_ShouldReturnSingleElementList() {
    String singleRole = "ROLE_PM";
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(singleRole));

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectNext(List.of("ROLE_PM"))
        .verifyComplete();
  }

  @Test
  void getSession_WhenRolesWithSpaces_ShouldPreserveSpaces() {
    String rolesWithSpaces = "ROLE_PM , ROLE_BA , ROLE_MANAGER";
    when(valueOperations.get(SESSION_KEY)).thenReturn(Mono.just(rolesWithSpaces));

    StepVerifier.create(redisSessionService.getSession(USER_ID, JTI))
        .expectNext(Arrays.asList("ROLE_PM ", " ROLE_BA ", " ROLE_MANAGER"))
        .verifyComplete();
  }
} 