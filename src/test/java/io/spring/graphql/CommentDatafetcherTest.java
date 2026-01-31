package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentDatafetcher.class, ArticleDatafetcher.class})
@Import({ProfileDatafetcher.class})
@ActiveProfiles("test")
public class CommentDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  private User user;
  private ArticleData articleData;
  private CommentData commentData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    DateTime now = DateTime.now();
    articleData = new ArticleData(
        "article-id",
        "test-article",
        "Test Article",
        "Test Description",
        "Test Body",
        false,
        0,
        now,
        now,
        Arrays.asList("tag1", "tag2"),
        profileData);
    commentData = new CommentData(
        "comment-id",
        "Test comment body",
        "article-id",
        now,
        now,
        profileData);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldGetArticleComments() {
    CursorPager<CommentData> pager = new CursorPager<>(
        Collections.singletonList(commentData),
        Direction.NEXT,
        false);

    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { slug comments(first: 10) { edges { node { id body } cursor } pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> pageInfo = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(false);
  }

  @Test
  void shouldGetArticleCommentsWithPagination() {
    CursorPager<CommentData> pager = new CursorPager<>(
        Collections.singletonList(commentData),
        Direction.NEXT,
        true);

    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { comments(first: 1) { edges { node { id body } } pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> pageInfo = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(false);
  }

  @Test
  void shouldGetArticleCommentsWithLastPagination() {
    CursorPager<CommentData> pager = new CursorPager<>(
        Collections.singletonList(commentData),
        Direction.PREV,
        true);

    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { comments(last: 10) { edges { node { id body } } pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> pageInfo = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);
  }

  @Test
  void shouldReturnErrorWhenCommentsFirstAndLastBothNull() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { comments { edges { node { id } } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldGetEmptyCommentsForArticle() {
    CursorPager<CommentData> pager = new CursorPager<>(
        Collections.emptyList(),
        Direction.NEXT,
        false);

    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { comments(first: 10) { edges { node { id body } } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");

    assertThat(edges).isEmpty();
  }
}
