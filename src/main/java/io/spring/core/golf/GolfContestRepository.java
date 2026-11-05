package io.spring.core.golf;

import java.util.List;
import java.util.Optional;

public interface GolfContestRepository {
  void save(GolfContest contest);

  Optional<GolfContest> findById(String id);

  List<GolfContest> findAll();
}
