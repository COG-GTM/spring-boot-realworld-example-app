package io.spring.infrastructure.repository;

import io.spring.core.golf.GolfContest;
import io.spring.core.golf.GolfContestRepository;
import io.spring.infrastructure.mybatis.mapper.GolfContestMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisGolfContestRepository implements GolfContestRepository {
  private final GolfContestMapper mapper;

  public MyBatisGolfContestRepository(GolfContestMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(GolfContest contest) {
    if (mapper.findById(contest.getId()) == null) {
      mapper.insert(contest);
    } else {
      mapper.update(contest);
    }
  }

  @Override
  public Optional<GolfContest> findById(String id) {
    return Optional.ofNullable(mapper.findById(id));
  }

  @Override
  public List<GolfContest> findAll() {
    return mapper.findAll();
  }
}
