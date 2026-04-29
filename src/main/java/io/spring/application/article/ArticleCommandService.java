package io.spring.application.article;

import datadog.trace.api.Trace;
import io.micrometer.core.annotation.Timed;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.observability.ArticleMetrics;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class ArticleCommandService {

  private ArticleRepository articleRepository;
  private ArticleMetrics articleMetrics;

  @Trace(operationName = "article.create", resourceName = "ArticleCommandService.createArticle")
  @Timed(value = "article.create.duration", description = "Time to create an article")
  public Article createArticle(@Valid NewArticleParam newArticleParam, User creator) {
    Article article =
        new Article(
            newArticleParam.getTitle(),
            newArticleParam.getDescription(),
            newArticleParam.getBody(),
            newArticleParam.getTagList(),
            creator.getId());
    articleRepository.save(article);
    articleMetrics.recordCreated();
    return article;
  }

  @Trace(operationName = "article.update", resourceName = "ArticleCommandService.updateArticle")
  @Timed(value = "article.update.duration", description = "Time to update an article")
  public Article updateArticle(Article article, @Valid UpdateArticleParam updateArticleParam) {
    article.update(
        updateArticleParam.getTitle(),
        updateArticleParam.getDescription(),
        updateArticleParam.getBody());
    articleRepository.save(article);
    articleMetrics.recordUpdated();
    return article;
  }
}
