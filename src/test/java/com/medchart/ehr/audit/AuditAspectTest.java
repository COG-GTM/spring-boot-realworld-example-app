package com.medchart.ehr.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

  @Mock private AuditService auditService;

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private Audited audited;

  @InjectMocks private AuditAspect auditAspect;

  @Test
  void audit_successfulCall_savesEventWithSuccessTrue() throws Throwable {
    when(audited.action()).thenReturn("GET_PATIENT");
    when(joinPoint.getArgs()).thenReturn(new Object[] {42L});
    when(joinPoint.proceed()).thenReturn("result");

    Object result = auditAspect.audit(joinPoint, audited);

    assertEquals("result", result);
    verify(auditService)
        .save(
            argThat(
                event ->
                    event.isSuccess()
                        && "GET_PATIENT".equals(event.getAction())
                        && Long.valueOf(42L).equals(event.getPatientId())
                        && event.getErrorMessage() == null));
  }

  @Test
  void audit_exceptionInTarget_savesEventWithSuccessFalseAndRethrows() throws Throwable {
    when(audited.action()).thenReturn("UPDATE_PATIENT");
    when(joinPoint.getArgs()).thenReturn(new Object[] {7L});
    when(joinPoint.proceed()).thenThrow(new RuntimeException("DB error"));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> auditAspect.audit(joinPoint, audited));

    assertEquals("DB error", ex.getMessage());
    verify(auditService)
        .save(
            argThat(
                event ->
                    !event.isSuccess()
                        && "UPDATE_PATIENT".equals(event.getAction())
                        && Long.valueOf(7L).equals(event.getPatientId())
                        && "DB error".equals(event.getErrorMessage())));
  }

  @Test
  void extractPatientId_picksUpLongArgument() {
    Object[] args = new Object[] {"someString", 123L, 456};
    Long result = auditAspect.extractPatientId(args);
    assertEquals(123L, result);
  }

  @Test
  void extractPatientId_noLongArg_returnsNull() {
    Object[] args = new Object[] {"someString", 456};
    Long result = auditAspect.extractPatientId(args);
    assertNull(result);
  }

  @Test
  void extractPatientId_nullArgs_returnsNull() {
    Long result = auditAspect.extractPatientId(null);
    assertNull(result);
  }

  @Test
  void extractPatientId_emptyArgs_returnsNull() {
    Long result = auditAspect.extractPatientId(new Object[] {});
    assertNull(result);
  }
}
