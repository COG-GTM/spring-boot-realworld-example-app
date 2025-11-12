package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleMutation.class})
public class ArticleMutationTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean ArticleCommandService articleCommandService;

  @MockBean ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean ArticleRepository articleRepository;

  @MockBean ArticleQueryService articleQueryService;

  private Article article;

  @BeforeEach
  @Override
  public void setUpUser() {
    super.setUpUser();
    article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1", "tag2"), user.getId());
  }

  @Test
  
  public void testCreateArticle() {
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    String mutation = "mutation { createArticle(input: { title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"tag1\", \"tag2\"] }) { article { slug title description body } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  
  public void testCreateArticleWithoutTags() {
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    String mutation = "mutation { createArticle(input: { title: \"Test Title\", description: \"Test Description\", body: \"Test Body\" }) { article { slug title } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  
  public void testUpdateArticle() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    String mutation = "mutation { updateArticle(slug: \"test-slug\", changes: { title: \"Updated Title\" }) { article { slug title } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(articleCommandService).updateArticle(any(), any());
  }

  @Test
  
  public void testUpdateArticleNotFound() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { updateArticle(slug: \"non-existent\", changes: { title: \"Updated Title\" }) { article { slug } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle");
    });
  }

  @Test
  
  public void testFavoriteArticle() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));

    String mutation = "mutation { favoriteArticle(slug: \"test-slug\") { article { slug favorited } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  
  public void testFavoriteArticleNotFound() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { favoriteArticle(slug: \"non-existent\") { article { slug } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle");
    });
  }

  @Test
  
  public void testUnfavoriteArticle() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), any())).thenReturn(Optional.of(favorite));

    String mutation = "mutation { unfavoriteArticle(slug: \"test-slug\") { article { slug favorited } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
  }

  @Test
  
  public void testDeleteArticle() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));

    String mutation = "mutation { deleteArticle(slug: \"test-slug\") { success } }";
    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");
    
    assertTrue(success);
    verify(articleRepository).remove(article);
  }

  @Test
  
  public void testDeleteArticleNotFound() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \"non-existent\") { success } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle");
    });
  }
}
