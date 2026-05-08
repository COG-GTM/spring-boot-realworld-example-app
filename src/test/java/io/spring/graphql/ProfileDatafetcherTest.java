package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dfe;
  @InjectMocks private ProfileDatafetcher datafetcher;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "k", "anon", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_query_profile_by_username() {
    User currentUser = new User("a@b.com", "alice", "secret", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
    ProfileData profileData = new ProfileData("uid", "bob", "bio", "image", true);
    when(profileQueryService.findByUsername(eq("bob"), nullable(User.class)))
        .thenReturn(Optional.of(profileData));
    when(dfe.getArgument("username")).thenReturn("bob");

    ProfilePayload payload = datafetcher.queryProfile("bob", dfe);

    assertNotNull(payload);
    Profile profile = payload.getProfile();
    assertEquals("bob", profile.getUsername());
    assertEquals("bio", profile.getBio());
    assertEquals("image", profile.getImage());
    assertTrue(profile.getFollowing());
  }

  @Test
  public void should_throw_when_profile_not_found() {
    when(profileQueryService.findByUsername(eq("missing"), any())).thenReturn(Optional.empty());
    when(dfe.getArgument("username")).thenReturn("missing");

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.queryProfile("missing", dfe));
  }

  @Test
  public void should_get_user_profile_from_local_context() {
    User user = new User("a@b.com", "alice", "secret", "", "");
    when(dfe.getLocalContext()).thenReturn(user);
    ProfileData profileData = new ProfileData("uid", "alice", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("alice"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = datafetcher.getUserProfile(dfe);

    assertNotNull(result);
    assertEquals("alice", result.getUsername());
  }
}
