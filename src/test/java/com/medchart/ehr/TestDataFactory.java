package com.medchart.ehr;

import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.model.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

  public static Patient createPatient() {
    return Patient.builder()
        .id(1L)
        .mrn("MRN-ABC12345")
        .firstName("John")
        .lastName("Doe")
        .dateOfBirth(LocalDate.of(1990, 1, 15))
        .gender("Male")
        .phone("555-0100")
        .email("john.doe@example.com")
        .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
        .updatedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
        .build();
  }

  public static Patient createPatient(Long id, String mrn) {
    return Patient.builder()
        .id(id)
        .mrn(mrn)
        .firstName("Jane")
        .lastName("Smith")
        .dateOfBirth(LocalDate.of(1985, 6, 20))
        .gender("Female")
        .phone("555-0200")
        .email("jane.smith@example.com")
        .createdAt(LocalDateTime.of(2024, 2, 1, 12, 0))
        .updatedAt(LocalDateTime.of(2024, 2, 1, 12, 0))
        .build();
  }

  public static PatientDTO createPatientDTO() {
    return PatientDTO.builder()
        .id(1L)
        .mrn("MRN-ABC12345")
        .firstName("John")
        .lastName("Doe")
        .dateOfBirth(LocalDate.of(1990, 1, 15))
        .gender("Male")
        .phone("555-0100")
        .email("john.doe@example.com")
        .build();
  }

  public static PatientDTO createPatientDTOWithoutMrn() {
    return PatientDTO.builder()
        .firstName("John")
        .lastName("Doe")
        .dateOfBirth(LocalDate.of(1990, 1, 15))
        .gender("Male")
        .phone("555-0100")
        .email("john.doe@example.com")
        .build();
  }

  public static PatientDTO createUpdatePatientDTO() {
    return PatientDTO.builder()
        .firstName("Jonathan")
        .lastName("Doe-Updated")
        .dateOfBirth(LocalDate.of(1990, 1, 15))
        .gender("Male")
        .phone("555-0199")
        .email("jonathan.doe@example.com")
        .build();
  }
}
