package io.spring.application.cache;

import static java.util.stream.Collectors.toList;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.ArticleChangedEvent;
import io.spring.core.user.UserChangedEvent;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Caches the user-independent part of {@link ArticleData} (article columns, tags and author
 * profile). Per-viewer fields such as {@code favorited}, {@code favoritesCount} and {@code
 * following} are never cached; callers receive a fresh copy they are free to mutate.
 */
@Component
public class ArticleDataCache {
  private final ArticleReadService articleReadService;
  private final GuardedCache<String, ArticleData> byId;
  private final GuardedCache<String, String> idBySlug;

  public ArticleDataCache(
      ArticleReadService articleReadService,
      @Value("${realworld.cache.enabled:true}") boolean enabled,
      @Value("${realworld.cache.ttl:10m}") Duration ttl,
      @Value("${realworld.cache.article-max-size:10000}") long maximumSize) {
    this.articleReadService = articleReadService;
    this.byId = new GuardedCache<>(enabled, maximumSize, ttl);
    this.idBySlug = new GuardedCache<>(enabled, maximumSize, ttl);
  }

  public Optional<ArticleData> findById(String id) {
    return Optional.ofNullable(byId.get(id, () -> articleReadService.findById(id)))
        .map(ArticleDataCache::copy);
  }

  public Optional<ArticleData> findBySlug(String slug) {
    String id = idBySlug.getIfPresent(slug);
    if (id != null) {
      ArticleData cached = byId.getIfPresent(id);
      if (cached != null && slug.equals(cached.getSlug())) {
        return Optional.of(copy(cached));
      }
    }
    long slugGeneration = idBySlug.generation();
    long idGeneration = byId.generation();
    ArticleData loaded = articleReadService.findBySlug(slug);
    if (loaded == null) {
      return Optional.empty();
    }
    byId.putIfUnchanged(idGeneration, loaded.getId(), loaded);
    idBySlug.putIfUnchanged(slugGeneration, slug, loaded.getId());
    return Optional.of(copy(loaded));
  }

  /** Same contract as {@link ArticleReadService#findArticles}: ordered by createdAt desc. */
  public List<ArticleData> findArticles(List<String> ids) {
    Map<String, ArticleData> found = new HashMap<>(byId.getAllPresent(ids));
    List<String> missing = ids.stream().filter(id -> !found.containsKey(id)).collect(toList());
    if (!missing.isEmpty()) {
      long loadGeneration = byId.generation();
      Map<String, ArticleData> loaded = new HashMap<>();
      articleReadService.findArticles(missing).forEach(a -> loaded.put(a.getId(), a));
      byId.putAllIfUnchanged(loadGeneration, loaded);
      found.putAll(loaded);
    }
    List<ArticleData> result = new ArrayList<>(found.size());
    for (ArticleData articleData : found.values()) {
      result.add(copy(articleData));
    }
    result.sort(Comparator.comparing(ArticleData::getCreatedAt).reversed());
    return result;
  }

  @EventListener
  public void onArticleChanged(ArticleChangedEvent event) {
    TransactionalEviction.evict(
        () -> {
          byId.invalidate(Collections.singleton(event.getArticleId()));
          idBySlug.invalidate(event.getSlugs());
        });
  }

  @EventListener
  public void onUserChanged(UserChangedEvent event) {
    TransactionalEviction.evict(
        () ->
            byId.invalidateIf(
                articleData ->
                    articleData.getProfileData() != null
                        && event.getUserId().equals(articleData.getProfileData().getId())));
  }

  private static ArticleData copy(ArticleData source) {
    ProfileData profile = source.getProfileData();
    return new ArticleData(
        source.getId(),
        source.getSlug(),
        source.getTitle(),
        source.getDescription(),
        source.getBody(),
        false,
        0,
        source.getCreatedAt(),
        source.getUpdatedAt(),
        source.getTagList() == null ? null : new ArrayList<>(source.getTagList()),
        profile == null
            ? null
            : new ProfileData(
                profile.getId(),
                profile.getUsername(),
                profile.getBio(),
                profile.getImage(),
                false));
  }
}
