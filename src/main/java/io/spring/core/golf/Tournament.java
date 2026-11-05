package io.spring.core.golf;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Tournament {
  private String id;
  private String contestId;
  private String name;
  private String course;
  private DateTime startsAt;
  private DateTime endsAt;

  public Tournament(
      String contestId, String name, String course, DateTime startsAt, DateTime endsAt) {
    this(UUID.randomUUID().toString(), contestId, name, course, startsAt, endsAt);
  }

  public Tournament(
      String id, String contestId, String name, String course, DateTime startsAt, DateTime endsAt) {
    this.id = id;
    this.contestId = contestId;
    this.name = name;
    this.course = course;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
  }

  public boolean hasStarted(DateTime now) {
    return !now.isBefore(startsAt);
  }

  public boolean hasEnded(DateTime now) {
    return now.isAfter(endsAt);
  }
}
