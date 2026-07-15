package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;

  private User currentUser;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    currentUser = new User("user@example.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private void stubProfile(User target) {
    ProfileData profileData =
        new ProfileData(target.getId(), target.getUsername(), "bio", "image", true);
    when(profileQueryService.findByUsername(eq(target.getUsername()), any(User.class)))
        .thenReturn(Optional.of(profileData));
  }

  @Test
  void follow_targetExists_savesRelationAndReturnsProfile() {
    authenticate(currentUser);
    User target = new User("target@example.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    stubProfile(target);

    ProfilePayload payload = relationMutation.follow("target");

    verify(userRepository).saveRelation(any(FollowRelation.class));
    assertNotNull(payload.getProfile());
    assertEquals("target", payload.getProfile().getUsername());
  }

  @Test
  void follow_targetNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("target"));
  }

  @Test
  void follow_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
  }

  @Test
  void unfollow_existingRelation_removesRelationAndReturnsProfile() {
    authenticate(currentUser);
    User target = new User("target@example.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    stubProfile(target);

    ProfilePayload payload = relationMutation.unfollow("target");

    verify(userRepository).removeRelation(relation);
    assertNotNull(payload.getProfile());
    assertEquals("target", payload.getProfile().getUsername());
  }

  @Test
  void unfollow_targetNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any(FollowRelation.class));
  }

  @Test
  void unfollow_noRelation_throwsResourceNotFound() {
    authenticate(currentUser);
    User target = new User("target@example.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any(FollowRelation.class));
  }

  @Test
  void unfollow_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("target"));
  }
}
