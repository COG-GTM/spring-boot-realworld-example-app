package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RegisterParamTest {

  @Test
  public void should_create_register_param_with_all_fields() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password123");

    assertEquals("test@example.com", param.getEmail());
    assertEquals("testuser", param.getUsername());
    assertEquals("password123", param.getPassword());
  }

  @Test
  public void should_create_register_param_with_no_arg_constructor() {
    RegisterParam param = new RegisterParam();
    assertNull(param.getEmail());
    assertNull(param.getUsername());
    assertNull(param.getPassword());
  }

  @Test
  public void should_create_register_param_with_empty_fields() {
    RegisterParam param = new RegisterParam("", "", "");

    assertEquals("", param.getEmail());
    assertEquals("", param.getUsername());
    assertEquals("", param.getPassword());
  }
}
