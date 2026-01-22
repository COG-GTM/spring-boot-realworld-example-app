package io.spring.application;

import static java.util.stream.Collectors.toList;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Helper component responsible for enriching ArticleData objects with additional
 * information such as favorite counts, favorite status, and author following status.
 * This separates the enrichment logic from the query service for better maintainability.
 */
@Component
@AllArgsConstructor
public class ArticleDataEnricher {
  private UserRelationshipQueryService userRelationshipQueryService;
  private ArticleFavoritesReadService articleFavoritesReadService;

  /**
   * Enriches a list of articles with favorite counts and user-specific data.
   *
   * @param articles the list of articles to enrich
   * @param currentUser the current user (may be null for anonymous requests)
   */
  public void enrichArticles(List<ArticleData> articles, User currentUser) {
    setFavoriteCount(articles);
    if (currentUser != null) {
      setIsFavorite(articles, currentUser);
      setIsFollowingAuthor(articles, currentUser);
    }
  }

  /**
   * Enriches a single article with user-specific data.
   *
   * @param id the article ID
   * @param user the current user
   * @param articleData the article data to enrich
   */
  public void enrichSingleArticle(String id, User user, ArticleData articleData) {
    articleData.setFavorited(articleFavoritesReadService.isUserFavorite(user.getId(), id));
    articleData.setFavoritesCount(articleFavoritesReadService.articleFavoriteCount(id));
    articleData
        .getProfileData()
        .setFollowing(
            userRelationshipQueryService.isUserFollowing(
                user.getId(), articleData.getProfileData().getId()));
  }

  /**
   * Sets the following status for article authors based on current user's relationships.
   */
  private void setIsFollowingAuthor(List<ArticleData> articles, User currentUser) {
    Set<String> followingAuthors =
        userRelationshipQueryService.followingAuthors(
            currentUser.getId(),
            articles.stream()
                .map(articleData -> articleData.getProfileData().getId())
                .collect(toList()));
    articles.forEach(
        articleData -> {
          if (followingAuthors.contains(articleData.getId())) {
            articleData.getProfileData().setFollowing(true);
          }
        });
  }

  /**
   * Sets the favorite count for each article in the list.
   */
  private void setFavoriteCount(List<ArticleData> articles) {
    List<ArticleFavoriteCount> favoritesCounts =
        articleFavoritesReadService.articlesFavoriteCount(
            articles.stream().map(ArticleData::getId).collect(toList()));
    Map<String, Integer> countMap = new HashMap<>();
    favoritesCounts.forEach(
        item -> {
          countMap.put(item.getId(), item.getCount());
        });
    articles.forEach(
        articleData -> articleData.setFavoritesCount(countMap.get(articleData.getId())));
  }

  /**
   * Sets the favorited status for each article based on current user's favorites.
   */
  private void setIsFavorite(List<ArticleData> articles, User currentUser) {
    Set<String> favoritedArticles =
        articleFavoritesReadService.userFavorites(
            articles.stream().map(articleData -> articleData.getId()).collect(toList()),
            currentUser);

    articles.forEach(
        articleData -> {
          if (favoritedArticles.contains(articleData.getId())) {
            articleData.setFavorited(true);
          }
        });
  }
}
