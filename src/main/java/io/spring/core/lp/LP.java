package io.spring.core.lp;

import io.spring.Util;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class LP {
  private String id;
  private String userId;
  private String name;
  private String company;
  private String email;
  private Stage stage;
  private DateTime createdAt;
  private DateTime updatedAt;

  public LP(String name, String company, String email, String userId) {
    this(name, company, email, Stage.TERRITORY, userId, new DateTime());
  }

  public LP(String name, String company, String email, Stage stage, String userId) {
    this(name, company, email, stage, userId, new DateTime());
  }

  public LP(
      String name, String company, String email, Stage stage, String userId, DateTime createdAt) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
    this.company = company;
    this.email = email;
    this.stage = stage == null ? Stage.TERRITORY : stage;
    this.userId = userId;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public void update(String name, String company, String email) {
    if (!Util.isEmpty(name)) {
      this.name = name;
      this.updatedAt = new DateTime();
    }
    if (!Util.isEmpty(company)) {
      this.company = company;
      this.updatedAt = new DateTime();
    }
    if (!Util.isEmpty(email)) {
      this.email = email;
      this.updatedAt = new DateTime();
    }
  }

  public void moveToStage(Stage stage) {
    if (stage != null && stage != this.stage) {
      this.stage = stage;
      this.updatedAt = new DateTime();
    }
  }
}
