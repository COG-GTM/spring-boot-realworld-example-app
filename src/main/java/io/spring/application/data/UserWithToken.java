package io.spring.application.data;

import lombok.Getter;

@Getter
public class UserWithToken {
  private String id;
  private String email;
  private String username;
  private String bio;
  private String image;
  private String token;

  public UserWithToken(UserData userData, String token) {
    this.id = userData.getId();
    this.email = userData.getEmail();
    this.username = userData.getUsername();
    this.bio = userData.getBio();
    this.image = userData.getImage();
    this.token = token;
  }
}
