package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher profileDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image.jpg");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_get_user_profile() {
    ProfileData profileData =
        new ProfileData(user.getId(), "testuser", "bio", "image.jpg", false);
    when(profileQueryService.findByUsername("testuser", user))
        .thenReturn(Optional.of(profileData));

    graphql.schema.DataFetchingEnvironment dfe =
        mock(graphql.schema.DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);

    Profile result = profileDatafetcher.getUserProfile(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("bio", result.getBio());
  }
}
