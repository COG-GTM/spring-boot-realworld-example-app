package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation mutation;
  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    mutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("user@test.com", "user", "123", "bio", "image");
    target = new User("target@test.com", "target", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private ProfileData profileData() {
    return new ProfileData("id", "target", "bio", "image", true);
  }

  @Test
  void follow_saves_relation_and_returns_profile() {
    GraphQLTestSecurity.login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData()));

    ProfilePayload payload = mutation.follow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void follow_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.follow("target"));
  }

  @Test
  void follow_when_target_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.follow("target"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void unfollow_removes_relation_and_returns_profile() {
    GraphQLTestSecurity.login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData()));

    ProfilePayload payload = mutation.unfollow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void unfollow_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.unfollow("target"));
  }

  @Test
  void unfollow_when_target_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.unfollow("target"));
  }

  @Test
  void unfollow_when_relation_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }
}
