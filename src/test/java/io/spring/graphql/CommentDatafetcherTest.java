package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommentDatafetcherTest extends GraphQLTestBase {

  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  public void setUpFixtures() {
    articleData = TestHelper.articleDataFixture("1", user);
    commentData =
        new CommentData(
            "comment-1",
            "comment body",
            articleData.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), username, "bio", defaultAvatar, false));
  }

  @Test
  public void should_query_article_comments_with_author() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(Optional.of(new ProfileData(user.getId(), username, "bio", defaultAvatar, false)));
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false));

    String body =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ article(slug: \"%s\") { slug comments(first: 10) { edges { node { body author { username } } } } } }",
                articleData.getSlug()),
            "data.article.comments.edges[0].node.body");

    Assertions.assertEquals(commentData.getBody(), body);
  }

  @Test
  public void should_reject_article_comments_without_first_or_last() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), any()))
        .thenReturn(Optional.of(articleData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "{ article(slug: \"%s\") { comments { edges { node { body } } } } }",
                articleData.getSlug()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }
}
