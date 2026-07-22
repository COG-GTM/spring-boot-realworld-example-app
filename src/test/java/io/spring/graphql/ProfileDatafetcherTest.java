package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProfileDatafetcherTest extends GraphQLTestBase {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dfe;

  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    setAnonymous();
  }

  private ProfileData profileData(String username) {
    return new ProfileData("id-" + username, username, "bio", "image", false);
  }

  @Test
  public void should_query_profile_by_argument() {
    when(dfe.getArgument("username")).thenReturn("target");
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(profileData("target")));

    ProfilePayload payload = profileDatafetcher.queryProfile("target", dfe);

    assertThat(payload.getProfile().getUsername(), is("target"));
    assertThat(payload.getProfile().getBio(), is("bio"));
    assertThat(payload.getProfile().getImage(), is("image"));
    assertThat(payload.getProfile().getFollowing(), is(false));
  }

  @Test
  public void should_throw_when_profile_not_found() {
    when(dfe.getArgument("username")).thenReturn("missing");
    when(profileQueryService.findByUsername(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> profileDatafetcher.queryProfile("missing", dfe));
  }

  @Test
  public void should_get_user_profile_from_local_context() {
    User user = new User("email@test.com", "username", "pass", "", "");
    when(dfe.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("username"), any()))
        .thenReturn(Optional.of(profileData("username")));

    Profile profile = profileDatafetcher.getUserProfile(dfe);
    assertThat(profile.getUsername(), is("username"));
  }

  @Test
  public void should_get_author_from_article_local_context() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData =
        new ArticleData(
            "article-id",
            "a-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            null,
            null,
            Collections.emptyList(),
            profileData("author"));
    Map<String, ArticleData> map = Collections.singletonMap("a-slug", articleData);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(map);
    when(dfe.<Article>getSource()).thenReturn(article);
    when(profileQueryService.findByUsername(eq("author"), any()))
        .thenReturn(Optional.of(profileData("author")));

    Profile profile = profileDatafetcher.getAuthor(dfe);
    assertThat(profile.getUsername(), is("author"));
  }

  @Test
  public void should_get_comment_author_from_local_context() {
    Comment comment = Comment.newBuilder().id("comment-id").build();
    CommentData commentData =
        new CommentData("comment-id", "body", "article-id", null, null, profileData("commenter"));
    Map<String, CommentData> map = Collections.singletonMap("comment-id", commentData);
    when(dfe.<Map<String, CommentData>>getLocalContext()).thenReturn(map);
    when(dfe.<Comment>getSource()).thenReturn(comment);
    when(profileQueryService.findByUsername(eq("commenter"), any()))
        .thenReturn(Optional.of(profileData("commenter")));

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);
    assertThat(profile.getUsername(), is("commenter"));
  }
}
