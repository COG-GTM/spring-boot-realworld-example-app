package io.spring.infrastructure.repository;

import io.spring.core.golf.ContestEntry;
import io.spring.core.golf.ContestEntryRepository;
import io.spring.infrastructure.mybatis.mapper.ContestEntryMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisContestEntryRepository implements ContestEntryRepository {
  private final ContestEntryMapper mapper;

  public MyBatisContestEntryRepository(ContestEntryMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(ContestEntry entry) {
    mapper.insert(entry);
  }

  @Override
  public boolean exists(String contestId, String userId) {
    return mapper.find(contestId, userId) != null;
  }

  @Override
  public List<ContestEntry> findByContestId(String contestId) {
    return mapper.findByContestId(contestId);
  }
}
