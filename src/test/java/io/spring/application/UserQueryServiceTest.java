package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserQueryServiceTest {
  private UserReadService userReadService;
  private UserQueryService userQueryService;

  @BeforeEach
  public void setUp() {
    userReadService = mock(UserReadService.class);
    userQueryService = new UserQueryService(userReadService);
  }

  @Test
  public void should_return_user_data_when_user_exists() {
    User user = TestHelper.userFixture("aisensiy");
    UserData userData = TestHelper.userDataFixture(user);
    when(userReadService.findById(user.getId())).thenReturn(userData);

    Optional<UserData> optional = userQueryService.findById(user.getId());

    assertThat(optional).hasValue(userData);
    verify(userReadService).findById(user.getId());
  }

  @Test
  public void should_return_empty_when_user_not_found() {
    when(userReadService.findById("not-exists")).thenReturn(null);

    assertThat(userQueryService.findById("not-exists")).isEmpty();
  }

  @Test
  public void should_return_empty_for_null_id() {
    when(userReadService.findById(null)).thenReturn(null);

    assertThat(userQueryService.findById(null)).isEmpty();
  }
}
