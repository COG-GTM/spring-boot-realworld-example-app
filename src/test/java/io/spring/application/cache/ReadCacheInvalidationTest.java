package io.spring.application.cache;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.application.TagsQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.mapper.ArticleMapper;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleQueryService.class,
  ArticleDataCache.class,
  ArticleListCache.class,
  TagsQueryService.class,
  TagListCache.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class
})
public class ReadCacheInvalidationTest extends DbTestBase {
  @Autowired private ArticleQueryService articleQueryService;
  @Autowired private TagsQueryService tagsQueryService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  @Autowired private ArticleMapper articleMapper;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("cache@test.com", "cacheuser", "123", "bio", "image");
    userRepository.save(user);
    article = new Article("cached title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);
  }

  @Test
  public void should_serve_article_from_cache_until_it_is_written_through_repository() {
    Assertions.assertEquals(
        "desc", articleQueryService.findBySlug(article.getSlug(), null).get().getDescription());

    article.update(null, "changed behind the cache", null);
    articleMapper.update(article);
    Assertions.assertEquals(
        "desc", articleQueryService.findBySlug(article.getSlug(), null).get().getDescription());
    Assertions.assertEquals(
        "desc", articleQueryService.findById(article.getId(), null).get().getDescription());

    article.update(null, "updated", null);
    articleRepository.save(article);
    Assertions.assertEquals(
        "updated", articleQueryService.findBySlug(article.getSlug(), null).get().getDescription());
    Assertions.assertEquals(
        "updated", articleQueryService.findById(article.getId(), null).get().getDescription());
  }

  @Test
  public void should_evict_old_slug_when_title_changes() {
    String oldSlug = article.getSlug();
    Assertions.assertTrue(articleQueryService.findBySlug(oldSlug, null).isPresent());

    article.update("brand new title", null, null);
    articleRepository.save(article);

    Assertions.assertFalse(articleQueryService.findBySlug(oldSlug, null).isPresent());
    ArticleData renamed = articleQueryService.findBySlug(article.getSlug(), null).get();
    Assertions.assertEquals("brand new title", renamed.getTitle());
  }

  @Test
  public void should_evict_article_on_delete() {
    Assertions.assertTrue(articleQueryService.findBySlug(article.getSlug(), null).isPresent());
    Assertions.assertTrue(articleQueryService.findById(article.getId(), null).isPresent());

    articleRepository.remove(article);

    Assertions.assertFalse(articleQueryService.findBySlug(article.getSlug(), null).isPresent());
    Assertions.assertFalse(articleQueryService.findById(article.getId(), null).isPresent());
  }

  @Test
  public void should_refresh_author_profile_when_user_is_updated() {
    Assertions.assertEquals(
        "bio",
        articleQueryService.findBySlug(article.getSlug(), null).get().getProfileData().getBio());
    Assertions.assertEquals(
        "bio",
        articleQueryService
            .findRecentArticles(null, null, null, new Page(), null)
            .getArticleDatas()
            .get(0)
            .getProfileData()
            .getBio());

    user.update(null, "renamed", null, "new bio", null);
    userRepository.save(user);

    ArticleData articleData = articleQueryService.findBySlug(article.getSlug(), null).get();
    Assertions.assertEquals("renamed", articleData.getProfileData().getUsername());
    Assertions.assertEquals("new bio", articleData.getProfileData().getBio());
    Assertions.assertEquals(
        "new bio",
        articleQueryService
            .findRecentArticles(null, null, null, new Page(), null)
            .getArticleDatas()
            .get(0)
            .getProfileData()
            .getBio());
  }

  @Test
  public void should_not_leak_viewer_specific_fields_between_callers() {
    ArticleData first = articleQueryService.findBySlug(article.getSlug(), user).get();
    first.setFavorited(true);
    first.getProfileData().setFollowing(true);
    first.getTagList().add("mutated");

    ArticleData second = articleQueryService.findBySlug(article.getSlug(), null).get();
    Assertions.assertFalse(second.isFavorited());
    Assertions.assertFalse(second.getProfileData().isFollowing());
    Assertions.assertEquals(Arrays.asList("java"), second.getTagList());
  }

  @Test
  public void should_list_articles_from_cache_in_created_at_order() {
    Article older =
        new Article(
            "older",
            "desc",
            "body",
            Arrays.asList("go"),
            user.getId(),
            new DateTime().minusDays(1));
    articleRepository.save(older);
    articleQueryService.findById(older.getId(), null);

    List<ArticleData> articles =
        articleQueryService
            .findRecentArticles(null, null, null, new Page(), null)
            .getArticleDatas();
    Assertions.assertEquals(2, articles.size());
    Assertions.assertEquals(article.getId(), articles.get(0).getId());
    Assertions.assertEquals(older.getId(), articles.get(1).getId());
  }

  @Test
  public void should_invalidate_tag_list_when_article_is_created() {
    Assertions.assertEquals(Arrays.asList("java"), tagsQueryService.allTags());

    articleRepository.save(
        new Article("another", "desc", "body", Arrays.asList("kotlin"), user.getId()));

    List<String> tags = tagsQueryService.allTags();
    Assertions.assertTrue(tags.contains("java"));
    Assertions.assertTrue(tags.contains("kotlin"));
  }

  @Test
  public void should_include_new_articles_in_cached_list_and_count() {
    Assertions.assertEquals(
        1, articleQueryService.findRecentArticles(null, null, null, new Page(), null).getCount());
    Assertions.assertEquals(
        1, articleQueryService.findRecentArticles("java", null, null, new Page(), null).getCount());

    Article newer = new Article("newer", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(newer);

    List<ArticleData> articles =
        articleQueryService
            .findRecentArticles("java", null, null, new Page(), null)
            .getArticleDatas();
    Assertions.assertEquals(2, articles.size());
    Assertions.assertEquals(
        2, articleQueryService.findRecentArticles(null, null, null, new Page(), null).getCount());

    articleRepository.remove(newer);
    Assertions.assertEquals(
        1, articleQueryService.findRecentArticles("java", null, null, new Page(), null).getCount());
  }

  @Test
  public void should_invalidate_favorited_by_list_when_favorites_change() {
    Assertions.assertEquals(
        0,
        articleQueryService
            .findRecentArticles(null, null, user.getUsername(), new Page(), null)
            .getCount());

    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    articleFavoriteRepository.save(favorite);
    Assertions.assertEquals(
        1,
        articleQueryService
            .findRecentArticles(null, null, user.getUsername(), new Page(), null)
            .getCount());

    articleFavoriteRepository.remove(favorite);
    Assertions.assertEquals(
        0,
        articleQueryService
            .findRecentArticles(null, null, user.getUsername(), new Page(), null)
            .getCount());
  }

  @Test
  public void should_invalidate_author_list_when_username_changes() {
    Assertions.assertEquals(
        1,
        articleQueryService
            .findRecentArticles(null, "cacheuser", null, new Page(), null)
            .getCount());

    user.update(null, "renamed", null, null, null);
    userRepository.save(user);

    Assertions.assertEquals(
        0,
        articleQueryService
            .findRecentArticles(null, "cacheuser", null, new Page(), null)
            .getCount());
    Assertions.assertEquals(
        1,
        articleQueryService.findRecentArticles(null, "renamed", null, new Page(), null).getCount());
  }

  @Test
  public void should_not_share_mutable_id_lists_between_cursor_callers() {
    Article older =
        new Article(
            "older",
            "desc",
            "body",
            Arrays.asList("go"),
            user.getId(),
            new DateTime().minusDays(1));
    articleRepository.save(older);
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 1, Direction.NEXT);

    for (int i = 0; i < 2; i++) {
      CursorPager<ArticleData> pager =
          articleQueryService.findRecentArticlesWithCursor(null, null, null, page, null);
      Assertions.assertEquals(1, pager.getData().size());
      Assertions.assertEquals(article.getId(), pager.getData().get(0).getId());
      Assertions.assertTrue(pager.hasNext());
    }
  }
}
