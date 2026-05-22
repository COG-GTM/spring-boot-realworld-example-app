package com.medchart.ehr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medchart.ehr.cache.InsuranceCache;
import com.medchart.ehr.gateway.InsuranceGateway;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

  @Mock private InsuranceCache insuranceCache;

  @Mock private InsuranceGateway insuranceGateway;

  @InjectMocks private AppointmentService appointmentService;

  private static final String PATIENT_MRN = "MRN-ABC12345";

  @Test
  void scheduleAppointment_eligible_returnsScheduledStatus() {
    when(insuranceCache.get(PATIENT_MRN)).thenReturn(Optional.of(true));
    when(insuranceCache.isStale(PATIENT_MRN)).thenReturn(false);

    LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 9, 0);
    Map<String, Object> result = appointmentService.scheduleAppointment(PATIENT_MRN, dateTime);

    assertNotNull(result);
    assertEquals("SCHEDULED", result.get("status"));
    assertEquals(PATIENT_MRN, result.get("patientMrn"));
    assertEquals(dateTime, result.get("dateTime"));
  }

  @Test
  void scheduleAppointment_ineligible_throwsIllegalStateException() {
    when(insuranceCache.get(PATIENT_MRN)).thenReturn(Optional.of(false));
    when(insuranceCache.isStale(PATIENT_MRN)).thenReturn(false);

    LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 9, 0);

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> appointmentService.scheduleAppointment(PATIENT_MRN, dateTime));

    assertEquals("Patient " + PATIENT_MRN + " is not eligible for scheduling", ex.getMessage());
  }

  @Test
  void checkEligibility_cacheHit_gatewayNotCalled() {
    when(insuranceCache.get(PATIENT_MRN)).thenReturn(Optional.of(true));
    when(insuranceCache.isStale(PATIENT_MRN)).thenReturn(false);

    boolean result = appointmentService.checkEligibility(PATIENT_MRN);

    assertTrue(result);
    verify(insuranceGateway, never()).checkEligibility(PATIENT_MRN);
  }

  @Test
  void checkEligibility_cacheMiss_gatewayCalledAndResultCached() {
    when(insuranceCache.get(PATIENT_MRN)).thenReturn(Optional.empty());
    when(insuranceGateway.checkEligibility(PATIENT_MRN)).thenReturn(true);

    boolean result = appointmentService.checkEligibility(PATIENT_MRN);

    assertTrue(result);
    verify(insuranceGateway).checkEligibility(PATIENT_MRN);
    verify(insuranceCache).put(PATIENT_MRN, true);
  }

  @Test
  void isStale_alwaysReturnsFalse_knownHipaaTtlBug() {
    // Documents the known HIPAA TTL bug: isStale() always returns false,
    // meaning cached eligibility data is never refreshed. This violates
    // HIPAA requirements for timely eligibility verification.
    InsuranceCache realCache = new InsuranceCache();
    realCache.put(PATIENT_MRN, true);

    assertFalse(realCache.isStale(PATIENT_MRN));
  }
}
