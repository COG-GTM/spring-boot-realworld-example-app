package io.spring.application.golf;

import io.spring.core.golf.PickRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import io.spring.core.golf.TournamentResult;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class TournamentResultsCommandService {
  private TournamentRepository tournamentRepository;
  private PickRepository pickRepository;

  /**
   * Records the final results of a tournament, replacing any earlier submission, and credits every
   * pick for the tournament with its golfer's prize money.
   *
   * @return the number of picks credited
   */
  @Transactional
  public int recordResults(@Valid RecordResultsCommand command) {
    Tournament tournament = command.getTournament();
    List<TournamentResult> results =
        command.getParam().getResults().stream()
            .map(
                r ->
                    new TournamentResult(
                        tournament.getId(), r.getGolferId(), r.getPosition(), r.getPrizeMoney()))
            .collect(Collectors.toList());
    tournamentRepository.replaceResults(tournament.getId(), results);
    return pickRepository.applyTournamentResults(tournament.getId());
  }
}
