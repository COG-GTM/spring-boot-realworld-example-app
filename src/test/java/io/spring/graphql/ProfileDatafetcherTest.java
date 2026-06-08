package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.schema.DataFetchingEnvironment;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  @Mock private DataFetchingEnvironment dfe;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("e@t.com", "testuser", "pass", "bio", "img");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getUserProfile_should_return_profile() {
    when(dfe.getLocalContext()).thenReturn(user);
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "img", false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("bio", result.getBio());
  }

  @Test
  void queryProfile_should_return_profile_payload() {
    when(dfe.getArgument("username")).thenReturn("testuser");
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "img", false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("testuser", result.getProfile().getUsername());
  }
}
