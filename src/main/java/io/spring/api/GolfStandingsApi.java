package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.GolfStandingsQueryService;
import io.spring.application.data.GolfStandingsData;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/golf/contests/{contestId}/standings")
@AllArgsConstructor
public class GolfStandingsApi {
  private GolfStandingsQueryService golfStandingsQueryService;

  @GetMapping
  public ResponseEntity getStandings(@PathVariable("contestId") String contestId) {
    GolfStandingsData data =
        golfStandingsQueryService
            .findByContestId(contestId)
            .orElseThrow(ResourceNotFoundException::new);
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("contest", data.getContest());
            put("standings", data.getStandings());
          }
        });
  }
}
