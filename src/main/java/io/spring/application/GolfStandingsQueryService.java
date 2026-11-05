package io.spring.application;

import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfStandingData;
import io.spring.application.data.GolfStandingsData;
import io.spring.infrastructure.mybatis.readservice.GolfStandingsReadService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GolfStandingsQueryService {
  private GolfStandingsReadService golfStandingsReadService;

  public Optional<GolfStandingsData> findByContestId(String contestId) {
    GolfContestData contest = golfStandingsReadService.findContestById(contestId);
    if (contest == null) {
      return Optional.empty();
    }
    List<GolfStandingData> standings = golfStandingsReadService.findStandings(contestId);
    assignCompetitionRanks(standings);
    return Optional.of(new GolfStandingsData(contest, standings));
  }

  /** Standard competition ranking (1, 2, 2, 4) over rows already sorted by prize money desc. */
  private void assignCompetitionRanks(List<GolfStandingData> standings) {
    for (int i = 0; i < standings.size(); i++) {
      GolfStandingData current = standings.get(i);
      if (i > 0 && current.getTotalPrizeMoney() == standings.get(i - 1).getTotalPrizeMoney()) {
        current.setRank(standings.get(i - 1).getRank());
      } else {
        current.setRank(i + 1);
      }
    }
  }
}
