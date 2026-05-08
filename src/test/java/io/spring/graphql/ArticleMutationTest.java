package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;
  @InjectMocks private ArticleMutation mutation;

  private User currentUser;
  private Article article;

  @BeforeEach
  public void setUp() {
    currentUser = new User("a@b.com", "alice", "secret", "", "");
    article = new Article("Title", "Desc", "Body", Arrays.asList("java"), currentUser.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_article() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList("java"))
            .build();
    when(articleCommandService.createArticle(any(), eq(currentUser))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = mutation.createArticle(input);

    assertNotNull(result);
    assertSame(article, result.getLocalContext());
    verify(articleCommandService, times(1)).createArticle(any(), eq(currentUser));
  }

  @Test
  public void should_throw_authentication_when_unauthenticated_create() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    CreateArticleInput input = CreateArticleInput.newBuilder().title("t").body("b").build();
    assertThrows(AuthenticationException.class, () -> mutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        mutation.updateArticle(
            article.getSlug(),
            UpdateArticleInput.newBuilder().title("New").body("nb").description("nd").build());

    assertNotNull(result);
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_throw_resource_not_found_when_updating_unknown_article() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            mutation.updateArticle(
                "missing",
                UpdateArticleInput.newBuilder().title("t").body("b").description("d").build()));
  }

  @Test
  public void should_throw_no_authorization_when_updating_article_of_another_user() {
    User otherUser = new User("b@b.com", "bob", "secret", "", "");
    Article otherArticle = new Article("T", "D", "B", Arrays.asList("a"), otherUser.getId());
    when(articleRepository.findBySlug(otherArticle.getSlug()))
        .thenReturn(Optional.of(otherArticle));

    assertThrows(
        NoAuthorizationException.class,
        () ->
            mutation.updateArticle(
                otherArticle.getSlug(),
                UpdateArticleInput.newBuilder().title("t").body("b").description("d").build()));
  }

  @Test
  public void should_favorite_article() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = mutation.favoriteArticle(article.getSlug());

    assertNotNull(result);
    verify(articleFavoriteRepository, times(1)).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_when_favorite_exists() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.of(favorite));

    mutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, times(1)).remove(favorite);
  }

  @Test
  public void should_unfavorite_article_no_op_when_favorite_missing() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.empty());

    mutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_article() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = mutation.deleteArticle(article.getSlug());

    assertTrue(status.getSuccess());
    verify(articleRepository, times(1)).remove(article);
  }

  @Test
  public void should_throw_no_authorization_when_deleting_article_of_another_user() {
    User otherUser = new User("b@b.com", "bob", "secret", "", "");
    Article otherArticle = new Article("T", "D", "B", Arrays.asList("a"), otherUser.getId());
    when(articleRepository.findBySlug(otherArticle.getSlug()))
        .thenReturn(Optional.of(otherArticle));

    assertThrows(
        NoAuthorizationException.class, () -> mutation.deleteArticle(otherArticle.getSlug()));
    verify(articleRepository, never()).remove(any());
  }
}
