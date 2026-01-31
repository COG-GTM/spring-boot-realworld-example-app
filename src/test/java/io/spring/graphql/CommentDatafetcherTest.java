package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class CommentDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private UserRepository userRepository;

  private User user;
  private ArticleData articleData;
  private CommentData commentData;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User(
        "test@example.com",
        "testuser",
        "password",
        "bio",
        "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null));

    profileData =
        new ProfileData(
            user.getId(),
            user.getUsername(),
            user.getBio(),
            user.getImage(),
            false);

    DateTime now = new DateTime();
    articleData =
        new ArticleData(
            "article-id",
            "test-title",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1", "tag2"),
            profileData);

    commentData =
        new CommentData(
            "comment-id",
            "Test comment body",
            "article-id",
            now,
            now,
            profileData);
  }

  @Test
  public void should_get_article_comments() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    CursorPager<CommentData> commentPager =
        new CursorPager<>(
            Arrays.asList(commentData),
            CursorPager.Direction.NEXT,
            false);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any()))
        .thenReturn(commentPager);

    String query =
        "query { article(slug: \"test-title\") { slug "
            + "comments(first: 10) { edges { cursor node { id body } } "
            + "pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
    assertEquals("test-title", result.get("slug"));
  }

  @Test
  public void should_get_empty_comments_list() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    CursorPager<CommentData> emptyPager =
        new CursorPager<>(
            Collections.emptyList(),
            CursorPager.Direction.NEXT,
            false);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any()))
        .thenReturn(emptyPager);

    String query =
        "query { article(slug: \"test-title\") { slug "
            + "comments(first: 10) { edges { node { id } } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
  }

  @Test
  public void should_handle_comments_pagination() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    CursorPager<CommentData> commentPager =
        new CursorPager<>(
            Arrays.asList(commentData),
            CursorPager.Direction.NEXT,
            true);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any()))
        .thenReturn(commentPager);

    String query =
        "query { article(slug: \"test-title\") { "
            + "comments(first: 5) { edges { cursor node { id body } } "
            + "pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
  }

  @Test
  public void should_get_comments_with_author() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    CursorPager<CommentData> commentPager =
        new CursorPager<>(
            Arrays.asList(commentData),
            CursorPager.Direction.NEXT,
            false);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any()))
        .thenReturn(commentPager);

    String query =
        "query { article(slug: \"test-title\") { "
            + "comments(first: 10) { edges { node { id body "
            + "author { username bio } } } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
  }

  @Test
  public void should_handle_backward_pagination_for_comments() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    CursorPager<CommentData> commentPager =
        new CursorPager<>(
            Arrays.asList(commentData),
            CursorPager.Direction.PREV,
            true);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any()))
        .thenReturn(commentPager);

    String query =
        "query { article(slug: \"test-title\") { "
            + "comments(last: 5) { "
            + "edges { cursor node { id } } "
            + "pageInfo { hasNextPage hasPreviousPage } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
  }
}
