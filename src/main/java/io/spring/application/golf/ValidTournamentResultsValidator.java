package io.spring.application.golf;

import io.spring.core.golf.GolferRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;

/** Business rules for a {@link RecordResultsCommand}; field-level checks live on the params. */
public class ValidTournamentResultsValidator
    implements ConstraintValidator<ValidTournamentResults, RecordResultsCommand> {
  static final String NOT_STARTED = "tournament has not started yet, results can't be recorded";

  private final GolferRepository golferRepository;

  public ValidTournamentResultsValidator(GolferRepository golferRepository) {
    this.golferRepository = golferRepository;
  }

  @Override
  public boolean isValid(RecordResultsCommand command, ConstraintValidatorContext context) {
    if (command == null || command.getParam() == null || command.getParam().getResults() == null) {
      return true;
    }
    List<String[]> errors = new ArrayList<>();
    if (!command.getTournament().hasStarted(new DateTime())) {
      errors.add(new String[] {"tournamentId", NOT_STARTED});
    }
    Set<String> seen = new HashSet<>();
    for (GolferResultParam result : command.getParam().getResults()) {
      String golferId = result == null ? null : result.getGolferId();
      if (golferId == null) {
        continue;
      }
      if (!seen.add(golferId)) {
        errors.add(new String[] {"golferId", "golfer " + golferId + " is listed more than once"});
      } else if (!golferRepository.findById(golferId).isPresent()) {
        errors.add(new String[] {"golferId", "golfer " + golferId + " not found"});
      }
    }

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
}
