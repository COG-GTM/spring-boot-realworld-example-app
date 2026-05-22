package com.medchart.ehr.service;

import com.medchart.ehr.cache.InsuranceCache;
import com.medchart.ehr.gateway.InsuranceGateway;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppointmentService {
  private final InsuranceCache insuranceCache;
  private final InsuranceGateway insuranceGateway;

  public Map<String, Object> scheduleAppointment(String patientMrn, LocalDateTime dateTime) {
    boolean eligible = checkEligibility(patientMrn);
    if (!eligible) {
      throw new IllegalStateException(
          "Patient " + patientMrn + " is not eligible for scheduling");
    }
    Map<String, Object> appointment = new HashMap<>();
    appointment.put("patientMrn", patientMrn);
    appointment.put("dateTime", dateTime);
    appointment.put("status", "SCHEDULED");
    return appointment;
  }

  public boolean checkEligibility(String patientMrn) {
    Optional<Boolean> cached = insuranceCache.get(patientMrn);
    if (cached.isPresent() && !insuranceCache.isStale(patientMrn)) {
      return cached.get();
    }
    boolean eligible = insuranceGateway.checkEligibility(patientMrn);
    insuranceCache.put(patientMrn, eligible);
    return eligible;
  }
}
