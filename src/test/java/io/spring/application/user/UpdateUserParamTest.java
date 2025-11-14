package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UpdateUserParamTest {

  @Test
  public void should_create_update_user_param_with_all_fields() {
    UpdateUserParam param =
        new UpdateUserParam(
            "new@example.com", "newpassword", "newusername", "New bio", "http://new-image.url");

    assertThat(param.getEmail(), is("new@example.com"));
    assertThat(param.getPassword(), is("newpassword"));
    assertThat(param.getUsername(), is("newusername"));
    assertThat(param.getBio(), is("New bio"));
    assertThat(param.getImage(), is("http://new-image.url"));
  }

  @Test
  public void should_create_empty_update_user_param_with_defaults() {
    UpdateUserParam param = new UpdateUserParam();

    assertNotNull(param);
    assertThat(param.getEmail(), is(""));
    assertThat(param.getPassword(), is(""));
    assertThat(param.getUsername(), is(""));
    assertThat(param.getBio(), is(""));
    assertThat(param.getImage(), is(""));
  }

  @Test
  public void should_create_update_user_param_with_builder() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("builder@example.com")
            .password("builderpass")
            .username("builderuser")
            .bio("Builder bio")
            .image("http://builder-image.url")
            .build();

    assertThat(param.getEmail(), is("builder@example.com"));
    assertThat(param.getPassword(), is("builderpass"));
    assertThat(param.getUsername(), is("builderuser"));
    assertThat(param.getBio(), is("Builder bio"));
    assertThat(param.getImage(), is("http://builder-image.url"));
  }

  @Test
  public void should_handle_null_values() {
    UpdateUserParam param = new UpdateUserParam(null, null, null, null, null);

    assertThat(param.getEmail(), is((String) null));
    assertThat(param.getPassword(), is((String) null));
    assertThat(param.getUsername(), is((String) null));
    assertThat(param.getBio(), is((String) null));
    assertThat(param.getImage(), is((String) null));
  }
}
