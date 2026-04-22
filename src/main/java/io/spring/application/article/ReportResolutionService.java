package io.spring.application.article;

import io.spring.api.exception.InvalidRequestException;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleReportRepository;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.mapper.ArticleReportMapper;
import java.util.HashMap;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class ReportResolutionService {

  private final ArticleReportRepository articleReportRepository;
  private final ArticleReportMapper articleReportMapper;

  @Transactional
  public ArticleReport resolve(String reportId, @Valid ResolveReportParam param, User moderator) {
    if (!moderator.isAdmin()) {
      throw new NoAuthorizationException();
    }

    ArticleReport report =
        articleReportRepository.findById(reportId).orElseThrow(ResourceNotFoundException::new);

    if (report.isResolved()) {
      throw invalid("resolution", "report already resolved");
    }

    if (!ArticleReport.isValidResolutionAction(param.getAction())) {
      throw invalid("action", "invalid action");
    }

    report.resolve(param.getAction(), moderator.getId(), param.getNote());
    articleReportRepository.save(report);

    if (ArticleReport.STATUS_UPHELD.equals(param.getAction())) {
      articleReportMapper.softDeleteArticle(report.getArticleId());
    }

    return report;
  }

  private InvalidRequestException invalid(String field, String message) {
    Errors errors = new MapBindingResult(new HashMap<String, Object>(), "resolution");
    errors.rejectValue(field, "INVALID", message);
    return new InvalidRequestException(errors);
  }
}
