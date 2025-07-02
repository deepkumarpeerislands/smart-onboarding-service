package com.aci.smart_onboarding.logging;

import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@NoArgsConstructor
@Component
@ConditionalOnProperty(name = "logging.method.enabled", havingValue = "true", matchIfMissing = true)
public class MethodLoggingAspect {

  @Around(
      "@annotation(LogMethodExecution) || "
          + "within(@org.springframework.stereotype.Service *) || "
          + "within(@org.springframework.stereotype.Repository *) || "
          + "within(@org.springframework.web.bind.annotation.RestController *) || "
          + "within(@org.springframework.stereotype.Controller *)")
  public Object logMethodExecution(final ProceedingJoinPoint joinPoint) throws Throwable {
    final Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
    final String fullMethodName = buildMethodName(joinPoint);
    final long startTime = System.currentTimeMillis();

    logMethodEntry(logger, fullMethodName);

    final Object result = joinPoint.proceed();

    return handleMethodResult(result, logger, fullMethodName, startTime);
  }

  private String buildMethodName(ProceedingJoinPoint joinPoint) {
    final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    final String className = joinPoint.getTarget().getClass().getSimpleName();
    return className + "." + signature.getName();
  }

  private void logMethodEntry(Logger logger, String fullMethodName) {
    if (logger.isDebugEnabled()) {
      logger.debug("Entering method: {}", fullMethodName);
    }
  }

  private Object handleMethodResult(
      Object result, Logger logger, String fullMethodName, long startTime) {
    if (result instanceof Mono) {
      return handleMonoResult((Mono<?>) result, logger, fullMethodName, startTime);
    }
    if (result instanceof Flux) {
      return handleFluxResult((Flux<?>) result, logger, fullMethodName, startTime);
    }

    logMethodExit(logger, fullMethodName, startTime);
    return result;
  }

  private Mono<?> handleMonoResult(
      Mono<?> mono, Logger logger, String fullMethodName, long startTime) {
    return mono.doOnSuccess(data -> logMethodExit(logger, fullMethodName, startTime))
        .doOnError(error -> logMethodError(logger, fullMethodName, startTime, error));
  }

  private Flux<?> handleFluxResult(
      Flux<?> flux, Logger logger, String fullMethodName, long startTime) {
    return flux.doOnComplete(() -> logMethodExit(logger, fullMethodName, startTime))
        .doOnError(error -> logMethodError(logger, fullMethodName, startTime, error));
  }

  private void logMethodExit(Logger logger, String fullMethodName, long startTime) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Exiting method: {} (execution time: {}ms)",
          fullMethodName,
          System.currentTimeMillis() - startTime);
    }
  }

  private void logMethodError(
      Logger logger, String fullMethodName, long startTime, Throwable error) {
    if (logger.isErrorEnabled()) {
      logger.error(
          "Error in method: {} (execution time: {}ms)",
          fullMethodName,
          System.currentTimeMillis() - startTime,
          error);
    }
  }
}
