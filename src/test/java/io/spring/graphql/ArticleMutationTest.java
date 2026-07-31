package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class
    })
class ArticleMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private io.spring.core.user.UserRepository userRepository;

  private User author;
  private Article article;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    author = userFixture("john");
    article =
        new Article("a title", "a description", "a body", Arrays.asList("java"), author.getId());
    articleData = TestHelper.getArticleDataFromArticleAndUser(article, author);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(author.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    author.getId(),
                    author.getUsername(),
                    author.getBio(),
                    author.getImage(),
                    false)));
  }

  @Test
  void should_create_article() {
    authenticate(author);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(article);

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\", body: \"a body\", tagList: [\"java\"]}) { article { slug title } } }",
            "data.createArticle.article.slug");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(author));
    assertThat(captor.getValue().getTitle()).isEqualTo("a title");
    assertThat(captor.getValue().getTagList()).containsExactly("java");
    assertThat(slug).isEqualTo(article.getSlug());
  }

  @Test
  void should_default_tag_list_to_empty_when_absent() {
    authenticate(author);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(article);

    dgsQueryExecutor.executeAndExtractJsonPath(
        "mutation { createArticle(input: {title: \"a title\", description: \"a description\", body: \"a body\"}) { article { slug } } }",
        "data.createArticle.article.slug");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(author));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_not_create_article_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\", body: \"a body\"}) { article { slug } } }");

    assertFailedWith(result, AuthenticationException.class);
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void should_update_article() {
    authenticate(author);
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new title\", body: \"new body\", description: \"new description\"}) { article { slug } } }",
                article.getSlug()),
            "data.updateArticle.article.slug");

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getBody()).isEqualTo("new body");
    assertThat(captor.getValue().getDescription()).isEqualTo("new description");
    assertThat(slug).isEqualTo(article.getSlug());
  }

  @Test
  void should_not_update_article_of_other_user() {
    authenticate(userFixture("jane"));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new title\"}) { article { slug } } }",
                article.getSlug()));

    assertFailedWith(result, NoAuthorizationException.class);
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void should_return_error_when_updating_unknown_article() {
    authenticate(author);
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"unknown\", changes: {title: \"new title\"}) { article { slug } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
  }

  @Test
  void should_favorite_article() {
    authenticate(author);

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { favoriteArticle(slug: \"%s\") { article { slug } } }",
                article.getSlug()),
            "data.favoriteArticle.article.slug");

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(author.getId());
    assertThat(slug).isEqualTo(article.getSlug());
  }

  @Test
  void should_unfavorite_article() {
    authenticate(author);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), author.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(author.getId())))
        .thenReturn(Optional.of(favorite));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }",
                article.getSlug()),
            "data.unfavoriteArticle.article.slug");

    verify(articleFavoriteRepository).remove(eq(favorite));
    assertThat(slug).isEqualTo(article.getSlug());
  }

  @Test
  void should_ignore_unfavorite_when_article_was_not_favorited() {
    authenticate(author);
    when(articleFavoriteRepository.find(eq(article.getId()), eq(author.getId())))
        .thenReturn(Optional.empty());

    dgsQueryExecutor.executeAndExtractJsonPath(
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }", article.getSlug()),
        "data.unfavoriteArticle.article.slug");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void should_delete_own_article() {
    authenticate(author);

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug()),
            "data.deleteArticle.success");

    verify(articleRepository).remove(eq(article));
    assertThat(success).isTrue();
  }

  @Test
  void should_not_delete_article_of_other_user() {
    authenticate(userFixture("jane"));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug()));

    assertFailedWith(result, NoAuthorizationException.class);
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void should_return_error_when_article_payload_article_is_gone() {
    authenticate(author);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), isNull())).thenReturn(Optional.empty());
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\", body: \"a body\"}) { article { slug } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
  }
}
