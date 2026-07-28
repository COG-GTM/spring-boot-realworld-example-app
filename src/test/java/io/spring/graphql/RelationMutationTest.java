package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private final User target = new User("target@test.com", "target", "123", "bio", "image");

  private ProfileData profileData(boolean following) {
    return new ProfileData(
        target.getId(), target.getUsername(), target.getBio(), target.getImage(), following);
  }

  @Test
  void should_follow_user() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData(true)));

    ProfilePayload payload = relationMutation.follow(target.getUsername());

    assertThat(payload.getProfile().getUsername()).isEqualTo(target.getUsername());
    assertThat(payload.getProfile().getFollowing()).isTrue();
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_fail_follow_unknown_user() {
    when(userRepository.findByUsername(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.follow("unknown"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_fail_follow_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.follow("target"));
  }

  @Test
  void should_unfollow_user() {
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData(false)));

    ProfilePayload payload = relationMutation.unfollow(target.getUsername());

    assertThat(payload.getProfile().getFollowing()).isFalse();
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  void should_fail_unfollow_when_relation_absent() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow(target.getUsername()));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  void should_fail_unfollow_unknown_user() {
    when(userRepository.findByUsername(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("unknown"));
  }

  @Test
  void should_fail_unfollow_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
  }
}
