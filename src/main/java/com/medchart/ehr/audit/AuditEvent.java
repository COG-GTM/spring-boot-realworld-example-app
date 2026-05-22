package com.medchart.ehr.audit;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
  private String action;
  private Long patientId;
  private boolean success;
  private String errorMessage;
  private LocalDateTime timestamp;
}
