package io.spring.core.favorite;

import lombok.Value;

@Value
public class ArticleFavoriteChangedEvent {
  String articleId;
  String userId;
}
