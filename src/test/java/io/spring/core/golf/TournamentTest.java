package io.spring.core.golf;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TournamentTest {
  private final DateTime startsAt = new DateTime("2030-04-01T12:00:00Z");
  private final DateTime endsAt = new DateTime("2030-04-04T23:00:00Z");
  private final Tournament tournament = new Tournament("c", "Open", "Course", startsAt, endsAt);

  @Test
  public void should_not_be_started_before_start_time() {
    Assertions.assertFalse(tournament.hasStarted(startsAt.minusSeconds(1)));
  }

  @Test
  public void should_be_started_at_start_time() {
    Assertions.assertTrue(tournament.hasStarted(startsAt));
    Assertions.assertTrue(tournament.hasStarted(startsAt.plusDays(1)));
  }

  @Test
  public void should_be_ended_only_after_end_time() {
    Assertions.assertFalse(tournament.hasEnded(endsAt));
    Assertions.assertTrue(tournament.hasEnded(endsAt.plusSeconds(1)));
  }
}
