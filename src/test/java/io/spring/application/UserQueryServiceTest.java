package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserQueryServiceTest {

  private UserReadService userReadService;
  private UserQueryService userQueryService;

  @BeforeEach
  void setUp() {
    userReadService = mock(UserReadService.class);
    userQueryService = new UserQueryService(userReadService);
  }

  @Test
  void should_return_user_when_found() {
    UserData userData = new UserData("id", "email@test.com", "user", "bio", "image");
    when(userReadService.findById("id")).thenReturn(userData);

    Optional<UserData> result = userQueryService.findById("id");

    assertTrue(result.isPresent());
    assertEquals("user", result.get().getUsername());
  }

  @Test
  void should_return_empty_when_not_found() {
    when(userReadService.findById("missing")).thenReturn(null);

    Optional<UserData> result = userQueryService.findById("missing");

    assertFalse(result.isPresent());
  }
}
