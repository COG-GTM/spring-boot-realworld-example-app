package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.UserData;
import io.spring.core.user.UserReadService;
import org.springframework.stereotype.Service;

@Service
public class UserReadServiceImpl implements UserReadService {
  private final MyBatisUserReadServiceMapper mapper;

  public UserReadServiceImpl(MyBatisUserReadServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public UserData findByUsername(String username) {
    return mapper.findByUsername(username);
  }

  @Override
  public UserData findById(String id) {
    return mapper.findById(id);
  }
}
