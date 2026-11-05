package io.spring.infrastructure.repository;

import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.infrastructure.mybatis.mapper.PickMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisPickRepository implements PickRepository {
  private final PickMapper mapper;

  public MyBatisPickRepository(PickMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(Pick pick) {
    if (mapper.findById(pick.getId()) == null) {
      mapper.insert(pick);
    } else {
      mapper.update(pick);
    }
  }

  @Override
  public Optional<Pick> findById(String id) {
    return Optional.ofNullable(mapper.findById(id));
  }

  @Override
  public List<Pick> findByContestAndUser(String contestId, String userId) {
    return mapper.findByContestAndUser(contestId, userId);
  }

  @Override
  public Optional<Pick> findByContestUserAndTournament(
      String contestId, String userId, String tournamentId) {
    return Optional.ofNullable(
        mapper.findByContestUserAndTournament(contestId, userId, tournamentId));
  }

  @Override
  public boolean existsByContestUserAndGolfer(String contestId, String userId, String golferId) {
    return mapper.countByContestUserAndGolfer(contestId, userId, golferId) > 0;
  }

  @Override
  public List<Pick> findByContest(String contestId) {
    return mapper.findByContest(contestId);
  }
}
