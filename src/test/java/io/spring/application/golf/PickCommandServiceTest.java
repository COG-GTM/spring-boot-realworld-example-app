package io.spring.application.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.golf.ContestEntry;
import io.spring.core.golf.ContestEntryRepository;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.user.User;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PickCommandServiceTest {
  private ContestEntryRepository contestEntryRepository;
  private PickRepository pickRepository;
  private PickCommandService service;
  private User user;
  private GolfContest contest;
  private Tournament tournament;

  @BeforeEach
  public void setUp() {
    contestEntryRepository = mock(ContestEntryRepository.class);
    pickRepository = mock(PickRepository.class);
    service = new PickCommandService(contestEntryRepository, pickRepository);
    user = new User("a@b.com", "golfer", "123", "", "");
    contest = new GolfContest("contest", "2026-27");
    tournament =
        new Tournament(
            contest.getId(),
            "The Open",
            "c",
            new DateTime().plusDays(5),
            new DateTime().plusDays(9));
  }

  @Test
  public void should_join_contest_once() {
    service.joinContest(contest, user);
    verify(contestEntryRepository).save(eq(new ContestEntry(contest.getId(), user.getId())));
  }

  @Test
  public void should_not_rejoin_when_already_entered() {
    when(contestEntryRepository.exists(eq(contest.getId()), eq(user.getId()))).thenReturn(true);
    service.joinContest(contest, user);
    verify(contestEntryRepository, never()).save(any());
  }

  @Test
  public void should_create_new_pick() {
    Golfer golfer = new Golfer("golfer-scheffler", "Scottie Scheffler", "USA");
    Pick pick = service.submitPick(new SubmitPickCommand(user, contest, tournament, golfer));
    assertEquals(golfer.getId(), pick.getGolferId());
    assertEquals(tournament.getId(), pick.getTournamentId());
    verify(pickRepository).save(eq(pick));
  }

  @Test
  public void should_swap_golfer_on_existing_pick() {
    Pick existing = new Pick(contest.getId(), tournament.getId(), user.getId(), "golfer-mcilroy");
    when(pickRepository.findByContestUserAndTournament(
            eq(contest.getId()), eq(user.getId()), eq(tournament.getId())))
        .thenReturn(Optional.of(existing));
    Golfer golfer = new Golfer("golfer-rahm", "Jon Rahm", "ESP");

    Pick pick = service.submitPick(new SubmitPickCommand(user, contest, tournament, golfer));

    assertSame(existing, pick);
    assertEquals("golfer-rahm", pick.getGolferId());
    verify(pickRepository).save(eq(existing));
  }
}
