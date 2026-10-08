package io.spring.core.bookmark;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ArticleBookmark {
  private String articleId;
  private String userId;
  @EqualsAndHashCode.Exclude private DateTime createdAt;

  public ArticleBookmark(String articleId, String userId) {
    this(articleId, userId, new DateTime());
  }

  public ArticleBookmark(String articleId, String userId, DateTime createdAt) {
    this.articleId = articleId;
    this.userId = userId;
    this.createdAt = createdAt;
  }
}
