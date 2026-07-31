package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      GraphQLCustomizeExceptionHandler.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class
    })
class ArticleDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private UserRepository userRepository;

  private User author;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    author = userFixture("john");
    articleData = TestHelper.articleDataFixture("test", author);
    when(profileQueryService.findByUsername(eq(author.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    author.getId(),
                    author.getUsername(),
                    author.getBio(),
                    author.getImage(),
                    false)));
    when(userRepository.findByUsername(eq(author.getUsername()))).thenReturn(Optional.of(author));
  }

  @Test
  void should_query_article_by_slug_with_author() {
    anonymous();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ article(slug: \"%s\") { slug title body author { username bio } } }",
                articleData.getSlug()));

    assertThat(context.read("data.article.slug", String.class)).isEqualTo(articleData.getSlug());
    assertThat(context.read("data.article.title", String.class)).isEqualTo(articleData.getTitle());
    assertThat(context.read("data.article.author.username", String.class))
        .isEqualTo(author.getUsername());
    assertThat(context.read("data.article.author.bio", String.class)).isEqualTo(author.getBio());
  }

  @Test
  void should_return_error_when_article_not_found() {
    anonymous();
    when(articleQueryService.findBySlug(eq("missing"), isNull())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"missing\") { slug } }");

    assertFailedWith(result, ResourceNotFoundException.class);
  }

  @Test
  void should_query_articles_forward_with_first() {
    anonymous();
    ArticleData other = TestHelper.articleDataFixture("other", author);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Arrays.asList(articleData, other), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(first: 2) { edges { cursor node { slug } } pageInfo { hasNextPage hasPreviousPage } } }");

    assertThat(context.<List<String>>read("data.articles.edges[*].node.slug"))
        .containsExactly(articleData.getSlug(), other.getSlug());
    assertThat(context.<List<String>>read("data.articles.edges[*].cursor"))
        .containsExactly(articleData.getCursor().toString(), other.getCursor().toString());
    assertThat(context.read("data.articles.pageInfo.hasNextPage", Boolean.class)).isTrue();
    assertThat(context.read("data.articles.pageInfo.hasPreviousPage", Boolean.class)).isFalse();
  }

  @Test
  void should_query_articles_backward_with_last_and_before_cursor() {
    anonymous();
    DateTime cursorTime = articleData.getUpdatedAt();
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jane"), any(), isNull()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    Boolean hasPreviousPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ articles(last: 1, before: \"%d\", withTag: \"java\", authoredBy: \"john\", favoritedBy: \"jane\") { pageInfo { hasPreviousPage } } }",
                cursorTime.getMillis()),
            "data.articles.pageInfo.hasPreviousPage");

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jane"), captor.capture(), isNull());
    assertThat(hasPreviousPage).isTrue();
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(1);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(cursorTime.getMillis());
  }

  @Test
  void should_return_error_when_neither_first_nor_last_is_given() {
    anonymous();

    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { cursor } } }");

    assertFailedWith(result, IllegalArgumentException.class);
  }

  @Test
  void should_query_feed_of_current_user() {
    authenticate(author);
    when(articleQueryService.findUserFeedWithCursor(eq(author), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { edges { node { slug } } } }", "data.feed.edges[*].node.slug");

    assertThat(slugs).containsExactly(articleData.getSlug());
  }

  @Test
  void should_query_articles_of_a_profile() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(author.getUsername()), isNull(), any(), isNull()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { articles(first: 10) { edges { node { slug } } } } } }",
                author.getUsername()),
            "data.profile.profile.articles.edges[*].node.slug");

    assertThat(slugs).containsExactly(articleData.getSlug());
  }

  @Test
  void should_query_favorites_of_a_profile() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(author.getUsername()), any(), isNull()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { favorites(first: 10) { edges { node { slug } } } } } }",
                author.getUsername()),
            "data.profile.profile.favorites.edges[*].node.slug");

    assertThat(slugs).containsExactly(articleData.getSlug());
  }

  @Test
  void should_query_feed_of_a_profile() {
    anonymous();
    when(articleQueryService.findUserFeedWithCursor(eq(author), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { feed(first: 10) { edges { node { slug } } } } } }",
                author.getUsername()),
            "data.profile.profile.feed.edges[*].node.slug");

    assertThat(slugs).containsExactly(articleData.getSlug());
  }

  @Test
  void should_return_error_when_profile_of_feed_does_not_exist() {
    anonymous();
    when(profileQueryService.findByUsername(eq("ghost"), any()))
        .thenReturn(Optional.of(new ProfileData("ghost-id", "ghost", "", DEFAULT_AVATAR, false)));
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"ghost\") { profile { feed(first: 10) { edges { cursor } } } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
  }
}
