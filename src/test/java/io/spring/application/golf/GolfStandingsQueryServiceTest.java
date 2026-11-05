package io.spring.application.golf;

import io.spring.application.GolfStandingsQueryService;
import io.spring.application.data.GolfStandingData;
import io.spring.application.data.GolfStandingsData;
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
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisContestEntryRepository;
import io.spring.infrastructure.repository.MyBatisGolfContestRepository;
import io.spring.infrastructure.repository.MyBatisGolferRepository;
import io.spring.infrastructure.repository.MyBatisPickRepository;
import io.spring.infrastructure.repository.MyBatisTournamentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  GolfStandingsQueryService.class,
  MyBatisUserRepository.class,
  MyBatisGolfContestRepository.class,
  MyBatisGolferRepository.class,
  MyBatisTournamentRepository.class,
  MyBatisContestEntryRepository.class,
  MyBatisPickRepository.class
})
public class GolfStandingsQueryServiceTest extends DbTestBase {
  @Autowired private GolfStandingsQueryService golfStandingsQueryService;
  @Autowired private UserRepository userRepository;
  @Autowired private GolfContestRepository golfContestRepository;
  @Autowired private GolferRepository golferRepository;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private ContestEntryRepository contestEntryRepository;
  @Autowired private PickRepository pickRepository;

  private GolfContest contest;
  private Tournament completedA;
  private Tournament completedB;
  private Tournament pending;
  private Golfer golferX;
  private Golfer golferY;
  private Golfer golferZ;

  @BeforeEach
  public void setUp() {
    contest = new GolfContest("Test One-and-Done", "2030-31");
    golfContestRepository.save(contest);

    golferX = new Golfer("Test Golfer X", "USA");
    golferY = new Golfer("Test Golfer Y", "NIR");
    golferZ = new Golfer("Test Golfer Z", "ESP");
    golferRepository.save(golferX);
    golferRepository.save(golferY);
    golferRepository.save(golferZ);

    DateTime base = new DateTime(2030, 4, 1, 12, 0);
    completedA = new Tournament(contest.getId(), "Test Open A", "Course A", base, base.plusDays(3));
    completedB =
        new Tournament(
            contest.getId(),
            "Test Open B",
            "Course B",
            base.plusWeeks(2),
            base.plusWeeks(2).plusDays(3));
    pending =
        new Tournament(
            contest.getId(),
            "Test Open C",
            "Course C",
            base.plusWeeks(4),
            base.plusWeeks(4).plusDays(3));
    tournamentRepository.save(completedA);
    tournamentRepository.save(completedB);
    tournamentRepository.save(pending);

    tournamentRepository.saveResult(
        new TournamentResult(completedA.getId(), golferX.getId(), 1, 1000));
    tournamentRepository.saveResult(
        new TournamentResult(completedA.getId(), golferY.getId(), 2, 500));
    tournamentRepository.saveResult(
        new TournamentResult(completedA.getId(), golferZ.getId(), 3, 300));
    tournamentRepository.saveResult(
        new TournamentResult(completedB.getId(), golferX.getId(), 2, 400));
    tournamentRepository.saveResult(
        new TournamentResult(completedB.getId(), golferY.getId(), 1, 800));
    tournamentRepository.saveResult(
        new TournamentResult(completedB.getId(), golferZ.getId(), 3, 200));
  }

  private User entrant(String username) {
    User user = new User(username + "@test.com", username, "123", "", "");
    userRepository.save(user);
    contestEntryRepository.save(new ContestEntry(contest.getId(), user.getId()));
    return user;
  }

  private void pick(User user, Tournament tournament, Golfer golfer) {
    pickRepository.save(
        new Pick(contest.getId(), tournament.getId(), user.getId(), golfer.getId()));
  }

  private List<String> usernames(List<GolfStandingData> standings) {
    return standings.stream().map(GolfStandingData::getUsername).collect(Collectors.toList());
  }

  private List<Integer> ranks(List<GolfStandingData> standings) {
    return standings.stream().map(GolfStandingData::getRank).collect(Collectors.toList());
  }

  @Test
  public void should_return_empty_for_unknown_contest() {
    Assertions.assertFalse(
        golfStandingsQueryService.findByContestId("no-such-contest").isPresent());
  }

  @Test
  public void should_return_contest_with_no_standings_when_nobody_entered() {
    GolfStandingsData data = golfStandingsQueryService.findByContestId(contest.getId()).get();
    Assertions.assertEquals(contest.getId(), data.getContest().getId());
    Assertions.assertEquals("Test One-and-Done", data.getContest().getName());
    Assertions.assertEquals("2030-31", data.getContest().getSeason());
    Assertions.assertTrue(data.getStandings().isEmpty());
  }

  @Test
  public void should_sort_by_prize_money_desc_and_sum_picks() {
    User low = entrant("low");
    User high = entrant("high");
    pick(low, completedA, golferZ); // 300
    pick(high, completedA, golferX); // 1000
    pick(high, completedB, golferY); // 800

    List<GolfStandingData> standings =
        golfStandingsQueryService.findByContestId(contest.getId()).get().getStandings();

    Assertions.assertEquals(List.of("high", "low"), usernames(standings));
    Assertions.assertEquals(List.of(1, 2), ranks(standings));
    GolfStandingData leader = standings.get(0);
    Assertions.assertEquals(1800L, leader.getTotalPrizeMoney());
    Assertions.assertEquals(2, leader.getPicksMade());
    Assertions.assertEquals(2, leader.getPicks().size());
    Assertions.assertEquals("Test Open A", leader.getPicks().get(0).getTournamentName());
    Assertions.assertEquals("Test Golfer X", leader.getPicks().get(0).getGolferName());
    Assertions.assertEquals(1000L, leader.getPicks().get(0).getPrizeMoney());
    Assertions.assertEquals("Test Open B", leader.getPicks().get(1).getTournamentName());
    Assertions.assertEquals(800L, leader.getPicks().get(1).getPrizeMoney());
    Assertions.assertEquals(300L, standings.get(1).getTotalPrizeMoney());
  }

  @Test
  public void should_use_competition_ranking_and_username_tiebreak() {
    User top = entrant("top");
    User zed = entrant("zed");
    User amy = entrant("amy");
    User last = entrant("last");
    pick(top, completedA, golferX); // 1000
    pick(zed, completedA, golferY); // 500
    pick(amy, completedA, golferY); // 500
    pick(last, completedA, golferZ); // 300

    List<GolfStandingData> standings =
        golfStandingsQueryService.findByContestId(contest.getId()).get().getStandings();

    Assertions.assertEquals(List.of("top", "amy", "zed", "last"), usernames(standings));
    Assertions.assertEquals(List.of(1, 2, 2, 4), ranks(standings));
  }

  @Test
  public void should_include_entrant_without_picks_with_zero() {
    User picker = entrant("picker");
    entrant("lurker");
    pick(picker, completedA, golferZ);

    List<GolfStandingData> standings =
        golfStandingsQueryService.findByContestId(contest.getId()).get().getStandings();

    Assertions.assertEquals(List.of("picker", "lurker"), usernames(standings));
    GolfStandingData lurker = standings.get(1);
    Assertions.assertEquals(0L, lurker.getTotalPrizeMoney());
    Assertions.assertEquals(0, lurker.getPicksMade());
    Assertions.assertTrue(lurker.getPicks().isEmpty());
    Assertions.assertEquals(2, lurker.getRank());
  }

  @Test
  public void should_count_pending_tournament_pick_as_zero_with_null_prize() {
    User user = entrant("pending");
    pick(user, completedA, golferZ); // 300
    pick(user, pending, golferX); // no results yet

    GolfStandingData row =
        golfStandingsQueryService.findByContestId(contest.getId()).get().getStandings().get(0);

    Assertions.assertEquals(300L, row.getTotalPrizeMoney());
    Assertions.assertEquals(2, row.getPicksMade());
    Assertions.assertEquals(300L, row.getPicks().get(0).getPrizeMoney());
    Assertions.assertEquals("Test Open C", row.getPicks().get(1).getTournamentName());
    Assertions.assertNull(row.getPicks().get(1).getPrizeMoney());
  }

  @Test
  public void should_rank_seeded_demo_contest() {
    Optional<GolfStandingsData> data =
        golfStandingsQueryService.findByContestId("golf-contest-2026");
    Assertions.assertTrue(data.isPresent());
    List<GolfStandingData> standings = data.get().getStandings();
    Assertions.assertEquals(List.of("golfdemo1", "golfdemo2", "golfdemo3"), usernames(standings));
    Assertions.assertEquals(List.of(1, 2, 3), ranks(standings));
    Assertions.assertEquals(9944000L, standings.get(0).getTotalPrizeMoney());
    Assertions.assertEquals(8856000L, standings.get(1).getTotalPrizeMoney());
    Assertions.assertEquals(3868500L, standings.get(2).getTotalPrizeMoney());
    Assertions.assertEquals(4, standings.get(0).getPicksMade());
  }
}
