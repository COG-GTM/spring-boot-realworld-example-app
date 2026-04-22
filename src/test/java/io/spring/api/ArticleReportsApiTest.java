package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.article.ArticleReportCommandService;
import io.spring.application.article.ArticleReportQueryService;
import io.spring.application.article.NewArticleReportParam;
import io.spring.application.data.ArticleReportData;
import io.spring.core.article.ArticleReport;
import io.spring.core.article.ArticleRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleReportsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleReportsApiTest extends TestWithCurrentUser {

  @MockBean private ArticleReportCommandService articleReportCommandService;
  @MockBean private ArticleReportQueryService articleReportQueryService;
  @MockBean private ArticleRepository articleRepository;

  @Autowired private MockMvc mvc;

  private ArticleReport report;
  private ArticleReportData reportData;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    super.setUp();
    report = new ArticleReport("article-1", user.getId(), "SPAM", "dropshipping ad");
    reportData =
        new ArticleReportData(
            report.getId(),
            "how-to-train-your-dragon",
            "SPAM",
            "dropshipping ad",
            ArticleReport.STATUS_PENDING,
            new ArticleReportData.ReportActor(user.getUsername(), user.getImage()),
            null,
            null,
            new DateTime(),
            null);
  }

  private Map<String, Object> reportPayload(String reason, String comment) {
    Map<String, Object> report = new HashMap<>();
    report.put("reason", reason);
    report.put("comment", comment);
    Map<String, Object> body = new HashMap<>();
    body.put("report", report);
    return body;
  }

  @Test
  public void should_file_report_success() throws Exception {
    when(articleReportCommandService.fileReport(
            eq("how-to-train-your-dragon"), any(NewArticleReportParam.class), eq(user)))
        .thenReturn(report);
    when(articleReportQueryService.findById(eq(report.getId())))
        .thenReturn(Optional.of(reportData));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(reportPayload("SPAM", "dropshipping ad"))
        .when()
        .post("/articles/{slug}/reports", "how-to-train-your-dragon")
        .then()
        .statusCode(201)
        .body("report.id", equalTo(report.getId()))
        .body("report.reason", equalTo("SPAM"))
        .body("report.status", equalTo("PENDING"))
        .body("report.reporter.username", equalTo(user.getUsername()));
  }

  @Test
  public void should_return_422_on_duplicate_report() throws Exception {
    when(articleReportCommandService.fileReport(
            eq("how-to-train-your-dragon"), any(NewArticleReportParam.class), eq(user)))
        .thenThrow(new io.spring.api.exception.InvalidRequestException(duplicateErrors()));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(reportPayload("SPAM", "dropshipping ad"))
        .when()
        .post("/articles/{slug}/reports", "how-to-train-your-dragon")
        .then()
        .statusCode(422)
        .body("errors.report[0]", equalTo("you have already reported this article"));
  }

  private org.springframework.validation.Errors duplicateErrors() {
    org.springframework.validation.Errors errors =
        new org.springframework.validation.MapBindingResult(new HashMap<>(), "report");
    errors.rejectValue("report", "INVALID", "you have already reported this article");
    return errors;
  }

  @Test
  public void should_return_401_when_unauthenticated() throws Exception {
    given()
        .contentType("application/json")
        .body(reportPayload("SPAM", "dropshipping ad"))
        .when()
        .post("/articles/{slug}/reports", "how-to-train-your-dragon")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_return_422_on_missing_reason() throws Exception {
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(reportPayload("", null))
        .when()
        .post("/articles/{slug}/reports", "how-to-train-your-dragon")
        .then()
        .statusCode(422)
        .body("errors.reason[0]", equalTo("can't be empty"));
  }
}
