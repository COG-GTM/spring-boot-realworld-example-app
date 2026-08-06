package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;

  private final User currentUser = new User("me@example.com", "me", "123", "", "");
  private final User target = new User("target@example.com", "target", "123", "", "");

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "target", "bio", "image", true)));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_follow_user_and_save_relation() {
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));

    ProfilePayload payload = relationMutation.follow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_throw_when_follow_without_authentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
  }

  @Test
  void should_throw_when_follow_target_not_found() {
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("target"));
  }

  @Test
  void should_unfollow_user_and_remove_relation() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.of(relation));

    ProfilePayload payload = relationMutation.unfollow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void should_throw_when_unfollow_target_not_found() {
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }

  @Test
  void should_throw_when_unfollow_relation_not_found() {
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(currentUser.getId(), target.getId()))
        .thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }
}
