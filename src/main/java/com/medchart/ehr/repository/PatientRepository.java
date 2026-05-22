package com.medchart.ehr.repository;

import com.medchart.ehr.model.Patient;
import java.util.List;
import java.util.Optional;

public interface PatientRepository {
  Optional<Patient> findById(Long id);

  Optional<Patient> findByMrn(String mrn);

  List<Patient> search(String query);

  Patient save(Patient patient);

  Patient update(Patient patient);

  boolean existsByMrn(String mrn);
}
