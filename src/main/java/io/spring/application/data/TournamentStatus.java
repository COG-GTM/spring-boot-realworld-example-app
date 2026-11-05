package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonValue;
import org.joda.time.DateTime;

public enum TournamentStatus {
  UPCOMING("upcoming"),
  IN_PROGRESS("in_progress"),
  COMPLETED("completed");

  private final String value;

  TournamentStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public static TournamentStatus of(
      DateTime startsAt, DateTime endsAt, boolean hasResults, DateTime now) {
    if (hasResults || now.isAfter(endsAt)) {
      return COMPLETED;
    }
    if (!now.isBefore(startsAt)) {
      return IN_PROGRESS;
    }
    return UPCOMING;
  }
}
