package io.spring.api;

import io.spring.api.exception.InvalidRequestException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ArticleCursorDataList;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Locale;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/articles")
@AllArgsConstructor
public class ArticlesApi {
  private ArticleCommandService articleCommandService;
  private ArticleQueryService articleQueryService;

  @PostMapping
  public ResponseEntity createArticle(
      @Valid @RequestBody NewArticleParam newArticleParam, @AuthenticationPrincipal User user) {
    Article article = articleCommandService.createArticle(newArticleParam, user);
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("article", articleQueryService.findById(article.getId(), user).get());
          }
        });
  }

  @GetMapping(path = "feed")
  public ResponseEntity getFeed(
      @RequestParam(value = "offset", required = false) Integer offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "direction", required = false) String direction,
      @AuthenticationPrincipal User user) {
    if (cursor == null && direction == null) {
      return ResponseEntity.ok(
          articleQueryService.findUserFeed(user, new Page(offset == null ? 0 : offset, limit)));
    }
    if (offset != null) {
      throw invalidFeedParam("offset", "can't be combined with cursor or direction");
    }
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(parseCursor(cursor), limit, parseDirection(direction));
    return ResponseEntity.ok(
        new ArticleCursorDataList(articleQueryService.findUserFeedWithCursor(user, page)));
  }

  @GetMapping
  public ResponseEntity getArticles(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "tag", required = false) String tag,
      @RequestParam(value = "favorited", required = false) String favoritedBy,
      @RequestParam(value = "author", required = false) String author,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        articleQueryService.findRecentArticles(
            tag, author, favoritedBy, new Page(offset, limit), user));
  }

  private static DateTime parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      return DateTimeCursor.parse(cursor.trim());
    } catch (NumberFormatException e) {
      throw invalidFeedParam("cursor", "is not a valid cursor");
    }
  }

  private static Direction parseDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return Direction.NEXT;
    }
    try {
      return Direction.valueOf(direction.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw invalidFeedParam("direction", "must be 'next' or 'prev'");
    }
  }

  private static InvalidRequestException invalidFeedParam(String field, String message) {
    MapBindingResult errors = new MapBindingResult(new HashMap<>(), "feed");
    errors.rejectValue(field, "INVALID", message);
    return new InvalidRequestException(errors);
  }
}
