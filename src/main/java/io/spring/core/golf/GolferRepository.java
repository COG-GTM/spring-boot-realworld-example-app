package io.spring.core.golf;

import java.util.List;
import java.util.Optional;

public interface GolferRepository {
  void save(Golfer golfer);

  Optional<Golfer> findById(String id);

  List<Golfer> findAll();
}
