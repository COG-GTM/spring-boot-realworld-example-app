package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;

  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("john@example.com", "john", "123", "", "");
    target = new User("jane@example.com", "jane", "123", "jane bio", "jane.png");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  private void authenticateAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private ProfileData profileDataOf(User u, boolean following) {
    return new ProfileData(u.getId(), u.getUsername(), u.getBio(), u.getImage(), following);
  }

  @Test
  void should_follow_target_user() {
    authenticateAs(user);
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileDataOf(target, true)));

    ProfilePayload payload = relationMutation.follow(target.getUsername());

    assertThat(payload.getProfile().getUsername()).isEqualTo(target.getUsername());
    assertThat(payload.getProfile().getBio()).isEqualTo(target.getBio());
    assertThat(payload.getProfile().getImage()).isEqualTo(target.getImage());
    assertThat(payload.getProfile().getFollowing()).isTrue();

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
  }

  @Test
  void should_reject_follow_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.follow("jane"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_reject_follow_when_target_missing() {
    authenticateAs(user);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.follow("ghost"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_unfollow_target_user() {
    authenticateAs(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileDataOf(target, false)));

    ProfilePayload payload = relationMutation.unfollow(target.getUsername());

    assertThat(payload.getProfile().getUsername()).isEqualTo(target.getUsername());
    assertThat(payload.getProfile().getFollowing()).isFalse();
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void should_reject_unfollow_when_target_missing() {
    authenticateAs(user);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow("ghost"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  void should_reject_unfollow_when_relation_missing() {
    authenticateAs(user);
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> relationMutation.unfollow(target.getUsername()));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  void should_reject_unfollow_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> relationMutation.unfollow("jane"));
  }
}
