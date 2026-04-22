package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.Page;
import io.spring.application.article.ArticleReportQueryService;
import io.spring.application.article.ArticleReportQueryService.ReportList;
import io.spring.application.article.ReportResolutionService;
import io.spring.application.article.ResolveReportParam;
import io.spring.application.data.ArticleReportData;
import io.spring.application.data.UserData;
import io.spring.core.article.ArticleReport;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
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

@WebMvcTest(AdminReportsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class AdminReportsApiTest extends TestWithCurrentUser {

  @MockBean private ArticleReportQueryService articleReportQueryService;
  @MockBean private ReportResolutionService reportResolutionService;

  @Autowired private MockMvc mvc;

  private ArticleReportData pendingReportData;

  private void adminFixture() {
    email = "admin@realworld.dev";
    username = "admin";
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    user = new User(email, username, "pass", "", defaultAvatar, User.ROLE_ADMIN);
    userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    token = "admin-token";
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
  }

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    super.setUp();
    pendingReportData =
        new ArticleReportData(
            "report-1",
            "how-to-train-your-dragon",
            "SPAM",
            "dropshipping ad",
            ArticleReport.STATUS_PENDING,
            new ArticleReportData.ReportActor("jane", null),
            null,
            null,
            new DateTime(),
            null);
  }

  @Test
  public void non_admin_should_be_forbidden() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/admin/reports")
        .then()
        .statusCode(403);
  }

  @Test
  public void unauthenticated_should_be_unauthorized() throws Exception {
    given().when().get("/admin/reports").then().statusCode(401);
  }

  @Test
  public void admin_should_list_pending_reports() throws Exception {
    adminFixture();
    when(articleReportQueryService.listByStatus(anyString(), any(Page.class)))
        .thenReturn(new ReportList(Arrays.asList(pendingReportData), 1));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/admin/reports?status=PENDING&limit=20&offset=0")
        .then()
        .statusCode(200)
        .body("reports", hasSize(1))
        .body("reports[0].id", equalTo("report-1"))
        .body("reports[0].articleSlug", equalTo("how-to-train-your-dragon"))
        .body("reports[0].status", equalTo("PENDING"))
        .body("reports[0].reporter.username", equalTo("jane"))
        .body("reportsCount", equalTo(1));
  }

  @Test
  public void admin_should_see_empty_list_when_no_reports() throws Exception {
    adminFixture();
    when(articleReportQueryService.listByStatus(anyString(), any(Page.class)))
        .thenReturn(new ReportList(Collections.emptyList(), 0));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/admin/reports")
        .then()
        .statusCode(200)
        .body("reports", hasSize(0))
        .body("reportsCount", equalTo(0));
  }

  @Test
  public void admin_should_resolve_upheld() throws Exception {
    adminFixture();
    ArticleReport resolved = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    resolved.resolve(ArticleReport.STATUS_UPHELD, user.getId(), "confirmed spam");

    ArticleReportData resolvedData =
        new ArticleReportData(
            resolved.getId(),
            "how-to-train-your-dragon",
            "SPAM",
            "bad",
            ArticleReport.STATUS_UPHELD,
            new ArticleReportData.ReportActor("jane", null),
            new ArticleReportData.ReportActor(user.getUsername(), user.getImage()),
            "confirmed spam",
            new DateTime(),
            new DateTime());

    when(reportResolutionService.resolve(
            eq(resolved.getId()), any(ResolveReportParam.class), eq(user)))
        .thenReturn(resolved);
    when(articleReportQueryService.findById(eq(resolved.getId())))
        .thenReturn(Optional.of(resolvedData));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(resolutionPayload("UPHELD", "confirmed spam"))
        .when()
        .post("/admin/reports/{id}/resolve", resolved.getId())
        .then()
        .statusCode(200)
        .body("report.status", equalTo("UPHELD"))
        .body("report.moderator.username", equalTo(user.getUsername()))
        .body("report.moderatorNote", equalTo("confirmed spam"));
  }

  @Test
  public void admin_should_resolve_dismissed() throws Exception {
    adminFixture();
    ArticleReport resolved = new ArticleReport("article-1", "reporter-1", "SPAM", "bad");
    resolved.resolve(ArticleReport.STATUS_DISMISSED, user.getId(), "no violation");

    ArticleReportData resolvedData =
        new ArticleReportData(
            resolved.getId(),
            "how-to-train-your-dragon",
            "SPAM",
            "bad",
            ArticleReport.STATUS_DISMISSED,
            new ArticleReportData.ReportActor("jane", null),
            new ArticleReportData.ReportActor(user.getUsername(), user.getImage()),
            "no violation",
            new DateTime(),
            new DateTime());

    when(reportResolutionService.resolve(
            eq(resolved.getId()), any(ResolveReportParam.class), eq(user)))
        .thenReturn(resolved);
    when(articleReportQueryService.findById(eq(resolved.getId())))
        .thenReturn(Optional.of(resolvedData));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(resolutionPayload("DISMISSED", "no violation"))
        .when()
        .post("/admin/reports/{id}/resolve", resolved.getId())
        .then()
        .statusCode(200)
        .body("report.status", equalTo("DISMISSED"))
        .body("report.moderator.username", equalTo(user.getUsername()));
  }

  private Map<String, Object> resolutionPayload(String action, String note) {
    Map<String, Object> resolution = new HashMap<>();
    resolution.put("action", action);
    resolution.put("note", note);
    Map<String, Object> body = new HashMap<>();
    body.put("resolution", resolution);
    return body;
  }
}
