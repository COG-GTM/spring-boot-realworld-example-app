package io.spring.infrastructure.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FlywayConfigurationTest {
  @Autowired private Flyway flyway;

  @Test
  public void should_configure_flyway_for_safe_versioned_migrations() {
    Configuration configuration = flyway.getConfiguration();

    assertArrayEquals(
        new Location[] {new Location("classpath:db/migration")}, configuration.getLocations());
    assertEquals("1", configuration.getBaselineVersion().getVersion());
    assertEquals("baseline of pre-migration schema", configuration.getBaselineDescription());
    assertFalse(configuration.isBaselineOnMigrate());
    assertTrue(configuration.isValidateOnMigrate());
    assertFalse(configuration.isOutOfOrder());
    assertTrue(configuration.isCleanDisabled());
  }
}
