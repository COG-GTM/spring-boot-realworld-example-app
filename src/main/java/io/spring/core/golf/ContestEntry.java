package io.spring.core.golf;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"contestId", "userId"})
public class ContestEntry {
  private String contestId;
  private String userId;
  private DateTime createdAt;

  public ContestEntry(String contestId, String userId) {
    this(contestId, userId, new DateTime());
  }

  public ContestEntry(String contestId, String userId, DateTime createdAt) {
    this.contestId = contestId;
    this.userId = userId;
    this.createdAt = createdAt;
  }
}
