package io.spring.infrastructure.repository;

import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import io.spring.core.golf.TournamentResult;
import io.spring.infrastructure.mybatis.mapper.TournamentMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisTournamentRepository implements TournamentRepository {
  private final TournamentMapper mapper;

  public MyBatisTournamentRepository(TournamentMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(Tournament tournament) {
    if (mapper.findById(tournament.getId()) == null) {
      mapper.insert(tournament);
    } else {
      mapper.update(tournament);
    }
  }

  @Override
  public Optional<Tournament> findById(String id) {
    return Optional.ofNullable(mapper.findById(id));
  }

  @Override
  public List<Tournament> findByContestId(String contestId) {
    return mapper.findByContestId(contestId);
  }

  @Override
  public void saveResult(TournamentResult result) {
    if (mapper.findResult(result.getTournamentId(), result.getGolferId()) == null) {
      mapper.insertResult(result);
    } else {
      mapper.updateResult(result);
    }
  }

  @Override
  public List<TournamentResult> findResultsByTournamentId(String tournamentId) {
    return mapper.findResultsByTournamentId(tournamentId);
  }
}
