package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationMutationTest extends GraphqlTestBase {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    target = new User("target@test.com", "target", "123", "bio", "image");
  }

  @Test
  void should_follow_user() {
    login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", true)));

    ProfilePayload payload = relationMutation.follow("target");

    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    assertThat(payload.getProfile().getFollowing()).isTrue();
    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
  }

  @Test
  void should_not_follow_missing_user() {
    login(user);
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.follow("unknown"));
    verify(userRepository, never()).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_not_follow_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.follow("target"));
  }

  @Test
  void should_unfollow_user() {
    login(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    assertThat(payload.getProfile().getFollowing()).isFalse();
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void should_not_unfollow_missing_user() {
    login(user);
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("unknown"));
  }

  @Test
  void should_not_unfollow_user_without_relation() {
    login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any(FollowRelation.class));
  }

  @Test
  void should_not_unfollow_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
  }
}
