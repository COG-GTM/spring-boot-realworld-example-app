package io.spring.core.golf;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Pick {
  private String id;
  private String contestId;
  private String tournamentId;
  private String userId;
  private String golferId;
  private DateTime createdAt;

  public Pick(String contestId, String tournamentId, String userId, String golferId) {
    this(UUID.randomUUID().toString(), contestId, tournamentId, userId, golferId, new DateTime());
  }

  public Pick(
      String id,
      String contestId,
      String tournamentId,
      String userId,
      String golferId,
      DateTime createdAt) {
    this.id = id;
    this.contestId = contestId;
    this.tournamentId = tournamentId;
    this.userId = userId;
    this.golferId = golferId;
    this.createdAt = createdAt;
  }
}
