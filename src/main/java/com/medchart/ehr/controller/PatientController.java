package com.medchart.ehr.controller;

import com.medchart.ehr.dto.PatientDTO;
import com.medchart.ehr.service.PatientService;
import java.util.List;
import com.medchart.ehr.service.EntityNotFoundException;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/patients")
@AllArgsConstructor
public class PatientController {
  private final PatientService patientService;

  @GetMapping("/{id}")
  public ResponseEntity<PatientDTO> getPatientById(@PathVariable Long id) {
    return ResponseEntity.ok(patientService.getPatientById(id));
  }

  @GetMapping("/mrn/{mrn}")
  public ResponseEntity<PatientDTO> getPatientByMrn(@PathVariable String mrn) {
    return ResponseEntity.ok(patientService.getPatientByMrn(mrn));
  }

  @GetMapping("/search")
  public ResponseEntity<List<PatientDTO>> searchPatients(@RequestParam String q) {
    return ResponseEntity.ok(patientService.searchPatients(q));
  }

  @PostMapping
  public ResponseEntity<PatientDTO> createPatient(@Valid @RequestBody PatientDTO patientDTO) {
    PatientDTO created = patientService.createPatient(patientDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PatientDTO> updatePatient(
      @PathVariable Long id, @Valid @RequestBody PatientDTO patientDTO) {
    return ResponseEntity.ok(patientService.updatePatient(id, patientDTO));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
