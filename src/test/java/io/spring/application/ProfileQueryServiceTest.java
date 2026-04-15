package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileQueryServiceTest {

  @Mock private UserReadService userReadService;

  @Mock private UserRelationshipQueryService userRelationshipQueryService;

  private ProfileQueryService profileQueryService;

  @BeforeEach
  void setUp() {
    profileQueryService = new ProfileQueryService(userReadService, userRelationshipQueryService);
  }

  @Test
  void should_find_profile_by_username() {
    UserData userData = new UserData("userId1", "test@example.com", "testuser", "bio", "image.jpg");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);

    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", null);

    assertTrue(result.isPresent());
    assertEquals("testuser", result.get().getUsername());
    assertEquals("bio", result.get().getBio());
    assertEquals("image.jpg", result.get().getImage());
    assertFalse(result.get().isFollowing());
  }

  @Test
  void should_return_empty_when_user_not_found() {
    when(userReadService.findByUsername("nonexistent")).thenReturn(null);

    Optional<ProfileData> result = profileQueryService.findByUsername("nonexistent", null);

    assertFalse(result.isPresent());
  }

  @Test
  void should_return_following_status_when_current_user_follows() {
    UserData userData = new UserData("userId1", "test@example.com", "testuser", "bio", "image.jpg");
    User currentUser = new User("current@example.com", "currentuser", "pass", "", "");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "userId1"))
        .thenReturn(true);

    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", currentUser);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFollowing());
  }

  @Test
  void should_return_not_following_when_current_user_does_not_follow() {
    UserData userData = new UserData("userId1", "test@example.com", "testuser", "bio", "image.jpg");
    User currentUser = new User("current@example.com", "currentuser", "pass", "", "");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "userId1"))
        .thenReturn(false);

    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", currentUser);

    assertTrue(result.isPresent());
    assertFalse(result.get().isFollowing());
  }
}
