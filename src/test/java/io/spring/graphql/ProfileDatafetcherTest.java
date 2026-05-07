package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher profileDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_user_profile() {
    ProfileData profileData =
        new ProfileData(user.getId(), "testuser", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);

    Profile profile = profileDatafetcher.getUserProfile(dfe);
    assertNotNull(profile);
    assertEquals("testuser", profile.getUsername());
  }

  @Test
  public void should_get_article_author_profile() {
    ProfileData authorProfile =
        new ProfileData("author-id", "author", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("author"), any()))
        .thenReturn(Optional.of(authorProfile));

    ArticleData articleData = mock(ArticleData.class);
    when(articleData.getProfileData()).thenReturn(authorProfile);

    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    Article article = Article.newBuilder().slug("test-slug").build();

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(map);
    when(dfe.getSource()).thenReturn(article);

    Profile profile = profileDatafetcher.getAuthor(dfe);
    assertNotNull(profile);
    assertEquals("author", profile.getUsername());
  }

  @Test
  public void should_throw_when_profile_not_found() {
    when(profileQueryService.findByUsername(eq("noone"), any())).thenReturn(Optional.empty());

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    User noUser = new User("noone@test.com", "noone", "pass", "", "");
    when(dfe.getLocalContext()).thenReturn(noUser);

    assertThrows(ResourceNotFoundException.class, () -> profileDatafetcher.getUserProfile(dfe));
  }

  @Test
  public void should_query_profile_by_username() {
    ProfileData profileData =
        new ProfileData(user.getId(), "testuser", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("testuser");

    ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);
    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("testuser", result.getProfile().getUsername());
  }
}
