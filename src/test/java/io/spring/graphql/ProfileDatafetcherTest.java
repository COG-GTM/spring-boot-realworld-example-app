package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("user@example.com", "user", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    return user;
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ProfileData profileData(String username) {
    return new ProfileData("pid", username, "bio", "image", true);
  }

  private ArticleData articleData(String slug, String authorUsername) {
    return new ArticleData(
        "article-id",
        slug,
        "title",
        "description",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("java"),
        profileData(authorUsername));
  }

  private CommentData commentData(String id, String authorUsername) {
    return new CommentData(
        id, "body", "article-id", new DateTime(), new DateTime(), profileData(authorUsername));
  }

  @Test
  void getUserProfile_returns_profile() {
    User current = authenticate();
    User localUser = new User("p@example.com", "profileuser", "pw", "", "");
    when(profileQueryService.findByUsername("profileuser", current))
        .thenReturn(Optional.of(profileData("profileuser")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(localUser);

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertEquals("profileuser", profile.getUsername());
    assertTrue(profile.getFollowing());
  }

  @Test
  void getUserProfile_throws_not_found() {
    User current = authenticate();
    User localUser = new User("p@example.com", "ghost", "pw", "", "");
    when(profileQueryService.findByUsername("ghost", current)).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(localUser);

    assertThrows(ResourceNotFoundException.class, () -> profileDatafetcher.getUserProfile(dfe));
  }

  @Test
  void getAuthor_resolves_author_from_article_local_context() {
    User current = authenticate();
    ArticleData articleData = articleData("slug-1", "author-1");
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("slug-1", articleData);
    Article article = Article.newBuilder().slug("slug-1").build();
    when(profileQueryService.findByUsername("author-1", current))
        .thenReturn(Optional.of(profileData("author-1")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(dfe.<Article>getSource()).thenReturn(article);

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertEquals("author-1", profile.getUsername());
  }

  @Test
  void getCommentAuthor_resolves_author_from_comment_local_context() {
    User current = authenticate();
    CommentData commentData = commentData("comment-1", "author-2");
    Map<String, CommentData> localContext = new HashMap<>();
    localContext.put("comment-1", commentData);
    Comment comment = Comment.newBuilder().id("comment-1").build();
    when(profileQueryService.findByUsername("author-2", current))
        .thenReturn(Optional.of(profileData("author-2")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, CommentData>>getLocalContext()).thenReturn(localContext);
    when(dfe.<Comment>getSource()).thenReturn(comment);

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertEquals("author-2", profile.getUsername());
  }

  @Test
  void queryProfile_returns_payload_using_username_argument() {
    User current = authenticate();
    when(profileQueryService.findByUsername("queried", current))
        .thenReturn(Optional.of(profileData("queried")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<String>getArgument("username")).thenReturn("queried");

    ProfilePayload payload = profileDatafetcher.queryProfile("queried", dfe);

    assertEquals("queried", payload.getProfile().getUsername());
  }

  @Test
  void queryProfile_returns_data_for_anonymous_user() {
    anonymous();
    when(profileQueryService.findByUsername(eq("queried"), isNull()))
        .thenReturn(Optional.of(profileData("queried")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<String>getArgument("username")).thenReturn("queried");

    ProfilePayload payload = profileDatafetcher.queryProfile("queried", dfe);

    assertEquals("queried", payload.getProfile().getUsername());
    verify(profileQueryService).findByUsername(eq("queried"), isNull());
  }

  @Test
  void queryProfile_throws_not_found() {
    User current = authenticate();
    when(profileQueryService.findByUsername("missing", current)).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<String>getArgument("username")).thenReturn("missing");

    assertThrows(
        ResourceNotFoundException.class, () -> profileDatafetcher.queryProfile("missing", dfe));
  }
}
