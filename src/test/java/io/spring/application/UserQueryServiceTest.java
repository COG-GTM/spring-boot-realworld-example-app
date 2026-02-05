package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserQueryServiceTest {

  @Mock private UserReadService userReadService;

  private UserQueryService userQueryService;

  @BeforeEach
  public void setUp() {
    userQueryService = new UserQueryService(userReadService);
  }

  @Test
  public void should_return_user_data_when_found() {
    String userId = "user123";
    UserData userData =
        new UserData(userId, "testuser@example.com", "testuser", "bio text", "image.jpg");
    when(userReadService.findById(userId)).thenReturn(userData);

    Optional<UserData> result = userQueryService.findById(userId);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getId(), is(userId));
    assertThat(result.get().getUsername(), is("testuser"));
    assertThat(result.get().getEmail(), is("testuser@example.com"));
  }

  @Test
  public void should_return_empty_optional_when_not_found() {
    String userId = "nonexistent";
    when(userReadService.findById(userId)).thenReturn(null);

    Optional<UserData> result = userQueryService.findById(userId);

    assertThat(result.isPresent(), is(false));
  }
}
