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
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleReportRepository;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.mapper.ArticleReportMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReportResolutionServiceTest {

  private ArticleReportRepository articleReportRepository;
  private ArticleReportMapper articleReportMapper;
  private ReportResolutionService service;

  private User admin;
  private User nonAdmin;

  @BeforeEach
  public void setUp() {
    articleReportRepository = Mockito.mock(ArticleReportRepository.class);
    articleReportMapper = Mockito.mock(ArticleReportMapper.class);
    service = new ReportResolutionService(articleReportRepository, articleReportMapper);

    admin = new User("admin@example.com", "admin", "pass", "", "", User.ROLE_ADMIN);
    nonAdmin = new User("user@example.com", "user", "pass", "", "", User.ROLE_USER);
  }

  @Test
  public void upheld_should_soft_delete_article_and_mark_report_upheld() {
    ArticleReport report = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    when(articleReportRepository.findById(eq(report.getId()))).thenReturn(Optional.of(report));

    ArticleReport resolved =
        service.resolve(report.getId(), new ResolveReportParam("UPHELD", "confirmed spam"), admin);

    assertEquals(ArticleReport.STATUS_UPHELD, resolved.getStatus());
    assertEquals(admin.getId(), resolved.getModeratorId());
    assertEquals("confirmed spam", resolved.getModeratorNote());
    assertNotNull(resolved.getResolvedAt());
    verify(articleReportRepository, times(1)).save(any(ArticleReport.class));
    verify(articleReportMapper, times(1)).softDeleteArticle(eq("article-1"));
  }

  @Test
  public void dismissed_should_not_soft_delete_article() {
    ArticleReport report = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    when(articleReportRepository.findById(eq(report.getId()))).thenReturn(Optional.of(report));

    ArticleReport resolved =
        service.resolve(report.getId(), new ResolveReportParam("DISMISSED", "no violation"), admin);

    assertEquals(ArticleReport.STATUS_DISMISSED, resolved.getStatus());
    assertEquals(admin.getId(), resolved.getModeratorId());
    verify(articleReportRepository, times(1)).save(any(ArticleReport.class));
    verify(articleReportMapper, never()).softDeleteArticle(any());
  }

  @Test
  public void should_reject_non_admin_actor() {
    ArticleReport report = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    when(articleReportRepository.findById(eq(report.getId()))).thenReturn(Optional.of(report));

    assertThrows(
        NoAuthorizationException.class,
        () -> service.resolve(report.getId(), new ResolveReportParam("UPHELD", null), nonAdmin));

    verify(articleReportRepository, never()).save(any(ArticleReport.class));
    verify(articleReportMapper, never()).softDeleteArticle(any());
  }

  @Test
  public void should_reject_missing_report() {
    when(articleReportRepository.findById(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.resolve("missing", new ResolveReportParam("UPHELD", null), admin));
  }

  @Test
  public void should_reject_resolving_already_resolved_report() {
    ArticleReport report = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    report.resolve(ArticleReport.STATUS_UPHELD, admin.getId(), "earlier note");
    when(articleReportRepository.findById(eq(report.getId()))).thenReturn(Optional.of(report));

    assertThrows(
        InvalidRequestException.class,
        () -> service.resolve(report.getId(), new ResolveReportParam("DISMISSED", null), admin));

    verify(articleReportMapper, never()).softDeleteArticle(any());
  }

  @Test
  public void should_reject_invalid_action() {
    ArticleReport report = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    when(articleReportRepository.findById(eq(report.getId()))).thenReturn(Optional.of(report));

    assertThrows(
        InvalidRequestException.class,
        () -> service.resolve(report.getId(), new ResolveReportParam("BOGUS", null), admin));
  }
}
