package com.gs.collateral.repository;

import com.gs.collateral.domain.Agreement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    Optional<Agreement> findByReference(String reference);
}
