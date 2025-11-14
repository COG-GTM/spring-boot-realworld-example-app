package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class RegisterParamTest {

  @Test
  public void should_create_register_param_with_all_fields() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password123");

    assertThat(param.getEmail(), is("test@example.com"));
    assertThat(param.getUsername(), is("testuser"));
    assertThat(param.getPassword(), is("password123"));
  }

  @Test
  public void should_create_empty_register_param() {
    RegisterParam param = new RegisterParam();

    assertNotNull(param);
  }

  @Test
  public void should_handle_null_values() {
    RegisterParam param = new RegisterParam(null, null, null);

    assertThat(param.getEmail(), is((String) null));
    assertThat(param.getUsername(), is((String) null));
    assertThat(param.getPassword(), is((String) null));
  }
}
