package io.spring.application.golf;

import io.spring.core.golf.ContestEntryRepository;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;

/** Enforces the one-and-done pick rules for a {@link SubmitPickCommand}. */
public class ValidPickValidator implements ConstraintValidator<ValidPick, SubmitPickCommand> {
  static final String NOT_ENTERED = "you must join this contest before making picks";
  static final String WRONG_CONTEST = "tournament does not belong to this contest";
  static final String STARTED = "tournament has already started, picks are locked";

  private final ContestEntryRepository contestEntryRepository;
  private final PickRepository pickRepository;
  private final TournamentRepository tournamentRepository;

  public ValidPickValidator(
      ContestEntryRepository contestEntryRepository,
      PickRepository pickRepository,
      TournamentRepository tournamentRepository) {
    this.contestEntryRepository = contestEntryRepository;
    this.pickRepository = pickRepository;
    this.tournamentRepository = tournamentRepository;
  }

  @Override
  public boolean isValid(SubmitPickCommand command, ConstraintValidatorContext context) {
    if (command == null) {
      return true;
    }
    String contestId = command.getContest().getId();
    String userId = command.getUser().getId();
    Tournament tournament = command.getTournament();
    String golferId = command.getGolfer().getId();

    List<String[]> errors = new ArrayList<>();
    if (!contestEntryRepository.exists(contestId, userId)) {
      errors.add(new String[] {"contestId", NOT_ENTERED});
    }
    if (!contestId.equals(tournament.getContestId())) {
      errors.add(new String[] {"tournamentId", WRONG_CONTEST});
    } else if (tournament.hasStarted(new DateTime())) {
      errors.add(new String[] {"tournamentId", STARTED});
    }
    findReuse(contestId, userId, tournament.getId(), golferId)
        .ifPresent(usedIn -> errors.add(new String[] {"golferId", reuseMessage(usedIn)}));

    if (errors.isEmpty()) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    for (String[] error : errors) {
      context
          .buildConstraintViolationWithTemplate(error[1])
          .addPropertyNode(error[0])
          .addConstraintViolation();
    }
    return false;
  }

  private Optional<Pick> findReuse(
      String contestId, String userId, String tournamentId, String golferId) {
    if (!pickRepository.existsByContestUserAndGolfer(contestId, userId, golferId)) {
      return Optional.empty();
    }
    return pickRepository.findByContestAndUser(contestId, userId).stream()
        .filter(pick -> pick.getGolferId().equals(golferId))
        .filter(pick -> !pick.getTournamentId().equals(tournamentId))
        .findFirst();
  }

  private String reuseMessage(Pick usedIn) {
    String tournamentName =
        tournamentRepository
            .findById(usedIn.getTournamentId())
            .map(Tournament::getName)
            .orElse(usedIn.getTournamentId());
    return "golfer already used in " + tournamentName + "; each golfer can be picked once a season";
  }
}
