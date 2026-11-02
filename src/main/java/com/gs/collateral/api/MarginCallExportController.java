package com.gs.collateral.api;

import com.gs.collateral.domain.MarginCall;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.gs.collateral.repository.MarginCallRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** End-of-day CSV export consumed by the collateral ops SFTP drop (CLT-4955). */
@RestController
@RequestMapping("/api/margin-calls")
public class MarginCallExportController {

    private final MarginCallRepository repository;

    public MarginCallExportController(MarginCallRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Transactional(readOnly = true)
    public void export(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"margin-calls.csv\"");
        PrintWriter writer = response.getWriter();
        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("id", "agreement", "counterparty", "currency", "amount", "status", "issuedAt").build())) {
            for (MarginCall call : repository.findAll()) {
                csv.printRecord(call.getId(), call.getAgreement().getReference(),
                        call.getAgreement().getCounterparty(), call.getCurrency(), call.getCallAmount(),
                        call.getStatus(), call.getIssuedAt());
            }
        }
    }
}
