package io.spring.core.lp;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Activity {
  private String id;
  private String lpId;
  private String userId;
  private String type;
  private String notes;
  private DateTime createdAt;

  public Activity(String lpId, String userId, String type, String notes) {
    this(lpId, userId, type, notes, new DateTime());
  }

  public Activity(String lpId, String userId, String type, String notes, DateTime createdAt) {
    this.id = UUID.randomUUID().toString();
    this.lpId = lpId;
    this.userId = userId;
    this.type = type;
    this.notes = notes;
    this.createdAt = createdAt;
  }
}
