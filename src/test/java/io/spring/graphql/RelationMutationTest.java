package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;
  private User user;
  private User targetUser;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
    targetUser = new User("target@test.com", "targetuser", "password", "bio2", "image2");

    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));

    ProfileData profileData =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            true);
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(profileData));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_follow_user() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));

    ProfilePayload result = relationMutation.follow("targetuser");
    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("targetuser", result.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_throw_when_follow_nonexistent_user() {
    when(userRepository.findByUsername(eq("noone"))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("noone"));
  }

  @Test
  public void should_throw_when_not_authenticated_for_follow() {
    SecurityContextHolder.clearContext();
    assertThrows(Exception.class, () -> relationMutation.follow("targetuser"));
  }

  @Test
  public void should_unfollow_user() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    FollowRelation relation = new FollowRelation(user.getId(), targetUser.getId());
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));

    ProfilePayload result = relationMutation.unfollow("targetuser");
    assertNotNull(result);
    verify(userRepository).removeRelation(relation);
  }

  @Test
  public void should_throw_when_unfollow_nonexistent_user() {
    when(userRepository.findByUsername(eq("noone"))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("noone"));
  }

  @Test
  public void should_throw_when_no_relation_to_unfollow() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("targetuser"));
  }
}
