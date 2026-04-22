package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.Page;
import io.spring.application.article.ArticleReportQueryService;
import io.spring.application.article.ArticleReportQueryService.ReportList;
import io.spring.application.article.ReportResolutionService;
import io.spring.application.article.ResolveReportParam;
import io.spring.application.data.ArticleReportData;
import io.spring.core.article.ArticleReport;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/admin/reports")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportsApi {

  private final ArticleReportQueryService articleReportQueryService;
  private final ReportResolutionService reportResolutionService;

  @GetMapping
  public ResponseEntity<?> listReports(
      @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
      @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
    Page page = new Page(offset, limit);
    ReportList list = articleReportQueryService.listByStatus(status, page);
    Map<String, Object> body = new HashMap<>();
    body.put("reports", list.getReports());
    body.put("reportsCount", list.getReportsCount());
    return ResponseEntity.ok(body);
  }

  @PostMapping(path = "/{id}/resolve")
  public ResponseEntity<?> resolveReport(
      @PathVariable("id") String id,
      @AuthenticationPrincipal User admin,
      @Valid @RequestBody ResolveReportParam param) {
    ArticleReport resolved = reportResolutionService.resolve(id, param, admin);
    ArticleReportData data =
        articleReportQueryService
            .findById(resolved.getId())
            .orElseThrow(ResourceNotFoundException::new);
    Map<String, Object> body = new HashMap<>();
    body.put("report", data);
    return ResponseEntity.ok(body);
  }
}
