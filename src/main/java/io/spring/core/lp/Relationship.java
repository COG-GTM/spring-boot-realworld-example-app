package io.spring.core.lp;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Relationship {
  private String lpId;
  private String contactId;
  private String role;

  public Relationship(String lpId, String contactId, String role) {
    this.lpId = lpId;
    this.contactId = contactId;
    this.role = role;
  }
}
