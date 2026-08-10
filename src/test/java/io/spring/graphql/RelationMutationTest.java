package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  @InjectMocks private RelationMutation relationMutation;

  private User user;
  private User target;

  @BeforeEach
  public void setUp() {
    user = new User("email@example.com", "username", "123", "", "");
    target = new User("target@example.com", "target", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void loginAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_follow_user() {
    login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", true)));

    ProfilePayload payload = relationMutation.follow("target");

    verify(userRepository).saveRelation(any(FollowRelation.class));
    Assertions.assertEquals("target", payload.getProfile().getUsername());
    Assertions.assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_authentication_exception_when_not_logged_in() {
    loginAnonymously();
    Assertions.assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
    Assertions.assertThrows(
        AuthenticationException.class, () -> relationMutation.unfollow("target"));
  }

  @Test
  public void should_throw_not_found_when_follow_target_missing() {
    login(user);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.follow("missing"));
  }

  @Test
  public void should_unfollow_user() {
    login(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "target", "bio", "image", false)));

    ProfilePayload payload = relationMutation.unfollow("target");

    verify(userRepository).removeRelation(relation);
    Assertions.assertFalse(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_not_found_when_relation_missing() {
    login(user);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }
}
