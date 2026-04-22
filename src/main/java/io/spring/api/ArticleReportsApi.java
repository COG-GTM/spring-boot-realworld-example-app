package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleReportCommandService;
import io.spring.application.article.ArticleReportQueryService;
import io.spring.application.article.NewArticleReportParam;
import io.spring.application.data.ArticleReportData;
import io.spring.core.article.ArticleReport;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/articles/{slug}/reports")
@AllArgsConstructor
public class ArticleReportsApi {

  private final ArticleReportCommandService articleReportCommandService;
  private final ArticleReportQueryService articleReportQueryService;

  @PostMapping
  public ResponseEntity<?> fileReport(
      @PathVariable("slug") String slug,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody NewArticleReportParam param) {
    ArticleReport created = articleReportCommandService.fileReport(slug, param, user);
    ArticleReportData data =
        articleReportQueryService
            .findById(created.getId())
            .orElseThrow(ResourceNotFoundException::new);
    Map<String, Object> body = new HashMap<>();
    body.put("report", data);
    return ResponseEntity.status(201).body(body);
  }
}
