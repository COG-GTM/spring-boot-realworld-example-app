package io.spring.api;

import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
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
@Tag(name = "Articles", description = "Article management endpoints")
public class ArticlesApi {
  private ArticleCommandService articleCommandService;
  private ArticleQueryService articleQueryService;

  @Operation(
      summary = "Create an article",
      description = "Create a new article. Authentication required.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Article created successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "422", description = "Validation error")
      })
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

  @Operation(
      summary = "Get article feed",
      description = "Get articles from users you follow. Authentication required.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Feed retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(path = "feed")
  public ResponseEntity getFeed(
      @Parameter(description = "Number of articles to skip") @RequestParam(value = "offset", defaultValue = "0") int offset,
      @Parameter(description = "Maximum number of articles to return") @RequestParam(value = "limit", defaultValue = "20") int limit,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(articleQueryService.findUserFeed(user, new Page(offset, limit)));
  }

  @Operation(
      summary = "List articles",
      description = "Get recent articles globally. Supports filtering by tag, author, and favorited user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Articles retrieved successfully")
      })
  @GetMapping
  public ResponseEntity getArticles(
      @Parameter(description = "Number of articles to skip") @RequestParam(value = "offset", defaultValue = "0") int offset,
      @Parameter(description = "Maximum number of articles to return") @RequestParam(value = "limit", defaultValue = "20") int limit,
      @Parameter(description = "Filter by tag") @RequestParam(value = "tag", required = false) String tag,
      @Parameter(description = "Filter by favorited user") @RequestParam(value = "favorited", required = false) String favoritedBy,
      @Parameter(description = "Filter by author") @RequestParam(value = "author", required = false) String author,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        articleQueryService.findRecentArticles(
            tag, author, favoritedBy, new Page(offset, limit), user));
  }
}
