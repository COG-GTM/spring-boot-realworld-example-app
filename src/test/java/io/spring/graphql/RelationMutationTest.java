package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;
  private User current;
  private User target;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    current = new User("alice@example.com", "alice", "pw", "", "");
    target = new User("bob@example.com", "bob", "pw", "bob bio", "bob.png");
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

  // ---- followUser ----

  @Test
  public void follow_should_save_relation_and_return_followed_profile() {
    authenticate(current);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("bob", current))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "bob", "bob bio", "bob.png", true)));

    ProfilePayload payload = relationMutation.follow("bob");

    verify(userRepository).saveRelation(new FollowRelation(current.getId(), target.getId()));
    assertEquals("bob", payload.getProfile().getUsername());
    assertEquals("bob bio", payload.getProfile().getBio());
    assertEquals("bob.png", payload.getProfile().getImage());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void follow_should_throw_not_found_for_unknown_user() {
    authenticate(current);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("ghost"));
    verify(userRepository, never()).saveRelation(any());
    verifyNoInteractions(profileQueryService);
  }

  @Test
  public void follow_should_require_authentication() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("bob"));
    verifyNoInteractions(userRepository, profileQueryService);
  }

  // ---- unfollowUser ----

  @Test
  public void unfollow_should_remove_relation_and_return_profile() {
    authenticate(current);
    FollowRelation relation = new FollowRelation(current.getId(), target.getId());
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(current.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("bob", current))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "bob", "bob bio", "bob.png", false)));

    ProfilePayload payload = relationMutation.unfollow("bob");

    verify(userRepository).removeRelation(relation);
    assertEquals("bob", payload.getProfile().getUsername());
    assertFalse(payload.getProfile().getFollowing());
  }

  @Test
  public void unfollow_should_throw_not_found_for_unknown_user() {
    authenticate(current);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("ghost"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void unfollow_should_throw_not_found_when_not_following() {
    authenticate(current);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(current.getId(), target.getId())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("bob"));
    verify(userRepository, never()).removeRelation(any());
    verifyNoInteractions(profileQueryService);
  }

  @Test
  public void unfollow_should_require_authentication() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("bob"));
    verifyNoInteractions(userRepository, profileQueryService);
  }
}
