package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = newUser();
    target = new User("target@test.com", "target", "123", "", "");
  }

  @Test
  void should_follow_user() {
    setCurrentUser(user);
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target")));

    ProfilePayload payload = relationMutation.follow("target");

    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_throw_when_follow_unauthenticated() {
    setAnonymous();
    assertThatThrownBy(() -> relationMutation.follow("target"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void should_throw_when_follow_target_missing() {
    setCurrentUser(user);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.follow("ghost"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_unfollow_user() {
    setCurrentUser(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target")));

    ProfilePayload payload = relationMutation.unfollow("target");

    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void should_throw_when_unfollow_unauthenticated() {
    setAnonymous();
    assertThatThrownBy(() -> relationMutation.unfollow("target"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void should_throw_when_unfollow_target_missing() {
    setCurrentUser(user);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("ghost"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_throw_when_unfollow_relation_missing() {
    setCurrentUser(user);
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("target"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).removeRelation(any());
  }
}
