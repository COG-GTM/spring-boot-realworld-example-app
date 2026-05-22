package com.medchart.ehr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class Patient {
  private Long id;
  private String mrn;
  private String firstName;
  private String lastName;
  private LocalDate dateOfBirth;
  private String gender;
  private String phone;
  private String email;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
