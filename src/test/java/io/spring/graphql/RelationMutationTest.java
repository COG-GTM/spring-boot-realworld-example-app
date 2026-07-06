package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;

  private User currentUser;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    profileQueryService = Mockito.mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);

    currentUser = new User("current@example.com", "current", "pass", "", "");
    targetUser = new User("target@example.com", "target", "pass", "", "");
    profileData =
        new ProfileData(
            targetUser.getId(), targetUser.getUsername(), "target bio", "target-image.png", true);
  }

  @AfterEach
  public void tearDown() {
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
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_follow_user_success() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload payload = relationMutation.follow(targetUser.getUsername());

    assertEquals(profileData.getUsername(), payload.getProfile().getUsername());
    assertEquals(profileData.getBio(), payload.getProfile().getBio());
    assertEquals(profileData.getImage(), payload.getProfile().getImage());
    assertEquals(profileData.isFollowing(), payload.getProfile().getFollowing());
    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertEquals(currentUser.getId(), captor.getValue().getUserId());
    assertEquals(targetUser.getId(), captor.getValue().getTargetId());
  }

  @Test
  public void should_throw_resource_not_found_when_follow_target_missing() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq(targetUser.getUsername()))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.follow(targetUser.getUsername()));
  }

  @Test
  public void should_throw_authentication_exception_when_follow_unauthenticated() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> relationMutation.follow(targetUser.getUsername()));
  }

  @Test
  public void should_unfollow_user_success() {
    authenticate(currentUser);
    FollowRelation followRelation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(followRelation));
    ProfileData unfollowedProfile =
        new ProfileData(
            targetUser.getId(), targetUser.getUsername(), "target bio", "target-image.png", false);
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(currentUser)))
        .thenReturn(Optional.of(unfollowedProfile));

    ProfilePayload payload = relationMutation.unfollow(targetUser.getUsername());

    assertEquals(unfollowedProfile.getUsername(), payload.getProfile().getUsername());
    assertEquals(unfollowedProfile.getBio(), payload.getProfile().getBio());
    assertEquals(unfollowedProfile.getImage(), payload.getProfile().getImage());
    assertEquals(false, payload.getProfile().getFollowing());
    verify(userRepository).removeRelation(eq(followRelation));
  }

  @Test
  public void should_throw_resource_not_found_when_unfollow_target_missing() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq(targetUser.getUsername()))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow(targetUser.getUsername()));
  }

  @Test
  public void should_throw_resource_not_found_when_unfollow_relation_missing() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow(targetUser.getUsername()));
  }

  @Test
  public void should_throw_authentication_exception_when_unfollow_unauthenticated() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> relationMutation.unfollow(targetUser.getUsername()));
  }
}
