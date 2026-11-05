package io.spring.application.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.data.GolfPickData;
import io.spring.application.data.GolferData;
import io.spring.application.data.TournamentStatus;
import io.spring.infrastructure.mybatis.readservice.GolfReadService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GolfQueryServiceTest {
  private GolfReadService golfReadService;
  private GolfQueryService service;

  @BeforeEach
  public void setUp() {
    golfReadService = mock(GolfReadService.class);
    service = new GolfQueryService(golfReadService);
  }

  @Test
  public void should_keep_prize_unknown_when_ended_tournament_has_no_results_yet() {
    when(golfReadService.findPickById(eq("p"))).thenReturn(endedPick(false));
    GolfPickData pick = service.findPick("p").get();
    assertEquals(TournamentStatus.COMPLETED, pick.getTournamentStatus());
    assertNull(pick.getPrizeMoney());
  }

  @Test
  public void should_report_zero_prize_when_results_exist_without_golfer_payout() {
    when(golfReadService.findPickById(eq("p"))).thenReturn(endedPick(true));
    assertEquals(0L, service.findPick("p").get().getPrizeMoney());
  }

  private GolfPickData endedPick(boolean hasResults) {
    DateTime now = new DateTime();
    return new GolfPickData(
        "p",
        "c",
        "t",
        "The Open",
        now.minusDays(4),
        now.minusDays(1),
        hasResults,
        null,
        new GolferData("golfer-rahm", "Jon Rahm", "ESP"),
        null,
        null,
        now.minusDays(5));
  }
}
