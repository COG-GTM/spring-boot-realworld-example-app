package com.gs.collateral.repository;

import com.gs.collateral.domain.MarginCall;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarginCallRepository extends JpaRepository<MarginCall, Long> {

    List<MarginCall> findByAgreementReferenceOrderByIssuedAtDesc(String reference);

    List<MarginCall> findByStatus(MarginCall.Status status);
}
