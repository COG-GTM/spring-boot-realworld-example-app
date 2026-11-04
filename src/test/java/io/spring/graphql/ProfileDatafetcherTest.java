package io.spring.graphql;

import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {
  @Mock private ProfileQueryService profileQueryService;

  private ProfileDatafetcher profileDatafetcher;
  private User current;
  private User author;
  private ProfileData authorProfile;

  @BeforeEach
  public void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    current = new User("a@test.com", "a", "123", "", "");
    author = new User("b@test.com", "b", "123", "bio", "image");
    authorProfile = new ProfileData(author.getId(), "b", "bio", "image", true);
    GraphQLTestSupport.authenticate(current);
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  private void assertAuthorProfile(Profile profile) {
    Assertions.assertEquals("b", profile.getUsername());
    Assertions.assertEquals("bio", profile.getBio());
    Assertions.assertEquals("image", profile.getImage());
    Assertions.assertTrue(profile.getFollowing());
  }

  @Test
  public void should_fetch_user_profile() {
    when(profileQueryService.findByUsername("b", current)).thenReturn(Optional.of(authorProfile));

    assertAuthorProfile(profileDatafetcher.getUserProfile(GraphQLTestSupport.env(null, author)));
  }

  @Test
  public void should_fetch_article_author() {
    when(profileQueryService.findByUsername("b", current)).thenReturn(Optional.of(authorProfile));
    ArticleData articleData = io.spring.TestHelper.articleDataFixture("1", author);
    Article article = Article.newBuilder().slug(articleData.getSlug()).build();

    Profile profile =
        profileDatafetcher.getAuthor(
            GraphQLTestSupport.env(
                article, Collections.singletonMap(articleData.getSlug(), articleData)));

    assertAuthorProfile(profile);
  }

  @Test
  public void should_fetch_comment_author() {
    when(profileQueryService.findByUsername("b", current)).thenReturn(Optional.of(authorProfile));
    CommentData commentData =
        new CommentData("c1", "body", "article", new DateTime(), new DateTime(), authorProfile);
    Comment comment = Comment.newBuilder().id("c1").build();

    Profile profile =
        profileDatafetcher.getCommentAuthor(
            GraphQLTestSupport.env(comment, Collections.singletonMap("c1", commentData)));

    assertAuthorProfile(profile);
  }

  @Test
  public void should_query_profile_by_username_argument() {
    GraphQLTestSupport.anonymous();
    when(profileQueryService.findByUsername("b", null)).thenReturn(Optional.of(authorProfile));

    ProfilePayload payload =
        profileDatafetcher.queryProfile(
            "b", GraphQLTestSupport.env(null, null, Collections.singletonMap("username", "b")));

    assertAuthorProfile(payload.getProfile());
  }

  @Test
  public void should_fail_for_unknown_profile() {
    when(profileQueryService.findByUsername("unknown", current)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () ->
            profileDatafetcher.queryProfile(
                "unknown",
                GraphQLTestSupport.env(
                    null, null, Collections.singletonMap("username", "unknown"))));
  }
}
