package io.spring.application.article;

import io.spring.api.exception.InvalidRequestException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleReportRepository;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.HashMap;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class ArticleReportCommandService {

  private final ArticleRepository articleRepository;
  private final ArticleReportRepository articleReportRepository;

  public ArticleReport fileReport(String slug, @Valid NewArticleReportParam param, User reporter) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);

    if (!ArticleReport.isValidReason(param.getReason())) {
      throw invalid("reason", "invalid reason");
    }

    articleReportRepository
        .findPendingReport(article.getId(), reporter.getId())
        .ifPresent(
            existing -> {
              throw invalid("report", "you have already reported this article");
            });

    ArticleReport report =
        new ArticleReport(article.getId(), reporter.getId(), param.getReason(), param.getComment());
    articleReportRepository.save(report);
    return report;
  }

  private InvalidRequestException invalid(String field, String message) {
    Errors errors = new MapBindingResult(new HashMap<String, Object>(), "report");
    errors.rejectValue(field, "INVALID", message);
    return new InvalidRequestException(errors);
  }
}
