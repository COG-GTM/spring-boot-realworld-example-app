package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private User testUser;
  private ProfileData profileData;
  private ProfileData followingProfileData;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(testUser.getId(), "testuser", "bio", "image", false);
    followingProfileData = new ProfileData(testUser.getId(), "testuser", "bio", "image", true);
  }

  @Test
  public void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("testuser");

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

      assertNotNull(result);
      assertNotNull(result.getProfile());
      assertEquals("testuser", result.getProfile().getUsername());
      assertEquals("bio", result.getProfile().getBio());
      assertEquals("image", result.getProfile().getImage());
      assertFalse(result.getProfile().getFollowing());
    }
  }

  @Test
  public void should_query_profile_with_following_status() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(followingProfileData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("testuser");

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

      assertNotNull(result);
      assertNotNull(result.getProfile());
      assertEquals("testuser", result.getProfile().getUsername());
      assertTrue(result.getProfile().getFollowing());
    }
  }
}
