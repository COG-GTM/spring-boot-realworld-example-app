package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserQueryServiceUnitTest {

  private final UserReadService userReadService = mock(UserReadService.class);
  private final UserQueryService userQueryService = new UserQueryService(userReadService);

  @Test
  void should_return_user_data_when_user_exists() {
    UserData userData = new UserData("123", "jake@jake.jake", "jake", "bio", "image");
    when(userReadService.findById("123")).thenReturn(userData);

    Optional<UserData> found = userQueryService.findById("123");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("jake");
    assertThat(found.get().getEmail()).isEqualTo("jake@jake.jake");
  }

  @Test
  void should_return_empty_when_user_not_found() {
    when(userReadService.findById("unknown")).thenReturn(null);

    assertThat(userQueryService.findById("unknown")).isEmpty();
  }
}
