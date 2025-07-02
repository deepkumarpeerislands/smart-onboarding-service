package com.aci.smart_onboarding.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MethodLoggingAspectTest {

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private MethodSignature methodSignature;

  private MethodLoggingAspect aspect;
  private TestService testService;

  @BeforeEach
  void setUp() {
    aspect = new MethodLoggingAspect();
    testService = mock(TestService.class);

    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(testService);
    when(methodSignature.getName()).thenReturn("testMethod");
  }

  @Test
  void logMethodExecution_WithSynchronousMethod_ShouldLogCorrectly() throws Throwable {
    // Given
    String result = "test result";
    when(joinPoint.proceed()).thenReturn(result);

    // When
    Object returnedResult = aspect.logMethodExecution(joinPoint);

    // Then
    assertEquals(result, returnedResult);
    verify(joinPoint).proceed();
  }

  @Test
  void logMethodExecution_WithMonoResult_ShouldLogCorrectly() throws Throwable {
    // Given
    String expectedResult = "test result";
    Mono<String> monoResult = Mono.just(expectedResult);
    when(joinPoint.proceed()).thenReturn(monoResult);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Mono);
    Mono<?> resultMono = (Mono<?>) result;

    StepVerifier.create(resultMono)
        .expectNextMatches(value -> value.equals(expectedResult))
        .verifyComplete();
  }

  @Test
  void logMethodExecution_WithFluxResult_ShouldLogCorrectly() throws Throwable {
    // Given
    String[] expectedResults = {"test1", "test2"};
    Flux<String> fluxResult = Flux.just(expectedResults);
    when(joinPoint.proceed()).thenReturn(fluxResult);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Flux);
    Flux<?> resultFlux = (Flux<?>) result;

    StepVerifier.create(resultFlux)
        .expectNextMatches(value -> value.equals(expectedResults[0]))
        .expectNextMatches(value -> value.equals(expectedResults[1]))
        .verifyComplete();
  }

  @Test
  void logMethodExecution_WithMonoError_ShouldLogError() throws Throwable {
    // Given
    RuntimeException testException = new RuntimeException("Test error");
    Mono<String> errorMono = Mono.error(testException);
    when(joinPoint.proceed()).thenReturn(errorMono);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Mono);
    Mono<?> resultMono = (Mono<?>) result;

    StepVerifier.create(resultMono)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Test error"))
        .verify();
  }

  @Test
  void logMethodExecution_WithFluxError_ShouldLogError() throws Throwable {
    // Given
    RuntimeException testException = new RuntimeException("Test error");
    Flux<String> errorFlux = Flux.error(testException);
    when(joinPoint.proceed()).thenReturn(errorFlux);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Flux);
    Flux<?> resultFlux = (Flux<?>) result;

    StepVerifier.create(resultFlux)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals("Test error"))
        .verify();
  }

  @Test
  void logMethodExecution_WhenMethodThrowsException_ShouldPropagateException() throws Throwable {
    // Given
    RuntimeException testException = new RuntimeException("Test error");
    when(joinPoint.proceed()).thenThrow(testException);

    // When/Then
    assertThrows(RuntimeException.class, () -> aspect.logMethodExecution(joinPoint));
  }

  @Test
  void logMethodExecution_WithEmptyMono_ShouldComplete() throws Throwable {
    // Given
    Mono<Void> emptyMono = Mono.empty();
    when(joinPoint.proceed()).thenReturn(emptyMono);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Mono);
    Mono<?> resultMono = (Mono<?>) result;

    StepVerifier.create(resultMono).verifyComplete();
  }

  @Test
  void logMethodExecution_WithEmptyFlux_ShouldComplete() throws Throwable {
    // Given
    Flux<String> emptyFlux = Flux.empty();
    when(joinPoint.proceed()).thenReturn(emptyFlux);

    // When
    Object result = aspect.logMethodExecution(joinPoint);

    // Then
    assertTrue(result instanceof Flux);
    Flux<?> resultFlux = (Flux<?>) result;

    StepVerifier.create(resultFlux).verifyComplete();
  }

  // Helper interface for mocking
  private interface TestService {
    String testMethod();
  }
}
