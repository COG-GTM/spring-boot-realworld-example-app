package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UpdateUserParamTest {

  @Test
  public void should_create_with_defaults() {
    UpdateUserParam param = new UpdateUserParam();
    assertEquals("", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  public void should_create_with_builder() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("test@test.com")
            .password("newpass")
            .username("newuser")
            .bio("new bio")
            .image("new image")
            .build();

    assertEquals("test@test.com", param.getEmail());
    assertEquals("newpass", param.getPassword());
    assertEquals("newuser", param.getUsername());
    assertEquals("new bio", param.getBio());
    assertEquals("new image", param.getImage());
  }

  @Test
  public void should_create_with_all_args() {
    UpdateUserParam param =
        new UpdateUserParam("email@test.com", "pass", "user", "bio", "img");

    assertEquals("email@test.com", param.getEmail());
    assertEquals("pass", param.getPassword());
    assertEquals("user", param.getUsername());
    assertEquals("bio", param.getBio());
    assertEquals("img", param.getImage());
  }

  @Test
  public void should_create_with_builder_partial_fields() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("test@test.com")
            .build();

    assertEquals("test@test.com", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  public void should_create_with_builder_only_bio() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .bio("updated bio")
            .build();

    assertEquals("", param.getEmail());
    assertEquals("updated bio", param.getBio());
  }
}
