package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class,
  MyBatisArticleBookmarkRepository.class
})
public class ArticleQueryServiceTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "", "");
    userRepository.save(user);
    article =
        new Article(
            "test", "desc", "body", Arrays.asList("java", "spring"), user.getId(), new DateTime());
    articleRepository.save(article);
  }

  @Test
  public void should_fetch_article_success() {
    Optional<ArticleData> optional = queryService.findById(article.getId(), user);
    Assertions.assertTrue(optional.isPresent());

    ArticleData fetched = optional.get();
    Assertions.assertEquals(fetched.getFavoritesCount(), 0);
    Assertions.assertFalse(fetched.isFavorited());
    Assertions.assertFalse(fetched.isBookmarked());
    Assertions.assertNotNull(fetched.getCreatedAt());
    Assertions.assertNotNull(fetched.getUpdatedAt());
    Assertions.assertTrue(fetched.getTagList().contains("java"));
  }

  @Test
  public void should_get_bookmarked_state_for_single_article() {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId()));
    User anotherUser = new User("other@test.com", "other", "123", "", "");
    userRepository.save(anotherUser);

    Assertions.assertTrue(
        queryService
            .findById(article.getId(), user)
            .orElseThrow(AssertionError::new)
            .isBookmarked());
    Assertions.assertTrue(
        queryService
            .findBySlug(article.getSlug(), user)
            .orElseThrow(AssertionError::new)
            .isBookmarked());
    ArticleData otherUserArticle =
        queryService.findById(article.getId(), anotherUser).orElseThrow(AssertionError::new);
    Assertions.assertFalse(otherUserArticle.isBookmarked());
    ArticleData anonymousArticle =
        queryService.findById(article.getId(), null).orElseThrow(AssertionError::new);

    Assertions.assertFalse(anonymousArticle.isBookmarked());
  }

  @Test
  public void should_get_article_with_right_favorite_and_favorite_count() {
    User anotherUser = new User("other@test.com", "other", "123", "", "");
    userRepository.save(anotherUser);
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), anotherUser.getId()));

    Optional<ArticleData> optional = queryService.findById(article.getId(), anotherUser);
    Assertions.assertTrue(optional.isPresent());

    ArticleData articleData = optional.get();
    Assertions.assertEquals(articleData.getFavoritesCount(), 1);
    Assertions.assertTrue(articleData.isFavorited());
  }

  @Test
  public void should_get_default_article_list() {
    Article anotherArticle =
        new Article(
            "new article",
            "desc",
            "body",
            Arrays.asList("test"),
            user.getId(),
            new DateTime().minusHours(1));
    articleRepository.save(anotherArticle);

    ArticleDataList recentArticles =
        queryService.findRecentArticles(null, null, null, new Page(), user);
    Assertions.assertEquals(recentArticles.getCount(), 2);
    Assertions.assertEquals(recentArticles.getArticleDatas().size(), 2);
    Assertions.assertEquals(recentArticles.getArticleDatas().get(0).getId(), article.getId());

    ArticleDataList nodata =
        queryService.findRecentArticles(null, null, null, new Page(2, 10), user);
    Assertions.assertEquals(nodata.getCount(), 2);
    Assertions.assertEquals(nodata.getArticleDatas().size(), 0);
  }

  @Test
  public void should_get_default_article_list_by_cursor() {
    Article anotherArticle =
        new Article(
            "new article",
            "desc",
            "body",
            Arrays.asList("test"),
            user.getId(),
            new DateTime().minusHours(1));
    articleRepository.save(anotherArticle);

    CursorPager<ArticleData> recentArticles =
        queryService.findRecentArticlesWithCursor(
            null, null, null, new CursorPageParameter<>(null, 20, Direction.NEXT), user);
    Assertions.assertEquals(recentArticles.getData().size(), 2);
    Assertions.assertEquals(recentArticles.getData().get(0).getId(), article.getId());

    CursorPager<ArticleData> nodata =
        queryService.findRecentArticlesWithCursor(
            null,
            null,
            null,
            new CursorPageParameter<DateTime>(
                DateTimeCursor.parse(recentArticles.getEndCursor().toString()), 20, Direction.NEXT),
            user);
    Assertions.assertEquals(nodata.getData().size(), 0);
    Assertions.assertEquals(nodata.getStartCursor(), null);

    CursorPager<ArticleData> prevArticles =
        queryService.findRecentArticlesWithCursor(
            null, null, null, new CursorPageParameter<>(null, 20, Direction.PREV), user);
    Assertions.assertEquals(prevArticles.getData().size(), 2);
  }

  @Test
  public void should_query_article_by_author() {
    User anotherUser = new User("other@email.com", "other", "123", "", "");
    userRepository.save(anotherUser);

    Article anotherArticle =
        new Article("new article", "desc", "body", Arrays.asList("test"), anotherUser.getId());
    articleRepository.save(anotherArticle);

    ArticleDataList recentArticles =
        queryService.findRecentArticles(null, user.getUsername(), null, new Page(), user);
    Assertions.assertEquals(recentArticles.getArticleDatas().size(), 1);
    Assertions.assertEquals(recentArticles.getCount(), 1);
  }

  @Test
  public void should_query_article_by_favorite() {
    User anotherUser = new User("other@email.com", "other", "123", "", "");
    userRepository.save(anotherUser);

    Article anotherArticle =
        new Article("new article", "desc", "body", Arrays.asList("test"), anotherUser.getId());
    articleRepository.save(anotherArticle);

    ArticleFavorite articleFavorite = new ArticleFavorite(article.getId(), anotherUser.getId());
    articleFavoriteRepository.save(articleFavorite);

    ArticleDataList recentArticles =
        queryService.findRecentArticles(
            null, null, anotherUser.getUsername(), new Page(), anotherUser);
    Assertions.assertEquals(recentArticles.getArticleDatas().size(), 1);
    Assertions.assertEquals(recentArticles.getCount(), 1);
    ArticleData articleData = recentArticles.getArticleDatas().get(0);
    Assertions.assertEquals(articleData.getId(), article.getId());
    Assertions.assertEquals(articleData.getFavoritesCount(), 1);
    Assertions.assertTrue(articleData.isFavorited());
  }

  @Test
  public void should_query_article_by_tag() {
    Article anotherArticle =
        new Article("new article", "desc", "body", Arrays.asList("test"), user.getId());
    articleRepository.save(anotherArticle);

    ArticleDataList recentArticles =
        queryService.findRecentArticles("spring", null, null, new Page(), user);
    Assertions.assertEquals(recentArticles.getArticleDatas().size(), 1);
    Assertions.assertEquals(recentArticles.getCount(), 1);
    Assertions.assertEquals(recentArticles.getArticleDatas().get(0).getId(), article.getId());

    ArticleDataList notag = queryService.findRecentArticles("notag", null, null, new Page(), user);
    Assertions.assertEquals(notag.getCount(), 0);
  }

  @Test
  public void should_show_following_if_user_followed_author() {
    User anotherUser = new User("other@email.com", "other", "123", "", "");
    userRepository.save(anotherUser);

    FollowRelation followRelation = new FollowRelation(anotherUser.getId(), user.getId());
    userRepository.saveRelation(followRelation);

    ArticleDataList recentArticles =
        queryService.findRecentArticles(null, null, null, new Page(), anotherUser);
    Assertions.assertEquals(recentArticles.getCount(), 1);
    ArticleData articleData = recentArticles.getArticleDatas().get(0);
    Assertions.assertTrue(articleData.getProfileData().isFollowing());
  }

  @Test
  public void should_get_user_feed() {
    User anotherUser = new User("other@email.com", "other", "123", "", "");
    userRepository.save(anotherUser);

    FollowRelation followRelation = new FollowRelation(anotherUser.getId(), user.getId());
    userRepository.saveRelation(followRelation);

    ArticleDataList userFeed = queryService.findUserFeed(user, new Page());
    Assertions.assertEquals(userFeed.getCount(), 0);

    ArticleDataList anotherUserFeed = queryService.findUserFeed(anotherUser, new Page());
    Assertions.assertEquals(anotherUserFeed.getCount(), 1);
    ArticleData articleData = anotherUserFeed.getArticleDatas().get(0);
    Assertions.assertTrue(articleData.getProfileData().isFollowing());
  }

  @Test
  public void should_set_bookmarked_for_recent_articles_and_user_feed() {
    User author = new User("author@test.com", "author", "123", "", "");
    userRepository.save(author);
    Article feedArticle =
        new Article("feed article", "desc", "body", Arrays.asList("java"), author.getId());
    articleRepository.save(feedArticle);
    userRepository.saveRelation(new FollowRelation(user.getId(), author.getId()));
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId()));
    articleBookmarkRepository.save(new ArticleBookmark(feedArticle.getId(), user.getId()));

    ArticleDataList recentForViewer =
        queryService.findRecentArticles(null, null, null, new Page(), user);
    Assertions.assertTrue(
        recentForViewer.getArticleDatas().stream()
            .filter(data -> data.getId().equals(article.getId()))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .isBookmarked());
    ArticleDataList recentForOtherViewer =
        queryService.findRecentArticles(null, null, null, new Page(), author);
    Assertions.assertFalse(
        recentForOtherViewer.getArticleDatas().stream()
            .filter(data -> data.getId().equals(article.getId()))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .isBookmarked());
    ArticleDataList feed = queryService.findUserFeed(user, new Page());

    Assertions.assertTrue(feed.getArticleDatas().get(0).isBookmarked());
  }

  @Test
  public void should_list_bookmarked_articles_by_saved_time_and_insertion_order() {
    Article olderArticle = createArticle("older saved article", new DateTime(1000L));
    Article newerArticle = createArticle("newer saved article", new DateTime(3000L));
    Article middleArticle = createArticle("middle saved article", new DateTime(2000L));
    articleRepository.save(olderArticle);
    articleRepository.save(newerArticle);
    articleRepository.save(middleArticle);

    DateTime tiedSavedAt = new DateTime(1_700_000_000_000L);
    articleBookmarkRepository.save(
        new ArticleBookmark(newerArticle.getId(), user.getId(), tiedSavedAt));
    articleBookmarkRepository.save(
        new ArticleBookmark(olderArticle.getId(), user.getId(), tiedSavedAt));
    articleBookmarkRepository.save(
        new ArticleBookmark(middleArticle.getId(), user.getId(), tiedSavedAt.plusSeconds(1)));

    ArticleDataList bookmarked = queryService.findBookmarkedArticles(user, new Page());

    Assertions.assertEquals(
        Arrays.asList(middleArticle.getId(), olderArticle.getId(), newerArticle.getId()),
        bookmarked.getArticleDatas().stream()
            .map(ArticleData::getId)
            .collect(java.util.stream.Collectors.toList()));
    Assertions.assertEquals(3, bookmarked.getCount());
  }

  @Test
  public void should_paginate_and_count_bookmarked_articles() {
    Article first = createArticle("first saved", new DateTime(1000L));
    Article middle = createArticle("middle saved", new DateTime(2000L));
    Article last = createArticle("last saved", new DateTime(3000L));
    articleRepository.save(first);
    articleRepository.save(middle);
    articleRepository.save(last);
    articleBookmarkRepository.save(
        new ArticleBookmark(first.getId(), user.getId(), new DateTime(1_700_000_000_001L)));
    articleBookmarkRepository.save(
        new ArticleBookmark(middle.getId(), user.getId(), new DateTime(1_700_000_000_002L)));
    articleBookmarkRepository.save(
        new ArticleBookmark(last.getId(), user.getId(), new DateTime(1_700_000_000_003L)));

    ArticleDataList bookmarked = queryService.findBookmarkedArticles(user, new Page(1, 1));

    Assertions.assertEquals(3, bookmarked.getCount());
    Assertions.assertEquals(1, bookmarked.getArticleDatas().size());
    Assertions.assertEquals(middle.getId(), bookmarked.getArticleDatas().get(0).getId());
  }

  @Test
  public void should_isolate_bookmarks_between_users() {
    User anotherUser = new User("other@test.com", "other", "123", "", "");
    userRepository.save(anotherUser);
    Article anotherArticle = createArticle("another user's article", new DateTime(2000L));
    articleRepository.save(anotherArticle);
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId()));
    articleBookmarkRepository.save(
        new ArticleBookmark(anotherArticle.getId(), anotherUser.getId()));

    ArticleDataList userBookmarks = queryService.findBookmarkedArticles(user, new Page());
    ArticleDataList otherUserBookmarks =
        queryService.findBookmarkedArticles(anotherUser, new Page());

    Assertions.assertEquals(1, userBookmarks.getCount());
    Assertions.assertEquals(article.getId(), userBookmarks.getArticleDatas().get(0).getId());
    Assertions.assertEquals(1, otherUserBookmarks.getCount());
    Assertions.assertEquals(
        anotherArticle.getId(), otherUserBookmarks.getArticleDatas().get(0).getId());
  }

  @Test
  public void should_not_reorder_article_when_bookmark_is_saved_again() {
    Article first = createArticle("first", new DateTime(1000L));
    Article second = createArticle("second", new DateTime(2000L));
    articleRepository.save(first);
    articleRepository.save(second);
    DateTime firstSavedAt = new DateTime(1_700_000_000_001L);
    articleBookmarkRepository.save(new ArticleBookmark(first.getId(), user.getId(), firstSavedAt));
    articleBookmarkRepository.save(
        new ArticleBookmark(second.getId(), user.getId(), new DateTime(1_700_000_000_002L)));
    articleBookmarkRepository.save(
        new ArticleBookmark(first.getId(), user.getId(), new DateTime(1_700_000_000_003L)));

    ArticleDataList bookmarked = queryService.findBookmarkedArticles(user, new Page());

    Assertions.assertEquals(second.getId(), bookmarked.getArticleDatas().get(0).getId());
    Assertions.assertEquals(first.getId(), bookmarked.getArticleDatas().get(1).getId());
    Assertions.assertEquals(
        firstSavedAt,
        articleBookmarkRepository
            .find(first.getId(), user.getId())
            .orElseThrow(AssertionError::new)
            .getCreatedAt());
  }

  @Test
  public void should_remove_bookmarks_when_article_is_deleted() {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId()));
    Assertions.assertEquals(1, queryService.findBookmarkedArticles(user, new Page()).getCount());

    articleRepository.remove(article);

    ArticleDataList bookmarked = queryService.findBookmarkedArticles(user, new Page());
    Assertions.assertEquals(0, bookmarked.getCount());
    Assertions.assertTrue(bookmarked.getArticleDatas().isEmpty());
    Assertions.assertFalse(
        articleBookmarkRepository.find(article.getId(), user.getId()).isPresent());
  }

  private Article createArticle(String title, DateTime createdAt) {
    return new Article(title, "desc", "body", Arrays.asList("java"), user.getId(), createdAt);
  }
}
