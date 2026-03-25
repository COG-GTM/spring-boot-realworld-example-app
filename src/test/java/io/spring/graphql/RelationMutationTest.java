package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ProfilePayload;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationTest {

  private UserRepository userRepository;
  private ProfileQueryService profileQueryService;
  private RelationMutation relationMutation;
  private User user;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    profileQueryService = mock(ProfileQueryService.class);
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("a@b.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_follow_user() {
    User target = new User("c@d.com", "target", "pass", "", "");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    ProfileData profileData = new ProfileData(target.getId(), "target", "", "", true);
    when(profileQueryService.findByUsername("target", user)).thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow("target");
    assertNotNull(result);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_throw_when_follow_user_not_found() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("unknown"));
  }

  @Test
  public void should_unfollow_user() {
    User target = new User("c@d.com", "target", "pass", "", "");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.of(relation));
    ProfileData profileData = new ProfileData(target.getId(), "target", "", "", false);
    when(profileQueryService.findByUsername("target", user)).thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow("target");
    assertNotNull(result);
    verify(userRepository).removeRelation(relation);
  }

  @Test
  public void should_throw_when_unfollow_user_not_found() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("unknown"));
  }

  @Test
  public void should_throw_when_unfollow_no_relation() {
    User target = new User("c@d.com", "target", "pass", "", "");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }
}
