package io.spring.infrastructure.repository;

import io.spring.core.golf.Golfer;
import io.spring.core.golf.GolferRepository;
import io.spring.infrastructure.mybatis.mapper.GolferMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisGolferRepository implements GolferRepository {
  private final GolferMapper mapper;

  public MyBatisGolferRepository(GolferMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(Golfer golfer) {
    if (mapper.findById(golfer.getId()) == null) {
      mapper.insert(golfer);
    } else {
      mapper.update(golfer);
    }
  }

  @Override
  public Optional<Golfer> findById(String id) {
    return Optional.ofNullable(mapper.findById(id));
  }

  @Override
  public List<Golfer> findAll() {
    return mapper.findAll();
  }
}
