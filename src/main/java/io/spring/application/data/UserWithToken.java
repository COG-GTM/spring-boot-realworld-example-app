package io.spring.application.data;

public record UserWithToken(String email, String username, String bio, String image, String token) {
  public UserWithToken(UserData userData, String token) {
    this(
        userData.getEmail(), userData.getUsername(), userData.getBio(), userData.getImage(), token);
  }
}
