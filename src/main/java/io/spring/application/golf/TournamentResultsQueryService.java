package io.spring.application.golf;

import io.spring.application.data.GolferResultData;
import io.spring.application.data.TournamentResultsData;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.GolferRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TournamentResultsQueryService {
  private TournamentRepository tournamentRepository;
  private GolferRepository golferRepository;

  public TournamentResultsData findResults(Tournament tournament) {
    Map<String, String> golferNames =
        golferRepository.findAll().stream()
            .collect(Collectors.toMap(Golfer::getId, Golfer::getName, (a, b) -> a));
    List<GolferResultData> results =
        tournamentRepository.findResultsByTournamentId(tournament.getId()).stream()
            .map(
                r ->
                    new GolferResultData(
                        r.getGolferId(),
                        golferNames.getOrDefault(r.getGolferId(), r.getGolferId()),
                        r.getFinishPosition(),
                        r.getPrizeMoney()))
            .collect(Collectors.toList());
    return new TournamentResultsData(tournament.getId(), tournament.getName(), results);
  }
}
