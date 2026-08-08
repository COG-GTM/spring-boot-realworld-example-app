package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User current;
  private User target;

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    current = new User("c@b.com", "current", "123", "", "");
    target = new User("t@b.com", "target", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));
  }

  @Test
  public void should_throw_authentication_exception_when_following_anonymously() {
    assertThatThrownBy(() -> relationMutation.follow("target"))
        .isInstanceOf(AuthenticationException.class);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_throw_not_found_when_follow_target_missing() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.follow("target"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_save_relation_and_return_profile_on_follow() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("target", current))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", true)));

    ProfilePayload payload = relationMutation.follow("target");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(current.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    assertThat(payload.getProfile().getFollowing()).isTrue();
    assertThat(payload.getProfile().getBio()).isEqualTo("bio");
  }

  @Test
  public void should_throw_authentication_exception_when_unfollowing_anonymously() {
    assertThatThrownBy(() -> relationMutation.unfollow("target"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void should_throw_not_found_when_unfollow_target_missing() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("target"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_throw_not_found_when_relation_missing_on_unfollow() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(current.getId(), target.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("target"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_remove_relation_and_return_profile_on_unfollow() {
    login();
    FollowRelation relation = new FollowRelation(current.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(current.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("target", current))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    verify(userRepository).removeRelation(relation);
    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    assertThat(payload.getProfile().getFollowing()).isFalse();
  }
}
