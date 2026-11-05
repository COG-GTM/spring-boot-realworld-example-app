package io.spring.core.golf;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Golfer {
  private String id;
  private String name;
  private String country;

  public Golfer(String name, String country) {
    this(UUID.randomUUID().toString(), name, country);
  }

  public Golfer(String id, String name, String country) {
    this.id = id;
    this.name = name;
    this.country = country;
  }
}
