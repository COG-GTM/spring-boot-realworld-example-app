package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
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
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private ProfileData profileData;
  private User user;

  @BeforeEach
  public void setUp() {
    profileData = new ProfileData("id", "testuser", "test bio", "test-image.jpg", false);
    user = new User("test@example.com", "testuser", "password", "test bio", "test-image.jpg");
  }

  @Test
  public void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      graphql.schema.DataFetchingEnvironment dfe = org.mockito.Mockito.mock(graphql.schema.DataFetchingEnvironment.class);
      when(dfe.getArgument("username")).thenReturn("testuser");
      
      ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

      assertThat(result).isNotNull();
      assertThat(result.getProfile()).isNotNull();
      assertThat(result.getProfile().getUsername()).isEqualTo("testuser");
      assertThat(result.getProfile().getBio()).isEqualTo("test bio");
      assertThat(result.getProfile().getImage()).isEqualTo("test-image.jpg");
      assertThat(result.getProfile().getFollowing()).isFalse();
    }
  }

  @Test
  public void should_query_profile_with_following_status() {
    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));
      
      ProfileData followingProfileData =
          new ProfileData("id", "testuser", "test bio", "test-image.jpg", true);
      when(profileQueryService.findByUsername(eq("testuser"), any()))
          .thenReturn(Optional.of(followingProfileData));

      graphql.schema.DataFetchingEnvironment dfe = org.mockito.Mockito.mock(graphql.schema.DataFetchingEnvironment.class);
      when(dfe.getArgument("username")).thenReturn("testuser");
      
      ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

      assertThat(result).isNotNull();
      assertThat(result.getProfile()).isNotNull();
      assertThat(result.getProfile().getFollowing()).isTrue();
    }
  }

  @Test
  public void should_fail_query_profile_with_nonexistent_username() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      graphql.schema.DataFetchingEnvironment dfe = org.mockito.Mockito.mock(graphql.schema.DataFetchingEnvironment.class);
      when(dfe.getArgument("username")).thenReturn("nonexistent");
      
      assertThrows(
          ResourceNotFoundException.class,
          () -> profileDatafetcher.queryProfile("nonexistent", dfe));
    }
  }
}
