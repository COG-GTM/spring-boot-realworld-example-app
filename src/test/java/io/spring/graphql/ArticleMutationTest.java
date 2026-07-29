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
import io.spring.TestHelper;
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
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  private User author;
  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    author = new User("author@test.com", "author", "123", "bio", "image");
    article = new Article("Test Article", "desc", "body", Arrays.asList("java"), author.getId());
    articleData = TestHelper.getArticleDataFromArticleAndUser(article, author);
    authenticate(author);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_an_article() {
    when(articleCommandService.createArticle(any(), eq(author))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createArticle(input: {title: \"Test Article\", description: \"desc\", body:"
                + " \"body\", tagList: [\"java\"]}) { article { slug title description body } } }");

    assertThat(result.<String>read("data.createArticle.article.slug")).isEqualTo(article.getSlug());
    assertThat(result.<String>read("data.createArticle.article.title"))
        .isEqualTo(article.getTitle());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(author));
    assertThat(captor.getValue().getTitle()).isEqualTo("Test Article");
    assertThat(captor.getValue().getDescription()).isEqualTo("desc");
    assertThat(captor.getValue().getBody()).isEqualTo("body");
    assertThat(captor.getValue().getTagList()).containsExactly("java");
  }

  @Test
  public void should_default_the_tag_list_to_an_empty_list() {
    when(articleCommandService.createArticle(any(), eq(author))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    dgsQueryExecutor.executeAndGetDocumentContext(
        "mutation { createArticle(input: {title: \"Test Article\", description: \"desc\", body:"
            + " \"body\"}) { article { slug } } }");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(author));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void should_reject_article_creation_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_an_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new title\", body: \"new"
                    + " body\", description: \"new desc\"}) { article { slug } } }",
                article.getSlug()));

    assertThat(result.<String>read("data.updateArticle.article.slug")).isEqualTo(article.getSlug());

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getBody()).isEqualTo("new body");
    assertThat(captor.getValue().getDescription()).isEqualTo("new desc");
  }

  @Test
  public void should_reject_updating_an_article_of_another_user() {
    User other = new User("other@test.com", "other", "123", "", "");
    authenticate(other);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new title\"}) { article {"
                    + " slug } } }",
                article.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_report_error_when_updating_an_unknown_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"missing\", changes: {title: \"t\"}) { article { slug }"
                + " } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_favorite_an_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { favoriteArticle(slug: \"%s\") { article { slug } } }",
                article.getSlug()));

    assertThat(result.<String>read("data.favoriteArticle.article.slug"))
        .isEqualTo(article.getSlug());
    verify(articleFavoriteRepository)
        .save(eq(new ArticleFavorite(article.getId(), author.getId())));
  }

  @Test
  public void should_report_error_when_favoriting_an_unknown_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { favoriteArticle(slug: \"missing\") { article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_unfavorite_an_article() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), author.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(author.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }",
                article.getSlug()));

    assertThat(result.<String>read("data.unfavoriteArticle.article.slug"))
        .isEqualTo(article.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_ignore_unfavorite_when_there_is_no_favorite() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(author.getId())))
        .thenReturn(Optional.empty());
    when(articleQueryService.findById(eq(article.getId()), eq(author)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }",
                article.getSlug()));

    assertThat(result.<String>read("data.unfavoriteArticle.article.slug"))
        .isEqualTo(article.getSlug());
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_an_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug()));

    assertThat(result.<Boolean>read("data.deleteArticle.success")).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_reject_deleting_an_article_of_another_user() {
    authenticate(new User("other@test.com", "other", "123", "", ""));
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_reject_deleting_an_article_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { deleteArticle(slug: \"whatever\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_report_error_when_deleting_an_unknown_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { deleteArticle(slug: \"missing\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
