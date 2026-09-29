package io.spring.core.article;

import java.util.Set;
import lombok.Value;

@Value
public class ArticleChangedEvent {
  String articleId;
  Set<String> slugs;
}
