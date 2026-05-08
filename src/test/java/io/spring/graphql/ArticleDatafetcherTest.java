package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment innerDfe;
  @InjectMocks private ArticleDatafetcher datafetcher;
  private DgsDataFetchingEnvironment dfe;

  @BeforeEach
  public void setUp() {
    dfe = new DgsDataFetchingEnvironment(innerDfe);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "k", "anon", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData buildArticleData(String slug) {
    DateTime now = new DateTime();
    return new ArticleData(
        "id-" + slug,
        slug,
        "title",
        "desc",
        "body",
        false,
        0,
        now,
        now,
        Arrays.asList("java"),
        new ProfileData("uid", "alice", "", "", false));
  }

  @Test
  public void should_throw_when_first_and_last_are_both_null_in_get_articles() {
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  public void should_throw_when_first_and_last_are_both_null_in_feed() {
    assertThrows(
        IllegalArgumentException.class, () -> datafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  public void should_find_article_by_slug() {
    ArticleData articleData = buildArticleData("slug-1");
    when(articleQueryService.findBySlug(eq("slug-1"), any())).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = datafetcher.findArticleBySlug("slug-1");

    assertNotNull(result.getData());
    assertEquals("slug-1", result.getData().getSlug());
  }

  @Test
  public void should_throw_when_article_not_found_by_slug() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.findArticleBySlug("missing"));
  }

  @Test
  public void should_get_articles_with_first_param() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(buildArticleData("a")), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }
}
