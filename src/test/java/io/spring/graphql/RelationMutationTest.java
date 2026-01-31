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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private User currentUser;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    currentUser = new User("current@example.com", "currentuser", "password", "bio", "image");
    targetUser = new User("target@example.com", "targetuser", "password", "bio", "image");
    profileData =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            false);
  }

  @Test
  public void should_follow_user_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    String username = targetUser.getUsername();
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(username), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow(username);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals(username, result.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_to_follow_user_without_authentication() {
    String username = targetUser.getUsername();

    assertThrows(NullPointerException.class, () -> relationMutation.follow(username));
  }

  @Test
  public void should_fail_to_follow_user_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    String username = "non-existent-user";
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow(username));
  }

  @Test
  public void should_unfollow_user_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    String username = targetUser.getUsername();
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());

    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(username), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow(username);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals(username, result.getProfile().getUsername());
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_fail_to_unfollow_user_without_authentication() {
    String username = targetUser.getUsername();

    assertThrows(NullPointerException.class, () -> relationMutation.unfollow(username));
  }

  @Test
  public void should_fail_to_unfollow_user_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    String username = "non-existent-user";
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow(username));
  }

  @Test
  public void should_fail_to_unfollow_when_relation_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    String username = targetUser.getUsername();
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow(username));
  }
}
