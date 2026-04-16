package io.spring.core.user;

import io.spring.application.data.UserData;

public interface UserReadService {
  UserData findByUsername(String username);

  UserData findById(String id);
}
