package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UpdateUserParamTest {

  @Test
  public void should_create_update_user_param_with_builder() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("test@example.com")
            .username("testuser")
            .password("newpass")
            .bio("new bio")
            .image("image.jpg")
            .build();

    assertEquals("test@example.com", param.getEmail());
    assertEquals("testuser", param.getUsername());
    assertEquals("newpass", param.getPassword());
    assertEquals("new bio", param.getBio());
    assertEquals("image.jpg", param.getImage());
  }

  @Test
  public void should_create_update_user_param_with_defaults() {
    UpdateUserParam param = UpdateUserParam.builder().build();

    assertEquals("", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  public void should_create_update_user_param_with_no_arg_constructor() {
    UpdateUserParam param = new UpdateUserParam();

    assertNotNull(param);
  }

  @Test
  public void should_create_update_user_param_with_all_args_constructor() {
    UpdateUserParam param = new UpdateUserParam("email@test.com", "pass", "user", "bio", "img.jpg");

    assertEquals("email@test.com", param.getEmail());
    assertEquals("pass", param.getPassword());
    assertEquals("user", param.getUsername());
    assertEquals("bio", param.getBio());
    assertEquals("img.jpg", param.getImage());
  }

  @Test
  public void should_create_update_user_param_with_partial_builder() {
    UpdateUserParam param = UpdateUserParam.builder().email("test@example.com").build();

    assertEquals("test@example.com", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  public void should_create_update_user_param_with_only_password() {
    UpdateUserParam param = UpdateUserParam.builder().password("newpass123").build();

    assertEquals("", param.getEmail());
    assertEquals("newpass123", param.getPassword());
    assertEquals("", param.getUsername());
  }

  @Test
  public void should_create_update_user_param_with_only_bio_and_image() {
    UpdateUserParam param =
        UpdateUserParam.builder().bio("updated bio").image("new-image.jpg").build();

    assertEquals("updated bio", param.getBio());
    assertEquals("new-image.jpg", param.getImage());
    assertEquals("", param.getEmail());
  }
}
