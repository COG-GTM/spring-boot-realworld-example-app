package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;
  @InjectMocks private RelationMutation mutation;

  private User currentUser;
  private User target;

  @BeforeEach
  public void setUp() {
    currentUser = new User("a@b.com", "alice", "secret", "", "");
    target = new User("b@b.com", "bob", "secret", "bob bio", "img");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_follow_user() {
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    ProfileData profileData = new ProfileData(target.getId(), "bob", "bob bio", "img", true);
    when(profileQueryService.findByUsername(eq("bob"), any())).thenReturn(Optional.of(profileData));

    ProfilePayload payload = mutation.follow("bob");

    assertNotNull(payload);
    assertEquals("bob", payload.getProfile().getUsername());
    verify(userRepository, times(1)).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_throw_resource_not_found_when_following_unknown_user() {
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.follow("missing"));
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_throw_authentication_when_unauthenticated() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "k", "anon", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));

    assertThrows(AuthenticationException.class, () -> mutation.follow("bob"));
  }

  @Test
  public void should_unfollow_user() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    ProfileData profileData = new ProfileData(target.getId(), "bob", "bob bio", "img", false);
    when(profileQueryService.findByUsername(eq("bob"), any())).thenReturn(Optional.of(profileData));

    ProfilePayload payload = mutation.unfollow("bob");

    assertNotNull(payload);
    verify(userRepository, times(1)).removeRelation(relation);
  }

  @Test
  public void should_throw_resource_not_found_when_unfollowing_user_not_followed() {
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.unfollow("bob"));
    verify(userRepository, never()).removeRelation(any());
  }
}
