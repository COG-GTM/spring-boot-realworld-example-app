package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.api.exception.InvalidRequestException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleReportRepository;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ArticleReportServiceTest {

  private ArticleRepository articleRepository;
  private ArticleReportRepository articleReportRepository;
  private ArticleReportCommandService service;

  private User reporter;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleRepository = Mockito.mock(ArticleRepository.class);
    articleReportRepository = Mockito.mock(ArticleReportRepository.class);
    service = new ArticleReportCommandService(articleRepository, articleReportRepository);

    reporter = new User("jane@example.com", "jane", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), "author-id");
  }

  @Test
  public void should_file_report_for_existing_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleReportRepository.findPendingReport(eq(article.getId()), eq(reporter.getId())))
        .thenReturn(Optional.empty());

    ArticleReport report =
        service.fileReport(
            article.getSlug(), new NewArticleReportParam("SPAM", "dropshipping ad"), reporter);

    assertNotNull(report.getId());
    assertEquals(article.getId(), report.getArticleId());
    assertEquals(reporter.getId(), report.getReporterId());
    assertEquals(ArticleReport.STATUS_PENDING, report.getStatus());
    assertEquals("SPAM", report.getReason());
    verify(articleReportRepository, times(1)).save(any(ArticleReport.class));
  }

  @Test
  public void should_reject_duplicate_pending_report() {
    ArticleReport existing =
        new ArticleReport(article.getId(), reporter.getId(), "SPAM", "earlier complaint");
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleReportRepository.findPendingReport(eq(article.getId()), eq(reporter.getId())))
        .thenReturn(Optional.of(existing));

    assertThrows(
        InvalidRequestException.class,
        () ->
            service.fileReport(
                article.getSlug(), new NewArticleReportParam("HARASSMENT", "still bad"), reporter));

    verify(articleReportRepository, never()).save(any(ArticleReport.class));
  }

  @Test
  public void should_return_not_found_when_article_missing() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.fileReport("missing", new NewArticleReportParam("SPAM", null), reporter));
  }

  @Test
  public void should_reject_invalid_reason() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        InvalidRequestException.class,
        () ->
            service.fileReport(
                article.getSlug(), new NewArticleReportParam("NOT_A_REASON", null), reporter));

    verify(articleReportRepository, never()).save(any(ArticleReport.class));
  }
}
