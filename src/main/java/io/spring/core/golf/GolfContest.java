package io.spring.core.golf;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class GolfContest {
  private String id;
  private String name;
  private String season;
  private DateTime createdAt;

  public GolfContest(String name, String season) {
    this(UUID.randomUUID().toString(), name, season, new DateTime());
  }

  public GolfContest(String id, String name, String season, DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.season = season;
    this.createdAt = createdAt;
  }
}
