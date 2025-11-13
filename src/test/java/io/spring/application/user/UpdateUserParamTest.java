package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateUserParamTest {

  @Test
  public void should_create_update_user_param_with_builder() {
    UpdateUserParam param = UpdateUserParam.builder()
        .email("test@example.com")
        .username("testuser")
        .password("password")
        .bio("bio")
        .image("image.jpg")
        .build();
    
    assertThat(param.getEmail(), is("test@example.com"));
    assertThat(param.getUsername(), is("testuser"));
    assertThat(param.getPassword(), is("password"));
    assertThat(param.getBio(), is("bio"));
    assertThat(param.getImage(), is("image.jpg"));
  }

  @Test
  public void should_have_default_empty_values() {
    UpdateUserParam param = new UpdateUserParam();
    
    assertThat(param.getEmail(), is(""));
    assertThat(param.getUsername(), is(""));
    assertThat(param.getPassword(), is(""));
    assertThat(param.getBio(), is(""));
    assertThat(param.getImage(), is(""));
  }
}
