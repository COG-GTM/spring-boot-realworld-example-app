package io.spring.core.golf;

import java.util.List;
import java.util.Optional;

public interface PickRepository {
  void save(Pick pick);

  Optional<Pick> findById(String id);

  List<Pick> findByContestAndUser(String contestId, String userId);

  Optional<Pick> findByContestUserAndTournament(
      String contestId, String userId, String tournamentId);

  boolean existsByContestUserAndGolfer(String contestId, String userId, String golferId);

  List<Pick> findByContest(String contestId);
}
