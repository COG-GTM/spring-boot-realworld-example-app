package io.spring.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.GolfStandingsQueryService;
import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfStandingData;
import io.spring.application.data.GolfStandingPickData;
import io.spring.application.data.GolfStandingsData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GolfStandingsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class GolfStandingsApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private GolfStandingsQueryService golfStandingsQueryService;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_standings_without_authentication() throws Exception {
    GolfStandingData leader =
        new GolfStandingData(
            1,
            "user-1",
            "alice",
            "",
            3600000L,
            2,
            Arrays.asList(
                new GolfStandingPickData("t-1", "Masters", "g-1", "Scottie Scheffler", 3600000L),
                new GolfStandingPickData("t-2", "Tour Championship", "g-2", "Rory McIlroy", null)));
    GolfStandingData noPicks =
        new GolfStandingData(2, "user-2", "bob", "", 0L, 0, new ArrayList<>());
    when(golfStandingsQueryService.findByContestId(eq("contest-1")))
        .thenReturn(
            Optional.of(
                new GolfStandingsData(
                    new GolfContestData("contest-1", "One-and-Done", "2026-27"),
                    Arrays.asList(leader, noPicks))));

    RestAssuredMockMvc.when()
        .get("/golf/contests/{contestId}/standings", "contest-1")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("contest.id", equalTo("contest-1"))
        .body("contest.name", equalTo("One-and-Done"))
        .body("contest.season", equalTo("2026-27"))
        .body("standings", hasSize(2))
        .body("standings[0].rank", equalTo(1))
        .body("standings[0].username", equalTo("alice"))
        .body("standings[0].totalPrizeMoney", equalTo(3600000))
        .body("standings[0].picksMade", equalTo(2))
        .body("standings[0].userId", nullValue())
        .body("standings[0].picks", hasSize(2))
        .body("standings[0].picks[0].tournamentName", equalTo("Masters"))
        .body("standings[0].picks[0].golferName", equalTo("Scottie Scheffler"))
        .body("standings[0].picks[0].prizeMoney", equalTo(3600000))
        .body("standings[0].picks[1].prizeMoney", nullValue())
        .body("standings[1].username", equalTo("bob"))
        .body("standings[1].totalPrizeMoney", equalTo(0))
        .body("standings[1].picks", hasSize(0));
  }

  @Test
  public void should_get_404_for_unknown_contest() throws Exception {
    when(golfStandingsQueryService.findByContestId(eq("missing"))).thenReturn(Optional.empty());

    RestAssuredMockMvc.when()
        .get("/golf/contests/{contestId}/standings", "missing")
        .then()
        .statusCode(404);
  }
}
