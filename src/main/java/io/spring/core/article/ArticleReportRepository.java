package io.spring.core.article;

import java.util.Optional;

public interface ArticleReportRepository {

  void save(ArticleReport report);

  Optional<ArticleReport> findById(String id);

  Optional<ArticleReport> findPendingReport(String articleId, String reporterId);
}
