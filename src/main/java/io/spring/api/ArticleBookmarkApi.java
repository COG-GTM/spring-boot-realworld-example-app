package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleBookmarksReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/articles")
@AllArgsConstructor
public class ArticleBookmarkApi {
  private ArticleBookmarkRepository articleBookmarkRepository;
  private ArticleRepository articleRepository;
  private ArticleQueryService articleQueryService;
  private ArticleBookmarksReadService articleBookmarksReadService;
  private ArticleReadService articleReadService;

  @PostMapping(path = "/{slug}/bookmark")
  public ResponseEntity createBookmark(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    ArticleBookmark bookmark = new ArticleBookmark(article.getId(), user.getId());
    articleBookmarkRepository.save(bookmark);
    return responseArticleData(articleQueryService.findById(article.getId(), user).get());
  }

  @DeleteMapping(path = "/{slug}/bookmark")
  public ResponseEntity removeBookmark(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    articleBookmarkRepository
        .find(article.getId(), user.getId())
        .ifPresent(
            bookmark -> {
              articleBookmarkRepository.remove(bookmark);
            });
    return responseArticleData(articleQueryService.findById(article.getId(), user).get());
  }

  @GetMapping(path = "/bookmarks")
  public ResponseEntity getBookmarks(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @AuthenticationPrincipal User user) {
    List<String> articleIds =
        articleBookmarksReadService.userBookmarkArticleIds(user.getId(), offset, limit);
    int count = articleBookmarksReadService.countUserBookmarks(user.getId());
    if (articleIds.size() == 0) {
      return ResponseEntity.ok(new ArticleDataList(new ArrayList<>(), 0));
    } else {
      List<ArticleData> articles = articleReadService.findArticles(articleIds);
      fillExtraInfo(articles, user);
      return ResponseEntity.ok(new ArticleDataList(articles, count));
    }
  }

  private void fillExtraInfo(List<ArticleData> articles, User user) {
    for (ArticleData article : articles) {
      ArticleData filled = articleQueryService.findById(article.getId(), user).orElse(article);
      article.setFavorited(filled.isFavorited());
      article.setFavoritesCount(filled.getFavoritesCount());
      article.setBookmarked(filled.isBookmarked());
      article.getProfileData().setFollowing(filled.getProfileData().isFollowing());
    }
  }

  private ResponseEntity<HashMap<String, Object>> responseArticleData(
      final ArticleData articleData) {
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("article", articleData);
          }
        });
  }
}
