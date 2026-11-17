package io.spring.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDataLoaderTest {
  private ProfileQueryService profileQueryService;
  private ProfileDataLoader loader;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    loader = new ProfileDataLoader(profileQueryService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_load_profiles_for_current_user() throws Exception {
    User current = new User("me@test.com", "me", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(current, null));
    Set<String> usernames = new HashSet<>(Arrays.asList("a", "b"));
    Map<String, ProfileData> data = new HashMap<>();
    data.put("a", new ProfileData("1", "a", "bio-a", "img-a", true));
    data.put("b", new ProfileData("2", "b", "bio-b", "img-b", false));
    when(profileQueryService.findByUsernames(eq(usernames), eq(current))).thenReturn(data);

    Map<String, Profile> profiles = loader.load(usernames).toCompletableFuture().get();

    Assertions.assertEquals(2, profiles.size());
    Profile a = profiles.get("a");
    Assertions.assertEquals("a", a.getUsername());
    Assertions.assertEquals("bio-a", a.getBio());
    Assertions.assertEquals("img-a", a.getImage());
    Assertions.assertTrue(a.getFollowing());
    Assertions.assertFalse(profiles.get("b").getFollowing());
  }

  @Test
  public void should_load_profiles_without_user_for_anonymous_request() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    Set<String> usernames = Collections.singleton("a");
    when(profileQueryService.findByUsernames(eq(usernames), isNull()))
        .thenReturn(Collections.singletonMap("a", new ProfileData("1", "a", "bio", "img", false)));

    Map<String, Profile> profiles = loader.load(usernames).toCompletableFuture().get();

    verify(profileQueryService).findByUsernames(eq(usernames), isNull());
    Assertions.assertFalse(profiles.get("a").getFollowing());
  }

  @Test
  public void should_omit_usernames_that_were_not_found() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    Set<String> usernames = new HashSet<>(Arrays.asList("a", "missing"));
    when(profileQueryService.findByUsernames(eq(usernames), isNull()))
        .thenReturn(Collections.singletonMap("a", new ProfileData("1", "a", "bio", "img", false)));

    Map<String, Profile> profiles = loader.load(usernames).toCompletableFuture().get();

    Assertions.assertEquals(1, profiles.size());
    Assertions.assertFalse(profiles.containsKey("missing"));
  }
}
