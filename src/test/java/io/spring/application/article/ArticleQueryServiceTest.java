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
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
  MyBatisArticleFavoriteRepository.class
})
public class ArticleQueryServiceTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

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
    Assertions.assertNotNull(fetched.getCreatedAt());
    Assertions.assertNotNull(fetched.getUpdatedAt());
    Assertions.assertTrue(fetched.getTagList().contains("java"));
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
  public void should_page_user_feed_with_offset() {
    User follower = followerOfUser();
    List<Article> older = saveArticlesOfUser(3);

    ArticleDataList firstPage = queryService.findUserFeed(follower, new Page(0, 2));
    Assertions.assertEquals(4, firstPage.getCount());
    Assertions.assertEquals(
        Arrays.asList(article.getId(), older.get(0).getId()), ids(firstPage.getArticleDatas()));

    ArticleDataList lastPage = queryService.findUserFeed(follower, new Page(2, 2));
    Assertions.assertEquals(4, lastPage.getCount());
    Assertions.assertEquals(
        Arrays.asList(older.get(1).getId(), older.get(2).getId()), ids(lastPage.getArticleDatas()));
  }

  @Test
  public void should_page_user_feed_with_cursor() {
    User follower = followerOfUser();
    List<Article> older = saveArticlesOfUser(3);

    CursorPager<ArticleData> firstPage =
        queryService.findUserFeedWithCursor(
            follower, new CursorPageParameter<>(null, 2, Direction.NEXT));
    Assertions.assertEquals(Arrays.asList(article.getId(), older.get(0).getId()), ids(firstPage));
    Assertions.assertTrue(firstPage.hasNext());
    Assertions.assertEquals(2, firstPage.getData().get(0).getTagList().size());
    Assertions.assertTrue(firstPage.getData().get(0).getProfileData().isFollowing());

    CursorPager<ArticleData> secondPage =
        queryService.findUserFeedWithCursor(
            follower,
            new CursorPageParameter<>(
                DateTimeCursor.parse(firstPage.getEndCursor().toString()), 2, Direction.NEXT));
    Assertions.assertEquals(
        Arrays.asList(older.get(1).getId(), older.get(2).getId()), ids(secondPage));
    Assertions.assertFalse(secondPage.hasNext());

    CursorPager<ArticleData> backToFirst =
        queryService.findUserFeedWithCursor(
            follower,
            new CursorPageParameter<>(
                DateTimeCursor.parse(secondPage.getStartCursor().toString()), 2, Direction.PREV));
    Assertions.assertEquals(ids(firstPage), ids(backToFirst));
    Assertions.assertFalse(backToFirst.hasPrevious());
  }

  @Test
  public void should_return_empty_cursor_feed_without_followings() {
    CursorPager<ArticleData> feed =
        queryService.findUserFeedWithCursor(
            user, new CursorPageParameter<>(null, 20, Direction.NEXT));
    Assertions.assertTrue(feed.getData().isEmpty());
    Assertions.assertFalse(feed.hasNext());
    Assertions.assertNull(feed.getEndCursor());
  }

  private User followerOfUser() {
    User follower = new User("follower@email.com", "follower", "123", "", "");
    userRepository.save(follower);
    userRepository.saveRelation(new FollowRelation(follower.getId(), user.getId()));
    return follower;
  }

  private List<Article> saveArticlesOfUser(int count) {
    List<Article> articles = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Article older =
          new Article(
              "older " + i,
              "desc",
              "body",
              Arrays.asList("tag-a", "tag-b"),
              user.getId(),
              article.getCreatedAt().minusHours(i));
      articleRepository.save(older);
      articles.add(older);
    }
    return articles;
  }

  private static List<String> ids(CursorPager<ArticleData> pager) {
    return ids(pager.getData());
  }

  private static List<String> ids(List<ArticleData> articles) {
    return articles.stream().map(ArticleData::getId).collect(Collectors.toList());
  }
}
