package io.spring.api;

import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.core.user.User;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/user/bookmarks")
@AllArgsConstructor
public class UserBookmarksApi {
  private ArticleQueryService articleQueryService;

  @GetMapping
  public ResponseEntity getBookmarks(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(articleQueryService.findUserBookmarks(user, new Page(offset, limit)));
  }
}
