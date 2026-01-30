package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentDatafetcher.class, ArticleDatafetcher.class})
public class CommentDatafetcherTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_get_article_comments() {
    Article article = new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData(user.getId(), username, "", defaultAvatar, false);
    CommentData comment1 = new CommentData("comment1", "Comment body 1", articleData.getId(), now, now, profileData);
    CommentData comment2 = new CommentData("comment2", "Comment body 2", articleData.getId(), now, now, profileData);
    List<CommentData> comments = Arrays.asList(comment1, comment2);

    CursorPager<CommentData> cursorPager = new CursorPager<>(comments, Direction.NEXT, false);

    when(articleQueryService.findBySlug(eq(article.getSlug()), any())).thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { article(slug: \""
            + article.getSlug()
            + "\") { slug comments(first: 10) { edges { node { id body } } pageInfo { hasNextPage } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");
    Boolean hasNextPage =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.pageInfo.hasNextPage");

    assertThat(edges).hasSize(2);
    assertThat(hasNextPage).isFalse();
  }

  @Test
  public void should_get_empty_comments_when_none_exist() {
    Article article = new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    CursorPager<CommentData> cursorPager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findBySlug(eq(article.getSlug()), any())).thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { article(slug: \""
            + article.getSlug()
            + "\") { comments(first: 10) { edges { node { id } } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");

    assertThat(edges).isEmpty();
  }
}
