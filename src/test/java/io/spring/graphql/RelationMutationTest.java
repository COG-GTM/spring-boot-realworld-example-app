package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private User user;
  private User targetUser;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    targetUser = new User("target@user.com", "targetuser", "456", "target bio", "target image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private void logIn(User loginUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(loginUser, null));
  }

  // ── follow ──

  @Test
  void follow_success() {
    logIn(user);
    ProfileData profileData =
        new ProfileData(
            targetUser.getId(), targetUser.getUsername(), targetUser.getBio(),
            targetUser.getImage(), true);

    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow(targetUser.getUsername());

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals(targetUser.getUsername(), result.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void follow_unauthenticated() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> relationMutation.follow("targetuser"));
  }

  @Test
  void follow_userNotFound() {
    logIn(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("nonexistent"));
  }

  // ── unfollow ──

  @Test
  void unfollow_success() {
    logIn(user);
    FollowRelation relation = new FollowRelation(user.getId(), targetUser.getId());
    ProfileData profileData =
        new ProfileData(
            targetUser.getId(), targetUser.getUsername(), targetUser.getBio(),
            targetUser.getImage(), false);

    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow(targetUser.getUsername());

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals(targetUser.getUsername(), result.getProfile().getUsername());
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  void unfollow_unauthenticated() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("targetuser"));
  }

  @Test
  void unfollow_userNotFound() {
    logIn(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow("nonexistent"));
  }

  @Test
  void unfollow_relationNotFound() {
    logIn(user);
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> relationMutation.unfollow(targetUser.getUsername()));
  }
}
