package com.gs.collateral.config;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.AssetClass;
import com.gs.collateral.domain.CollateralPosition;
import com.gs.collateral.repository.AgreementRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

/** Loads CSA agreements from a YAML file in non-production environments. */
@Configuration
@ConditionalOnProperty(prefix = "collateral.seed", name = "enabled", havingValue = "true")
public class SeedDataLoader {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    @Bean
    CommandLineRunner seedAgreements(AgreementRepository repository, ResourceLoader resourceLoader,
                                     org.springframework.core.env.Environment env) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            Resource resource = resourceLoader.getResource(env.getProperty("collateral.seed.file"));
            try (InputStream in = resource.getInputStream()) {
                Map<String, Object> root = new Yaml().load(in);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> agreements = (List<Map<String, Object>>) root.get("agreements");
                for (Map<String, Object> raw : agreements) {
                    repository.save(toAgreement(raw));
                }
                log.info("Seeded {} agreements from {}", agreements.size(), resource.getDescription());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Agreement toAgreement(Map<String, Object> raw) {
        Agreement agreement = new Agreement(
                (String) raw.get("reference"),
                (String) raw.get("counterparty"),
                (String) raw.get("currency"),
                decimal(raw.get("threshold")),
                decimal(raw.get("minimumTransferAmount")));
        agreement.setExposure(decimal(raw.get("exposure")));
        for (Map<String, Object> p : (List<Map<String, Object>>) raw.getOrDefault("positions", List.of())) {
            agreement.addPosition(new CollateralPosition(
                    (String) p.get("asset"),
                    AssetClass.valueOf((String) p.get("assetClass")),
                    decimal(p.get("marketValue")),
                    ((Number) p.get("haircutBps")).intValue()));
        }
        return agreement;
    }

    private static BigDecimal decimal(Object value) {
        return new BigDecimal(String.valueOf(value)).setScale(2);
    }
}
