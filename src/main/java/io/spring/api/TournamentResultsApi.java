package io.spring.api;

import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.data.TournamentResultsData;
import io.spring.application.golf.GolfAdminPolicy;
import io.spring.application.golf.RecordResultsCommand;
import io.spring.application.golf.TournamentResultsCommandService;
import io.spring.application.golf.TournamentResultsParam;
import io.spring.application.golf.TournamentResultsQueryService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/golf/tournaments/{tournamentId}/results")
@AllArgsConstructor
public class TournamentResultsApi {
  private TournamentRepository tournamentRepository;
  private TournamentResultsCommandService tournamentResultsCommandService;
  private TournamentResultsQueryService tournamentResultsQueryService;
  private GolfAdminPolicy golfAdminPolicy;

  @GetMapping
  public ResponseEntity<?> results(@PathVariable("tournamentId") String tournamentId) {
    Tournament tournament = findTournament(tournamentId);
    return ResponseEntity.ok(response(tournamentResultsQueryService.findResults(tournament)));
  }

  @PutMapping
  public ResponseEntity<?> recordResults(
      @PathVariable("tournamentId") String tournamentId,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody TournamentResultsParam param) {
    if (!golfAdminPolicy.isAdmin(user)) {
      throw new NoAuthorizationException();
    }
    Tournament tournament = findTournament(tournamentId);
    int picksUpdated =
        tournamentResultsCommandService.recordResults(new RecordResultsCommand(tournament, param));
    Map<String, Object> body = response(tournamentResultsQueryService.findResults(tournament));
    body.put("picksUpdated", picksUpdated);
    return ResponseEntity.ok(body);
  }

  private Tournament findTournament(String tournamentId) {
    return tournamentRepository.findById(tournamentId).orElseThrow(ResourceNotFoundException::new);
  }

  private Map<String, Object> response(TournamentResultsData data) {
    Map<String, Object> body = new HashMap<>();
    body.put("tournamentResults", data);
    return body;
  }
}
