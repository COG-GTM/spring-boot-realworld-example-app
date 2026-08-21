package io.spring.api;

import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import io.spring.infrastructure.service.PendoTrackService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  private PendoTrackService pendoTrackService;

  @PostMapping
  public ResponseEntity createArticle(
      @Valid @RequestBody NewArticleParam newArticleParam, @AuthenticationPrincipal User user) {
    Article article = articleCommandService.createArticle(newArticleParam, user);

    List<String> tagNames =
        article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    Map<String, Object> trackProps = new HashMap<>();
    trackProps.put("articleId", article.getId());
    trackProps.put("slug", article.getSlug());
    trackProps.put("title", article.getTitle());
    trackProps.put("tagList", String.join(",", tagNames));
    trackProps.put("tagCount", tagNames.size());
    trackProps.put("bodyLength", article.getBody() != null ? article.getBody().length() : 0);
    trackProps.put("userId", user.getId());
    pendoTrackService.track("article_created", user.getId(), trackProps);

    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("article", articleQueryService.findById(article.getId(), user).get());
          }
        });
  }

  @GetMapping(path = "feed")
  public ResponseEntity getFeed(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(articleQueryService.findUserFeed(user, new Page(offset, limit)));
  }

  @GetMapping
  public ResponseEntity getArticles(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "tag", required = false) String tag,
      @RequestParam(value = "favorited", required = false) String favoritedBy,
      @RequestParam(value = "author", required = false) String author,
      @AuthenticationPrincipal User user) {
    ArticleDataList result =
        articleQueryService.findRecentArticles(
            tag, author, favoritedBy, new Page(offset, limit), user);

    boolean hasFilters =
        (tag != null && !tag.isEmpty())
            || (author != null && !author.isEmpty())
            || (favoritedBy != null && !favoritedBy.isEmpty());
    if (hasFilters) {
      Map<String, Object> trackProps = new HashMap<>();
      trackProps.put("tag", tag != null ? tag : "");
      trackProps.put("author", author != null ? author : "");
      trackProps.put("favoritedBy", favoritedBy != null ? favoritedBy : "");
      trackProps.put("offset", offset);
      trackProps.put("limit", limit);
      trackProps.put("resultsCount", result.getCount());
      String visitorId = user != null ? user.getId() : "anonymous";
      pendoTrackService.track("articles_filtered", visitorId, trackProps);
    }

    return ResponseEntity.ok(result);
  }
}
