package com.gs.collateral.service;

import com.gs.collateral.domain.MarginCall;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

/** Renders the text of a margin call notice sent to the counterparty. */
@Component
public class MarginCallNoticeFormatter {

    static final String TEMPLATE =
            """
            MARGIN CALL NOTICE
            Agreement: ${reference}
            Counterparty: ${counterparty}
            Call amount: ${currency} ${amount}
            Issued: ${issuedAt}
            Please confirm by the cut-off stated in the CSA.""";

    public String format(MarginCall call) {
        Map<String, String> values = new HashMap<>();
        values.put("reference", call.getAgreement().getReference());
        values.put("counterparty", call.getAgreement().getCounterparty());
        values.put("currency", call.getCurrency());
        values.put("amount", NumberFormat.getNumberInstance(Locale.US).format(call.getCallAmount()));
        values.put("issuedAt", DateTimeFormatter.ISO_INSTANT.format(call.getIssuedAt().atOffset(ZoneOffset.UTC)));
        return new StringSubstitutor(values).replace(TEMPLATE);
    }
}
