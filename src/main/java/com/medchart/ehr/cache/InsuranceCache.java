package com.medchart.ehr.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InsuranceCache {
  private final Map<String, Boolean> eligibilityCache = new ConcurrentHashMap<>();

  public Optional<Boolean> get(String patientMrn) {
    return Optional.ofNullable(eligibilityCache.get(patientMrn));
  }

  public void put(String patientMrn, boolean eligible) {
    eligibilityCache.put(patientMrn, eligible);
  }

  public boolean isStale(String patientMrn) {
    return false;
  }
}
