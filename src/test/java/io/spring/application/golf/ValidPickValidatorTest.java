package io.spring.application.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.golf.ContestEntryRepository;
import io.spring.core.golf.GolfContest;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.Pick;
import io.spring.core.golf.PickRepository;
import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ValidPickValidatorTest {
  private ContestEntryRepository contestEntryRepository;
  private PickRepository pickRepository;
  private TournamentRepository tournamentRepository;
  private Validator validator;

  private User user;
  private GolfContest contest;
  private Tournament upcoming;
  private Tournament otherUpcoming;
  private Tournament completed;
  private Golfer scheffler;
  private Golfer mcilroy;

  @BeforeEach
  public void setUp() {
    contestEntryRepository = mock(ContestEntryRepository.class);
    pickRepository = mock(PickRepository.class);
    tournamentRepository = mock(TournamentRepository.class);
    ValidPickValidator pickValidator =
        new ValidPickValidator(contestEntryRepository, pickRepository, tournamentRepository);
    validator =
        Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(factoryFor(pickValidator))
            .buildValidatorFactory()
            .getValidator();

    user = new User("a@b.com", "golfer", "123", "", "");
    contest = new GolfContest("2026-27 One-and-Done", "2026-27");
    DateTime now = new DateTime();
    upcoming =
        new Tournament(contest.getId(), "The Open", "Course", now.plusDays(10), now.plusDays(14));
    otherUpcoming =
        new Tournament(contest.getId(), "The Memorial", "Course", now.plusDays(3), now.plusDays(6));
    completed =
        new Tournament(
            contest.getId(), "The Masters", "Augusta", now.minusDays(30), now.minusDays(26));
    when(tournamentRepository.findById(eq(completed.getId()))).thenReturn(Optional.of(completed));
    when(tournamentRepository.findById(eq(otherUpcoming.getId())))
        .thenReturn(Optional.of(otherUpcoming));
    scheffler = new Golfer("golfer-scheffler", "Scottie Scheffler", "USA");
    mcilroy = new Golfer("golfer-mcilroy", "Rory McIlroy", "NIR");

    when(contestEntryRepository.exists(eq(contest.getId()), eq(user.getId()))).thenReturn(true);
    when(pickRepository.findByContestAndUser(eq(contest.getId()), eq(user.getId())))
        .thenReturn(Collections.emptyList());
  }

  @Test
  public void should_accept_valid_pick() {
    assertTrue(validate(upcoming, scheffler).isEmpty());
  }

  @Test
  public void should_reject_when_user_not_entered() {
    when(contestEntryRepository.exists(eq(contest.getId()), eq(user.getId()))).thenReturn(false);
    Map<String, String> errors = validate(upcoming, scheffler);
    assertEquals(ValidPickValidator.NOT_ENTERED, errors.get("contestId"));
  }

  @Test
  public void should_reject_started_tournament() {
    Map<String, String> errors = validate(completed, scheffler);
    assertEquals(ValidPickValidator.STARTED, errors.get("tournamentId"));
  }

  @Test
  public void should_reject_tournament_starting_exactly_now() {
    Tournament startingNow =
        new Tournament(contest.getId(), "Now", "c", new DateTime(), new DateTime().plusDays(4));
    assertEquals(ValidPickValidator.STARTED, validate(startingNow, scheffler).get("tournamentId"));
  }

  @Test
  public void should_reject_tournament_from_another_contest() {
    Tournament foreign =
        new Tournament(
            "other-contest",
            "Elsewhere",
            "c",
            new DateTime().plusDays(5),
            new DateTime().plusDays(9));
    assertEquals(
        ValidPickValidator.WRONG_CONTEST, validate(foreign, scheffler).get("tournamentId"));
  }

  @Test
  public void should_reject_golfer_reused_in_another_tournament() {
    usePicks(new Pick(contest.getId(), completed.getId(), user.getId(), scheffler.getId()));
    Map<String, String> errors = validate(upcoming, scheffler);
    assertEquals(
        "golfer already used in The Masters; each golfer can be picked once a season",
        errors.get("golferId"));
  }

  @Test
  public void should_reject_golfer_held_by_another_upcoming_pick() {
    usePicks(new Pick(contest.getId(), otherUpcoming.getId(), user.getId(), scheffler.getId()));
    assertTrue(validate(upcoming, scheffler).get("golferId").contains("The Memorial"));
  }

  @Test
  public void should_allow_swapping_golfer_for_unstarted_tournament() {
    usePicks(
        new Pick(contest.getId(), completed.getId(), user.getId(), scheffler.getId()),
        new Pick(contest.getId(), upcoming.getId(), user.getId(), mcilroy.getId()));
    Golfer rahm = new Golfer("golfer-rahm", "Jon Rahm", "ESP");
    assertTrue(validate(upcoming, rahm).isEmpty());
  }

  @Test
  public void should_allow_resubmitting_the_same_golfer_for_the_same_tournament() {
    usePicks(new Pick(contest.getId(), upcoming.getId(), user.getId(), mcilroy.getId()));
    assertTrue(validate(upcoming, mcilroy).isEmpty());
  }

  @Test
  public void should_reject_swap_to_golfer_used_elsewhere() {
    usePicks(
        new Pick(contest.getId(), completed.getId(), user.getId(), scheffler.getId()),
        new Pick(contest.getId(), upcoming.getId(), user.getId(), mcilroy.getId()));
    assertTrue(validate(upcoming, scheffler).containsKey("golferId"));
  }

  @Test
  public void should_report_every_broken_rule() {
    when(contestEntryRepository.exists(eq(contest.getId()), eq(user.getId()))).thenReturn(false);
    usePicks(new Pick(contest.getId(), otherUpcoming.getId(), user.getId(), scheffler.getId()));
    Map<String, String> errors = validate(completed, scheffler);
    assertEquals(3, errors.size());
  }

  private void usePicks(Pick... picks) {
    when(pickRepository.findByContestAndUser(eq(contest.getId()), eq(user.getId())))
        .thenReturn(Arrays.asList(picks));
    for (Pick pick : picks) {
      when(pickRepository.existsByContestUserAndGolfer(
              eq(contest.getId()), eq(user.getId()), eq(pick.getGolferId())))
          .thenReturn(true);
    }
  }

  private Map<String, String> validate(Tournament tournament, Golfer golfer) {
    Set<ConstraintViolation<SubmitPickCommand>> violations =
        validator.validate(new SubmitPickCommand(user, contest, tournament, golfer));
    return violations.stream()
        .collect(
            Collectors.toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));
  }

  private static ConstraintValidatorFactory factoryFor(ValidPickValidator pickValidator) {
    ConstraintValidatorFactory defaults =
        Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory();
    return new ConstraintValidatorFactory() {
      @Override
      @SuppressWarnings("unchecked")
      public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return key == ValidPickValidator.class ? (T) pickValidator : defaults.getInstance(key);
      }

      @Override
      public void releaseInstance(ConstraintValidator<?, ?> instance) {}
    };
  }
}
