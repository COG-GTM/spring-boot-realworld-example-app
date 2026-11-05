package io.spring.application.golf;

import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfPickData;
import io.spring.application.data.GolfTournamentData;
import io.spring.application.data.GolferData;
import io.spring.application.data.MyGolfPicksData;
import io.spring.application.data.TournamentStatus;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.GolfReadService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GolfQueryService {
  private GolfReadService golfReadService;

  public List<GolfContestData> allContests() {
    return golfReadService.allContests();
  }

  public Optional<GolfContestData> findContest(String contestId, User user) {
    GolfContestData contest = golfReadService.findContestById(contestId);
    if (contest == null) {
      return Optional.empty();
    }
    DateTime now = new DateTime();
    List<GolfTournamentData> tournaments = golfReadService.findTournamentsByContest(contestId);
    tournaments.forEach(
        t ->
            t.setStatus(
                TournamentStatus.of(t.getStartsAt(), t.getEndsAt(), t.isHasResults(), now)));
    contest.setTournaments(tournaments);
    if (user != null) {
      contest.setEntered(golfReadService.isEntered(contestId, user.getId()));
    }
    return Optional.of(contest);
  }

  public List<GolferData> allGolfers() {
    return golfReadService.allGolfers();
  }

  public Optional<GolfPickData> findPick(String pickId) {
    return Optional.ofNullable(golfReadService.findPickById(pickId))
        .map(pick -> withStatus(pick, new DateTime()));
  }

  public MyGolfPicksData findMyPicks(String contestId, User user) {
    DateTime now = new DateTime();
    List<GolfPickData> picks =
        golfReadService.findPicksByContestAndUser(contestId, user.getId()).stream()
            .map(pick -> withStatus(pick, now))
            .collect(Collectors.toList());
    Set<String> usedGolferIds =
        picks.stream().map(pick -> pick.getGolfer().getId()).collect(Collectors.toSet());
    List<GolferData> available =
        golfReadService.allGolfers().stream()
            .filter(golfer -> !usedGolferIds.contains(golfer.getId()))
            .collect(Collectors.toList());
    return new MyGolfPicksData(
        contestId, golfReadService.isEntered(contestId, user.getId()), picks, available);
  }

  private GolfPickData withStatus(GolfPickData pick, DateTime now) {
    TournamentStatus status =
        TournamentStatus.of(
            pick.getTournamentStartsAt(), pick.getTournamentEndsAt(), pick.isHasResults(), now);
    pick.setTournamentStatus(status);
    if (status == TournamentStatus.COMPLETED) {
      if (pick.getPrizeMoney() == null) {
        pick.setPrizeMoney(0L);
      }
    } else {
      pick.setPrizeMoney(null);
      pick.setFinishPosition(null);
    }
    return pick;
  }
}
