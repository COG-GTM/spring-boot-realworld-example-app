package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private User currentUser;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@example.com", "currentuser", "password", "bio", "image");
    targetUser = new User("target@example.com", "targetuser", "password", "bio", "image");
    profileData = new ProfileData("id", "targetuser", "test bio", "test-image.jpg", false);
  }

  @Test
  public void should_follow_user_successfully() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    ProfileData followingProfileData =
        new ProfileData("id", "targetuser", "test bio", "test-image.jpg", true);
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(followingProfileData));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(currentUser));

      ProfilePayload result = relationMutation.follow("targetuser");

      assertThat(result).isNotNull();
      assertThat(result.getProfile()).isNotNull();
      assertThat(result.getProfile().getUsername()).isEqualTo("targetuser");
      assertThat(result.getProfile().getFollowing()).isTrue();
      verify(userRepository).saveRelation(any(FollowRelation.class));
    }
  }

  @Test
  public void should_fail_follow_user_without_authentication() {
    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(AuthenticationException.class, () -> relationMutation.follow("targetuser"));
    }
  }

  @Test
  public void should_fail_follow_nonexistent_user() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(currentUser));

      assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("nonexistent"));
    }
  }

  @Test
  public void should_unfollow_user_successfully() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(any(), any())).thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(profileData));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(currentUser));

      ProfilePayload result = relationMutation.unfollow("targetuser");

      assertThat(result).isNotNull();
      assertThat(result.getProfile()).isNotNull();
      assertThat(result.getProfile().getUsername()).isEqualTo("targetuser");
      assertThat(result.getProfile().getFollowing()).isFalse();
      verify(userRepository).removeRelation(any(FollowRelation.class));
    }
  }

  @Test
  public void should_fail_unfollow_when_not_following() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(any(), any())).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(currentUser));

      assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("targetuser"));
    }
  }
}
