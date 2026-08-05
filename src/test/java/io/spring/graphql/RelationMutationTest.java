package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;
  private User currentUser;
  private User target;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    currentUser = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    target = new User("target@jacob.com", "target", "123", "target bio", "target image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_save_follow_relation_and_return_following_profile() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("target"), eq(currentUser)))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), "target", "target bio", "target image", true)));

    ProfilePayload payload = relationMutation.follow("target");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertEquals(currentUser.getId(), captor.getValue().getUserId());
    assertEquals(target.getId(), captor.getValue().getTargetId());
    assertEquals("target", payload.getProfile().getUsername());
    assertEquals("target bio", payload.getProfile().getBio());
    assertEquals("target image", payload.getProfile().getImage());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_not_allow_anonymous_user_to_follow() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_throw_not_found_when_follow_target_does_not_exist() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("ghost"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_remove_follow_relation_and_return_unfollowed_profile() {
    authenticate(currentUser);
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("target"), eq(currentUser)))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), "target", "target bio", "target image", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    verify(userRepository).removeRelation(eq(relation));
    assertEquals("target", payload.getProfile().getUsername());
    assertEquals(false, payload.getProfile().getFollowing());
  }

  @Test
  public void should_not_allow_anonymous_user_to_unfollow() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_throw_not_found_when_unfollow_target_does_not_exist() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("ghost"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_throw_not_found_when_follow_relation_does_not_exist() {
    authenticate(currentUser);
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }
}
