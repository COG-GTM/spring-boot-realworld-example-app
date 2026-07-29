package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  @Captor private ArgumentCaptor<FollowRelation> followRelationCaptor;

  @InjectMocks private RelationMutation relationMutation;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final User target =
      new User("john@john.com", "john", "123", "john's bio", "john's image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_follow_target_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("john", currentUser))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "john", "bio", "image", true)));

    ProfilePayload payload = relationMutation.follow("john");

    verify(userRepository).saveRelation(followRelationCaptor.capture());
    assertEquals(currentUser.getId(), followRelationCaptor.getValue().getUserId());
    assertEquals(target.getId(), followRelationCaptor.getValue().getTargetId());
    assertEquals("john", payload.getProfile().getUsername());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_when_following_unknown_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("missing"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_reject_follow_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("john"));
  }

  @Test
  public void should_unfollow_target_user() {
    SecurityContextHelper.authenticate(currentUser);
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("john", currentUser))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "john", "bio", "image", false)));

    ProfilePayload payload = relationMutation.unfollow("john");

    verify(userRepository).removeRelation(relation);
    assertEquals("john", payload.getProfile().getUsername());
  }

  @Test
  public void should_throw_when_unfollowing_unknown_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("missing"));
  }

  @Test
  public void should_throw_when_relation_does_not_exist() {
    SecurityContextHelper.authenticate(currentUser);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("john"));
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_reject_unfollow_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("john"));
  }
}
