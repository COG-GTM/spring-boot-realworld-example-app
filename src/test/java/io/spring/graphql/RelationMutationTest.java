package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RelationMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User currentUser;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    currentUser = new User("me@test.com", "me", "pass", "", "");
  }

  private ProfileData profileData(String username, boolean following) {
    return new ProfileData("id-" + username, username, "bio", "image", following);
  }

  @Test
  public void should_follow_target_user() {
    setCurrentUser(currentUser);
    User target = new User("target@test.com", "target", "pass", "", "");
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target", true)));

    ProfilePayload payload = relationMutation.follow("target");

    assertThat(payload.getProfile().getUsername(), is("target"));
    assertThat(payload.getProfile().getFollowing(), is(true));
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_throw_when_follow_anonymous() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
  }

  @Test
  public void should_throw_when_follow_target_missing() {
    setCurrentUser(currentUser);
    when(userRepository.findByUsername(eq("missing"))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("missing"));
  }

  @Test
  public void should_unfollow_target_user() {
    setCurrentUser(currentUser);
    User target = new User("target@test.com", "target", "pass", "", "");
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    assertThat(payload.getProfile().getUsername(), is("target"));
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_throw_when_unfollow_target_missing() {
    setCurrentUser(currentUser);
    when(userRepository.findByUsername(eq("missing"))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("missing"));
  }

  @Test
  public void should_throw_when_unfollow_relation_missing() {
    setCurrentUser(currentUser);
    User target = new User("target@test.com", "target", "pass", "", "");
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }

  @Test
  public void should_throw_when_unfollow_anonymous() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("target"));
  }
}
