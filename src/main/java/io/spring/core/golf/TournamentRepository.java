package io.spring.core.golf;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository {
  void save(Tournament tournament);

  Optional<Tournament> findById(String id);

  List<Tournament> findByContestId(String contestId);

  void saveResult(TournamentResult result);

  List<TournamentResult> findResultsByTournamentId(String tournamentId);

  /** Replaces all results of the tournament with the given ones. */
  void replaceResults(String tournamentId, List<TournamentResult> results);
}
