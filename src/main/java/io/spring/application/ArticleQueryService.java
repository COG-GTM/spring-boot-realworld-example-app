package io.spring.application;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ArticleQueryService {
  private ArticleReadService articleReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private ArticleDataEnricher articleDataEnricher;

  public Optional<ArticleData> findById(String id, User user) {
    ArticleData articleData = articleReadService.findById(id);
    if (articleData == null) {
      return Optional.empty();
    } else {
      if (user != null) {
        articleDataEnricher.enrichSingleArticle(id, user, articleData);
      }
      return Optional.of(articleData);
    }
  }

  public Optional<ArticleData> findBySlug(String slug, User user) {
    ArticleData articleData = articleReadService.findBySlug(slug);
    if (articleData == null) {
      return Optional.empty();
    } else {
      if (user != null) {
        articleDataEnricher.enrichSingleArticle(articleData.getId(), user, articleData);
      }
      return Optional.of(articleData);
    }
  }

  public CursorPager<ArticleData> findRecentArticlesWithCursor(
      String tag,
      String author,
      String favoritedBy,
      CursorPageParameter<DateTime> page,
      User currentUser) {
    List<String> articleIds =
        articleReadService.findArticlesWithCursor(tag, author, favoritedBy, page);
    if (articleIds.size() == 0) {
      return new CursorPager<>(new ArrayList<>(), page.getDirection(), false);
    } else {
      boolean hasExtra = articleIds.size() > page.getLimit();
      if (hasExtra) {
        articleIds.remove(page.getLimit());
      }
      if (!page.isNext()) {
        Collections.reverse(articleIds);
      }

      List<ArticleData> articles = articleReadService.findArticles(articleIds);
      articleDataEnricher.enrichArticles(articles, currentUser);

      return new CursorPager<>(articles, page.getDirection(), hasExtra);
    }
  }

  public CursorPager<ArticleData> findUserFeedWithCursor(
      User user, CursorPageParameter<DateTime> page) {
    List<String> followdUsers = userRelationshipQueryService.followedUsers(user.getId());
    if (followdUsers.size() == 0) {
      return new CursorPager<>(new ArrayList<>(), page.getDirection(), false);
    } else {
      List<ArticleData> articles =
          articleReadService.findArticlesOfAuthorsWithCursor(followdUsers, page);
      boolean hasExtra = articles.size() > page.getLimit();
      if (hasExtra) {
        articles.remove(page.getLimit());
      }
      if (!page.isNext()) {
        Collections.reverse(articles);
      }
      articleDataEnricher.enrichArticles(articles, user);
      return new CursorPager<>(articles, page.getDirection(), hasExtra);
    }
  }

  public ArticleDataList findRecentArticles(
      String tag, String author, String favoritedBy, Page page, User currentUser) {
    List<String> articleIds = articleReadService.queryArticles(tag, author, favoritedBy, page);
    int articleCount = articleReadService.countArticle(tag, author, favoritedBy);
    if (articleIds.size() == 0) {
      return new ArticleDataList(new ArrayList<>(), articleCount);
    } else {
      List<ArticleData> articles = articleReadService.findArticles(articleIds);
      articleDataEnricher.enrichArticles(articles, currentUser);
      return new ArticleDataList(articles, articleCount);
    }
  }

  public ArticleDataList findUserFeed(User user, Page page) {
    List<String> followdUsers = userRelationshipQueryService.followedUsers(user.getId());
    if (followdUsers.size() == 0) {
      return new ArticleDataList(new ArrayList<>(), 0);
    } else {
      List<ArticleData> articles = articleReadService.findArticlesOfAuthors(followdUsers, page);
      articleDataEnricher.enrichArticles(articles, user);
      int count = articleReadService.countFeedSize(followdUsers);
      return new ArticleDataList(articles, count);
    }
  }
}
