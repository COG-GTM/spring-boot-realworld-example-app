package io.spring.application.golf;

import io.spring.core.golf.GolfContest;
import io.spring.core.golf.Golfer;
import io.spring.core.golf.Tournament;
import io.spring.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ValidPick
public class SubmitPickCommand {
  private User user;
  private GolfContest contest;
  private Tournament tournament;
  private Golfer golfer;
}
