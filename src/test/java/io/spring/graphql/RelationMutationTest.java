package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
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
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {
  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User user;
  private User target;

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("a@test.com", "a", "123", "", "");
    target = new User("b@test.com", "b", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_follow_user() {
    GraphQLTestSupport.authenticate(user);
    when(userRepository.findByUsername("b")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("b", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "b", "bio", "image", true)));

    ProfilePayload payload = relationMutation.follow("b");

    verify(userRepository).saveRelation(new FollowRelation(user.getId(), target.getId()));
    Profile profile = payload.getProfile();
    Assertions.assertEquals("b", profile.getUsername());
    Assertions.assertEquals("bio", profile.getBio());
    Assertions.assertEquals("image", profile.getImage());
    Assertions.assertTrue(profile.getFollowing());
  }

  @Test
  public void should_fail_to_follow_unknown_user() {
    GraphQLTestSupport.authenticate(user);
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.follow("unknown"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_reject_anonymous_follow() {
    GraphQLTestSupport.anonymous();

    Assertions.assertThrows(AuthenticationException.class, () -> relationMutation.follow("b"));
  }

  @Test
  public void should_unfollow_user() {
    GraphQLTestSupport.authenticate(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("b")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("b", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "b", "bio", "image", false)));

    ProfilePayload payload = relationMutation.unfollow("b");

    verify(userRepository).removeRelation(relation);
    Assertions.assertFalse(payload.getProfile().getFollowing());
  }

  @Test
  public void should_fail_to_unfollow_when_not_following() {
    GraphQLTestSupport.authenticate(user);
    when(userRepository.findByUsername("b")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    Assertions.assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("b"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_fail_to_unfollow_unknown_user() {
    GraphQLTestSupport.authenticate(user);
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow("unknown"));
  }
}
