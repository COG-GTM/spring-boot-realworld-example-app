package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the pick flow end to end against the Flyway-migrated, seeded golf demo data. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GolfPickFlowIntegrationTest {
  private static final String CONTEST = "golf-contest-2026";
  private static final String UPCOMING = "tourney-open-2027";
  private static final String OTHER_UPCOMING = "tourney-tourchamp-2027";
  private static final String COMPLETED = "tourney-masters-2026";

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtService jwtService;
  @Autowired private PickRepository pickRepository;

  private String demoToken;

  @DynamicPropertySource
  static void freshDatabase(DynamicPropertyRegistry registry) throws IOException {
    File db = File.createTempFile("golf-pick-flow", ".db");
    db.deleteOnExit();
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + db.getAbsolutePath());
  }

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    demoToken = jwtService.toToken(userRepository.findById("user-golfdemo1").get());
  }

  @Test
  public void should_show_contest_with_tournament_statuses() {
    given()
        .when()
        .get("/golf/contests/{id}", CONTEST)
        .then()
        .statusCode(200)
        .body("contest.tournaments", hasSize(6))
        .body(
            "contest.tournaments.find { it.id == '" + COMPLETED + "' }.status",
            equalTo("completed"))
        .body(
            "contest.tournaments.find { it.id == '" + UPCOMING + "' }.status", equalTo("upcoming"));
  }

  @Test
  public void should_reject_golfer_already_used_this_season() {
    pick(demoToken, UPCOMING, "golfer-scheffler")
        .then()
        .statusCode(422)
        .body(
            "errors.golferId[0]",
            equalTo(
                "golfer already used in Masters Tournament; each golfer can be picked once a"
                    + " season"));
  }

  @Test
  public void should_reject_started_tournament() {
    pick(demoToken, COMPLETED, "golfer-rahm")
        .then()
        .statusCode(422)
        .body(
            "errors.tournamentId[0]", equalTo("tournament has already started, picks are locked"));
  }

  @Test
  public void should_pick_swap_and_free_old_golfer() {
    pick(demoToken, UPCOMING, "golfer-rahm")
        .then()
        .statusCode(200)
        .body("pick.tournamentId", equalTo(UPCOMING))
        .body("pick.golfer.id", equalTo("golfer-rahm"))
        .body("pick.prizeMoney", nullValue());

    pick(demoToken, OTHER_UPCOMING, "golfer-rahm").then().statusCode(422);

    pick(demoToken, UPCOMING, "golfer-aberg")
        .then()
        .statusCode(200)
        .body("pick.golfer.id", equalTo("golfer-aberg"));

    pick(demoToken, OTHER_UPCOMING, "golfer-rahm").then().statusCode(200);

    given()
        .header("Authorization", "Token " + demoToken)
        .when()
        .get("/golf/contests/{id}/picks/me", CONTEST)
        .then()
        .statusCode(200)
        .body("picks", hasSize(6))
        .body("picks.find { it.tournamentId == '" + COMPLETED + "' }.prizeMoney", equalTo(3600000))
        .body(
            "picks.find { it.tournamentId == '" + UPCOMING + "' }.golfer.id",
            equalTo("golfer-aberg"))
        .body("availableGolfers", hasSize(6))
        .body("availableGolfers.id", not(hasItem("golfer-aberg")))
        .body("availableGolfers.id", not(hasItem("golfer-scheffler")))
        .body("availableGolfers.id", everyItem(not(equalTo("golfer-rahm"))));
  }

  @Test
  public void should_require_entry_before_picking_and_join_idempotently() {
    User newcomer = new User("newcomer@example.com", "newcomer", "x", "", "");
    userRepository.save(newcomer);
    String token = jwtService.toToken(newcomer);

    pick(token, UPCOMING, "golfer-scheffler")
        .then()
        .statusCode(422)
        .body("errors.contestId[0]", equalTo("you must join this contest before making picks"));

    for (int i = 0; i < 2; i++) {
      given()
          .header("Authorization", "Token " + token)
          .when()
          .post("/golf/contests/{id}/entries", CONTEST)
          .then()
          .statusCode(200)
          .body("contest.entered", equalTo(true));
    }

    pick(token, UPCOMING, "golfer-scheffler").then().statusCode(200);
  }

  @Test
  public void should_surface_unique_key_conflicts_as_data_integrity_violations() {
    Pick duplicate = new Pick(CONTEST, COMPLETED, "user-golfdemo1", "golfer-rahm");
    assertThrows(DataIntegrityViolationException.class, () -> pickRepository.save(duplicate));
  }

  @Test
  public void should_404_for_unknown_ids() {
    pick(demoToken, "tourney-nope", "golfer-rahm").then().statusCode(404);
    pick(demoToken, UPCOMING, "golfer-nope").then().statusCode(404);
    given()
        .header("Authorization", "Token " + demoToken)
        .when()
        .get("/golf/contests/{id}/picks/me", "contest-nope")
        .then()
        .statusCode(404);
  }

  private io.restassured.module.mockmvc.response.MockMvcResponse pick(
      String token, String tournamentId, String golferId) {
    Map<String, Object> pick = new HashMap<>();
    pick.put("tournamentId", tournamentId);
    pick.put("golferId", golferId);
    Map<String, Object> body = new HashMap<>();
    body.put("pick", pick);
    return given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(body)
        .when()
        .post("/golf/contests/{id}/picks", CONTEST);
  }
}
