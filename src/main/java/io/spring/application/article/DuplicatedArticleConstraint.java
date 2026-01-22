package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.core.article.Article;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import org.springframework.beans.factory.annotation.Autowired;

@Documented
@Constraint(validatedBy = DuplicatedArticleValidatorImpl.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DuplicatedArticleConstraint {
  String message() default "article with this title already exists";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

class DuplicatedArticleValidatorImpl
    implements ConstraintValidator<DuplicatedArticleConstraint, String> {

  @Autowired private ArticleQueryService articleQueryService;

  @Override
  public void initialize(DuplicatedArticleConstraint constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    return !articleQueryService.findBySlug(Article.toSlug(value), null).isPresent();
  }
}
