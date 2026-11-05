package io.spring.core.golf;

import java.util.List;

public interface ContestEntryRepository {
  void save(ContestEntry entry);

  boolean exists(String contestId, String userId);

  List<ContestEntry> findByContestId(String contestId);
}
