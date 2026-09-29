package io.spring.application.cache;

import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.core.article.ArticleChangedEvent;
import io.spring.core.favorite.ArticleFavoriteChangedEvent;
import io.spring.core.user.UserChangedEvent;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Caches the article id lists and counts behind the filtered article list queries. */
@Component
public class ArticleListCache {
  private final ArticleReadService articleReadService;
  private final GuardedCache<ListQuery, List<String>> ids;
  private final GuardedCache<ListQuery, Integer> counts;

  public ArticleListCache(
      ArticleReadService articleReadService,
      @Value("${realworld.cache.enabled:true}") boolean enabled,
      @Value("${realworld.cache.ttl:10m}") Duration ttl,
      @Value("${realworld.cache.list-max-size:10000}") long maximumSize) {
    this.articleReadService = articleReadService;
    this.ids = new GuardedCache<>(enabled, maximumSize, ttl);
    this.counts = new GuardedCache<>(enabled, maximumSize, ttl);
  }

  public List<String> queryArticles(String tag, String author, String favoritedBy, Page page) {
    ListQuery key =
        new ListQuery(tag, author, favoritedBy, page.getOffset(), page.getLimit(), null, null);
    return new ArrayList<>(
        ids.get(
            key,
            () -> List.copyOf(articleReadService.queryArticles(tag, author, favoritedBy, page))));
  }

  public int countArticle(String tag, String author, String favoritedBy) {
    ListQuery key = new ListQuery(tag, author, favoritedBy, 0, 0, null, null);
    return counts.get(key, () -> articleReadService.countArticle(tag, author, favoritedBy));
  }

  public List<String> findArticlesWithCursor(
      String tag, String author, String favoritedBy, CursorPageParameter<DateTime> page) {
    ListQuery key =
        new ListQuery(
            tag,
            author,
            favoritedBy,
            0,
            page.getQueryLimit(),
            page.getCursor(),
            page.getDirection());
    return new ArrayList<>(
        ids.get(
            key,
            () ->
                List.copyOf(
                    articleReadService.findArticlesWithCursor(tag, author, favoritedBy, page))));
  }

  @EventListener
  public void onArticleChanged(ArticleChangedEvent event) {
    TransactionalEviction.evict(
        () -> {
          ids.invalidateAll();
          counts.invalidateAll();
        });
  }

  @EventListener
  public void onFavoriteChanged(ArticleFavoriteChangedEvent event) {
    TransactionalEviction.evict(
        () -> {
          ids.invalidateKeysIf(ListQuery::filtersByFavorite);
          counts.invalidateKeysIf(ListQuery::filtersByFavorite);
        });
  }

  @EventListener
  public void onUserChanged(UserChangedEvent event) {
    TransactionalEviction.evict(
        () -> {
          ids.invalidateKeysIf(ListQuery::filtersByUsername);
          counts.invalidateKeysIf(ListQuery::filtersByUsername);
        });
  }

  @lombok.Value
  static class ListQuery {
    String tag;
    String author;
    String favoritedBy;
    int offset;
    int limit;
    DateTime cursor;
    Direction direction;

    boolean filtersByFavorite() {
      return favoritedBy != null;
    }

    boolean filtersByUsername() {
      return author != null || favoritedBy != null;
    }
  }
}
