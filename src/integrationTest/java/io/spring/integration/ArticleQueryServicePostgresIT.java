package io.spring.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Page;
import io.spring.application.TagsQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArticleQueryServicePostgresIT extends PostgresIntegrationTest {

  @Autowired private ArticleQueryService articleQueryService;
  @Autowired private TagsQueryService tagsQueryService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  private User author;
  private User reader;

  @BeforeEach
  void setUp() {
    author = new User("author@example.com", "author", "secret", "", "");
    reader = new User("reader@example.com", "reader", "secret", "", "");
    userRepository.save(author);
    userRepository.save(reader);
  }

  private Article saveArticle(String title, int hoursAgo, String... tags) {
    Article article =
        new Article(
            title,
            "desc",
            "body",
            Arrays.asList(tags),
            author.getId(),
            new DateTime().minusHours(hoursAgo));
    articleRepository.save(article);
    return article;
  }

  @Test
  void should_paginate_recent_articles_with_offset_and_limit() {
    for (int i = 0; i < 5; i++) {
      saveArticle("article " + i, i, "java");
    }

    ArticleDataList firstPage =
        articleQueryService.findRecentArticles(null, null, null, new Page(0, 2), reader);
    assertThat(firstPage.getCount()).isEqualTo(5);
    assertThat(titles(firstPage)).containsExactly("article 0", "article 1");

    ArticleDataList secondPage =
        articleQueryService.findRecentArticles(null, null, null, new Page(2, 2), reader);
    assertThat(titles(secondPage)).containsExactly("article 2", "article 3");

    ArticleDataList lastPage =
        articleQueryService.findRecentArticles(null, null, null, new Page(4, 2), reader);
    assertThat(titles(lastPage)).containsExactly("article 4");
  }

  @Test
  void should_filter_articles_by_tag_author_and_favorited_user() {
    Article java = saveArticle("java article", 1, "java", "spring");
    saveArticle("pg article", 2, "postgres");
    articleFavoriteRepository.save(new ArticleFavorite(java.getId(), reader.getId()));

    assertThat(
            titles(articleQueryService.findRecentArticles("spring", null, null, new Page(), null)))
        .containsExactly("java article");
    assertThat(
            titles(articleQueryService.findRecentArticles(null, "author", null, new Page(), null)))
        .containsExactly("java article", "pg article");
    assertThat(
            titles(articleQueryService.findRecentArticles(null, "reader", null, new Page(), null)))
        .isEmpty();

    ArticleDataList favorited =
        articleQueryService.findRecentArticles(null, null, "reader", new Page(), reader);
    assertThat(titles(favorited)).containsExactly("java article");
    ArticleData data = favorited.getArticleDatas().get(0);
    assertThat(data.isFavorited()).isTrue();
    assertThat(data.getFavoritesCount()).isEqualTo(1);
    assertThat(data.getTagList()).containsExactlyInAnyOrder("java", "spring");

    assertThat(tagsQueryService.allTags()).containsExactlyInAnyOrder("java", "spring", "postgres");
  }

  @Test
  void should_paginate_articles_with_cursor() {
    for (int i = 0; i < 3; i++) {
      saveArticle("article " + i, i);
    }

    CursorPager<ArticleData> first =
        articleQueryService.findRecentArticlesWithCursor(
            null, null, null, new CursorPageParameter<>(null, 2, Direction.NEXT), reader);
    assertThat(first.getData())
        .extracting(ArticleData::getTitle)
        .containsExactly("article 0", "article 1");
    assertThat(first.hasNext()).isTrue();

    CursorPager<ArticleData> second =
        articleQueryService.findRecentArticlesWithCursor(
            null,
            null,
            null,
            new CursorPageParameter<>(
                DateTimeCursor.parse(first.getEndCursor().toString()), 2, Direction.NEXT),
            reader);
    assertThat(second.getData()).extracting(ArticleData::getTitle).containsExactly("article 2");
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  void should_build_user_feed_from_followed_authors() {
    saveArticle("followed article", 1);
    userRepository.saveRelation(new FollowRelation(reader.getId(), author.getId()));

    ArticleDataList feed = articleQueryService.findUserFeed(reader, new Page(0, 10));
    assertThat(feed.getCount()).isEqualTo(1);
    assertThat(titles(feed)).containsExactly("followed article");
    assertThat(feed.getArticleDatas().get(0).getProfileData().isFollowing()).isTrue();

    assertThat(articleQueryService.findUserFeed(author, new Page()).getCount()).isZero();
  }

  private static List<String> titles(ArticleDataList list) {
    return list.getArticleDatas().stream().map(ArticleData::getTitle).collect(Collectors.toList());
  }
}
