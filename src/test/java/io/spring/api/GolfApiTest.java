package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfPickData;
import io.spring.application.data.GolfTournamentData;
import io.spring.application.data.GolferData;
import io.spring.application.data.MyGolfPicksData;
import io.spring.application.data.TournamentStatus;
import io.spring.application.golf.GolfQueryService;
import io.spring.application.golf.PickCommandService;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.GolfContestRepository;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.GolferRepository;
import io.spring.core.golf.Pick;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
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

@WebMvcTest(GolfApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class GolfApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private GolfQueryService golfQueryService;
  @MockBean private PickCommandService pickCommandService;
  @MockBean private GolfContestRepository golfContestRepository;
  @MockBean private TournamentRepository tournamentRepository;
  @MockBean private GolferRepository golferRepository;

  private GolfContest contest;
  private Tournament tournament;
  private Golfer golfer;
  private GolferData golferData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    contest =
        new GolfContest("golf-contest-2026", "2026-27 One-and-Done", "2026-27", new DateTime());
    DateTime start = new DateTime().plusDays(30);
    tournament =
        new Tournament(
            "tourney-open",
            contest.getId(),
            "The Open",
            "Royal Birkdale",
            start,
            start.plusDays(4));
    golfer = new Golfer("golfer-scheffler", "Scottie Scheffler", "USA");
    golferData = new GolferData(golfer.getId(), golfer.getName(), golfer.getCountry());
    when(golfContestRepository.findById(eq(contest.getId()))).thenReturn(Optional.of(contest));
    when(tournamentRepository.findById(eq(tournament.getId()))).thenReturn(Optional.of(tournament));
    when(golferRepository.findById(eq(golfer.getId()))).thenReturn(Optional.of(golfer));
  }

  @Test
  public void should_list_contests_without_auth() {
    when(golfQueryService.allContests())
        .thenReturn(
            Collections.singletonList(
                new GolfContestData(
                    contest.getId(), contest.getName(), contest.getSeason(), null, null, null)));
    given()
        .when()
        .get("/golf/contests")
        .then()
        .statusCode(200)
        .body("contests[0].id", equalTo(contest.getId()))
        .body("contests[0].season", equalTo("2026-27"));
  }

  @Test
  public void should_get_contest_with_tournament_status() {
    GolfTournamentData t =
        new GolfTournamentData(
            tournament.getId(),
            contest.getId(),
            tournament.getName(),
            tournament.getCourse(),
            tournament.getStartsAt(),
            tournament.getEndsAt(),
            false,
            TournamentStatus.UPCOMING);
    when(golfQueryService.findContest(eq(contest.getId()), any()))
        .thenReturn(
            Optional.of(
                new GolfContestData(
                    contest.getId(),
                    contest.getName(),
                    contest.getSeason(),
                    contest.getCreatedAt(),
                    null,
                    Collections.singletonList(t))));
    given()
        .when()
        .get("/golf/contests/{id}", contest.getId())
        .then()
        .statusCode(200)
        .body("contest.tournaments[0].id", equalTo(tournament.getId()))
        .body("contest.tournaments[0].status", equalTo("upcoming"));
  }

  @Test
  public void should_404_for_unknown_contest() {
    given().when().get("/golf/contests/{id}", "nope").then().statusCode(404);
  }

  @Test
  public void should_list_golfers() {
    when(golfQueryService.allGolfers()).thenReturn(Collections.singletonList(golferData));
    given()
        .when()
        .get("/golf/golfers")
        .then()
        .statusCode(200)
        .body("golfers[0].name", equalTo("Scottie Scheffler"));
  }

  @Test
  public void should_join_contest() {
    when(golfQueryService.findContest(eq(contest.getId()), eq(user)))
        .thenReturn(
            Optional.of(
                new GolfContestData(
                    contest.getId(), contest.getName(), contest.getSeason(), null, true, null)));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/golf/contests/{id}/entries", contest.getId())
        .then()
        .statusCode(200)
        .body("contest.entered", equalTo(true));
    verify(pickCommandService).joinContest(eq(contest), eq(user));
  }

  @Test
  public void should_require_auth_to_join() {
    given().when().post("/golf/contests/{id}/entries", contest.getId()).then().statusCode(401);
  }

  @Test
  public void should_submit_pick() {
    Pick pick = new Pick(contest.getId(), tournament.getId(), user.getId(), golfer.getId());
    when(pickCommandService.submitPick(any())).thenReturn(pick);
    when(golfQueryService.findPick(eq(pick.getId())))
        .thenReturn(
            Optional.of(
                new GolfPickData(
                    pick.getId(),
                    contest.getId(),
                    tournament.getId(),
                    tournament.getName(),
                    tournament.getStartsAt(),
                    tournament.getEndsAt(),
                    false,
                    TournamentStatus.UPCOMING,
                    golferData,
                    null,
                    null,
                    pick.getCreatedAt())));
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(pickParam(tournament.getId(), golfer.getId()))
        .when()
        .post("/golf/contests/{id}/picks", contest.getId())
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("pick.tournamentId", equalTo(tournament.getId()))
        .body("pick.golfer.id", equalTo(golfer.getId()))
        .body("pick.tournamentStatus", equalTo("upcoming"));
  }

  @Test
  public void should_422_when_pick_fields_missing() {
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(pickParam(tournament.getId(), ""))
        .when()
        .post("/golf/contests/{id}/picks", contest.getId())
        .then()
        .statusCode(422)
        .body("errors.golferId[0]", equalTo("can't be empty"));
    verify(pickCommandService, never()).submitPick(any());
  }

  @Test
  public void should_404_for_unknown_tournament_or_golfer() {
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(pickParam("tourney-unknown", golfer.getId()))
        .when()
        .post("/golf/contests/{id}/picks", contest.getId())
        .then()
        .statusCode(404);
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(pickParam(tournament.getId(), "golfer-unknown"))
        .when()
        .post("/golf/contests/{id}/picks", contest.getId())
        .then()
        .statusCode(404);
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(pickParam(tournament.getId(), golfer.getId()))
        .when()
        .post("/golf/contests/{id}/picks", "contest-unknown")
        .then()
        .statusCode(404);
    verify(pickCommandService, never()).submitPick(any());
  }

  @Test
  public void should_require_auth_to_pick() {
    given()
        .contentType("application/json")
        .body(pickParam(tournament.getId(), golfer.getId()))
        .when()
        .post("/golf/contests/{id}/picks", contest.getId())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_my_picks_and_available_golfers() {
    GolferData mcilroy = new GolferData("golfer-mcilroy", "Rory McIlroy", "NIR");
    GolfPickData pick =
        new GolfPickData(
            "pick-1",
            contest.getId(),
            "tourney-masters",
            "The Masters",
            new DateTime().minusDays(40),
            new DateTime().minusDays(36),
            true,
            TournamentStatus.COMPLETED,
            golferData,
            1,
            3600000L,
            new DateTime().minusDays(41));
    when(golfQueryService.findMyPicks(eq(contest.getId()), eq(user)))
        .thenReturn(
            new MyGolfPicksData(
                contest.getId(), true, Arrays.asList(pick), Arrays.asList(mcilroy)));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/golf/contests/{id}/picks/me", contest.getId())
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("picks", hasSize(1))
        .body("picks[0].prizeMoney", equalTo(3600000))
        .body("picks[0].tournamentStatus", equalTo("completed"))
        .body("availableGolfers[0].id", equalTo("golfer-mcilroy"));
  }

  @Test
  public void should_require_auth_for_my_picks() {
    given().when().get("/golf/contests/{id}/picks/me", contest.getId()).then().statusCode(401);
  }

  private Map<String, Object> pickParam(String tournamentId, String golferId) {
    Map<String, Object> pick = new HashMap<>();
    pick.put("tournamentId", tournamentId);
    pick.put("golferId", golferId);
    Map<String, Object> body = new HashMap<>();
    body.put("pick", pick);
    return body;
  }
}
