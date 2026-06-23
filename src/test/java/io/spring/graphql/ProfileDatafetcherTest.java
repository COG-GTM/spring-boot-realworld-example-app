package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher datafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    datafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private ProfileData profileData(String username) {
    return new ProfileData("id", username, "bio", "image", true);
  }

  @Test
  void getUserProfile_resolves_from_local_context_user() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    doReturn(user).when(dfe).getLocalContext();
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData(user.getUsername())));

    Profile profile = datafetcher.getUserProfile(dfe);

    assertEquals(user.getUsername(), profile.getUsername());
    assertTrue(profile.getFollowing());
  }

  @Test
  void getUserProfile_not_found_throws() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    doReturn(user).when(dfe).getLocalContext();
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.getUserProfile(dfe));
  }

  @Test
  void getAuthor_resolves_from_article_local_context() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Article article = Article.newBuilder().slug("slug").build();
    doReturn(article).when(dfe).getSource();
    ArticleData articleData = new ArticleData();
    articleData.setSlug("slug");
    articleData.setProfileData(profileData("author"));
    Map<String, ArticleData> localContext = Map.of("slug", articleData);
    doReturn(localContext).when(dfe).getLocalContext();
    when(profileQueryService.findByUsername(eq("author"), any()))
        .thenReturn(Optional.of(profileData("author")));

    Profile profile = datafetcher.getAuthor(dfe);

    assertEquals("author", profile.getUsername());
  }

  @Test
  void getCommentAuthor_resolves_from_comment_local_context() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Comment comment = Comment.newBuilder().id("cid").build();
    doReturn(comment).when(dfe).getSource();
    CommentData commentData = new CommentData();
    commentData.setId("cid");
    commentData.setProfileData(profileData("commenter"));
    Map<String, CommentData> localContext = Map.of("cid", commentData);
    doReturn(localContext).when(dfe).getLocalContext();
    when(profileQueryService.findByUsername(eq("commenter"), any()))
        .thenReturn(Optional.of(profileData("commenter")));

    Profile profile = datafetcher.getCommentAuthor(dfe);

    assertEquals("commenter", profile.getUsername());
  }

  @Test
  void queryProfile_by_argument_returns_payload() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("target");
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target")));

    ProfilePayload payload = datafetcher.queryProfile("target", dfe);

    assertEquals("target", payload.getProfile().getUsername());
  }
}
