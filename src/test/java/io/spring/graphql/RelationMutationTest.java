package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  private RelationMutation relationMutation;
  private User user;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    relationMutation = new RelationMutation(userRepository, profileQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    targetUser = new User("target@test.com", "targetuser", "password", "target bio", "target image");
    profileData =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            true);
  }

  @Test
  void follow_withValidUsername_returnsProfilePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      when(profileQueryService.findByUsername(targetUser.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      ProfilePayload result = relationMutation.follow(targetUser.getUsername());

      assertNotNull(result);
      assertNotNull(result.getProfile());
      assertEquals(targetUser.getUsername(), result.getProfile().getUsername());
      assertTrue(result.getProfile().getFollowing());
      verify(userRepository).saveRelation(any(FollowRelation.class));
    }
  }

  @Test
  void follow_userNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> relationMutation.follow("nonexistent"));
    }
  }

  @Test
  void follow_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class, () -> relationMutation.follow(targetUser.getUsername()));
    }
  }

  @Test
  void unfollow_withValidUsername_returnsProfilePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
      when(userRepository.findRelation(user.getId(), targetUser.getId()))
          .thenReturn(Optional.of(followRelation));

      ProfileData unfollowedProfile =
          new ProfileData(
              targetUser.getId(),
              targetUser.getUsername(),
              targetUser.getBio(),
              targetUser.getImage(),
              false);
      when(profileQueryService.findByUsername(targetUser.getUsername(), user))
          .thenReturn(Optional.of(unfollowedProfile));

      ProfilePayload result = relationMutation.unfollow(targetUser.getUsername());

      assertNotNull(result);
      assertNotNull(result.getProfile());
      assertEquals(targetUser.getUsername(), result.getProfile().getUsername());
      verify(userRepository).removeRelation(followRelation);
    }
  }

  @Test
  void unfollow_userNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> relationMutation.unfollow("nonexistent"));
    }
  }

  @Test
  void unfollow_relationNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      when(userRepository.findRelation(user.getId(), targetUser.getId()))
          .thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> relationMutation.unfollow(targetUser.getUsername()));
    }
  }

  @Test
  void unfollow_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> relationMutation.unfollow(targetUser.getUsername()));
    }
  }

  @Test
  void follow_createsCorrectFollowRelation() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      when(profileQueryService.findByUsername(targetUser.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      relationMutation.follow(targetUser.getUsername());

      verify(userRepository)
          .saveRelation(
              argThat(
                  relation ->
                      relation.getUserId().equals(user.getId())
                          && relation.getTargetId().equals(targetUser.getId())));
    }
  }

  @Test
  void follow_returnsProfileWithCorrectFields() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      when(profileQueryService.findByUsername(targetUser.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      ProfilePayload result = relationMutation.follow(targetUser.getUsername());

      assertNotNull(result.getProfile());
      assertEquals(profileData.getUsername(), result.getProfile().getUsername());
      assertEquals(profileData.getBio(), result.getProfile().getBio());
      assertEquals(profileData.getImage(), result.getProfile().getImage());
      assertEquals(profileData.isFollowing(), result.getProfile().getFollowing());
    }
  }

  @Test
  void unfollow_removesCorrectRelation() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(userRepository.findByUsername(targetUser.getUsername()))
          .thenReturn(Optional.of(targetUser));
      FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
      when(userRepository.findRelation(user.getId(), targetUser.getId()))
          .thenReturn(Optional.of(followRelation));

      ProfileData unfollowedProfile =
          new ProfileData(
              targetUser.getId(),
              targetUser.getUsername(),
              targetUser.getBio(),
              targetUser.getImage(),
              false);
      when(profileQueryService.findByUsername(targetUser.getUsername(), user))
          .thenReturn(Optional.of(unfollowedProfile));

      relationMutation.unfollow(targetUser.getUsername());

      verify(userRepository).removeRelation(followRelation);
    }
  }
}
