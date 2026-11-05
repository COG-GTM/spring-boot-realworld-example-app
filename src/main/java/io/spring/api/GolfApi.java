package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.data.GolfContestData;
import io.spring.application.data.MyGolfPicksData;
import io.spring.application.golf.GolfQueryService;
import io.spring.application.golf.NewPickParam;
import io.spring.application.golf.PickCommandService;
import io.spring.application.golf.SubmitPickCommand;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.GolfContestRepository;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.GolferRepository;
import io.spring.core.golf.Pick;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/golf")
@AllArgsConstructor
public class GolfApi {
  private GolfQueryService golfQueryService;
  private PickCommandService pickCommandService;
  private GolfContestRepository golfContestRepository;
  private TournamentRepository tournamentRepository;
  private GolferRepository golferRepository;

  @GetMapping(path = "contests")
  public ResponseEntity<?> contests() {
    return ResponseEntity.ok(response("contests", golfQueryService.allContests()));
  }

  @GetMapping(path = "contests/{contestId}")
  public ResponseEntity<?> contest(
      @PathVariable("contestId") String contestId, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(response("contest", findContestData(contestId, user)));
  }

  @GetMapping(path = "golfers")
  public ResponseEntity<?> golfers() {
    return ResponseEntity.ok(response("golfers", golfQueryService.allGolfers()));
  }

  @PostMapping(path = "contests/{contestId}/entries")
  public ResponseEntity<?> joinContest(
      @PathVariable("contestId") String contestId, @AuthenticationPrincipal User user) {
    GolfContest contest = findContest(contestId);
    pickCommandService.joinContest(contest, user);
    return ResponseEntity.ok(response("contest", findContestData(contestId, user)));
  }

  @PostMapping(path = "contests/{contestId}/picks")
  public ResponseEntity<?> submitPick(
      @PathVariable("contestId") String contestId,
      @Valid @RequestBody NewPickParam newPickParam,
      @AuthenticationPrincipal User user) {
    GolfContest contest = findContest(contestId);
    Tournament tournament =
        tournamentRepository
            .findById(newPickParam.getTournamentId())
            .orElseThrow(ResourceNotFoundException::new);
    Golfer golfer =
        golferRepository
            .findById(newPickParam.getGolferId())
            .orElseThrow(ResourceNotFoundException::new);
    Pick pick =
        pickCommandService.submitPick(new SubmitPickCommand(user, contest, tournament, golfer));
    return ResponseEntity.ok(response("pick", golfQueryService.findPick(pick.getId()).get()));
  }

  @GetMapping(path = "contests/{contestId}/picks/me")
  public ResponseEntity<MyGolfPicksData> myPicks(
      @PathVariable("contestId") String contestId, @AuthenticationPrincipal User user) {
    findContest(contestId);
    return ResponseEntity.ok(golfQueryService.findMyPicks(contestId, user));
  }

  private GolfContest findContest(String contestId) {
    return golfContestRepository.findById(contestId).orElseThrow(ResourceNotFoundException::new);
  }

  private GolfContestData findContestData(String contestId, User user) {
    return golfQueryService
        .findContest(contestId, user)
        .orElseThrow(ResourceNotFoundException::new);
  }

  private Map<String, Object> response(String key, Object value) {
    return new HashMap<String, Object>() {
      {
        put(key, value);
      }
    };
  }
}
