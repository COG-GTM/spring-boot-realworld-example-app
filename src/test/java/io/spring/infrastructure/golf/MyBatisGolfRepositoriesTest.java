package io.spring.infrastructure.golf;

import io.spring.core.golf.ContestEntry;
import io.spring.core.golf.ContestEntryRepository;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.GolfContestRepository;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.GolferRepository;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import io.spring.core.golf.TournamentResult;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisContestEntryRepository;
import io.spring.infrastructure.repository.MyBatisGolfContestRepository;
import io.spring.infrastructure.repository.MyBatisGolferRepository;
import io.spring.infrastructure.repository.MyBatisPickRepository;
import io.spring.infrastructure.repository.MyBatisTournamentRepository;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;

@Import({
  MyBatisGolfContestRepository.class,
  MyBatisGolferRepository.class,
  MyBatisTournamentRepository.class,
  MyBatisPickRepository.class,
  MyBatisContestEntryRepository.class
})
public class MyBatisGolfRepositoriesTest extends DbTestBase {
  @Autowired private GolfContestRepository contestRepository;
  @Autowired private GolferRepository golferRepository;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private PickRepository pickRepository;
  @Autowired private ContestEntryRepository entryRepository;

  private GolfContest contest;
  private Tournament first;
  private Tournament second;
  private Golfer golferA;
  private Golfer golferB;

  @BeforeEach
  public void setUp() {
    contest = new GolfContest("Test Contest", "2030");
    contestRepository.save(contest);
    first =
        new Tournament(
            contest.getId(),
            "First Open",
            "Course A",
            new DateTime("2030-04-01T12:00:00Z"),
            new DateTime("2030-04-04T23:00:00Z"));
    second =
        new Tournament(
            contest.getId(),
            "Second Open",
            "Course B",
            new DateTime("2030-05-01T12:00:00Z"),
            new DateTime("2030-05-04T23:00:00Z"));
    tournamentRepository.save(first);
    tournamentRepository.save(second);
    golferA = new Golfer("Test Golfer A", "USA");
    golferB = new Golfer("Test Golfer B", "Spain");
    golferRepository.save(golferA);
    golferRepository.save(golferB);
  }

  @Test
  public void should_save_and_find_contest() {
    Optional<GolfContest> found = contestRepository.findById(contest.getId());
    Assertions.assertTrue(found.isPresent());
    Assertions.assertEquals("Test Contest", found.get().getName());
    Assertions.assertEquals("2030", found.get().getSeason());
    Assertions.assertNotNull(found.get().getCreatedAt());
    Assertions.assertTrue(contestRepository.findAll().contains(contest));
  }

  @Test
  public void should_save_and_find_golfer() {
    Golfer found = golferRepository.findById(golferA.getId()).get();
    Assertions.assertEquals("Test Golfer A", found.getName());
    Assertions.assertEquals("USA", found.getCountry());
    Assertions.assertTrue(golferRepository.findAll().contains(golferB));
  }

  @Test
  public void should_reject_duplicate_golfer_name() {
    Assertions.assertThrows(
        DataAccessException.class,
        () -> golferRepository.save(new Golfer("Test Golfer A", "Canada")));
  }

  @Test
  public void should_save_and_find_tournaments_in_start_order() {
    Tournament found = tournamentRepository.findById(first.getId()).get();
    Assertions.assertEquals("First Open", found.getName());
    Assertions.assertEquals(first.getStartsAt().getMillis(), found.getStartsAt().getMillis());
    Assertions.assertEquals(first.getEndsAt().getMillis(), found.getEndsAt().getMillis());

    List<Tournament> tournaments = tournamentRepository.findByContestId(contest.getId());
    Assertions.assertEquals(2, tournaments.size());
    Assertions.assertEquals(first.getId(), tournaments.get(0).getId());
    Assertions.assertEquals(second.getId(), tournaments.get(1).getId());
  }

  @Test
  public void should_save_and_update_tournament_results() {
    tournamentRepository.saveResult(new TournamentResult(first.getId(), golferB.getId(), 2, 500));
    tournamentRepository.saveResult(new TournamentResult(first.getId(), golferA.getId(), 1, 1000));
    tournamentRepository.saveResult(new TournamentResult(first.getId(), golferB.getId(), 3, 400));

    List<TournamentResult> results = tournamentRepository.findResultsByTournamentId(first.getId());
    Assertions.assertEquals(2, results.size());
    Assertions.assertEquals(golferA.getId(), results.get(0).getGolferId());
    Assertions.assertEquals(1000L, results.get(0).getPrizeMoney());
    Assertions.assertEquals(Integer.valueOf(3), results.get(1).getFinishPosition());
    Assertions.assertEquals(400L, results.get(1).getPrizeMoney());
  }

  @Test
  public void should_save_and_check_contest_entry() {
    Assertions.assertFalse(entryRepository.exists(contest.getId(), "user-1"));
    entryRepository.save(new ContestEntry(contest.getId(), "user-1"));
    entryRepository.save(new ContestEntry(contest.getId(), "user-1"));
    Assertions.assertTrue(entryRepository.exists(contest.getId(), "user-1"));
    Assertions.assertEquals(1, entryRepository.findByContestId(contest.getId()).size());
  }

  @Test
  public void should_save_and_find_picks() {
    Pick pick = new Pick(contest.getId(), first.getId(), "user-1", golferA.getId());
    pickRepository.save(pick);

    Pick found = pickRepository.findById(pick.getId()).get();
    Assertions.assertEquals(golferA.getId(), found.getGolferId());
    Assertions.assertEquals(first.getId(), found.getTournamentId());
    Assertions.assertNotNull(found.getCreatedAt());

    Assertions.assertTrue(
        pickRepository
            .findByContestUserAndTournament(contest.getId(), "user-1", first.getId())
            .isPresent());
    Assertions.assertFalse(
        pickRepository
            .findByContestUserAndTournament(contest.getId(), "user-1", second.getId())
            .isPresent());
    Assertions.assertTrue(
        pickRepository.existsByContestUserAndGolfer(contest.getId(), "user-1", golferA.getId()));
    Assertions.assertFalse(
        pickRepository.existsByContestUserAndGolfer(contest.getId(), "user-2", golferA.getId()));

    pickRepository.save(new Pick(contest.getId(), second.getId(), "user-1", golferB.getId()));
    pickRepository.save(new Pick(contest.getId(), first.getId(), "user-2", golferA.getId()));
    List<Pick> mine = pickRepository.findByContestAndUser(contest.getId(), "user-1");
    Assertions.assertEquals(2, mine.size());
    Assertions.assertEquals(first.getId(), mine.get(0).getTournamentId());
    Assertions.assertEquals(second.getId(), mine.get(1).getTournamentId());
    Assertions.assertEquals(3, pickRepository.findByContest(contest.getId()).size());
  }

  @Test
  public void should_update_existing_pick() {
    Pick pick = new Pick(contest.getId(), first.getId(), "user-1", golferA.getId());
    pickRepository.save(pick);
    pickRepository.save(
        new Pick(
            pick.getId(),
            contest.getId(),
            second.getId(),
            "user-1",
            golferB.getId(),
            pick.getCreatedAt()));

    Pick found = pickRepository.findById(pick.getId()).get();
    Assertions.assertEquals(second.getId(), found.getTournamentId());
    Assertions.assertEquals(golferB.getId(), found.getGolferId());
    Assertions.assertEquals(
        1, pickRepository.findByContestAndUser(contest.getId(), "user-1").size());
  }

  @Test
  public void should_reject_second_pick_for_same_tournament() {
    pickRepository.save(new Pick(contest.getId(), first.getId(), "user-1", golferA.getId()));
    Assertions.assertThrows(
        DataAccessException.class,
        () ->
            pickRepository.save(
                new Pick(contest.getId(), first.getId(), "user-1", golferB.getId())));
  }

  @Test
  public void should_reject_reusing_golfer_in_same_contest() {
    pickRepository.save(new Pick(contest.getId(), first.getId(), "user-1", golferA.getId()));
    Assertions.assertThrows(
        DataAccessException.class,
        () ->
            pickRepository.save(
                new Pick(contest.getId(), second.getId(), "user-1", golferA.getId())));
  }

  @Test
  public void should_load_seeded_demo_contest() {
    Assertions.assertTrue(contestRepository.findById("golf-contest-2026").isPresent());
    List<Tournament> tournaments = tournamentRepository.findByContestId("golf-contest-2026");
    Assertions.assertEquals(6, tournaments.size());
    DateTime cutoff = new DateTime("2026-11-01T00:00:00Z");
    Assertions.assertEquals(4, tournaments.stream().filter(t -> t.hasEnded(cutoff)).count());
    Assertions.assertEquals(2, tournaments.stream().filter(t -> !t.hasStarted(cutoff)).count());
    Assertions.assertEquals(
        12, tournamentRepository.findResultsByTournamentId("tourney-masters-2026").size());
    Assertions.assertEquals(
        4, pickRepository.findByContestAndUser("golf-contest-2026", "user-golfdemo1").size());
    Assertions.assertTrue(entryRepository.exists("golf-contest-2026", "user-golfdemo2"));
  }
}
