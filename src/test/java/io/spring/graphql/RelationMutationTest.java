package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {
  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User user;
  private User target;

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    target = new User("jane@doe.com", "janedoe", "123", "jane bio", "jane.png");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_follow_user_and_return_profile() {
    when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("janedoe", user))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "janedoe", "jane bio", "jane.png", true)));

    ProfilePayload payload = relationMutation.follow("janedoe");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertEquals(user.getId(), captor.getValue().getUserId());
    assertEquals(target.getId(), captor.getValue().getTargetId());
    assertEquals("janedoe", payload.getProfile().getUsername());
    assertEquals("jane bio", payload.getProfile().getBio());
    assertEquals("jane.png", payload.getProfile().getImage());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_not_found_when_following_missing_user() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("ghost"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_user_and_return_profile() {
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("janedoe", user))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "janedoe", "jane bio", "jane.png", false)));

    ProfilePayload payload = relationMutation.unfollow("janedoe");

    verify(userRepository).removeRelation(relation);
    assertFalse(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_not_found_when_unfollowing_user_not_followed() {
    when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("janedoe"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_reject_follow_and_unfollow_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("janedoe"));
    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("janedoe"));
    verifyNoInteractions(userRepository, profileQueryService);
  }
}
