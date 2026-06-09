package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User currentUser;
  private User targetUser;

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    currentUser = new User("me@test.com", "currentuser", "pass", "", "");
    targetUser = new User("target@test.com", "targetuser", "pass", "", "");
  }

  @AfterEach
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticated(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  @Test
  public void should_follow_user() {
    setAuthenticated(currentUser);
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
    ProfileData profileData =
        new ProfileData(targetUser.getId(), "targetuser", "", "", true);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow("targetuser");
    assertThat(result, notNullValue());
    assertThat(result.getProfile().getUsername(), is("targetuser"));
    assertThat(result.getProfile().getFollowing(), is(true));
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_follow_when_not_authenticated() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(null);
    assertThrows(NullPointerException.class, () -> relationMutation.follow("targetuser"));
  }

  @Test
  public void should_unfollow_user() {
    setAuthenticated(currentUser);
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findRelation(currentUser.getId(), targetUser.getId()))
        .thenReturn(Optional.of(relation));
    ProfileData profileData =
        new ProfileData(targetUser.getId(), "targetuser", "", "", false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow("targetuser");
    assertThat(result, notNullValue());
    assertThat(result.getProfile().getFollowing(), is(false));
    verify(userRepository).removeRelation(relation);
  }

  @Test
  public void should_fail_unfollow_when_not_following() {
    setAuthenticated(currentUser);
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(currentUser.getId(), targetUser.getId()))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow("targetuser"));
  }
}
