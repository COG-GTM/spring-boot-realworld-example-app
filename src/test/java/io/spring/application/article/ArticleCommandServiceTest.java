package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @Test
  public void should_create_and_save_article_from_param() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    User creator = new User("john@example.com", "john", "123", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = service.createArticle(param, creator);

    assertThat(article.getTitle()).isEqualTo("a new title");
    assertThat(article.getSlug()).isEqualTo("a-new-title");
    assertThat(article.getDescription()).isEqualTo("desc");
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    assertThat(article.getTags())
        .extracting(Tag::getName)
        .containsExactlyInAnyOrder("java", "spring");

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(captor.capture());
    assertThat(captor.getValue()).isEqualTo(article);
  }

  @Test
  public void should_update_and_save_existing_article() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    DateTime createdAt = new DateTime(1000L);
    Article article =
        new Article(
            "old title", "old desc", "old body", Collections.emptyList(), "userId", createdAt);

    Article updated =
        service.updateArticle(article, new UpdateArticleParam("new title", "new body", "new desc"));

    assertThat(updated).isSameAs(article);
    assertThat(article.getTitle()).isEqualTo("new title");
    assertThat(article.getSlug()).isEqualTo("new-title");
    assertThat(article.getBody()).isEqualTo("new body");
    assertThat(article.getDescription()).isEqualTo("new desc");
    assertThat(article.getUpdatedAt()).isGreaterThan(createdAt);
    verify(articleRepository).save(article);
  }
}
