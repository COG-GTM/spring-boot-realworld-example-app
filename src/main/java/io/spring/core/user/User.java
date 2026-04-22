package io.spring.core.user;

import io.spring.Util;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class User {
  public static final String ROLE_USER = "USER";
  public static final String ROLE_ADMIN = "ADMIN";

  private String id;
  private String email;
  private String username;
  private String password;
  private String bio;
  private String image;
  private String role = ROLE_USER;

  public User(String email, String username, String password, String bio, String image) {
    this.id = UUID.randomUUID().toString();
    this.email = email;
    this.username = username;
    this.password = password;
    this.bio = bio;
    this.image = image;
    this.role = ROLE_USER;
  }

  public User(
      String email, String username, String password, String bio, String image, String role) {
    this(email, username, password, bio, image);
    this.role = (role == null || role.isEmpty()) ? ROLE_USER : role;
  }

  public boolean isAdmin() {
    return ROLE_ADMIN.equals(role);
  }

  public void update(String email, String username, String password, String bio, String image) {
    if (!Util.isEmpty(email)) {
      this.email = email;
    }

    if (!Util.isEmpty(username)) {
      this.username = username;
    }

    if (!Util.isEmpty(password)) {
      this.password = password;
    }

    if (!Util.isEmpty(bio)) {
      this.bio = bio;
    }

    if (!Util.isEmpty(image)) {
      this.image = image;
    }
  }
}
