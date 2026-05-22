package com.medchart.ehr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medchart.ehr.TestDataFactory;
import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.service.EntityNotFoundException;
import com.medchart.ehr.service.PatientService;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

  private MockMvc mvc;

  @Mock private PatientService patientService;

  @InjectMocks private PatientController patientController;

  private ObjectMapper objectMapper;
  private PatientDTO patientDTO;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(patientController).build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    patientDTO = TestDataFactory.createPatientDTO();
  }

  @Test
  void getPatientById_found_returns200() throws Exception {
    when(patientService.getPatientById(1L)).thenReturn(patientDTO);

    mvc.perform(get("/v1/patients/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mrn").value("MRN-ABC12345"))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"));
  }

  @Test
  void getPatientById_notFound_returns404() throws Exception {
    when(patientService.getPatientById(99L))
        .thenThrow(new EntityNotFoundException("Patient not found with id: 99"));

    mvc.perform(get("/v1/patients/99")).andExpect(status().isNotFound());
  }

  @Test
  void getPatientByMrn_found_returns200() throws Exception {
    when(patientService.getPatientByMrn("MRN-ABC12345")).thenReturn(patientDTO);

    mvc.perform(get("/v1/patients/mrn/MRN-ABC12345"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mrn").value("MRN-ABC12345"));
  }

  @Test
  void getPatientByMrn_notFound_returns404() throws Exception {
    when(patientService.getPatientByMrn("UNKNOWN"))
        .thenThrow(new EntityNotFoundException("Patient not found with MRN: UNKNOWN"));

    mvc.perform(get("/v1/patients/mrn/UNKNOWN")).andExpect(status().isNotFound());
  }

  @Test
  void searchPatients_returns200() throws Exception {
    PatientDTO dto2 =
        PatientDTO.builder().id(2L).mrn("MRN-XYZ99999").firstName("Jane").lastName("Smith").build();
    when(patientService.searchPatients("Doe")).thenReturn(Arrays.asList(patientDTO, dto2));

    mvc.perform(get("/v1/patients/search").param("q", "Doe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].firstName").value("John"))
        .andExpect(jsonPath("$[1].firstName").value("Jane"));
  }

  @Test
  void createPatient_validBody_returns201() throws Exception {
    when(patientService.createPatient(any(PatientDTO.class))).thenReturn(patientDTO);

    mvc.perform(
            post("/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mrn").value("MRN-ABC12345"))
        .andExpect(jsonPath("$.firstName").value("John"));
  }

  @Test
  void createPatient_missingRequiredFields_returns400() throws Exception {
    PatientDTO invalid = PatientDTO.builder().build();

    mvc.perform(
            post("/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updatePatient_found_returns200() throws Exception {
    PatientDTO updateDto = TestDataFactory.createUpdatePatientDTO();
    when(patientService.updatePatient(eq(1L), any(PatientDTO.class))).thenReturn(patientDTO);

    mvc.perform(
            put("/v1/patients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("John"));
  }

  @Test
  void updatePatient_notFound_returns404() throws Exception {
    PatientDTO updateDto = TestDataFactory.createUpdatePatientDTO();
    when(patientService.updatePatient(eq(99L), any(PatientDTO.class)))
        .thenThrow(new EntityNotFoundException("Patient not found with id: 99"));

    mvc.perform(
            put("/v1/patients/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isNotFound());
  }
}
