package io.spring.infrastructure.mybatis.readservice;

import io.spring.core.article.TagReadService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TagReadServiceImpl implements TagReadService {
  private final MyBatisTagReadServiceMapper mapper;

  public TagReadServiceImpl(MyBatisTagReadServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<String> all() {
    return mapper.all();
  }
}
