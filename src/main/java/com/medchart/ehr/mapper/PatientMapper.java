package com.medchart.ehr.mapper;

import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

  public PatientDTO toDto(Patient patient) {
    if (patient == null) {
      return null;
    }
    return PatientDTO.builder()
        .id(patient.getId())
        .mrn(patient.getMrn())
        .firstName(patient.getFirstName())
        .lastName(patient.getLastName())
        .dateOfBirth(patient.getDateOfBirth())
        .gender(patient.getGender())
        .phone(patient.getPhone())
        .email(patient.getEmail())
        .build();
  }

  public Patient toEntity(PatientDTO dto) {
    if (dto == null) {
      return null;
    }
    return Patient.builder()
        .id(dto.getId())
        .mrn(dto.getMrn())
        .firstName(dto.getFirstName())
        .lastName(dto.getLastName())
        .dateOfBirth(dto.getDateOfBirth())
        .gender(dto.getGender())
        .phone(dto.getPhone())
        .email(dto.getEmail())
        .build();
  }
}
