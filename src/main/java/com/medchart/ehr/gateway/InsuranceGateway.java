package com.medchart.ehr.gateway;

import org.springframework.stereotype.Component;

@Component
public class InsuranceGateway {

  public boolean checkEligibility(String patientMrn) {
    throw new UnsupportedOperationException("External insurance gateway not yet implemented");
  }
}
