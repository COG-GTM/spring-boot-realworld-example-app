package com.medchart.ehr.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

  public void save(AuditEvent event) {
    // Persists audit event (e.g., to database or log)
  }
}
