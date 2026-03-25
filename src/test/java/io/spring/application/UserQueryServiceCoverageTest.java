package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserQueryServiceCoverageTest {

  private UserReadService userReadService;
  private UserQueryService userQueryService;

  @BeforeEach
  void setUp() {
    userReadService = mock(UserReadService.class);
    userQueryService = new UserQueryService(userReadService);
  }

  @Test
  public void should_find_by_id_returns_data() {
    UserData userData = new UserData("id1", "test@test.com", "user", "bio", "img");
    when(userReadService.findById("id1")).thenReturn(userData);

    Optional<UserData> result = userQueryService.findById("id1");

    assertTrue(result.isPresent());
    assertEquals("id1", result.get().getId());
  }

  @Test
  public void should_find_by_id_returns_empty_when_not_found() {
    when(userReadService.findById("unknown")).thenReturn(null);

    Optional<UserData> result = userQueryService.findById("unknown");

    assertTrue(result.isEmpty());
  }
}
