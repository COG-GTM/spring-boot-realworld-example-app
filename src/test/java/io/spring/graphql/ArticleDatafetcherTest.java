package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleDatafetcherTest extends GraphQLTestBase {
  private ArticleData articleData;
  private ProfileData authorProfile;

  @BeforeEach
  public void setUp() {
    articleData = TestHelper.articleDataFixture("1", user);
    authorProfile =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(authorProfile));
  }

  @Test
  public void should_query_article_by_slug() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "query ($slug: String!) { article(slug: $slug) { slug title description body"
                + " favorited favoritesCount tagList createdAt author { username following } } }",
            Collections.singletonMap("slug", articleData.getSlug()));

    assertThat(context.<String>read("data.article.slug")).isEqualTo(articleData.getSlug());
    assertThat(context.<String>read("data.article.title")).isEqualTo(articleData.getTitle());
    assertThat(context.<String>read("data.article.body")).isEqualTo(articleData.getBody());
    assertThat(context.<Boolean>read("data.article.favorited")).isFalse();
    assertThat(context.<Integer>read("data.article.favoritesCount")).isEqualTo(0);
    assertThat(context.<String>read("data.article.createdAt")).isNotEmpty();
    assertThat(context.<String>read("data.article.author.username")).isEqualTo(user.getUsername());
    assertThat(context.<Boolean>read("data.article.author.following")).isFalse();
  }

  @Test
  public void should_return_error_for_unknown_article() {
    when(articleQueryService.findBySlug(eq("unknown"), any())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"unknown\") { slug } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getPath()).containsExactly("article");
  }

  @Test
  public void should_query_article_with_comments() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), any()))
        .thenReturn(Optional.of(articleData));
    CommentData commentData =
        new CommentData(
            "comment-1",
            "nice article",
            articleData.getId(),
            new DateTime(),
            new DateTime(),
            authorProfile);
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments(first: 10) { edges { cursor node { id body author { username"
                + " } } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } } }");

    assertThat(context.<String>read("data.article.comments.edges[0].node.id"))
        .isEqualTo("comment-1");
    assertThat(context.<String>read("data.article.comments.edges[0].node.body"))
        .isEqualTo("nice article");
    assertThat(context.<String>read("data.article.comments.edges[0].node.author.username"))
        .isEqualTo(user.getUsername());
    assertThat(context.<Boolean>read("data.article.comments.pageInfo.hasNextPage")).isTrue();
    assertThat(context.<Boolean>read("data.article.comments.pageInfo.hasPreviousPage")).isFalse();
    assertThat(context.<String>read("data.article.comments.pageInfo.startCursor"))
        .isEqualTo(commentData.getCursor().toString());
  }

  @Test
  public void should_list_articles_with_first() {
    ArticleData another = TestHelper.articleDataFixture("2", user);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq(user.getUsername()), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Arrays.asList(articleData, another), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(first: 2, withTag: \"java\", authoredBy: \""
                + user.getUsername()
                + "\") { edges { cursor node { slug author { username } } } pageInfo {"
                + " hasNextPage hasPreviousPage endCursor } } }");

    List<String> slugs = context.read("data.articles.edges[*].node.slug");
    assertThat(slugs).containsExactly(articleData.getSlug(), another.getSlug());
    assertThat(context.<String>read("data.articles.edges[1].node.author.username"))
        .isEqualTo(user.getUsername());
    assertThat(context.<Boolean>read("data.articles.pageInfo.hasNextPage")).isTrue();
    assertThat(context.<Boolean>read("data.articles.pageInfo.hasPreviousPage")).isFalse();
    assertThat(context.<String>read("data.articles.pageInfo.endCursor"))
        .isEqualTo(another.getCursor().toString());
  }

  @Test
  public void should_list_articles_with_last_and_before() {
    long before = new DateTime().getMillis();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), any(), isNull()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(last: 1, before: \""
                + before
                + "\", favoritedBy: \""
                + user.getUsername()
                + "\") { edges { node { slug } } pageInfo { hasNextPage hasPreviousPage } } }");

    assertThat(context.<String>read("data.articles.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
    assertThat(context.<Boolean>read("data.articles.pageInfo.hasNextPage")).isFalse();
    assertThat(context.<Boolean>read("data.articles.pageInfo.hasPreviousPage")).isTrue();
  }

  @Test
  public void should_return_error_when_neither_first_nor_last_provided() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { cursor } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getPath()).containsExactly("articles");
  }

  @Test
  public void should_query_feed_of_current_user() {
    loginAs(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }");

    assertThat(context.<String>read("data.feed.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
    assertThat(context.<Boolean>read("data.feed.pageInfo.hasNextPage")).isFalse();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), any());
  }

  @Test
  public void should_return_empty_feed_page() {
    loginAs(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(last: 10) { edges { cursor } pageInfo { startCursor endCursor } } }");

    assertThat(context.<List<Object>>read("data.feed.edges")).isEmpty();
    assertThat(context.<String>read("data.feed.pageInfo.startCursor")).isNull();
    assertThat(context.<String>read("data.feed.pageInfo.endCursor")).isNull();
  }

  @Test
  public void should_query_profile_articles_favorites_and_feed() {
    loginAs(user);
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    ArticleData authored = TestHelper.articleDataFixture("authored", user);
    ArticleData favorited = TestHelper.articleDataFixture("favorited", user);
    ArticleData feed = TestHelper.articleDataFixture("feed", user);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(user.getUsername()), isNull(), any(), eq(user)))
        .thenReturn(new CursorPager<>(Collections.singletonList(authored), Direction.NEXT, false));
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), any(), eq(user)))
        .thenReturn(new CursorPager<>(Collections.singletonList(favorited), Direction.PREV, false));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(feed), Direction.NEXT, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { username"
                + " articles(first: 5) { edges { node { slug } } }"
                + " favorites(last: 5) { edges { node { slug } } }"
                + " feed(first: 5) { edges { node { slug } } } } } }");

    assertThat(context.<String>read("data.profile.profile.username")).isEqualTo(user.getUsername());
    assertThat(context.<String>read("data.profile.profile.articles.edges[0].node.slug"))
        .isEqualTo(authored.getSlug());
    assertThat(context.<String>read("data.profile.profile.favorites.edges[0].node.slug"))
        .isEqualTo(favorited.getSlug());
    assertThat(context.<String>read("data.profile.profile.feed.edges[0].node.slug"))
        .isEqualTo(feed.getSlug());
  }
}
