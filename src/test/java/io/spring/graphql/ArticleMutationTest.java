package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
public class ArticleMutationTest extends GraphQLTestBase {

  private static final DateTime TIME = new DateTime(2022, 2, 2, 10, 0, DateTimeZone.UTC);

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private io.spring.core.user.UserRepository userRepository;

  @Test
  void should_create_article() {
    Article article = article(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData(article)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\","
                + " body: \"a body\", tagList: [\"joda\"]}) { article { slug title body tagList } }"
                + " }");

    assertThat(context.read("$.data.createArticle.article.slug", String.class))
        .isEqualTo(article.getSlug());
    assertThat(context.read("$.data.createArticle.article.title", String.class))
        .isEqualTo("a title");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTitle()).isEqualTo("a title");
    assertThat(captor.getValue().getDescription()).isEqualTo("a description");
    assertThat(captor.getValue().getBody()).isEqualTo("a body");
    assertThat(captor.getValue().getTagList()).containsExactly("joda");
  }

  @Test
  void should_default_tag_list_to_empty_when_absent() {
    Article article = article(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\","
                + " body: \"a body\"}) { __typename } }");

    assertThat(result.getErrors()).isEmpty();
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_not_create_article_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\","
                + " body: \"a body\"}) { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void should_update_own_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData(article)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"new title\", body: \"new body\", description: \"new"
                + " description\"}) { article { slug } } }");

    assertThat(context.read("$.data.updateArticle.article.slug", String.class))
        .isEqualTo(article.getSlug());

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getBody()).isEqualTo("new body");
    assertThat(captor.getValue().getDescription()).isEqualTo("new description");
  }

  @Test
  void should_not_update_article_of_another_user() {
    Article article = article(new User("other@jacob.com", "other", "123", "", DEFAULT_AVATAR));
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"new title\"}) { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void should_not_update_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"unknown\", changes: {title: \"new title\"}) {"
                + " __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_favorite_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData(article)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { favoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug favorited } } }");

    assertThat(context.read("$.data.favoriteArticle.article.slug", String.class))
        .isEqualTo(article.getSlug());

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
  }

  @Test
  void should_not_favorite_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { favoriteArticle(slug: \"unknown\") { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  void should_unfavorite_article() {
    Article article = article(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData(article)));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfavoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }");

    assertThat(result.getErrors()).isEmpty();
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void should_ignore_unfavorite_when_favorite_is_absent() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { __typename } }");

    assertThat(result.getErrors()).isEmpty();
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void should_delete_own_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }");

    assertThat(context.read("$.data.deleteArticle.success", Boolean.class)).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void should_not_delete_article_of_another_user() {
    Article article = article(new User("other@jacob.com", "other", "123", "", DEFAULT_AVATAR));
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void should_not_delete_article_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { deleteArticle(slug: \"a-title\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
  }

  @Test
  void should_fail_article_payload_when_article_is_gone() {
    Article article = article(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"a title\", description: \"a description\","
                + " body: \"a body\"}) { article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  private Article article(User author) {
    return new Article(
        "a title", "a description", "a body", Arrays.asList("joda"), author.getId(), TIME);
  }

  private ArticleData articleData(Article article) {
    return new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        TIME,
        TIME,
        Arrays.asList("joda"),
        profileData);
  }
}
