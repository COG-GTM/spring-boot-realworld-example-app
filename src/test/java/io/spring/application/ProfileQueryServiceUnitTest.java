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

public class ProfileQueryServiceUnitTest {

  private UserReadService userReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private ProfileQueryService profileQueryService;

  @BeforeEach
  void setUp() {
    userReadService = mock(UserReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    profileQueryService = new ProfileQueryService(userReadService, userRelationshipQueryService);
  }

  @Test
  public void should_return_empty_when_user_not_found() {
    when(userReadService.findByUsername("unknown")).thenReturn(null);
    Optional<ProfileData> result = profileQueryService.findByUsername("unknown", null);
    assertFalse(result.isPresent());
  }

  @Test
  public void should_return_profile_when_user_found_without_current_user() {
    UserData userData = new UserData("id1", "test@test.com", "testuser", "bio", "image");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);
    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", null);
    assertTrue(result.isPresent());
    assertEquals("testuser", result.get().getUsername());
    assertFalse(result.get().isFollowing());
  }

  @Test
  public void should_return_profile_with_following_true() {
    UserData userData = new UserData("id1", "test@test.com", "testuser", "bio", "image");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);
    User currentUser = new User("a@b.com", "current", "pass", "", "");
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "id1")).thenReturn(true);
    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", currentUser);
    assertTrue(result.isPresent());
    assertTrue(result.get().isFollowing());
  }

  @Test
  public void should_return_profile_with_following_false() {
    UserData userData = new UserData("id1", "test@test.com", "testuser", "bio", "image");
    when(userReadService.findByUsername("testuser")).thenReturn(userData);
    User currentUser = new User("a@b.com", "current", "pass", "", "");
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "id1")).thenReturn(false);
    Optional<ProfileData> result = profileQueryService.findByUsername("testuser", currentUser);
    assertTrue(result.isPresent());
    assertFalse(result.get().isFollowing());
  }
}
