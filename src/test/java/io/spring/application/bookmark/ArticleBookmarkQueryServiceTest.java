package io.spring.application.bookmark;

import io.spring.application.ArticleQueryService;
import io.spring.application.BookmarkCursor;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.BookmarkedArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Covers docs/specs/article-bookmarks.md AC-7, AC-8, AC-9, AC-10, AC-15, AC-16 against a real
 * database.
 */
@Import({
  ArticleQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class,
  MyBatisArticleBookmarkRepository.class
})
public class ArticleBookmarkQueryServiceTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  private User reader;
  private User otherReader;
  private User author;
  private Article first;
  private Article second;
  private Article third;
  private DateTime base;

  @BeforeEach
  public void setUp() {
    reader = new User("reader@test.com", "reader", "123", "", "");
    otherReader = new User("other@test.com", "other", "123", "", "");
    author = new User("author@test.com", "author", "123", "", "");
    userRepository.save(reader);
    userRepository.save(otherReader);
    userRepository.save(author);

    base = new DateTime().minusHours(1).withMillisOfSecond(0);
    first = article("first", base);
    second = article("second", base.plusMinutes(1));
    third = article("third", base.plusMinutes(2));
  }

  private Article article(String title, DateTime createdAt) {
    Article article =
        new Article(title, "desc", "body", Arrays.asList("java"), author.getId(), createdAt);
    articleRepository.save(article);
    return article;
  }

  private void bookmark(Article article, User user, DateTime at) {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId(), at));
  }

  private static java.util.List<String> slugs(java.util.List<ArticleData> articles) {
    return articles.stream().map(ArticleData::getSlug).collect(Collectors.toList());
  }

  // AC-7
  @Test
  public void ac7_should_list_only_current_users_bookmarks_newest_bookmark_first() {
    // bookmark order deliberately differs from article creation order
    bookmark(second, reader, base.plusMinutes(10));
    bookmark(first, reader, base.plusMinutes(20));
    bookmark(third, otherReader, base.plusMinutes(30));

    ArticleDataList result = queryService.findUserBookmarks(reader, new Page(0, 20));
    Assertions.assertEquals(2, result.getCount());
    Assertions.assertEquals(
        Arrays.asList(first.getSlug(), second.getSlug()), slugs(result.getArticleDatas()));
    result.getArticleDatas().forEach(a -> Assertions.assertTrue(a.isBookmarked()));

    ArticleDataList empty = queryService.findUserBookmarks(author, new Page(0, 20));
    Assertions.assertEquals(0, empty.getCount());
    Assertions.assertTrue(empty.getArticleDatas().isEmpty());
  }

  // AC-8
  @Test
  public void ac8_should_paginate_bookmarks_with_limit_offset_and_total_count() {
    bookmark(first, reader, base.plusMinutes(10));
    bookmark(second, reader, base.plusMinutes(20));
    bookmark(third, reader, base.plusMinutes(30));

    ArticleDataList page1 = queryService.findUserBookmarks(reader, new Page(0, 2));
    Assertions.assertEquals(3, page1.getCount());
    Assertions.assertEquals(
        Arrays.asList(third.getSlug(), second.getSlug()), slugs(page1.getArticleDatas()));

    ArticleDataList page2 = queryService.findUserBookmarks(reader, new Page(2, 2));
    Assertions.assertEquals(3, page2.getCount());
    Assertions.assertEquals(Arrays.asList(first.getSlug()), slugs(page2.getArticleDatas()));
  }

  // AC-9
  @Test
  public void ac9_bookmarked_flag_is_true_only_for_the_bookmarking_user() {
    bookmark(first, reader, base.plusMinutes(10));

    Assertions.assertTrue(queryService.findById(first.getId(), reader).get().isBookmarked());
    Assertions.assertTrue(queryService.findBySlug(first.getSlug(), reader).get().isBookmarked());
    Assertions.assertFalse(queryService.findById(first.getId(), otherReader).get().isBookmarked());
    Assertions.assertFalse(queryService.findById(first.getId(), null).get().isBookmarked());
    Assertions.assertFalse(queryService.findById(second.getId(), reader).get().isBookmarked());

    ArticleDataList recent =
        queryService.findRecentArticles(null, null, null, new Page(0, 10), reader);
    recent
        .getArticleDatas()
        .forEach(
            a ->
                Assertions.assertEquals(
                    a.getId().equals(first.getId()), a.isBookmarked(), a.getSlug()));
    queryService
        .findRecentArticles(null, null, null, new Page(0, 10), otherReader)
        .getArticleDatas()
        .forEach(a -> Assertions.assertFalse(a.isBookmarked()));
  }

  // AC-10
  @Test
  public void ac10_bookmarks_and_favorites_are_independent() {
    bookmark(first, reader, base.plusMinutes(10));
    ArticleData bookmarkedOnly = queryService.findById(first.getId(), reader).get();
    Assertions.assertTrue(bookmarkedOnly.isBookmarked());
    Assertions.assertFalse(bookmarkedOnly.isFavorited());
    Assertions.assertEquals(0, bookmarkedOnly.getFavoritesCount());

    articleFavoriteRepository.save(new ArticleFavorite(second.getId(), reader.getId()));
    ArticleData favoritedOnly = queryService.findById(second.getId(), reader).get();
    Assertions.assertTrue(favoritedOnly.isFavorited());
    Assertions.assertFalse(favoritedOnly.isBookmarked());
    Assertions.assertEquals(
        Arrays.asList(first.getSlug()),
        slugs(queryService.findUserBookmarks(reader, new Page(0, 20)).getArticleDatas()));
  }

  // AC-15
  @Test
  public void ac15_should_page_bookmarks_by_bookmark_time_cursor() {
    bookmark(first, reader, base.plusMinutes(10));
    bookmark(second, reader, base.plusMinutes(20));
    bookmark(third, reader, base.plusMinutes(30));

    CursorPager<BookmarkedArticleData> page1 =
        queryService.findUserBookmarksWithCursor(
            reader, new CursorPageParameter<>(null, 2, Direction.NEXT));
    Assertions.assertEquals(2, page1.getData().size());
    Assertions.assertEquals(third.getSlug(), page1.getData().get(0).getArticle().getSlug());
    Assertions.assertEquals(second.getSlug(), page1.getData().get(1).getArticle().getSlug());
    Assertions.assertTrue(page1.hasNext());
    Assertions.assertTrue(page1.getData().get(0).getArticle().isBookmarked());

    BookmarkCursor.Position cursor = page1.getData().get(1).getCursor().getData();
    CursorPager<BookmarkedArticleData> page2 =
        queryService.findUserBookmarksWithCursor(
            reader, new CursorPageParameter<>(cursor, 2, Direction.NEXT));
    Assertions.assertEquals(1, page2.getData().size());
    Assertions.assertEquals(first.getSlug(), page2.getData().get(0).getArticle().getSlug());
    Assertions.assertFalse(page2.hasNext());
  }

  // AC-15
  @Test
  public void ac15_cursor_pages_through_bookmarks_sharing_the_same_timestamp() {
    DateTime sameTime = base.plusMinutes(10);
    bookmark(first, reader, sameTime);
    bookmark(second, reader, sameTime);
    bookmark(third, reader, sameTime);

    java.util.Set<String> seen = new java.util.HashSet<>();
    BookmarkCursor.Position cursor = null;
    int pages = 0;
    boolean hasNext = true;
    while (hasNext) {
      CursorPager<BookmarkedArticleData> page =
          queryService.findUserBookmarksWithCursor(
              reader, new CursorPageParameter<>(cursor, 2, Direction.NEXT));
      page.getData().forEach(b -> Assertions.assertTrue(seen.add(b.getArticle().getSlug())));
      hasNext = page.hasNext();
      if (!page.getData().isEmpty()) {
        cursor = page.getData().get(page.getData().size() - 1).getCursor().getData();
      }
      Assertions.assertTrue(++pages <= 3, "pagination did not terminate");
    }
    Assertions.assertEquals(
        new java.util.HashSet<>(Arrays.asList(first.getSlug(), second.getSlug(), third.getSlug())),
        seen);
  }

  // AC-16
  @Test
  public void ac16_deleted_articles_are_excluded_from_bookmark_lists() {
    bookmark(first, reader, base.plusMinutes(10));
    bookmark(second, reader, base.plusMinutes(20));
    articleRepository.remove(second);

    ArticleDataList result = queryService.findUserBookmarks(reader, new Page(0, 20));
    Assertions.assertEquals(1, result.getCount());
    Assertions.assertEquals(Arrays.asList(first.getSlug()), slugs(result.getArticleDatas()));

    CursorPager<BookmarkedArticleData> cursorPage =
        queryService.findUserBookmarksWithCursor(
            reader, new CursorPageParameter<>(null, 20, Direction.NEXT));
    Assertions.assertEquals(1, cursorPage.getData().size());
    Assertions.assertEquals(first.getSlug(), cursorPage.getData().get(0).getArticle().getSlug());
  }
}
