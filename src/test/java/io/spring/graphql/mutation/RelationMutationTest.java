package io.spring.graphql.mutation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.spring.graphql.RelationMutation;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ProfilePayload;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RelationMutationTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final ProfileQueryService profileQueryService = mock(ProfileQueryService.class);
  private final RelationMutation mutation =
      new RelationMutation(userRepository, profileQueryService);

  private final User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final User target = new User("john@john.com", "john", "123", "john bio", "john image");

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_follow_user() {
    login(user);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("john"), eq(user)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "john", "john bio", "john image", true)));

    ProfilePayload payload = mutation.follow("john");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
    assertThat(payload.getProfile().getUsername()).isEqualTo("john");
    assertThat(payload.getProfile().getBio()).isEqualTo("john bio");
    assertThat(payload.getProfile().getFollowing()).isTrue();
  }

  @Test
  void should_reject_follow_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.follow("john"));
    verify(userRepository, never()).saveRelation(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void should_throw_not_found_when_following_missing_user() {
    login(user);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.follow("ghost"));
  }

  @Test
  void should_unfollow_user() {
    login(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("john"), eq(user)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "john", "john bio", "john image", false)));

    ProfilePayload payload = mutation.unfollow("john");

    verify(userRepository).removeRelation(relation);
    assertThat(payload.getProfile().getUsername()).isEqualTo("john");
    assertThat(payload.getProfile().getFollowing()).isFalse();
  }

  @Test
  void should_throw_not_found_when_unfollowing_missing_user() {
    login(user);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.unfollow("ghost"));
  }

  @Test
  void should_throw_not_found_when_relation_absent() {
    login(user);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.unfollow("john"));
    verify(userRepository, never()).removeRelation(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void should_reject_unfollow_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.unfollow("john"));
  }
}
