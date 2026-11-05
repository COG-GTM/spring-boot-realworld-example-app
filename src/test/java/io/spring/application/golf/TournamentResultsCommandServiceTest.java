package io.spring.application.golf;

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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;

@Import({
  ValidationAutoConfiguration.class,
  TournamentResultsCommandService.class,
  MyBatisGolfContestRepository.class,
  MyBatisGolferRepository.class,
  MyBatisTournamentRepository.class,
  MyBatisPickRepository.class,
  MyBatisContestEntryRepository.class
})
public class TournamentResultsCommandServiceTest extends DbTestBase {
  @Autowired private TournamentResultsCommandService service;
  @Autowired private GolfContestRepository contestRepository;
  @Autowired private GolferRepository golferRepository;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private PickRepository pickRepository;
  @Autowired private ContestEntryRepository entryRepository;

  private Tournament finished;
  private Tournament upcoming;
  private Golfer golferA;
  private Golfer golferB;
  private Golfer golferC;
  private Pick pickA;
  private Pick pickB;

  @BeforeEach
  public void setUp() {
    GolfContest contest = new GolfContest("Results Test Contest", "2031");
    contestRepository.save(contest);
    finished =
        new Tournament(
            contest.getId(),
            "Finished Open",
            "Course A",
            new DateTime().minusDays(5),
            new DateTime().minusDays(2));
    upcoming =
        new Tournament(
            contest.getId(),
            "Upcoming Open",
            "Course B",
            new DateTime().plusDays(5),
            new DateTime().plusDays(8));
    tournamentRepository.save(finished);
    tournamentRepository.save(upcoming);
    golferA = new Golfer("Results Golfer A", "USA");
    golferB = new Golfer("Results Golfer B", "USA");
    golferC = new Golfer("Results Golfer C", "USA");
    golferRepository.save(golferA);
    golferRepository.save(golferB);
    golferRepository.save(golferC);

    entryRepository.save(new ContestEntry(contest.getId(), "results-user-1"));
    entryRepository.save(new ContestEntry(contest.getId(), "results-user-2"));
    pickA = new Pick(contest.getId(), finished.getId(), "results-user-1", golferA.getId());
    pickB = new Pick(contest.getId(), finished.getId(), "results-user-2", golferB.getId());
    pickRepository.save(pickA);
    pickRepository.save(pickB);
  }

  @Test
  public void should_record_results_and_credit_picks() {
    int updated =
        service.recordResults(
            command(finished, result(golferA, 1, 1000000L), result(golferB, 2, 500000L)));

    Assertions.assertEquals(2, updated);
    Assertions.assertEquals(1000000L, prizeOf(pickA));
    Assertions.assertEquals(500000L, prizeOf(pickB));
    List<TournamentResult> results =
        tournamentRepository.findResultsByTournamentId(finished.getId());
    Assertions.assertEquals(2, results.size());
    Assertions.assertEquals(golferA.getId(), results.get(0).getGolferId());
    Assertions.assertEquals(1, results.get(0).getFinishPosition());
  }

  @Test
  public void should_credit_zero_to_picks_whose_golfer_has_no_result() {
    service.recordResults(command(finished, result(golferA, 1, 1000000L)));

    Assertions.assertEquals(1000000L, prizeOf(pickA));
    Assertions.assertEquals(0L, prizeOf(pickB));
  }

  @Test
  public void should_replace_results_on_resubmit() {
    service.recordResults(
        command(finished, result(golferA, 1, 1000000L), result(golferB, 2, 500000L)));

    service.recordResults(
        command(finished, result(golferB, 1, 1200000L), result(golferC, 2, 600000L)));

    Assertions.assertEquals(0L, prizeOf(pickA));
    Assertions.assertEquals(1200000L, prizeOf(pickB));
    List<String> golfers =
        tournamentRepository.findResultsByTournamentId(finished.getId()).stream()
            .map(TournamentResult::getGolferId)
            .collect(Collectors.toList());
    Assertions.assertEquals(Arrays.asList(golferB.getId(), golferC.getId()), golfers);
  }

  @Test
  public void should_reject_results_for_tournament_not_started() {
    ConstraintViolationException e =
        Assertions.assertThrows(
            ConstraintViolationException.class,
            () -> service.recordResults(command(upcoming, result(golferA, 1, 1000000L))));

    Assertions.assertTrue(
        messages(e).contains(ValidTournamentResultsValidator.NOT_STARTED), messages(e).toString());
    Assertions.assertTrue(
        tournamentRepository.findResultsByTournamentId(upcoming.getId()).isEmpty());
  }

  @Test
  public void should_reject_unknown_and_duplicate_golfers() {
    GolferResultParam unknown = new GolferResultParam("no-such-golfer", 3, 100L);
    ConstraintViolationException e =
        Assertions.assertThrows(
            ConstraintViolationException.class,
            () ->
                service.recordResults(
                    new RecordResultsCommand(
                        finished,
                        new TournamentResultsParam(
                            Arrays.asList(
                                result(golferA, 1, 10L), result(golferA, 2, 5L), unknown)))));

    List<String> messages = messages(e);
    Assertions.assertTrue(
        messages.contains("golfer " + golferA.getId() + " is listed more than once"));
    Assertions.assertTrue(messages.contains("golfer no-such-golfer not found"));
    Assertions.assertNull(pickRepository.findById(pickA.getId()).get().getPrizeMoney());
  }

  private long prizeOf(Pick pick) {
    return pickRepository.findById(pick.getId()).get().getPrizeMoney();
  }

  private static List<String> messages(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList());
  }

  private static GolferResultParam result(Golfer golfer, Integer position, long prize) {
    return new GolferResultParam(golfer.getId(), position, prize);
  }

  private static RecordResultsCommand command(Tournament tournament, GolferResultParam... results) {
    return new RecordResultsCommand(tournament, new TournamentResultsParam(Arrays.asList(results)));
  }
}
