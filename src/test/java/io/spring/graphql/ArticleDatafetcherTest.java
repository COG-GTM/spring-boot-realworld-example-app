package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;

  @Mock private io.spring.core.user.UserRepository userRepository;

  @InjectMocks private ArticleDatafetcher articleDatafetcher;

  private User testUser;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image");
    ProfileData profileData =
        new ProfileData(testUser.getId(), testUser.getUsername(), testUser.getBio(), testUser.getImage(), false);
    articleData =
        new ArticleData(
            "article-id",
            "test-slug",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("tag1", "tag2"),
            profileData);
  }

  @Test
  public void should_find_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-slug"), any()))
        .thenReturn(Optional.of(articleData));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals("test-slug", result.getData().getSlug());
      assertEquals("Test Title", result.getData().getTitle());
      assertEquals("Test Description", result.getData().getDescription());
      assertEquals("Test Body", result.getData().getBody());
    }
  }

  @Test
  public void should_throw_exception_for_non_existent_article() {
    when(articleQueryService.findBySlug(eq("non-existent"), any())).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      assertThrows(
          ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("non-existent"));
    }
  }

  @Test
  public void should_find_article_without_authentication() {
    when(articleQueryService.findBySlug(eq("test-slug"), any()))
        .thenReturn(Optional.of(articleData));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals("test-slug", result.getData().getSlug());
    }
  }
}
