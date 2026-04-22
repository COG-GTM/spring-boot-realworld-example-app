package io.spring.application.article;

import io.spring.application.Page;
import io.spring.application.data.ArticleReportData;
import io.spring.core.article.ArticleReport;
import io.spring.infrastructure.mybatis.readservice.ArticleReportReadService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ArticleReportQueryService {

  private final ArticleReportReadService articleReportReadService;

  public Optional<ArticleReportData> findById(String id) {
    return Optional.ofNullable(articleReportReadService.findById(id));
  }

  public ReportList listByStatus(String status, Page page) {
    String effectiveStatus =
        (status == null || status.isEmpty()) ? ArticleReport.STATUS_PENDING : status;
    List<ArticleReportData> reports = articleReportReadService.findByStatus(effectiveStatus, page);
    int count = articleReportReadService.countByStatus(effectiveStatus);
    return new ReportList(reports, count);
  }

  public static class ReportList {
    private final List<ArticleReportData> reports;
    private final int reportsCount;

    public ReportList(List<ArticleReportData> reports, int reportsCount) {
      this.reports = reports;
      this.reportsCount = reportsCount;
    }

    public List<ArticleReportData> getReports() {
      return reports;
    }

    public int getReportsCount() {
      return reportsCount;
    }
  }
}
