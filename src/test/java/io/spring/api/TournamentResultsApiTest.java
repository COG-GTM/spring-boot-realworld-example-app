package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.data.GolferResultData;
import io.spring.application.data.TournamentResultsData;
import io.spring.application.golf.GolfAdminPolicy;
import io.spring.application.golf.RecordResultsCommand;
import io.spring.application.golf.TournamentResultsCommandService;
import io.spring.application.golf.TournamentResultsQueryService;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

@WebMvcTest({TournamentResultsApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class TournamentResultsApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private TournamentRepository tournamentRepository;
  @MockBean private TournamentResultsCommandService commandService;
  @MockBean private TournamentResultsQueryService queryService;
  @MockBean private GolfAdminPolicy golfAdminPolicy;

  private Tournament tournament;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    tournament =
        new Tournament(
            "tourney-api-test",
            "contest-api-test",
            "API Open",
            "Course",
            new DateTime().minusDays(4),
            new DateTime().minusDays(1));
    when(tournamentRepository.findById(eq(tournament.getId()))).thenReturn(Optional.of(tournament));
    when(queryService.findResults(eq(tournament)))
        .thenReturn(
            new TournamentResultsData(
                tournament.getId(),
                tournament.getName(),
                Arrays.asList(new GolferResultData("golfer-x", "Golfer X", 1, 3600000L))));
  }

  @Test
  public void should_record_results_as_admin() {
    when(golfAdminPolicy.isAdmin(eq(user))).thenReturn(true);
    when(commandService.recordResults(any(RecordResultsCommand.class))).thenReturn(3);

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body(result("golfer-x", 1, 3600000L)))
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(200)
        .body("picksUpdated", equalTo(3))
        .body("tournamentResults.tournamentId", equalTo(tournament.getId()))
        .body("tournamentResults.results[0].golferName", equalTo("Golfer X"))
        .body("tournamentResults.results[0].position", equalTo(1))
        .body("tournamentResults.results[0].prizeMoney", equalTo(3600000));
  }

  @Test
  public void should_require_authentication() {
    given()
        .contentType("application/json")
        .body(body(result("golfer-x", 1, 100L)))
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(401);
    verify(commandService, never()).recordResults(any());
  }

  @Test
  public void should_forbid_non_admin() {
    when(golfAdminPolicy.isAdmin(eq(user))).thenReturn(false);

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body(result("golfer-x", 1, 100L)))
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(403);
    verify(commandService, never()).recordResults(any());
  }

  @Test
  public void should_404_for_unknown_tournament() {
    when(golfAdminPolicy.isAdmin(eq(user))).thenReturn(true);
    when(tournamentRepository.findById(eq("nope"))).thenReturn(Optional.empty());

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body(result("golfer-x", 1, 100L)))
        .when()
        .put("/golf/tournaments/{id}/results", "nope")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_422_for_invalid_payload() {
    when(golfAdminPolicy.isAdmin(eq(user))).thenReturn(true);

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body(result("golfer-x", 0, -5L)))
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(422)
        .body("errors['results[0].position']", hasItem("must be 1 or greater"))
        .body("errors['results[0].prizeMoney']", hasItem("can't be negative"));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body())
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(422)
        .body("errors.results", hasItem("can't be empty"));
    verify(commandService, never()).recordResults(any());
  }

  @Test
  public void should_422_for_null_entry() {
    when(golfAdminPolicy.isAdmin(eq(user))).thenReturn(true);

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"tournamentResults\":{\"results\":[null]}}")
        .when()
        .put("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(422);
    verify(commandService, never()).recordResults(any());
  }

  @Test
  public void should_read_results_without_auth() {
    RestAssuredMockMvc.when()
        .get("/golf/tournaments/{id}/results", tournament.getId())
        .then()
        .statusCode(200)
        .body("tournamentResults.results[0].golferId", equalTo("golfer-x"));
  }

  private static Map<String, Object> result(String golferId, Integer position, long prizeMoney) {
    Map<String, Object> r = new HashMap<>();
    r.put("golferId", golferId);
    r.put("position", position);
    r.put("prizeMoney", prizeMoney);
    return r;
  }

  @SafeVarargs
  private static Map<String, Object> body(Map<String, Object>... results) {
    List<Map<String, Object>> list = new ArrayList<>(Arrays.asList(results));
    Map<String, Object> inner = new HashMap<>();
    inner.put("results", list);
    Map<String, Object> outer = new HashMap<>();
    outer.put("tournamentResults", inner);
    return outer;
  }
}
