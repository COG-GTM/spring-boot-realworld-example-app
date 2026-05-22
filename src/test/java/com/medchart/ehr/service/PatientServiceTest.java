package com.medchart.ehr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medchart.ehr.TestDataFactory;
import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.mapper.PatientMapper;
import com.medchart.ehr.model.Patient;
import com.medchart.ehr.repository.PatientRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

  @Mock private PatientRepository patientRepository;

  @Mock private PatientMapper patientMapper;

  @InjectMocks private PatientService patientService;

  private Patient patient;
  private PatientDTO patientDTO;

  @BeforeEach
  void setUp() {
    patient = TestDataFactory.createPatient();
    patientDTO = TestDataFactory.createPatientDTO();
  }

  @Test
  void getPatientById_found_returnsDto() {
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
    when(patientMapper.toDto(patient)).thenReturn(patientDTO);

    PatientDTO result = patientService.getPatientById(1L);

    assertEquals(patientDTO.getMrn(), result.getMrn());
    assertEquals(patientDTO.getFirstName(), result.getFirstName());
    verify(patientRepository).findById(1L);
    verify(patientMapper).toDto(patient);
  }

  @Test
  void getPatientById_notFound_throwsEntityNotFoundException() {
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> patientService.getPatientById(99L));

    assertEquals("Patient not found with id: 99", ex.getMessage());
    verify(patientMapper, never()).toDto(any());
  }

  @Test
  void getPatientByMrn_found_returnsDto() {
    when(patientRepository.findByMrn("MRN-ABC12345")).thenReturn(Optional.of(patient));
    when(patientMapper.toDto(patient)).thenReturn(patientDTO);

    PatientDTO result = patientService.getPatientByMrn("MRN-ABC12345");

    assertEquals("MRN-ABC12345", result.getMrn());
    verify(patientRepository).findByMrn("MRN-ABC12345");
  }

  @Test
  void getPatientByMrn_notFound_throwsEntityNotFoundException() {
    when(patientRepository.findByMrn("UNKNOWN")).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(
            EntityNotFoundException.class, () -> patientService.getPatientByMrn("UNKNOWN"));

    assertEquals("Patient not found with MRN: UNKNOWN", ex.getMessage());
  }

  @Test
  void createPatient_newPatient_autoGeneratesMrn() {
    PatientDTO inputDto = TestDataFactory.createPatientDTOWithoutMrn();
    Patient inputEntity = TestDataFactory.createPatient();
    inputEntity.setMrn(null);

    when(patientMapper.toEntity(inputDto)).thenReturn(inputEntity);
    when(patientRepository.save(any(Patient.class))).thenReturn(inputEntity);
    when(patientMapper.toDto(inputEntity)).thenReturn(patientDTO);

    PatientDTO result = patientService.createPatient(inputDto);

    assertNotNull(result);
    verify(patientRepository).save(any(Patient.class));
    assertNotNull(inputEntity.getMrn());
  }

  @Test
  void createPatient_duplicateMrn_throwsIllegalArgumentException() {
    PatientDTO inputDto = TestDataFactory.createPatientDTO();
    when(patientRepository.existsByMrn(inputDto.getMrn())).thenReturn(true);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> patientService.createPatient(inputDto));

    assertEquals("Patient with MRN MRN-ABC12345 already exists", ex.getMessage());
    verify(patientRepository, never()).save(any());
  }

  @Test
  void updatePatient_success_returnsUpdatedDto() {
    PatientDTO updateDto = TestDataFactory.createUpdatePatientDTO();
    when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
    when(patientRepository.update(any(Patient.class))).thenReturn(patient);
    when(patientMapper.toDto(patient)).thenReturn(patientDTO);

    PatientDTO result = patientService.updatePatient(1L, updateDto);

    assertNotNull(result);
    verify(patientRepository).findById(1L);
    verify(patientRepository).update(patient);
  }

  @Test
  void updatePatient_notFound_throwsEntityNotFoundException() {
    when(patientRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> patientService.updatePatient(99L, TestDataFactory.createUpdatePatientDTO()));

    verify(patientRepository, never()).update(any());
  }

  @Test
  void searchPatients_delegatesToRepoAndMapsResult() {
    Patient patient2 = TestDataFactory.createPatient(2L, "MRN-XYZ99999");
    PatientDTO dto2 =
        PatientDTO.builder().id(2L).mrn("MRN-XYZ99999").firstName("Jane").lastName("Smith").build();

    when(patientRepository.search("Doe")).thenReturn(Arrays.asList(patient, patient2));
    when(patientMapper.toDto(patient)).thenReturn(patientDTO);
    when(patientMapper.toDto(patient2)).thenReturn(dto2);

    List<PatientDTO> results = patientService.searchPatients("Doe");

    assertEquals(2, results.size());
    verify(patientRepository).search("Doe");
    verify(patientMapper).toDto(patient);
    verify(patientMapper).toDto(patient2);
  }
}
