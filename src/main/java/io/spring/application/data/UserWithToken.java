package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class UserWithToken {
  private String email;
  private String username;
  private String bio;
  private String image;
  private String token;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String refreshToken;

  public UserWithToken(UserData userData, String token) {
    this.email = userData.getEmail();
    this.username = userData.getUsername();
    this.bio = userData.getBio();
    this.image = userData.getImage();
    this.token = token;
  }

  public UserWithToken(UserData userData, String token, String refreshToken) {
    this(userData, token);
    this.refreshToken = refreshToken;
  }
}
