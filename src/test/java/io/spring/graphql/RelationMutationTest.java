package io.spring.graphql;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private final User user = new User("email@test.com", "username", "123", "", "");
  private final User target = new User("target@test.com", "target", "123", "bio", "image");

  @BeforeEach
  public void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, emptyList()));
  }

  private void loginAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private void stubProfile(boolean following) {
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "target", "bio", "image", following)));
  }

  @Test
  public void should_follow_target_user() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    stubProfile(true);

    ProfilePayload payload = relationMutation.follow("target");

    assertEquals("target", payload.getProfile().getUsername());
    assertEquals("bio", payload.getProfile().getBio());
    assertTrue(payload.getProfile().getFollowing());

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertEquals(user.getId(), captor.getValue().getUserId());
    assertEquals(target.getId(), captor.getValue().getTargetId());
  }

  @Test
  public void should_not_follow_for_anonymous_user() {
    loginAnonymous();

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
  }

  @Test
  public void should_not_follow_missing_user() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("target"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_target_user() {
    login();
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    stubProfile(false);

    ProfilePayload payload = relationMutation.unfollow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).removeRelation(relation);
  }

  @Test
  public void should_not_unfollow_without_existing_relation() {
    login();
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
    verify(userRepository, never()).removeRelation(any());
  }
}
