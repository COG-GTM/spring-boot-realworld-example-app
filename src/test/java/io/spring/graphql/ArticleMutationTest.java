package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;

  @Mock private ArticleFavoriteRepository articleFavoriteRepository;

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article =
        new Article(
            "test-title",
            "test-description",
            "test-body",
            Arrays.asList("tag1", "tag2"),
            user.getId(),
            new DateTime());
  }

  @Test
  public void should_create_article_successfully() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("test-title")
            .description("test-description")
            .body("test-body")
            .tagList(Arrays.asList("tag1", "tag2"))
            .build();

    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

      assertThat(result).isNotNull();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getLocalContext()).isEqualTo(article);
      verify(articleCommandService).createArticle(any(), any());
    }
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("test-title")
            .description("test-description")
            .body("test-body")
            .build();

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    }
  }

  @Test
  public void should_update_article_successfully() {
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .title("updated-title")
            .description("updated-description")
            .body("updated-body")
            .build();

    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DataFetcherResult<ArticlePayload> result =
          articleMutation.updateArticle("test-slug", input);

      assertThat(result).isNotNull();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getLocalContext()).isEqualTo(article);
      verify(articleCommandService).updateArticle(any(), any());
    }
  }

  @Test
  public void should_fail_update_article_with_nonexistent_slug() {
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder().title("updated-title").build();

    when(articleRepository.findBySlug(eq("nonexistent-slug"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      assertThrows(
          ResourceNotFoundException.class,
          () -> articleMutation.updateArticle("nonexistent-slug", input));
    }
  }

  @Test
  public void should_favorite_article_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-slug");

      assertThat(result).isNotNull();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getLocalContext()).isEqualTo(article);
      verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    }
  }

  @Test
  public void should_unfavorite_article_successfully() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(any(), any())).thenReturn(Optional.of(favorite));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-slug");

      assertThat(result).isNotNull();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getLocalContext()).isEqualTo(article);
      verify(articleFavoriteRepository).remove(any(ArticleFavorite.class));
    }
  }

  @Test
  public void should_delete_article_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DeletionStatus result = articleMutation.deleteArticle("test-slug");

      assertThat(result).isNotNull();
      assertThat(result.getSuccess()).isTrue();
      verify(articleRepository).remove(any(Article.class));
    }
  }

  @Test
  public void should_fail_delete_article_without_authorization() {
    User otherUser = new User("other@example.com", "otheruser", "password", "bio", "image");
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(otherUser));

      assertThrows(
          NoAuthorizationException.class, () -> articleMutation.deleteArticle("test-slug"));
    }
  }
}
