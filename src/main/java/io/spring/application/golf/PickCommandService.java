package io.spring.application.golf;

import io.spring.core.golf.ContestEntry;
import io.spring.core.golf.ContestEntryRepository;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.user.User;
import java.util.Optional;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class PickCommandService {
  private ContestEntryRepository contestEntryRepository;
  private PickRepository pickRepository;

  public void joinContest(GolfContest contest, User user) {
    if (!contestEntryRepository.exists(contest.getId(), user.getId())) {
      contestEntryRepository.save(new ContestEntry(contest.getId(), user.getId()));
    }
  }

  @Transactional
  public Pick submitPick(@Valid SubmitPickCommand command) {
    String contestId = command.getContest().getId();
    String userId = command.getUser().getId();
    String tournamentId = command.getTournament().getId();
    String golferId = command.getGolfer().getId();

    Optional<Pick> existing =
        pickRepository.findByContestUserAndTournament(contestId, userId, tournamentId);
    Pick pick;
    if (existing.isPresent()) {
      pick = existing.get();
      pick.changeGolfer(golferId);
    } else {
      pick = new Pick(contestId, tournamentId, userId, golferId);
    }
    pickRepository.save(pick);
    return pick;
  }
}
