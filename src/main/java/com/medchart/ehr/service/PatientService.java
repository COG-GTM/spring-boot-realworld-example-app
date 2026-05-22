package com.medchart.ehr.service;

import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.mapper.PatientMapper;
import com.medchart.ehr.model.Patient;
import com.medchart.ehr.repository.PatientRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.medchart.ehr.service.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PatientService {
  private final PatientRepository patientRepository;
  private final PatientMapper patientMapper;

  public PatientDTO getPatientById(Long id) {
    Patient patient =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));
    return patientMapper.toDto(patient);
  }

  public PatientDTO getPatientByMrn(String mrn) {
    Patient patient =
        patientRepository
            .findByMrn(mrn)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with MRN: " + mrn));
    return patientMapper.toDto(patient);
  }

  public PatientDTO createPatient(PatientDTO dto) {
    if (dto.getMrn() != null && patientRepository.existsByMrn(dto.getMrn())) {
      throw new IllegalArgumentException("Patient with MRN " + dto.getMrn() + " already exists");
    }
    Patient patient = patientMapper.toEntity(dto);
    if (patient.getMrn() == null || patient.getMrn().isEmpty()) {
      patient.setMrn(generateMrn());
    }
    patient.setCreatedAt(LocalDateTime.now());
    patient.setUpdatedAt(LocalDateTime.now());
    Patient saved = patientRepository.save(patient);
    return patientMapper.toDto(saved);
  }

  public PatientDTO updatePatient(Long id, PatientDTO dto) {
    Patient existing =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));
    existing.setFirstName(dto.getFirstName());
    existing.setLastName(dto.getLastName());
    existing.setDateOfBirth(dto.getDateOfBirth());
    existing.setGender(dto.getGender());
    existing.setPhone(dto.getPhone());
    existing.setEmail(dto.getEmail());
    existing.setUpdatedAt(LocalDateTime.now());
    Patient updated = patientRepository.update(existing);
    return patientMapper.toDto(updated);
  }

  public List<PatientDTO> searchPatients(String query) {
    return patientRepository.search(query).stream()
        .map(patientMapper::toDto)
        .collect(Collectors.toList());
  }

  private String generateMrn() {
    return "MRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
