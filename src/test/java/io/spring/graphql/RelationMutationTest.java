package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {
  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;
  private User user;
  private User target;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("a@b.com", "alice", "pw", "", "");
    target = new User("b@b.com", "bob", "pw", "bob bio", "bob.png");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void follow_should_save_relation_and_return_profile() {
    authenticate(user);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("bob", user))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "bob", "bob bio", "bob.png", true)));

    ProfilePayload payload = relationMutation.follow("bob");

    verify(userRepository).saveRelation(new FollowRelation(user.getId(), target.getId()));
    Profile profile = payload.getProfile();
    assertThat(profile.getUsername()).isEqualTo("bob");
    assertThat(profile.getBio()).isEqualTo("bob bio");
    assertThat(profile.getImage()).isEqualTo("bob.png");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  public void follow_should_throw_not_found_when_target_missing() {
    authenticate(user);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.follow("ghost"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void follow_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> relationMutation.follow("bob"))
        .isInstanceOf(AuthenticationException.class);
    verify(userRepository, never()).findByUsername(any());
  }

  @Test
  public void unfollow_should_remove_relation_and_return_profile() {
    authenticate(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("bob", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "bob", "bio", null, false)));

    ProfilePayload payload = relationMutation.unfollow("bob");

    verify(userRepository).removeRelation(relation);
    assertThat(payload.getProfile().getUsername()).isEqualTo("bob");
    assertThat(payload.getProfile().getFollowing()).isFalse();
  }

  @Test
  public void unfollow_should_throw_not_found_when_target_missing() {
    authenticate(user);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("ghost"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void unfollow_should_throw_not_found_when_not_following() {
    authenticate(user);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("bob"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void unfollow_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> relationMutation.unfollow("bob"))
        .isInstanceOf(AuthenticationException.class);
  }
}
