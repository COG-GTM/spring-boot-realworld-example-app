package io.spring.infrastructure.repository;

import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleReportRepository;
import io.spring.infrastructure.mybatis.mapper.ArticleReportMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisArticleReportRepository implements ArticleReportRepository {
  private final ArticleReportMapper articleReportMapper;

  public MyBatisArticleReportRepository(ArticleReportMapper articleReportMapper) {
    this.articleReportMapper = articleReportMapper;
  }

  @Override
  @Transactional
  public void save(ArticleReport report) {
    if (articleReportMapper.findById(report.getId()) == null) {
      articleReportMapper.insert(report);
    } else {
      articleReportMapper.update(report);
    }
  }

  @Override
  public Optional<ArticleReport> findById(String id) {
    return Optional.ofNullable(articleReportMapper.findById(id));
  }

  @Override
  public Optional<ArticleReport> findPendingReport(String articleId, String reporterId) {
    return Optional.ofNullable(
        articleReportMapper.findPendingByArticleAndReporter(articleId, reporterId));
  }
}
