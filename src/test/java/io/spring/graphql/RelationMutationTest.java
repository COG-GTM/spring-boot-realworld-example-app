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
  private User user;
  private User target;

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("a@test.com", "aisensiy", "123", "bio", "image");
    target = new User("b@test.com", "target", "123", "target bio", "target image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_follow_user() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), "target", "target bio", "target image", true)));

    ProfilePayload payload = relationMutation.follow("target");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    assertThat(payload.getProfile().getBio()).isEqualTo("target bio");
    assertThat(payload.getProfile().getFollowing()).isTrue();
  }

  @Test
  public void should_not_follow_without_login() {
    anonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.follow("target"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_throw_when_follow_target_not_found() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.follow("target"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_user() {
    login();
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), "target", "target bio", "target image", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    verify(userRepository).removeRelation(relation);
    assertThat(payload.getProfile().getUsername()).isEqualTo("target");
    assertThat(payload.getProfile().getFollowing()).isFalse();
  }

  @Test
  public void should_not_unfollow_without_login() {
    anonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_throw_when_unfollow_target_not_found() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
  }

  @Test
  public void should_throw_when_relation_not_found() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }
}
