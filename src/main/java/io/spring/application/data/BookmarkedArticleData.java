package io.spring.application.data;

import io.spring.application.BookmarkCursor;
import io.spring.application.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.DateTime;

@Getter
@AllArgsConstructor
public class BookmarkedArticleData implements Node {
  private ArticleData article;
  private DateTime bookmarkedAt;

  @Override
  public BookmarkCursor getCursor() {
    return new BookmarkCursor(bookmarkedAt, article.getId());
  }
}
