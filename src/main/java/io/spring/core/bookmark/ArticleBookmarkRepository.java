package io.spring.core.bookmark;

import java.util.List;
import java.util.Optional;

public interface ArticleBookmarkRepository {
  void save(ArticleBookmark bookmark);

  Optional<ArticleBookmark> find(String articleId, String userId);

  void remove(ArticleBookmark bookmark);

  List<String> findBookmarkedArticleIds(String userId);

  int countByUser(String userId);
}
