package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;

  private User user;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    targetUser = new User("target@test.com", "targetuser", "password", "target bio", "target image");
    profileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), true);
    
    setAnonymousAuthentication();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", 
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  void shouldFollowUserWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow(targetUser.getUsername());
    
    assertThat(result).isNotNull();
    assertThat(result.getProfile().getUsername()).isEqualTo(targetUser.getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void shouldFailToFollowUserWhenNotAuthenticated() {
    assertThatThrownBy(() -> relationMutation.follow("targetuser"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFailToFollowUserWhenUserNotFound() {
    setAuthenticatedUser(user);
    
    when(userRepository.findByUsername(eq("non-existent")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.follow("non-existent"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldUnfollowUserWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
    ProfileData unfollowedProfile = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
    
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq(targetUser.getUsername()), eq(user)))
        .thenReturn(Optional.of(unfollowedProfile));

    ProfilePayload result = relationMutation.unfollow(targetUser.getUsername());
    
    assertThat(result).isNotNull();
    assertThat(result.getProfile().getUsername()).isEqualTo(targetUser.getUsername());
    assertThat(result.getProfile().getFollowing()).isFalse();
    verify(userRepository).removeRelation(eq(followRelation));
  }

  @Test
  void shouldFailToUnfollowUserWhenNotAuthenticated() {
    assertThatThrownBy(() -> relationMutation.unfollow("targetuser"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFailToUnfollowUserWhenUserNotFound() {
    setAuthenticatedUser(user);
    
    when(userRepository.findByUsername(eq("non-existent")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> relationMutation.unfollow("non-existent"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
