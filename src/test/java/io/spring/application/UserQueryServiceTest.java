package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserQueryServiceTest {

  @Mock private UserReadService userReadService;

  @InjectMocks private UserQueryService userQueryService;

  @Test
  void should_return_user_data_when_user_exists() {
    UserData userData = new UserData("user-id-123", "user@test.com", "testuser", "bio", "image.png");
    when(userReadService.findById("user-id-123")).thenReturn(userData);

    Optional<UserData> result = userQueryService.findById("user-id-123");

    assertTrue(result.isPresent());
    assertEquals("user-id-123", result.get().getId());
    assertEquals("user@test.com", result.get().getEmail());
    assertEquals("testuser", result.get().getUsername());
    verify(userReadService).findById("user-id-123");
  }

  @Test
  void should_return_empty_when_user_does_not_exist() {
    when(userReadService.findById("nonexistent-id")).thenReturn(null);

    Optional<UserData> result = userQueryService.findById("nonexistent-id");

    assertFalse(result.isPresent());
    verify(userReadService).findById("nonexistent-id");
  }
}
