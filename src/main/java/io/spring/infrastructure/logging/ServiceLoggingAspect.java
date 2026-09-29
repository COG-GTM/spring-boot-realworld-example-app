package io.spring.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ServiceLoggingAspect {
  static final String LOGGER_NAME = "io.spring.service";
  private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

  @Around("execution(public * io.spring.application..*Service.*(..))")
  public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!log.isDebugEnabled()) {
      return joinPoint.proceed();
    }
    String method =
        joinPoint.getSignature().getDeclaringType().getSimpleName()
            + "."
            + joinPoint.getSignature().getName();
    long start = System.nanoTime();
    log.debug("-> {}", method);
    try {
      Object result = joinPoint.proceed();
      log.debug("<- {} ({} ms)", method, (System.nanoTime() - start) / 1_000_000);
      return result;
    } catch (Throwable e) {
      log.debug(
          "<- {} threw {} ({} ms)",
          method,
          e.getClass().getSimpleName(),
          (System.nanoTime() - start) / 1_000_000);
      throw e;
    }
  }
}
