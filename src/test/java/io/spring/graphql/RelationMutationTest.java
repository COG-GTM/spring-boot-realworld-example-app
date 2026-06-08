package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

  @InjectMocks private RelationMutation relationMutation;

  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    user = new User("e@t.com", "testuser", "pass", "", "");
    target = new User("t@t.com", "targetuser", "pass", "bio", "img");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void follow_should_save_relation() {
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(target));
    ProfileData profileData = new ProfileData(target.getId(), "targetuser", "bio", "img", true);
    when(profileQueryService.findByUsername("targetuser", user))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow("targetuser");

    assertNotNull(result);
    assertNotNull(result.getProfile());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void unfollow_should_remove_relation() {
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(target));
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    ProfileData profileData = new ProfileData(target.getId(), "targetuser", "bio", "img", false);
    when(profileQueryService.findByUsername("targetuser", user))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow("targetuser");

    assertNotNull(result);
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void operations_without_authentication_should_throw() {
    SecurityContextHolder.clearContext();
    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    assertThrows(AuthenticationException.class, () -> relationMutation.follow("targetuser"));
    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("targetuser"));
  }
}
