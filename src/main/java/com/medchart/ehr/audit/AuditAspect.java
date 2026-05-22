package com.medchart.ehr.audit;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@AllArgsConstructor
public class AuditAspect {
  private final AuditService auditService;

  @Around("@annotation(audited)")
  public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    Long patientId = extractPatientId(joinPoint.getArgs());
    try {
      Object result = joinPoint.proceed();
      auditService.save(
          AuditEvent.builder()
              .action(audited.action())
              .patientId(patientId)
              .success(true)
              .timestamp(LocalDateTime.now())
              .build());
      return result;
    } catch (Throwable ex) {
      try {
        auditService.save(
            AuditEvent.builder()
                .action(audited.action())
                .patientId(patientId)
                .success(false)
                .errorMessage(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
      } catch (Exception auditEx) {
        // Don't let audit failure mask the original exception
      }
      throw ex;
    }
  }

  Long extractPatientId(Object[] args) {
    if (args != null) {
      for (Object arg : args) {
        if (arg instanceof Long) {
          return (Long) arg;
        }
      }
    }
    return null;
  }
}
