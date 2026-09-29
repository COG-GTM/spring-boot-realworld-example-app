package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.core.article.Article;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

class DuplicatedArticleValidator
    implements ConstraintValidator<DuplicatedArticleConstraint, String> {

  @Autowired private ArticleQueryService articleQueryService;

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    String slug = Article.toSlug(value);
    return !articleQueryService.findBySlug(slug, null).isPresent()
        && !articleQueryService.isSlugUsedByDeletedArticle(slug);
  }
}
