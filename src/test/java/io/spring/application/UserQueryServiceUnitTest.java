package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserQueryServiceUnitTest {

  private UserReadService userReadService;
  private UserQueryService userQueryService;

  @BeforeEach
  void setUp() {
    userReadService = mock(UserReadService.class);
    userQueryService = new UserQueryService(userReadService);
  }

  @Test
  public void should_return_user_when_found() {
    UserData userData = new UserData("id1", "test@test.com", "testuser", "bio", "image");
    when(userReadService.findById("id1")).thenReturn(userData);
    Optional<UserData> result = userQueryService.findById("id1");
    assertTrue(result.isPresent());
    assertEquals("testuser", result.get().getUsername());
  }

  @Test
  public void should_return_empty_when_not_found() {
    when(userReadService.findById("unknown")).thenReturn(null);
    Optional<UserData> result = userQueryService.findById("unknown");
    assertFalse(result.isPresent());
  }
}
