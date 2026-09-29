package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_articles_connection() {
    DateTime now = DateTime.now();
    ArticleData article1 =
        new ArticleData(
            "id-1", "slug-1", "title-1", "desc-1", "body-1", false, 0, now, now, List.of(), null);
    ArticleData article2 =
        new ArticleData(
            "id-2",
            "slug-2",
            "title-2",
            "desc-2",
            "body-2",
            false,
            0,
            now.plus(1),
            now.plus(1),
            List.of(),
            null);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(List.of(article1, article2), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(first: 2) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }");

    List<String> cursors = context.read("data.articles.edges[*].cursor");
    List<String> slugs = context.read("data.articles.edges[*].node.slug");
    List<String> titles = context.read("data.articles.edges[*].node.title");
    assertThat(slugs).containsExactly("slug-1", "slug-2");
    assertThat(titles).containsExactly("title-1", "title-2");
    assertThat(cursors)
        .containsExactly(article1.getCursor().toString(), article2.getCursor().toString());
    assertThat((Boolean) context.read("data.articles.pageInfo.hasNextPage")).isTrue();
    assertThat((Boolean) context.read("data.articles.pageInfo.hasPreviousPage")).isFalse();
    assertThat((String) context.read("data.articles.pageInfo.startCursor"))
        .isEqualTo(cursors.get(0));
    assertThat((String) context.read("data.articles.pageInfo.endCursor")).isEqualTo(cursors.get(1));
  }
}
